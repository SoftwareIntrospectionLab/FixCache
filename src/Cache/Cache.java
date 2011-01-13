package Cache;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import Cache.CacheItem.CacheReason;

public class Cache {

    /**
     *  Invariant: cacheTable.size() <= size;
     */
    
    /**
     * Fields 
     */
    private final int maxsize; 
    private int size = 0; // current size of cache
    
    // keeps every cacheitem that was ever in the cache
    private Hashtable<Integer, CacheItem> cacheTable = new Hashtable<Integer, CacheItem>();
    
    private CacheReplacement policy;
    String startDate;
    int repID;
    
    // counter, used to decide which cacheitem is LRU
    private int time = 0;

    public Cache(int cacheSize, CacheReplacement pol, String start, int rep) {
        maxsize = cacheSize;
        policy = pol;
        startDate = start;
        repID = rep;
    }

    /**
     * Methods
     */
    
    /**
     * Compares the current cache size with the maximum allowable
     * @return whether the cache is at full capacity
     */
    public boolean isFull() {
        int currentCacheSize = size;
        assert (currentCacheSize <= maxsize);
        return currentCacheSize == maxsize;
    }

    /**
     * Loads an item into the cache, removing an item if full
     * @param cacheItem
     */
    // XXX: load a chunk at a time??
    public void load(CacheItem cacheItem) {
        int entityId = cacheItem.getEntityId();
        if (isFull())
            bumpOutItem();
        if (!cacheTable.contains(cacheItem))
            cacheTable.put(entityId, cacheItem);
        size++;
    }
    
    public void remove(int fileid) {
        cacheTable.get(fileid).removeFromCache();
        size--;
    }


    public void add(int eid, int cid, String cdate, CacheReason reason) {
        if (cacheTable.containsKey(eid)){
            CacheItem ci = cacheTable.get(eid);
            if (!ci.isInCache()){
                load(ci);
            }
            ci.update(cid, cdate, startDate);
        } else {
            load(new CacheItem(eid, cid, cdate, reason, this));
        }
    }

    public void add(ArrayList<Integer> eids, int cid, String cdate,
            CacheReason reas) {
        for (int eid : eids)
            add(eid, cid, cdate, reas);
    }


    // figures out what to remove with cache replacement policy
    // iterates through the map and find the minimum element (given the cache
    // replacement policy) then removes that element
    // TODO: keep cache always sorted using cache replacement policy
    public void bumpOutItem() {
        int entityId = getMinimum();
        remove(entityId);
    }

    public int getMinimum() {
        CacheItem min = null;

        for (CacheItem c : cacheTable.values()) {
            if (!c.isInCache()) continue;
            
            if (min == null)
                min = c;
            else
                min = policy.minimum(min, c);
        }
        return min.getEntityId();
    }

    public void bumpOutItem(int numItems) {
        for (int i = 0; i < numItems; ++i)
            bumpOutItem();
    }
    
    /**
     * Getters
     */

    
    public CacheItem getCacheItem(int entityId) {
        CacheItem ci = cacheTable.get(entityId);
        if (ci == null)
            return null;
        if (!ci.isInCache())
            return null;
        return cacheTable.get(entityId);
    }

    public CacheReplacement.Policy getPolicy() {
        return policy.currentPolicy;
    }

    public String getStartDate() {
        return startDate;
    }

    public int getRepID() {
        return repID;
    }

    public int getCacheSize() {
        return size;
    }
    
    /**
     * Checks the cache table for whether a particular entity is in the cache
     * @param eid -- entity id
     * @return whether that entity is in the cache
     */
    public boolean contains(int eid){
        final CacheItem ci = cacheTable.get(eid);
        if (ci == null)
            return false;
        else // in cacheTable
            return ci.isInCache();
    }
    
    
    /**
     *  Methods for debugging
     */
    
    public ArrayList<CacheItem> getCacheItemList() {
        ArrayList<CacheItem> CIList = new ArrayList<CacheItem>();
        Iterator<CacheItem> it = cacheTable.values().iterator();
        while (it.hasNext()) {
            CIList.add(it.next());
        }
        return CIList;
    }

    public int getNumber(int fileid) {
        CacheItem ci = cacheTable.get(fileid);
        return ci.getNumber();
    }
    
    public int getLoadCount(int fileid){
        CacheItem ci = cacheTable.get(fileid);
        return ci.getLoadCount();
    }

    public int getTime() {
        return time++;
    }

}
