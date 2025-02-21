package sqlancer.mutable.jdbc;

import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.DatabaseMetaData;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Struct;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;

public abstract class MutableJDBCAbstractConnection implements java.sql.Connection {
    
    protected String path_to_binary;
    protected MutableSystemWrapper systemWrapper;
    protected final String args = " --plan-enumerator PEall --quiet";
    protected boolean isClosed = false;
    protected boolean debug;
    
    public MutableSystemWrapper getSystemWrapper() { return this.systemWrapper; }

    public List<String> getAllTableNames() {
        return new ArrayList<String>(this.systemWrapper.getAllTableNames());
    }
    
    protected abstract List<String> executeOnProcess(String sql) throws SQLException;
    
	/*----------------------------------------------------------------------------------
		Mostly unimplemented methods
	----------------------------------------------------------------------------------*/
	
	@Override
	public PreparedStatement prepareStatement(String s) throws SQLException {
		// TODO implement?
		throw new UnsupportedOperationException();
	}
	
	@Override
        public CallableStatement prepareCall(String s) throws SQLException {
        throw new UnsupportedOperationException();
	}
	
	@Override
    	public String nativeSQL(String s) throws SQLException {
    	throw new UnsupportedOperationException();
	}
	
	@Override
    	public void setAutoCommit(boolean b) throws SQLException {
    	throw new UnsupportedOperationException();
	}
	
	@Override
    	public boolean getAutoCommit() throws SQLException {
    	throw new UnsupportedOperationException();
	}
	
	@Override
    	public void commit() throws SQLException {
    	throw new UnsupportedOperationException();
	}
	
	@Override
    	public void rollback() throws SQLException {
    	throw new UnsupportedOperationException();
	}
	
	@Override
    	public DatabaseMetaData getMetaData() throws SQLException {
    	throw new UnsupportedOperationException();
	}
	
	@Override
    	public void setReadOnly(boolean b) throws SQLException {
    	throw new UnsupportedOperationException();
	}
	
	@Override
    	public boolean isReadOnly() throws SQLException {
    	throw new UnsupportedOperationException();
	}
	
	@Override
    	public void setCatalog(String s) throws SQLException {
    	throw new UnsupportedOperationException();
	}
	
	@Override
    	public String getCatalog() throws SQLException {
    	throw new UnsupportedOperationException();
	}
	
	@Override
    	public void setTransactionIsolation(int i) throws SQLException {
    	throw new UnsupportedOperationException();
	}
	
	@Override
    	public int getTransactionIsolation() throws SQLException {
    	throw new UnsupportedOperationException();
	}
	
	@Override
    	public SQLWarning getWarnings() throws SQLException {
    	throw new UnsupportedOperationException();
	}
	
	@Override
    	public void clearWarnings() throws SQLException {
    	throw new UnsupportedOperationException();
	}
	
	@Override
    	public Statement createStatement(int i, int i1) throws SQLException {
    	throw new UnsupportedOperationException();
	}
	
	@Override
    	public PreparedStatement prepareStatement(String s, int i, int i1) throws SQLException {
    	throw new UnsupportedOperationException();
	}
	
	@Override
    	public CallableStatement prepareCall(String s, int i, int i1) throws SQLException {
    	throw new UnsupportedOperationException();
	}
	
	@Override
    	public Map<String, Class<?>> getTypeMap() throws SQLException {
    	throw new UnsupportedOperationException();
	}
	
	@Override
    	public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
    	throw new UnsupportedOperationException();
	}
	
	@Override
    	public void setHoldability(int i) throws SQLException {
    	throw new UnsupportedOperationException();
	}
	
	@Override
    	public int getHoldability() throws SQLException {
    	throw new UnsupportedOperationException();
	}
	
	@Override
    	public Savepoint setSavepoint() throws SQLException {
    	throw new UnsupportedOperationException();
	}
	
	@Override
    	public Savepoint setSavepoint(String s) throws SQLException {
    	throw new UnsupportedOperationException();
	}
	
	@Override
    	public void rollback(Savepoint savepoint) throws SQLException {
    	throw new UnsupportedOperationException();
	}
	
	@Override
    	public void releaseSavepoint(Savepoint savepoint) throws SQLException {
    	throw new UnsupportedOperationException();
	}
	
	@Override
    	public Statement createStatement(int i, int i1, int i2) throws SQLException {
    	throw new UnsupportedOperationException();
	}
	
	@Override
    	public PreparedStatement prepareStatement(String s, int i, int i1, int i2) throws SQLException {
    	throw new UnsupportedOperationException();
	}
	
	@Override
    	public CallableStatement prepareCall(String s, int i, int i1, int i2) throws SQLException {
    	throw new UnsupportedOperationException();
	}
	
	@Override
    	public PreparedStatement prepareStatement(String s, int i) throws SQLException {
    	throw new UnsupportedOperationException();
	}
	
	@Override
    	public PreparedStatement prepareStatement(String s, int[] ints) throws SQLException {
    	throw new UnsupportedOperationException();
	}
	
	@Override
    	public PreparedStatement prepareStatement(String s, String[] strings) throws SQLException {
    	throw new UnsupportedOperationException();
	}
	
	@Override
    	public Clob createClob() throws SQLException {
    	throw new UnsupportedOperationException();
	}
	
	@Override
    	public Blob createBlob() throws SQLException {
    	throw new UnsupportedOperationException();
	}
	
	@Override
    	public NClob createNClob() throws SQLException {
    	throw new UnsupportedOperationException();
	}
	
	@Override
    	public SQLXML createSQLXML() throws SQLException {
    	throw new UnsupportedOperationException();
	}
	
	@Override
    	public boolean isValid(int i) throws SQLException {
    	throw new UnsupportedOperationException();
	}
	
	@Override
    	public void setClientInfo(String s, String s1) throws SQLClientInfoException {
    	throw new UnsupportedOperationException();
	}
	
	@Override
    	public void setClientInfo(Properties properties) throws SQLClientInfoException {
    	throw new UnsupportedOperationException();
	}
	
	@Override
    	public String getClientInfo(String s) throws SQLException {
    	throw new UnsupportedOperationException();
	}
	
	@Override
    	public Properties getClientInfo() throws SQLException {
    	throw new UnsupportedOperationException();
	}
	
	@Override
    	public Array createArrayOf(String s, Object[] objects) throws SQLException {
    	throw new UnsupportedOperationException();
	}
	
	@Override
    	public Struct createStruct(String s, Object[] objects) throws SQLException {
    	throw new UnsupportedOperationException();
	}
	
	@Override
    	public void setSchema(String s) throws SQLException {
    	throw new UnsupportedOperationException();
	}
	
	@Override
    	public String getSchema() throws SQLException {
    	throw new UnsupportedOperationException();
	}
	
	@Override
    	public void setNetworkTimeout(Executor executor, int i) throws SQLException {
    	throw new UnsupportedOperationException();
	}
	
	@Override
    	public int getNetworkTimeout() throws SQLException {
    	throw new UnsupportedOperationException();
	}
	
	@Override
    	public <T> T unwrap(Class<T> aClass) throws SQLException {
    	throw new UnsupportedOperationException();
	}
	
	@Override
    	public boolean isWrapperFor(Class<?> aClass) throws SQLException {
    	throw new UnsupportedOperationException();
	}
}
