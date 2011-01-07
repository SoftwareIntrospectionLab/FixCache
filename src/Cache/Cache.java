package Cache;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import Cache.CacheItem.CacheReason;

public class Cache {
	
	// Invariant: cacheTable.size() <= size;
	
	final int size;
	final Hashtable<Integer, CacheItem> cacheTable = new Hashtable<Integer, CacheItem>();
	final CacheReplacement policy;
	String startDate;
	
	public Cache(int cacheSize, CacheReplacement pol, String start)
	{
		this.size = cacheSize;
		policy = pol;
		startDate = start;
	}
	
	
	public boolean isFull()
	{	  
		int currentCacheSize = cacheTable.size();
		assert(currentCacheSize<=size); // TODO: debug interaction
		return currentCacheSize == size;
	}
	
	// TODO: load a chunk at a time??
	public void load(CacheItem cacheItem)
	{
		int entityId = cacheItem.getEntityId();
		if (isFull())
			bumpOutItem();
		cacheTable.put(entityId, cacheItem);
	}
	
	public void add(int eid, String cdate, CacheReason reas){
		// XXX move hit/miss logic here? 
		if (cacheTable.containsKey(eid))
			cacheTable.get(eid).update(reas, cdate, startDate);
		else
			load (new CacheItem(eid, cdate, reas, startDate));
	}
	
	
	public void add(ArrayList<Integer> eids, String cdate, CacheReason reas){
		for (int eid: eids)
			add(eid, cdate, reas);
		
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
	

	public int getCacheSize()
	{
		return cacheTable.size();
	}
	
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
