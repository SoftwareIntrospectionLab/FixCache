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
	String sql;
	ResultSet r;

	public CacheItem(int eid, String cdate, CacheReason reas, String sdate)
	{
		entityId = eid;
		commitDate = cdate;
		update(reas, cdate, sdate);
	}


	public void update(CacheReason reas, String cdate, String sdate){
		loadDate = Calendar.getInstance().getTime(); // XXX fix this, deprecated method
		LOC = findLoc(entityId, cdate);
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
		sql = "select count(id) from people where id in( select author_id from scmlog, actions where scmlog.id = actions.commit_id and date <='"+cdate+"' and date >= '"+start +"' and file_id = "+eid+")";
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

	private int findNumberOfBugs(int eid, String cdate, String start) {
		int numBugs = 0;
		sql = "select count(commit_id) from actions where file_id="+eid+" and commit_id in" +
		"(select id from scmlog where is_bug_fix=1 and date <= '"+cdate+"' and date >='"+start+"')";
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

	private int findNumberOfChanges(int eid, String cdate, String start) {
		int numChanges = 0;
		sql = "select count(actions.id) from actions, scmlog where actions.commit_id = scmlog.id and date <='"+cdate+"' and date >='"+ start+"' and file_id="+eid;
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

	private int findLoc(int eid, String cdate) {
		int loc =0;
		sql = "select loc from content_loc, scmlog where file_id="+eid+" and date = '"+cdate + "' and content_loc.commit_id = scmlog.id";
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

	
}
