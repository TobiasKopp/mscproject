package sqlancer.mutable.jdbc;

import java.sql.*;
import java.util.Properties;
import java.util.logging.Logger;

public class MutableJDBCDriver implements Driver {
    public static final String PREFIX = "jdbc:mutable:";
    private static final String DEBUG_PREFIX = "debug:";
    private static final Driver INSTANCE = new MutableJDBCDriver();
    private static boolean registered = false;

    public MutableJDBCDriver() { }

    @Override
    public Connection connect(String url, Properties properties) throws SQLException {
        if (acceptsURL(url)) {
        	boolean debug = url.contains(DEBUG_PREFIX);
        	String binary = url.replace(PREFIX, "").replace(DEBUG_PREFIX, "");
            return new MutableJDBCConnectionA(binary, debug);
        } else throw new SQLException("url not supported");
    }

    @Override
    public boolean acceptsURL(String url) throws SQLException {
        return url.startsWith(PREFIX);
    }

    @Override
    public DriverPropertyInfo[] getPropertyInfo(String s, Properties properties) throws SQLException {
        return new DriverPropertyInfo[0];
    }

    @Override
    public int getMajorVersion() {
        return 0;
    }

    @Override
    public int getMinorVersion() {
        return 0;
    }

    @Override
    public boolean jdbcCompliant() {
        return true;
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return null;
    }

    // Creates a driver and registers it. Makes sure that only exactly one driver exists.
    public static synchronized Driver load() {
        if (!registered) {
            registered = true;
            try {
                DriverManager.registerDriver(INSTANCE);
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
        }

        return INSTANCE;
    }

    // Perform load() when this class is created
    static {
        load();
    }
}
