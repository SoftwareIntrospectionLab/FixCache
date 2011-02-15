package Cache;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import Database.DatabaseManager;

public class CoChange {

    /**
     * database setup
     */
    final static Connection conn = DatabaseManager.getConnection();
    static final String findCommitId = "select commit_id from actions, scmlog, files " +
            "where file_name=? and actions.file_id=files.id and actions.commit_id=scmlog.id and " +
            "date between ? and ? and scmlog.repository_id=?";
    static final String findCochangeFileName = "select file_name from files, actions, file_types " +
    		"where files.id=actions.file_id and commit_id =? and " +
    		"files.id=file_types.file_id and file_types.type='code'";
    private static PreparedStatement findCommitIdQuery;
    private static PreparedStatement findCochangeFileNameQuery;

    String fileName; // which file is cochange list for
    
    private CoChange(String fName) {
        this.fileName = fName;
    }

    public static ArrayList<Entry<String, Integer>> getCoChangeFileList(
            String fileName, String startDate,
            String commitDate, int pid) {
        CoChange co = new CoChange(fileName);
        return co.buildCoChangeMap(startDate, commitDate, pid).getSortedFiles();
    }

    /**
     *  build a table of files that are changed with fileID, before time committDate
     * @param commitDate
     * @return
     */
    // commitDate
    private CoChangeFileMap buildCoChangeMap(String startDate, String commitDate, int pid) {
        CoChangeFileMap coChangeCounts = new CoChangeFileMap();
        try {
            // get a list of all prior commits for fileID before commitID:
            final ResultSet allCommits = getCommits(startDate, commitDate, pid);
            while(allCommits.next()) {
                // for each commit in the list, get a list of all fileIDs involved in
                // that commit
                final ResultSet files = getFiles(allCommits.getInt(1));
                while (files.next()) {
                    final String coChangeFile = files.getString(1); 
                    if (!coChangeFile.equals(fileName)) {
                        coChangeCounts.add(coChangeFile);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return coChangeCounts;
    }

    /**
     * get a list of files involved in a particular commit
     * @param currCommit
     * @return list of all filenames changed in currCommit
     * @throws SQLException
     */
    private ResultSet getFiles(int currCommit) throws SQLException {
        if (findCochangeFileNameQuery == null)
            findCochangeFileNameQuery = conn
                    .prepareStatement(findCochangeFileName);
        findCochangeFileNameQuery.setInt(1, currCommit);
        return findCochangeFileNameQuery.executeQuery();
    }

    /**
     * get all commits involving a particular file
     * @param startDate -- initial date
     * @param commitDate -- end date
     * @param pid -- project id
     * @return list of commits
     * @throws SQLException
     */
    private ResultSet getCommits(String startDate, String commitDate,
            int pid) throws SQLException {
            if (findCommitIdQuery == null) 
                findCommitIdQuery = conn.prepareStatement(findCommitId);
            final PreparedStatement commitIdQuery = findCommitIdQuery;
            commitIdQuery.setString(1, fileName); 
            commitIdQuery.setString(2, startDate);
            commitIdQuery.setString(3, commitDate);
            commitIdQuery.setInt(4, pid);
            return commitIdQuery.executeQuery();
    }


    /**
     * 
     * a map from filenames to co-change counts
     * 
     *
     */
    public class CoChangeFileMap {
        // TODO: make more efficient:
        // HashSet<Integer> newFiles;
        // int []fileIds;
        // int []counts;
        // int index;
        private HashMap<String, Integer> map;

        CoChangeFileMap() {
            map = new HashMap<String, Integer>();
        }

        /**
         * if it is not there, create a new entry else ++count
         * @param f -- file name 
         */
        void add(String fName) {
            if (map.containsKey(fName)) {
                int count = map.get(fName);
                map.put(fName, count + 1);
            } else
                map.put(fName, 1);
        }

        // TODO: when two files have the same cochange count, use loc to break ties
        ArrayList<Map.Entry<String, Integer>> getSortedFiles() {

            ArrayList<Map.Entry<String, Integer>> list = 
                new ArrayList<Map.Entry<String, Integer>>(map.entrySet());
            Collections.sort(list,
                    new Comparator<Map.Entry<String, Integer>>() {
                        public int compare(Map.Entry<String, Integer> o1,
                                Map.Entry<String, Integer> o2) {
                            return (o2.getValue().compareTo(o1.getValue())); // DESCENDING order
                        }
                    });
            
            return list;
        }

    }

    /**
     * For Debugging
     * 
     */


    private static ArrayList<String> getList(int blksize, 
            ArrayList<Map.Entry<String, Integer>> entries){

        ArrayList<String> topFiles = new ArrayList<String>();

        for (int i = 0; i < blksize - 1; i++)
        {
            if (entries.size() > i) {
                Map.Entry<String, Integer> curr = entries.get(i);
                topFiles.add(curr.getKey());
            }
        }
        return topFiles;
    }

    public static ArrayList<String> getCoChangeFileList(String fname,
            String start, String cdate, int blksz, int pid) {
        return getList(blksz, getCoChangeFileList(fname, start, cdate, pid));
    }

    
}




