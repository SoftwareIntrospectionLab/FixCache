package Test;

import java.io.FileInputStream;
import java.sql.Connection;

import org.dbunit.database.DatabaseConnection;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.xml.FlatXmlDataSetBuilder;
import org.dbunit.operation.DatabaseOperation;

import Database.DatabaseManager;

public class TestHelper {
	public static Connection getJDBCConnection() throws Exception {
//		Class.forName("com.mysql.jdbc.Driver").newInstance();
//        Connection jdbcConnection = DriverManager.getConnection(
//                "jdbc:mysql://localhost:3306/fixcache", "root", "jacjac");     
//		return jdbcConnection;
		DatabaseManager dbManager = DatabaseManager.getInstance();
		Connection conn = dbManager.getConnection();
		return conn;
	}

	public static IDatabaseConnection getDBUnitConnection() throws Exception {
		return new DatabaseConnection(getJDBCConnection());
	}

	public static IDataSet getDataSet() throws Exception {
		return new FlatXmlDataSetBuilder().build(new FileInputStream("fixcache.xml"));
	}
    
    protected DatabaseOperation getSetUpOperation() throws Exception
    {
        return DatabaseOperation.REFRESH;
    }

    protected DatabaseOperation getTearDownOperation() throws Exception
    {
        return DatabaseOperation.NONE;
    }
	
	public static void handleSetUpOperation() throws Exception {
		final IDatabaseConnection conn = getDBUnitConnection();
		final IDataSet data = getDataSet();
		try {
			DatabaseOperation.CLEAN_INSERT.execute(conn, data);
		} finally {
//			conn.close();
		}
	}
	
	public static void cleanDatabase() throws Exception {
		final IDatabaseConnection conn = getDBUnitConnection();
		final IDataSet data = getDataSet();
		try {
			DatabaseOperation.DELETE_ALL.execute(conn, data);
		} finally {
			//conn.close();
		}
		
	}
}