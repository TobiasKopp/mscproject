package sqlancer.mutable.gen;

import sqlancer.Randomly;
import sqlancer.common.query.ExpectedErrors;
import sqlancer.common.query.SQLQueryAdapter;
import sqlancer.mutable.MutableErrors;
import sqlancer.mutable.MutableProvider.MutableGlobalState;
import sqlancer.mutable.MutableSchema.MutableColumn;
import sqlancer.mutable.MutableSchema.MutableCompositeDataType;

import java.util.ArrayList;
import java.util.List;

public class MutableTableGenerator {

    public static SQLQueryAdapter generate(MutableGlobalState globalState) {
        return new MutableTableGenerator().getQuery(globalState);
    }

    public SQLQueryAdapter getQuery(MutableGlobalState globalState) {
        ExpectedErrors errors = new ExpectedErrors();
        StringBuilder sb = new StringBuilder();
        String tableName = globalState.getSchema().getFreeTableName();
        MutableErrors.addCommonErrors(errors);
    	errors.add("Table " + tableName + " already exists in database");

        sb.append("CREATE TABLE ");
        sb.append(tableName);
        sb.append(" (");
        List<MutableColumn> columns = getNewColumns(tableName);

        for (int i = 0; i < columns.size(); i++) {
            if (i != 0) {
                sb.append(", ");
            }
            MutableColumn col = columns.get(i);
            sb.append(col.getName());
            sb.append(" ");
            sb.append(col.getType());
            // Constraints
            if (globalState.getDbmsSpecificOptions().testPrimaryKeyConstraints && col.isPrimaryKey()) {
                sb.append(" PRIMARY KEY");
            }
            if (globalState.getDbmsSpecificOptions().testNotNullConstraints && !col.isNullable()) {
                sb.append(" NOT NULL");
            }
            if (globalState.getDbmsSpecificOptions().testUniqueConstraints && col.isUnique()) {
                sb.append(" UNIQUE");
            }

            errors.add("Attribute " + col.getName() + " cannot be defined with type");
            errors.add("Attribute " + col.getName() + " occurs multiple times in defintion of table " + tableName);
            errors.add("Duplicate definition of primary key as attribute");
            errors.add("Duplicate definition of attribute " + col.getName() + " as UNIQUE");
            errors.add("Duplicate definition of attribute " + col.getName() + " as NOT NULL");
        }
        sb.append(");");
        
        if (globalState.getDbmsSpecificOptions().debug) {
        	System.out.println(sb.toString());
        }
        
        return new SQLQueryAdapter(sb.toString(), errors, true);
    }


    // Random generation of columns
    private static List<MutableColumn> getNewColumns(String tableName) {
        List<MutableColumn> columns = new ArrayList<>();
        boolean hasPK = false;
        for (int i = 0; i < Randomly.smallNumber() + 1; i++) {
            String columnName = String.format("c%d", i);
            boolean primary_key = Randomly.getBoolean() && !hasPK;
            boolean isNullable = Randomly.getBoolean();
            boolean unique = Randomly.getBooleanWithRatherLowProbability();
            MutableCompositeDataType columnType = MutableCompositeDataType.getRandomWithoutNull();
            columns.add(new MutableColumn(columnName, columnType, primary_key, isNullable, unique));
            hasPK = hasPK || primary_key;
        }
        return columns;
    }

}
