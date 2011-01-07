package Cache;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
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
import Database.DatabaseManager;

public class CoChange {

	int fileID;
	DatabaseManager dbManager = DatabaseManager.getInstance();
	Connection conn = dbManager.getConnection();

	private CoChange(int fileID) {
		this.fileID = fileID;
	}

	public static ArrayList<Integer> getCoChangeFileList(int fileid,
			String commitDate, int blocksize) {
		CoChange co = new CoChange(fileid);
		return co.getCoChangeList(co.buildCoChangeMap(commitDate), blocksize);

	}

	// build a table of files that are changed with fileID, before time commitID
	private CoChangeFileMap buildCoChangeMap(String commitDate) {
		CoChangeFileMap coChangeCounts = new CoChangeFileMap();

		// get a list of all prior commits for fileID before commitID:
		// TODO

		Statement stmt1;
		ResultSet r1;
		String sql = "SELECT commit_id from actions, scmlog where file_id=" + fileID
				+ " and actions.commit_id=scmlog.id and date <= " + commitDate;// cochange commit_id may be
													// smaller than
													// STARTIDDEFAULT
		List commitList = new ArrayList();

		try {
			stmt1 = conn.createStatement();
			r1 = dbManager.executeQuery(sql);
			while (r1.next()) {
				commitList.add(r1.getInt(1));
			}
		} catch (Exception e) {
			System.out.println(e);
			System.exit(0);
		}
		// for each commit in the list, get a list of all fileIDs involved in
		// that commit
		// TODO fix bug
		int coChangeCommitID;
		// List coChangeList = new ArrayList();
		Statement stmt2;
		ResultSet r2;
		int coChangeFile;
		for (int i = 0; i < commitList.size(); i++) {
			coChangeCommitID = (Integer) commitList.get(i);
			sql = "SELECT file_id from actions where commit_id = "
					+ coChangeCommitID;

			try {
				stmt2 = conn.createStatement();
				r2 = stmt2.executeQuery(sql);
				while (r2.next()) {
					coChangeFile = r2.getInt(1);
					if (coChangeFile != fileID) {
						// coChangeList.add(r2.getInt(1));
						coChangeCounts.add(coChangeFile);
					}

				}
			} catch (Exception e) {
				System.out.println(e);
				System.exit(0);
			}
		}
		// for each such file f:
		// int f;
		// for(int j = 0; j < commitList.size(); j++)//I think here commitList
		// should be coChangeList
		// {
		// f = (Integer)commitList.get(j);
		// coChangeCounts.add(f);
		// }

		return coChangeCounts;

	}

	// get BLOCKSIZE-1
	private ArrayList<Integer> getCoChangeList(CoChangeFileMap countTable,
			int blocksize) {
		return countTable.getTopFiles(blocksize);
	}

	// private List<Integer> coChangedFiles getCoChangeList(HashMap<Integer,
	// Integer> countTable)
	// {
	// for Entry<Integer, Integer> e in countTable.entrySet()
	// if (1/e.snd < DISTANCE)
	// add the fileID to the list
	// }

	public static void main(String args[]) {
		CoChange coChange = new CoChange(3679);
		CoChangeFileMap countTable = coChange.buildCoChangeMap("");
		List coChangeList = countTable.getTopFiles(100);
		for (int i = 0; i < coChangeList.size(); i++) {
			System.out.println((Integer) coChangeList.get(i));
		}
	}

	public class CoChangeFileMap {
		// HashSet<Integer> newFiles;
		// int []fileIds;
		// int []counts;
		// int index;
		HashMap<Integer, Integer> map;

		CoChangeFileMap() {
			map = new HashMap<Integer, Integer>();
		}

		void add(int f) {
			// if it is not there, create a new entry
			// if it is htere ++count
			if (map.containsKey(f)) {
				int count = map.get(f);
				map.remove(f);
				map.put(f, count + 1);
			}

			else
				map.put(f, 1);
		}

		// TODO: make this return top files, not bottom files
		ArrayList<Integer> getTopFiles(int blocksize) {
			List list = new ArrayList(map.entrySet());
			ArrayList topFiles = new ArrayList();
			Collections.sort(list, new Comparator() {
				public int compare(Object o1, Object o2) {
					return ((Comparable) ((Map.Entry) (o2)).getValue())
							.compareTo(((Map.Entry) (o1)).getValue());// the
																		// list
																		// should
																		// be
																		// sorted
																		// in
																		// descending
																		// order
				}
			});
			for (int i = 0; i < blocksize - 1; i++)// a block size b indicate s
													// that we load b-1 closest
													// entities.
			{
				if (list.size() > i) {
					Map.Entry curr = (Entry) list.get(i);
					topFiles.add(curr.getKey());
				}
			}
			return topFiles;

		}

	}
}
