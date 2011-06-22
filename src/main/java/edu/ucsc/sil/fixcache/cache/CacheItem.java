package edu.ucsc.sil.fixcache.cache;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import edu.ucsc.sil.fixcache.cache.CacheReplacement.Policy;
import edu.ucsc.sil.fixcache.database.DatabaseManager;

import edu.ucsc.sil.fixcache.util.Dates;
import edu.ucsc.sil.fixcache.util.Database;

public class CacheItem {
    /**
     * Database: Setting up the SQL statement strings used in this class. These
     * are static PreparedStatements that can be shared across all cache items.
     */

    static Connection conn = DatabaseManager.getConnection();
    static final String findNumberOfAuthors = "select count(distinct(scmlog.author_id)) "
            + "from scmlog, actions "
            + "where scmlog.id=actions.commit_id and actions.file_id=? "
            + "and date between ? and ? and scmlog.repository_id=?";
    static final String findNumberOfChanges = "select count(actions.file_id) "
            + "from scmlog, actions where scmlog.id=actions.commit_id "
            + "and actions.file_id=? and date between ? and ?"
            + "and scmlog.repository_id=?";
    static final String findNumberOfBugs = "select count(actions.file_id) "
            + "from scmlog, actions where scmlog.id = actions.commit_id "
            + "and actions.file_id=? and date between ? and ? "
            + "and scmlog.repository_id=? and is_bug_fix=1";
    static final String findLoc = "select loc from content "
            + "where file_id = ? and commit_id = ?";
    private static PreparedStatement findNumberOfAuthorsQuery;
    private static PreparedStatement findNumberOfChangesQuery;
    private static PreparedStatement findNumberOfBugsQuery;
    private static PreparedStatement findLocQuery;

    /**
     * Fields for Debugging
     */

    private static final String checkRepo = "select id from scmlog where "
            + "repository_id = ? and id = ?";
    private static PreparedStatement checkRepoQuery;

    private static final String checkFileType = "select ft.file_id from "
            + "file_types ft where ft.file_id = ? and ft.type='code' ";
    private static PreparedStatement checkFileTypeQuery;

    /**
     * Member fields
     **/

    // this enum tracks the reason for cache entry and is used by other classes
    public enum CacheReason {
        Preload, CoChange, NewEntity, ModifiedEntity, BugEntity
    }

    private final int fileId; // id of file
    private String filePath = null;
    private int loadDate; // changed on cache hit
    private int LOC; // changed on cache hit; max LOC
    private int number; // represents either the number of bugs, changes, or
                        // authors
    private int loadCount = 0; // count how many time a file is put into cache
    private int loadDuration = 0; // represents how long in repo time a file
                                  // stays in cache
    private String timeAdded; // represents repo time when a file is added to
                              // cache
    private final Cache parent;
    private boolean inCache = false; // stores whether the cacheitem is in the
                                     // cache
    private int hitCount = 0;
    private int missCount = 0;

    private CacheReason reason;

    /**
     * Methods
     */

    public CacheItem(int fId, int cid, String cdate, CacheReason r, Cache p) {
        fileId = fId;
        reason = r;
        parent = p;
        update(cid, cdate, p.getStartDate(), r);
        assert (r != CacheReason.BugEntity || missCount != 0);
        assert (fileId > 0);
        assert (parent.neverInCache(fileId));
        assert (checkFileType(fileId));
        assert (checkRepo(p.repID, cid));
    }

    /**
     * called every time entityId moved into the cache, or cache hit occurs
     * updates the time of cache load, loc, and number
     * 
     * @param cid
     *            -- commit id
     * @param cdate
     *            -- commit date
     * @param sdate
     *            -- starting date
     */
    public void update(int cid, String cdate, String sdate, CacheReason r) {
        // update the load count each time an entry is added to the cache
        if (!inCache) {
            inCache = true;
            loadCount++;
            timeAdded = cdate; // for tracking the duration
            if (r == CacheReason.BugEntity)
                missCount++;
        } else { // is in cache
            if (r == CacheReason.BugEntity)
                hitCount++;
        }
        loadDate = parent.getTime(cid); 
        LOC = Math.max(LOC, findLoc(fileId, cid)); // max LOC
        int newnumber = findNumber(fileId, parent.repID, cdate, sdate,
                parent.getPolicy());
        number = newnumber < 0? number: newnumber;
    }

    public boolean isInCache() {
        return inCache;
    }

    public int removeFromCache(String cdate) {
        loadDuration += Dates.getMinuteDuration(timeAdded, cdate);
        assert (inCache);
        inCache = false;
        return loadDuration;
    }

    // XXX: Maybe get rid of pid here once we switch back to eids.
    /**
     * 
     * Finds a number used by the cache replacement policy to decide what to 
     * kick out of the cache. Could be authors, changes, bugs, etc.
     * 
     * @param eid
     *            -- entity id
     * @param pid
     *            -- repository id
     * @param cdate
     *            -- the commit date
     * @param start
     *            -- the starting date for repository access
     * @return the number of bug fixes for file eid in repository pid between
     *         cdate and start
     */
    private static int findNumber(int fileId, int pid, String cdate,
            String sdate, Policy pol) {
        int ret = 0;
        switch (pol) {
        case BUGS:
            ret = findNumberOfBugs(fileId, pid, cdate, sdate);
            break;
        case CHANGES:
            ret = findNumberOfChanges(fileId, pid, cdate, sdate);
            break;
        case AUTHORS:
            ret = findNumberOfAuthors(fileId, pid, cdate, sdate);
            break;
        case LRU: // do nothing
        }
        assert(ret >= 0); // XXX may not hold if updated with incorrect commit id
        return ret;
    }

    /**
     * 
     * @param eid
     *            -- entity id
     * @param pid
     *            -- repository id
     * @param cdate
     *            -- the commit date
     * @param start
     *            -- the starting date for repository access
     * @return the number of distinct authors for file eid in repository pid
     *         DatabaseTest dbTest = new DatabaseTest(); dbTest. between cdate
     *         and start
     */
    private static int findNumberOfAuthors(int fileId, int pid,
            String cdate, String start) {
        int ret = 0;
        try {
            // if (findNumberOfAuthorsQuery == null)
            findNumberOfAuthorsQuery = conn
                    .prepareStatement(findNumberOfAuthors);
            findNumberOfAuthorsQuery.setInt(1, fileId); 
            findNumberOfAuthorsQuery.setString(2, start);
            findNumberOfAuthorsQuery.setString(3, cdate);
            findNumberOfAuthorsQuery.setInt(4, pid);
            ret = Database.getIntResult(findNumberOfAuthorsQuery);
            findNumberOfAuthorsQuery.close();
        } catch (SQLException e1) {
            e1.printStackTrace();
        }
        return ret;
    }

    /**
     * 
     * @param eid
     *            -- entity id
     * @param pid
     *            -- repository id
     * @param cdate
     *            -- the commit date
     * @param startreturn 
     *            -- the starting date for repository access
     * @return the number of commits for file eid in repository pid between
     *         cdate and start
     */
    private static int findNumberOfChanges(int fileId, int pid,
            String cdate, String start) {
        int ret = 0;
        try {
            // if (findNumberOfChangesQuery == null)
            findNumberOfChangesQuery = conn
                    .prepareStatement(findNumberOfChanges);
            findNumberOfChangesQuery.setInt(1, fileId);
            findNumberOfChangesQuery.setString(2, start);
            findNumberOfChangesQuery.setString(3, cdate);
            findNumberOfChangesQuery.setInt(4, pid);
            ret = Database.getIntResult(findNumberOfChangesQuery);
            findNumberOfChangesQuery.close();
        } catch (SQLException e1) {
            e1.printStackTrace();
        }
        return ret;
    }

    /**
     * 
     * @param eid
     *            -- entity id
     * @param pid
     *            -- repository id
     * @param cdate
     *            -- the commit date
     * @param start
     *            -- the starting date for repository access
     * @return the number of bug fixes for file eid in repository pid between
     *         cdate and start
     */
    private static int findNumberOfBugs(int fileId, int pid, String cdate,
            String start) {
        int ret = 0;
        try {
            // if (findNumberOfBugsQuery == null)
            findNumberOfBugsQuery = conn.prepareStatement(findNumberOfBugs);

            findNumberOfBugsQuery.setInt(1, fileId);
            findNumberOfBugsQuery.setString(2, start);
            findNumberOfBugsQuery.setString(3, cdate);
            findNumberOfBugsQuery.setInt(4, pid);
            ret = Database.getIntResult(findNumberOfBugsQuery);
            findNumberOfBugsQuery.close();
        } catch (SQLException e1) {
            e1.printStackTrace();
        }
        return ret;
    }

    /**
     * This method is also used in CoChange.java
     * 
     * @param eid
     *            -- entity id
     * @param cid
     *            -- commit id
     * @return the lines of code for eid at cid
     */
    protected static int findLoc(int fileId, int cid) {
        int ret = 0;
        try {
            // if (findLocQuery == null)
            findLocQuery = conn.prepareStatement(findLoc);
            findLocQuery.setInt(1, fileId);
            findLocQuery.setInt(2, cid);
            ret = Database.getIntResult(findLocQuery);
            findLocQuery.close();
        } catch (SQLException e1) {
            e1.printStackTrace();
        }
        return ret;
    }

    /**
     * @return Returns the file path. The first time this runs, its expensive.
     */
    public String getFilePath() {
        if (filePath == null) {   
            // Just in case we don't have a file path, return something reasonably
            // useful
            filePath = Integer.toString(getFileId());
            
            final String findFilePath = "select fp.file_path " + 
                "from file_paths fp " + 
                "where file_id = ? " + 
                "order by id desc " + 
                "limit 1";
            
            try {
                final PreparedStatement findFilePathQuery = 
                    conn.prepareStatement(findFilePath);
                findFilePathQuery.setInt(1, getFileId());
                filePath = Database.getStringResult(findFilePathQuery);
            } catch (SQLException e1) {
                e1.printStackTrace();
            }
        }
        
        return filePath;
    }
    
    public int getFileId() {
      return fileId;
    }

    /**
     * @return Returns the cachedDate.
     */
    public int getLoadedDate() {
        return loadDate;
    }

    /**
     * @return Returns the LOC.
     */
    public int getLOC() {
        return LOC;
    }

    /**
     * @return Returns the numberOfChanges.
     */
    public int getNumberOfChanges() {
        assert (parent.getPolicy() == CacheReplacement.Policy.CHANGES);
        return number;
    }

    /**
     * @return Returns the numberOfBugs.
     */
    public int getNumberOfBugs() {
        assert (parent.getPolicy() == CacheReplacement.Policy.BUGS);
        return number;
    }

    /**
     * @return Returns the numberOfAuthors
     */
    public int getNumberOfAuthors() {
        assert (parent.getPolicy() == CacheReplacement.Policy.AUTHORS);
        return number;
    }

    /**
     * @return Returns the loadCount
     */
    public int getLoadCount() {
        return loadCount;
    }

    /**
     * @return Returns the hitcount
     */
    public int getHitCount() {
        return hitCount;
    }

    /**
     * 
     * @return the reason this was added to cache
     */
    public CacheReason getReason() {
        return reason;
    }

    /**
     * 
     * @return the amount of time this cache item was in cache
     */
    public int getDuration() {
        if (inCache)
            loadDuration += Dates.getMinuteDuration(timeAdded,
                    parent.endDate);
        return loadDuration;
    }

    /**
     * 
     * @return number of times files was not in cache and had a bug fix
     */
    public int getMissCount() {
        return missCount;
    }

    /**
     * Methods for Debugging
     */

    /**
     * used only for the DBUnit tests
     * 
     * @return number
     */
    protected int getNumber() {
        return number;
    }

    /**
     * checks that the repo in pid matches that associated with commit cid
     * 
     * @param pid
     *            -- project id
     * @param cid
     *            -- commit id
     * @return true if they match, false otherwise
     */
    private boolean checkRepo(int pid, int cid) {
        boolean isInRepo = false;
        try {
            checkRepoQuery = conn.prepareStatement(checkRepo);
            checkRepoQuery.setInt(1, pid);
            checkRepoQuery.setInt(2, cid);
            isInRepo = checkRepoQuery.executeQuery().next();
            checkRepoQuery.close();
        } catch (SQLException e1) {
            e1.printStackTrace();
            return false;
        }
        return isInRepo;
    }

    /**
     * checks that the filename is a code file
     * 
     * @param fname
     *            -- filename
     * @return true if it is a code file, false otherwise
     */
    private boolean checkFileType(int fileId) {
        boolean isCode = false;
        try {
            checkFileTypeQuery = conn.prepareStatement(checkFileType);
            checkFileTypeQuery.setInt(1, fileId);
            isCode = checkFileTypeQuery.executeQuery().next();
            checkFileTypeQuery.close();
        } catch (SQLException e1) {
            e1.printStackTrace();
            return false;
        }
        return isCode;
    }

}
