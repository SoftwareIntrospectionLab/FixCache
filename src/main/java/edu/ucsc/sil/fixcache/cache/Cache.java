package edu.ucsc.sil.fixcache.cache;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import edu.ucsc.sil.fixcache.cache.CacheItem.CacheReason;

import edu.ucsc.sil.fixcache.util.Dates;

public class Cache implements Iterable<CacheItem>{

    /**
     *  Invariant: cacheTable.size() <= size;
     */

    /**
     * Fields 
     */
    final int maxsize; // cache size parameter
    private int size = 0; // current size of cache

    // keeps every cacheitem that was ever in the cache
    private Hashtable<Integer, CacheItem> cacheTable = new Hashtable<Integer, CacheItem>();

    final private CacheReplacement policy;
    final String startDate; 
    final String endDate;
    final int repID;

    // counter, used to decide which cacheitem is LRU
    private int time = 0;
    private int currcommit = -1;

    private int addCount = 0;
    private int numNewItems = 0;
    
    /**
     * 
     * @param cacheSize
     * @param pol
     * @param start
     * @param end
     * @param rep
     */
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
    
    // used in file distribution output
    @Override
    public Iterator<CacheItem> iterator() {
        List<CacheItem> inCacheList = new ArrayList<CacheItem>();
        
        for (CacheItem ci : cacheTable.values()) {
            inCacheList.add(ci);
        }
        
        return inCacheList.iterator();
    }
    
    public Iterator<CacheItem> allCacheValues() {
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
        int fileId = cacheItem.getFileId();
        if (isFull())
            bumpOutItem(cdate);
        if (!cacheTable.contains(cacheItem))
            cacheTable.put(fileId, cacheItem);
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
    public void add(int fileId, int cid, String cdate, CacheReason reason) {
        if (cacheTable.containsKey(fileId)){ // either in cache or was bumped out
            CacheItem ci = cacheTable.get(fileId);
            if (!ci.isInCache()){
                load(ci, cdate);
                addCount++;
            }
            ci.update(cid, cdate, startDate, reason); // updates inCache status
        } else { // need to create a new CacheItem
            load(new CacheItem(fileId, cid, cdate, reason, this), cdate);
            addCount++;
            numNewItems++;
        }
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
        return min.getFileId();
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

    public int getTime(int cid) {
        if (cid == currcommit) return time;
        currcommit = cid;
        return time++;
    }
    
    public int getTotalDuration(){
        return Dates.getMinuteDuration(startDate, endDate);
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
    

    int getLoc(int fileId){
        CacheItem ci = cacheTable.get(fileId);
        if (ci == null)
            return -1;
        return ci.getLOC();
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
    
    
    public boolean neverInCache(int entityId){
        return (cacheTable.get(entityId) == null);
    }

    public int getNumber(String fileid) {
        CacheItem ci = cacheTable.get(fileid);
        return ci.getNumber();
    }

    public int getLoadCount(int fileId){
        CacheItem ci = cacheTable.get(fileId);
        return ci.getLoadCount();
    }
    
    @Override
    public String toString(){
        StringBuilder s = new StringBuilder();
        s.append("[");
        for (CacheItem ci: this){
            if (ci.isInCache()) {
                s.append(ci.getFileId());
            	s.append(",");
            }
        }
        s.append("]");
        return s.toString();
    }

}
