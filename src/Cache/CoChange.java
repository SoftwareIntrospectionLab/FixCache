package Cache;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    public static ArrayList<String> getCoChangeFileList(String fileName, String startDate,
            String commitDate, int blocksize, int pid) {
        CoChange co = new CoChange(fileName);
        return co.getCoChangeList(co.buildCoChangeMap(startDate, commitDate, pid), blocksize);
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
     * @param countTable
     * @param blocksize
     * @return
     */
    private ArrayList<String> getCoChangeList(CoChangeFileMap countTable,
            int blocksize) {
        return countTable.getTopFiles(blocksize);
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
        HashMap<String, Integer> map;

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
        ArrayList<String> getTopFiles(int blocksize) {
            ArrayList<Map.Entry<String, Integer>> list = 
                new ArrayList<Map.Entry<String, Integer>>(map.entrySet());
            ArrayList<String> topFiles = new ArrayList<String>();
            Collections.sort(list,
                    new Comparator<Map.Entry<String, Integer>>() {
                        public int compare(Map.Entry<String, Integer> o1,
                                Map.Entry<String, Integer> o2) {
                            return (o2.getValue().compareTo(o1.getValue())); // DESCENDING order
                        }
                    });
            // a block size b indicates that we load b-1 closest entities.
            for (int i = 0; i < blocksize - 1; i++)
            {
                if (list.size() > i) {
                    Map.Entry<String, Integer> curr = list.get(i);
                    topFiles.add(curr.getKey());
                }
            }
            return topFiles;
        }
    }
    
}
