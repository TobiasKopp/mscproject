package sqlancer.mutable.ast;

import sqlancer.Randomly;
import sqlancer.common.ast.BinaryOperatorNode;
import sqlancer.common.ast.BinaryOperatorNode.Operator;
import sqlancer.mutable.MutableSchema.MutableDataType;
import sqlancer.mutable.ast.MutableBinaryLogicalExpression.MutableBinaryLogicalOperator;
import sqlancer.mutable.ast.MutableConstant.MutableBooleanConstant;

public class MutableBinaryLogicalExpression extends BinaryOperatorNode<MutableExpression, MutableBinaryLogicalOperator>
        implements MutableExpression {

    public enum MutableBinaryLogicalOperator implements Operator {
        AND {
            @Override
            public MutableConstant apply(MutableConstant left, MutableConstant right) {
            	if (right.isBooleanConstant() && left.isBooleanConstant()) {
            		boolean leftValue = ((MutableBooleanConstant) left).getValue();
                    boolean rightValue = ((MutableBooleanConstant) right).getValue();
                    return MutableConstant.createBooleanConstant(leftValue && rightValue);
                }
                if (left.isNullConstant() && right.isBooleanConstant()) {
                	return MutableConstant.createBooleanConstant(false);
                }
                if (left.isBooleanConstant() && right.isNullConstant()) {
                	return MutableConstant.createBooleanConstant(false);
                }
                if (left.isNullConstant() && right.isNullConstant()) {
                	return MutableConstant.createNullConstant();
                }
                throw new AssertionError("Operands of binary logical expression must be of boolean type");
            }
        },
        OR {
            @Override
            public MutableConstant apply(MutableConstant left, MutableConstant right) {
            	if (right.isBooleanConstant() && left.isBooleanConstant()) {
            		boolean leftValue = ((MutableBooleanConstant) left).getValue();
                    boolean rightValue = ((MutableBooleanConstant) right).getValue();
                    return MutableConstant.createBooleanConstant(leftValue || rightValue);
                }
                if (left.isNullConstant() && right.isBooleanConstant()) {
                	boolean rightValue = ((MutableBooleanConstant) right).getValue();
                	return MutableConstant.createBooleanConstant(rightValue);
                }
                if (left.isBooleanConstant() && right.isNullConstant()) {
                	boolean leftValue = ((MutableBooleanConstant) left).getValue();
                	return MutableConstant.createBooleanConstant(leftValue);
                }
                if (left.isNullConstant() && right.isNullConstant()) {
                	return MutableConstant.createNullConstant();
                }
                throw new AssertionError("Operands of binary logical expression must be of boolean type");
            }
        };

        public abstract MutableConstant apply(MutableConstant left, MutableConstant right);

        public static MutableBinaryLogicalOperator getRandom() {
            return Randomly.fromOptions(values());
        }

        @Override
        public String getTextRepresentation() {
            return toString();
        }
    }

    public MutableBinaryLogicalExpression(MutableExpression left, MutableExpression right, MutableBinaryLogicalOperator op) {
        super(left, right, op);
    }

    @Override
    public MutableDataType getExpressionType() {
        return MutableDataType.BOOL;
    }

    @Override
    public MutableConstant getExpectedValue() {
        MutableConstant leftExpectedValue = getLeft().getExpectedValue();
        MutableConstant rightExpectedValue = getRight().getExpectedValue();
        if (leftExpectedValue == null || rightExpectedValue == null) {
            return null;
        }
//        System.out.println("MutableBinaryLogicalExpression: " + leftExpectedValue + getOp().getTextRepresentation() + rightExpectedValue);
        return getOp().apply(leftExpectedValue, rightExpectedValue);
    }

}
