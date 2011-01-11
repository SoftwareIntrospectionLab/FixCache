package Cache;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import Cache.CacheItem.CacheReason;

public class Cache {
	
	// Invariant: cacheTable.size() <= size;
	
	private int size;
	Hashtable<Integer, CacheItem> cacheTable = new Hashtable<Integer, CacheItem>();
	
	CacheReplacement policy;
	String startDate;
	int repID;
	
	static Cache singleton;
	
	public static Cache getCache(int csize, CacheReplacement pol, String start, int rep)
	{
		if (singleton == null)
			singleton = new Cache(csize, pol, start, rep);
		return singleton;
	}
	
	
	public static Cache getCache()
	{
		assert(singleton != null);
		return singleton;
	}
	
	// for debugging only
	public static Cache getTestCache(int csize, CacheReplacement pol, String start, int rep)
	{
		Cache c = getCache(csize, pol, start, rep);
		c.cacheTable = new Hashtable<Integer, CacheItem>();
		c.policy = pol;
		c.startDate = start;
		c.repID = rep;
		return c;
	}
	
	private Cache(int cacheSize, CacheReplacement pol, String start, int rep)
	{
		size = cacheSize;
		policy = pol;
		startDate = start;
		repID = rep;
	}
	
	
	public boolean isFull()
	{	  
		int currentCacheSize = cacheTable.size();
		assert(currentCacheSize<=size); // TODO: debug interaction
		return currentCacheSize == size;
	}
	
	// XXX: load a chunk at a time??
	public void load(CacheItem cacheItem)
	{
		int entityId = cacheItem.getEntityId();
		if (isFull())
			bumpOutItem();
		cacheTable.put(entityId, cacheItem);
	}
	
	public void add(int eid, int cid, String cdate, CacheReason reason){
		// XXX move hit/miss logic here? 
		if (cacheTable.containsKey(eid))
			cacheTable.get(eid).update(cid, cdate, startDate);
		else
			load (new CacheItem(eid, cid, cdate, reason));
	}
	
	
	public void add(ArrayList<Integer> eids, int cid, String cdate, CacheReason reas){
		for (int eid: eids)
			add(eid, cid, cdate, reas);
		
	}
	
	public void remove(int fileid){
		cacheTable.remove(fileid);
	}

	public void bumpOutItem()
	{
		// figure out what to remove with cache replacement policy
		// iterate through the map and find the minimum element, given the cache replacement policy
		// then remove that element
		// TODO: keep cache always sorted using cache replacement policy
		
		CacheItem min = null;
		
		for (CacheItem c : cacheTable.values()){
			if (min == null)
				min = c;
			else 
				min = policy.minimum(min, c);				
		}
		
		int entityId = min.getEntityId();
		cacheTable.remove(entityId);
	}
	
	public void bumpOutItem(int numItems)
	{
		for (int i = 0; i < numItems; ++i)
			bumpOutItem();
	}
	
	public CacheItem getCacheItem(int entityId)
	{
		return cacheTable.get(entityId);
	}
	
	public CacheReplacement.Policy getPolicy(){
		return policy.currentPolicy;
	}
	
	public String getStartDate(){
		return startDate;
	}
	
	public int getRepID(){
		return repID;
	}

	public int getCacheSize()
	{
		return cacheTable.size();
	}
	
	// For debugging
	public ArrayList<CacheItem> getCacheItemList()
	{
		ArrayList<CacheItem> CIList = new ArrayList<CacheItem>();
		Iterator<CacheItem> it = cacheTable.values().iterator();
		while(it.hasNext())
		{
			CIList.add(it.next());
		}
		return CIList;
	}
	
}
