package Cache;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import Database.DBOperation;
import Database.DatabaseManager;

public class CacheItem {

	public enum CacheReason{Prefetch, CoChange, NewEntity, ModifiedEntity, BugEntity}

	private final int entityId;
	private CacheReason loadType;
	private Date loadDate; // changed on cache hit

	private int LOC;

	private int numberOfChanges;
	private int numberOfBugs;
	private int numberOfAuthors;

	private String commitDate; // for debugging?

	DatabaseManager dbManager = DatabaseManager.getInstance();
	Connection conn = dbManager.getConnection();
	Statement stmt;
	String sql;
	ResultSet r;

	//public CacheItem(String eid, CacheReason reas, Date time, int loc, int noc, int nob, int noa, int cid)
	public CacheItem(int eid, String cdate, CacheReason reas, String startDate)
	{
		Date time = Calendar.getInstance().getTime(); // XXX fix this
		entityId = eid;
		loadType = reas;
		commitDate = cdate;
		int loc = findLoc(eid);
		int noc = findNumberOfChanges(eid,startDate);
		int nob = findNumberOfBugs(eid, startDate);
		int noa = findNumberOfAuthors(eid, startDate);
		update(time, loc, noc, nob, noa);
	}


//	public void update(int cid, Cache c){
//		Date time = Calendar.getInstance().getTime(); // XXX fix this
//		int loc = findLoc(entityId, cid);
//		int noc = findNumberOfChanges(entityId, cid, c.startDate);
//		int nob = findNumberOfBugs(entityId, cid, c.startDate);
//		int noa = findNumberOfAuthors(entityId, cid, c.startDate);
//		update(time, loc, noc, nob, noa);
//
//	}

	private void update(Date time, int loc, int noc, int nob, int noa)
	{
		loadDate = time;
		LOC = loc;
		numberOfChanges = noc;
		numberOfBugs = nob;
		numberOfAuthors = noa;
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
		return numberOfChanges;
	}


	/**
	 * @return Returns the numberOfBugs.
	 */
	public int getNumberOfBugs() {
		return numberOfBugs;
	}
	/**
	 * @return Returns the numberOfAuthors
	 */
	public int getNumberOfAuthors() {
		return numberOfAuthors;
	}

	private int findNumberOfAuthors(int eid, String start) {
		// TODO Auto-generated method stub
		int numAuthor = 0;
		//		sql = "select count(distinct(author_id)) from scmlog where id in(" + //two slow to find the number of authors from the database
		//				"select commit_id from actions where file_id="+eid+" and commit_id between "+Simulator.STARTIDDEFAULT +" and "+cid +")";//???start_Id
		sql = "select count(id) from people where id in( select author_id from scmlog, actions where scmlog.id = actions.commit_id and date <='"+commitDate+"' and date >= '"+start +"' and file_id = "+eid+")";
		try
		{
			stmt = conn.createStatement();
			r = stmt.executeQuery(sql);
			while(r.next())
			{
				numAuthor = r.getInt(1);
			}

		}catch (Exception e) {
			System.out.println(e);
			System.exit(0);
		}		

		return numAuthor;
	}

	private int findNumberOfBugs(int eid, String start) {
		// TODO Auto-generated method stub
		int numBugs = 0;
		sql = "select count(commit_id) from actions where file_id="+eid+" and commit_id in" +
		"(select id from scmlog where is_bug_fix=1 and date <= '"+commitDate+"' and date >='"+start+"')";
		try
		{
			stmt = conn.createStatement();
			r = stmt.executeQuery(sql);
			while(r.next()){
				numBugs = r.getInt(1);
			}

		}catch (Exception e) {
			System.out.println(e);
			System.exit(0);
		}		
		return numBugs;
	}

	private int findNumberOfChanges(int eid, String start) {
		// XXX >= startCId?
		int numChanges = 0;
		sql = "select count(actions.id) from actions, scmlog where actions.commit_id = scmlog.id and date <='"+commitDate+"' and date >='"+ start+"' and file_id="+eid;//???
		try
		{
			stmt = conn.createStatement();
			r = stmt.executeQuery(sql);
			while(r.next())
			{
				numChanges = r.getInt(1);
			}

		}catch (Exception e) {
			System.out.println(e);
			System.exit(0);
		}		

		return numChanges;
	}

	private int findLoc(int eid) {
		// TODO Auto-generated method stub
		int loc =0;
		sql = "select loc from content_loc, scmlog where file_id="+eid+" and date = '"+commitDate + "' and content_loc.commit_id = scmlog.id";
		try
		{
			stmt = conn.createStatement();
			r = stmt.executeQuery(sql);
			while(r.next())
			{
				loc = r.getInt(1);
			}

		}catch (Exception e) {
			System.out.println(e);
			System.exit(0);
		}		
		return loc;
	}

	
	public static void main(String[] args){

	}
}
