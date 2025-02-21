package sqlancer.mutable.ast;


// TODO use sqlancer.common.ast.newast.NewPostfixTextNode instead
public class MutablePostFixTextNode implements MutableExpression {
	private final MutableExpression expr;
    private final String text;

    public MutablePostFixTextNode(MutableExpression expr, String text) {
        this.expr = expr;
        this.text = text;
    }

    public MutableExpression getExpr() {
        return expr;
    }

    public String getText() {
        return text;
    }
}
