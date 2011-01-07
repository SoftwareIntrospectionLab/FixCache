package Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

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
	public void testPreLoad() {
		
		Simulator sim1 = new Simulator(2, 2, 5, 1, CacheReplacement.Policy.BUGS, "2009-10-20 01:32:19.0");
		sim1.initialPreLoad();
		assertEquals(sim1.getCache().getCacheSize(), 2);
		ArrayList<CacheItem> CIList1 = sim1.getCache().getCacheItemList();
		assertEquals(((CacheItem)CIList1.get(0)).getEntityId(),4);
		assertEquals(((CacheItem)CIList1.get(1)).getEntityId(),2);
		Simulator sim2 = new Simulator(2, 2, 5, 1, CacheReplacement.Policy.BUGS, "2009-10-20 14:37:47.0");
		sim2.initialPreLoad();
		assertEquals(sim2.getCache().getCacheSize(), 2);
		ArrayList<CacheItem> CIList2 = sim2.getCache().getCacheItemList();
		assertEquals(((CacheItem)CIList2.get(0)).getEntityId(), 2);
		assertEquals(((CacheItem)CIList2.get(1)).getEntityId(), 1);
		Simulator sim3 = new Simulator(2, 2, 5, 1, CacheReplacement.Policy.BUGS, "2009-10-23 09:50:25.0");
		sim3.initialPreLoad();
		ArrayList<CacheItem> CIList3 = sim3.getCache().getCacheItemList();
		assertEquals(sim3.getCache().getCacheSize(), 1);
		assertEquals(((CacheItem)CIList3.get(0)).getEntityId(), 1);
		Simulator sim4 = new Simulator(2, 2, 5, 1, CacheReplacement.Policy.BUGS, "2010-10-21 09:50:25.0");
		sim4.initialPreLoad();
		assertEquals(sim4.getCache().getCacheSize(), 0);
		Simulator sim5 = new Simulator(2, 2, 5, 1, CacheReplacement.Policy.BUGS, "2009-10-21 09:50:25.0");
		sim5.initialPreLoad();
		assertEquals(sim5.getCache().getCacheSize(), 0);
		
	}
	
	
	@Test
	public void testVersionPreLoad()
	{
		Simulator sim = new Simulator(2, 2, 5, 1, CacheReplacement.Policy.BUGS, "2009-10-20 01:32:19.0");
		Cache cache = sim.getCache();
		sim.initialPreLoad();
		assertEquals(cache.getCacheSize(), 2);
	    sim.versionPreLoad(0, 1, 1, CacheItem.CacheReason.NewEntity);
	    assertEquals(cache.getCacheSize(), 3);
	    sim.versionPreLoad(1, 3, 1, CacheItem.CacheReason.NewEntity);
	    assertEquals(cache.getCacheSize(), 4);
	    sim.versionPreLoad(2, 5, 2, CacheItem.CacheReason.ModifiedEntity);
	    assertEquals(cache.getCacheSize(), 4);
	    assertNull(cache.getCacheItem(5));
	    
	}
	
	@Test
	public void testGetBugIntroCid() {
		
		Simulator sim = new Simulator(2, 2, 5, 1, CacheReplacement.Policy.BUGS, "2009-10-20 01:32:19.0");
		assertEquals(sim.getBugIntroCid(8, 10),6);
		assertEquals(sim.getBugIntroCid(5, 9),7);
	}
	
	
	@Test
	public void testLoadBuggyEntity()
	{
		Simulator sim = new Simulator(3, 2, 5, 1, CacheReplacement.Policy.BUGS, "2009-10-20 01:32:19.0");
		Cache cache = sim.getCache();
		sim.loadBuggyEntity(5, 9, 7);
		assertNotNull(cache.getCacheItem(5));
		assertNotNull(cache.getCacheItem(2));
		assertNotNull(cache.getCacheItem(1));
	    assertEquals(cache.getCacheItem(2).getNumberOfChanges(),3);
		assertEquals(sim.getHit(), 0);
		assertEquals(sim.getMiss(), 1);
		sim.loadBuggyEntity(2, 6, 5);
		assertEquals(sim.getHit(),1);
		assertEquals(sim.getMiss(),1);
	    assertEquals(cache.getCacheItem(2).getNumberOfChanges(),3);
	}
	
	@Test
	public void testSimulate()
	{
		Simulator sim = new Simulator(3, 2, 5, 1, CacheReplacement.Policy.BUGS, "2009-10-20 01:32:19.0");
		sim.initialPreLoad();
		sim.simulate();
		//System.out.print(sim.getHitRatio());
		int rat = (int) (sim.getHitRatio()*10);
		assertEquals(rat, 6);
	}

}
