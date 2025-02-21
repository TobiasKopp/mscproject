package sqlancer.mutable.jdbc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MutableTableWrapper {
	private String name;
	private List<String> columns;
	private String createStmt;
	private List<String> alteringStmts;

	public MutableTableWrapper(String createStmt) {
		this.createStmt = createStmt;
		this.alteringStmts = new ArrayList<String>();
		this.name = createStmt.split(" ")[2];
	}

	// Getters
	public String getName() { return this.name; }
	public String getCreate() { return this.createStmt; }
	public List<String> getAlteringStmts() { return this.alteringStmts; }

	public List<String> getColumns() {
		if (columns != null) {
			return columns;
		}

		// Isolate columns from rest
    	String s = createStmt.substring(createStmt.indexOf("("));
    	s = s.replace(";", "");
    	s = s.substring(1, s.length()-1);

		// IMPORTANT NOTE: Any column of type DECIMAL must be declared as DECIMAL (p,s) (without any space after the comma)
		// in order for this to work. Otherwise, it would be hard to parse the statements.
		columns = Arrays.asList(s.split(", "));
		return columns;
	}


	public void addInsert(String stmt) {
		this.alteringStmts.add(stmt);
	}

	public void addUpdate(String stmt) {
		this.alteringStmts.add(stmt);
	}

	public void addDelete(String stmt) {
		this.alteringStmts.add(stmt);
	}

}
