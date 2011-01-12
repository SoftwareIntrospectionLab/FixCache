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
	static final String findNumberOfAuthors = "select count(id) from people where id in( select author_id from scmlog, actions where repository_id=? and scmlog.id = actions.commit_id and date <=? and date >= ? and file_id = ?)"; // with ? symbols
	static final String findNumberOfChanges = "select count(actions.id) from actions, scmlog where repository_id=? and actions.commit_id = scmlog.id and date <=? and date >=? and file_id=?";
	static final String findNumberOfBugs = "select count(commit_id) from actions where file_id=? and commit_id in (select id from scmlog where repository_id=? and is_bug_fix=1 and date <=? and date >=?)";
	static final String findLoc = "select loc from content_loc where file_id=? and commit_id =?";
	private static PreparedStatement findNumberOfAuthorsQuery;
	private static PreparedStatement findNumberOfChangesQuery;
	private static PreparedStatement findNumberOfBugsQuery;
	private static PreparedStatement findLocQuery;


	public enum CacheReason{Prefetch, CoChange, NewEntity, ModifiedEntity, BugEntity}
	private final int entityId;
	private int loadDate; // changed on cache hit
	private int LOC; // changed on cache hit
	private int number; // represents either the number of bugs, changes, or authors
	CacheReason reason;

	private final Cache parent;


	public CacheItem(int eid, int cid, String cdate, CacheReason r, Cache p)
	{
		entityId = eid;
		reason = r;
		parent = p;
		update(cid, cdate, p.getStartDate());
	}

	public void update(int cid, String cdate, String sdate){
		loadDate = parent.getTime(); // XXX fix this, deprecated method
		LOC = findLoc(entityId, cid);
		number = findNumber(entityId, parent.repID, cdate, sdate);
	}

	private int findNumber(int entityId, int pid, String cdate, String sdate) {
		int ret = 0; 
		switch (parent.getPolicy()){
		case BUGS: ret = findNumberOfBugs(entityId, pid, cdate, sdate); break;
		case CHANGES: ret = findNumberOfChanges(entityId, pid, cdate, sdate); break;
		case AUTHORS: ret = findNumberOfAuthors(entityId, pid, cdate, sdate);break; 
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
		assert(parent.getPolicy() == CacheReplacement.Policy.CHANGES);
		return number;
	}


	/**
	 * @return Returns the numberOfBugs.
	 */
	public int getNumberOfBugs() {
		assert(parent.getPolicy() == CacheReplacement.Policy.BUGS);
		return number;
	}
	/**
	 * @return Returns the numberOfAuthors
	 */
	public int getNumberOfAuthors() {
		assert(parent.getPolicy() == CacheReplacement.Policy.AUTHORS);
		return number;
	}

	private int findNumberOfAuthors(int eid, int pid, String cdate, String start) {
		int ret = 0;
		try {
			if (findNumberOfAuthorsQuery == null)
				findNumberOfAuthorsQuery = conn.prepareStatement(findNumberOfAuthors);
			findNumberOfAuthorsQuery.setInt(1, pid);
			findNumberOfAuthorsQuery.setString(2, cdate);
			findNumberOfAuthorsQuery.setString(3, start);
			findNumberOfAuthorsQuery.setInt(4, eid);
			ret = Util.Database.getIntResult(findNumberOfAuthorsQuery);
		} catch (SQLException e1) {
			e1.printStackTrace();}
		return ret;
	}

	private int findNumberOfChanges(int eid, int pid, String cdate, String start) {
		int ret = 0;
		try {
			if (findNumberOfChangesQuery == null)
				findNumberOfChangesQuery = conn.prepareStatement(findNumberOfChanges);
			findNumberOfChangesQuery.setInt(1, pid);
			findNumberOfChangesQuery.setString(2, cdate);
			findNumberOfChangesQuery.setString(3, start);
			findNumberOfChangesQuery.setInt(4, eid);
			ret = Util.Database.getIntResult(findNumberOfChangesQuery);
		} catch (SQLException e1) {
			e1.printStackTrace();}
		return ret;
	}


	private int findNumberOfBugs(int eid, int pid, String cdate, String start) {
		int ret = 0;
		try {
			if (findNumberOfBugsQuery == null)
				findNumberOfBugsQuery = conn.prepareStatement(findNumberOfBugs);

			findNumberOfBugsQuery.setInt(1, eid);
			findNumberOfBugsQuery.setInt(2, pid);
			findNumberOfBugsQuery.setString(3, cdate);
			findNumberOfBugsQuery.setString(4, start);
			ret = Util.Database.getIntResult(findNumberOfBugsQuery);
		} catch (SQLException e1) {
			e1.printStackTrace();}
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
			e1.printStackTrace();}
		return ret;
	}
	
	// for debugging 
	protected int getNumber(){
		return number;
	}

}
