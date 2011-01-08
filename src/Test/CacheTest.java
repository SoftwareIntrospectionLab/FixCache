package Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.sql.Connection;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import Cache.Cache;
import Cache.CacheReplacement;
import Cache.CacheItem.CacheReason;

public class CacheTest {
	
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
	public void testCacheAdd()
	{
		Cache cache = new Cache(5, new CacheReplacement(CacheReplacement.Policy.AUTHORS), "2009-10-20 01:32:19.0");
		cache.add(1, 1, "2009-10-20 01:32:19.0", CacheReason.NewEntity);
		assertEquals(cache.getCacheSize(),1);
		cache.add(2, 1, "2009-10-20 01:32:19.0", CacheReason.NewEntity);
		assertEquals(cache.getCacheSize(),2);
		cache.add(3, 1, "2009-10-20 01:32:19.0", CacheReason.NewEntity);
		assertEquals(cache.getCacheSize(),3);
		cache.add(4, 1, "2009-10-20 01:32:19.0", CacheReason.NewEntity);
		assertEquals(cache.getCacheSize(),4);
		cache.add(1, 2, "2009-10-20 14:37:38.0", CacheReason.ModifiedEntity);
		assertEquals(cache.getCacheSize(),4);
		cache.add(4, 2, "2009-10-20 14:37:38.0",CacheReason.ModifiedEntity);
		assertEquals(cache.getCacheSize(),4);
		cache.add(2, 3, "2009-10-20 14:37:47.0", CacheReason.BugEntity);
		assertEquals(cache.getCacheSize(),4);
		cache.add(5, 3, "2009-10-20 14:37:47.0", CacheReason.BugEntity);
		assertTrue(cache.isFull());
	}
}