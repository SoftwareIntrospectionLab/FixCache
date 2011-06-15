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
    static final String findNumberOfAuthors = "select count(distinct(s.author_id)) " + 
        "from scmlog s, actions a, file_paths fp " + 
        "where s.id = a.commit_id " + 
        "and s.date between ? and ? " +
        "and a.file_id = fp.file_id " +
        "and fp.id = (select max(id) " + 
        "               from file_paths " + 
        "               where fp.file_path = ? " + 
        "               and commit_id <= a.commit_id) " + 
        "and s.repository_id = ?";
    static final String findNumberOfChanges = "select count(a.file_id) " + 
        "from scmlog s, actions a, file_paths fp " + 
        "where s.id = a.commit_id " + 
        "and a.file_id = fp.file_id " + 
        "and s.date between ? and ? " + 
        "and fp.id = (select max(id) " + 
        "             from file_paths " + 
        "             where file_path = ? " + 
        "             and commit_id <= a.commit_id) " + 
        "and s.repository_id = ?";
    static final String findNumberOfBugs = "select count(a.file_id) " + 
        "from scmlog s, actions a, file_paths fp " + 
        "where s.id = a.commit_id " + 
        "and a.file_id = fp.file_id " + 
        "and fp.id = (select max(id) " + 
        "             from file_paths " + 
        "             where fp.file_path = ? " + 
        "             and commit_id <= a.commit_id) " + 
        "and s.date between ? and ? " + 
        "and s.repository_id = ? " + 
        "and s.is_bug_fix = 1";
    static final String findLoc = "select c.loc " + 
        "from content c, file_paths fp " + 
        "where c.file_id = fp.file_id " + 
        "and fp.id = (select max(id) " + 
        "             from file_paths " + 
        "             where fp.file_path = ? " + 
        "             and fp.commit_id <= ?)";
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

    private static final String checkFileType = "select fp.file_id " + 
        "from file_types ft, file_paths fp " + 
        "where fp.file_path = ? " + 
        "and fp.file_id = ft.file_id " + 
        "and ft.type='code'";
    private static PreparedStatement checkFileTypeQuery;

    /**
     * Member fields
     **/

    // this enum tracks the reason for cache entry and is used by other classes
    public enum CacheReason {
        Preload, CoChange, NewEntity, ModifiedEntity, BugEntity
    }

    private final String fileName; // id of file
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

    public CacheItem(String fName, int cid, String cdate, CacheReason r, Cache p) {
        fileName = fName;
        reason = r;
        parent = p;
        update(cid, cdate, p.getStartDate(), r);
        assert (r != CacheReason.BugEntity || missCount != 0);
        assert (parent.neverInCache(fileName));
        assert (checkFileType(fileName));
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
        LOC = Math.max(LOC, findLoc(fileName, cid)); // max LOC
        int newnumber = findNumber(fileName, parent.repID, cdate, sdate,
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
    private static int findNumber(String fileName, int pid, String cdate,
            String sdate, Policy pol) {
        int ret = 0;
        switch (pol) {
        case BUGS:
            ret = findNumberOfBugs(fileName, pid, cdate, sdate);
            break;
        case CHANGES:
            ret = findNumberOfChanges(fileName, pid, cdate, sdate);
            break;
        case AUTHORS:
            ret = findNumberOfAuthors(fileName, pid, cdate, sdate);
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
    private static int findNumberOfAuthors(String fileName, int pid,
            String cdate, String start) {
        int ret = 0;
        try {
            // if (findNumberOfAuthorsQuery == null)
            findNumberOfAuthorsQuery = conn
                    .prepareStatement(findNumberOfAuthors);
            findNumberOfAuthorsQuery.setString(1, start);
            findNumberOfAuthorsQuery.setString(2, cdate);
            findNumberOfAuthorsQuery.setString(3, fileName); 
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
    private static int findNumberOfChanges(String fileName, int pid,
            String cdate, String start) {
        int ret = 0;
        try {
            // if (findNumberOfChangesQuery == null)
            findNumberOfChangesQuery = conn
                    .prepareStatement(findNumberOfChanges);
            findNumberOfChangesQuery.setString(1, start);
            findNumberOfChangesQuery.setString(2, cdate);
            findNumberOfChangesQuery.setString(3, fileName); 
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
    private static int findNumberOfBugs(String fileName, int pid, String cdate,
            String start) {
        int ret = 0;
        try {
            // if (findNumberOfBugsQuery == null)
            findNumberOfBugsQuery = conn.prepareStatement(findNumberOfBugs);

            findNumberOfBugsQuery.setString(1, fileName);
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
    protected static int findLoc(String fileName, int cid) {
        int ret = 0;
        try {
            // if (findLocQuery == null)
            findLocQuery = conn.prepareStatement(findLoc);
            findLocQuery.setString(1, fileName);
            findLocQuery.setInt(2, cid);
            ret = Database.getIntResult(findLocQuery);
            findLocQuery.close();
        } catch (SQLException e1) {
            e1.printStackTrace();
        }
        return ret;
    }

    /**
     * @return Returns the entityId.
     */
    public String getFileName() {
        return fileName;
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
    private boolean checkFileType(String fname) {
        boolean isCode = false;
        try {
            checkFileTypeQuery = conn.prepareStatement(checkFileType);
            checkFileTypeQuery.setString(1, fname);
            isCode = checkFileTypeQuery.executeQuery().next();
            checkFileTypeQuery.close();
        } catch (SQLException e1) {
            e1.printStackTrace();
            return false;
        }
        return isCode;
    }

}
