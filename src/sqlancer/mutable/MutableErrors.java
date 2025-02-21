package sqlancer.mutable;

import sqlancer.common.query.ExpectedErrors;

public final class MutableErrors {

    private MutableErrors() {
    }
    
    public static void addCommonErrors(ExpectedErrors errors) {
    	errors.add("No database selected");
    	errors.add("No database in use");
    }
    
    public static void addInsertErrors(ExpectedErrors errors) {
    	errors.add("has not enough values");
    	errors.add("is not valid for attribute");
    	errors.add("Value NULL is not valid for attribute");
    }
    
    public static void addExpressionErrors(ExpectedErrors errors) {    	
    	// Designator
    	errors.add("nested queries must have an alias");	// insist
    	errors.add("is ambiguous, multiple occurrences in SELECT clause");
    	errors.add("is ambiguous, multiple occurrences in GROUP BY clause");
    	errors.add("grouping expression must be of primitive type");	// insist
    	errors.add("not found. Maybe you forgot to specify it in the FROM clause?");
    	errors.add("has no attribute");
    	errors.add("has multiple attributes");
    	errors.add("is ambiguous");
    	// TODO ? regex for "Attribute specifier " << e.attr_name.text << " is ambiguous.\n"
    	errors.add("not found");
    	// TODO ? regex for "Attribute " << e.attr_name.text << " not found.\n"
    	errors.add("Correlated attributes are not allowed in the FROM clause");
    	
    	// Constant
    	errors.add("has invalid year (after year -1 (1 BC) follows year 1 (1 AD))");
    	errors.add("has invalid month");
    	errors.add("has invalid day");
    	errors.add("has invalid hour");
    	errors.add("has invalid minute");
    	errors.add("has invalid second");
    	
    	// FnApplicationExpr
    	errors.add("is not a valid function");
    	errors.add("This identifier has already been analyzed");	// insist
    	errors.add("is not defined in database");
    	errors.add("is not defined");
    	errors.add("User-defined functions are not yet supported");
    	errors.add("Missing argument for aggregate");
    	errors.add("Too many arguments for aggregate");
    	errors.add("Argument of aggregate function must be of numeric type");
    	errors.add("Argument of aggregate is not of vectorial type");	// warning
    	errors.add("Function ISNULL can only be applied to expressions of primitive type");
    	errors.add("Aggregate functions are not allowed in WHERE clause");
    	errors.add("Aggregate functions are not allowed in GROUP BY clause");
    	
    	errors.add("Invalid expression");
    	// UnaryExpr
    	errors.add("must be boolean");
    	errors.add("must be numeric");
    	
    	// BinaryExpr
    	errors.add("operands must be of numeric type");
    	errors.add("concatenation requires string operands");
    	errors.add("both operands must be of numeric type");
    	errors.add("both operands must be strings");
    	errors.add("both operands must be dates");
    	errors.add("both operands must be datetimes");
    	errors.add("operator not supported for given operands");
    	errors.add("operands are incomparable");
    	errors.add("operands must be character sequences");
    	errors.add("operands must be of boolean type");
    	
    	// QueryExpr
    	errors.add("nested statements are always select statements");	// insist
    	errors.add("nested statement must return a single column");
    	errors.add("nested statement must return a primitive value");
    	errors.add("Nested statements are not allowed in this stage");
    	errors.add("nested statement must return a scalar value");
    	errors.add("nested statement must return a single value");
    	
    	// SelectClause
    	errors.add("grouping key must be of primitive type");	// insist
    	errors.add("The '*' has no meaning in this query.  Did you forget the GROUP BY clause?");	// warning
    	errors.add("result of nested query must be of primitive type");		// insist
    	errors.add("contains free variables (not yet supported)");
    	errors.add("is not scalar");
    	errors.add("SELECT clause with mixed scalar and vectorial values is forbidden");
    	
    	// FromClause
    	// TODO regex for "Table name " << table_name.text << " already in use.\n"
    	// TODO regex for "No table " << name->text << " in database " << DB.name << ".\n"
    	errors.add("nested statements are always select statements");	// insist
    	
    	// WhereClause
    	errors.add("The expression in the WHERE clause must be of boolean type");
    	
    	// GroupByClause
    	errors.add("contains free variable(s) (not yet supported)");
    	errors.add("Cannot group by");
    	errors.add("has invalid type");
    	errors.add("Expressions in the GROUP BY clause must be vectorial, i.e. they must depend on each row separately");
    	
    	// HavingClause
    	errors.add("The expression in the HAVING clause must be of boolean type");
    	errors.add("The expression in the HAVING clause must be scalar");
    	
    	// OrderByClause
    	errors.add("contains free variable(s) (not yet supported)");
    	errors.add("Cannot order by");
    	// TODO ? regex for "Cannot order by " << *e << ", expression must be scalar.\n"
    	// TODO ? regex for "Cannot order by " << *e << ", expression must be vectorial.\n"
    	errors.add("expression must be scalar");
    	errors.add("expression must be vectorial");
    	
    	// LimitClause
    	errors.add("Invalid value for LIMIT");
    	errors.add("Value of LIMIT out of range");
    	errors.add("Invalid LIMIT");
    	errors.add("Invalid value for OFFSET");
    	errors.add("Value of OFFSET out of rang");
    	errors.add("Invalid OFFSET");
    	
    	// Instruction
    	// TODO ? regex for "Instruction " << I.name << " unknown\n"
    	errors.add("unknown");
    	
    	// CreateDatabaseStmt
    	// TODO ? regex for "Database " << db_name << " already exists.\n"
    	errors.add("already exists");
    	
    	// DropDatabaseStmt
    	// TODO ? regex for "Database " << db_name << " is in use.\n"
    	errors.add("is in use");
    	// TODO ? regex for "Database " << db_name << " does not exist.\n"
    	errors.add("does not exist");
    	
    	// Expressions
    	errors.add("expected expression");
    	
    	// Dates
    	errors.add("unterminated date");
    }

}
