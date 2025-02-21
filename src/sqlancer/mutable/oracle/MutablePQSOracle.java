package sqlancer.mutable.oracle;

import sqlancer.Randomly;
import sqlancer.SQLConnection;
import sqlancer.common.oracle.PivotedQuerySynthesisBase;
import sqlancer.common.query.Query;
import sqlancer.common.query.SQLQueryAdapter;
import sqlancer.mutable.MutableErrors;
import sqlancer.mutable.MutableProvider.MutableGlobalState;
import sqlancer.mutable.MutableSchema.MutableColumn;
import sqlancer.mutable.MutableSchema.MutableDataType;
import sqlancer.mutable.MutableSchema.MutableRowValue;
import sqlancer.mutable.MutableSchema.MutableTables;
import sqlancer.mutable.MutableToStringVisitor;
import sqlancer.mutable.ast.*;
import sqlancer.mutable.gen.MutableTypedExpressionGenerator;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class MutablePQSOracle
        extends PivotedQuerySynthesisBase<MutableGlobalState, MutableRowValue, MutableExpression, SQLConnection> {

    private List<MutableColumn> fetchColumns;

    public MutablePQSOracle(MutableGlobalState globalState) throws SQLException {
        super(globalState);
        MutableErrors.addCommonErrors(errors);
        MutableErrors.addExpressionErrors(errors);
    }

    /**
     * Obtains a rectified query (i.e., a query that is guaranteed to fetch the pivot row. This corresponds to steps 2-5
     * of the PQS paper.
     *
     * @return the rectified query
     *
     * @throws Exception
     *             if an unexpected error occurs
     */
    @Override
    public SQLQueryAdapter getRectifiedQuery() throws SQLException {
    	// Get random tables to include in the query
    	try {
			globalState.updateSchema();
		} catch (Exception e) {
			throw new AssertionError(e.getMessage());
		}
        MutableTables randomFromTables = globalState.getSchema().getRandomTableNonEmptyTables();
        List<MutableColumn> columns = randomFromTables.getColumns();
        fetchColumns = columns;

        // Get the random pivot row
        pivotRow = randomFromTables.getRandomRowValue(globalState);
//        if (globalState.getDbmsSpecificOptions().debug) System.out.println("Pivot Row: " + pivotRow.toString());

        // Generate a query that must fetch the pivot row
        MutableSelect selectStatement = new MutableSelect();
        selectStatement.setFromList(randomFromTables.getTables().stream().map(
        		t -> new MutableTableReference(t)).collect(Collectors.toList()));
        selectStatement.setFetchColumns(fetchColumns.stream()
                .map(c -> new MutableColumnValue(getFetchValueAliasedColumn(c), pivotRow.getValues().get(c)))
                .collect(Collectors.toList()));
        MutableExpression whereClause = generateRectifiedExpression(columns, pivotRow);
        selectStatement.setWhereClause(whereClause);
//        List<MutableExpression> groupByClause = generateGroupByClause(columns, pivotRow);
//        selectStatement.setGroupByExpressions(groupByClause);
        List<MutableExpression> orderBy = new MutableTypedExpressionGenerator(globalState).setColumns(columns)
                .generateOrderBys();
        selectStatement.setOrderByExpressions(orderBy);
        
        if (globalState.getDbmsSpecificOptions().debug) System.out.println("rectifiedQuery: " + MutableToStringVisitor.asString(selectStatement));
        return new SQLQueryAdapter(MutableToStringVisitor.asString(selectStatement));
    }

    /*
     * Prevent name collisions by aliasing the column.
     */
    private MutableColumn getFetchValueAliasedColumn(MutableColumn c) {
        MutableColumn aliasedColumn = new MutableColumn(
        		c.getName() + " AS " + c.getTable().getName() + c.getName(),
                c.getType(),
                c.isPrimaryKey(),
                c.isNullable(),
                c.isUnique());
        aliasedColumn.setTable(c.getTable());
        return aliasedColumn;
    }

    
    private List<MutableExpression> generateGroupByClause(List<MutableColumn> columns, MutableRowValue rw) {
        if (Randomly.getBoolean()) {
            return columns.stream().map(c -> MutableColumnValue.create(c, rw.getValues().get(c)))
                    .collect(Collectors.toList());
        } else {
            return Collections.emptyList();
        }
    }

    /**
     * Generates a predicate that is guaranteed to evaluate to <code>true</code> for the given pivot row. PQS uses this
     * method to generate predicates used in WHERE and JOIN clauses. See step 4 of the PQS paper.
     *
     * @param columns
     * @param pivotRow
     *
     * @return an expression that evaluates to <code>true</code>.
     */
    private MutableExpression generateRectifiedExpression(List<MutableColumn> columns, MutableRowValue pivotRow) {
        MutableTypedExpressionGenerator gen = new MutableTypedExpressionGenerator(globalState).setColumns(columns);
        gen = gen.setRowValue(pivotRow);
        MutableExpression expr = gen.generateExpressionWithExpectedResult(MutableDataType.BOOL);
        MutableExpression rectifiedPredicate;
        MutableConstant expectedValue = expr.getExpectedValue();
        if (expectedValue.isNullConstant()) {
        	rectifiedPredicate = gen.isNull(expr);
        } else {
        	if (expectedValue.asBoolean()) {
        		rectifiedPredicate = expr;
        	} else {
        		rectifiedPredicate = gen.negatePredicate(expr);
        	}
        }
        if (globalState.getDbmsSpecificOptions().debug) System.out.println("rectified expression: " + MutableToStringVisitor.asString(rectifiedPredicate));
        return rectifiedPredicate;
    }

    /**
     * Gets a query that checks whether the pivot row is contained in the result. If the pivot row is contained, the
     * query will fetch at least one row. If the pivot row is not contained, no rows will be fetched. This corresponds
     * to step 7 described in the PQS paper.
     *
     * @param pivotRowQuery
     *            the query that is guaranteed to fetch the pivot row, potentially among other rows
     *
     * @return a query that checks whether the pivot row is contained in pivotRowQuery
     *
     * @throws Exception
     *             if an unexpected error occurs
     */
    @Override
    protected Query<SQLConnection> getContainmentCheckQuery(Query<?> query) throws SQLException {
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT * FROM ("); // ANOTHER SELECT TO USE ORDER BY without restrictions
        sb.append(query.getUnterminatedQueryString());
        sb.append(") AS result WHERE ");
        int i = 0;
        for (MutableColumn c : fetchColumns) {
            if (i++ != 0) {
                sb.append(" AND ");
            }
            if (pivotRow.getValues().get(c).isNullConstant()) {
            	sb.append("ISNULL(");
            	sb.append("result.");
                sb.append(c.getTable().getName());
                sb.append(c.getName());
                sb.append(")");
            } else {
            	sb.append("result.");
                sb.append(c.getTable().getName());
                sb.append(c.getName());
                sb.append(" = ");
                String val = pivotRow.getValues().get(c).getTextRepresentation();
                if (val.charAt(0)=='"' && val.charAt(val.length()-1)=='"') {
                	val = val.substring(1, val.length()-1);
                }
                sb.append(val);
            }
        }
        String resultingQueryString = sb.toString();
        return new SQLQueryAdapter(resultingQueryString, errors);
    }

    /**
     * Prints the value to which the expression is expected to evaluate, and then recursively prints the subexpressions'
     * expected values.
     *
     * @param expr
     *            the expression whose expected value should be printed
     *
     * @return a string representing the expected value of the expression and its subexpressions
     */
    // TODO
    @Override
    protected String getExpectedValues(MutableExpression expr) {
        return expr.getExpectedValue().toString();
    	//return MutableVisitor.asExpectedValues(expr);
    }

}
