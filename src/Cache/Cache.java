package Cache;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import Cache.CacheItem.CacheReason;

public class Cache implements Iterable<CacheItem>{

    /**
     *  Invariant: cacheTable.size() <= size;
     */

    /**
     * Fields 
     */
    final int maxsize; 
    private int size = 0; // current size of cache

    // keeps every cacheitem that was ever in the cache
    private Hashtable<String, CacheItem> cacheTable = new Hashtable<String, CacheItem>();

    private CacheReplacement policy;
    String startDate; 
    String endDate;
    int repID;

    // counter, used to decide which cacheitem is LRU
    private int time = 0;

    private int addCount = 0;
    private int numNewItems = 0;
    
    public Cache(int cacheSize, CacheReplacement pol, String start, String end, int rep) {
        assert(start !=null);
        assert(end != null);
        maxsize = cacheSize;
        policy = pol;
        startDate = start;
        endDate = end;
        repID = rep;
    }

    /**
     * Methods
     */
    
    @Override
    public Iterator<CacheItem> iterator() {
        return cacheTable.values().iterator();
    }

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
        String fileName = cacheItem.getFileName();
        if (isFull())
            bumpOutItem(cdate);
        if (!cacheTable.contains(cacheItem))
            cacheTable.put(fileName, cacheItem);
        size++;
    }

    /**
     * Removes an item from the cache by switching the 'inCache' boolean to false.
     * The only method that decreases size.
     * @param fileid 
     */
    public void remove(String fileid, String cdate) {
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
    public void add(String fileName, int cid, String cdate, CacheReason reason) {
        if (cacheTable.containsKey(fileName)){ // either in cache or was bumped out
            CacheItem ci = cacheTable.get(fileName);
            if (!ci.isInCache()){
                load(ci, cdate);
                addCount++;
            }
            ci.update(cid, cdate, startDate, reason); // updates inCache status
        } else { // need to create a new CacheItem
            load(new CacheItem(fileName, cid, cdate, reason, this), cdate);
            addCount++;
            numNewItems++;
        }
    }

    /**
     * Wrapper for add to add in bulk
     * @param eids -- entityid list
     * @param cid -- commitid
     * @param cdate -- commit date
     * @param reason -- reason for adding to the cache
     */
    public void add(ArrayList<String> fileNames, int cid, String cdate, CacheReason reas) {
        for (String fName : fileNames)
            add(fName, cid, cdate, reas);
    }


    /**
     *  Figures out what to remove with cache replacement policy.
     *   iterates through the map and find the minimum element (given the cache
     *   replacement policy) then removes that element
     */
    // TODO: keep cache always sorted using cache replacement policy
    public void bumpOutItem(String cdate) {
        String entityId = getMinimum();
        remove(entityId, cdate);
    }

    public String getMinimum() {
        CacheItem min = null;

        for (CacheItem c : cacheTable.values()) {
            if (!c.isInCache()) continue;

            if (min == null)
                min = c;
            else
                min = policy.minimum(min, c);
        }
        return min.getFileName();
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

    public int getTime() {
        return time++;
    }
    
    public int getTotalDuration(){
        return Util.Dates.getMinuteDuration(startDate, endDate);
    }

    
    /**
     * Checks the cache table for whether a particular entity is in the cache
     * @param eid -- entity id
     * @return whether that entity is in the cache
     */
    public boolean contains(String eid){
        final CacheItem ci = cacheTable.get(eid);
        if (ci == null)
            return false;
        else // in cacheTable
            return ci.isInCache();
    }

    /**
     * These two methods reset within-slice counts at slice boundaries
     * @return the in-slice count
     */
    public int resetAddCount() {
        int oldAdds = addCount;
        addCount = 0;
        return oldAdds;
    }
    public int resetCICount() {
        int oldcis = numNewItems;
        numNewItems = 0;
        return oldcis;
    }
    

    
    /**
     *  Methods for debugging
     */

    
    /**
     * Returns either the CacheItem associated with entityId, if in the cache
     * or null if it is not in the cache.
     */
    public CacheItem getCacheItem(String entityId) {
        CacheItem ci = cacheTable.get(entityId);
        if (ci == null)
            return null;
        if (!ci.isInCache())
            return null;
        return ci;
    }
    
    
    public boolean neverInCache(String entityId){
        return (cacheTable.get(entityId) == null);
    }

    public int getNumber(String fileid) {
        CacheItem ci = cacheTable.get(fileid);
        return ci.getNumber();
    }

    public int getLoadCount(String fileName){
        CacheItem ci = cacheTable.get(fileName);
        return ci.getLoadCount();
    }

}
