package Cache;

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.HashMap;
import java.util.Set;

import javax.swing.RowFilter.Entry;

import Database.DBOperation;	

public class N_Simulator {
	
	N_CacheItem cacheItem;
	N_Cache cache = new N_Cache(10);;

	
	public N_Simulator()	
	{
		System.out.println("start to simulate");
	}
	
	public void fillCache()
	{
		
	}

	public List replacement(int outSize)
	{
		N_CacheReplacement rep= new N_CacheReplacementCHANGE();
		List outList =  rep.compute_replacement_set(outSize, cache);
		return outList;
		
	}
	
	// build a table of files that are changed with fileID, before time commitID
	private HashMap<Integer, Integer> buildCoChangeMap(int fileID, int commitID)
	{
		HashMap<Integer, Integer> coChangeCounts = new HashMap<Integer, Integer>();
		
		// get a list of all prior commits for fileID before commitID:
		//TODO
		DBOperation dbOp = new DBOperation("jdbc:mysql://db-01:3306/ejw_xzhu1","ejw_xzhu1","opfMf477");
		Connection conn = dbOp.getConnection();
		ResultSet r1 = dbOp.ExeQuery(conn, "SELECT commit_id from actions where file_id = fileID and commit_id <= commitID");
		List commitList = new ArrayList();
		try{
			 while(r1.next()) {
				 commitList.add(r1.getInt(0));
	       }
		}  
		catch (Exception e) {
	              System.out.println(e);
	              System.exit(0);
	         }
		// for each commit in the list, get a list of all fileIDs involved in that commit
		// TODO
		int coChangeCommitID;
		List coChangeList = new ArrayList();
		ResultSet r2;
		for(int i = 0; i < commitList.size();i++)
		{
			coChangeCommitID = (Integer)commitList.get(i);
			r2 = dbOp.ExeQuery(conn, "SELECT file_id from actions where commit_id =coChangeCommitID");
			try{
				 while(r2.next()) {
					 
					 if(r2.getInt(0)!=fileID)
					 {
						 coChangeList.add(r2.getInt(0));
					 }
					 
		       }
			}  
			catch (Exception e) {
		              System.out.println(e);
		              System.exit(0);
		         }
		}
		// for each such file f:
		int f;
		int count;
		for(int j = 0; j < commitList.size(); j++)
		{
			f =  (Integer)commitList.get(j);
			if (coChangeCounts.containsKey(f))
			{
				count = coChangeCounts.get(f);
				coChangeCounts.remove(f);
			    coChangeCounts.put(f, count+1);
			}
				
		    else
			    coChangeCounts.put(f, 1);
		
		}
		
      return coChangeCounts;
			
	}
	
	//get cochanged file for fileID
	private List getCoChangeList(HashMap<Integer, Integer> countTable)
	{
		List coChangeList = new ArrayList();
		int fileID;
		int count;
		Set fileSet = countTable.keySet();
		Iterator it = fileSet.iterator();
		while(it.hasNext())
		{
			fileID = (Integer) it.next();
			count = countTable.get(fileID);
//			if((double)1/(double)count < DISTANCE)
//			{
//				coChangeList.add(fileID);
//			}
		}
		
		return coChangeList;
	}
	
//	private List<Integer> coChangedFiles getCoChangeList(HashMap<Integer, Integer> countTable)
//	{
//		for Entry<Integer, Integer> e in countTable.entrySet()
//			if (1/e.snd < DISTANCE)
//				add the fileID to the list
//	}
	

	public static void main(String args[])
	{
		N_Simulator sim =  new N_Simulator();
		sim.fillCache();
		List outList = sim.replacement(1);
		N_CacheItem item = (N_CacheItem) outList.get(0);
		System.out.println(item.getEntityId());
		
	}

}
