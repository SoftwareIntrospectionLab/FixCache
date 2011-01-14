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
    static final String findCommitId = "SELECT commit_id from actions, scmlog " +
            "where file_id=? and actions.commit_id=scmlog.id and date between ? and ?";
    static final String findCochangeFileId = "SELECT file_id from actions where commit_id =?";
    private static PreparedStatement findCommitIdQuery;
    private static PreparedStatement findCochangeFileIdQuery;

    int fileID; // which 
    
    private CoChange(int fileID) {
        this.fileID = fileID;
    }

    public PreparedStatement getCommitIdStatement() {
        if (findCommitIdQuery == null) {
            try {
                findCommitIdQuery = conn.prepareStatement(findCommitId);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return findCommitIdQuery;

    }

    public PreparedStatement getCochangeFileIdStatement() {
        if (findCochangeFileIdQuery == null)
            try {
                findCochangeFileIdQuery = conn
                        .prepareStatement(findCochangeFileId);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        return findCochangeFileIdQuery;
    }

    public static ArrayList<Integer> getCoChangeFileList(int fileid, String startDate,
            String commitDate, int blocksize) {
        CoChange co = new CoChange(fileid);
        return co.getCoChangeList(co.buildCoChangeMap(startDate, commitDate), blocksize);

    }

    /**
     *  build a table of files that are changed with fileID, before time committDate
     * @param commitDate
     * @return
     */
    // commitDate
    private CoChangeFileMap buildCoChangeMap(String startDate, String commitDate) {
        CoChangeFileMap coChangeCounts = new CoChangeFileMap();

        // get a list of all prior commits for fileID before commitID:
        final PreparedStatement commitIdQuery = getCommitIdStatement();
        ArrayList<Integer> commitList = new ArrayList<Integer>();

        try {
            commitIdQuery.setInt(1, fileID);
            commitIdQuery.setString(2, startDate);
            commitIdQuery.setString(3, commitDate);
            commitList = Util.Database.getIntArrayResult(commitIdQuery);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // for each commit in the list, get a list of all fileIDs involved in
        // that commit
        int coChangeCommitID;
        ResultSet r2;
        int coChangeFile;
        final PreparedStatement cochangeFileIdQuery = getCochangeFileIdStatement();
        for (int i = 0; i < commitList.size(); i++) {
            coChangeCommitID = commitList.get(i);
            try {
                cochangeFileIdQuery.setInt(1, coChangeCommitID);
                r2 = cochangeFileIdQuery.executeQuery();
                while (r2.next()) {
                    coChangeFile = r2.getInt(1);
                    if (coChangeFile != fileID) {
                        // coChangeList.add(r2.getInt(1));
                        coChangeCounts.add(coChangeFile);
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }

        }
        // for each such file f:
        // int f;
        // for(int j = 0; j < commitList.size(); j++)//I think here commitList
        // should be coChangeList
        // {
        // f = (Integer)commitList.get(j);
        // coChangeCounts.add(f);
        // }

        return coChangeCounts;

    }

    // get BLOCKSIZE-1
    private ArrayList<Integer> getCoChangeList(CoChangeFileMap countTable,
            int blocksize) {
        return countTable.getTopFiles(blocksize);
    }


    public class CoChangeFileMap {
        // TODO: make more efficient:
        // HashSet<Integer> newFiles;
        // int []fileIds;
        // int []counts;
        // int index;
        HashMap<Integer, Integer> map;

        CoChangeFileMap() {
            map = new HashMap<Integer, Integer>();
        }

        /**
         * if it is not there, create a new entry else ++count
         * @param f -- file id 
         */
        void add(int f) {
            if (map.containsKey(f)) {
                int count = map.get(f);
                map.put(f, count + 1);
            } else
                map.put(f, 1);
        }

        // TODO: when two files have the same cochange count, use loc to break ties
        ArrayList<Integer> getTopFiles(int blocksize) {
            ArrayList<Map.Entry<Integer, Integer>> list = 
                new ArrayList<Map.Entry<Integer, Integer>>(map.entrySet());
            ArrayList<Integer> topFiles = new ArrayList<Integer>();
            Collections.sort(list,
                    new Comparator<Map.Entry<Integer, Integer>>() {
                        public int compare(Map.Entry<Integer, Integer> o1,
                                Map.Entry<Integer, Integer> o2) {
                            return (o2.getValue().compareTo(o1.getValue())); // DESCENDING order
                        }
                    });
            // a block size b indicates that we load b-1 closest entities.
            for (int i = 0; i < blocksize - 1; i++)
            {
                if (list.size() > i) {
                    Map.Entry<Integer, Integer> curr = list.get(i);
                    topFiles.add(curr.getKey());
                }
            }
            return topFiles;
        }
    }
    
    /**
     * For testing.
     * @param args
     */
    public static void main(String args[]) {
        CoChange coChange = new CoChange(3679);
        CoChangeFileMap countTable = coChange.buildCoChangeMap("","");
        List<Integer> coChangeList = countTable.getTopFiles(100);
        for (int i = 0; i < coChangeList.size(); i++) {
            System.out.println(coChangeList.get(i));
        }
    }

}
