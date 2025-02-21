package sqlancer.mutable.oracle.tlp;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import sqlancer.ComparatorHelper;
import sqlancer.Randomly;
import sqlancer.mutable.MutableToStringVisitor;
import sqlancer.mutable.MutableProvider.MutableGlobalState;
import sqlancer.mutable.MutableSchema.MutableColumn;
import sqlancer.mutable.ast.MutableColumnReference;
import sqlancer.mutable.ast.MutableExpression;

public class MutableTLPHavingOracle extends MutableTLPBase {

    public MutableTLPHavingOracle(MutableGlobalState state) {
        super(state);
    }

    @Override
    public void check() throws SQLException {
        super.check();

        // Create different queries
        select.setGroupByExpressions(select.getFetchColumns());
        List<MutableExpression> fetch = new ArrayList<>();
        fetch.add(new MutableColumnReference(MutableColumn.createDummy("*")));
        select.setFetchColumns(fetch);
        select.setWhereClause(null);
        String originalQueryString = MutableToStringVisitor.asString(select);
        select.setHavingClause(predicate);
        String firstQueryString = MutableToStringVisitor.asString(select);
        select.setHavingClause(negatedPredicate);
        String secondQueryString = MutableToStringVisitor.asString(select);
        select.setHavingClause(isNullPredicate);
        String thirdQueryString = MutableToStringVisitor.asString(select);
        debugPrintQueries(originalQueryString, firstQueryString, secondQueryString, thirdQueryString);

        // Execute queries and get their result sets
        List<String> originalResultSet = ComparatorHelper.getResultSetFirstColumnAsString(originalQueryString, errors, state);
        List<String> combinedString = new ArrayList<>();

        List<String> secondResultSet = ComparatorHelper.getCombinedResultSet(firstQueryString, secondQueryString,
                thirdQueryString, combinedString, false, state, errors);

        // Check whether original result set and combined result set are equal (oracle success condition)
        ComparatorHelper.assumeResultSetsAreEqual(originalResultSet, secondResultSet, originalQueryString, combinedString,
                state, ComparatorHelper::canonicalizeResultValue);
    }

    @Override
    List<MutableExpression> generateFetchColumns() {
        return Randomly.nonEmptySubset(targetTables.getColumns()).stream()
                .map(c -> new MutableColumnReference(c)).collect(Collectors.toList());
    }

}