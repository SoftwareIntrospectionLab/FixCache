package Cache;

import java.util.Comparator;

public class ComparatorChanges implements Comparator<CacheItem> {

    public int compare(CacheItem c1, CacheItem c2) {
        return c1.getNumberOfChanges() - c2.getNumberOfChanges();
    }

}