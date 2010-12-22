package Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import Cache.CacheItem;
import Cache.CacheReplacement;
import Cache.Simulator;
import Cache.CacheItem.CacheReason;
import Database.DBOperation;
import Database.DatabaseManager;

public class FixCacheTest {
	
	private static DatabaseManager dbManager;
	private static Connection conn;
	private static Statement stmt;

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
		stmt = conn.createStatement();
	}

	@After
	public void tearDown() throws Exception {
			TestHelper.cleanDatabase();
	}
	
	
	@Test
	public void test() {

//		CacheItem ci = new CacheItem(4, 5, CacheReason.ModifiedEntity, "2009-10-20 01:32:19.0");
//		System.out.println(ci.getLOC());
//		assertEquals(ci.getLOC(),1);
		try {
			ResultSet rs = stmt
					.executeQuery("select loc from content_loc where id = 2");
			rs.next();
			assertEquals(rs.getInt(1), 1);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
/*
	@Test
	public void testGetModules() {
		try {
			project.setSession(session);
			IModule[] modules = project.getModules();
			ResultSet rs = stmt
					.executeQuery("select count(*) from cocomo_module where ProjectId="
							+ project.getProjectId());
			rs.next();
			assertEquals(rs.getInt(1), modules.length);
			assertNotNull(((CocomoModule) modules[0]).getSession());
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Test
	public void testAddModule() {

		boolean has_next = false;
		try {
			CocomoModule module = new CocomoModule(session);
			String moduleName = "Memory Management";
			module.setName(moduleName);
			project.setSession(session);
			project.addModule(module);
			assertTrue(project.save());
			ResultSet rs = stmt
					.executeQuery("select ProjectId from cocomo_module where name='"
							+ moduleName + "'");
			has_next = rs.next();
			assertTrue(has_next);
			int pid = rs.getInt(1);
			assertEquals(project.getProjectId(), pid);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Test
	public void testRetrieveProject() {
		Transaction tx = session.beginTransaction();
		String hql = "from hibernate.pojos.CocomoProject";
		Query q = session.createQuery(hql);
		List l = q.list();
		tx.commit();
	}

	@Test
	public void testNoDuplicateName() {
		CocomoProject p = new CocomoProject(session);
		p.setName(projectName);
		assertFalse(p.save());
	}

	@Test
	public void testDispose() throws Exception {
		logger.debug("start testDispose");
		project.dispose();
		ResultSet rs;
		rs = stmt
				.executeQuery("select count(*) from cocomo_project where Name='"
						+ projectName + "'");
		assertTrue(rs.next());
		assertEquals(0, rs.getInt(1));
		rs = stmt
				.executeQuery("select count(*) from cocomo_pm_factor_value where PMId="
						+ project.getProjectId() + " and type<>'module'");
		assertTrue(rs.next());
		assertEquals(0, rs.getInt(1));
		// Modules of the project get deleted
		rs = stmt
				.executeQuery("select count(*) from cocomo_module where ProjectId="
						+ project.getProjectId());
		assertTrue(rs.next());
		assertEquals(0, rs.getInt(1));

	}

	@Test
	public void testRemoveAssociatedParameters() throws SQLException {
		ResultSet rs;// = stmt
//				.executeQuery("select count(*) from cocomo_pm_factor_value where PMId="
//						+ project.getProjectId() + " and type='project'");
//		assertTrue(rs.next());
//		assertTrue(rs.getInt(1)>0);
		project.dispose();
		rs = stmt
				.executeQuery("select count(*) from cocomo_pm_factor_value where PMId="
						+ project.getProjectId() + " and type='project'");
		assertTrue(rs.next());
		assertEquals(0, rs.getInt(1));

	}

	@Test
	public void testGetCategorizedParameters() {
		HibernateFactory factory = new HibernateFactory(session);
		project = (CocomoProject) factory.getProject("PD2");
		Map<Category, List<IPMFactorValue>> map = project
				.getCategorizedParameters();
		assertEquals(1, map.size());
		Query q = session.createQuery("from " + CocomoCategory.class.getName()
				+ " v where v.categoryId= 5");
		Object cat_5 = q.list().get(0);
		assertTrue(map.keySet().contains(cat_5));
		assertEquals(1, map.get(cat_5).size());

		// q = session.createQuery("from "
		// + CocomoCategory.class.getName()
		// + " v where v.categoryId= 5");
		// Object cat_5 = q.list().get(0);
		// assertTrue(map.keySet().contains(cat_5));
		// assertEquals(5, map.get(cat_5).size());
	}

	@Test
	public void testGetParameters() {
		HibernateFactory factory = new HibernateFactory(session);
		project = (CocomoProject) factory.getProject("PD2");
		Map<CocomoFactor, CocomoPmFactorValue> factorValueMaping = project
				.getParameters();
		for (CocomoFactor factor : factorValueMaping.keySet()) {
			assertNotNull(factor.getCategory().getName());
		}
	}

	@Test
	public void testSetCategorizedParameters() throws Exception {
		HibernateFactory factory = new HibernateFactory(session);
		project = (CocomoProject) factory.getProject("PD2");
		Map<Category, List<IPMFactorValue>> map = project
				.getCategorizedParameters();
		Category c = map.keySet().iterator().next();
		IPMFactorValue factorValue = map.get(c).iterator().next();
		CocomoFactor factor = (CocomoFactor) factorValue.getFactor();
		CocomoLevel level = factor.getLevels()[0];
		factorValue.setLevelLikely(level);
		project.save();

		ResultSet rs = stmt
				.executeQuery("select LevelIdLikely from cocomo_pm_factor_value where PMId="
						+ project.getProjectId()
						+ " and type='project' and FactorId="
						+ factor.getFactorId());
		assertTrue(rs.next());
		assertEquals(level.getId(), rs.getInt(1));
	}
	
	@Test
	public void testProjectTypeStruct() throws Exception
	{
		assertTrue(IProject.TypeStart == 0);
		assertTrue(IProject.TypeEnd == IProject.arrTypes.length - 1);
//		assertTrue(IProject.Type >= IProject.TypeStart && IProject.Type <= IProject.TypeEnd);
		assertTrue(IProject.TypeApp >= IProject.TypeStart && IProject.TypeApp <= IProject.TypeEnd);
		assertTrue(IProject.TypePlat >= IProject.TypeStart && IProject.TypePlat <= IProject.TypeEnd);
		assertTrue(IProject.TypeArc >= IProject.TypeStart && IProject.TypeArc <= IProject.TypeEnd);
		assertTrue(IProject.TypeAcq >= IProject.TypeStart && IProject.TypeAcq <= IProject.TypeEnd);
		
		
		Session ses = SessionManager.currentSession();
		for(int i = IProject.TypeStart; i <= IProject.TypeEnd; i++)
		{
			IProject.ProjectTypeStruct pts = IProject.arrTypes[i];
			
			Query q = ses.createQuery(String.format("from %s where typeName = :typeName", pts.strClass));
			q.setString("typeName", pts.strDefault);
			ProjectType pt = (ProjectType)q.uniqueResult();
			assertNotNull(pt);
			assertTrue(pt.getTypeName().equals(pts.strDefault));
			
			q = ses.createSQLQuery(String.format("select %s from %s where %s = :typeName", 
					pts.strNameField, pts.strTable, pts.strNameField));
			q.setString("typeName", pts.strDefault);
			String strTypeName = (String)q.uniqueResult();
			assertNotNull(strTypeName);
			assertTrue(strTypeName.equals(pts.strDefault));
		}
		SessionManager.closeSession(ses);
	}

	@Test
	public void testGetProjectTypeArray() throws Exception
	{
		for(int i = IProject.TypeStart; i <= IProject.TypeEnd; i++)
		{
			String[] asTypeName = CocomoProject.getProjectTypeArray(i, session);
			assertNotNull(asTypeName);
			assertTrue(asTypeName.length > 0);
		}
	}

	
	@Test
	public void testAddDeleteProjectType() throws Exception
	{
		String sName1, sName2 = "It is not a type name";
		
		for(int i = IProject.TypeStart; i <= IProject.TypeEnd; i++)
		{
			IProject.ProjectTypeStruct pts = IProject.arrTypes[i];
			sName1 = pts.strDefault;
			
			int iTypeNum = CocomoProject.getProjectTypeArray(i, session).length;
			assertFalse(CocomoProject.addProjectType(i, session, sName1));
			assertTrue(iTypeNum == CocomoProject.getProjectTypeArray(i, session).length);
			assertTrue(CocomoProject.addProjectType(i, session, sName2));
			assertTrue(iTypeNum + 1 == CocomoProject.getProjectTypeArray(i, session).length);
			
			assertTrue(CocomoProject.deleteProjectType(i, session, sName2));
			assertTrue(iTypeNum == CocomoProject.getProjectTypeArray(i, session).length);
			assertFalse(CocomoProject.deleteProjectType(i, session, sName2));
			assertFalse(CocomoProject.deleteProjectType(i, session, sName1));
		}
	}

	@Test
	public void testGetSetProjectType() throws Exception
	{
		String sName1 = "PD1", sName2, sName3, sName4;
		
		HibernateFactory hf = new HibernateFactory(session);
		CocomoProject cp = (CocomoProject)hf.getProject(sName1);
		assertNotNull(cp);
		for(int i = IProject.TypeStart; i <= IProject.TypeEnd; i++)
		{
			IProject.ProjectTypeStruct pts = IProject.arrTypes[i];
			String[] asTypes = CocomoProject.getProjectTypeArray(i);
			sName2 = asTypes[asTypes.length / 2];
			
			ProjectType pt = cp.getProjectType(i, session);
			assertNotNull(pt);
			sName4 = pt.getTypeName();
			sName3 = new String(sName4 + "impossible suffix");
			assertFalse(cp.setProjectType(i, session, sName3));

			assertTrue(cp.setProjectType(i, session, sName2));
			Session ses = SessionManager.currentSession();
			Query q = ses.createQuery("from CocomoProject as cp where cp.name = :prjName");
			q.setString("prjName", sName1);
			CocomoProject cpSave = (CocomoProject)q.uniqueResult();
			assertNotNull(cpSave);
			assertTrue(cpSave.getProjectType(i, ses).getTypeName().equals(sName2));
			SessionManager.closeSession(ses);

			assertFalse(CocomoProject.deleteProjectType(i, session, sName2));

			assertTrue(cp.setProjectType(i, session, sName4));
			ses = SessionManager.currentSession();
			q = ses.createQuery("from CocomoProject as cp where cp.name = :prjName");
			q.setString("prjName", sName1);
			cpSave = (CocomoProject)q.uniqueResult();
			assertNotNull(cpSave);
			assertTrue(cp.getProjectType(i, ses).getTypeName().equals(sName4));
			SessionManager.closeSession(ses);
			
			assertFalse(CocomoProject.deleteProjectType(i, session, sName4));
		}
	}
	*/
/*	
	@Test
	public void test() throws Exception
	{
	}
*/
}