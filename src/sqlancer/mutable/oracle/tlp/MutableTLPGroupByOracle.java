package sqlancer.mutable.oracle.tlp;

import sqlancer.ComparatorHelper;
import sqlancer.IgnoreMeException;
import sqlancer.Randomly;
import sqlancer.SQLGlobalState;
import sqlancer.common.query.ExpectedErrors;
import sqlancer.common.query.SQLQueryAdapter;
import sqlancer.common.query.SQLancerResultSet;
import sqlancer.mutable.MutableProvider.MutableGlobalState;
import sqlancer.mutable.MutableSchema.MutableColumn;
import sqlancer.mutable.MutableToStringVisitor;
import sqlancer.mutable.ast.MutableColumnReference;
import sqlancer.mutable.ast.MutableExpression;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MutableTLPGroupByOracle extends MutableTLPBase {

    public MutableTLPGroupByOracle(MutableGlobalState state) {
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
        select.setWhereClause(predicate);
        String firstQueryString = MutableToStringVisitor.asString(select);
        select.setWhereClause(negatedPredicate);
        String secondQueryString = MutableToStringVisitor.asString(select);
        select.setWhereClause(isNullPredicate);
        String thirdQueryString = MutableToStringVisitor.asString(select);
        debugPrintQueries(originalQueryString, firstQueryString, secondQueryString, thirdQueryString);


        // Execute queries and get their result sets
        List<String> originalResultSet = getResultSet(originalQueryString, select.getFetchColumns().size(), errors, state);
        List<String> firstResultSet = getResultSet(firstQueryString, select.getFetchColumns().size(), errors, state);
        List<String> secondResultSet = getResultSet(secondQueryString, select.getFetchColumns().size(), errors, state);
        List<String> thirdResultSet = getResultSet(thirdQueryString, select.getFetchColumns().size(), errors, state);
        
        if (state.getDbmsSpecificOptions().debug) {
            System.out.println("Original Result: " + originalResultSet.toString());
            System.out.println("First Result: " + firstResultSet.toString());
            System.out.println("Second Result: " + secondResultSet.toString());
            System.out.println("Third Result: " + thirdResultSet.toString());
        }
        
        List<String> combinedResultSet = new ArrayList<String>();
        combinedResultSet.addAll(firstResultSet);
        
        // Add distinct rows to first result
        for (String row : secondResultSet) {
            if (!combinedResultSet.contains(row)) combinedResultSet.add(row);   // Exclude duplicates
            // -> implementation of:
            // SELECT DISTINCT * FROM (firstQueryString UNION ALL secondQueryString UNION ALL thirdQueryString);
        }
        for (String row : thirdResultSet) {
            if (!combinedResultSet.contains(row)) combinedResultSet.add(row);   // Exclude duplicates
        }
        
        if (state.getDbmsSpecificOptions().debug) {
            System.out.println("Combined Result: " + combinedResultSet.toString());
        }
        
        // Second implementation:
//        List<String> secondResultSet = new ArrayList<>();
//        List<String> tmpResult = ComparatorHelper.getCombinedResultSet(firstQueryString, secondQueryString, thirdQueryString, combinedString, false, state, errors);
//        for (String row : tmpResult) {
//            if (!secondResultSet.contains(row)) secondResultSet.add(row);
//        }

        List<String> combinedString = new ArrayList<>(); 
        combinedString.add(firstQueryString);
        combinedString.add(secondQueryString);
        combinedString.add(thirdQueryString);
        
        // Check whether original result set and combined result set are equal (oracle success condition)
        ComparatorHelper.assumeResultSetsAreEqual(originalResultSet, combinedResultSet, originalQueryString, combinedString,
                state, ComparatorHelper::canonicalizeResultValue);
    }

    @Override
    List<MutableExpression> generateFetchColumns() {
        return Randomly.nonEmptySubset(targetTables.getColumns()).stream()
                .map(c -> new MutableColumnReference(c)).collect(Collectors.toList());
    }
    
    public static List<String> getResultSet(String queryString, int nrColumns, ExpectedErrors errors,
            SQLGlobalState<?, ?> state) throws SQLException {
        if (state.getOptions().logEachSelect()) {
            state.getLogger().writeCurrent(queryString);
        }
        boolean canonicalizeString = state.getOptions().canonicalizeSqlString();
        SQLQueryAdapter q = new SQLQueryAdapter(queryString, errors, true, canonicalizeString);
        List<String> resultSet = new ArrayList<>();
        SQLancerResultSet result = null;
        try {
            result = q.executeAndGet(state);
            if (result == null) {
                throw new IgnoreMeException();
            }
            while (result.next()) {
                String resultTemp = "";
                for (int i=1; i<nrColumns+1; i++) {
                    if (i>1) resultTemp += ",";
                    String s = result.getString(i);
                    resultTemp += s==null ? "NULL" : s;
                }
                if (resultTemp != null) {
                    resultTemp = resultTemp.replaceAll("[\\.]0+$", ""); // Remove the trailing zeros as many DBMS treat
                    // it as non-bugs
                }
                resultSet.add(resultTemp);
            }
        } catch (Exception e) {
            if (e instanceof IgnoreMeException) {
                throw e;
            }

            if (e.getMessage() == null) {
                throw new AssertionError(queryString, e);
            }
            if (errors.errorIsExpected(e.getMessage())) {
                throw new IgnoreMeException();
            }
            throw new AssertionError(queryString, e);
        } finally {
            if (result != null && !result.isClosed()) {
                result.close();
            }
        }
        return resultSet;
    }

}
