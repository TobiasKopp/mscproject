package sqlancer.mutable.ast;

import sqlancer.mutable.MutableSchema.MutableColumn;

// TODO use sqlancer.common.ast.newast.ColumnReferenceNode instead
public class MutableColumnReference implements MutableExpression {
	private final MutableColumn c;

    public MutableColumnReference(MutableColumn c) {
        this.c = c;
    }

    public MutableColumn getColumn() {
        return c;
    }
}
