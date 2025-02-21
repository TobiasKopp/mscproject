package sqlancer.mutable.jdbc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class MutableDatabaseWrapper {
	private final String name;
	private Map<String, MutableTableWrapper> tables;

	public MutableDatabaseWrapper(String name) {
		this.name = name;
		this.tables = new HashMap<String, MutableTableWrapper>();
	}

	// Getters
	public String getName() { return this.name; }
	public Map<String, MutableTableWrapper> getTables() { return this.tables; }

	// Get all table names in this database
	public List<String> getAllTableNames() {
		List<String> tableNames = new ArrayList<String>();
		for (Entry<String, MutableTableWrapper> entry : this.getTables().entrySet()) {
			if (entry.getKey()!=null) {
				tableNames.add(entry.getKey());
			}
		}
		return tableNames;
	}

	// Get a MutableTableWrapper for given table name
	public MutableTableWrapper getTableWrapper(String name) throws MutableWrapperException {
		if (this.tables.containsKey(name)) {
			return this.tables.get(name);
		} else {
			throw new MutableWrapperException("Table " + name + " does not exist.");
		}
	}


	public boolean createTable(String stmt) {
		MutableTableWrapper tbl = new MutableTableWrapper(stmt);
		if (this.tables.containsKey(tbl.getName())) {
			return false;
		} else {
			this.tables.put(tbl.getName(), tbl);
			return true;
		}
	}

	public boolean insertInto(String stmt) {
		String name = stmt.split(" ")[2];
		if (this.tables.containsKey(name)) {
			this.tables.get(name).addInsert(stmt);
			return true;
		} else {
			return false;
		}
	}

	public boolean update(String stmt) {
		String name = stmt.split(" ")[1];
		if (this.tables.containsKey(name)) {
			this.tables.get(name).addUpdate(stmt);
			return true;
		} else {
			return false;
		}
	}

	public boolean delete(String stmt) {
		String name = stmt.split(" ")[2];
		if (this.tables.containsKey(name)) {
			this.tables.get(name).addDelete(stmt);
			return true;
		} else {
			return false;
		}
	}
}
