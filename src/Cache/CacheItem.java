package Cache;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import Cache.CacheReplacement.Policy;
import Database.DatabaseManager;

public class CacheItem {
    /**
     *  Database: Setting up the SQL statement strings used in this class.
     *  These are static PreparedStatements that can be shared across all cache items.
     */
    
    static Connection conn = DatabaseManager.getConnection();
    static final String findNumberOfAuthors = 
        "select count(id) from people " +
        "where id in( " +
            "select author_id from scmlog, actions " +
            "where scmlog.id = actions.commit_id " +
                "and date <=? and date >= ? and file_id = ?)";
    static final String findNumberOfChanges = 
        "select count(actions.id) " +
        "from actions, scmlog " +
        "where actions.commit_id = scmlog.id " +
        "and date <=? and date >=? and file_id=?";
    static final String findNumberOfBugs = 
        "select count(commit_id) from actions " +
        "where file_id=? and commit_id in " +
            "(select id from scmlog " +
            "where is_bug_fix=1 and date <=? and date >=?)";
    static final String findLoc = 
        "select loc from content_loc where file_id=? and commit_id =?";
    private static PreparedStatement findNumberOfAuthorsQuery;
    private static PreparedStatement findNumberOfChangesQuery;
    private static PreparedStatement findNumberOfBugsQuery;
    private static PreparedStatement findLocQuery;

    /**
     * Member fields
     */
    
    // this enum tracks reason for cache entry and is used by other classes
    public enum CacheReason {
        Prefetch, CoChange, NewEntity, ModifiedEntity, BugEntity
    }

    private final int entityId; // id of file
    private int loadDate; // changed on cache hit
    private int LOC; // changed on cache hit
    private int number; // represents either the number of bugs, changes, or authors
    private int loadCount = 0; //count how many time a file is put into cache 
    private final Cache parent;
    private boolean inCache = false; // stores whether the cacheitem is in the cache

    @SuppressWarnings("unused") // may be useful output
    private CacheReason reason; 

    /**
     * Methods
     */
    
    public CacheItem(int eid, int cid, String cdate, CacheReason r, Cache p) {
        entityId = eid;
        reason = r;
        parent = p;
        update(cid, cdate, p.getStartDate());
    }
    
    
    /**
     * called every time entityId moved into the cache, or cache hit occurs
     * updates the time of cache load, loc, and number
     *
     * @param cid -- commit id
     * @param cdate -- commit date
     * @param sdate -- starting date
     */
    public void update(int cid, String cdate, String sdate) {
        // update the load count each time an entry is added to the cache
        if (!inCache){
            inCache = true;
            loadCount++;
        }
        loadDate = parent.getTime(); 
        LOC = findLoc(entityId, cid);
        number = findNumber(entityId, parent.repID, cdate, sdate, parent.getPolicy());
    }
    
    public boolean isInCache(){
        return inCache;
    }
    
    public void removeFromCache(){
        assert(inCache);
        inCache = false;
    }

    // XXX: Do we need pid? or is eid unique enough for the called methods?
    /**
     * 
     * @param eid -- entity id
     * @param pid -- repository id
     * @param cdate -- the commit date
     * @param start -- the starting date for repository access
     * @return the number of bug fixes for file eid in repository pid between cdate and start
     */
    private static int findNumber(int eid, int pid, String cdate, String sdate, Policy pol) {
        int ret = 0;
        switch (pol) {
        case BUGS:
            ret = findNumberOfBugs(eid, pid, cdate, sdate);
            break;
        case CHANGES:
            ret = findNumberOfChanges(eid, pid, cdate, sdate);
            break;
        case AUTHORS:
            ret = findNumberOfAuthors(eid, pid, cdate, sdate);
            break;
        case LRU: // do nothing
        }
        return ret;
    }

    // XXX: Do we need pid? or is eid unique enough for this query?
    /**
     * 
     * @param eid -- entity id
     * @param pid -- repository id
     * @param cdate -- the commit date
     * @param start -- the starting date for repository access
     * @return the number of distinct authors for file eid in repository pid between cdate and start
     */
    private static int findNumberOfAuthors(int eid, int pid, String cdate, String start) {
        int ret = 0;
        try {
            if (findNumberOfAuthorsQuery == null)
                findNumberOfAuthorsQuery = conn
                .prepareStatement(findNumberOfAuthors);
            findNumberOfAuthorsQuery.setString(1, cdate);
            findNumberOfAuthorsQuery.setString(2, start);
            findNumberOfAuthorsQuery.setInt(3, eid);
            ret = Util.Database.getIntResult(findNumberOfAuthorsQuery);
        } catch (SQLException e1) {
            e1.printStackTrace();
        }
        return ret;
    }
    
    // XXX: Do we need pid? or is eid unique enough for this query?
    /**
     * 
     * @param eid -- entity id
     * @param pid -- repository id
     * @param cdate -- the commit date
     * @param start -- the starting date for repository access
     * @return the number of commits for file eid in repository pid between cdate and start
     */
    private static int findNumberOfChanges(int eid, int pid, String cdate, String start) {
        int ret = 0;
        try {
            if (findNumberOfChangesQuery == null)
                findNumberOfChangesQuery = conn
                .prepareStatement(findNumberOfChanges);
            findNumberOfChangesQuery.setString(1, cdate);
            findNumberOfChangesQuery.setString(2, start);
            findNumberOfChangesQuery.setInt(3, eid);
            ret = Util.Database.getIntResult(findNumberOfChangesQuery);
        } catch (SQLException e1) {
            e1.printStackTrace();
        }
        return ret;
    }

    // XXX: Do we need pid? or is eid unique enough for this query?
    /**
     * 
     * @param eid -- entity id
     * @param pid -- repository id
     * @param cdate -- the commit date
     * @param start -- the starting date for repository access
     * @return the number of bug fixes for file eid in repository pid between cdate and start
     */
    private static int findNumberOfBugs(int eid, int pid, String cdate, String start) {
        int ret = 0;
        try {
            if (findNumberOfBugsQuery == null)
                findNumberOfBugsQuery = conn.prepareStatement(findNumberOfBugs);

            findNumberOfBugsQuery.setInt(1, eid);
            findNumberOfBugsQuery.setString(2, cdate);
            findNumberOfBugsQuery.setString(3, start);
            ret = Util.Database.getIntResult(findNumberOfBugsQuery);
        } catch (SQLException e1) {
            e1.printStackTrace();
        }
        return ret;
    }

    /**
     * 
     * @param eid -- entity id
     * @param cid -- commit id
     * @return the lines of code for eid at cid
     */
    private static int findLoc(int eid, int cid) {
        int ret = 0;
        try {
            if (findLocQuery == null)
                findLocQuery = conn.prepareStatement(findLoc);
            findLocQuery.setInt(1, eid);
            findLocQuery.setInt(2, cid);
            ret = Util.Database.getIntResult(findLocQuery);
        } catch (SQLException e1) {
            e1.printStackTrace();
        }
        return ret;
    }
    
    /**
     * @return Returns the entityId.
     */
    public int getEntityId() {
        return entityId;
    }

    /**
     * @return Returns the cachedDate.
     */
    public int getCachedDate() {
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
     * for debugging; used only for the DBUnit tests
     * @return number
     */
    protected int getNumber() {
        return number;
    }

}
