package edu.ucsc.sil.fixcache.cache;

import java.util.Comparator;

public class ComparatorAuthors implements Comparator<CacheItem> {

    public int compare(CacheItem c1, CacheItem c2) {
        return c1.getNumberOfAuthors() - c2.getNumberOfAuthors();
    }

}
