package Cache;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Map.Entry;

import Cache.CacheItem.CacheReason;
import Database.DatabaseManager;

public class Simulator {

    /**
     * Database prepared sql statements.
     */
    
    static final String findCommit = "select id, date, is_bug_fix from scmlog "
        + "where repository_id =? and date between ? and ? order by date ASC";
    static final String findFile = "select file_name, actions.type " +
    		"from actions, content_loc, files, file_types "
        + "where actions.file_id=content_loc.file_id and actions.file_id=files.id "
        + "and actions.commit_id=? and content_loc.commit_id=? " //XXX why two '?'
        + "and actions.file_id=file_types.file_id and file_types.type='code' order by loc DESC";
    static final String findHunkId = "select hunks.id from hunks, files " +
    		"where hunks.file_id=files.id and file_name =? and commit_id =?";
    static final String findBugIntroCdate = "select date from hunk_blames, scmlog "
        + "where hunk_id =? and hunk_blames.bug_commit_id=scmlog.id";
    private PreparedStatement findCommitQuery;
    private PreparedStatement findFileQuery;
    private PreparedStatement findHunkIdQuery;
    private PreparedStatement findBugIntroCdateQuery;

    /**
     * From the actions table. See the cvsanaly manual
     * (http://gsyc.es/~carlosgc/files/cvsanaly.pdf), pg 11
     */
    public enum ActionType {
        A, M, D, V, C, R
    }

    /**
     * Member fields
     */
    final int blocksize; // number of co-change files to import
    final int prefetchsize; // number of (new or modified but not buggy) files
    final int pid; // project (repository) id
    final CacheReplacement.Policy cacheRep; // cache replacement policy
    final Cache cache; // the cache
    final static Connection conn = DatabaseManager.getConnection(); // for database
    final OutputManager output; // handles output

    // all initialized to 0 by default
    int hit;
    int miss;
    private int commits;
    private int totalcommits;
    private int bugcount;
    private int filesProcessed;
    
    /**
     * Create a new simulator, with input parameters designated by "in"
     * @param in
     */
    public Simulator(InputManager in) {
        // initialize final fields
        this.pid = in.pid;
        this.blocksize = in.blksize;
        this.prefetchsize = in.prefetchsize;
        this.cacheRep = in.crp;

        cache = new Cache(in.cachesize, new CacheReplacement(in.crp), in.start, in.end,
                in.pid);
        
        output= new OutputManager(cache.startDate, in.saveToFile, in.monthly);
  
        try {
            findFileQuery = conn.prepareStatement(findFile);
            findCommitQuery = conn.prepareStatement(findCommit);
            findHunkIdQuery = conn.prepareStatement(findHunkId);
            findBugIntroCdateQuery = conn.prepareStatement(findBugIntroCdate);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    

    /**
     * Loads an entity containing a bug into the cache.
     * 
     * @param fileId
     * @param cid
     *            -- commit id
     * @param commitDate
     *            -- commit date
     * @param intro_cdate
     *            -- bug introducing commit date
     */
    // XXX move hit and miss to the cache?
    // could add if (reas == BugEntity) logic to add() code
    public void loadBuggyEntity(String fileId, int cid, String commitDate, String intro_cdate) {

        bugcount++;
        if (cache.contains(fileId))
            hit++; 
        else
            miss++;

        cache.add(fileId, cid, commitDate, CacheItem.CacheReason.BugEntity);

        // add the co-changed files as well
        ArrayList<Entry<String, Integer>> cochanges = 
                        CoChange.getCoChangeFileList(fileId, cache.startDate, 
                                intro_cdate, pid, cache);

        for (int i = 0; i < blocksize - 1; i++)
        {
            if (cochanges.size() > i) {
                // XXX this may add files which were not modified in cid
                cache.add(cochanges.get(i).getKey(), 
                        cid, commitDate, CacheItem.CacheReason.CoChange);
            }
        }
    }

    /**
     * The main simulate loop. This loop processes all revisions starting at
     * cache.startDate
     * 
     */
    public void simulate() {

        final ResultSet allCommits;
        int cid;// means commit_id in actions
        String cdate = null;

        boolean isBugFix;
        String fileName;
        ActionType type;
        int numprefetch = 0;

        // iterate over the selection
        try {
            findCommitQuery.setInt(1, pid);
            findCommitQuery.setString(2, cache.startDate);
            findCommitQuery.setString(3, cache.endDate);

            // returns all commits to pid after cache.startDate
            allCommits = findCommitQuery.executeQuery();

            while (allCommits.next()) {
                // inc commit counters
                commits++;
                totalcommits++;
                cid = allCommits.getInt(1);
                cdate = allCommits.getString(2);
                isBugFix = allCommits.getBoolean(3);

                findFileQuery.setInt(1, cid);
                findFileQuery.setInt(2, cid);

                final ResultSet files = findFileQuery.executeQuery();
                // loop through those file ids
                while (files.next()) {
                    fileName = files.getString(1); //XXX fix query
                    type = ActionType.valueOf(files.getString(2));
                    numprefetch = processOneFile(cid, cdate, isBugFix, fileName,
                            type, numprefetch);
                }
                numprefetch = 0;
                output.manage(cdate, this);
            }      
        } catch (Exception e) {
            System.out.println(e);
            e.printStackTrace();
        }
    }

    
    /**
     * Processes one file action from a particular commit
     * @param cid -- the commit id
     * @param cdate -- the commit date
     * @param isBugFix -- whether the commit is bug fixing
     * @param file_id -- the file id/name
     * @param type -- the type of action performed on the file in this commit
     * @param numprefetch -- the number of files prefetched so far
     * @return the number of files prefetched, inc. this one if needed
     */
    private int processOneFile(int cid, String cdate, boolean isBugFix,
            String file_id, ActionType type, int numprefetch) {
        filesProcessed++;

        switch (type) {
        case V:
            break;
        case R:
        case C:
        case A:
            if (numprefetch < prefetchsize) {
                numprefetch++;
                cache.add(file_id, cid, cdate, CacheItem.CacheReason.NewEntity);
            }
            break;
        case D:
            if(cache.contains(file_id)){
                this.cache.remove(file_id, cdate);
            }
            break;
        case M: // modified
            if (isBugFix) {
                String intro_cdate = this.getBugIntroCdate(file_id, cid);
                this.loadBuggyEntity(file_id, cid, cdate, intro_cdate);
            } else if (numprefetch < prefetchsize) {
                numprefetch++;
                cache.add(file_id, cid, cdate, CacheItem.CacheReason.ModifiedEntity);
            }
        }
        return numprefetch;
    }

    /**
     * Gets the current hit rate of the cache
     * 
     * @return hit rate of the cache
     */
    public double getHitRate() {
        double hitrate = (double) hit / (hit + miss);
        return (double) Math.round(hitrate * 10000) / 100;
    }


    /**
     * Fills cache with pre-fetch size number of top-LOC files from initial
     * commit. Only called once per simulation // implicit input: initial commit
     * ID // implicit input: LOC for every file in initial commit ID // implicit
     * input: pre-fetch size
     */
    public void initialPreLoad() {

        final String findInitialPreload = "select files.file_name, content_loc.commit_id "
            + "from content_loc, scmlog, actions, file_types, files "
            + "where files.repository_id=? and content_loc.commit_id = scmlog.id and date <=? "
            + "and content_loc.file_id=actions.file_id and files.id=actions.file_id "
            + "and content_loc.commit_id=actions.commit_id and actions.type!='D' "
            + "and file_types.file_id=content_loc.file_id and file_types.type='code' " +
            		"order by loc DESC";
        final PreparedStatement findInitialPreloadQuery;
        ResultSet preloadFiles = null;
        String fileName = null;
        int commitId = 0;

        try {
            findInitialPreloadQuery = conn.prepareStatement(findInitialPreload);
            findInitialPreloadQuery.setInt(1, pid);
            findInitialPreloadQuery.setString(2, cache.startDate);
            preloadFiles = findInitialPreloadQuery.executeQuery();
        } catch (SQLException e1) {
            e1.printStackTrace();
        }

        // Note: preload may not fill the cache completely if there are 
        // not enough code files before the starting date
        try {
        	while (preloadFiles.next()) {
        		fileName = preloadFiles.getString(1);
        		commitId = preloadFiles.getInt(2);
        		cache.add(fileName, commitId, cache.startDate,
        				CacheItem.CacheReason.Preload);
        		if (cache.isFull())
        			break;
        	}
        } catch (SQLException e) {
        	e.printStackTrace();
        }
    }


    /**
     * use the fileId and commitId to get a list of changed hunks from the hunk
     * table. for each changed hunk, get the blamedHunk from the hunk_blame
     * table; get the commit id associated with this blamed hunk take the
     * maximum (in terms of date?) commit id and return it
     * */

    public String getBugIntroCdate(String fileName, int commitId) {
        // XXX right now we just pick one 

        String bugIntroCdate = "";
        int hunkId;
        ResultSet r = null;
        ResultSet r1 = null;
        try {
            findHunkIdQuery.setString(1, fileName); 
            findHunkIdQuery.setInt(2, commitId);
            r = findHunkIdQuery.executeQuery();
            if (r.next()){
            //while (r.next()) {
                hunkId = r.getInt(1);

                findBugIntroCdateQuery.setInt(1, hunkId);
                r1 = findBugIntroCdateQuery.executeQuery();
                if (r1.next()) {
//                while(r1.next()){
//                    if (r1.getString(1).compareTo(bugIntroCdate) > 0) {
                        bugIntroCdate = r1.getString(1);
//                    }
                }
            }
        } catch (Exception e) {
            System.out.println(e);
            System.exit(0);
        }

        return bugIntroCdate;
    }



    public static void main(String args[]) {

        InputManager in = new InputManager(args);
        /**
         * Create a new simulator and run simulation.
         */
        Simulator sim;

        if(in.tune)
        {
            System.out.println("tuning...");
            sim = tune(in.pid, in.blksize, in.prefetchsize, in.cachesize);
            System.out.println(".... finished tuning!");
            System.out.println("highest hitrate:"+sim.getHitRate());
        }
        else
        {
            sim = new Simulator(in);
            sim.initialPreLoad();
            sim.simulate();
            sim.output.finish(sim);

        }

        // should always happen
        sim.close();
        OutputManager.printSummary(sim);
    }

    private static Simulator tune(int pid, int blksz, int pfsz, int csz)
    {
        Simulator maxsim = null;
        double maxhitrate = 0;
        
        boolean testblks = blksz < 0; 
        boolean testpfs = pfsz < 0; 
        
        if (testblks) blksz = 0;
        if (testpfs) pfsz = 0;
        
        if (csz < 0){
            System.out.println("Must specify a cache size to tune with");
            System.exit(1);
        }
        
        int onepercent = Math.round(csz/10);
        if (onepercent == 0) onepercent = 1; 
        int halfpercent = Math.round(onepercent/2);
        if (halfpercent == 0) halfpercent = 1; 
        int limit = Math.round(csz/2);

        System.out.print("Cache size: ");
        System.out.println(csz);
        System.out.println("One percent of files (assumed): " + onepercent);
        System.out.println("Half percent of files (assumed): " + halfpercent);
        System.out.println("Upper limit for blksize and pfsize: " + limit);
        
        CacheReplacement.Policy crp = CacheReplacement.REPDEFAULT;
        

        if (testblks){
            System.out.println("Testing blocksizes....");
            for(int b=onepercent;b<limit;b+=onepercent){
                final Simulator sim = new Simulator(new InputManager(b, pfsz, csz, pid, crp));
                sim.initialPreLoad();
                sim.simulate();
                System.out.print("blksize: ");
                System.out.println(b);
                System.out.print("hitrate: ");
                System.out.println(sim.getHitRate());
                if(sim.getHitRate()>maxhitrate)
                {
                    maxhitrate = sim.getHitRate();
                    maxsim = sim;
                    blksz = maxsim.blocksize;
                }else if (sim.getHitRate() < maxhitrate){
                    break; // hit rates decreasing
                }
            }
        }
        
        System.out.print("Best blksize: ");
        System.out.println(blksz);
        
        if (testpfs) {
            System.out.println("Testing prefetchsizes....");
            for(int p=halfpercent;p<limit;p+=halfpercent){
                final Simulator sim = new Simulator(new InputManager(blksz, p, csz, pid, crp));
                sim.initialPreLoad();
                sim.simulate();
                System.out.print("pfsz: ");
                System.out.println(p);
                System.out.print("hitrate: ");
                System.out.println(sim.getHitRate());
                if(sim.getHitRate()>maxhitrate)
                {
                    maxhitrate = sim.getHitRate();
                    maxsim = sim;
                    pfsz = maxsim.prefetchsize;
                }else if (sim.getHitRate() < maxhitrate){
                    break;
                }
            }
        }

        System.out.print("Best pfsize: ");
        System.out.println(pfsz);

        System.out.println("Testing cache replacements....");
        System.out.println("Trying out different cache replacment policies...");
        for(CacheReplacement.Policy crtst :CacheReplacement.Policy.values()){
            final Simulator sim = 
                new Simulator(new InputManager(blksz, pfsz,
                        csz, pid, crtst));
            sim.initialPreLoad();
            sim.simulate();
            System.out.print("Cache Replacement: ");
            System.out.println(crtst.toString());
            System.out.print("hitrate: ");
            System.out.println(sim.getHitRate());
            if(sim.getHitRate()>maxhitrate)
            {
                maxhitrate = sim.getHitRate();
                maxsim = sim;
            }
        }

        maxsim.close();
        return maxsim;
    }


    protected int resetCommitCount() {
        int oldcommits = commits;
        commits = 0;
        return oldcommits;
    }

    protected int resetBugCount() {
        int oldbugs = bugcount;
        bugcount = 0;
        return oldbugs;
    }

    protected int resetFilesProcessedCount() {
        int oldfiles = filesProcessed;
        filesProcessed = 0;
        return oldfiles;
    }

    int getTotalCommitCount() {
        return totalcommits;
    }

    
    public int getCacheSize(){
    	return cache.maxsize;
    }

    /**
     * Closes the database connection
     */
    private void close() {
        DatabaseManager.close();
    }

    public int getHit() {
        return hit;
    }

    public int getMiss() {
        return miss;
    }

    public Cache getCache() {
        return cache;
    }

    public void add(String eid, int cid, String cdate, CacheReason reas) {
        cache.add(eid, cid, cdate, reas);
    }


}

