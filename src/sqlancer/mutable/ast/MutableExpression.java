package sqlancer.mutable.ast;

import sqlancer.mutable.MutableSchema.MutableDataType;

public interface MutableExpression {
	
	default MutableDataType getExpressionType() {
        return null;
    }

    default MutableConstant getExpectedValue() {
        return null;
    }
}
