package sqlancer.mutable.ast;

import sqlancer.mutable.MutableSchema.MutableColumn;
import sqlancer.mutable.MutableSchema.MutableDataType;

// TODO ?
public class MutableColumnValue implements MutableExpression {

    private final MutableColumn c;
    private final MutableConstant expectedValue;

    public MutableColumnValue(MutableColumn c, MutableConstant expectedValue) {
        this.c = c;
        this.expectedValue = expectedValue;
    }

    @Override
    public MutableDataType getExpressionType() {
        return c.getType().getPrimitiveDataType();
    }

    @Override
    public MutableConstant getExpectedValue() {
        return expectedValue;
    }

    public static MutableColumnValue create(MutableColumn c, MutableConstant expected) {
        return new MutableColumnValue(c, expected);
    }

    public MutableColumn getColumn() {
        return c;
    }

}
