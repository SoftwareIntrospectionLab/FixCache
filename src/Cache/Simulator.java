package Cache;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import Util.CmdLineParser;

import Database.DatabaseManager;

public class Simulator {

    static final int BLKDEFAULT = 3;
    static final int PFDEFAULT = 3;
    static final int CSIZEDEFAULT = 10;
    static final int PRODEFAULT = 1;

    public enum FileType {
        A, M, D, V, C, R
    }

    int blocksize;
    int prefetchsize;
    int cachesize;
    int pid;
    CacheReplacement.Policy cacheRep;
    Cache cache;
    Connection conn = DatabaseManager.getConnection();
    int hit;
    int miss;
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
    private static PreparedStatement findBugIntroCdateQuery;

    public Simulator(int bsize, int psize, int csize, int projid,
            CacheReplacement.Policy rep, String start) {
        blocksize = bsize;
        prefetchsize = psize;
        cachesize = csize;
        this.pid = projid;
        cacheRep = rep;
        cache = new Cache(cachesize, new CacheReplacement(rep), start, projid);
        hit = 0;
        miss = 0;

    }

    // input: initial commit ID
    // input: LOC for every file in initial commit ID
    // input: pre-fetch size
    // output: fills cache with pre-fetch size number of top-LOC files from
    // initial commit
    public void initialPreLoad() {
        String firstDate = findFirstDate();
        final String findInitialPreload = "select content_loc.file_id, content_loc.commit_id " +
        		"from content_loc, scmlog, actions " +
                "where repository_id=? and content_loc.commit_id = scmlog.id and date =? " +
                "and content_loc.file_id=actions.file_id " +
                "and content_loc.commit_id=actions.commit_id " +
                "and actions.type!='D' order by loc DESC";
        PreparedStatement findInitialPreloadQuery;
        ResultSet r = null;
        int fileId = 0;
        int commitId = 0;
        try {
            findInitialPreloadQuery = conn.prepareStatement(findInitialPreload);
            findInitialPreloadQuery.setInt(1, pid);
            findInitialPreloadQuery.setString(2, firstDate);
            r = findInitialPreloadQuery.executeQuery();
        } catch (SQLException e1) {
            e1.printStackTrace();
        }

        for (int size = 0; size < prefetchsize; size++) {
            try {
                if (r.next()) {
                    fileId = r.getInt(1);
                    commitId = r.getInt(2);
                    cache.add(fileId, commitId, firstDate,
                            CacheItem.CacheReason.Prefetch);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private String findFirstDate() {
        String findFirstDate = "";
        PreparedStatement findFirstDateQuery;
        String firstDate = "";
        if (cache.startDate == null) {
            findFirstDate = "select min(date) from scmlog where repository_id=?";
            try {
                findFirstDateQuery = conn.prepareStatement(findFirstDate);
                findFirstDateQuery.setInt(1, pid);
                firstDate = Util.Database.getStringResult(findFirstDateQuery);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } else {
            findFirstDate = "select min(date) from scmlog where repository_id=? and date >=?";
            try {
                findFirstDateQuery = conn.prepareStatement(findFirstDate);
                findFirstDateQuery.setInt(1, pid);
                findFirstDateQuery.setString(2, cache.startDate);
                firstDate = Util.Database.getStringResult(findFirstDateQuery);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        return firstDate;
    }

    private static void printUsage() {
        System.err
                .println("Example Usage: FixCache -b=10000 -c=500 -f=600 -r=\"LRU\" -p=1");
        System.err
                .println("Example Usage: FixCache --blksize=10000 " +
                		"--csize=500 --pfsize=600 --cacherep=\"LRU\" --pid=1");
        System.err.println("-p/--pid option is required");
    }

    public void versionPreLoad(int numprefetch, int fileId, int cid,
            String commitDate, CacheItem.CacheReason cacheReason) {
        if (numprefetch < prefetchsize) {
            numprefetch++;
            cache.add(fileId, cid, commitDate, CacheItem.CacheReason.Prefetch);
        }
    }

    public String getBugIntroCdate(int fileId, int commitId) {
        // use the fileId and commitId to get a list of changed hunks from the
        // hunk table.
        // for each changed hunk, get the blamedHunk from the hunk_blame table;
        // get the commit id associated with this blamed hunk
        // take the maximum (in terms of date?) commit id and return it

        // XXX optimize this code?
        String bugIntroCdate = "";
        int hunkId;
        ResultSet r = null;
        ResultSet r1 = null;
        try {
            findHunkIdQuery = conn.prepareStatement(findHunkId);
            findHunkIdQuery.setInt(1, fileId);
            findHunkIdQuery.setInt(2, commitId);
            r = findHunkIdQuery.executeQuery();
            while (r.next()) {
                hunkId = r.getInt(1);
                findBugIntroCdateQuery = conn
                        .prepareStatement(findBugIntroCdate);
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

    public Cache getCache() {
        return cache;
    }

    public void loadBuggyEntity(int fileId, int cid, String commitDate,
            String intro_cdate) {
        if (cache.contains(fileId)) {
            hit++;
        } else {
            miss++;
        }
        // XXX cid or intro_cid?
        cache.add(fileId, cid, commitDate, CacheItem.CacheReason.BugEntity);
        ArrayList<Integer> cochanges = CoChange.getCoChangeFileList(fileId, intro_cdate, blocksize);
        cache.add(cochanges, cid, commitDate, CacheItem.CacheReason.CoChange);
    }

    public int getHit() {
        return hit;
    }

    public int getMiss() {
        return miss;
    }

    public void simulate() {

        ResultSet r1;
        ResultSet r2;
        int cid;// means commit_id in actions
        String cdate;
        boolean isBugFix;
        int file_id;
        FileType type;
        int numprefetch = 0;
        // iterate over the selection
        try {
            findCommitQuery = conn.prepareStatement(findCommit);
            findCommitQuery.setInt(1, pid);
            findCommitQuery.setString(2, cache.startDate);
            r1 = findCommitQuery.executeQuery();
            while (r1.next()) {
                cid = r1.getInt(1);
                cdate = r1.getString(2);
                isBugFix = r1.getBoolean(3);
                // only deal with .java files
                findFileQuery = conn.prepareStatement(findFile);
                findFileQuery.setInt(1, cid);
                findFileQuery.setInt(2, cid);
                r2 = findFileQuery.executeQuery();
                // loop through those file ids
                while (r2.next()) {
                    file_id = r2.getInt(1);
                    type = FileType.valueOf(r2.getString(2));
                    switch (type) {
                    case V:
                        break;
                    case R:
                        this.versionPreLoad(numprefetch, file_id, cid, cdate,
                                CacheItem.CacheReason.NewEntity);
                        break;
                    case C:
                        this.versionPreLoad(numprefetch, file_id, cid, cdate,
                                CacheItem.CacheReason.NewEntity);
                        break;
                    case A:
                        this.versionPreLoad(numprefetch, file_id, cid, cdate,
                                CacheItem.CacheReason.NewEntity);
                        break;
                    case D:
                        this.cache.remove(file_id);// remove from the cache
                        break;
                    case M: // modified
                        if (isBugFix) {
                            String intro_cdate = this.getBugIntroCdate(file_id,
                                    cid);
                            this.loadBuggyEntity(file_id, cid, cdate,
                                    intro_cdate);
                        } else {
                            this.versionPreLoad(numprefetch, file_id, cid,
                                    cdate, CacheItem.CacheReason.ModifiedEntity);

                        }
                    }
                }
                numprefetch = 0;
            }
        } catch (Exception e) {
            System.out.println(e);
            System.exit(0);
        }
    }

    public double getHitRate() {
        return (double) hit / (hit + miss);
    }

    public static void main(String args[]) {

        // String startDate, endDate;
        String start;
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
        String crp_string = (String) parser.getOptionValue(crp_opt,
                CacheReplacement.REPDEFAULT);
        Integer pid = (Integer) parser.getOptionValue(pid_opt, PRODEFAULT);
        String dt = (String) parser.getOptionValue(dt_opt,
                "2000-01-01 00:00:00");
        CacheReplacement.Policy crp;
        try {
            crp = CacheReplacement.Policy.valueOf(crp_string);
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            System.err.println("Must specify a valid cache replacement policy");
            crp = CacheReplacement.REPDEFAULT;
        }
        // startCId = (Integer)parser.getOptionValue(sCId_opt, STARTIDDEFAULT);
        // endCId = (Integer)parser.getOptionValue(eCId_opt, Integer.MAX_VALUE);
        start = dt;
        if (pid == null) {
            System.err.println("Error: must specify a Project Id");
            System.exit(2);
        }
        // create a new simulator
        Simulator sim = new Simulator(blksz, pfsz, csz, pid, crp, start);
        sim.initialPreLoad();
        sim.simulate();
        sim.close();
        System.out.println(sim.getHitRate());
    }

    private void close() {
        DatabaseManager.close();
    }
}
