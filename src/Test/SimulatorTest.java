package Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.sql.Connection;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import Cache.Cache;
import Cache.CacheItem;
import Cache.CacheReplacement;
import Cache.Simulator;

public class SimulatorTest {

    private static Connection conn;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        try {
            conn = TestHelper.getJDBCConnection();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        if (conn != null) {
            conn.close();
        }
    }

    @Before
    public void setUp() throws Exception {
        TestHelper.handleSetUpOperation();
    }

    @After
    public void tearDown() throws Exception {
        TestHelper.cleanDatabase();
    }

    @Test
    public void testInitialPreLoad() {

        Simulator sim1 = new Simulator(2, 2, 5, 1,
                CacheReplacement.Policy.BUGS, "2009-10-20 01:32:19.0", null, false);
        sim1.initialPreLoad();
        assertEquals(sim1.getCache().getCacheSize(), 2);
        assertTrue(sim1.getCache().contains("d.java"));
        assertTrue(sim1.getCache().contains("c.java"));
        Simulator sim2 = new Simulator(2, 2, 5, 1,
                CacheReplacement.Policy.BUGS, "2009-10-20 14:37:47.0", null, false);
        sim2.initialPreLoad();
        assertEquals(sim2.getCache().getCacheSize(), 2);
        assertTrue(sim2.getCache().contains("a.java"));
        assertTrue(sim2.getCache().contains("e.java"));
        Simulator sim3 = new Simulator(2, 2, 5, 1,
                CacheReplacement.Policy.BUGS, "2009-10-23 09:50:25.0", null, false);
        sim3.initialPreLoad();
        assertEquals(sim3.getCache().getCacheSize(), 1);
        assertTrue(sim3.getCache().contains("a.java"));
//        Simulator sim4 = new Simulator(2, 2, 5, 1,
//                CacheReplacement.Policy.BUGS, "2010-10-21 09:50:25.0", null);
//        sim4.initialPreLoad();
//        assertEquals(sim4.getCache().getCacheSize(), 0);
//        Simulator sim6 = new Simulator(2, 2, 5, 1,
//                CacheReplacement.Policy.BUGS, null, "2000-10-21 09:50:25.0");
//        sim1.initialPreLoad();
//        assertEquals(sim6.getCache().getCacheSize(), 0);

    }

    @Test
    public void testVersionPreLoad() {
        Simulator sim = new Simulator(2, 2, 5, 1, CacheReplacement.Policy.BUGS,
                "2009-10-20 01:32:19.0", null, false);
        Cache cache = sim.getCache();
        sim.initialPreLoad();
        assertEquals(cache.getCacheSize(), 2);
        sim.add("a.java", 1, "2009-10-20 01:32:19.0", CacheItem.CacheReason.NewEntity);
        assertEquals(cache.getCacheSize(), 3);
        sim.add("c.java", 1, "2009-10-20 01:32:19.0", CacheItem.CacheReason.NewEntity);
        assertEquals(cache.getCacheSize(), 3);
    }

    @Test
    public void testGetBugIntroCdate() {

        Simulator sim = new Simulator(2, 2, 5, 1, CacheReplacement.Policy.BUGS,
                "2009-10-20 01:32:19.0", null, false);
        assertEquals(sim.getBugIntroCdate("h.java", 10), "2009-10-23 14:29:05.0");
        assertEquals(sim.getBugIntroCdate("e.java", 9), "2009-10-23 20:01:52.0");
    }

    @Test
    public void testLoadBuggyEntity() {
        Simulator sim = new Simulator(3, 2, 5, 1,
                CacheReplacement.Policy.CHANGES, "2009-10-20 01:32:19.0", null, false);
        Cache cache = sim.getCache();
        sim.loadBuggyEntity("e.java", 9, "2009-10-24 09:50:26.0",
                "2009-10-23 20:01:52.0");
        assertNotNull(cache.getCacheItem("e.java"));
        assertNull(cache.getCacheItem("b"));
        assertNotNull(cache.getCacheItem("a.java"));
        assertEquals(sim.getHit(), 0);
        assertEquals(sim.getMiss(), 1);
    }

    @Test
    public void testSimulate() {
        Simulator sim1 = new Simulator(3, 2, 5, 1,
                CacheReplacement.Policy.BUGS, "2009-10-24 14:30:53.0", null, false);
        sim1.initialPreLoad();
        sim1.simulate();
        int rat = (int) (sim1.getHitRate()*100);
        assertEquals(10000, rat);
        Simulator sim2 = new Simulator(3, 2, 5, 1,
                CacheReplacement.Policy.BUGS, "2009-10-24 09:50:26.0", null, false);
        sim2.initialPreLoad();
        sim2.simulate();
        rat = (int) (sim2.getHitRate()*100);
        assertEquals(10000, rat);
        Simulator sim3 = new Simulator(3, 2, 5, 1,
                CacheReplacement.Policy.BUGS, "2009-10-24 07:51:22.0", null, false);
        sim3.initialPreLoad();
        sim3.simulate();
        rat = (int) (sim3.getHitRate()*100);
        assertEquals(5000, rat);
        Simulator sim4 = new Simulator(3, 2, 5, 1,
                CacheReplacement.Policy.BUGS, "2009-10-20 01:32:19.0", null, false);
        sim4.initialPreLoad();
        sim4.simulate();
        rat = (int) (sim4.getHitRate()*100);
        assertEquals(6000, rat);
    }
    

}
