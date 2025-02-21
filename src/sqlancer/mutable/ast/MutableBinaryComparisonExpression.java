package sqlancer.mutable.ast;

import sqlancer.Randomly;
import sqlancer.common.ast.BinaryOperatorNode;
import sqlancer.common.ast.BinaryOperatorNode.Operator;
import sqlancer.mutable.MutableSchema.MutableDataType;
import sqlancer.mutable.ast.MutableBinaryComparisonExpression.MutableBinaryComparisonOperator;
import sqlancer.mutable.utils.Tuple;

import java.util.ArrayList;
import java.util.List;

public class MutableBinaryComparisonExpression
        extends BinaryOperatorNode<MutableExpression, MutableBinaryComparisonOperator> implements MutableExpression {

    public enum MutableBinaryComparisonOperator implements Operator {
        EQUALS("=") {
            @Override
            public MutableConstant getExpectedValue(MutableConstant leftVal, MutableConstant rightVal) {
                return leftVal.isEquals(rightVal);
            }

            @Override
            // BOOL added for this operator
            public Tuple<MutableDataType, MutableDataType> getChildTypes() {
            	MutableDataType left_type = MutableDataType.getRandomWithoutNull();
            	List<MutableDataType> possible_right_types = new ArrayList<MutableDataType>();

            	switch (left_type) {
            	case BOOL: possible_right_types.add(MutableDataType.BOOL); break;
            	case CHAR:
            	case VARCHAR: possible_right_types.add(MutableDataType.CHAR);
        					  possible_right_types.add(MutableDataType.VARCHAR); break;
            	case DATE: possible_right_types.add(MutableDataType.DATE); break;
            	case DATETIME: possible_right_types.add(MutableDataType.DATETIME); break;
            	case INT:
            	case FLOAT:
            	case DOUBLE:
            	case DECIMAL: possible_right_types.add(MutableDataType.INT);
    					      possible_right_types.add(MutableDataType.FLOAT);
    					      possible_right_types.add(MutableDataType.DOUBLE);
    					      possible_right_types.add(MutableDataType.DECIMAL); break;
            	}

            	MutableDataType right_type = Randomly.fromList(possible_right_types);
            	if (Randomly.getBoolean()) {
            		return new Tuple<MutableDataType, MutableDataType>(left_type, right_type);
            	} else {
            		return new Tuple<MutableDataType, MutableDataType>(right_type, left_type);
            	}
            }
        },
        NOT_EQUALS("!=") {
            @Override
            public MutableConstant getExpectedValue(MutableConstant leftVal, MutableConstant rightVal) {
                MutableConstant isEquals = leftVal.isEquals(rightVal);
                if (isEquals.isBooleanConstant()) {
                    return MutableConstant.createBooleanConstant(!isEquals.asBoolean());
                }
                return isEquals;
            }

            @Override
            // Bool added for this operator
            public Tuple<MutableDataType, MutableDataType> getChildTypes() {
            	MutableDataType left_type = MutableDataType.getRandomWithoutNull();
            	List<MutableDataType> possible_right_types = new ArrayList<MutableDataType>();

            	switch (left_type) {
            	case BOOL: possible_right_types.add(MutableDataType.BOOL); break;
            	case CHAR:
            	case VARCHAR: possible_right_types.add(MutableDataType.CHAR);
        					  possible_right_types.add(MutableDataType.VARCHAR); break;
            	case DATE: possible_right_types.add(MutableDataType.DATE); break;
            	case DATETIME: possible_right_types.add(MutableDataType.DATETIME); break;
            	case INT:
            	case FLOAT:
            	case DOUBLE:
            	case DECIMAL: possible_right_types.add(MutableDataType.INT);
    					      possible_right_types.add(MutableDataType.FLOAT);
    					      possible_right_types.add(MutableDataType.DOUBLE);
    					      possible_right_types.add(MutableDataType.DECIMAL); break;
            	}

            	MutableDataType right_type = Randomly.fromList(possible_right_types);
            	if (Randomly.getBoolean()) {
            		return new Tuple<MutableDataType, MutableDataType>(left_type, right_type);
            	} else {
            		return new Tuple<MutableDataType, MutableDataType>(right_type, left_type);
            	}
            }
        },
        SMALLER("<") {
            @Override
            public MutableConstant getExpectedValue(MutableConstant leftVal, MutableConstant rightVal) {
                return leftVal.isLessThan(rightVal);
            }
        },
        SMALLER_EQUALS("<=") {
            @Override
            public MutableConstant getExpectedValue(MutableConstant leftVal, MutableConstant rightVal) {
                MutableConstant lessThan = leftVal.isLessThan(rightVal);
                if (lessThan.isBooleanConstant() && !lessThan.asBoolean()) {
                    return leftVal.isEquals(rightVal);
                } else {
                    return lessThan;
                }
            }
        },
        GREATER(">") {
            @Override
            public MutableConstant getExpectedValue(MutableConstant leftVal, MutableConstant rightVal) {
                MutableConstant equals = leftVal.isEquals(rightVal);
                if (equals.isBooleanConstant() && equals.asBoolean()) {
                    return MutableConstant.createBooleanConstant(false);
                } else {
                    MutableConstant lessThan = leftVal.isLessThan(rightVal);
                    if (lessThan.isBooleanConstant()) {
                    	return MutableConstant.createBooleanConstant(!lessThan.asBoolean());
                    } else {
                        return lessThan;
                    }
                }
            }
        },
        GREATER_EQUALS(">=") {
            @Override
            public MutableConstant getExpectedValue(MutableConstant leftVal, MutableConstant rightVal) {
                MutableConstant equals = leftVal.isEquals(rightVal);
                if (equals.isBooleanConstant() && equals.asBoolean()) {
                    return MutableConstant.createBooleanConstant(true);
                } else {
                    MutableConstant lessThan = leftVal.isLessThan(rightVal);
                    if (lessThan.isBooleanConstant()) {
                    	return MutableConstant.createBooleanConstant(!lessThan.asBoolean());
                    } else {
                        return lessThan;
                    }
                }
            }
        };

        private final String textRepresentation;

        @Override
        public String getTextRepresentation() {
            return textRepresentation;
        }

        MutableBinaryComparisonOperator(String textRepresentation) {
            this.textRepresentation = textRepresentation;
        }

        public abstract MutableConstant getExpectedValue(MutableConstant leftVal, MutableConstant rightVal);

        public Tuple<MutableDataType, MutableDataType> getChildTypes() {
        	MutableDataType left_type;
        	do {
        		left_type = MutableDataType.getRandomWithoutNull();
        	} while (left_type == MutableDataType.BOOL);
        	List<MutableDataType> possible_right_types = new ArrayList<MutableDataType>();

        	switch (left_type) {
        	case CHAR:
        	case VARCHAR: possible_right_types.add(MutableDataType.CHAR);
    					  possible_right_types.add(MutableDataType.VARCHAR); break;
        	case DATE: possible_right_types.add(MutableDataType.DATE); break;
        	case DATETIME: possible_right_types.add(MutableDataType.DATETIME); break;
        	case INT:
        	case FLOAT:
        	case DOUBLE:
        	case DECIMAL: possible_right_types.add(MutableDataType.INT);
					      possible_right_types.add(MutableDataType.FLOAT);
					      possible_right_types.add(MutableDataType.DOUBLE);
					      possible_right_types.add(MutableDataType.DECIMAL); break;
        	case BOOL: throw new AssertionError("Illegal type BOOL");
        	}

        	MutableDataType right_type = Randomly.fromList(possible_right_types);
        	if (Randomly.getBoolean()) {
        		return new Tuple<MutableDataType, MutableDataType>(left_type, right_type);
        	} else {
        		return new Tuple<MutableDataType, MutableDataType>(right_type, left_type);
        	}
        }

        public static MutableBinaryComparisonOperator getRandom() {
            return Randomly.fromOptions(MutableBinaryComparisonOperator.values());
        }

    }

    public MutableBinaryComparisonExpression(MutableExpression left, MutableExpression right,
            MutableBinaryComparisonOperator op) {
        super(left, right, op);
    }

    @Override
    public MutableConstant getExpectedValue() {
        MutableConstant leftExpectedValue = getLeft().getExpectedValue();
        MutableConstant rightExpectedValue = getRight().getExpectedValue();
        if (leftExpectedValue == null || rightExpectedValue == null) {
            return null;
        }
//        System.out.println("MutableBinaryComparisonExpression: " + leftExpectedValue + getOp().textRepresentation + rightExpectedValue);
        return getOp().getExpectedValue(leftExpectedValue, rightExpectedValue);
    }

    @Override
    public MutableDataType getExpressionType() {
        return MutableDataType.BOOL;
    }

}
