package sqlancer.mutable.ast;

import sqlancer.common.ast.BinaryOperatorNode.Operator;
import sqlancer.mutable.MutableSchema.MutableDataType;


// TODO maybe implement input types for typed expression generator
public class MutablePrefixExpression implements MutableExpression {

    public enum PrefixOperator implements Operator {
        NOT("NOT") {
            @Override
            protected MutableConstant getExpectedValue(MutableConstant expectedValue) {
                if (expectedValue.isNullConstant()) {
                    return MutableConstant.createNullConstant();
                } 
                if (expectedValue.isBooleanConstant()) {
                	return MutableConstant.createBooleanConstant(!expectedValue.asBoolean());
                }
                throw new AssertionError("Operand of unary `NOT` must be boolean: " + expectedValue);
            }
        },
        UNARY_PLUS("+") {
            @Override
            protected MutableConstant getExpectedValue(MutableConstant expectedValue) {
            	if (expectedValue.isNullConstant() || expectedValue.isIntConstant() || expectedValue.isFloatingPointConstant()) {
            		return expectedValue;
            	}
            	throw new AssertionError("Operand of unary `+` must be numeric: " + expectedValue);
            }

        },
        UNARY_MINUS("-") {
            @Override
            protected MutableConstant getExpectedValue(MutableConstant expectedValue) {
                if (expectedValue.isNullConstant()) {
                    return MutableConstant.createNullConstant();
                }
                if (expectedValue.isIntConstant()) {
                	return MutableConstant.createIntegerConstant(-expectedValue.asInt());
                }
                if (expectedValue.isFloatingPointConstant()) {
                	return MutableConstant.createFloatingPointConstant(-expectedValue.asDouble());
                }
                throw new AssertionError("Operand of unary `-` must be numeric: " + expectedValue);
            }

        },
        NEG("~") {
        	@Override
            protected MutableConstant getExpectedValue(MutableConstant expectedValue) {
        		if (expectedValue.isNullConstant()) {
                    return MutableConstant.createNullConstant();
                }
                if (expectedValue.isIntConstant()) {
                	return MutableConstant.createIntegerConstant(~expectedValue.asInt());
                }
                throw new AssertionError("Operand of unary `~` must be integer: " + expectedValue);
            }
        };

        private String textRepresentation;

        PrefixOperator(String textRepresentation) {
            this.textRepresentation = textRepresentation;
        }

        public MutableDataType getExpressionType(MutableExpression expr) {
        	return expr.getExpressionType();
        }

        protected abstract MutableConstant getExpectedValue(MutableConstant expectedValue);

        @Override
        public String getTextRepresentation() {
            return toString();
        }

    }

    private final MutableExpression expr;
    private final PrefixOperator op;

    public MutablePrefixExpression(MutableExpression expr, PrefixOperator op) {
        this.expr = expr;
        this.op = op;
    }

    @Override
    public MutableDataType getExpressionType() {
        return op.getExpressionType(expr);
    }

    @Override
    public MutableConstant getExpectedValue() {
        MutableConstant expectedValue = expr.getExpectedValue();
        if (expectedValue == null) {
            return null;
        }
//        System.out.println("MutablePrefixExpression: " + op.getTextRepresentation() + "(" + expectedValue + ")");
        return op.getExpectedValue(expectedValue);
    }

    public String getTextRepresentation() {
        return op.textRepresentation;
    }

    public MutableExpression getExpression() {
        return expr;
    }

}
