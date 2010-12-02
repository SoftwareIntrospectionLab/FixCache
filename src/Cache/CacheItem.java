package Cache;

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import Database.DBOperation;

public class CacheItem {
	
	public enum CacheReason{Prefetch, CoChange, NewEntity, ModifiedEntity, BugEntity}

	private final int entityId;
	private CacheReason loadType;
	private Date loadDate; // changed on cache hit
	
	private int LOC;
	
	private int numberOfChanges;
	private int numberOfBugs;
	private int numberOfAuthors;
	
	private int commitId; // for debugging?

	String sql;
	ResultSet r;
	
	//public CacheItem(String eid, CacheReason reas, Date time, int loc, int noc, int nob, int noa, int cid)
	public CacheItem(int eid, int cid, CacheReason reas)
	{
		Date time = Calendar.getInstance().getTime(); // XXX fix this
		entityId = eid;
		int loc = findLoc(eid, cid);
		int noc = findNumberOfChanges(eid, cid);
		int nob = findNumberOfBugs(eid, cid);
		int noa = findNumberOfAuthors(eid, cid);
		update(reas, time, loc, noc, nob, noa, cid);
	}

	
	public void update(CacheReason reas, int cid){
		Date time = Calendar.getInstance().getTime(); // XXX fix this
		int loc = findLoc(entityId, cid);
		int noc = findNumberOfChanges(entityId, cid);
		int nob = findNumberOfBugs(entityId, cid);
		int noa = findNumberOfAuthors(entityId, cid);
		update(reas, time, loc, noc, nob, noa, cid);
		
	}

	private void update(CacheReason reas, Date time, int loc, int noc, int nob, int noa, int cid)
	{
		loadType = reas;
		loadDate = time;
		LOC = loc;
		numberOfChanges = noc;
		numberOfBugs = nob;
		numberOfAuthors = noa;
		commitId = cid;
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
	
	private int findNumberOfAuthors(int eid, int cid) {
	         // TODO Auto-generated method stub
		int numAuthor = 0;
		sql = "select count(distinct(author_id)) from scmlog where id in(" +
				"select commit_id from actions where file_id="+eid+" and commit_id between "+Simulator.STARTIDDEFAULT +" and "+cid +")";//???start_Id
		r = Simulator.dbOp.ExeQuery(Simulator.conn, sql);
		try
		{
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
	
	 private int findNumberOfBugs(int eid, int cid) {
	         // TODO Auto-generated method stub
			int numBugs = 0;
			sql = "select count(commit_id) from actions where file_id="+eid+" and commit_id in" +
					"(select id from scmlog where is_bug_fix=1 and id between "+Simulator.STARTIDDEFAULT+" and "+cid+")";
			r = Simulator.dbOp.ExeQuery(Simulator.conn, sql);
			try
			{
				while(r.next()){
					numBugs = r.getInt(1);
				}
				
			}catch (Exception e) {
	          System.out.println(e);
	          System.exit(0);
	        }		
			
		         return numBugs;
	 }
	
	 private int findNumberOfChanges(int eid, int cid) {
	         // XXX >= startCId?
		    int numChanges = 0;
			sql = "select count(id) from actions where file_id="+eid+" and commit_id between "+Simulator.STARTIDDEFAULT +" and "+cid;//???
			r = Simulator.dbOp.ExeQuery(Simulator.conn, sql);
			try
			{
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
	
	 private int findLoc(int eid, int cid) {
	         // TODO Auto-generated method stub
		    int loc =0;
		    sql = "select loc from content_loc where file_id="+eid+" and commit_id = "+cid;
			r = Simulator.dbOp.ExeQuery(Simulator.conn, sql);
			try
			{
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
