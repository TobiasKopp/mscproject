package sqlancer.mutable;

import com.datastax.oss.driver.shaded.guava.common.base.CharMatcher;

import sqlancer.GlobalState;
import sqlancer.IgnoreMeException;
import sqlancer.Randomly;
import sqlancer.SQLConnection;
import sqlancer.common.DBMSCommon;
import sqlancer.common.query.SQLQueryAdapter;
import sqlancer.common.query.SQLancerResultSet;
import sqlancer.common.schema.*;
import sqlancer.mutable.MutableProvider.MutableGlobalState;
import sqlancer.mutable.MutableSchema.MutableTable;
import sqlancer.mutable.ast.MutableConstant;
import sqlancer.mutable.ast.MutableConstant.*;
import sqlancer.mutable.jdbc.MutableSpecialStatement;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.stream.IntStream;


public class MutableSchema extends AbstractSchema<MutableGlobalState, MutableTable> {

    public enum MutableDataType {

        BOOL, CHAR, VARCHAR, DATE, DATETIME, INT, FLOAT, DOUBLE, DECIMAL;

        public static MutableDataType getRandomWithoutNull() {
            MutableDataType dt;
            do {
                dt = Randomly.fromOptions(values());
            } while (dt == null);
            return dt;
        }

    }

    public static class MutableCompositeDataType {

        private final MutableDataType dataType;

        private final int size;
        private final int optional_size;

        public MutableCompositeDataType(MutableDataType dataType, int size) {
            this.dataType = dataType;
            this.size = size;
            this.optional_size = -1;
        }

        public MutableCompositeDataType(MutableDataType dataType, int size, int optional_size) {
            this.dataType = dataType;
            this.size = size;
            this.optional_size = optional_size;
        }

        public MutableDataType getPrimitiveDataType() {
            return dataType;
        }

        public int getSize() {
            if (size == -1) { throw new AssertionError(this); }
            return size;
        }
        public int getOptionalSize() {
            if (optional_size == -1) { throw new AssertionError(this); }
            return optional_size;
        }

        public static MutableCompositeDataType getRandomWithoutNull() {
            final int max_char_size = 1024;
            MutableDataType type = MutableDataType.getRandomWithoutNull();
            int size = -1;
            int optional_size = -1;
            switch (type) {
                case INT:
                	size = Randomly.fromOptions(1, 2, 4, 8);
                	break;
                case CHAR:
                case VARCHAR:
                    List<Integer> range = IntStream.rangeClosed(1, max_char_size)
                            .boxed().toList();
                    size = Randomly.fromList(range);
                    break;
                case BOOL:
                case DATE:
                case DATETIME:
                case FLOAT:
                case DOUBLE:
                	size = 0;
                	break;
                case DECIMAL:
                    size = Randomly.fromList(IntStream.rangeClosed(1, 19).boxed().toList());
                    optional_size = Randomly.fromList(IntStream.rangeClosed(1, 19).boxed().toList());
                    return new MutableCompositeDataType(type, size, optional_size);
                default:
                	throw new AssertionError(type);
            }

            return new MutableCompositeDataType(type, size);
        }

        @Override
        public String toString() {
            switch (getPrimitiveDataType()) {
            case INT:
                return String.format("INT(%d)", size);
            case CHAR:
                return String.format("CHAR(%d)", size);
            case VARCHAR:
                return String.format("VARCHAR(%d)", size);
            case BOOL:
                return "BOOL";
            case DATE:
                return "DATE";
            case DATETIME:
                return "DATETIME";
            case FLOAT:
                return "FLOAT";
            case DOUBLE:
                return "DOUBLE";
            case DECIMAL:
                if (optional_size<0) { return String.format("DECIMAL(%d)", size); }
                else { return String.format("DECIMAL(%d,%d)", size, optional_size); }
            default:
                throw new AssertionError(getPrimitiveDataType());
            }
        }

    }

    // Representation of a column in a table as a java object. Extends `AbstractTableColumn`.
    // Consists of a column name, type, and constraints.
    public static class MutableColumn extends AbstractTableColumn<MutableTable, MutableCompositeDataType> {

        private final boolean isPrimaryKey;
        private final boolean isNullable;
        private final boolean isUnique;

        public MutableColumn(
    		String name,
    		MutableCompositeDataType columnType,
    		boolean isPrimaryKey,
    		boolean isNullable,
    		boolean isUnique
		) {
            super(name, null, columnType);
            this.isPrimaryKey = isPrimaryKey;
            this.isNullable = isNullable;
            this.isUnique = isUnique;
        }

        public boolean isPrimaryKey() { return isPrimaryKey; }

        public boolean isNullable() { return isNullable; }

        public boolean isUnique() { return isUnique; }

        public static MutableColumn createDummy(String name) {
        	MutableCompositeDataType type = new MutableCompositeDataType(MutableDataType.INT, 4);
            return new MutableColumn(name, type, false, false, false);
        }

    }

    // Representation of a table as a java object. Extends `AbstractRelationalTable`.
    // Consists of a table name and a list of columns.
    public static class MutableTable extends AbstractRelationalTable<MutableColumn, TableIndex, MutableGlobalState> {

        public MutableTable(String tableName, List<MutableColumn> columns) {

            super(tableName, columns, Collections.emptyList(), false);
            for (MutableColumn c : columns) {
                c.setTable(this);
            }
        }

    }

    // Representation of a database as a java object. Extends `AbstractTables`.
    // Basically just a list of tables.
    public static class MutableTables extends AbstractTables<MutableTable, MutableColumn> {

        public MutableTables(List<MutableTable> tables) {
            super(tables);
        }

        // Get a random row (represented as a `RowValue`) from the list of tables
        public MutableRowValue getRandomRowValue(MutableGlobalState state) throws SQLException {
        	// Get all columns with alias
        	String selectColumns = columnNamesAsString(
                    c -> c.getTable().getName() + "." + c.getName() + " AS " + c.getTable().getName() + c.getName());
            // Create a select statement that fetches ALL rows of the cartesian product of all tables of THIS
        	String selectAllRows = "SELECT " + selectColumns + " FROM " + tableNamesAsString() + ";";
        	if (state.getDbmsSpecificOptions().debug) System.out.println("getRandomRowValue(): " + selectAllRows);
        	
            List<Map<MutableColumn, MutableConstant>> allRows = new ArrayList<Map<MutableColumn, MutableConstant>>();
            
            try (Statement s = state.getConnection().createStatement()) {
                if (state.getOptions().logEachSelect()) {
                    state.getLogger().writeCurrent(selectAllRows);
                }
                ResultSet resultSet = s.executeQuery(selectAllRows);

                if (resultSet == null) {
                    throw new IgnoreMeException();
                }

                if (!resultSet.next()) {
                    throw new AssertionError("Could not find any rows! " + selectAllRows + "\n");
//                    throw new IgnoreMeException();
                }

                // Parse each row
                while (resultSet.next()) {
                	Map<MutableColumn, MutableConstant> currentRow = new HashMap<>();
                	
                	// Parse each column
                	for (int i = 0; i < getColumns().size(); i++) {
                        MutableColumn column = getColumns().get(i);
//                        int columnIndex = resultSet.findColumn(column.getTable().getName() + column.getName());
//                        assert columnIndex == i + 1;
                        int columnIndex = i + 1;
                        MutableConstant constant;
                        if (resultSet.getString(columnIndex) == null) {
                            constant = new MutableNullConstant();
                        } else {
                            switch (column.getType().getPrimitiveDataType()) {
                            case INT:
                                constant = new MutableIntegerConstant(resultSet.getLong(columnIndex));
                                break;
                            case FLOAT:
                            case DOUBLE:
                            case DECIMAL:
                                constant = new MutableFloatingPointConstant(resultSet.getDouble(columnIndex));
                                break;
                            case BOOL:
                                constant = new MutableBooleanConstant(resultSet.getBoolean(columnIndex));
                                break;
                            case CHAR:
                            case VARCHAR:
                                constant = new MutableStringLiteral(resultSet.getString(columnIndex));
                                break;
                            case DATE:
                            	constant = new MutableDateConstant(resultSet.getDate(columnIndex));
                            	break;
                            case DATETIME:
                            	constant = new MutableDatetimeConstant(resultSet.getTimestamp(columnIndex));
                            	break;
                            default:
                                throw new IgnoreMeException();
                            }
                        }
                        currentRow.put(column, constant);
                    }
                	allRows.add(currentRow);
                }

                assert !resultSet.next();

                // Randomly select pivot row
                Map<MutableColumn, MutableConstant> randomSelectedRow = Randomly.fromList(allRows);
                if (state.getDbmsSpecificOptions().debug) System.out.println("Pivot Row: " + randomSelectedRow);
                return new MutableRowValue(this, randomSelectedRow);
            }
        }
    }

    // Representation of a whole dbms schema as a java object.
    public MutableSchema(List<MutableTable> databaseTables) {
        super(databaseTables);
    }

    public MutableTables getRandomTableNonEmptyTables() {
        return new MutableTables(Randomly.nonEmptySubset(getDatabaseTables()));
    }

    private static MutableCompositeDataType getColumnType(String typeString) {
        MutableDataType primitiveType;
        int size = -1;
        if (typeString.startsWith("CHAR")) {
            size = Integer.parseInt(CharMatcher.inRange('0', '9').retainFrom(typeString));
            // ^^ this is faster than       Integer.parseInt(typeString.replaceAll("[^0-9]", ""));
            return new MutableCompositeDataType(MutableDataType.CHAR, size);
        } else if (typeString.startsWith("VARCHAR")) {
            size = Integer.parseInt(CharMatcher.inRange('0', '9').retainFrom(typeString));
            return new MutableCompositeDataType(MutableDataType.VARCHAR, size);
        } else if (typeString.startsWith("INT")) {
            size = Integer.parseInt(CharMatcher.inRange('0', '9').retainFrom(typeString));
            return new MutableCompositeDataType(MutableDataType.INT, size);
        } else if (typeString.startsWith("DECIMAL")) {
        	String[] split = typeString.split(",");
            size = Integer.parseInt(CharMatcher.inRange('0', '9').retainFrom(split[0]));
            int optionalSize = Integer.parseInt(CharMatcher.inRange('0', '9').retainFrom(split[1]));
            return new MutableCompositeDataType(MutableDataType.DECIMAL, size, optionalSize);
        }
        switch (typeString) {
            case "BOOL":
                primitiveType = MutableDataType.BOOL;
                size = 0;
                break;
            case "DATE":
                primitiveType = MutableDataType.DATE;
                size = 0;
                break;
            case "DATETIME":
                primitiveType = MutableDataType.DATETIME;
                size = 0;
                break;
            case "FLOAT":
                primitiveType = MutableDataType.FLOAT;
                size = 0;
                break;
            case "DOUBLE":
                primitiveType = MutableDataType.DOUBLE;
                size = 0;
                break;
            default:
            	throw new AssertionError(typeString);
        }
        return new MutableCompositeDataType(primitiveType, size);
    }


    // Create a whole schema from the given connection containing all tables and their columns.
    public static MutableSchema fromConnection(SQLConnection con, String databaseName) throws SQLException {
    	List<MutableTable> databaseTables = new ArrayList<>();
        List<String> tableNames = getTableNames(con);
        for (String tableName : tableNames) {
            if (DBMSCommon.matchesIndexName(tableName)) {
                continue; // TODO: unexpected?
            }
            List<MutableColumn> databaseColumns = getTableColumns(con, tableName);
            MutableTable t = new MutableTable(tableName, databaseColumns);
            databaseTables.add(t);

        }
        return new MutableSchema(databaseTables);
    }

    private static List<String> getTableNames(SQLConnection con) throws SQLException {
        List<String> tableNames = new ArrayList<>();
        Statement s = con.createStatement();
        ResultSet rs = s.executeQuery(MutableSpecialStatement.GET_ALL_TABLE_NAMES_IN_USE.toString());
        while (rs.next()) {
            tableNames.add(rs.getString(1));
        }
        return tableNames;
    }

    private static List<MutableColumn> getTableColumns(SQLConnection con, String tableName) throws SQLException {
    	List<MutableColumn> columns = new ArrayList<>();
        try (Statement s = con.createStatement()) {
            try (ResultSet rs = s.executeQuery(MutableSpecialStatement.GET_TABLE_COLUMNS + " " + tableName)) {
                while (rs.next()) {
                	// TODO better way to parse column def ?
                	String columnDef = rs.getString(1);
                	String[] specs = columnDef.split(" ");
                    String columnName = specs[0];
                    String dataType = specs[1];
                    boolean isNullable = true;
                    boolean isPrimaryKey = false;
                    boolean isUnique = false;
                	if (columnDef.contains("NOT NULL")) { isNullable = false; }
                	if (columnDef.contains("PRIMARY KEY")) { isPrimaryKey = true; }
                	if (columnDef.contains("UNIQUE")) { isUnique = true; }
                    MutableColumn c = new MutableColumn(columnName, getColumnType(dataType), isPrimaryKey, isNullable, isUnique);
                    columns.add(c);
                }
            }
        }
        return columns;
    }


    public static class MutableRowValue extends AbstractRowValue<MutableTables, MutableColumn, MutableConstant> {

        public MutableRowValue(MutableTables tables, Map<MutableColumn, MutableConstant> values) {
            super(tables, values);
        }

    }

}
