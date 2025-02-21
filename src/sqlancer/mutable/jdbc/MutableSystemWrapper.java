package sqlancer.mutable.jdbc;

import java.util.*;

public class MutableSystemWrapper {
	private Map<String, MutableDatabaseWrapper> databases;
	private String dbInUse;

	public MutableSystemWrapper() {
		this.databases = new HashMap<String, MutableDatabaseWrapper>();
	}

	// Getters / Setters
	public Map<String, MutableDatabaseWrapper> getDatabases() { return this.databases; }
	public String getDbInUse() { return this.dbInUse; }
	private void setDbInUse(String db) { this.dbInUse = db; }

	// Get all table names of all databases as list
	public List<String> getAllTableNames() {
		List<String> names = new ArrayList<String>();
		for (MutableDatabaseWrapper db : this.databases.values()) {
			names.addAll(db.getAllTableNames());
		}

		return names;
	}

	public MutableDatabaseWrapper getDatabaseWrapper(String name) {
		return databases.get(name);
	}

	public MutableDatabaseWrapper getDatabaseWrapperInUse() {
		return databases.get(dbInUse);
	}

	// Get a MutableTableWrapper for given table name from the currently used database
	public MutableTableWrapper getTableWrapper(String name) throws MutableWrapperException {
		return this.databases.get(getDbInUse()).getTableWrapper(name);
	}


	/*----------------------------------------------------------------------------------
			DATABASES
	----------------------------------------------------------------------------------*/
	// Create a new database
	public boolean createDatabase(String create) {
		String name = create.split(" ")[2].replace(";", "");
		if (databases.containsKey(name)) {
			return false;
		} else {
			MutableDatabaseWrapper db = new MutableDatabaseWrapper(name);
			databases.put(name, db);
			return true;
		}
	}

	// Drop a database
	// DROP DATABASE vs DROP DATABASE IF EXISTS
	public boolean dropDatabase(String drop) {
		List<String> split = Arrays.asList(drop.split(" "));
		String name = split.get(split.size()-1).replace(";", "");
		this.databases.remove(name);
		return true;
	}

	// Use a database
	public boolean useDatabase(String use) {
		String name = use.split(" ")[1].replace(";", "");
		this.setDbInUse(name);
		return true;
	}


	/*----------------------------------------------------------------------------------
			Tables
	----------------------------------------------------------------------------------*/

	// Create a table in the currently used database
	public boolean createTable(String stmt) {
		if (dbInUse == null) { return false; }
		return databases.get(dbInUse).createTable(stmt);
	}

	// Try to insert in current db
	public boolean insertInto(String stmt) {
		if (dbInUse == null) { return false; }
		return this.databases.get(getDbInUse()).insertInto(stmt);
    }

	// Try to update in current db
	public boolean update(String stmt) {
		if (dbInUse == null) { return false; }
		return this.databases.get(getDbInUse()).update(stmt);
    }

	// Try to delete from current db
	public boolean deleteFromTable(String stmt) {
		if (dbInUse == null) { return false; }
        return this.databases.get(getDbInUse()).delete(stmt);
    }
}
