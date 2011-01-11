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
		Cache cache = Cache.getTestCache(5, new CacheReplacement(CacheReplacement.Policy.AUTHORS), "2009-10-20 01:32:19.0", 1);
		cache.add(1, 1, "2009-10-20 01:32:19.0", CacheReason.NewEntity);
		cache.add(2, 1, "2009-10-20 01:32:19.0", CacheReason.NewEntity);
		cache.add(3, 1, "2009-10-20 01:32:19.0", CacheReason.NewEntity);
		cache.add(4, 1, "2009-10-20 01:32:19.0", CacheReason.NewEntity);
		cache.add(1, 2, "2009-10-20 14:37:38.0", CacheReason.BugEntity);
		cache.add(4, 2, "2009-10-20 14:37:38.0", CacheReason.BugEntity);
		cache.add(2, 3, "2009-10-20 14:37:47.0", CacheReason.BugEntity);
		cache.add(5, 3, "2009-10-20 14:37:47.0", CacheReason.NewEntity);
		cache.add(1, 3, "2009-10-20 14:37:47.0", CacheReason.BugEntity);
		cache.remove(4);
		cache.add(1, 5, "2009-10-23 14:10:37.0", CacheReason.BugEntity);
		cache.add(2, 6, "2009-10-23 14:29:05.0", CacheReason.BugEntity);
		cache.add(6, 6, "2009-10-23 14:29:05.0", CacheReason.NewEntity);	
		cache.add(7, 6, "2009-10-23 14:29:05.0", CacheReason.NewEntity);
		assertNull(cache.getCacheItem(3));
		cache.add(3, 7, "2009-10-23 20:01:52.0", CacheReason.BugEntity);
		assertNull(cache.getCacheItem(5));
		cache.add(8, 8, "2009-10-24 07:51:22.0", CacheReason.NewEntity);
		assertNull(cache.getCacheItem(6));
		cache.add(1, 8, "2009-10-24 07:51:22.0", CacheReason.ModifiedEntity);
		cache.add(5, 9, "2009-10-24 09:50:26.0", CacheReason.ModifiedEntity);
		assertNull(cache.getCacheItem(7));
	}
	
	
	@Test
	public void testCacheReplacementLRU()
	{
		Cache cache = Cache.getTestCache(5, new CacheReplacement(CacheReplacement.Policy.LRU), "2009-10-20 01:32:19.0", 1);
		cache.add(1, 1, "2009-10-20 01:32:19.0", CacheReason.NewEntity);
		cache.add(2, 1, "2009-10-20 01:32:19.0", CacheReason.NewEntity);
		cache.add(3, 1, "2009-10-20 01:32:19.0", CacheReason.NewEntity);
		cache.add(4, 1, "2009-10-20 01:32:19.0", CacheReason.NewEntity);
		cache.add(1, 2, "2009-10-20 14:37:38.0", CacheReason.BugEntity);
		cache.add(4, 2, "2009-10-20 14:37:38.0", CacheReason.BugEntity);
		cache.add(2, 3, "2009-10-20 14:37:47.0", CacheReason.BugEntity);
		cache.add(5, 3, "2009-10-20 14:37:47.0", CacheReason.NewEntity);
		cache.add(1, 3, "2009-10-20 14:37:47.0", CacheReason.BugEntity);
		cache.remove(4);
		cache.add(1, 5, "2009-10-23 14:10:37.0", CacheReason.BugEntity);
		cache.add(2, 6, "2009-10-23 14:29:05.0", CacheReason.BugEntity);
		cache.add(6, 6, "2009-10-23 14:29:05.0", CacheReason.NewEntity);	
		cache.add(7, 6, "2009-10-23 14:29:05.0", CacheReason.NewEntity);
		assertNull(cache.getCacheItem(3));
		cache.add(3, 7, "2009-10-23 20:01:52.0", CacheReason.BugEntity);
		assertNull(cache.getCacheItem(5));
		cache.add(8, 8, "2009-10-24 07:51:22.0", CacheReason.NewEntity);
		assertNull(cache.getCacheItem(1));
		cache.add(1, 8, "2009-10-24 07:51:22.0", CacheReason.ModifiedEntity);
		assertNull(cache.getCacheItem(2));
		cache.add(5, 9, "2009-10-24 09:50:26.0", CacheReason.ModifiedEntity);
		assertNull(cache.getCacheItem(6));
	}
	
	
	@Test
	public void testCacheReplacementChanges()
	{
		Cache cache = Cache.getTestCache(5, new CacheReplacement(CacheReplacement.Policy.CHANGES), "2009-10-20 01:32:19.0", 1);
		cache.add(1, 1, "2009-10-20 01:32:19.0", CacheReason.NewEntity);
		cache.add(2, 1, "2009-10-20 01:32:19.0", CacheReason.NewEntity);
		cache.add(3, 1, "2009-10-20 01:32:19.0", CacheReason.NewEntity);
		cache.add(4, 1, "2009-10-20 01:32:19.0", CacheReason.NewEntity);
		cache.add(1, 2, "2009-10-20 14:37:38.0", CacheReason.BugEntity);
		cache.add(4, 2, "2009-10-20 14:37:38.0", CacheReason.BugEntity);
		cache.add(2, 3, "2009-10-20 14:37:47.0", CacheReason.BugEntity);
		cache.add(5, 3, "2009-10-20 14:37:47.0", CacheReason.NewEntity);
		cache.add(1, 3, "2009-10-20 14:37:47.0", CacheReason.BugEntity);
		cache.remove(4);
		cache.add(1, 5, "2009-10-23 14:10:37.0", CacheReason.BugEntity);
		cache.add(2, 6, "2009-10-23 14:29:05.0", CacheReason.BugEntity);
		cache.add(6, 6, "2009-10-23 14:29:05.0", CacheReason.NewEntity);	
		cache.add(7, 6, "2009-10-23 14:29:05.0", CacheReason.NewEntity);
		assertNull(cache.getCacheItem(3));
		cache.add(3, 7, "2009-10-23 20:01:52.0", CacheReason.BugEntity);
		assertNull(cache.getCacheItem(5));
		cache.add(8, 8, "2009-10-24 07:51:22.0", CacheReason.NewEntity);
		assertNull(cache.getCacheItem(6));
		cache.add(1, 8, "2009-10-24 07:51:22.0", CacheReason.ModifiedEntity);
		cache.add(5, 9, "2009-10-24 09:50:26.0", CacheReason.ModifiedEntity);
		assertNull(cache.getCacheItem(7));
	}
	
	@Test
	public void testCacheReplacementBugs()
	{
		Cache cache = Cache.getTestCache(5, new CacheReplacement(CacheReplacement.Policy.BUGS), "2009-10-20 01:32:19.0", 1);	
		
		cache.add(1, 1, "2009-10-20 01:32:19.0", CacheReason.NewEntity);
		cache.add(2, 1, "2009-10-20 01:32:19.0", CacheReason.NewEntity);
		cache.add(3, 1, "2009-10-20 01:32:19.0", CacheReason.NewEntity);
		cache.add(4, 1, "2009-10-20 01:32:19.0", CacheReason.NewEntity);
		cache.add(1, 2, "2009-10-20 14:37:38.0", CacheReason.BugEntity);
		cache.add(4, 2, "2009-10-20 14:37:38.0", CacheReason.BugEntity);
		cache.add(2, 3, "2009-10-20 14:37:47.0", CacheReason.BugEntity);
		cache.add(5, 3, "2009-10-20 14:37:47.0", CacheReason.NewEntity);
		cache.add(1, 3, "2009-10-20 14:37:47.0", CacheReason.BugEntity);
		cache.remove(4);
		cache.add(1, 5, "2009-10-23 14:10:37.0", CacheReason.BugEntity);
		cache.add(2, 6, "2009-10-23 14:29:05.0", CacheReason.BugEntity);
		cache.add(6, 6, "2009-10-23 14:29:05.0", CacheReason.NewEntity);	
		cache.add(7, 6, "2009-10-23 14:29:05.0", CacheReason.NewEntity);
		assertNull(cache.getCacheItem(3));
		cache.add(3, 7, "2009-10-23 20:01:52.0", CacheReason.BugEntity);
		assertNull(cache.getCacheItem(5));
		cache.add(8, 8, "2009-10-24 07:51:22.0", CacheReason.NewEntity);
		assertNull(cache.getCacheItem(3));
		cache.add(1, 8, "2009-10-24 07:51:22.0", CacheReason.ModifiedEntity);
		cache.add(5, 9, "2009-10-24 09:50:26.0", CacheReason.ModifiedEntity);
		assertNull(cache.getCacheItem(8));
	}	
	
}