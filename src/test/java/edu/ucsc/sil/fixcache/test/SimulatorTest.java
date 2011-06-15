package edu.ucsc.sil.fixcache.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.sql.Connection;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import edu.ucsc.sil.fixcache.cache.Cache;
import edu.ucsc.sil.fixcache.cache.CacheItem;
import edu.ucsc.sil.fixcache.cache.CacheReplacement;
import edu.ucsc.sil.fixcache.cache.InputManager;
import edu.ucsc.sil.fixcache.cache.Simulator;
import edu.ucsc.sil.fixcache.util.TestHelper;

@RunWith(JUnit4.class)
public class SimulatorTest {

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
    public void testInitialPreLoad() {

        InputManager in = new InputManager(2,2,5,1, CacheReplacement.Policy.BUGS);
        in.setStartDate("2009-10-20 01:32:19");

        Simulator sim1 = new Simulator(in);
        sim1.initialPreLoad();
        assertEquals(3, sim1.getCache().getCacheSize()); // only 3 files in inital commit
        assertTrue(sim1.getCache().contains("/foo/bar/d.java"));
        assertTrue(sim1.getCache().contains("/foo/bar/c.java"));
        sim1.closeStatement();

        in.setStartDate("2009-10-20 14:37:47");
        Simulator sim2 = new Simulator(in);
        sim2.initialPreLoad();
        assertEquals(4, sim2.getCache().getCacheSize());
        assertTrue(sim2.getCache().contains("/foo/bar/a.java"));
        assertTrue(sim2.getCache().contains("/foo/bar/e.java"));
        sim2.closeStatement();


        in.setStartDate("2009-10-23 09:50:25");
        Simulator sim3 = new Simulator(in);
        sim3.initialPreLoad();
        assertEquals(4, sim3.getCache().getCacheSize());
        assertTrue(sim3.getCache().contains("/foo/bar/a.java"));
        sim3.closeStatement();


    }

    @Test
    public void testVersionPreLoad() {
        InputManager in = new InputManager(2,2,5,1, CacheReplacement.Policy.BUGS);

        in.setStartDate("2009-10-20 01:32:19");
        Simulator sim = new Simulator(in);
        Cache cache = sim.getCache();
        sim.initialPreLoad();
        assertEquals(cache.getCacheSize(), 3);
        sim.add("/foo/bar/a.java", 1, "2009-10-20 01:32:19", CacheItem.CacheReason.NewEntity);
        assertEquals(cache.getCacheSize(), 3);
        sim.add("/foo/bar/c.java", 1, "2009-10-20 01:32:19", CacheItem.CacheReason.NewEntity);
        assertEquals(cache.getCacheSize(), 3);
        sim.closeStatement();
    }

    @Test
    public void testGetBugIntroCdate() {
        InputManager in = new InputManager(2,2,5,1, CacheReplacement.Policy.BUGS);

        in.setStartDate("2009-10-20 01:32:19");
        Simulator sim = new Simulator(in);
        // XXX no longer finds maximum bug introducing date
 //       assertEquals(sim.getBugIntroCdate("h.java", 10), "2009-10-23 14:29:05.0");
//        assertEquals(sim.getBugIntroCdate("e.java", 9), "2009-10-23 20:01:52.0");
        sim.closeStatement();
    }

    @Test
    public void testLoadBuggyEntity() {
        InputManager in = new InputManager(3,2,5,1, CacheReplacement.Policy.CHANGES);
        in.setStartDate("2009-10-20 01:32:19");
        Simulator sim = new Simulator(in);
        Cache cache = sim.getCache();
        sim.loadBuggyEntity("/foo/bar/e.java", 9, "2009-10-24 09:50:26",
                "2009-10-23 20:01:52");
        assertNotNull(cache.getCacheItem("/foo/bar/e.java"));
        assertNull(cache.getCacheItem("/foo/bar/b"));
        assertNotNull(cache.getCacheItem("/foo/bar/a.java"));
        assertEquals(sim.getHit(), 0);
        assertEquals(sim.getMiss(), 1);
        sim.closeStatement();
    }

    @Test
    public void testSimulate() {
        InputManager in = new InputManager(3,2,5,1, CacheReplacement.Policy.BUGS);

        in.setStartDate("2009-10-24 14:30:53");
        Simulator sim1 = new Simulator(in);
        sim1.initialPreLoad();
        sim1.simulate();
        int rat = (int) (sim1.getHitRate());
        assertEquals(100, rat);
        sim1.closeStatement();

        in.setStartDate("2009-10-24 09:50:26");
        Simulator sim2 = new Simulator(in);
        sim2.initialPreLoad();
        sim2.simulate();
        rat = (int) (sim2.getHitRate());
        assertEquals(100, rat);
        sim2.closeStatement();

        in.setStartDate("2009-10-24 07:51:22");
        Simulator sim3 = new Simulator(in);
        sim3.initialPreLoad();
        sim3.simulate();
        rat = (int) (sim3.getHitRate());
        assertEquals(100, rat);
        sim3.closeStatement();

        //System.out.println("===========================================");
        in.setStartDate("2009-10-20 01:32:19");
        Simulator sim4 = new Simulator(in);
        sim4.initialPreLoad();
        sim4.simulate();
        rat = (int) (sim4.getHitRate());
        assertEquals(60, rat);
        sim4.closeStatement();
    }


}
