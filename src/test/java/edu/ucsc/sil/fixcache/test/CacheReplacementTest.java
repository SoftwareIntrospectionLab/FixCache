package edu.ucsc.sil.fixcache.test;

import static org.junit.Assert.*;
import java.sql.Connection;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import edu.ucsc.sil.fixcache.cache.Cache;
import edu.ucsc.sil.fixcache.cache.CacheReplacement;
import edu.ucsc.sil.fixcache.cache.CacheItem.CacheReason;
import edu.ucsc.sil.fixcache.util.TestHelper;

@RunWith(JUnit4.class)
public class CacheReplacementTest {

    private static Connection conn;

    @After
    public void tearDownAfterClass() throws Exception {
        TestHelper.cleanDatabase();
        if (conn != null) {
            conn.close();
        }
    }

    @Before
    public void setUp() throws Exception {
        TestHelper.handleSetUpOperation();
    }

    @Test
    public void testCacheReplacementAuthors() {
        Cache cache = new Cache(5, new CacheReplacement(
                CacheReplacement.Policy.AUTHORS), "2009-10-20 01:32:19", "2010-01-01 01:01:01",1);
        cache.add(4, 1, "2009-10-20 01:32:19", CacheReason.NewEntity);
        cache.add(3, 1, "2009-10-20 01:32:19", CacheReason.NewEntity);
        cache.add(1, 1, "2009-10-20 01:32:19", CacheReason.NewEntity);
        cache.add(4, 2, "2009-10-20 14:37:38", CacheReason.BugEntity);
        cache.add(1, 2, "2009-10-20 14:37:38", CacheReason.BugEntity);
        cache.add(1, 3, "2009-10-20 14:37:47", CacheReason.NewEntity);
        cache.add(5, 3, "2009-10-20 14:37:47", CacheReason.BugEntity);
        cache.remove(4, "2009-10-20 14:37:47");
        cache.add(1, 5, "2009-10-23 14:10:37", CacheReason.BugEntity);
        cache.add(6, 6, "2009-10-23 14:29:05", CacheReason.NewEntity);
        cache.add(7, 6, "2009-10-23 14:29:05", CacheReason.NewEntity);
        cache.add(3, 7, "2009-10-23 20:01:52", CacheReason.BugEntity);
        cache.add(8, 8, "2009-10-24 07:51:22", CacheReason.NewEntity);
        assertNull(cache.getCacheItem(5) );
        cache.add(1, 8, "2009-10-24 07:51:22", CacheReason.ModifiedEntity);
        cache.add(5, 9, "2009-10-24 09:50:26", CacheReason.ModifiedEntity);
        assertNull(cache.getCacheItem(6) );
    }

    @Test
    public void testCacheReplacementLRU() {
        // add stuff to the cache, without bumping out anything
        Cache cache = new Cache(5, new CacheReplacement(
                CacheReplacement.Policy.LRU), "2009-10-20 01:32:19", "2010-01-01 01:01:01",1);
        cache.add(1, 1, "2009-10-20 01:32:19", CacheReason.NewEntity);
        cache.add(3, 1, "2009-10-20 01:32:19", CacheReason.NewEntity);
        cache.add(4, 1, "2009-10-20 01:32:19", CacheReason.NewEntity);
        cache.add(1, 2, "2009-10-20 14:37:38", CacheReason.BugEntity); // LRU
        cache.add(4, 2, "2009-10-20 14:37:38", CacheReason.BugEntity); // LRU
        assertEquals(3, cache.getCacheSize());
        cache.add(5, 3, "2009-10-20 14:37:47", CacheReason.NewEntity); // LRU 3, 5, 2, 4, 1, 3
        cache.add(1, 3, "2009-10-20 14:37:47", CacheReason.BugEntity); // 1,
        assertEquals(4, cache.getCacheSize());
        cache.remove(4, "2009-10-20 14:37:47"); // 1, 5, 2, 3
        assertEquals(3, cache.getCacheSize());
        cache.add(1, 5, "2009-10-23 14:10:37", CacheReason.BugEntity); // 1,
        assertEquals(3, cache.getCacheSize());
        cache.add(6, 6, "2009-10-23 14:29:05", CacheReason.NewEntity); // 6,
        assertEquals(4, cache.getCacheSize());
        assertEquals(1, cache.getLoadCount(3));

        assertEquals(3, cache.getMinimum());
        cache.add(7, 6, "2009-10-23 14:29:05", CacheReason.NewEntity); // 7,
        assertEquals(5, cache.getCacheSize());
        cache.add(4, 1, "2009-10-20 01:32:19", CacheReason.NewEntity);
        assertEquals(5, cache.getCacheSize());
        assertNull(cache.getCacheItem(3) );

        cache.add(3, 7, "2009-10-23 20:01:52", CacheReason.BugEntity); // 3,
        assertEquals(2, cache.getLoadCount(3));
        assertNull(cache.getCacheItem(5) );
        cache.add(8, 8, "2009-10-24 07:51:22", CacheReason.NewEntity); // 8,
        assertNull(cache.getCacheItem(1) );
        cache.add(1, 8, "2009-10-24 07:51:22", CacheReason.ModifiedEntity); // 1,
        assertNull(cache.getCacheItem(2) );
        cache.add(5, 9, "2009-10-24 09:50:26", CacheReason.ModifiedEntity);
        assertNull(cache.getCacheItem(6) );
    }

    @Test
    public void testCacheReplacementChanges() {
        Cache cache = new Cache(5, new CacheReplacement(
                CacheReplacement.Policy.CHANGES), "2009-10-20 01:32:19.0", "2010-01-01 01:01:01.0",1);
        cache.add(4, 1, "2009-10-20 01:32:19", CacheReason.NewEntity);
        cache.add(3, 1, "2009-10-20 01:32:19", CacheReason.NewEntity);
        cache.add(1, 1, "2009-10-20 01:32:19", CacheReason.NewEntity);
        cache.add(4, 2, "2009-10-20 14:37:38", CacheReason.BugEntity);
        cache.add(1, 2, "2009-10-20 14:37:38", CacheReason.BugEntity);
        cache.add(1, 3, "2009-10-20 14:37:47", CacheReason.NewEntity);
        cache.add(5, 3, "2009-10-20 14:37:47", CacheReason.BugEntity);
        cache.remove(4, "2009-10-20 14:37:47");
        cache.add(1, 5, "2009-10-23 14:10:37", CacheReason.BugEntity);
        cache.add(6, 6, "2009-10-23 14:29:05", CacheReason.NewEntity);
        cache.add(7, 6, "2009-10-23 14:29:05", CacheReason.NewEntity);
        cache.add(3, 7, "2009-10-23 20:01:52", CacheReason.BugEntity);
        cache.add(8, 8, "2009-10-24 07:51:22", CacheReason.NewEntity);
        assertNull(cache.getCacheItem(5) );
        cache.add(1, 8, "2009-10-24 07:51:22", CacheReason.ModifiedEntity);
        cache.add(5, 9, "2009-10-24 09:50:26", CacheReason.ModifiedEntity);
        assertNull(cache.getCacheItem(6) );
    }

    @Test
    public void testCacheReplacementBugs() {
        Cache cache = new Cache(5, new CacheReplacement(
                CacheReplacement.Policy.BUGS), "2009-10-20 01:32:19.0", "2010-01-01 01:01:01.0",1);

        cache.add(4, 1, "2009-10-20 01:32:19", CacheReason.NewEntity);
        cache.add(3, 1, "2009-10-20 01:32:19", CacheReason.NewEntity);
        cache.add(1, 1, "2009-10-20 01:32:19", CacheReason.NewEntity);
        cache.add(4, 2, "2009-10-20 14:37:38", CacheReason.BugEntity);
        cache.add(1, 2, "2009-10-20 14:37:38", CacheReason.BugEntity);
        cache.add(1, 3, "2009-10-20 14:37:47", CacheReason.NewEntity);
        cache.add(5, 3, "2009-10-20 14:37:47", CacheReason.BugEntity);
        cache.remove(4, "2009-10-20 14:37:47");
        cache.add(1, 5, "2009-10-23 14:10:37", CacheReason.BugEntity);
        cache.add(6, 6, "2009-10-23 14:29:05", CacheReason.NewEntity);
        cache.add(7, 6, "2009-10-23 14:29:05", CacheReason.NewEntity);
        cache.add(3, 7, "2009-10-23 20:01:52", CacheReason.BugEntity);
        cache.add(8, 8, "2009-10-24 07:51:22", CacheReason.NewEntity);
        assertNull(cache.getCacheItem(5) );
        cache.add(1, 8, "2009-10-24 07:51:22", CacheReason.ModifiedEntity);
        cache.add(5, 9, "2009-10-24 09:50:26", CacheReason.ModifiedEntity);
        assertNull(cache.getCacheItem(8) );
    }

}