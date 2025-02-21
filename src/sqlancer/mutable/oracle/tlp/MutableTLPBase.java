package sqlancer.mutable.oracle.tlp;

import sqlancer.Randomly;
import sqlancer.common.gen.ExpressionGenerator;
import sqlancer.common.oracle.TernaryLogicPartitioningOracleBase;
import sqlancer.common.oracle.TestOracle;
import sqlancer.mutable.MutableErrors;
import sqlancer.mutable.MutableProvider.MutableGlobalState;
import sqlancer.mutable.MutableSchema;
import sqlancer.mutable.MutableSchema.MutableColumn;
import sqlancer.mutable.MutableSchema.MutableTable;
import sqlancer.mutable.MutableSchema.MutableTables;
import sqlancer.mutable.ast.MutableColumnReference;
import sqlancer.mutable.ast.MutableExpression;
import sqlancer.mutable.ast.MutableSelect;
import sqlancer.mutable.ast.MutableTableReference;
import sqlancer.mutable.gen.MutableTypedExpressionGenerator;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


public class MutableTLPBase extends TernaryLogicPartitioningOracleBase<MutableExpression, MutableGlobalState>
        implements TestOracle<MutableGlobalState> {

    MutableSchema s;
    MutableTables targetTables;
    MutableTypedExpressionGenerator gen;
    MutableSelect select;

    public MutableTLPBase(MutableGlobalState state) {
        super(state);
        MutableErrors.addExpressionErrors(errors);
        MutableErrors.addCommonErrors(errors);
    }

    // Basic setup for all TLP oracles. Specific changes to the query etc. are done in the subclasses.
    @Override
    public void check() throws SQLException {
        s = state.getSchema();
        targetTables = s.getRandomTableNonEmptyTables();
        gen = new MutableTypedExpressionGenerator(state).setColumns(targetTables.getColumns());
        initializeTernaryPredicateVariants();
        select = new MutableSelect();
        select.setFetchColumns(generateFetchColumns());
        List<MutableTable> tables = targetTables.getTables();
        List<MutableTableReference> tableList = tables.stream().map(t -> new MutableTableReference(t)).collect(Collectors.toList());
        select.setFromList(tableList.stream().collect(Collectors.toList()));
        select.setWhereClause(null);
    }

    // Either produces a `*` or a random nonempty subset of available columns as fetch columns for the SELECT clause
    List<MutableExpression> generateFetchColumns() {
    	List<MutableExpression> columns = new ArrayList<>();
        if (Randomly.getBoolean()) {
        	columns.add(new MutableColumnReference(MutableColumn.createDummy("*")));
        } else {
            columns = Randomly.nonEmptySubset(targetTables.getColumns()).stream()
                    .map(c -> new MutableColumnReference(c)).collect(Collectors.toList());
        }
        return columns;
    }

    @Override
    protected ExpressionGenerator<MutableExpression> getGen() {
        return gen;
    }

    protected void debugPrintQueries(String original, String first, String second, String third) {
    	if (state.getDbmsSpecificOptions().debug) {
        	System.out.println("\n\n");
            System.out.println("Original Query: " + original + ";");
            System.out.println("First Query: " + first + ";");
            System.out.println("Second Query: " + second + ";");
            System.out.println("Third Query: " + third + ";");
        }
    }

    protected void debugPrintResults(double original, double combined) {
    	if (state.getDbmsSpecificOptions().debug) {
        	System.out.println("original | combined : " + original + " | " + combined);
    	}
    }
}
