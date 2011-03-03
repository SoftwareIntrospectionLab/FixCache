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
import java.util.Properties;

public class DatabaseManager {

    private static DatabaseManager dbManager;
    private String drivername, databasename, username, password;
    private Connection conn = null;
    Statement stmt;
    String datafile;

    private DatabaseManager(String file) {
        datafile = file;
    }
    
    public void createConnection(){
    
        File file = new File(datafile);
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            Properties prop = new Properties();
            prop.load(fis);
            //Enumeration enums = prop.propertyNames(); 
            drivername = (String) prop.get("Driver");
            databasename = (String) prop.get("URL");
            username = (String) prop.get("UserName");
            password = (String) prop.get("UserPass");

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
            conn = DriverManager
                    .getConnection(databasename, username, password);
//            stmt = conn.createStatement();

        } catch (Exception e) {
            System.out.println(e);
            System.exit(0);
        }
    }
    
    public void createTestConnection(){
        File dbFile = new File("fixcache.db");
        if(!dbFile.exists()){
            try {
                dbFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        
        File file = new File(datafile);
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            Properties prop = new Properties();
            prop.load(fis);
            //Enumeration enums = prop.propertyNames(); 
            drivername = (String) prop.get("Driver");
            databasename = (String) prop.get("URL");

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
            conn = DriverManager
                    .getConnection(databasename);
//            stmt = conn.createStatement();

        } catch (Exception e) {
            System.out.println(e);
            System.exit(0);
        }
    }
    
    
    public static Connection getTestConnection() {
        dbManager = new DatabaseManager("testdatabase.properties");
        dbManager.createTestConnection();
        return dbManager.conn;
    }

    public static Connection getConnection() {
        if (dbManager == null) {
            dbManager = new DatabaseManager("database.properties");
            dbManager.createConnection();
        }
        return dbManager.conn;
    }

    public ResultSet executeQuery(String sql) {
        ResultSet rs = null;
        try {
            rs = stmt.executeQuery(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return rs;
    }

    public static void close() {
        try {
            dbManager.conn.close();
        } catch (Exception e) {
            System.out.println(e);
            System.exit(0);
        }
    }

    public static void main(String[] args) {
        Connection conn = getConnection();
        int commitId;
        try {
            Statement stmt1 = conn.createStatement();
            Statement stmt2 = conn.createStatement();
            ResultSet r1 = stmt1.executeQuery("select id from scmlog");
            while (r1.next()) {
                commitId = r1.getInt(1);
                ResultSet r2 = stmt2
                        .executeQuery("select file_id from actions where commit_id =" + commitId);
                while (r2.next()) {
                    System.out.print(r2.getInt(1));
                }
            }
        } catch (Exception e) {
            System.out.println(e);
            System.exit(0);
        }

    }



}