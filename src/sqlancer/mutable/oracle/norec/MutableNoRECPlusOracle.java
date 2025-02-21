package sqlancer.mutable.oracle.norec;

import sqlancer.common.oracle.TestOracle;
import sqlancer.mutable.MutableErrors;
import sqlancer.mutable.MutableProvider.MutableGlobalState;
import sqlancer.mutable.MutableSchema;
import sqlancer.mutable.MutableSchema.MutableColumn;
import sqlancer.mutable.MutableSchema.MutableDataType;
import sqlancer.mutable.MutableSchema.MutableTable;
import sqlancer.mutable.MutableSchema.MutableTables;
import sqlancer.mutable.MutableToStringVisitor;
import sqlancer.mutable.ast.MutableExpression;
import sqlancer.mutable.ast.MutableTableReference;
import sqlancer.mutable.gen.MutableTypedExpressionGenerator;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;


public class MutableNoRECPlusOracle extends MutableNoRECBase implements TestOracle<MutableGlobalState> {

    private final MutableSchema schema;

    public MutableNoRECPlusOracle(MutableGlobalState globalState) {
        super(globalState);
        this.schema = globalState.getSchema();
        MutableErrors.addExpressionErrors(errors);
    }

    @Override
    public void check() throws SQLException {
        // Get a random non-empty subset of all available tables + columns
        MutableTables randomTables = schema.getRandomTableNonEmptyTables();
        List<MutableColumn> columns = randomTables.getColumns();

        // Generate a random WHERE condition
        MutableTypedExpressionGenerator gen = new MutableTypedExpressionGenerator(state).setColumns(columns);
        MutableExpression randomWhereCondition = gen.generateExpression(MutableDataType.BOOL, 0);
        MutableExpression negatedCondition = gen.negatePredicate(randomWhereCondition);
        MutableExpression isNullCondition = gen.isNull(randomWhereCondition);
        debugList.add("Random WhereCondition: " + MutableToStringVisitor.asString(randomWhereCondition));
        debugList.add("Negated Condition: " + MutableToStringVisitor.asString(negatedCondition));
        debugList.add("isNull Condition: " + MutableToStringVisitor.asString(isNullCondition));

        // Create a TableReferenceNode for each table
        List<MutableTable> tables = randomTables.getTables();
        List<MutableTableReference> tableList = tables.stream().map(MutableTableReference::new).toList();

        // TRUE
        int unOptimizedCount = getUnoptimizedQueryCount(new ArrayList<>(tableList), columns, randomWhereCondition, randomTables, true);
        int optimizedCount = getOptimizedQueryCount(con, new ArrayList<>(tableList), columns, randomWhereCondition);
        debugList.add("Unoptimized count: " + unOptimizedCount + "      Optimized count: " + optimizedCount);
        if (state.getDbmsSpecificOptions().debug) {
            System.out.println(String.join("\n", debugList) + "\n");
        }
        debugList.clear();
        compareResults(optimizedCount, unOptimizedCount);

        // FALSE
        unOptimizedCount = getUnoptimizedQueryCount(new ArrayList<>(tableList), columns, randomWhereCondition, randomTables, false);
        optimizedCount = getOptimizedQueryCount(con, new ArrayList<>(tableList), columns, negatedCondition);
        debugList.add("Unoptimized count: " + unOptimizedCount + "      Optimized count: " + optimizedCount);
        if (state.getDbmsSpecificOptions().debug) {
            System.out.println(String.join("\n", debugList) + "\n");
        }
        debugList.clear();
        compareResults(optimizedCount, unOptimizedCount);

        // NULL
        unOptimizedCount = getUnoptimizedQueryCount(new ArrayList<>(tableList), columns, randomWhereCondition, randomTables, null);
        optimizedCount = getOptimizedQueryCount(con, new ArrayList<>(tableList), columns, isNullCondition);
        debugList.add("Unoptimized count: " + unOptimizedCount + "      Optimized count: " + optimizedCount);
        if (state.getDbmsSpecificOptions().debug) {
            System.out.println(String.join("\n", debugList) + "\n");
        }
        debugList.clear();
        compareResults(optimizedCount, unOptimizedCount);

    }

}
