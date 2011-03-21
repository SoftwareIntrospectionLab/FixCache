package edu.ucsc.sil.fixcache.database;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import edu.ucsc.sil.fixcache.util.Dates;

public class DatabaseManager {

    private static DatabaseManager dbManager;
    private String drivername, databasename, username, password;
    private Connection conn = null;
    Statement stmt;

    public void createConnection(String propertyFile) {

        Dates.initializeFormat(Dates.DBtype.mysql);
        File file = new File(propertyFile);
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            Properties prop = new Properties();
            prop.load(fis);
            // Enumeration enums = prop.propertyNames();
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
            // stmt = conn.createStatement();

        } catch (Exception e) {
            System.out.println(e);
            System.exit(0);
        }
    }

    public void createTestConnection() {
        Dates.initializeFormat(Dates.DBtype.sqlite);
        String s;
        StringBuffer sb = new StringBuffer();

        if (conn == null) {
            try {
                Class.forName("org.sqlite.JDBC").newInstance();
                conn = DriverManager.getConnection("jdbc:sqlite::memory:");

                FileReader fr = new FileReader(new File("test.sql"));
                BufferedReader br = new BufferedReader(fr);

                while ((s = br.readLine()) != null) {
                    sb.append(s);
                }
                br.close();

                // here is our splitter ! We use ";" as a delimiter for each
                // request
                // then we are sure to have well formed statements
                String[] inst = sb.toString().split(";");
                Statement st = conn.createStatement();

                for (int i = 0; i < inst.length; i++) {
                    // we ensure that there is no spaces before or after the
                    // request
                    // string
                    // in order to not execute empty statements
                    if (!inst[i].trim().equals("")) {
                        st.executeUpdate(inst[i]);
                        // System.out.println(">>" + inst[i]);
                    }
                }
                st.close();

            } catch (Exception e) {
                System.err.println("*** Error : " + e.toString());
                System.err.println("*** ");
                System.err.println("*** Error : ");
                e.printStackTrace();
                System.err
                        .println("################################################");
                System.err.println(sb.toString());
            }

        } else {
            try {
                Class.forName("org.sqlite.JDBC").newInstance();
                conn = DriverManager.getConnection("jdbc:sqlite::memory:");
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

    }

    public static Connection getTestConnection() {
        dbManager = new DatabaseManager();
        dbManager.createTestConnection();
        return dbManager.conn;
    }

    public static Connection getConnection() {
        if (dbManager == null) {
            dbManager = new DatabaseManager();
            dbManager.createConnection("database.properties");
        }
        return dbManager.conn;
    }

    public static void close() {
        try {
            dbManager.conn.close();
        } catch (Exception e) {
            System.out.println(e);
            System.exit(0);
        }
    }

    /**
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
                        .executeQuery("select file_id from actions where commit_id ="
                                + commitId);
                while (r2.next()) {
                    System.out.print(r2.getInt(1));
                }
            }
        } catch (Exception e) {
            System.out.println(e);
            System.exit(0);
        }

		close();
    }
    **/

}