package Cache;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

public class Cache {
	
	// Invariant: cacheTable.size() <= size;
	
	final int size;
	final Hashtable cacheTable = new Hashtable();
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
		String entityId = cacheItem.getEntityId();
		if (isFull())
			bumpOutItem();
		cacheTable.put(entityId, cacheItem);
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
		String entityId = min.getEntityId();
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
