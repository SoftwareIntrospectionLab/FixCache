package Cache;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import com.csvreader.CsvWriter;

import Util.CmdLineParser;
import Cache.CacheItem.CacheReason;
import Database.DatabaseManager;

public class Simulator {

    /**
     * Database prepared sql statements.
     */

    static final String findCommit = "select id, date, is_bug_fix from scmlog "
        + "where repository_id =? and date between ? and ? order by date ASC";
    static final String findFile = "select actions.file_id, type from actions, content_loc "
        + "where actions.file_id=content_loc.file_id "
        + "and actions.commit_id=? and content_loc.commit_id=? "
        + "and actions.file_id in( "
        + "select file_id from file_types where type='code') order by loc DESC";
    static final String findHunkId = "select id from hunks where file_id =? and commit_id =?";
    static final String findBugIntroCdate = "select date from hunk_blames, scmlog "
        + "where hunk_id =? and hunk_blames.bug_commit_id=scmlog.id";
    static final String findPid = "select id from repositories where id=?";
    static final String findFileCount = "select count(files.id) from files, file_types "
        + "where files.id = file_types.file_id and type = 'code' and repository_id=?";
    private static PreparedStatement findCommitQuery;
    private static PreparedStatement findFileQuery;
    private static PreparedStatement findHunkIdQuery;
    static PreparedStatement findBugIntroCdateQuery;
    static PreparedStatement findPidQuery;
    static PreparedStatement findFileCountQuery;

    /**
     * From the actions table. See the cvsanaly manual
     * (http://gsyc.es/~carlosgc/files/cvsanaly.pdf), pg 11
     */
    public enum FileType {
        A, M, D, V, C, R
    }

    /**
     * Member fields
     */
    final int blocksize; // number of co-change files to import
    final int prefetchsize; // number of (new or modified but not buggy) files
    // to import
    final int cachesize; // size of cache
    final int pid; // project (repository) id
    final boolean saveToFile; // whether there should be csv output
    final CacheReplacement.Policy cacheRep; // cache replacement policy
    final Cache cache; // the cache
    final static Connection conn = DatabaseManager.getConnection(); // for database

    int hit;
    int miss;
    private int commits;
    
    // For output
    // XXX separate class to manage output
    String outputDate;
    int outputSpacing = 3; // output the hit rate every 3 months
    int month = outputSpacing;
    CsvWriter csvWriter;
    int fileCount; // XXX where is this set? why static?
    String filename;

    public Simulator(int bsize, int psize, int csize, int projid,
            CacheReplacement.Policy rep, String start, String end, Boolean save) {

        pid = projid;

        fileCount = getFileCount(pid);
        
        if (bsize == -1)
            blocksize = (int) Math.round(fileCount * 0.05);
        else
            blocksize = bsize;
        if (csize == -1)
            cachesize = (int) Math.round(fileCount * 0.1);
        else
            cachesize = csize;
        if (psize == -1)
            prefetchsize = (int) Math.round(fileCount * 0.01);
        else
            prefetchsize = psize;

        cacheRep = rep;
        cache = new Cache(cachesize, new CacheReplacement(rep), start, end,
                projid);
        hit = 0;
        miss = 0;
        this.saveToFile = save;

        if (saveToFile == true) {
            filename = pid + "_" + cachesize + "_" + blocksize + "_"
            + prefetchsize + "_" + cacheRep;
            csvWriter = new CsvWriter("Results/" + filename + "_hitrate.csv");
            csvWriter.setComment('#');
            try {
                csvWriter.writeComment("hitrate for every 3 months, "
                        + "used to describe the variation of hit rate with time");
                csvWriter.writeComment("project: " + pid + ", cachesize: "
                        + cachesize + ", blocksize: " + cachesize
                        + ", prefetchsize: " + prefetchsize
                        + ", cache replacement policy: " + cacheRep);
                csvWriter.write("Month");
                //csvWriter.write("Range");
                csvWriter.write("HitRate");
                csvWriter.write("NumCommits");
                csvWriter.endRecord();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            findFileQuery = conn.prepareStatement(findFile);
            findCommitQuery = conn.prepareStatement(findCommit);
            findHunkIdQuery = conn.prepareStatement(findHunkId);
            findBugIntroCdateQuery = conn.prepareStatement(findBugIntroCdate);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    static int getFileCount(int projid) {
        int ret = 0;
        try {
            findFileCountQuery = conn.prepareStatement(findFileCount);
            findFileCountQuery.setInt(1, projid);
            ret = Util.Database.getIntResult(findFileCountQuery);
        } catch (SQLException e1) {
            e1.printStackTrace();
        }
        return ret;
    }

    /**
     * Prints out the command line options
     */
    private static void printUsage() {
        System.err
        .println("Example Usage: FixCache -b=10000 -c=500 -f=600 -r=\"LRU\" -p=1");
        System.err.println("Example Usage: FixCache --blksize=10000 "
                + "--csize=500 --pfsize=600 --cacherep=\"LRU\" --pid=1");
        System.err.println("-p/--pid option is required");
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
    public void loadBuggyEntity(int fileId, int cid, String commitDate, String intro_cdate) {

        if (cache.contains(fileId))
            hit++; 
        else
            miss++;
        
        cache.add(fileId, cid, commitDate, CacheItem.CacheReason.BugEntity);

        // add the co-changed files as well
        ArrayList<Integer> cochanges = CoChange.getCoChangeFileList(fileId,
                cache.startDate, intro_cdate, blocksize);
        cache.add(cochanges, cid, commitDate, CacheItem.CacheReason.CoChange);
    }

    /**
     * The main simulate loop. This loop processes all revisions starting at
     * cache.startDate
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
            findCommitQuery.setString(3, cache.endDate);

            // returns all commits to pid after cache.startDate
            allCommits = findCommitQuery.executeQuery();

            while (allCommits.next()) {
                commits++;
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
                    numprefetch = processOneFile(cid, cdate, isBugFix, file_id,
                            type, numprefetch);
                }
                numprefetch = 0;
                
                if (saveToFile == true) {
                    if (Util.Dates.getMonthDuration(outputDate, cdate) > outputSpacing
                            || cdate.equals(cache.endDate)) {
                        outputHitRate(cdate);
                    }
                }

            }
        } catch (Exception e) {
            System.out.println(e);
            e.printStackTrace();
        }
    }

    private void outputHitRate(String cdate) {
        // XXX what if commits are more than 3 months apart?
        // XXX eliminate range?
        //final String formerOutputDate = outputDate;
        
        if (!cdate.equals(cache.endDate)) {
            outputDate = Util.Dates.monthsLater(outputDate, outputSpacing);
        } else {
            outputDate = cdate;
        }
        
        try {
            csvWriter.write(Integer.toString(month));
            //csvWriter.write(Util.Dates.getRange(formerOutputDate, outputDate));
            csvWriter.write(Double.toString(getHitRate()));
            csvWriter.write(Integer.toString(getCommitCount()));
            csvWriter.endRecord();
        } catch (IOException e) {
            e.printStackTrace();
        }
        month += outputSpacing;
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
            cache.add(file_id, cid, cdate, CacheItem.CacheReason.Prefetch);
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
     * Database accessors
     */

    /**
     * Fills cache with pre-fetch size number of top-LOC files from initial
     * commit. Only called once per simulation // implicit input: initial commit
     * ID // implicit input: LOC for every file in initial commit ID // implicit
     * input: pre-fetch size
     */
    public void initialPreLoad() {

        cache.startDate = findFirstDate();
        cache.endDate = findLastDate();
        if (cache.startDate.compareTo(cache.endDate) > 0) {
            System.out.println("There is no commit between " + cache.startDate
                    + " and " + cache.endDate);
            System.exit(1);
        }
        outputDate = cache.startDate;
        final String findInitialPreload = "select content_loc.file_id, content_loc.commit_id "
            + "from content_loc, scmlog, actions, file_types "
            + "where repository_id=? and content_loc.commit_id = scmlog.id and date =? "
            + "and content_loc.file_id=actions.file_id "
            + "and content_loc.commit_id=actions.commit_id and actions.type!='D' "
            + "and file_types.file_id=content_loc.file_id and file_types.type='code' order by loc DESC";
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
                    cache.add(fileId, commitId, cache.startDate,
                            CacheItem.CacheReason.Prefetch);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Finds the first date after the startDate with repository entries. Only
     * called once per simulation.
     * 
     * @return The date for the prefetch.
     */
    private String findFirstDate() {
        String findFirstDate = "";
        final PreparedStatement findFirstDateQuery;
        String firstDate = "";
        try {
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
            if (firstDate == null) {
                System.out.println("Can not find any commit after "
                        + cache.startDate);
                System.exit(2);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return firstDate;
    }

    /**
     * Finds the last date before the endDate with repository entries. Only
     * called once per simulation.
     * 
     * @return The date for the the simulator.
     */
    private String findLastDate() {
        String findLastDate = null;
        final PreparedStatement findLastDateQuery;
        String lastDate = null;
        try {
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
        } catch (SQLException e) {
            e.printStackTrace();
        }
        if (lastDate == null) {
            System.out.println("Can not find any commit before "
                    + cache.endDate);
            System.exit(2);
        }
        return lastDate;
    }

    /**
     * use the fileId and commitId to get a list of changed hunks from the hunk
     * table. for each changed hunk, get the blamedHunk from the hunk_blame
     * table; get the commit id associated with this blamed hunk take the
     * maximum (in terms of date?) commit id and return it
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

    // XXX move to a different part of the file
    public void checkParameter() {
        if (cache.startDate != null && cache.endDate != null) {
            if (cache.startDate.compareTo(cache.endDate) > 0) {
                System.err
                .println("Error:Start date must be earlier than end date");
                printUsage();
                System.exit(2);
            }
        }
        try {
            findPidQuery = conn.prepareStatement(findPid);
            findPidQuery.setInt(1, pid);
            if (Util.Database.getIntResult(findPidQuery) == -1) {
                System.out.println("There is no project whose id is " + pid);
                System.exit(2);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    public static void main(String args[]) {

        /**
         * Command line parsing
         */
        CmdLineParser parser = new CmdLineParser();
        CmdLineParser.Option blksz_opt = parser.addIntegerOption('b', "blksize");
        CmdLineParser.Option csz_opt = parser.addIntegerOption('c', "csize");
        CmdLineParser.Option pfsz_opt = parser.addIntegerOption('f', "pfsize");
        CmdLineParser.Option crp_opt = parser.addStringOption('r', "cacherep");
        CmdLineParser.Option pid_opt = parser.addIntegerOption('p', "pid");
        CmdLineParser.Option sd_opt = parser.addStringOption('s', "start");
        CmdLineParser.Option ed_opt = parser.addStringOption('e', "end");
        CmdLineParser.Option save_opt = parser.addBooleanOption('o',"save");
        CmdLineParser.Option tune_opt = parser.addBooleanOption('u', "tune");
        // CmdLineParser.Option sCId_opt = parser.addIntegerOption('s',"start");
        // CmdLineParser.Option eCId_opt = parser.addIntegerOption('e',"end");
        try {
            parser.parse(args);
        } catch (CmdLineParser.OptionException e) {
            System.err.println(e.getMessage());
            printUsage();
            System.exit(2);
        }

        Integer blksz = (Integer) parser.getOptionValue(blksz_opt, -1);
        Integer csz = (Integer) parser.getOptionValue(csz_opt, -1);
        Integer pfsz = (Integer) parser.getOptionValue(pfsz_opt, -1);
        String crp_string = (String) parser.getOptionValue(crp_opt,
                CacheReplacement.REPDEFAULT.toString());
        Integer pid = (Integer) parser.getOptionValue(pid_opt);
        String start = (String) parser.getOptionValue(sd_opt, null);
        String end = (String) parser.getOptionValue(ed_opt, null);
        Boolean saveToFile = (Boolean) parser.getOptionValue(save_opt, false);
        Boolean tune = (Boolean)parser.getOptionValue(tune_opt, false);
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
        

        /**
         * Create a new simulator and run simulation.
         */
        if(tune)
        {
            System.out.println("tuning...");
            Simulator sim = tune(pid);
            System.out.println("highest hitrate:"+sim.getHitRate());
            System.out.println(sim.blocksize);
            System.out.println(sim.prefetchsize);
            System.out.println(sim.cacheRep.toString());
        }
        else
        {
            Simulator sim = mainloop(blksz, pfsz, csz, pid, crp, start, end,
                    saveToFile);

            if(sim.saveToFile==true)
            {
                sim.csvWriter.close();
                sim.outputFileDist();
            }

            sim.close();

            System.out.print("Hit rate...");
            System.out.println(sim.getHitRate());

            System.out.print("Num commits processed...");
            System.out.println(sim.getCommitCount());

            System.out.print("Num bug fixes...");
            System.out.println(sim.getHit() + sim.getMiss());

        }

    }
    
    public static Simulator mainloop(int blksz, int csz, int pfsz, int pid, CacheReplacement.Policy crp,
            String start, String end, Boolean saveToFile)
    {
        Simulator sim = new Simulator(blksz, pfsz, csz, pid, crp, start, end, saveToFile);
        sim.checkParameter();
        sim.initialPreLoad();
        sim.simulate();
        sim.close();
        return sim;
    }
    
    private static Simulator tune(int pid)
    {
        Simulator sim;
        Simulator maxsim = null;
        double maxhitrate = 0;
        int blksz;
        int pfsz;
        int onepercent = getPercentOfFiles(pid);
        final int UPPER = 21*onepercent;
                
        for(blksz=onepercent;blksz<UPPER;blksz+=onepercent*3){
            for(pfsz=onepercent;pfsz<UPPER;pfsz+=onepercent*3){
                for(CacheReplacement.Policy  crp:CacheReplacement.Policy.values()){
                    sim = new Simulator(blksz, pfsz,-1, pid, crp, null, null, false);
                    sim.checkParameter();
                    sim.initialPreLoad();
                    sim.simulate();
                    //sim.close();
                    if(sim.getHitRate()>maxhitrate)
                    {
                        maxhitrate = sim.getHitRate();
                        maxsim = sim;
                    }
                }
            }
        }
        return maxsim;
    }

    private static int getPercentOfFiles(int pid) {
        int ret =  (int) Math.round(getFileCount(pid)*0.01);
        if (ret == 0)
           return 1;
        else
           return ret;
    }

    public void outputFileDist() {

        csvWriter = new CsvWriter("Results/" + filename + "_filedist.csv");
        csvWriter.setComment('#');
        try {
            // csvWriter.write("# number of hit, misses and time stayed in Cache for every file");
            csvWriter.writeComment("number of hit, misses and time stayed in Cache for every file");
            csvWriter.writeComment("project: " + pid + ", cachesize: " + cachesize
                    + ", blocksize: " + cachesize + ", prefetchsize: "
                    + prefetchsize + ", cache replacement policy: " + cacheRep);
            csvWriter.write("file_id");
            csvWriter.write("loc");
            csvWriter.write("num_hits");
            csvWriter.write("num_misses");
            csvWriter.write("duration");
            csvWriter.endRecord();
            csvWriter.write("0");
            csvWriter.write("0");
            csvWriter.write("0");
            csvWriter.write("0");
            csvWriter.write(Integer.toString(cache.getTotalDuration()));
            csvWriter.endRecord();
            // else assume that the file already has the correct header line
            // write out record
            //XXX rewrite with built in iteratable
            for (CacheItem ci : cache.getCacheItemList()){
                csvWriter.write(Integer.toString(ci.getEntityId()));
                csvWriter.write(Integer.toString(ci.getLOC())); // LOC at time
                // of last
                // update
                csvWriter.write(Integer.toString(ci.getHitCount()));
                csvWriter.write(Integer.toString(ci.getMissCount()));
                csvWriter.write(Integer.toString(ci.getDuration()));
                csvWriter.endRecord();
            }

            csvWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private int getCommitCount() {
        return commits;
    }

    public CsvWriter getCsvWriter() {
        return csvWriter;
    }
}
