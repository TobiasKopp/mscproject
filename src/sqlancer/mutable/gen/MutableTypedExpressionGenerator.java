package sqlancer.mutable.gen;

import sqlancer.IgnoreMeException;
import sqlancer.Randomly;
import sqlancer.common.gen.TypedExpressionGenerator;
import sqlancer.mutable.MutableProvider;
import sqlancer.mutable.MutableSchema.MutableColumn;
import sqlancer.mutable.MutableSchema.MutableDataType;
import sqlancer.mutable.MutableSchema.MutableRowValue;
import sqlancer.mutable.MutableToStringVisitor;
import sqlancer.mutable.ast.*;
import sqlancer.mutable.ast.MutableAggregate.MutableAggregateFunction;
import sqlancer.mutable.ast.MutableBinaryArithmeticExpression.MutableBinaryArithmeticOperator;
import sqlancer.mutable.ast.MutableFunctionExpression.MutableFunction;
import sqlancer.mutable.ast.MutablePrefixExpression.PrefixOperator;
import sqlancer.mutable.utils.Tuple;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class MutableTypedExpressionGenerator
	extends TypedExpressionGenerator<MutableExpression, MutableColumn, MutableDataType> {

	private final MutableProvider.MutableGlobalState globalState;

	// To evaluate an AST for PQS
	private MutableRowValue rowValue;
	private boolean expectedResult;

	public MutableTypedExpressionGenerator(MutableProvider.MutableGlobalState globalState) {
        this.globalState = globalState;
    }

	public MutableTypedExpressionGenerator setRowValue(MutableRowValue rw) {
        this.rowValue = rw;
        return this;
    }

    // 33% chance to be true
    private boolean percent33Chance() {
        return Randomly.fromOptions(1,2,3) == 1;
    }

	// Generate a random boolean expression
	@Override
	public MutableExpression generatePredicate() {
		return generateExpression(MutableDataType.BOOL, 0);
	}

	@Override
	public MutableExpression negatePredicate(MutableExpression predicate) {
		return new MutablePrefixExpression(predicate, PrefixOperator.NOT);
	}

	@Override
	public MutableExpression isNull(MutableExpression expr) {
		return new MutableFunctionExpression(MutableFunction.ISNULL, MutableDataType.BOOL, expr);
	}

	@Override
	public MutableExpression generateConstant(MutableDataType type) {
		switch (type) {
	        case BOOL:
	            if (!globalState.getDbmsSpecificOptions().testBooleanConstants) {
	                throw new IgnoreMeException();
	            }
	            return MutableConstant.createBooleanConstant(Randomly.getBoolean());
	        case CHAR:
	        case VARCHAR:
	            if (!globalState.getDbmsSpecificOptions().testStringConstants) {
	                throw new IgnoreMeException();
	            }
	            String str = "";
	            // Allow empty string with 10% probability
//	            while (str.isEmpty() && !Randomly.getBooleanWithSmallProbability()) {
//	            	str = globalState.getRandomly().getString();
//	            }
//	            // Remove some unwanted characters
//	            // TODO more efficient way?
//	            str = str.replace("\\", "");
//	            str = str.replace("^", "");
//	            str = str.replace("'", "");
//	            str = str.replace("\n", "");
//	            str = str.replace(",", "");
	            
	            String characters = "abcdefgijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ§%&()=?.-_#+*~/{}[]^";
            	int size = Randomly.fromList(IntStream.range(0, 16).boxed().collect(Collectors.toList()));
            	for (int i=0; i<size; i++) {
            		str += characters.charAt(Randomly.fromList(IntStream.range(0, characters.length()).boxed().collect(Collectors.toList())));
            	} 
	            return MutableConstant.createStringConstant(str);
	        case DATE:
	            if (!globalState.getDbmsSpecificOptions().testDateConstants) {
	                throw new IgnoreMeException();
	            }
	            return MutableConstant.createDateConstant(globalState.getRandomly().getInteger());
	        case DATETIME:
	            if (!globalState.getDbmsSpecificOptions().testDatetimeConstants) {
	                throw new IgnoreMeException();
	            }
	            return MutableConstant.createDatetimeConstant(globalState.getRandomly().getInteger());
	        case INT:
	            if (!globalState.getDbmsSpecificOptions().testIntegerConstants) {
	                throw new IgnoreMeException();
	            }
	            return MutableConstant.createIntegerConstant(globalState.getRandomly().getInteger());
	        case FLOAT:
	            if (!globalState.getDbmsSpecificOptions().testFloatConstants) {
	                throw new IgnoreMeException();
	            }
	            return MutableConstant.createFloatingPointConstant(globalState.getRandomly().getDouble());
	        case DOUBLE:
	            if (!globalState.getDbmsSpecificOptions().testDoubleConstants) {
	                throw new IgnoreMeException();
	            }
	            return MutableConstant.createFloatingPointConstant(globalState.getRandomly().getDouble());
	        case DECIMAL:
	            if (!globalState.getDbmsSpecificOptions().testDecimalConstants) {
	                throw new IgnoreMeException();
	            }
	            return MutableConstant.createFloatingPointConstant(globalState.getRandomly().getDouble());
	        default:
	        	throw new AssertionError();
	    }
	}

	public MutableExpression generateConstantofRandomType() {
        MutableDataType type = MutableDataType.getRandomWithoutNull();
        return generateConstant(type);
    }

	public MutableExpression generateLeafNode(MutableDataType type) {
        if (Randomly.getBoolean() && !columns.isEmpty()) {
            return generateColumn(type);
        } else {
            return generateConstant(type);
        }
    }

	@Override
	public MutableExpression generateExpression(MutableDataType type, int depth) {
		switch(type) {
    	case BOOL: return generateExpressionEvaluatingToBoolean(depth);
    	case INT: return generateExpressionEvaluatingToINT(depth);
    	case FLOAT:
    	case DOUBLE:
    	case DECIMAL:
    		return generateExpressionEvaluatingToFloatDoubleDecimal(depth);
    	// TODO The following types can just be constants or columns?
    	case CHAR:
    	case VARCHAR:
    	case DATE:
    	case DATETIME:

		default:
    		return generateColumn(type);
    	}
	}

	@Override
	protected MutableExpression generateColumn(MutableDataType type) {
    	List<MutableColumn> cols = new ArrayList<MutableColumn>();
		for (MutableColumn c : columns) {
			if (c.getType().getPrimitiveDataType()==type) { cols.add(c); }
		}
		if (!cols.isEmpty() && Randomly.getBoolean()) {	// 50% chance to pick a column if one exists, other wise generate a constant
			MutableColumn column = Randomly.fromList(cols);
			MutableConstant value = rowValue == null ? null : rowValue.getValues().get(column);
			return new MutableColumnValue(column, value);
		} else {
			return generateConstant(type);
		}
	}

	@Override
	protected MutableDataType getRandomType() {
		return MutableDataType.getRandomWithoutNull();
	}

	@Override
	protected boolean canGenerateColumnOfType(MutableDataType type) {
		// TODO Auto-generated method stub
		return false;
	}

	// Generate a random aggregate expression
    public MutableExpression generateAggregate(MutableDataType type) {
    	// Get a random aggregate function that supports return type `type`
    	MutableAggregateFunction aggregateFunction = MutableAggregateFunction.getRandom();
    	while (!aggregateFunction.supportsReturnType(type)) {
    		aggregateFunction = MutableAggregateFunction.getRandom();
    	}
    	List<MutableExpression> expr = new ArrayList<MutableExpression>();
    	if (aggregateFunction == MutableAggregateFunction.COUNT) {
    		// COUNT can be done for any type of column
    		expr.add(generateLeafNode(MutableDataType.getRandomWithoutNull()));
    	} else {
    		// AVG, SUM, MIN, MAX
    		expr.add(generateLeafNode(type));
    	}
    	return new MutableAggregate(aggregateFunction, expr);
    }

    // Generate a random aggregate expression using the given function
    public MutableExpression generateAggregateForFunction(MutableAggregateFunction aggregateFunction) {
    	List<MutableColumn> cols = new ArrayList<MutableColumn>();
    	for (MutableColumn c : this.columns) {
    		if (c.getType().getPrimitiveDataType() == MutableDataType.INT
    				|| c.getType().getPrimitiveDataType() == MutableDataType.FLOAT
    				|| c.getType().getPrimitiveDataType() == MutableDataType.DOUBLE
    				|| c.getType().getPrimitiveDataType() == MutableDataType.DECIMAL) {
    			cols.add(c);
    		}
    	}
    	if (cols.isEmpty()) {
    		throw new IgnoreMeException();
    	}
    	MutableExpression col_ref = new MutableColumnReference(Randomly.fromList(cols));
    	return new MutableAggregate(aggregateFunction, Arrays.asList(col_ref));
    }

    // Generate random order by for random type
    public MutableExpression generateOrderBy() {
    	return new MutableColumnReference(Randomly.fromList(columns));
    }

    // Generate random order by for given type
    public MutableExpression generateOrderBy(MutableDataType type) {
    	return generateColumn(type);
    }

    @Override
    public List<MutableExpression> generateOrderBys() {
        List<MutableExpression> expressions = new ArrayList<MutableExpression>();
        List<String> used = new ArrayList<String>();
    	for (int i=0; i<Randomly.smallNumber(); i++) {
    	    MutableColumnReference e = (MutableColumnReference) generateOrderBy();;
    	    if (!used.contains(e.getColumn().getFullQualifiedName())) {
                expressions.add(generateOrderBy());
                used.add(e.getColumn().getFullQualifiedName());   // avoid order by same column
    	    }
    	}
        return expressions;
    }

    // BOOLEAN
    private MutableExpression generateExpressionEvaluatingToBoolean(int depth) {
    	if (depth >= globalState.getOptions().getMaxExpressionDepth() || percent33Chance()) {
    		return generateColumn(MutableDataType.BOOL);
        }

    	int n = Randomly.fromOptions(1, 2, 3);
    	switch (n) {
    	case 1:
    		// NOT
    		return new MutablePrefixExpression(generateExpressionEvaluatingToBoolean(depth + 1), PrefixOperator.NOT);
        case 2:
        	// BINARY LOGICAL
    		return new MutableBinaryLogicalExpression(
    				generateExpressionEvaluatingToBoolean(depth + 1),
    				generateExpressionEvaluatingToBoolean(depth + 1),
    				MutableBinaryLogicalExpression.MutableBinaryLogicalOperator.getRandom()
    			);
		case 3:
			// BINARY COMPARISON
			MutableBinaryComparisonExpression.MutableBinaryComparisonOperator op = MutableBinaryComparisonExpression.MutableBinaryComparisonOperator.getRandom();
			Tuple<MutableDataType, MutableDataType> types = op.getChildTypes();
			return new MutableBinaryComparisonExpression(
    				generateExpression(types.a, depth + 1),
    				generateExpression(types.b, depth + 1),
    				op
    			);
        default:
        	throw new Error("Unreachable");
    	}
    }

    // Random numeric expression (INT, FLOAT, DOUBLE, DECIMAL)
    private MutableExpression generateExpressionEvaluatingToNumeric(int depth) {
    	int n = Randomly.fromOptions(1, 2, 3, 4);
    	switch (n) {
    	case 1:
			return generateExpressionEvaluatingToINT(depth);
    	default:
    		return generateExpressionEvaluatingToFloatDoubleDecimal(depth);
    	}
    }

    // FLOAT, DOUBLE, DECIMAL
    private MutableExpression generateExpressionEvaluatingToFloatDoubleDecimal(int depth) {
    	if (depth >= globalState.getOptions().getMaxExpressionDepth() || percent33Chance()) {
    		MutableDataType t = Randomly.fromOptions(MutableDataType.FLOAT,
    				MutableDataType.DECIMAL, MutableDataType.DOUBLE);
    		return generateColumn(t);
        }

		if (allowAggregates && Randomly.getBooleanWithSmallProbability()) {
			allowAggregates = false;
			MutableDataType t = Randomly.fromOptions(MutableDataType.FLOAT,
    				MutableDataType.DECIMAL, MutableDataType.DOUBLE);
			return generateAggregate(t);
        }


    	int n = Randomly.fromOptions(1, 2);
    	switch (n) {
        case 1:
        	// UNARY
        	PrefixOperator prefix_op = Randomly.fromOptions(
        			PrefixOperator.UNARY_PLUS,
        			PrefixOperator.UNARY_MINUS
        		);
        	return new MutablePrefixExpression(generateExpressionEvaluatingToFloatDoubleDecimal(depth + 1), prefix_op);
		case 2:
			// BINARY
			MutableBinaryArithmeticOperator binary_op = Randomly.fromOptions(
					MutableBinaryArithmeticOperator.ADD,
					MutableBinaryArithmeticOperator.SUB,
					MutableBinaryArithmeticOperator.MUL,
					MutableBinaryArithmeticOperator.DIV
				);
			return new MutableBinaryArithmeticExpression(
					generateExpressionEvaluatingToFloatDoubleDecimal(depth + 1),
					generateExpressionEvaluatingToFloatDoubleDecimal(depth + 1),
					binary_op
				);
		default:
        	throw new Error("Unreachable");
    	}
    }

    // INT
    private MutableExpression generateExpressionEvaluatingToINT(int depth) {
    	if (depth >= globalState.getOptions().getMaxExpressionDepth() || percent33Chance()) {
    		return generateColumn(MutableDataType.INT);
        }

    	if (allowAggregates && Randomly.getBooleanWithSmallProbability()) {
			allowAggregates = false;
			return generateAggregate(MutableDataType.INT);
        }

    	int n = Randomly.fromOptions(1, 2);
    	switch (n) {
        case 1:
        	// UNARY
        	PrefixOperator prefix_op = Randomly.fromOptions(
        			PrefixOperator.UNARY_PLUS,
        			PrefixOperator.UNARY_MINUS,
        			PrefixOperator.NEG
        		);
        	return new MutablePrefixExpression(generateExpressionEvaluatingToINT(depth + 1), prefix_op);
		case 2:
			// BINARY
			MutableBinaryArithmeticOperator binary_op = Randomly.fromOptions(
					MutableBinaryArithmeticOperator.ADD,
					MutableBinaryArithmeticOperator.SUB,
					MutableBinaryArithmeticOperator.MUL,
					MutableBinaryArithmeticOperator.MOD
				);
			return new MutableBinaryArithmeticExpression(
					generateExpressionEvaluatingToINT(depth + 1),
					generateExpressionEvaluatingToINT(depth + 1),
					binary_op
				);
		default:
        	throw new Error("Unreachable");
    	}
    }

    public MutableExpression generateExpressionWithExpectedResult(MutableDataType type) {
        this.expectedResult = true;
        MutableTypedExpressionGenerator gen = new MutableTypedExpressionGenerator(globalState).setColumns(columns);
        gen = gen.setRowValue(rowValue);
        MutableExpression expr = null;
        try {
	        do {
	            expr = gen.generateExpression(type, 0);
	            // DEBUG
//                System.out.println("Expression: " + MutableToStringVisitor.asString(expr));
//                System.out.println("expected value: " + expr.getExpectedValue());
	        } while (expr.getExpectedValue() == null);
	        return expr;
        } catch (AssertionError e) {
        	if (expr!=null) System.out.println(MutableToStringVisitor.asString(expr));
        	System.out.println(e.getMessage());
        	throw e;
        }
    }

}
