package Cache;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Date;
import Database.DatabaseManager;

public class CacheItem {

	// Setting up the sql statement strings used in this class
	static Connection conn = DatabaseManager.getConnection();
	static final String findNumberOfAuthors = "select count(id) from people where id in( select author_id from scmlog, actions where scmlog.id = actions.commit_id and date <=? and date >= ? and file_id = ?)"; // with ? symbols
	static final String findNumberOfChanges = "select count(actions.id) from actions, scmlog where actions.commit_id = scmlog.id and date <=? and date >=? and file_id=?";
	static final String findNumberOfBugs = "select count(commit_id) from actions where file_id=? and commit_id in (select id from scmlog where is_bug_fix=1 and date <=? and date >=?)";
	static final String findLoc = "select loc from content_loc where file_id=? and commit_id =?";
	private static PreparedStatement findNumberOfAuthorsQuery;
	private static PreparedStatement findNumberOfChangesQuery;
	private static PreparedStatement findNumberOfBugsQuery;
	private static PreparedStatement findLocQuery;


	public enum CacheReason{Prefetch, CoChange, NewEntity, ModifiedEntity, BugEntity}
	private final int entityId;
	private Date loadDate; // changed on cache hit
	private int LOC; // changed on cache hit
	private int number; // represents either the number of bugs, changes, or authors
	CacheReason reason;

	private final static CacheReplacement.Policy pol = Cache.getCache().getPolicy();


	public CacheItem(int eid, int cid, String cdate, CacheReason r)
	{
		entityId = eid;
		reason = r;
		update(cid, cdate, Cache.getCache().getStartDate());
	}

	public void update(int cid, String cdate, String sdate){
		loadDate = Calendar.getInstance().getTime(); // XXX fix this, deprecated method
		LOC = findLoc(entityId, cid);
		number = findNumber(entityId, cdate, sdate);
	}

	private int findNumber(int entityId, String cdate, String sdate) {
		int ret = 0; 
		switch (pol){
		case BUGS: ret = findNumberOfBugs(entityId, cdate, sdate); break;
		case CHANGES: ret = findNumberOfChanges(entityId, cdate, sdate); break;
		case AUTHORS: ret = findNumberOfAuthors(entityId, cdate, sdate);break; 
		case LRU: // do nothing
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
	public Date getCachedDate() {
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
		//assert(pol == CacheReplacement.Policy.CHANGES);
		return number;
	}


	/**
	 * @return Returns the numberOfBugs.
	 */
	public int getNumberOfBugs() {
		//assert(pol == CacheReplacement.Policy.BUGS);
		return number;
	}
	/**
	 * @return Returns the numberOfAuthors
	 */
	public int getNumberOfAuthors() {
		assert(pol == CacheReplacement.Policy.AUTHORS);
		return number;
	}

	private int findNumberOfAuthors(int eid, String cdate, String start) {
		int ret = 0;
		try {
			if (findNumberOfAuthorsQuery == null)
				findNumberOfAuthorsQuery = conn.prepareStatement(findNumberOfAuthors);
			findNumberOfAuthorsQuery.setString(1, cdate);
			findNumberOfAuthorsQuery.setString(2, start);
			findNumberOfAuthorsQuery.setInt(3, eid);
			ret = Util.Database.getIntResult(findNumberOfAuthorsQuery);
		} catch (SQLException e1) {
			e1.printStackTrace();
		}
		return ret;
	}

	private int findNumberOfChanges(int eid, String cdate, String start) {
		int ret = 0;
		try {
			if (findNumberOfChangesQuery == null)
				findNumberOfChangesQuery = conn.prepareStatement(findNumberOfChanges);
			findNumberOfChangesQuery.setString(1, cdate);
			findNumberOfChangesQuery.setString(2, start);
			findNumberOfChangesQuery.setInt(3, eid);
			ret = Util.Database.getIntResult(findNumberOfChangesQuery);
		} catch (SQLException e1) {
			e1.printStackTrace();
		}
		return ret;
	}


	private int findNumberOfBugs(int eid, String cdate, String start) {
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

	private int findLoc(int eid, int cid)
	{
		int ret = 0;
		try{
			if(findLocQuery == null)
				findLocQuery = conn.prepareStatement(findLoc);			
			findLocQuery.setInt(1, eid);
			findLocQuery.setInt(2, cid);
			ret = Util.Database.getIntResult(findLocQuery);
		}catch(SQLException e1){
			e1.printStackTrace();
		}
		return ret;
	}

}
