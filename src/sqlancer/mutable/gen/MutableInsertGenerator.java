package sqlancer.mutable.gen;

import sqlancer.Randomly;
import sqlancer.common.gen.AbstractInsertGenerator;
import sqlancer.common.query.ExpectedErrors;
import sqlancer.common.query.SQLQueryAdapter;
import sqlancer.mutable.MutableErrors;
import sqlancer.mutable.MutableProvider;
import sqlancer.mutable.MutableSchema;
import sqlancer.mutable.MutableSchema.MutableColumn;
import sqlancer.mutable.MutableToStringVisitor;

import java.util.List;

public class MutableInsertGenerator extends AbstractInsertGenerator<MutableColumn> {

    private final MutableProvider.MutableGlobalState globalState;
    private final ExpectedErrors errors = new ExpectedErrors();

    public MutableInsertGenerator(MutableProvider.MutableGlobalState globalState) {
        this.globalState = globalState;
    }

    public static SQLQueryAdapter getQuery(MutableProvider.MutableGlobalState globalState) {
        return new MutableInsertGenerator(globalState).generate();
    }

    private SQLQueryAdapter generate() {
    	// Create statement
        MutableSchema.MutableTable table = globalState.getSchema().getRandomTable(t -> !t.isView());
        List<MutableColumn> columns = table.getColumns();
        sb.append("INSERT INTO ");
        sb.append(table.getName());
        sb.append(" VALUES ");
        insertColumns(columns);		// from AbstractInsertGenerator
        sb.append(";");
        
        // Add expected errors
        MutableErrors.addCommonErrors(errors);
        MutableErrors.addInsertErrors(errors);
        errors.add("Table " + table.getName() + " does not exist in database");
        
        if (globalState.getDbmsSpecificOptions().debug) {
        	System.out.println(sb.toString());
        }
        
        return new SQLQueryAdapter(sb.toString(), errors);
    }

    @Override
    protected void insertValue(MutableColumn mutableColumn) {
    	if (mutableColumn.isNullable() && Randomly.getBooleanWithRatherLowProbability()) {
    		sb.append("NULL"); return;
    	}

    	if (Randomly.getBooleanWithRatherLowProbability()) {
    		sb.append("DEFAULT"); return;
    	}

    	sb.append(MutableToStringVisitor.asString(new MutableTypedExpressionGenerator(globalState).generateConstant(mutableColumn.getType().getPrimitiveDataType())));

    }

}
