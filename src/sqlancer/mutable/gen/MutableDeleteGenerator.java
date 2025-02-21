package sqlancer.mutable.gen;

import sqlancer.Randomly;
import sqlancer.common.query.ExpectedErrors;
import sqlancer.common.query.SQLQueryAdapter;
import sqlancer.mutable.MutableProvider.MutableGlobalState;
import sqlancer.mutable.MutableSchema.MutableDataType;
import sqlancer.mutable.MutableSchema.MutableTable;
import sqlancer.mutable.MutableToStringVisitor;

public final class MutableDeleteGenerator {

    private MutableDeleteGenerator() {
    }

    // TODO implement expected errors
    public static SQLQueryAdapter generate(MutableGlobalState globalState) {
        StringBuilder sb = new StringBuilder("DELETE FROM ");
        ExpectedErrors errors = new ExpectedErrors();
        MutableTable table = globalState.getSchema().getRandomTable(t -> !t.isView());
        sb.append(table.getName());
        if (Randomly.getBoolean()) {
        	MutableTypedExpressionGenerator gen = new MutableTypedExpressionGenerator(globalState).setColumns(table.getColumns());
            sb.append(" WHERE ");
            sb.append(MutableToStringVisitor.asString(gen.generateExpression(MutableDataType.BOOL, 0)));
        }
        //MutableErrors.addExpressionErrors(errors);
        return new SQLQueryAdapter(sb.toString(), errors);
    }

}
