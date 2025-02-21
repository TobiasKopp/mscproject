package sqlancer.mutable.ast;

import sqlancer.Randomly;
import sqlancer.common.ast.BinaryOperatorNode;
import sqlancer.common.ast.BinaryOperatorNode.Operator;
import sqlancer.mutable.MutableToStringVisitor;
import sqlancer.mutable.MutableSchema.MutableDataType;
import sqlancer.mutable.ast.MutableBinaryArithmeticExpression.MutableBinaryArithmeticOperator;

public class MutableBinaryArithmeticExpression extends BinaryOperatorNode<MutableExpression, MutableBinaryArithmeticOperator>
        implements MutableExpression {

    public enum MutableBinaryArithmeticOperator implements Operator {

        ADD("+") {
            @Override
            public MutableConstant apply(MutableConstant left, MutableConstant right) {
            	if (left.isNullConstant() || right.isNullConstant()) {
                    return MutableConstant.createNullConstant();
                } else if (left.isIntConstant() && right.isIntConstant()) {
                	long leftVal = left.asInt();
                	long rightVal = right.asInt();
                    return MutableConstant.createIntegerConstant(leftVal + rightVal);
                } else {
                    double leftVal = left.asDouble();
                    double rightVal = right.asDouble();
                    return MutableConstant.createFloatingPointConstant(leftVal + rightVal);
                }
            }
        },
        SUB("-") {
            @Override
            public MutableConstant apply(MutableConstant left, MutableConstant right) {
            	if (left.isNullConstant() || right.isNullConstant()) {
                    return MutableConstant.createNullConstant();
                } else if (left.isIntConstant() && right.isIntConstant()) {
                	long leftVal = left.asInt();
                	long rightVal = right.asInt();
                    return MutableConstant.createIntegerConstant(leftVal - rightVal);
                } else {
                    double leftVal = left.asDouble();
                    double rightVal = right.asDouble();
                    return MutableConstant.createFloatingPointConstant(leftVal - rightVal);
                }
            }
        },
        MUL("*") {
            @Override
            public MutableConstant apply(MutableConstant left, MutableConstant right) {
            	if (left.isNullConstant() || right.isNullConstant()) {
                    return MutableConstant.createNullConstant();
                } else if (left.isIntConstant() && right.isIntConstant()) {
                	long leftVal = left.asInt();
                	long rightVal = right.asInt();
                    return MutableConstant.createIntegerConstant(leftVal * rightVal);
                } else {
                    double leftVal = left.asDouble();
                    double rightVal = right.asDouble();
                    return MutableConstant.createFloatingPointConstant(leftVal * rightVal);
                }
            }
        },
        DIV("/") {
            @Override
            public MutableConstant apply(MutableConstant left, MutableConstant right) {
            	if (left.isNullConstant() || right.isNullConstant()) {
                    return MutableConstant.createNullConstant();
                } else {
                    double leftVal = left.asDouble();
                    double rightVal = right.asDouble();
                    if (rightVal == 0.0) { return null; }
                    return MutableConstant.createFloatingPointConstant(leftVal / rightVal);
                }
            }
        },
        MOD("%") {
            @Override
            public MutableConstant apply(MutableConstant left, MutableConstant right) {
            	if (left.isNullConstant() || right.isNullConstant()) {
                    return MutableConstant.createNullConstant();
                } 
            	else if (left.isIntConstant() && right.isIntConstant()) {
                    long leftVal = left.asInt();
                    long rightVal = right.asInt();
                    if (rightVal == 0) { return null; }
                    return MutableConstant.createIntegerConstant(leftVal % rightVal);
                } else {
                    double leftVal = left.asDouble();
                    double rightVal = right.asDouble();
                    if (rightVal == 0.0) { return null; }
                    return MutableConstant.createFloatingPointConstant(leftVal % rightVal);
                }
            }
        };

        private String textRepresentation;

        MutableBinaryArithmeticOperator(String textRepresentation) {
            this.textRepresentation = textRepresentation;
        }

        @Override
        public String getTextRepresentation() {
            return textRepresentation;
        }

        public abstract MutableConstant apply(MutableConstant left, MutableConstant right);

        public static MutableBinaryArithmeticOperator getRandom() {
            return Randomly.fromOptions(values());
        }

    }

    public MutableBinaryArithmeticExpression(MutableExpression left, MutableExpression right,
            MutableBinaryArithmeticOperator op) {
        super(left, right, op);
    }

    @Override
    public MutableConstant getExpectedValue() {
        MutableConstant leftExpected = getLeft().getExpectedValue();
        MutableConstant rightExpected = getRight().getExpectedValue();
//        System.out.println("MutableBinaryArithmeticExpression: " + leftExpected + getOp().textRepresentation + rightExpected);
        if (leftExpected == null || rightExpected == null) {
            return null;
        }
        return getOp().apply(leftExpected, rightExpected);
    }

    @Override
    public MutableDataType getExpressionType() {
        return MutableDataType.INT;
    }

}
