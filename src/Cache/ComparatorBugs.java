package Cache;

import java.util.Comparator;

public class ComparatorBugs implements Comparator<CacheItem> {

    public int compare(CacheItem c1, CacheItem c2) {
        return c1.getNumberOfBugs() - c2.getNumberOfBugs();
    }

}