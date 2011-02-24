package Cache;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import Database.DatabaseManager;
import Util.CmdLineParser;

public class InputManager {
	
	static Connection conn = DatabaseManager.getConnection();

    static final String findFileCount = "select count(distinct(file_name)) " +
    "from files, file_types "
    + "where files.id = file_types.file_id " +
    "and type = 'code' and repository_id=?";
    static final String findPid = "select id from repositories where id=?";      

    boolean tune;
    int blksize;
    int prefetchsize;
    int cachesize;
    int pid; // project id, repository id
    CacheReplacement.Policy crp;
    String start;
    String end;
    boolean saveToFile;
    boolean monthly;

    /**
     * Special constructor for tuning and debugging. 
     * @param blksz -- blocksize
     * @param pfsz -- prefetchsize
     * @param csz -- cachesize
     * @param projid -- project id
     * @param cr -- cache replacement policy
     * @param conn -- database connection (to check params are valid)
     */
    public InputManager(int blksz, int pfsz, int csz, int projid, 
            CacheReplacement.Policy cr){
        this.blksize = blksz;
        this.prefetchsize = pfsz;
        this.cachesize = csz;
        this.pid = projid;
        this.crp = cr;
        this.start = null;
        this.end = null;
        this.saveToFile = false;
        this.monthly = false;
        this.checkParameter();
    }        

    /**
     * For debugging; used only in SimulatorTest
     * @param start
     */
    public void setStartDate(String start) {
        this.start = start;
    }

    /**
     * Constructs an input file out of parameter string
     * @param args -- input arguement string
     * @param conn -- database connection; used to validate some params
     */
    public InputManager(String[] args) {

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
        CmdLineParser.Option month_opt = parser.addBooleanOption('m',"multiple");
        CmdLineParser.Option tune_opt = parser.addBooleanOption('u', "tune");

        // first, parse the argument array
        try {
            parser.parse(args);
        } catch (CmdLineParser.OptionException e) {
            System.err.println(e.getMessage());
            printUsage();
            System.exit(2);
        }

        // get boolean inputs
        this.saveToFile = (Boolean) parser.getOptionValue(save_opt, false);
        this.tune = (Boolean)parser.getOptionValue(tune_opt, false);
        this.monthly = (Boolean)parser.getOptionValue(month_opt, false);

        // get the cache replacement policy input
        String crp_string = (String) parser.getOptionValue(crp_opt,
                CacheReplacement.REPDEFAULT.toString());
        try {
            this.crp = CacheReplacement.Policy.valueOf(crp_string);
        } catch (Exception e) {
            System.err.println(e.getMessage());
            System.err.println("Must specify a valid cache replacement policy");
            printUsage();
            this.crp = CacheReplacement.REPDEFAULT;
        }

        // get the project id
        Integer projid = (Integer) parser.getOptionValue(pid_opt);
        if (projid == null) {
            System.err.println("Error: must specify a Project Id");
            printUsage();
            System.exit(2);
        }
        this.pid = projid;

        // get the blocksize, cachesize, and prefetchsize
        this.blksize = (Integer) parser.getOptionValue(blksz_opt, -1);
        this.cachesize = (Integer) parser.getOptionValue(csz_opt, -1);
        this.prefetchsize = (Integer) parser.getOptionValue(pfsz_opt, -1);

        // get the start and end dates
        this.start = (String) parser.getOptionValue(sd_opt, null);
        this.end = (String) parser.getOptionValue(ed_opt, null);

        // check invariants, and replace default parameters
        this.checkParameter();      
    }


    /**
     * checks to make sure all the fields are well-formed
     * @param conn -- database connection
     */
    private void checkParameter() {
        // if start and end are specified, start should be first
        if (start != null && end != null) {
            if (start.compareTo(end) > 0) {
                System.err
                .println("Error:Start date must be earlier than end date");
                printUsage();
                System.exit(2);
            }
        }
        // make sure the specified project id exists
        try {
            PreparedStatement findPidQuery = conn.prepareStatement(findPid);
            findPidQuery.setInt(1, pid);
            if (Util.Database.getIntResult(findPidQuery) == -1) {
                System.out.println("There is no project whose id is " + pid);
                System.exit(2);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // set defaults for unspecified parameters
        int onepercent = getPercentOfFiles(pid);
        if (blksize == -1)
            blksize = onepercent*5;
        if (cachesize == -1)
            cachesize = onepercent*10; 
        if (prefetchsize == -1)
            prefetchsize = onepercent;

        // set defaults for start and end dates
        start = findFirstDate(start, pid);
        end = findLastDate(end, pid);
    }

    /**
     * Private Static methods
     */

    /**
     * Returns one percent of files
     * @param pid -- project id
     * @param conn -- database connection
     * @return one percent of the files
     */
    private static int getPercentOfFiles(int pid) {
        int ret =  (int) Math.round(getFileCount(pid)*0.01);
        if (ret == 0)
            return 1;
        else
            return ret;
    }

    /**
     * get an estimate for the number of files; used to set defaults
     * @param projid -- project id
     * @param conn -- database connection
     * @return number of files
     */
    private static int getFileCount(int projid) {
        int ret = 0;
        try {
            PreparedStatement findFileCountQuery = conn.prepareStatement(findFileCount);
            findFileCountQuery.setInt(1, projid);
            ret = Util.Database.getIntResult(findFileCountQuery);
        } catch (SQLException e1) {
            e1.printStackTrace();
        }
        return ret;
    }        

    /**
     * Finds the first date after the startDate with repository entries. Only
     * called once per simulation.
     * 
     * @return The date for the prefetch.
     */
    private static String findFirstDate(String start, int pid) {
        String findFirstDate = "";
        final PreparedStatement findFirstDateQuery;
        String firstDate = "";
        try {
            if (start == null) {
                findFirstDate = "select min(date) from scmlog where repository_id=?";
                findFirstDateQuery = conn.prepareStatement(findFirstDate);
                findFirstDateQuery.setInt(1, pid);
            } else {
                findFirstDate = "select min(date) from scmlog where repository_id=? and date >=?";
                findFirstDateQuery = conn.prepareStatement(findFirstDate);
                findFirstDateQuery.setInt(1, pid);
                findFirstDateQuery.setString(2, start);
            }
            firstDate = Util.Database.getStringResult(findFirstDateQuery);
            if (firstDate == null) {
                System.out.println("Can not find any commit after "
                        + start);
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
     * @return The end date for the the simulator.
     */
    private static String findLastDate(String end, int pid) {
        String findLastDate = null;
        final PreparedStatement findLastDateQuery;
        String lastDate = null;
        try {
            if (end == null) {
                findLastDate = "select max(date) from scmlog where repository_id=?";
                findLastDateQuery = conn.prepareStatement(findLastDate);
                findLastDateQuery.setInt(1, pid);
            } else {
                findLastDate = "select max(date) from scmlog where repository_id=? and date <=?";
                findLastDateQuery = conn.prepareStatement(findLastDate);
                findLastDateQuery.setInt(1, pid);
                findLastDateQuery.setString(2, end);
            }
            lastDate = Util.Database.getStringResult(findLastDateQuery);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        if (lastDate == null) {
            System.out.println("Can not find any commit before "
                    + end);
            System.exit(2);
        }
        return lastDate;
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
}
