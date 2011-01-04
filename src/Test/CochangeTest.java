package Test;

import static org.junit.Assert.assertEquals;

import java.sql.Connection;
import java.util.ArrayList;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import Cache.CoChange;

public class CochangeTest {
	
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
	
	public void testCoChange()
	{
		ArrayList<Integer> cochanges1 = CoChange.getCoChangeFileList(5, 9, 3);
		assertEquals(cochanges1.size(),2);
		assertEquals(cochanges1.get(0).intValue(),1);
		assertEquals(cochanges1.get(1).intValue(),2);
		ArrayList<Integer> cochanges2 = CoChange.getCoChangeFileList(1, 8, 4);
		assertEquals(cochanges2.size(),3);
		assertEquals(cochanges2.get(0).intValue(),2);
		assertEquals(cochanges2.get(1).intValue(),4);
		assertEquals(cochanges2.get(2).intValue(),3);
		ArrayList<Integer> cochanges3 = CoChange.getCoChangeFileList(7, 6, 5);
		assertEquals(cochanges3.size(), 2);
		assertEquals(cochanges3.get(0).intValue(), 2);
		assertEquals(cochanges3.get(1).intValue(), 6);
	}
	
}
	