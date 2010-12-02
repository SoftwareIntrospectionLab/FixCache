package Cache;

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import Util.CmdLineParser;

import Database.DBOperation;	

public class Simulator {
	
	static final int BLKDEFAULT = 10;
	static final int PFDEFAULT = 10;
	static final int CSIZEDEFAULT = 100;
	static final int STARTIDDEFAULT = 1;
	static final int PRODEFAULT = 1;
	
	public enum FileType{A, M, D, V, C, R}
	
	int blocksize;
	int prefetchsize;
	int cachesize;
	int pid;
	CacheReplacement.Policy cacheRep;		
	Cache cache; 
	
	static DBOperation dbOp;
	static Connection conn;
	static String sql;
	static ResultSet r;

	
	public Simulator(int bsize, int psize, int csize, int projid, CacheReplacement.Policy rep)	
	{
		blocksize = bsize;
		prefetchsize = psize;
		cachesize = csize;
		this.pid = projid;
		cacheRep = rep;	
		cache =  new Cache(cachesize, new CacheReplacement(rep));
		
		System.out.println("start to simulate");
	}

	// input: initial commit ID
	// input: LOC for every file in initial commit ID
	// input: pre-fetch size
	// output: fills cache with pre-fetch size number of top-LOC files from initial commit
	public void preLoad(int startCId, int prefetchSize)
	{
		// database query: top prefetchsize fileIDs (in terms of LOC) in the first commitID for pid
		// for each fileId in the list create a cacheItem
		//sql = "select file_id, LOC from actions where commit_id ="+startCId +" order by LOC DESC";
		sql = "select file_id from content_loc where commit_id = "+ startCId + " order by loc DESC";
		r = dbOp.ExeQuery(conn, sql);
		int fileId = 0;
		try {
			for (int size = 0; size < prefetchSize; size++) {
				if (r.next()) {
					fileId = r.getInt(1);
					cache.add(fileId, startCId, CacheItem.CacheReason.Prefetch);
				}
			}
		} catch (Exception e) {
			System.out.println(e);
			System.exit(0);
		}	
	}
	


    private static void printUsage() {
        System.err.println("Example Usage: FixCache -b=10000 -c=500 -f=600 -r=\"LRU\" -p=1");
        System.err.println("Example Usage: FixCache --blksize=10000 --csize=500 --pfsize=600 --cacherep=\"LRU\" --pid=1");
        System.err.println("-p/--pid option is required");
    }
    
    
    //TODO: find the bug introducing file id for a given bug fixding commitId
    public static int getBugIntroCid(int fileid, int commitId)
    {
    	// use the fileId and commitId to get a list of changed hunks from the hunk table.
    	// for each changed hunk, get the blamedHunk from the hunk_blame table; get the commit id associated with this blamed hunk
    	// take the maximum (in terms of date?) commit id and return it
    	int bugIntroId = 10;
    	return bugIntroId;
    }


	public static void main(String args[])
	{
		// TODO: write unit tests

		int startCId, endCId;
        CmdLineParser parser = new CmdLineParser();

        CmdLineParser.Option blksz_opt = parser.addIntegerOption('b', "blksize");
        CmdLineParser.Option csz_opt = parser.addIntegerOption('c', "csize");
        CmdLineParser.Option pfsz_opt = parser.addIntegerOption('f', "pfsize");
        CmdLineParser.Option crp_opt = parser.addStringOption('r', "cacherep");        
        CmdLineParser.Option pid_opt = parser.addIntegerOption('p', "pid");
        CmdLineParser.Option pw_opt = parser.addStringOption('w', "pw");
        //CmdLineParser.Option sCId_opt = parser.addIntegerOption('s',"start");
        //CmdLineParser.Option eCId_opt = parser.addIntegerOption('e',"end");
        
        
        try {
            parser.parse(args);
        }
        catch ( CmdLineParser.OptionException e ) {
            System.err.println(e.getMessage());
            printUsage();
            System.exit(2);
        }

        Integer blksz = (Integer)parser.getOptionValue(blksz_opt, BLKDEFAULT);
        Integer csz = (Integer)parser.getOptionValue(csz_opt, CSIZEDEFAULT);
        Integer pfsz = (Integer)parser.getOptionValue(pfsz_opt, PFDEFAULT);
        String crp_string = (String)parser.getOptionValue(crp_opt, CacheReplacement.REPDEFAULT);
        Integer pid = (Integer)parser.getOptionValue(pid_opt, PRODEFAULT);
        String pw = (String)parser.getOptionValue(pw_opt, "fixcache");
    	dbOp = new DBOperation("jdbc:mysql://db-01:3306/ejw_xzhu1","ejw_xzhu1",pw);;
    	conn = dbOp.getConnection();
        CacheReplacement.Policy crp;
        try{
        	crp = CacheReplacement.Policy.valueOf(crp_string);
        } catch (IllegalArgumentException e){
            System.err.println(e.getMessage());
            System.err.println("Must specify a valid cache replacement policy");
            crp = CacheReplacement.REPDEFAULT;
        }
        //startCId = (Integer)parser.getOptionValue(sCId_opt, STARTIDDEFAULT);
        //endCId = (Integer)parser.getOptionValue(eCId_opt, Integer.MAX_VALUE);
        
        if (pid == null){
            System.err.println("Error: must specify a Project Id");
            System.exit(2);
        }

        // create a new simulator
		Simulator sim = new Simulator(blksz, pfsz, csz, pid, crp);
		
		sim.preLoad(STARTIDDEFAULT, sim.prefetchsize);
		// XXX if you order scmlog by commitid or by date, do you get the same order?
		sql = "select id, is_bug_fix from scmlog where repository_id = "+pid+" order by date ASC";
		r = dbOp.ExeQuery(conn, sql);
		
		//select (id, bugfix) from scmlog orderedby date  --- need join
		// main loop
		int id;
		boolean isBugFix;
		
		int numprefetch = 0;
		//iterate over the selection 
		ResultSet r1;
		int file_id;
		FileType type;
		int loc;
		try {
			while (r.next()) {
				id = r.getInt(1);
				isBugFix = r.getBoolean(2);
				sql = "select actions.file_id, type ,loc from actions, content_loc where actions.file_id=content_loc.file_id actions.commit_id = content_loc.commit_id = "
						+ id + " order by loc DESC";
				r1 = dbOp.ExeQuery(conn, sql);
				// loop through those file ids
				while (r1.next()) {
					file_id = r1.getInt(1);
					type = FileType.valueOf(r1.getString(2));
					loc = r1.getInt(3);
					switch (type) {
					case V:
						if (numprefetch < sim.prefetchsize) {
							numprefetch++;
							sim.cache.add(file_id, id,
									CacheItem.CacheReason.NewEntity);						
						}
						break;
					case R:
						break;
					case C:
						if (numprefetch < sim.prefetchsize) {
							numprefetch++;
							sim.cache.add(file_id, id,
									CacheItem.CacheReason.NewEntity);
						}
						break;
					case A:
						if (numprefetch < sim.prefetchsize) {
							numprefetch++;
							sim.cache.add(file_id, id,
									CacheItem.CacheReason.NewEntity);
						};
						break;
					case D:
						sim.cache.remove(file_id);// remove from the cache
						break;
					case M: // modified
						if (isBugFix) {
							int intro_cid = getBugIntroCid(file_id, id);
							sim.cache.add(file_id, intro_cid,
									CacheItem.CacheReason.BugEntity); // XXX
																		// should
																		// this
																		// be id
																		// or
																		// intro_cid?
							ArrayList<Integer> cochanges = CoChange
									.getCoChangeFileList(file_id, intro_cid,
											sim.blocksize);
							sim.cache.add(cochanges, id,
									CacheItem.CacheReason.CoChange);
						} else {
							if (numprefetch < sim.prefetchsize) {
								numprefetch++;
								sim.cache.add(file_id, id,
										CacheItem.CacheReason.ModifiedEntity);
							}

						}
					}
				}
			}
			conn.close();
		} catch (Exception e) {
			System.out.println(e);
			System.exit(0);
       }
//		select (file_id, type) from actions where commit_id == id, ordered_by loc
//		int file_id;
//		FileType type;
//		
//		// loop through those file ids
//			switch (type){
//			case V:
//			case R:
//			case C:
//			case A: if (numprefetch < sim.prefetchsize) {
//				numprefetch++;
//				sim.cache.add(file_id, id, CacheItem.CacheReason.NewEntity);
//			};
//			case D: sim.cache.remove(file_id);// remove from the cache
//			case M: 
//				if (bugfix){
//					int intro_cid = getBugIntroCid(file_id, id);
//					sim.cache.add(file_id, intro_cid, CacheItem.CacheReason.BugEntity); // XXX should this be id or intro_cid?
//					ArrayList<Integer> cochanges = CoChange.getCoChangeFileList(file_id, intro_cid, sim.blocksize);
//					sim.cache.add(cochanges, id, CacheItem.CacheReason.CoChange);
//				} else {
//					if (numprefetch < sim.prefetchsize) {
//						numprefetch++;
//						sim.cache.add(file_id, id, CacheItem.CacheReason.ModifiedEntity);
//					}
//					
//				}
//			}
//	}

	
	}
}
