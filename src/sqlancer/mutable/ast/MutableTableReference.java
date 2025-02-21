package sqlancer.mutable.ast;

import sqlancer.mutable.MutableSchema.MutableTable;

// TODO use sqlancer.common.ast.newast.TableReferenceNode instead
public class MutableTableReference implements MutableExpression {
	private final MutableTable t;

    public MutableTableReference(MutableTable table) {
        this.t = table;
    }

    public MutableTable getTable() {
        return t;
    }
}
