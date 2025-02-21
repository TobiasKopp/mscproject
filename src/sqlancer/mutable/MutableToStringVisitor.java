package sqlancer.mutable;

import sqlancer.common.visitor.ToStringVisitor;
import sqlancer.mutable.ast.*;

public class MutableToStringVisitor extends ToStringVisitor<MutableExpression> {

    @Override
    public void visitSpecific(MutableExpression expr) {
        if (expr instanceof MutableConstant) {
            visit((MutableConstant) expr);
        } else if (expr instanceof MutableAggregate) {
            visit((MutableAggregate) expr);
        } else if (expr instanceof MutableFunctionExpression) {
            visit((MutableFunctionExpression) expr);
        } else if (expr instanceof MutablePrefixExpression) {
            visit((MutablePrefixExpression) expr);
        } else if (expr instanceof MutableSelect) {
            visit((MutableSelect) expr);
        } else if (expr instanceof MutablePostFixTextNode) {
            visit((MutablePostFixTextNode) expr);
        } else if (expr instanceof MutableColumnReference) {
            visit((MutableColumnReference) expr);
        } else if (expr instanceof MutableTableReference) {
            visit((MutableTableReference) expr);
        } else if (expr instanceof MutableColumnValue) {
            visit((MutableColumnValue) expr);
        } else {
            throw new AssertionError(expr.getClass());
        }
    }

    private void visit(MutableConstant constant) {
        sb.append(constant.getTextRepresentation());
    }

    private void visit(MutableAggregate expr) {
    	sb.append(expr.getFunction().name() + "(");
		visit(expr.getArgs());
		sb.append(")");
    }

    private void visit(MutableFunctionExpression expr) {
    	sb.append(expr.getFunctionName());
    	sb.append("(");
    	for (int i = 0; i < expr.getArguments().length; i++) {
    		if (i != 0) {
                sb.append(", ");
            }
    		visit(expr.getArguments()[i]);
    	}
    	sb.append(")");
    }

    private void visit(MutablePrefixExpression expr) {
    	sb.append(expr.getTextRepresentation());
    	sb.append("(");
    	visit(expr.getExpression());
    	sb.append(")");
    }

    private void visit(MutableSelect select) {
        sb.append("SELECT ");

        visit(select.getFetchColumns());
        sb.append(" FROM ");
        visit(select.getFromList());
        if (!select.getFromList().isEmpty() && !select.getJoinList().isEmpty()) {
            sb.append(", ");
        }
        if (select.getWhereClause() != null) {
            sb.append(" WHERE ");
            visit(select.getWhereClause());
        }
        if (!select.getGroupByExpressions().isEmpty()) {
            sb.append(" GROUP BY ");
            visit(select.getGroupByExpressions());
        }
        if (select.getHavingClause() != null) {
            sb.append(" HAVING ");
            visit(select.getHavingClause());
        }
        if (!select.getOrderByExpressions().isEmpty()) {
            sb.append(" ORDER BY ");
            visit(select.getOrderByExpressions());
        }
        if (select.getLimitClause() != null) {
            sb.append(" LIMIT ");
            visit(select.getLimitClause());
        }
    }

    private void visit(MutablePostFixTextNode expr) {
    	visit(expr.getExpr());
    	sb.append(" ");
    	sb.append(expr.getText());
    }

    private void visit(MutableColumnReference expr) {
    	sb.append(expr.getColumn().getFullQualifiedName());
    }

    private void visit(MutableColumnValue expr) {
    	sb.append(expr.getColumn().getFullQualifiedName());
    }

    private void visit(MutableTableReference expr) {
    	sb.append(expr.getTable().getName());
    }

    public static String asString(MutableExpression expr) {
        MutableToStringVisitor visitor = new MutableToStringVisitor();
        visitor.visit(expr);
        return visitor.get();
    }

}
