package Cache;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;

public class N_Cache {
	
	int size;
	Hashtable cacheTable = new Hashtable();
	
	public N_Cache(int cacheSize)
	{
		this.size = cacheSize;
	}
	
	
	public boolean isFull()
	{
		int currentCacheSize = cacheTable.size();
		if (currentCacheSize > size) {

			System.err.println("Cache overflow " + currentCacheSize + "/"
					+ size);
			System.exit(0);
		}
		return currentCacheSize == size;
	}
	
	public void load(N_CacheItem cacheItem)
	{
		String entityId = cacheItem.getEntityId();
		cacheTable.put(entityId, cacheItem);
	}

	public void unload(String entityId)
	{
		cacheTable.remove(entityId);
	}
	
	public void unload(N_CacheItem cacheItem)
	{
		String entityId = cacheItem.getEntityId();
		cacheTable.remove(entityId);
		
		
	}
	
	public N_CacheItem getCacheItem(String entityId)
	{
		return (N_CacheItem)cacheTable.get(entityId);
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
