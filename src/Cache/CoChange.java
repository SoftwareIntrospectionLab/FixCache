package Cache;

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Map;

import Database.DBOperation;

public class CoChange {



	int fileID;

	public CoChange(int fileID)
	{
		this.fileID = fileID;
	}

	// build a table of files that are changed with fileID, before time commitID
	private CoChangeFileMap buildCoChangeMap(int commitID)
	{
		CoChangeFileMap coChangeCounts = new CoChangeFileMap();

		// get a list of all prior commits for fileID before commitID:
		//TODO
		DBOperation dbOp = new DBOperation("jdbc:mysql://db-01:3306/ejw_xzhu1","ejw_xzhu1","opfMf477");
		Connection conn = dbOp.getConnection();
		String sql = "SELECT commit_id from actions where file_id="+fileID+" and commit_id <="+commitID;
		ResultSet r1 = dbOp.ExeQuery(conn, sql);
		List commitList = new ArrayList();

		try{
			while(r1.next()) {
				commitList.add(r1.getInt(1));
			}
		}  
		catch (Exception e) {
			System.out.println(e);
			System.exit(0);
		}
		// for each commit in the list, get a list of all fileIDs involved in that commit
		// TODO fix bug
		int coChangeCommitID;
		List coChangeList = new ArrayList();
		ResultSet r2;
		for(int i = 0; i < commitList.size();i++)
		{
			coChangeCommitID = (Integer)commitList.get(i);
			sql = "SELECT file_id from actions where commit_id = "+coChangeCommitID;
			r2 = dbOp.ExeQuery(conn, sql);
			try{
				while(r2.next()) {

					if(r2.getInt(1)!=fileID)
					{
						coChangeList.add(r2.getInt(1));
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
			coChangeCounts.add(f);		
		}

		return coChangeCounts;

	}
	//get BLOCKSIZE-1 
	private ArrayList<Integer> getCoChangeList(CoChangeFileMap countTable, int blocksize)
	{
		return countTable.getTopFiles(blocksize);
	}


	//	private List<Integer> coChangedFiles getCoChangeList(HashMap<Integer, Integer> countTable)
	//	{
	//		for Entry<Integer, Integer> e in countTable.entrySet()
	//			if (1/e.snd < DISTANCE)
	//				add the fileID to the list
	//	}

	public static void main(String args[])
	{
		CoChange coChange = new CoChange(3679);
		CoChangeFileMap countTable = coChange.buildCoChangeMap(10000);
		List coChangeList = countTable.getTopFiles(100);
		System.out.print(coChangeList.size());
		for(int i=0;i<coChangeList.size();i++)
		{
			System.out.println((Integer)coChangeList.get(i));
		}
	}
	public class CoChangeFileMap{
		//HashSet<Integer> newFiles;
		//int []fileIds;
		//int []counts;
		//int index;
		HashMap<Integer, Integer> map;

		CoChangeFileMap(){
			map = new HashMap<Integer, Integer>();
		}

		void add(int f) 
		{
			// if it is not there, create a new entry
			// if it is htere ++count
			if (map.containsKey(f))
			{
				int count = map.get(f);
				map.remove(f);
				map.put(f, count+1);
			}

			else
				map.put(f, 1);
		}

		// TODO: make this return top files, not bottom files
		ArrayList<Integer> getTopFiles(int blocksize) { 
			List list = new ArrayList(map.entrySet());
			ArrayList topFiles = new ArrayList();
			Collections.sort(list, new Comparator(){
				public int compare(Object o1, Object o2)
				{
					return((Comparable) ((Map.Entry) (o1)).getValue()).compareTo(((Map.Entry) (o2)).getValue());
				}
			});
			for(int i=0;i<blocksize-1;i++)
			{
				if (list.size() > i){
					Map.Entry curr = (Entry) list.get(i);
					topFiles.add(curr.getKey());
				}
			}
			return topFiles;

		}

	}
}
