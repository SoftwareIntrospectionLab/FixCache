package Test;

import static org.junit.Assert.assertNull;

import java.sql.Connection;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import Cache.Cache;
import Cache.CacheReplacement;
import Cache.CacheItem.CacheReason;

public class CacheReplacementTest {
	
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
	public void testCacheReplacementAuthors()
	{
		Cache cache = new Cache(5, new CacheReplacement(CacheReplacement.Policy.AUTHORS), "2009-10-20 01:32:19.0");
		cache.add(1, 1, CacheReason.NewEntity);
		cache.add(2, 1, CacheReason.NewEntity);
		cache.add(3, 1, CacheReason.NewEntity);
		cache.add(4, 1, CacheReason.NewEntity);
		cache.add(1, 2, CacheReason.BugEntity);
		cache.add(4, 2, CacheReason.BugEntity);
		cache.add(2, 3, CacheReason.BugEntity);
		cache.add(5, 3, CacheReason.NewEntity);
		cache.add(1, 3, CacheReason.BugEntity);
		cache.remove(4);
		cache.add(1, 5, CacheReason.BugEntity);
		cache.add(2, 6, CacheReason.BugEntity);
		cache.add(6, 6, CacheReason.NewEntity);	
		cache.add(7, 6, CacheReason.NewEntity);
		assertNull(cache.getCacheItem(3));
		cache.add(3, 7, CacheReason.BugEntity);
		assertNull(cache.getCacheItem(5));
		cache.add(8, 8, CacheReason.NewEntity);
		assertNull(cache.getCacheItem(6));
		cache.add(1, 8, CacheReason.ModifiedEntity);
		cache.add(5, 9, CacheReason.ModifiedEntity);
		assertNull(cache.getCacheItem(7));
	}
	
	
	@Test
	public void testCacheReplacementLRU()
	{
		Cache cache = new Cache(5, new CacheReplacement(CacheReplacement.Policy.LRU), "2009-10-20 01:32:19.0");
		cache.add(1, 1, CacheReason.NewEntity);
		cache.add(2, 1, CacheReason.NewEntity);
		cache.add(3, 1, CacheReason.NewEntity);
		cache.add(4, 1, CacheReason.NewEntity);
		cache.add(1, 2, CacheReason.BugEntity);
		cache.add(4, 2, CacheReason.BugEntity);
		cache.add(2, 3, CacheReason.BugEntity);
		cache.add(5, 3, CacheReason.NewEntity);
		cache.add(1, 3, CacheReason.BugEntity);
		cache.remove(4);
		cache.add(1, 5, CacheReason.BugEntity);
		cache.add(2, 6, CacheReason.BugEntity);
		cache.add(6, 6, CacheReason.NewEntity);	
		cache.add(7, 6, CacheReason.NewEntity);
		assertNull(cache.getCacheItem(3));
		cache.add(3, 7, CacheReason.BugEntity);
		assertNull(cache.getCacheItem(5));
		cache.add(8, 8, CacheReason.NewEntity);
		assertNull(cache.getCacheItem(1));
		cache.add(1, 8, CacheReason.ModifiedEntity);
		assertNull(cache.getCacheItem(2));
		cache.add(5, 9, CacheReason.BugEntity);
		assertNull(cache.getCacheItem(6));
	}
	
	
	@Test
	public void testCacheReplacementChanges()
	{
		Cache cache = new Cache(5, new CacheReplacement(CacheReplacement.Policy.CHANGES), "2009-10-20 01:32:19.0");
		cache.add(1, 1, CacheReason.NewEntity);
		cache.add(2, 1, CacheReason.NewEntity);
		cache.add(3, 1, CacheReason.NewEntity);
		cache.add(4, 1, CacheReason.NewEntity);
		cache.add(1, 2, CacheReason.BugEntity);
		cache.add(4, 2, CacheReason.BugEntity);
		cache.add(2, 3, CacheReason.BugEntity);
		cache.add(5, 3, CacheReason.NewEntity);
		cache.add(1, 3, CacheReason.BugEntity);
		cache.remove(4);
		cache.add(1, 5, CacheReason.BugEntity);
		cache.add(2, 6, CacheReason.BugEntity);
		cache.add(6, 6, CacheReason.NewEntity);	
		cache.add(7, 6, CacheReason.NewEntity);
		assertNull(cache.getCacheItem(3));
		cache.add(3, 7, CacheReason.BugEntity);
		assertNull(cache.getCacheItem(5));
		cache.add(8, 8, CacheReason.NewEntity);
		assertNull(cache.getCacheItem(6));
		cache.add(1, 8, CacheReason.ModifiedEntity);
		cache.add(5, 9, CacheReason.BugEntity);
		assertNull(cache.getCacheItem(7));
	}
	
	@Test
	public void testCacheReplacementBugs()
	{
		Cache cache = new Cache(5, new CacheReplacement(CacheReplacement.Policy.BUGS), "2009-10-20 01:32:19.0");
		cache.add(1, 1, CacheReason.NewEntity);
		cache.add(2, 1, CacheReason.NewEntity);
		cache.add(3, 1, CacheReason.NewEntity);
		cache.add(4, 1, CacheReason.NewEntity);
		cache.add(1, 2, CacheReason.BugEntity);
		cache.add(4, 2, CacheReason.BugEntity);
		cache.add(2, 3, CacheReason.BugEntity);
		cache.add(5, 3, CacheReason.NewEntity);
		cache.add(1, 3, CacheReason.BugEntity);
		cache.remove(4);
		cache.add(1, 5, CacheReason.BugEntity);
		cache.add(2, 6, CacheReason.BugEntity);
		cache.add(6, 6, CacheReason.NewEntity);	
		cache.add(7, 6, CacheReason.NewEntity);
		assertNull(cache.getCacheItem(3));
		cache.add(3, 7, CacheReason.BugEntity);
		assertNull(cache.getCacheItem(5));
		cache.add(8, 8, CacheReason.NewEntity);
		assertNull(cache.getCacheItem(3));
		cache.add(6, 8, CacheReason.ModifiedEntity);
		cache.add(5, 9, CacheReason.BugEntity);
		assertNull(cache.getCacheItem(8));
	}	
	
}