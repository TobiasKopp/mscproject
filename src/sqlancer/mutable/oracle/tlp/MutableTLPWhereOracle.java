package sqlancer.mutable.oracle.tlp;

import sqlancer.ComparatorHelper;
import sqlancer.Randomly;
import sqlancer.mutable.MutableProvider.MutableGlobalState;
import sqlancer.mutable.MutableToStringVisitor;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class MutableTLPWhereOracle extends MutableTLPBase {

    private String generatedQueryString;

    public MutableTLPWhereOracle(MutableGlobalState state) {
        super(state);
    }

    @Override
    public void check() throws SQLException {
        super.check();

        // Possibly add an ORDER BY expression
        if (Randomly.getBooleanWithRatherLowProbability()) {
            select.setOrderByExpressions(gen.generateOrderBys());
        }

        // Create different queries
        select.setWhereClause(null);
        String originalQueryString = MutableToStringVisitor.asString(select);
        generatedQueryString = originalQueryString;
        select.setWhereClause(predicate);
        String firstQueryString = MutableToStringVisitor.asString(select);
        select.setWhereClause(negatedPredicate);
        String secondQueryString = MutableToStringVisitor.asString(select);
        select.setWhereClause(isNullPredicate);
        String thirdQueryString = MutableToStringVisitor.asString(select);
        debugPrintQueries(originalQueryString, firstQueryString, secondQueryString, thirdQueryString);

        // Execute queries and get their result sets
        List<String> originalResultSet = ComparatorHelper.getResultSetFirstColumnAsString(originalQueryString, errors, state);
        List<String> combinedQueryString = new ArrayList<>();
        List<String> combinedResultSet = ComparatorHelper.getCombinedResultSet(firstQueryString, secondQueryString,
                thirdQueryString, combinedQueryString, false, state, errors);
        debugPrintResults(originalResultSet.size(), combinedResultSet.size());

        // Check whether original result set and combined result set are equal (oracle success condition)
        ComparatorHelper.assumeResultSetsAreEqual(originalResultSet, combinedResultSet, originalQueryString, combinedQueryString,
                state);
    }

    @Override
    public String getLastQueryString() {
        return generatedQueryString;
    }

}
