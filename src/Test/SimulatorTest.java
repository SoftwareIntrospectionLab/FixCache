package Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.sql.Connection;
import java.util.ArrayList;

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
                CacheReplacement.Policy.BUGS, "2009-10-20 01:32:19.0", null);
        sim1.initialPreLoad();
        assertEquals(sim1.getCache().getCacheSize(), 2);
        ArrayList<CacheItem> CIList1 = sim1.getCache().getCacheItemList();
        assertEquals(((CacheItem) CIList1.get(0)).getEntityId(), 4);
        assertEquals(((CacheItem) CIList1.get(1)).getEntityId(), 2);
        Simulator sim2 = new Simulator(2, 2, 5, 1,
                CacheReplacement.Policy.BUGS, "2009-10-20 14:37:47.0", null);
        sim2.initialPreLoad();
        assertEquals(sim2.getCache().getCacheSize(), 2);
        ArrayList<CacheItem> CIList2 = sim2.getCache().getCacheItemList();
        assertEquals(((CacheItem) CIList2.get(0)).getEntityId(), 2);
        assertEquals(((CacheItem) CIList2.get(1)).getEntityId(), 1);
        Simulator sim3 = new Simulator(2, 2, 5, 1,
                CacheReplacement.Policy.BUGS, "2009-10-23 09:50:25.0", null);
        sim3.initialPreLoad();
        ArrayList<CacheItem> CIList3 = sim3.getCache().getCacheItemList();
        assertEquals(sim3.getCache().getCacheSize(), 1);
        assertEquals(((CacheItem) CIList3.get(0)).getEntityId(), 1);
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
                "2009-10-20 01:32:19.0", null);
        Cache cache = sim.getCache();
        sim.initialPreLoad();
        assertEquals(cache.getCacheSize(), 2);
        sim.add(1, 1, "2009-10-20 01:32:19.0", CacheItem.CacheReason.NewEntity);
        assertEquals(cache.getCacheSize(), 3);
        sim.add(3, 1, "2009-10-20 01:32:19.0", CacheItem.CacheReason.NewEntity);
        assertEquals(cache.getCacheSize(), 4);
    }

    @Test
    public void testGetBugIntroCdate() {

        Simulator sim = new Simulator(2, 2, 5, 1, CacheReplacement.Policy.BUGS,
                "2009-10-20 01:32:19.0", null);
        assertEquals(sim.getBugIntroCdate(8, 10), "2009-10-23 14:29:05.0");
        assertEquals(sim.getBugIntroCdate(5, 9), "2009-10-23 20:01:52.0");
    }

    @Test
    public void testLoadBuggyEntity() {
        Simulator sim = new Simulator(3, 2, 5, 1,
                CacheReplacement.Policy.CHANGES, "2009-10-20 01:32:19.0", null);
        Cache cache = sim.getCache();
        sim.loadBuggyEntity(5, 9, "2009-10-24 09:50:26.0",
                "2009-10-23 20:01:52.0");
        assertNotNull(cache.getCacheItem(5));
        assertNotNull(cache.getCacheItem(2));
        assertNotNull(cache.getCacheItem(1));
        assertEquals(cache.getCacheItem(2).getNumberOfChanges(), 3);
        assertEquals(sim.getHit(), 0);
        assertEquals(sim.getMiss(), 1);
        sim.loadBuggyEntity(2, 5, "2009-10-23 14:29:05.0",
                "2009-10-23 14:10:37.0");
        assertEquals(sim.getHit(), 1);
        assertEquals(sim.getMiss(), 1);
        assertEquals(cache.getCacheItem(2).getNumberOfChanges(), 3);
    }

    @Test
    public void testSimulate() {
        Simulator sim1 = new Simulator(3, 2, 5, 1,
                CacheReplacement.Policy.BUGS, "2009-10-24 14:30:53.0", null);
        sim1.initialPreLoad();
        sim1.simulate();
        int rat = (int) (sim1.getHitRate() * 10);
        assertEquals(rat, 10);
        Simulator sim2 = new Simulator(3, 2, 5, 1,
                CacheReplacement.Policy.BUGS, "2009-10-24 09:50:26.0", null);
        sim2.initialPreLoad();
        sim2.simulate();
        rat = (int) (sim2.getHitRate() * 10);
        assertEquals(rat, 10);
        Simulator sim3 = new Simulator(3, 2, 5, 1,
                CacheReplacement.Policy.BUGS, "2009-10-24 07:51:22.0", null);
        sim3.initialPreLoad();
        sim3.simulate();
        rat = (int) (sim3.getHitRate() * 10);
        assertEquals(rat, 5);
        Simulator sim4 = new Simulator(3, 2, 5, 1,
                CacheReplacement.Policy.BUGS, "2009-10-20 01:32:19.0", null);
        sim4.initialPreLoad();
        sim4.simulate();
        rat = (int) (sim4.getHitRate() * 10);
        assertEquals(6, rat);
    }
    

}
