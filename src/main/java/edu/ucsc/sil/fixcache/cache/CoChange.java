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
        + "where actions.file_id=? and actions.commit_id=scmlog.id and "
        + "date between ? and ? and scmlog.repository_id=?";
    // XXX: don't add deleted files to map
    static final String findCochangeFileId = "select files.id from files, actions, file_types "
        + "where files.id=actions.file_id and commit_id =? and "
        + "files.id=file_types.file_id and file_types.type='code'"; // XXX
    // and
    // actions.type
    // =
    // 'M'?
    private static PreparedStatement findCommitIdQuery;
    private static PreparedStatement findCochangeFileNameQuery;

    int fileId; // which file is cochange list for
    Cache cache; // cache contains LOC and such

    private CoChange(int fId, Cache cache) {
        this.fileId = fId;
        this.cache = cache;
    }

    public static ArrayList<Entry<Integer, Integer>> getCoChangeFileList(
            int fileId, String startDate, String commitDate, int pid,
            Cache cache) {
        CoChange co = new CoChange(fileId, cache);
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
                    final int coChangeFileId = files.getInt(1);
                    if (coChangeFileId != fileId) {
                        int loc = cache.getLoc(coChangeFileId);
                        if (loc < 0) // only query database if necessary
                            loc = CacheItem.findLoc(coChangeFileId, cid);
                        coChangeCounts.add(coChangeFileId, loc);
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
        findCochangeFileNameQuery = conn.prepareStatement(findCochangeFileId);
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
        commitIdQuery.setInt(1, fileId);
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
    public static class CoChangeFileMap {
        // TODO: make more efficient:
        // HashSet<Integer> newFiles;
        // int []fileIds;
        // int []counts;
        // int index;
        private HashMap<Integer, Integer> map;
        private HashMap<Integer, Integer> locmap;

        CoChangeFileMap() {
            map = new HashMap<Integer, Integer>();
            locmap = new HashMap<Integer, Integer>();
        }

        /**
         * if it is not there, create a new entry else ++count
         * 
         * @param f
         *            -- file name
         */
        void add(int fId, int loc) {
            if (map.containsKey(fId)) {
                assert (locmap.containsKey(fId));
                int count = map.get(fId);
                map.put(fId, count + 1);
                // if (locmap.get(fName) < loc) // XXX using the maximum LOC is
                // really slow
                // locmap.put(fName, loc);
            } else {
                assert (!locmap.containsKey(fId));
                map.put(fId, 1);
                locmap.put(fId, loc);
            }
        }

        ArrayList<Map.Entry<Integer, Integer>> getSortedFiles() {

            ArrayList<Map.Entry<Integer, Integer>> list = new ArrayList<Map.Entry<Integer, Integer>>(
                    map.entrySet());
            Collections.sort(list,
                    new Comparator<Map.Entry<Integer, Integer>>() {
                @Override
                public int compare(Map.Entry<Integer, Integer> f1,
                        Map.Entry<Integer, Integer> f2) {
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
            });

            return list;
        }

    }

    /**
     * For Debugging
     * 
     */
    public static ArrayList<Integer> getCoChangeFileList(int fileId,
            String start, String cdate, int blksz, int pid, Cache cache) {

        ArrayList<Integer> topFiles = new ArrayList<Integer>();
        ArrayList<Map.Entry<Integer, Integer>> entries = getCoChangeFileList(
                fileId, start, cdate, pid, cache);

        for (int i = 0; i < blksz - 1; i++) {
            if (entries.size() > i) {
                Map.Entry<Integer, Integer> curr = entries.get(i);
                topFiles.add(curr.getKey());
            }
        }
        return topFiles;

    }

}
