package edu.ucsc.sil.fixcache.cache;

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

import edu.ucsc.sil.fixcache.database.DatabaseManager;

// Spacial locality (Sec 2.2, #5)
public class CoChange {

    /**
     * database setup
     */
    final static Connection conn = DatabaseManager.getConnection();
    static final String findCommitId = "select commit_id from actions, scmlog, files "
        + "where file_name=? and actions.file_id=files.id and actions.commit_id=scmlog.id and "
        + "date between ? and ? and scmlog.repository_id=?";
    // XXX: don't add deleted files to map
    static final String findCochangeFileName = "select file_name from files, actions, file_types "
        + "where files.id=actions.file_id and commit_id =? and "
        + "files.id=file_types.file_id and file_types.type='code'"; // XXX
    // and
    // actions.type
    // =
    // 'M'?
    private static PreparedStatement findCommitIdQuery;
    private static PreparedStatement findCochangeFileNameQuery;

    String fileName; // which file is cochange list for
    Cache cache; // cache contains LOC and such

    private CoChange(String fName, Cache cache) {
        this.fileName = fName;
        this.cache = cache;
    }

    public static ArrayList<Entry<String, Integer>> getCoChangeFileList(
            String fileName, String startDate, String commitDate, int pid,
            Cache cache) {
        CoChange co = new CoChange(fileName, cache);
        return co.buildCoChangeMap(startDate, commitDate, pid).getSortedFiles();
    }

    /**
     * build a table of files that are changed with fileID, before time
     * committDate
     * 
     * @param commitDate
     * @return
     */
    private CoChangeFileMap buildCoChangeMap(String startDate,
            String commitDate, int pid) {
        CoChangeFileMap coChangeCounts = new CoChangeFileMap();
        try {
            // get a list of all prior commits for fileID before commitID:
            final ResultSet allCommits = getCommits(startDate, commitDate, pid);
            while (allCommits.next()) {
                // for each commit in the list, get a list of all fileIDs
                // involved in
                // that commit
                final int cid = allCommits.getInt(1);
                final ResultSet files = getFiles(cid);
                while (files.next()) {
                    final String coChangeFile = files.getString(1);
                    if (!coChangeFile.equals(fileName)) {
                        int loc = cache.getLoc(coChangeFile);
                        if (loc < 0) // only query database if necessary
                            loc = CacheItem.findLoc(coChangeFile, cid);
                        coChangeCounts.add(coChangeFile, loc);
                    }
                }
            }
            if(findCommitIdQuery!=null)
                findCommitIdQuery.close();
            if(findCochangeFileNameQuery!=null)
                findCochangeFileNameQuery.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return coChangeCounts;
    }

    /**
     * get a list of files involved in a particular commit
     * 
     * @param currCommit
     * @return list of all filenames changed in currCommit
     * @throws SQLException
     */
    private ResultSet getFiles(int currCommit) throws SQLException {
        findCochangeFileNameQuery = conn.prepareStatement(findCochangeFileName);
        findCochangeFileNameQuery.setInt(1, currCommit);
        return findCochangeFileNameQuery.executeQuery();
    }

    /**
     * get all commits involving a particular file
     * 
     * @param startDate
     *            -- initial date
     * @param commitDate
     *            -- end date
     * @param pid
     *            -- project id
     * @return list of commits
     * @throws SQLException
     */
    private ResultSet getCommits(String startDate, String commitDate, int pid)
    throws SQLException {
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
    // XXX maybe add to CacheItem along with date of current co-change map
    // then, only need to add to CoChangeMap from prev. stopping point
    // this would increase space usage, but make co-change calculations faster
    public class CoChangeFileMap {
        // TODO: make more efficient:
        // HashSet<Integer> newFiles;
        // int []fileIds;
        // int []counts;
        // int index;
        private HashMap<String, Integer> map;
        private HashMap<String, Integer> locmap;

        CoChangeFileMap() {
            map = new HashMap<String, Integer>();
            locmap = new HashMap<String, Integer>();
        }

        /**
         * if it is not there, create a new entry else ++count
         * 
         * @param f
         *            -- file name
         */
        void add(String fName, int loc) {
            if (map.containsKey(fName)) {
                assert (locmap.containsKey(fName));
                int count = map.get(fName);
                map.put(fName, count + 1);
                // if (locmap.get(fName) < loc) // XXX using the maximum LOC is
                // really slow
                // locmap.put(fName, loc);
            } else {
                assert (!locmap.containsKey(fName));
                map.put(fName, 1);
                locmap.put(fName, loc);
            }
        }

        ArrayList<Map.Entry<String, Integer>> getSortedFiles() {

            ArrayList<Map.Entry<String, Integer>> list = new ArrayList<Map.Entry<String, Integer>>(
                    map.entrySet());
            Collections.sort(list,
                    new Comparator<Map.Entry<String, Integer>>() {
                public int compare(Map.Entry<String, Integer> f1,
                        Map.Entry<String, Integer> f2) {
                    // DESCENDING order; return <0 if o2 is smaller
                    int comparison = (f2.getValue().compareTo(f1
                            .getValue()));
                    if (comparison == 0) { // use LOC to break ties
                        final int f2loc = locmap.get(f2.getKey());
                        final int f1loc = locmap.get(f1.getKey());
                        return (f2loc - f1loc);
                    } else
                        return comparison;
                }
                // TODO Auto-generated catch block
            });

            return list;
        }

    }

    /**
     * For Debugging
     * 
     */
    public static ArrayList<String> getCoChangeFileList(String fname,
            String start, String cdate, int blksz, int pid, Cache cache) {

        ArrayList<String> topFiles = new ArrayList<String>();
        ArrayList<Map.Entry<String, Integer>> entries = getCoChangeFileList(
                fname, start, cdate, pid, cache);

        for (int i = 0; i < blksz - 1; i++) {
            if (entries.size() > i) {
                Map.Entry<String, Integer> curr = entries.get(i);
                topFiles.add(curr.getKey());
            }
        }
        return topFiles;

    }

}
