package sqlancer.mutable.gen;

import sqlancer.Randomly;
import sqlancer.common.gen.AbstractUpdateGenerator;
import sqlancer.common.query.SQLQueryAdapter;
import sqlancer.mutable.MutableProvider.MutableGlobalState;
import sqlancer.mutable.MutableSchema.MutableColumn;
import sqlancer.mutable.MutableSchema.MutableDataType;
import sqlancer.mutable.MutableSchema.MutableTable;
import sqlancer.mutable.MutableToStringVisitor;
import sqlancer.mutable.ast.MutableExpression;

import java.util.List;

public final class MutableUpdateGenerator extends AbstractUpdateGenerator<MutableColumn> {

    private final MutableGlobalState globalState;
    private MutableTypedExpressionGenerator gen;

    private MutableUpdateGenerator(MutableGlobalState globalState) {
        this.globalState = globalState;
    }

    public static SQLQueryAdapter getQuery(MutableGlobalState globalState) {
        return new MutableUpdateGenerator(globalState).generate();
    }

    private SQLQueryAdapter generate() {
        MutableTable table = globalState.getSchema().getRandomTable(t -> !t.isView());
        List<MutableColumn> columns = table.getRandomNonEmptyColumnSubset();
        gen = new MutableTypedExpressionGenerator(globalState).setColumns(table.getColumns());
        sb.append("UPDATE ");
        sb.append(table.getName());
        sb.append(" SET ");
        updateColumns(columns);
        //MutableErrors.addInsertErrors(errors);
        return new SQLQueryAdapter(sb.toString(), errors);
    }

    @Override
    protected void updateValue(MutableColumn column) {
        MutableExpression expr;
        MutableDataType type = column.getType().getPrimitiveDataType();
        if (Randomly.getBooleanWithSmallProbability()) {
            expr = gen.generateExpression(type, 0);
            //MutableErrors.addExpressionErrors(errors);
        } else {
            expr = gen.generateConstant(type);
        }
        sb.append(MutableToStringVisitor.asString(expr));
    }

}
