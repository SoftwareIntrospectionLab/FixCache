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
		
		Simulator sim = new Simulator(2, 2, 5, 1, CacheReplacement.Policy.BUGS, "2009-10-20 01:32:19.0");
		sim.initialPreLoad();
		assertEquals(sim.getCache().getCacheSize(), 2);
		ArrayList<CacheItem> CIList = sim.getCache().getCacheItemList();
		assertEquals(((CacheItem)CIList.get(0)).getEntityId(),4);
		assertEquals(((CacheItem)CIList.get(1)).getEntityId(),2);
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
	public void testLoadBuggyentity()
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
	    assertEquals(cache.getCacheItem(2).getNumberOfChanges(),2);
	}

}
