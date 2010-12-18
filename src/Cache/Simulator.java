package Cache;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import Util.CmdLineParser;

import Database.DBOperation;	
import Database.DatabaseManager;

public class Simulator {
	
	static final int BLKDEFAULT = 3;
	static final int PFDEFAULT = 3;
	static final int CSIZEDEFAULT = 10;
	static final int PRODEFAULT = 1;
	
	public enum FileType{A, M, D, V, C, R}
	
	int blocksize;
	int prefetchsize;
	int cachesize;
	int pid;
	CacheReplacement.Policy cacheRep;		
	Cache cache; 
	DatabaseManager dbManager = DatabaseManager.getInstance();
	Connection conn = dbManager.getConnection();
	
	public Simulator(int bsize, int psize, int csize, int projid, CacheReplacement.Policy rep, String start)	
	{
		blocksize = bsize;
		prefetchsize = psize;
		cachesize = csize;
		this.pid = projid;
		cacheRep = rep;	
		cache =  new Cache(cachesize, new CacheReplacement(rep), start);
		
		System.out.println("start to simulate");
	}

	// input: initial commit ID
	// input: LOC for every file in initial commit ID
	// input: pre-fetch size
	// output: fills cache with pre-fetch size number of top-LOC files from initial commit
	public void preLoad(int prefetchSize)
	{
		// database query: top prefetchsize fileIDs (in terms of LOC) in the first commitID for pid
		// for each fileId in the list create a cacheItem
		//sql = "select file_id, LOC from actions where commit_id ="+startCId +" order by LOC DESC";
		String sql;
		Statement stmt;
		ResultSet r;
		if (cache.startDate == null)
			sql = "select min(date) from scmlog";
		else
			sql = "select min(date) from scmlog where date >= '" +cache.startDate+"'";
		String firstDate = "";
		try{
			stmt = conn.createStatement();
			r = stmt.executeQuery(sql);
			while(r.next())
			{
				firstDate = r.getDate(1).toString()+" "+r.getTime(1).toString();
			}
		}catch (Exception e) {
			System.out.println(e);
			System.exit(0);}
		sql = "select file_id,content_loc.commit_id from content_loc, scmlog where content_loc.commit_id = scmlog.id and date ='"+ firstDate + "' order by loc DESC";
		
		int fileId = 0;
		int startCommitId = 0;
		try {
			stmt = conn.createStatement();
			r = stmt.executeQuery(sql);
			for (int size = 0; size < prefetchSize; size++) {
				if (r.next()) {
					fileId = r.getInt(1);
					startCommitId = r.getInt(2);
					cache.add(fileId, startCommitId, CacheItem.CacheReason.Prefetch);
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
    public int getBugIntroCid(int fileId, int commitId, int pId)
    {
    	// use the fileId and commitId to get a list of changed hunks from the hunk table.
    	// for each changed hunk, get the blamedHunk from the hunk_blame table; get the commit id associated with this blamed hunk
    	// take the maximum (in terms of date?) commit id and return it
    	int bugIntroCId = -1;
    	int hunkId;
    	DatabaseManager dbManager = DatabaseManager.getInstance();
    	Connection conn = dbManager.getConnection();
    	Statement stmt;
    	Statement stmt1;
    	Statement stmt2;
    	String sql = "select id from hunks where file_id = "+fileId+" and commit_id ="+commitId;//select the hunk id of fileId for a bug_introducing commitId
    	ResultSet r;
    	ResultSet r1;
    	ResultSet r2;
    	String rev;
    	try{
    		stmt = conn.createStatement();
    		r = stmt.executeQuery(sql);
    		while(r.next())
    	{
    		hunkId = r.getInt(1);
    		stmt1 = conn.createStatement();
    		sql = "select bug_rev from hunk_blames where hunk_id = "+ hunkId;//for each hunk find the bug introducing rev
    		r1 = stmt1.executeQuery(sql);
    		while(r1.next())
    		{
    			rev = r1.getString(1);
    			stmt2 = conn.createStatement();
    			sql = "select id from scmlog where rev = "+"'"+rev+"'" +" and repository_id = "+pId;//find the commit id according to rev and project id
    			r2 = stmt2.executeQuery(sql);
    			while(r2.next())
    			{
    				if(r2.getInt(1) > bugIntroCId)//bugIntroCId is always the maximum bug introducing commit id
    				{
    					bugIntroCId = r2.getInt(1);
    				}
    			}
    		}
    	}
    	}catch (Exception e) {
			System.out.println(e);
			System.exit(0);}
    
    	return bugIntroCId;
    }


	public static void main(String args[])
	{
		// TODO: write unit tests

		//String startDate, endDate;
		String start;
		int hit = 0;
		int miss = 0;
        CmdLineParser parser = new CmdLineParser();
        CmdLineParser.Option blksz_opt = parser.addIntegerOption('b', "blksize");
        CmdLineParser.Option csz_opt = parser.addIntegerOption('c', "csize");
        CmdLineParser.Option pfsz_opt = parser.addIntegerOption('f', "pfsize");
        CmdLineParser.Option crp_opt = parser.addStringOption('r', "cacherep");        
        CmdLineParser.Option pid_opt = parser.addIntegerOption('p', "pid");
        CmdLineParser.Option dt_opt =  parser.addStringOption('t',"datetime");
        
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
        String dt = (String)parser.getOptionValue(dt_opt, "2000-01-01 00:00:00");
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
        // TODO: make command line input for start and end date
        start = dt;
        
        if (pid == null){
            System.err.println("Error: must specify a Project Id");
            System.exit(2);
        }

        
        // create a new simulator
		Simulator sim = new Simulator(blksz, pfsz, csz, pid, crp, start);		
		sim.preLoad(sim.prefetchsize);
		//  if you order scmlog by commitid or by date, the order is different: so order by date
		String sql = "select id, is_bug_fix from scmlog where repository_id = "+pid+" and date>='"+sim.cache.startDate+"' order by date ASC";
		
		DatabaseManager dbManager = DatabaseManager.getInstance();
		Connection conn = dbManager.getConnection();
		Statement stmt;
		Statement stmt1;
		ResultSet r;
		ResultSet r1;
		
		//select (id, bugfix) from scmlog orderedby date  --- need join
		// main loop
		int id;//means commit_id in actions
		boolean isBugFix;
		
		int numprefetch = 0;
		//iterate over the selection 
		int file_id;
		FileType type;
		int loc;
		try {
			stmt = conn.createStatement();
			r = stmt.executeQuery(sql);
			while (r.next()) {
				id = r.getInt(1);
				isBugFix = r.getBoolean(2);
				//only deal with .java files
				sql = "select actions.file_id, type ,loc from actions, content_loc, files where actions.file_id = files.id and files.file_name like '%.java' and actions.file_id=content_loc.file_id and actions.commit_id = "+id+" and content_loc.commit_id ="+id+" order by loc DESC";
//				sql = "select actions.file_id, type ,loc from actions, content_loc where actions.file_id=content_loc.file_id and actions.commit_id = "+id+" and content_loc.commit_id ="+id+" order by loc DESC";
				stmt1 = conn.createStatement();
				r1 = stmt1.executeQuery(sql);
				// loop through those file ids
				while (r1.next()) {
					file_id = r1.getInt(1);
					type = FileType.valueOf(r1.getString(2));
					loc = r1.getInt(3);
					switch (type) {
					case V:	
						break;
					case R:
						if (numprefetch < sim.prefetchsize) {
							numprefetch++;
							sim.cache.add(file_id, id,
									CacheItem.CacheReason.NewEntity);
						}
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
							int intro_cid = sim.getBugIntroCid(file_id, id, pid);
							if(sim.cache.cacheTable.containsKey(intro_cid))
							{
								hit++;
							}
							else
							{
								miss++;
							}
							sim.cache.add(file_id, intro_cid,
									CacheItem.CacheReason.BugEntity); // XXX
																		// should
																		// this
																		// be id
																		// or
																		// intro_cid?
							ArrayList<Integer> cochanges = CoChange.getCoChangeFileList(file_id, intro_cid, sim.blocksize);
							sim.cache.add(cochanges, id, CacheItem.CacheReason.CoChange);
						} else {
							if (numprefetch < sim.prefetchsize) {
								numprefetch++;
								sim.cache.add(file_id, id, CacheItem.CacheReason.ModifiedEntity);
							}

						}
					}
				}
			numprefetch = 0;
			}
		} catch (Exception e) {
			System.out.println(e);
			System.exit(0);}
		dbManager.close();
		System.out.println(hit+"***"+miss);
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
