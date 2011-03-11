package edu.ucsc.sil.fixcache.cache;

import java.util.Comparator;

public class CacheReplacement {

    /**
     * In order to add a new Cache Replacement policy, code should be added to
     * 1. The 'Policy' enum and the CacheReplacement constructor (see below)
     * 
     * 2. A new Comparator<CacheItem> is probably needed. (see ComparatorLRU.java)
     * 
     * 3. If the policy uses information that is not currently being tracked, new
     * fields may be needed in CacheItem.java and set/modified in the update() method
     * (see findNumberOfAuthors()) Alternatively, the 'number' field may be overloaded
     * (see findNumber()).
     */
    public static enum Policy {
        LRU, BUGS, CHANGES, AUTHORS
    };
    static final Policy REPDEFAULT = Policy.LRU;
    
    Policy currentPolicy;
    protected Comparator<CacheItem> compareFunc;
    protected ComparatorLRU tiebreaker = new ComparatorLRU();

    public CacheReplacement(Policy p) {
        currentPolicy = p;

        switch (p) {
        case BUGS:
            compareFunc = new ComparatorBugs();
            break;
        case CHANGES:
            compareFunc = new ComparatorChanges();
            break;
        case AUTHORS:
            compareFunc = new ComparatorAuthors();
            break;
        case LRU:
            compareFunc = new ComparatorLRU();
        }
    }

    /**
     * Compares two cache items using the cache replacement policy and returns the minimum
     * @param o1 
     * @param o2
     * @return the minimum CacheItem
     */
    public CacheItem minimum(CacheItem o1, CacheItem o2) {
        int comparison = compareFunc.compare(o1, o2);
        if (comparison == 0) {
            assert (tiebreaker.compare(o1, o2) != 0);
            if (tiebreaker.compare(o1, o2) < 0)
                return o1;
            else
                return o2;
        } else if (comparison < 0)
            return o1;
        else
            return o2;
    }

}
