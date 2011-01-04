package Database;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Enumeration;
import java.util.Properties;
import static org.junit.Assert.assertTrue;

public class DatabaseManager {
	
	private static DatabaseManager dbManager = new DatabaseManager();
	private String drivername, databasename, username, password;
	private Connection conn = null;
	Statement stmt;
	
	

	private DatabaseManager() {
		File file = new File("database.properties");
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(file);
			Properties prop = new Properties();
			prop.load(fis);
			Enumeration enums = prop.propertyNames();
			drivername = (String) prop.get("Driver");  
            databasename = 	(String) prop.get("URL");
            username = (String) prop.get("UserName");
            password = (String) prop.get("UserPass");
			
			while (enums.hasMoreElements()) {
				String key = (String) enums.nextElement();
				String value = prop.getProperty(key);

			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (fis != null)
				try {
					fis.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
		}

		try {
            Class.forName(drivername).newInstance();
            conn = DriverManager.getConnection(databasename,username,password);
            stmt = conn.createStatement();

    }
    catch (Exception e) {
            System.out.println(e);
            System.exit(0);
            }
	}
	
	public static DatabaseManager getInstance(){
		
		if(dbManager == null)
		{
			dbManager = new DatabaseManager(); 
		}
		return dbManager;
	}
	
	public Connection getConnection()
	{
		return conn;
	}
	
	public ResultSet executeQuery(String sql)
	{
		ResultSet rs = null;
		try{
			rs = stmt.executeQuery(sql);
		}catch(SQLException e){
			e.printStackTrace();
		}
		return rs;
	}
	
	public void close()
	{
		try{
			conn.close();
		}catch (Exception e) {
			System.out.println(e);
			System.exit(0);}	
	}

	public static void main(String[] args) {
		DatabaseManager dbm1 = DatabaseManager.getInstance();
		Connection conn = dbm1.getConnection();
		int commitId;
		try{
			Statement stmt1 = conn.createStatement();
			Statement stmt2 = conn.createStatement();
			ResultSet r1 = stmt1.executeQuery("select id from scmlog");
			while(r1.next()){
			commitId = r1.getInt(1);
			ResultSet r2 = stmt2.executeQuery("select file_id from actions where commit_id ="+10);
			while(r2.next())
			{
				System.out.print(r2.getInt(1));
			}
			}
		}catch (Exception e) {
			System.out.println(e);
			System.exit(0);}



		
	}

}