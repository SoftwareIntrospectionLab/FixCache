package Cache;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import Util.CmdLineParser;

import Cache.CacheItem.CacheReason;
import Database.DatabaseManager;

public class Simulator {

    /**
     * Database prepared sql statements.
     */

    static final String findCommit = "select id, date, is_bug_fix from scmlog " +
    "where repository_id =? and date>=? order by date ASC";
    static final String findFile = "select actions.file_id, type from actions, content_loc " +
    "where actions.file_id=content_loc.file_id " +
    "and actions.commit_id=? and content_loc.commit_id=? " +
    "and actions.file_id in( " +
    "select file_id from file_types where type='code') order by loc DESC";
    static final String findHunkId = "select id from hunks where file_id =? and commit_id =?";
    static final String findBugIntroCdate = "select date from hunk_blames, scmlog " +
    "where hunk_id =? and hunk_blames.bug_commit_id=scmlog.id";
    private static PreparedStatement findCommitQuery;
    private static PreparedStatement findFileQuery;
    private static PreparedStatement findHunkIdQuery;
    static PreparedStatement findBugIntroCdateQuery;


    /**
     * defaults
     */
    static final int BLKDEFAULT = 3;
    static final int PFDEFAULT = 3;
    static final int CSIZEDEFAULT = 10;
    static final int PRODEFAULT = 1;

    /**
     * From the actions table.
     * See the cvsanaly manual (http://gsyc.es/~carlosgc/files/cvsanaly.pdf), pg 11
     */
    public enum FileType {
        A, M, D, V, C, R
    }

    /**
     * Member fields
     */
    final int blocksize; // number of co-change files to import
    final int prefetchsize; // number of (new or modified but not buggy) files to import
    final int cachesize; // size of cache
    final int pid; // project (repository) id
    final CacheReplacement.Policy cacheRep; // cache replacement policy
    final Cache cache; // the cache
    final Connection conn = DatabaseManager.getConnection(); // for database

    int hit;
    int miss;

    public Simulator(int bsize, int psize, int csize, int projid,
            CacheReplacement.Policy rep, String start, String end) {
        blocksize = bsize;
        prefetchsize = psize;
        cachesize = csize;
        this.pid = projid;
        cacheRep = rep;
        cache = new Cache(cachesize, new CacheReplacement(rep), start, end, projid);
        hit = 0;
        miss = 0;
        
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
     * Prints out the command line options
     */
    private static void printUsage() {
        System.err
        .println("Example Usage: FixCache -b=10000 -c=500 -f=600 -r=\"LRU\" -p=1");
        System.err
        .println("Example Usage: FixCache --blksize=10000 " +
        "--csize=500 --pfsize=600 --cacherep=\"LRU\" --pid=1");
        System.err.println("-p/--pid option is required");
    }

    /**
     *  Loads an entity containing a bug into the cache. 
     * @param fileId 
     * @param cid -- commit id
     * @param commitDate -- commit date
     * @param intro_cdate -- bug introducing commit date
     */
    // XXX move hit and miss to the cache? 
    // could add if (reas == BugEntity) logic to add() code
    public void loadBuggyEntity(int fileId, int cid, String commitDate, String intro_cdate) {
        if (cache.contains(fileId)) 
            hit++;
        else 
            miss++;
        
        // XXX commitDate or intro_cdate?
        cache.add(fileId, cid, commitDate, CacheItem.CacheReason.BugEntity);
        
        // add the co-changed files as well
        ArrayList<Integer> cochanges = CoChange.getCoChangeFileList(fileId, intro_cdate, blocksize);
        cache.add(cochanges, cid, commitDate, CacheItem.CacheReason.CoChange);
    }

    /**
     * The main simulate loop. 
     * This loop processes all revisions starting at cache.startDate
     * 
     */
    public void simulate() {

        final ResultSet allCommits;
        int cid;// means commit_id in actions
        String cdate;
        
        boolean isBugFix;
        int file_id;
        FileType type;
        int numprefetch = 0;
        
        // iterate over the selection
        try {
            findCommitQuery.setInt(1, pid);
            findCommitQuery.setString(2, cache.startDate);
            
            // returns all commits to pid after cache.startDate
            allCommits = findCommitQuery.executeQuery(); 
            
            while (allCommits.next()) {
                cid = allCommits.getInt(1);
                cdate = allCommits.getString(2);
                isBugFix = allCommits.getBoolean(3);

                findFileQuery.setInt(1, cid);
                findFileQuery.setInt(2, cid);

                final ResultSet files = findFileQuery.executeQuery();
                
                // loop through those file ids
                while (files.next()) {
                    file_id = files.getInt(1);
                    type = FileType.valueOf(files.getString(2)); 
                    numprefetch = processOneFile(cid, cdate, isBugFix, file_id, type, numprefetch);
                }
                numprefetch = 0;
            }
        } catch (Exception e) {
            System.out.println(e);
            e.printStackTrace();
        }
    }

    private int processOneFile(int cid, String cdate, boolean isBugFix,
            int file_id, FileType type, int numprefetch) {
        switch (type) {
        case V:
            break;
        case R: 
        case C:
        case A:
            if (numprefetch < prefetchsize) {
                numprefetch++;
                cache.add(file_id, cid, cdate, CacheItem.CacheReason.Prefetch);
            }
            break;
        case D:
            this.cache.remove(file_id, cdate);// remove from the cache
            break;
        case M: // modified
            if (isBugFix) {
                String intro_cdate = this.getBugIntroCdate(file_id,cid);
                this.loadBuggyEntity(file_id, cid, cdate, intro_cdate);
            } else if (numprefetch < prefetchsize) {
                    numprefetch++;
                    cache.add(file_id, cid, cdate, CacheItem.CacheReason.Prefetch);
            }
        }
        return numprefetch;
    }

    /**
     * Gets the current hit rate of the cache
     * @return hit rate of the cache
     */
    public double getHitRate() {
        return (double) hit / (hit + miss);
    }
    
    public static void main(String args[]) {

        /**
         * Command line parsing
         */
        CmdLineParser parser = new CmdLineParser();
        CmdLineParser.Option blksz_opt = parser
        .addIntegerOption('b', "blksize");
        CmdLineParser.Option csz_opt = parser.addIntegerOption('c', "csize");
        CmdLineParser.Option pfsz_opt = parser.addIntegerOption('f', "pfsize");
        CmdLineParser.Option crp_opt = parser.addStringOption('r', "cacherep");
        CmdLineParser.Option pid_opt = parser.addIntegerOption('p', "pid");
        CmdLineParser.Option dt_opt = parser.addStringOption('t', "datetime");

        // CmdLineParser.Option sCId_opt = parser.addIntegerOption('s',"start");
        // CmdLineParser.Option eCId_opt = parser.addIntegerOption('e',"end");
        try {
            parser.parse(args);
        } catch (CmdLineParser.OptionException e) {
            System.err.println(e.getMessage());
            printUsage();
            System.exit(2);
        }

        Integer blksz = (Integer) parser.getOptionValue(blksz_opt, BLKDEFAULT);
        Integer csz = (Integer) parser.getOptionValue(csz_opt, CSIZEDEFAULT);
        Integer pfsz = (Integer) parser.getOptionValue(pfsz_opt, PFDEFAULT);
        String crp_string = 
            (String) parser.getOptionValue(crp_opt, CacheReplacement.REPDEFAULT.toString());
        Integer pid = (Integer) parser.getOptionValue(pid_opt, PRODEFAULT);
        String start = (String) parser.getOptionValue(dt_opt, null);
        String end = (String)parser.getOptionValue(dt_opt, null);
        CacheReplacement.Policy crp;
        try {
            crp = CacheReplacement.Policy.valueOf(crp_string);
        } catch (Exception e) {
            System.err.println(e.getMessage());
            System.err.println("Must specify a valid cache replacement policy");
            printUsage();
            crp = CacheReplacement.REPDEFAULT;
        }
        // startCId = (Integer)parser.getOptionValue(sCId_opt, STARTIDDEFAULT);
        // endCId = (Integer)parser.getOptionValue(eCId_opt, Integer.MAX_VALUE);
        if (pid == null) {
            System.err.println("Error: must specify a Project Id");
            printUsage();
            System.exit(2);
        }
       if(start!=null&&end!=null)
       {
           if(start.compareTo(end)>0)
           {
               System.err.println("Error:Start date must be earlier than end date");
               printUsage();
               System.exit(2);
           }
       }
       
        /**
         *  Create a new simulator and run simulation.
         */
        Simulator sim = new Simulator(blksz, pfsz, csz, pid, crp, start, end);
        sim.initialPreLoad();
        sim.simulate();
        sim.close();
        System.out.println(sim.getHitRate());
    }

    /**
     * Database accessors
     */
    
    /**
     * Fills cache with pre-fetch size number of top-LOC files from  initial commit.
     * Only called once per simulation
    // implicit input: initial commit ID
    // implicit input: LOC for every file in initial commit ID
    // implicit input: pre-fetch size
     */
    public void initialPreLoad() {
        cache.startDate = findFirstDate();
        cache.endDate  = findLastDate();
        if(cache.startDate.compareTo(cache.endDate )>=0)
        {
            System.out.println("There is no commit between "+cache.startDate+" and "+cache.endDate);
            System.exit(1);
        }       
        final String findInitialPreload = "select content_loc.file_id, content_loc.commit_id " +
        "from content_loc, scmlog, actions " +
        "where repository_id=? and content_loc.commit_id = scmlog.id and date =? " +
        "and content_loc.file_id=actions.file_id " +
        "and content_loc.commit_id=actions.commit_id " +
        "and actions.type!='D' order by loc DESC";
        final PreparedStatement findInitialPreloadQuery;
        ResultSet r = null;
        int fileId = 0;
        int commitId = 0;
        
        try {
            findInitialPreloadQuery = conn.prepareStatement(findInitialPreload);
            findInitialPreloadQuery.setInt(1, pid);
            findInitialPreloadQuery.setString(2, cache.startDate);
            r = findInitialPreloadQuery.executeQuery();
        } catch (SQLException e1) {
            e1.printStackTrace();
        }

        for (int size = 0; size < prefetchsize; size++) {
            try {
                if (r.next()) {
                    fileId = r.getInt(1);
                    commitId = r.getInt(2);
                    cache.add(fileId, commitId, cache.startDate, CacheItem.CacheReason.Prefetch);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Finds the first date after the startDate with repository entries.
     * Only called once per simulation.
     * @return The date for the prefetch.
     */
    private String findFirstDate() {
        String findFirstDate = "";
        final PreparedStatement findFirstDateQuery;
        String firstDate = "";
        try{
            if (cache.startDate == null) {
                findFirstDate = "select min(date) from scmlog where repository_id=?";
                findFirstDateQuery = conn.prepareStatement(findFirstDate);
                findFirstDateQuery.setInt(1, pid);
            } else {
                findFirstDate = "select min(date) from scmlog where repository_id=? and date >=?";
                findFirstDateQuery = conn.prepareStatement(findFirstDate);
                findFirstDateQuery.setInt(1, pid);
                findFirstDateQuery.setString(2, cache.startDate);
            }
            firstDate = Util.Database.getStringResult(findFirstDateQuery);
            if(firstDate==null)
            {
            	System.out.println("Can not find any commit after"+cache.startDate);
            	System.exit(2);
            }
        }   
        catch (SQLException e) {
            e.printStackTrace();
        }
        return firstDate;
    }
    
    /**
     * Finds the last date before the endDate with repository entries.
     * Only called once per simulation.
     * @return The date for the the simulator.
     */
    private String findLastDate() {
        String findLastDate = null;
        final PreparedStatement findLastDateQuery;
        String lastDate = null;
        try{
            if (cache.endDate == null) {
                findLastDate = "select max(date) from scmlog where repository_id=?";
                findLastDateQuery = conn.prepareStatement(findLastDate);
                findLastDateQuery.setInt(1, pid);
            } else {
                findLastDate = "select max(date) from scmlog where repository_id=? and date <=?";
                findLastDateQuery = conn.prepareStatement(findLastDate);
                findLastDateQuery.setInt(1, pid);
                findLastDateQuery.setString(2, cache.endDate);
            }
            lastDate = Util.Database.getStringResult(findLastDateQuery);
        }   
        catch (SQLException e) {
            e.printStackTrace();
        }
        if(lastDate==null)
        {
        	System.out.println("Can not find any commit before"+cache.endDate);
        	System.exit(2);
        }
        return lastDate;
    }

    /** use the fileId and commitId to get a list of changed hunks from the hunk table.
    * for each changed hunk, get the blamedHunk from the hunk_blame table;
    * get the commit id associated with this blamed hunk
    * take the maximum (in terms of date?) commit id and return it
    * */

    public String getBugIntroCdate(int fileId, int commitId) {

        // XXX optimize this code?
        String bugIntroCdate = "";
        int hunkId;
        ResultSet r = null;
        ResultSet r1 = null;
        try {
            findHunkIdQuery.setInt(1, fileId);
            findHunkIdQuery.setInt(2, commitId);
            r = findHunkIdQuery.executeQuery();
            while (r.next()) {
                hunkId = r.getInt(1);
                
                findBugIntroCdateQuery.setInt(1, hunkId);
                r1 = findBugIntroCdateQuery.executeQuery();
                while (r1.next()) {
                    if (r1.getString(1).compareTo(bugIntroCdate) > 0) {
                        bugIntroCdate = r1.getString(1);
                    }
                }
            }
        } catch (Exception e) {
            System.out.println(e);
            System.exit(0);
        }

        return bugIntroCdate;
    }

    /**
     * Closes the database connection
     */
    private void close() {
        DatabaseManager.close();
    }

    
    /**
     * For Debugging
     */
    
    public int getHit() {
        return hit;
    }

    public int getMiss() {
        return miss;
    }
    
    public Cache getCache() {
        return cache;
    }    

    public void add(int eid, int cid, String cdate, CacheReason reas) {
        cache.add(eid, cid, cdate, reas);
    }
    
}
