package edu.ucsc.sil.fixcache.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.sql.Connection;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import edu.ucsc.sil.fixcache.cache.Cache;
import edu.ucsc.sil.fixcache.cache.CacheReplacement;
import edu.ucsc.sil.fixcache.cache.CacheItem.CacheReason;
import edu.ucsc.sil.fixcache.util.TestHelper;

public class CacheTest {

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
    public void testCacheAdd() {
        Cache cache = new Cache(5, new CacheReplacement(
                CacheReplacement.Policy.AUTHORS), "2009-10-20 01:32:19.0", "2010-01-01 01:01:01.0",1);
        cache.add("a.java", 1, "2009-10-20 01:32:19.0", CacheReason.NewEntity);
        assertEquals(cache.getCacheSize(), 1);
        cache.add("g.java", 1, "2009-10-20 01:32:19.0", CacheReason.NewEntity);
        assertEquals(cache.getCacheSize(), 2);
        cache.add("c.java", 1, "2009-10-20 01:32:19.0", CacheReason.NewEntity);
        assertEquals(cache.getCacheSize(), 3);
        cache.add("d.java", 1, "2009-10-20 01:32:19.0", CacheReason.NewEntity);
        assertEquals(cache.getCacheSize(), 4);
        cache.add("a.java", 2, "2009-10-20 14:37:38.0", CacheReason.ModifiedEntity);
        assertEquals(cache.getCacheSize(), 4);
        cache.add("d.java", 2, "2009-10-20 14:37:38.0", CacheReason.ModifiedEntity);
        assertEquals(cache.getCacheSize(), 4);
        cache.add("g.java", 3, "2009-10-20 14:37:47.0", CacheReason.BugEntity);
        assertEquals(cache.getCacheSize(), 4);
        cache.add("e.java", 3, "2009-10-20 14:37:47.0", CacheReason.BugEntity);
        assertTrue(cache.isFull());
        assertEquals(1, cache.getLoadCount("a.java"));
    }

}