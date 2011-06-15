package edu.ucsc.sil.fixcache.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.sql.Connection;
import java.util.ArrayList;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import edu.ucsc.sil.fixcache.cache.Cache;
import edu.ucsc.sil.fixcache.cache.CacheReplacement;
import edu.ucsc.sil.fixcache.cache.CoChange;
import edu.ucsc.sil.fixcache.util.TestHelper;

@RunWith(JUnit4.class)
public class CochangeTest {

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
    public void testCoChange() {
        Cache cache = new Cache(1, new CacheReplacement(CacheReplacement.Policy.LRU), "","", 1);
        ArrayList<String> cochanges1 = CoChange.getCoChangeFileList("/foo/bar/e.java", "2009-10-20 01:32:19",
                "2009-10-24 09:50:26.0", 3, 1, cache);
        assertEquals(cochanges1.size(), 1);
        assertTrue(cochanges1.contains("/foo/bar/a.java"));
        ArrayList<String> cochanges2 = CoChange.getCoChangeFileList("/foo/bar/a.java","2009-10-20 01:32:19",
                "2009-10-24 07:51:22.0", 4, 1, cache);
        assertEquals(cochanges2.size(), 3);
        assertTrue(cochanges2.contains("/foo/bar/d.java"));
        assertTrue(cochanges2.contains("/foo/bar/h.java"));
        assertTrue(cochanges2.contains("/foo/bar/e.java"));
        ArrayList<String> cochanges3 = CoChange.getCoChangeFileList("/foo/bar/g.java","2009-10-20 01:32:19",
                "2009-10-23 14:29:05.0", 5, 1, cache);
        assertEquals(cochanges3.size(), 1);
        assertTrue(cochanges3.contains("/foo/bar/f.java"));
    }

}
