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
import Cache.InputManager;
import Cache.Simulator;
import Util.TestHelper;

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

        InputManager in = new InputManager(2,2,5,1, CacheReplacement.Policy.BUGS,conn);
        in.setStartDate("2009-10-20 01:32:19.0");
        
        Simulator sim1 = new Simulator(in);
        sim1.initialPreLoad();
        assertEquals(3, sim1.getCache().getCacheSize()); // only 3 files in inital commit
        assertTrue(sim1.getCache().contains("d.java"));
        assertTrue(sim1.getCache().contains("c.java"));
        
        in.setStartDate("2009-10-20 14:37:47.0");
        Simulator sim2 = new Simulator(in);
        sim2.initialPreLoad();
        assertEquals(4, sim2.getCache().getCacheSize());
        assertTrue(sim2.getCache().contains("a.java"));
        assertTrue(sim2.getCache().contains("e.java"));
        in.setStartDate("2009-10-23 09:50:25.0");
        Simulator sim3 = new Simulator(in);
        sim3.initialPreLoad();
        assertEquals(4, sim3.getCache().getCacheSize());
        assertTrue(sim3.getCache().contains("a.java"));

    }

    @Test
    public void testVersionPreLoad() {
        InputManager in = new InputManager(2,2,5,1, CacheReplacement.Policy.BUGS,conn);
        
        in.setStartDate("2009-10-20 01:32:19.0");
        Simulator sim = new Simulator(in);
        Cache cache = sim.getCache();
        sim.initialPreLoad();
        assertEquals(cache.getCacheSize(), 3);
        sim.add("a.java", 1, "2009-10-20 01:32:19.0", CacheItem.CacheReason.NewEntity);
        assertEquals(cache.getCacheSize(), 3);
        sim.add("c.java", 1, "2009-10-20 01:32:19.0", CacheItem.CacheReason.NewEntity);
        assertEquals(cache.getCacheSize(), 3);
    }

    @Test
    public void testGetBugIntroCdate() {
        InputManager in = new InputManager(2,2,5,1, CacheReplacement.Policy.BUGS,conn);

        in.setStartDate("2009-10-20 01:32:19.0");
        Simulator sim = new Simulator(in);
        // XXX no longer finds maximum bug introducing date
 //       assertEquals(sim.getBugIntroCdate("h.java", 10), "2009-10-23 14:29:05.0");
//        assertEquals(sim.getBugIntroCdate("e.java", 9), "2009-10-23 20:01:52.0");
    }

    @Test
    public void testLoadBuggyEntity() {
        InputManager in = new InputManager(3,2,5,1, CacheReplacement.Policy.CHANGES,conn);
        in.setStartDate("2009-10-20 01:32:19.0");
        Simulator sim = new Simulator(in);
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
        InputManager in = new InputManager(3,2,5,1, CacheReplacement.Policy.BUGS,conn);

        in.setStartDate("2009-10-24 14:30:53.0");
        Simulator sim1 = new Simulator(in);
        sim1.initialPreLoad();
        sim1.simulate();
        int rat = (int) (sim1.getHitRate());
        assertEquals(100, rat);
        
        in.setStartDate("2009-10-24 09:50:26.0");
        Simulator sim2 = new Simulator(in);
        sim2.initialPreLoad();
        sim2.simulate();
        rat = (int) (sim2.getHitRate());
        assertEquals(100, rat);
        
        in.setStartDate("2009-10-24 07:51:22.0");
        Simulator sim3 = new Simulator(in);
        sim3.initialPreLoad();
        sim3.simulate();
        rat = (int) (sim3.getHitRate());
        assertEquals(100, rat);
        
        in.setStartDate("2009-10-20 01:32:19.0");
        Simulator sim4 = new Simulator(in);
        sim4.initialPreLoad();
        sim4.simulate();
        rat = (int) (sim4.getHitRate());
        assertEquals(60, rat);
    }
    

}
