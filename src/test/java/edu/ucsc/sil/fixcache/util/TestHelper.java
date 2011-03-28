package edu.ucsc.sil.fixcache.util;

import java.io.FileInputStream;
import java.sql.Connection;

import org.dbunit.database.DatabaseConfig;
import org.dbunit.database.DatabaseConnection;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.xml.FlatXmlDataSetBuilder;
import org.dbunit.operation.DatabaseOperation;

import edu.ucsc.sil.fixcache.database.DatabaseManager;
import edu.ucsc.sil.fixcache.database.SqliteDataTypeFactory;

public class TestHelper {

    public static Connection getJDBCConnection() throws Exception {
        return DatabaseManager.getTestConnection();
    }

    public static IDatabaseConnection getDBUnitConnection() throws Exception {
        IDatabaseConnection conn = new DatabaseConnection(getJDBCConnection());

        return conn;
    }

    public static IDataSet getDataSet() throws Exception {
        return new FlatXmlDataSetBuilder().build(new FileInputStream(
                "fixcache.xml"));
    }

    protected DatabaseOperation getSetUpOperation() throws Exception {
        return DatabaseOperation.REFRESH;
    }

    protected DatabaseOperation getTearDownOperation() throws Exception {
        return DatabaseOperation.NONE;
    }

    public static void handleSetUpOperation() throws Exception {

        final IDatabaseConnection conn = getDBUnitConnection();
        configureConnection(conn);

        final IDataSet data = getDataSet();
        try {
            DatabaseOperation.CLEAN_INSERT.execute(conn, data);
        } finally {
            // conn.close();
        }
    }

    public static void cleanDatabase() throws Exception {
        final IDatabaseConnection conn = getDBUnitConnection();
        configureConnection(conn);
        
        final IDataSet data = getDataSet();
        try {
            DatabaseOperation.DELETE_ALL.execute(conn, data);
        } finally {
            // conn.close();
        }
    }
    
    private static void configureConnection(IDatabaseConnection conn) {
        DatabaseConfig config = conn.getConfig();
        config.setProperty(DatabaseConfig.PROPERTY_DATATYPE_FACTORY, 
            new SqliteDataTypeFactory());
    }
}