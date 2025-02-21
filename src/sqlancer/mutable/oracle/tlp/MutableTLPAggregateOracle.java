package sqlancer.mutable.oracle.tlp;

import sqlancer.IgnoreMeException;
import sqlancer.Randomly;
import sqlancer.common.oracle.TestOracle;
import sqlancer.common.query.SQLQueryAdapter;
import sqlancer.common.query.SQLancerResultSet;
import sqlancer.mutable.MutableProvider.MutableGlobalState;
import sqlancer.mutable.MutableToStringVisitor;
import sqlancer.mutable.ast.MutableAggregate.MutableAggregateFunction;
import sqlancer.mutable.ast.MutableExpression;
import sqlancer.mutable.ast.MutablePostFixTextNode;
import sqlancer.mutable.utils.Tuple;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class MutableTLPAggregateOracle extends MutableTLPBase
        implements TestOracle<MutableGlobalState> {

    private Double originalResult;
    private Double firstResult;
    private Double secondResult;
    private Double thirdResult;

    public MutableTLPAggregateOracle(MutableGlobalState state) {
        super(state);
        // MutableErrors.addGroupByErrors(errors);
    }

    @Override
    public void check() throws SQLException {
        super.check();

        // Create fetch columns
        List<MutableExpression> fetchColumns = new ArrayList<>();

        // Create a random aggregate expression
        MutableAggregateFunction aggregateFunction = MutableAggregateFunction.getRandom();
        MutableExpression aggregate = gen.generateAggregateForFunction(aggregateFunction);

        if (aggregateFunction == MutableAggregateFunction.AVG) {
        	// Need to add a COUNT(*) in this case to compute the correct result
        	MutableExpression withCount = new MutablePostFixTextNode(aggregate, ", COUNT(*)");
        	fetchColumns.add(withCount);
        	select.setFetchColumns(fetchColumns);
        	checkAVG();
        	return;
        }

        fetchColumns.add(aggregate);
        select.setFetchColumns(fetchColumns);

        // Create the different query strings
        String originalQueryString = MutableToStringVisitor.asString(select);
        select.setWhereClause(predicate);
        String firstQueryString = MutableToStringVisitor.asString(select);
        select.setWhereClause(negatedPredicate);
        String secondQueryString = MutableToStringVisitor.asString(select);
        select.setWhereClause(isNullPredicate);
        String thirdQueryString = MutableToStringVisitor.asString(select);
        debugPrintQueries(originalQueryString, firstQueryString, secondQueryString, thirdQueryString);

        // Execute queries and get results
        originalResult = executeAndgetAggregateResult(originalQueryString, aggregateFunction).a;
        firstResult = executeAndgetAggregateResult(firstQueryString, aggregateFunction).a;
        secondResult = executeAndgetAggregateResult(secondQueryString, aggregateFunction).a;
        thirdResult = executeAndgetAggregateResult(thirdQueryString, aggregateFunction).a;

        if (state.getDbmsSpecificOptions().debug) {
        	System.out.println(String.format("Results: %f | %f %f %f", originalResult, firstResult, secondResult, thirdResult));
        }

        List<String> combinedQuery = new ArrayList<String>();
        combinedQuery.add(firstQueryString);
        combinedQuery.add(secondQueryString);
        combinedQuery.add(thirdQueryString);
        checkResults(originalQueryString, combinedQuery, originalResult, firstResult, secondResult, thirdResult, aggregateFunction);
    }


    // Execute the given `queryString` on the current database connection.
    // Return a tuple where the first element is the aggregate result
    // and the second element is the value of `COUNT(*)` (special case for AVG).
    private Tuple<Double, Double> executeAndgetAggregateResult(String queryString, MutableAggregateFunction func) throws SQLException {
    	Double result;
    	Double count = 0.0;
        SQLQueryAdapter q = new SQLQueryAdapter(queryString, errors);
        try (SQLancerResultSet resultSet = q.executeAndGetLogged(state)) {
            if (resultSet == null) {
                throw new IgnoreMeException();
            }
            if (!resultSet.next()) {
            	result = null;
            } else {
            	String s = resultSet.getString(1);
            	if (s == null) { result = null; }
            	else { result = Double.parseDouble(s); }

                if (func == MutableAggregateFunction.AVG) {
                	count = Double.parseDouble(resultSet.getString(2));
                }
            }
            return new Tuple<Double, Double>(result, count);
        } catch (SQLException e) {
            if (!e.getMessage().contains("Not implemented type")) {
                throw new AssertionError(queryString, e);
            } else {
                throw new IgnoreMeException();
            }
        } catch (Exception e) {
            throw new AssertionError(queryString, e);
        }
    }
    
    private String convertNumberToString(Double d) {
    	return d == null ? "NULL" : d.toString();
    }

    // Checks whether the original result and the combined result are NULL.
    // If the original result is NULL, all three other results have to be NULL as well and vice versa.
    private boolean checkNULL(String originalQuery, List<String> combinedQuery, Double originalResult, Double firstResult, Double secondResult, Double thirdResult) {
    	if (	(originalResult == null && !(firstResult == null && secondResult == null && thirdResult == null))
    		 || (!(originalResult == null) && (firstResult == null && secondResult == null && thirdResult == null))) {
            String combinedQueryString = String.join(";", combinedQuery);
            String combinedResultString = convertNumberToString(firstResult) + " | " + convertNumberToString(secondResult) + " | " + convertNumberToString(thirdResult);
            String assertionMessage = String.format(
            		"NULL mismatch bewteen original result and combined result" + System.lineSeparator()
                            + "Original query: \"%s;\" -- %s" + System.lineSeparator()
                            + "Combined query: \"%s;\" -- %s",
                    originalQuery, convertNumberToString(originalResult),
                    combinedQueryString, combinedResultString);
            throw new AssertionError(assertionMessage);
    	}
    	
    	if (originalResult == null && firstResult == null && secondResult == null && thirdResult == null) {
    		return true;
    	}
    	return false;
    }


    // Combines the three results to one and compares it to the original result.
    // If they differ, the oracle fails, which indicates a possible bug in the DBMS.
    // The results are combined depending on the aggregate function:
    // - SUM and COUNT -> just sum up the three results
    // - MIN           -> take the minimum of the three results
    // - MAX		   -> take the maximum of the three results
    // - AVG   		   -> handled individually down below
    private void checkResults(
    	String originalQuery,
    	List<String> combinedQuery,
		Double originalResult,
		Double firstResult,
		Double secondResult,
		Double thirdResult,
		MutableAggregateFunction aggregateFunction
    ) {
    	// Check for NULL values
    	if (checkNULL(originalQuery, combinedQuery, originalResult, firstResult, secondResult, thirdResult)) {
    		return;
    	}

    	Double combinedResult;
    	switch (aggregateFunction) {
			case SUM:
			case COUNT:
				combinedResult = 0.0;
				if (firstResult  != null) { combinedResult += firstResult;  }
				if (secondResult != null) { combinedResult += secondResult; }
				if (thirdResult  != null) { combinedResult += thirdResult;  }
				break;
			case MIN:
				combinedResult = Double.POSITIVE_INFINITY;
				if (firstResult  != null)  { combinedResult = Math.min(combinedResult, firstResult);  }
				if (secondResult  != null) { combinedResult = Math.min(combinedResult, secondResult); }
				if (thirdResult  != null)  { combinedResult = Math.min(combinedResult, thirdResult);  }
				break;
			case MAX:
				combinedResult = Double.NEGATIVE_INFINITY;
				if (firstResult  != null)  { combinedResult = Math.max(combinedResult, firstResult);  }
				if (secondResult  != null) { combinedResult = Math.max(combinedResult, secondResult); }
				if (thirdResult  != null)  { combinedResult = Math.max(combinedResult, thirdResult);  }
				break;
			default:
				combinedResult = null;
    	}

    	if (combinedResult == null) {
    		throw new AssertionError("Unreachable");
    	}

    	// Debug
    	debugPrintResults(originalResult, combinedResult);

    	// Check for result equality (oracle success condition)
    	if (!originalResult.equals(combinedResult)) {
    		String combinedQueryString = String.join(";", combinedQuery);
            String combinedResultString = convertNumberToString(firstResult) + " | " + convertNumberToString(secondResult) + " | " + convertNumberToString(thirdResult);
            String assertionMessage = String.format(
            		"Result mismatch bewteen original result and combined result" + System.lineSeparator()
                            + "Original query: \"%s;\" -- %s" + System.lineSeparator()
                            + "Combined query: \"%s;\" -- %s",
                    originalQuery, convertNumberToString(originalResult),
                    combinedQueryString, combinedResultString);
            throw new AssertionError(assertionMessage);
    	}
    }


    // AVG is a special case because it needs to query for COUNT as well
    // (to combine the three separate results into one)
    public void checkAVG() throws SQLException {
        // Create the different query strings
        String originalQueryString = MutableToStringVisitor.asString(select);
        select.setWhereClause(predicate);
        String firstQueryString = MutableToStringVisitor.asString(select);
        select.setWhereClause(negatedPredicate);
        String secondQueryString = MutableToStringVisitor.asString(select);
        select.setWhereClause(isNullPredicate);
        String thirdQueryString = MutableToStringVisitor.asString(select);
        debugPrintQueries(originalQueryString, firstQueryString, secondQueryString, thirdQueryString);

        // Execute queries and get results
        Tuple<Double, Double> originalRes = executeAndgetAggregateResult(originalQueryString, MutableAggregateFunction.AVG);
        Tuple<Double, Double> res1 = executeAndgetAggregateResult(firstQueryString, MutableAggregateFunction.AVG);
        Tuple<Double, Double> res2 = executeAndgetAggregateResult(secondQueryString, MutableAggregateFunction.AVG);
        Tuple<Double, Double> res3 = executeAndgetAggregateResult(thirdQueryString, MutableAggregateFunction.AVG);

        // Check for NULL values
        List<String> combinedQuery = new ArrayList<String>();
        combinedQuery.add(firstQueryString);
        combinedQuery.add(secondQueryString);
        combinedQuery.add(thirdQueryString);
        if (checkNULL(originalQueryString, combinedQuery, originalRes.a, res1.a, res2.a, res3.a)) {
    		return;
    	}

        // Check results by weighting the AVG values with their corresponding COUNT values
        Double numerator = 0.0;
		Double denominator = 0.0;
		if (res1.a != null) { numerator += res1.a * res1.b; }
		if (res2.a != null) { numerator += res2.a * res2.b; }
		if (res3.a != null) { numerator += res3.a * res3.b; }
		if (res1.b != null) { denominator += res1.b; }
		if (res2.b != null) { denominator += res2.b; }
		if (res3.b != null) { denominator += res3.b; }

		// Round to 4 decimal paces to account for float errors
		Double combinedResult = round(numerator / denominator, 4);
    	Double originalResult = round(originalRes.a, 4);
		debugPrintResults(originalResult, combinedResult);

    	// Check for result equality (oracle success condition)
    	if (!originalResult.equals(combinedResult)) {
    		String combinedQueryString = String.join(";", combinedQuery);
            String combinedResultString = convertNumberToString(res1.a) + "," + convertNumberToString(res1.b) + 
                                    " | " + convertNumberToString(res2.a) + "," + convertNumberToString(res2.b) + 
                                    " | " + convertNumberToString(res3.a) + "," + convertNumberToString(res3.b);
            String assertionMessage = String.format(
            		"NULL mismatch bewteen original result and combined result" + System.lineSeparator()
                            + "Original query: \"%s;\" -- %s" + System.lineSeparator()
                            + "Combined query: \"%s;\" -- %s",
                            originalQueryString, convertNumberToString(originalResult),
                    combinedQueryString, combinedResultString);
            throw new AssertionError(assertionMessage);
    	}
    }

    // Round the given `value` to `places` decimal-places.
    private static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        BigDecimal bd = BigDecimal.valueOf(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

}
