package Test;

import java.io.FileInputStream;
import java.sql.Connection;

import org.dbunit.database.DatabaseConfig;
import org.dbunit.database.DatabaseConnection;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.xml.FlatXmlDataSetBuilder;
import org.dbunit.ext.mysql.MySqlDataTypeFactory;
import org.dbunit.operation.DatabaseOperation;

import Database.DatabaseManager;

public class TestHelper {

    public static Connection getJDBCConnection() throws Exception {
        return DatabaseManager.getTestConnection();
    }

    public static IDatabaseConnection getDBUnitConnection() throws Exception {
        IDatabaseConnection conn = new DatabaseConnection(getJDBCConnection());
        DatabaseConfig config = conn.getConfig();
        config.setProperty(DatabaseConfig.PROPERTY_DATATYPE_FACTORY,
                new MySqlDataTypeFactory());
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
        final IDataSet data = getDataSet();
        try {
            DatabaseOperation.CLEAN_INSERT.execute(conn, data);
        } finally {
            // conn.close();
        }
    }

    public static void cleanDatabase() throws Exception {
        final IDatabaseConnection conn = getDBUnitConnection();
        final IDataSet data = getDataSet();
        try {
            DatabaseOperation.DELETE_ALL.execute(conn, data);
        } finally {
            // conn.close();
        }

    }

}