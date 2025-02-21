package sqlancer.mutable.oracle.norec;

import sqlancer.IgnoreMeException;
import sqlancer.Randomly;
import sqlancer.SQLConnection;
import sqlancer.common.oracle.NoRECBase;
import sqlancer.common.query.SQLQueryAdapter;
import sqlancer.common.query.SQLancerResultSet;
import sqlancer.mutable.MutableErrors;
import sqlancer.mutable.MutableProvider.MutableGlobalState;
import sqlancer.mutable.MutableSchema.MutableColumn;
import sqlancer.mutable.MutableSchema.MutableTables;
import sqlancer.mutable.MutableToStringVisitor;
import sqlancer.mutable.ast.MutableColumnReference;
import sqlancer.mutable.ast.MutableExpression;
import sqlancer.mutable.ast.MutablePostFixTextNode;
import sqlancer.mutable.ast.MutableSelect;
import sqlancer.mutable.gen.MutableTypedExpressionGenerator;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


public class MutableNoRECBase extends NoRECBase<MutableGlobalState> {
    protected final List<String> debugList;

    public MutableNoRECBase(MutableGlobalState globalState) {
        super(globalState);
        this.debugList = new ArrayList<String>();
        MutableErrors.addCommonErrors(errors);
        MutableErrors.addExpressionErrors(errors);
    }

    // Create a query that is likely to !NOT! be optimized. Then, execute the query and return the number of rows of the result set.
    protected int getUnoptimizedQueryCount(
            List<MutableExpression> tableList,
            List<MutableColumn> columns,
            MutableExpression randomWhereCondition,
            MutableTables tables,
            Boolean flagValue
    ) throws SQLException {
        // Create the query
        MutableSelect select = new MutableSelect();
        MutableExpression asText = new MutablePostFixTextNode(randomWhereCondition, "AS flag");
        select.setFetchColumns(List.of(asText));
        select.setFromList(tableList);
        if (Randomly.getBooleanWithSmallProbability()) {
            select.setOrderByExpressions(new MutableTypedExpressionGenerator(state).setColumns(columns).generateOrderBys());
        }
        String flagString;
        if (flagValue==null) {
            flagString = "ISNULL(flag)";
        } else if (flagValue) {
            flagString = "flag=TRUE";
        } else {
            flagString = "flag=FALSE";
        }
        unoptimizedQueryString = "SELECT COUNT(*) FROM (" + MutableToStringVisitor.asString(select) + ") AS res WHERE " + flagString + ";";
        debugList.add("unoptimizedQueryString: " + unoptimizedQueryString);

        // Execute query and get result
        SQLQueryAdapter q = new SQLQueryAdapter(unoptimizedQueryString, errors);
        SQLancerResultSet rs;
        try {
            rs = q.executeAndGetLogged(state);
        } catch (Exception e) {
            throw new AssertionError(unoptimizedQueryString, e);
        }
        if (rs == null) {
            return -1;
        }
        int count = rs.next() ? rs.getInt(1) : 0;
        rs.close();
        return count;
    }

    // TODO there might be a bug where `optimizedCount` is sometimes 1 too much
    // might be caused from this or from JDBC
    // Create a query that is likely to be optimized. Then, execute the query and return the number of rows of the result set.
    protected int getOptimizedQueryCount(
            SQLConnection con,
            List<MutableExpression> tableList,
            List<MutableColumn> columns,
            MutableExpression randomWhereCondition
    ) throws SQLException {
        // Create the query
        MutableSelect select = new MutableSelect();
        List<MutableExpression> allColumns = columns.stream().map(MutableColumnReference::new).collect(Collectors.toList());
        select.setFetchColumns(allColumns);
        select.setFromList(tableList);
        select.setWhereClause(randomWhereCondition);
        if (Randomly.getBooleanWithSmallProbability()) {
            select.setOrderByExpressions(new MutableTypedExpressionGenerator(state).setColumns(columns).generateOrderBys());
        }
        optimizedQueryString = MutableToStringVisitor.asString(select) + ";";
        debugList.add("optimizedQueryString: " + optimizedQueryString);

        // Execute query and get result
        int optimizedCount = 0;
        try (Statement stmt = con.createStatement()) {
            if (options.logEachSelect()) {
                logger.writeCurrent(optimizedQueryString);
            }
            try (ResultSet rs = stmt.executeQuery(optimizedQueryString)) {
                if (rs==null) { return optimizedCount; }
                while (rs.next()) {
                    optimizedCount++;
                }
            }
        } catch (SQLException e) {
            throw new IgnoreMeException();
        }
        return optimizedCount;
    }

    // Analyze results: oracle success if `optimizedCount` == `unOptimizedCount`
    protected void compareResults(int optimizedCount, int unOptimizedCount) {
        if (optimizedCount == -1 || unOptimizedCount == -1) {
            throw new IgnoreMeException();
        }
        if (optimizedCount != unOptimizedCount) {
            throw new AssertionError(
                    optimizedQueryString + "; -- " + optimizedCount + "\n" + unoptimizedQueryString + " -- " + unOptimizedCount);
        }
    }

    @Override
    public void check() throws Exception {

    }
}
