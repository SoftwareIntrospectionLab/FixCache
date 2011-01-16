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
    String endDate;
    int repID;

    // counter, used to decide which cacheitem is LRU
    private int time = 0;

    public Cache(int cacheSize, CacheReplacement pol, String start, String end, int rep) {
        maxsize = cacheSize;
        policy = pol;
        startDate = start;
        endDate = end;
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
        assert (size <= maxsize);
        return size == maxsize;
    }

    /**
     * Loads an item into the cache, removing an item if full
     * The only method where size is increased.
     * @param cacheItem
     */
    // XXX: load a chunk at a time??
    public void load(CacheItem cacheItem, String cdate) {
        int entityId = cacheItem.getEntityId();
        if (isFull())
            bumpOutItem(cdate);
        if (!cacheTable.contains(cacheItem))
            cacheTable.put(entityId, cacheItem);
        size++;
    }

    /**
     * Removes an item from the cache by switching the 'inCache' boolean to false.
     * The only method that decreases size.
     * @param fileid 
     */
    public void remove(int fileid, String cdate) {
        cacheTable.get(fileid).removeFromCache(cdate);
        size--;
    }

    /**
     * Called on cache hit and cache miss.
     * Adds an item into the cache, if not already there.
     * If already there, updates the cache entry.
     * @param eid -- entity id
     * @param cid -- commit id
     * @param cdate -- commit date
     * @param reason -- reason for adding to the cache
     */
    public void add(int eid, int cid, String cdate, CacheReason reason) {
        if (cacheTable.containsKey(eid)){ // either in cache or was bumped out
            CacheItem ci = cacheTable.get(eid);
            if (!ci.isInCache()){
                load(ci, cdate);
            }
            ci.update(cid, cdate, startDate); // updates inCache status
        } else { // need to create a new CacheItem
            load(new CacheItem(eid, cid, cdate, reason, this), cdate);
        }
    }

    /**
     * Wrapper for add to add in bulk
     * @param eids -- entityid list
     * @param cid -- commitid
     * @param cdate -- commit date
     * @param reason -- reason for adding to the cache
     */
    public void add(ArrayList<Integer> eids, int cid, String cdate, CacheReason reas) {
        for (int eid : eids)
            add(eid, cid, cdate, reas);
    }


    /**
     *  Figures out what to remove with cache replacement policy.
     *   iterates through the map and find the minimum element (given the cache
     *   replacement policy) then removes that element
     */
    // TODO: keep cache always sorted using cache replacement policy
    public void bumpOutItem(String cdate) {
        int entityId = getMinimum();
        remove(entityId, cdate);
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

    /**
     * Wrapper for bumping out multiple items at once.
     * @param numItems
     */
    public void bumpOutItem(int numItems, String cdate) {
        for (int i = 0; i < numItems; ++i)
            bumpOutItem(cdate);
    }
    

    /**
     * Getters
     */


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

    
    /**
     * Returns either the CacheItem associated with entityId, if in the cache
     * or null if it is not in the cache.
     */
    public CacheItem getCacheItem(int entityId) {
        CacheItem ci = cacheTable.get(entityId);
        if (ci == null)
            return null;
        if (!ci.isInCache())
            return null;
        return ci;
    }

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
