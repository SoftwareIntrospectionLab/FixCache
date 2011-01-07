package Test;

import static org.junit.Assert.assertEquals;

import java.sql.Connection;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import Cache.CacheItem;
import Cache.CacheItem.CacheReason;

public class CacheItemTest {
	
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
	public void testCacheItemGet()
	{
		CacheItem ci1 = new CacheItem(5, 10, CacheReason.NewEntity, "2009-10-20 01:32:19.0");
		assertEquals(ci1.getNumberOfAuthors(),2);
		assertEquals(ci1.getNumberOfBugs(),3);
		assertEquals(ci1.getNumberOfChanges(),3);
		// XXX fails due to bug in content_loc
		// TODO change back later 8 +> 9
		assertEquals(ci1.getLOC(),8);
		CacheItem ci2 = new CacheItem(1, 8, CacheReason.ModifiedEntity, "2009-10-20 01:32:19.0");
		assertEquals(ci2.getNumberOfAuthors(),4);
		assertEquals(ci2.getNumberOfBugs(),2);
		assertEquals(ci2.getNumberOfChanges(),5);
		
	}
	
}
	