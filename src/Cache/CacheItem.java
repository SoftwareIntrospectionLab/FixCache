package Cache;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Calendar;
import java.util.Date;
import Database.DatabaseManager;

public class CacheItem {

	public enum CacheReason{Prefetch, CoChange, NewEntity, ModifiedEntity, BugEntity}
	private final int entityId;
	private Date loadDate; // changed on cache hit
	private int LOC;
	private int numberOfChanges;
	private int numberOfBugs;
	private int numberOfAuthors;
	
	//XXX do we need these fields?
	private CacheReason loadType;
	private String commitDate; // for debugging?

	DatabaseManager dbManager = DatabaseManager.getInstance();
	Connection conn = dbManager.getConnection();
	
	Statement stmt;
	StringBuilder sql = new StringBuilder();
	ResultSet r;

	public CacheItem(int eid, int cid, String cdate, CacheReason reas, String sdate)
	{
		entityId = eid;
		commitDate = cdate;
		update(reas, cid, cdate, sdate);
	}


	public void update(CacheReason reas, int cid, String cdate, String sdate){
		loadDate = Calendar.getInstance().getTime(); // XXX fix this, deprecated method
		LOC = findLoc(entityId, cid);
		numberOfChanges = findNumberOfChanges(entityId, cdate, sdate);
		numberOfBugs = findNumberOfBugs(entityId, cdate, sdate);
		numberOfAuthors = findNumberOfAuthors(entityId, cdate, sdate);
		loadType = reas;
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

	private int findNumberOfAuthors(int eid, String cdate, String start) {
		int numAuthor = 0;
		//		sql = "select count(distinct(author_id)) from scmlog where id in(" + //two slow to find the number of authors from the database
		//				"select commit_id from actions where file_id="+eid+" and commit_id between "+Simulator.STARTIDDEFAULT +" and "+cid +")";//???start_Id
		sql.setLength(0);
		sql.append("select count(id) from people where id in( select author_id from scmlog, actions where scmlog.id = actions.commit_id and date <='"+cdate+"' and date >= '"+start +"' and file_id = "+eid+")");
//		sql = "select count(id) from people where id in( select author_id from scmlog, actions where scmlog.id = actions.commit_id and date <='"+cdate+"' and date >= '"+start +"' and file_id = "+eid+")";
		try
		{
			stmt = conn.createStatement();
			r = stmt.executeQuery(sql.toString());
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

	private int findNumberOfBugs(int eid, String cdate, String start) {
		int numBugs = 0;
		sql.setLength(0);
		sql.append("select count(commit_id) from actions where file_id="+eid+" and commit_id in (select id from scmlog where is_bug_fix=1 and date <= '"+cdate+"' and date >='"+start+"')");
//		sql = "select count(commit_id) from actions where file_id="+eid+" and commit_id in" +
//		"(select id from scmlog where is_bug_fix=1 and date <= '"+cdate+"' and date >='"+start+"')";
		try
		{
			stmt = conn.createStatement();
			r = stmt.executeQuery(sql.toString());
			while(r.next()){
				numBugs = r.getInt(1);
			}

		}catch (Exception e) {
			System.out.println(e);
			System.exit(0);
		}		
		return numBugs;
	}

	private int findNumberOfChanges(int eid, String cdate, String start) {
		int numChanges = 0;
		sql.setLength(0);
		sql.append("select count(actions.id) from actions, scmlog where actions.commit_id = scmlog.id and date <='"+cdate+"' and date >='"+ start+"' and file_id="+eid);
//		sql = "select count(actions.id) from actions, scmlog where actions.commit_id = scmlog.id and date <='"+cdate+"' and date >='"+ start+"' and file_id="+eid;
		try
		{
			stmt = conn.createStatement();
			r = stmt.executeQuery(sql.toString());
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

	// TODO: fix to use commit id to ensure unique lookup
	private int findLoc(int eid, int cid) {
		int loc =0;
		sql.setLength(0);
		sql.append("select loc from content_loc where file_id="+eid+" and commit_id = "+cid);
//		sql = "select loc from content_loc where file_id="+eid+" and commit_id = "+cid;
		try
		{
			stmt = conn.createStatement();
			r = stmt.executeQuery(sql.toString());
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

	
}
