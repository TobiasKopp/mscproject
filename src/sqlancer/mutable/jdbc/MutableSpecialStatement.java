package sqlancer.mutable.jdbc;

// Workaround to get information about schema
public enum MutableSpecialStatement {
	GET_ALL_TABLE_NAMES_IN_USE("GET_ALL_TABLE_NAMES_IN_USE"),
	GET_TABLE_COLUMNS("GET_TABLE_COLUMNS");
	
	private String textRepr;

    private MutableSpecialStatement(String textRepr) { 
    	this.textRepr = textRepr;
    }
    
    public String toString() {
    	return this.textRepr;
    }
    
}
