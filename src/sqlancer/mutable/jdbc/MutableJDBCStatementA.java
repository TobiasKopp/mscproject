package sqlancer.mutable.jdbc;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MutableJDBCStatementA extends MutableJDBCAbstractStatement {
	
    MutableJDBCStatementA(MutableJDBCAbstractConnection connection, boolean debug)  {
        this.connection = connection;
		this.debug = debug;
    }

    @Override
	public boolean execute(String sql) throws SQLException {
    	if (isClosed) throw new SQLException("Statement already closed.");

    	if (debug) System.out.println("MutableJDBCStatement#execute(): " + sql);

		sql = sql.strip();
        if (sql.isEmpty()) throw new SQLException("Empty sql expression");

    	if (sql.startsWith("CREATE DATABASE")) { return executeCreateDB(sql); }
    	if (sql.startsWith("USE")) { return executeUseDB(sql); }
    	if (sql.startsWith("DROP DATABASE")) { return executeDropDB(sql); }
    	if (sql.startsWith("CREATE TABLE")) { return executeCreateTable(sql); }
    	if (sql.startsWith("INSERT")) { return executeInsert(sql); }
    	if (sql.startsWith("UPDATE")) { return executeUpdateTable(sql); }
    	if (sql.startsWith("DELETE FROM")) { return executeDeleteFromTable(sql); }

    	// Queries
    	if (sql.startsWith("SELECT")
    			|| sql.startsWith(MutableSpecialStatement.GET_ALL_TABLE_NAMES_IN_USE.toString())
    			|| sql.startsWith(MutableSpecialStatement.GET_TABLE_COLUMNS.toString())) {
    		resultSet = executeQuery(sql);
    	}

    	throw new SQLException("Unsupported statement: " + sql);
	}

    @Override
    public ResultSet executeQuery(String query) throws SQLException {
    	if (isClosed) throw new SQLException("Statement already closed.");

		if (debug) System.out.println("MutableJDBCStatementA#executeQuery(): " + query);

    	// Special queries
		if (query.startsWith(MutableSpecialStatement.GET_ALL_TABLE_NAMES_IN_USE.toString())) { return getAllTableNamesInUse(); }
    	if (query.startsWith(MutableSpecialStatement.GET_TABLE_COLUMNS.toString())) { return getTableColumns(query); }

		// Usual queries
    	if (query.startsWith("SELECT")) { return executeSelect(query); }

    	throw new SQLException("Unsupported statement type (executeQuery()): " + query);
    }

    // Get a ResultSet containing all table names in the current database.
    private ResultSet getAllTableNamesInUse() {
		return new MutableJDBCResultSet(this, connection.getSystemWrapper().getDatabaseWrapperInUse().getAllTableNames());
    }

    // Get a ResultSet containing all column names of the specified table in the current database.
    private ResultSet getTableColumns(String query) throws SQLException {
    	String tableName = query.split(" ")[1];
    	MutableTableWrapper table;
    	try {
			table = connection.getSystemWrapper().getTableWrapper(tableName);
			MutableJDBCResultSet res = new MutableJDBCResultSet(this, table.getColumns());
			res.setSeparator("!");
			return res;
		} catch (MutableWrapperException e) {
			// Should usually never happen
			e.printStackTrace();
			throw new SQLException(e.getMessage());
		}
    }

    private ResultSet executeSelect(String query) throws SQLException {
    	if (debug) System.out.println("MutableJDBCStatementA#executeSelect() : " + query);

    	List<String> result = new ArrayList<String>();

		for (String line : connection.executeOnProcess(query)) {
			line = line.replace("\n", "");

			// Ignore lines that do not belong to the result of the query
			if (line.contains("PID")) { continue; }
			if (line.contains("Created")) { continue; }
			if (line.contains("Using")) { continue; }
			if (line == "\n") { continue; }
			if (line.isEmpty()) { continue; }
			if (line.isBlank()) { continue; }

			// Throw SQLException when warning or error occurred
			if (line.contains("warning")) { throw new SQLException(line); }
			if (line.contains("error")) { throw new SQLException(line); }

			// `x rows` indicates end of query result
			if (line.contains("rows")) { break; }
			
			result.add(line);
		}

//		if (debug) System.out.println(result);
		resultSet = new MutableJDBCResultSet(this, result);
		return resultSet;
    }

    private boolean executeCreateDB(String query) throws SQLException {
    	resultSet = new MutableJDBCResultSet(this, connection.executeOnProcess(query));
    	return connection.getSystemWrapper().createDatabase(query);
    }

    private boolean executeUseDB(String query) throws SQLException {
    	resultSet = new MutableJDBCResultSet(this, connection.executeOnProcess(query));
    	return connection.getSystemWrapper().useDatabase(query);
    }

    private boolean executeDropDB(String query) throws SQLException {
    	resultSet = new MutableJDBCResultSet(this, connection.executeOnProcess(query));
    	return connection.getSystemWrapper().dropDatabase(query);
    }

    private boolean executeCreateTable(String query) throws SQLException {
    	resultSet = new MutableJDBCResultSet(this, connection.executeOnProcess(query));
    	return connection.getSystemWrapper().createTable(query);
    }

    private boolean executeInsert(String query) throws SQLException {
    	resultSet = new MutableJDBCResultSet(this, connection.executeOnProcess(query));
    	return connection.getSystemWrapper().insertInto(query);
    }

    private boolean executeUpdateTable(String query) throws SQLException {
    	throw new UnsupportedOperationException("UPDATE not supported yet.");
    	//resultSet = new MutableJDBCResultSet(connection.executeOnProcess(query), this);
    	//return connection.getSystemWrapper().update(query);
    }

    private boolean executeDeleteFromTable(String query) throws SQLException {
    	throw new UnsupportedOperationException("DELETE not supported yet.");
    	//resultSet = new MutableJDBCResultSet(connection.executeOnProcess(query), this);
    	//return connection.getSystemWrapper().deleteFromTable(query);
    }

}
