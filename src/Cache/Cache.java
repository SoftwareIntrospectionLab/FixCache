package Cache;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;

import Cache.CacheItem.CacheReason;

public class Cache {
	
	// Invariant: cacheTable.size() <= size;
	
	final int size;
	final Hashtable<Integer, CacheItem> cacheTable = new Hashtable();
	// List<Integer> files;
	final CacheReplacement policy;
	
	public Cache(int cacheSize, CacheReplacement pol)
	{
		this.size = cacheSize;
		policy = pol;
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
	
	public void add(int eid, int cid, CacheReason reas){
		// if reas == BugEntity
			// if it is already in the cache, register a hit
			// else register a fault
		if (cacheTable.containsKey(eid))
			cacheTable.get(eid).update(reas, cid);
		else
			load (new CacheItem(eid, cid, reas));
	}
	
	
	public void add(ArrayList<Integer> eids, int cid, CacheReason reas){
		for (int eid: eids)
			add(eid, cid, reas);
		
	}
	
	public void remove(int fileid){
		cacheTable.remove(fileid);
	}

	public void bumpOutItem()
	{
		// figure out what to remove with cache replacement policy
		// iterate through the map and find the minimum element, given the cache replacement policy
		// then remove that element
		
		CacheItem min = null;
		
		for (Object o : cacheTable.values()){
			CacheItem c = (CacheItem) o;
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
	
	public CacheItem getCacheItem(String entityId)
	{
		return (CacheItem)cacheTable.get(entityId);
	}
	
	/**
	 * Make cached item to List
	 * 
	 * @return
	 */
	public ArrayList toList() {
		ArrayList cacheList = new ArrayList();
		for (Iterator it = cacheTable.values().iterator(); it.hasNext(); cacheList.add(it.next()))
			;

		return cacheList;
	}
}
