package sqlancer.mutable.utils;

import sqlancer.IgnoreMeException;
import sqlancer.Randomly;
import sqlancer.common.query.Query;
import sqlancer.common.query.SQLQueryAdapter;
import sqlancer.mutable.MutableProvider.MutableGlobalState;
import sqlancer.mutable.MutableSchema;
import sqlancer.mutable.MutableSchema.MutableTables;
import sqlancer.mutable.MutableSchema.MutableTable;
import sqlancer.mutable.MutableSchema.MutableCompositeDataType;
import sqlancer.mutable.MutableSchema.MutableDataType;
import sqlancer.mutable.MutableSchema.MutableRowValue;
import sqlancer.mutable.MutableSchema.MutableColumn;
import sqlancer.mutable.MutableToStringVisitor;
import sqlancer.mutable.ast.*;
import sqlancer.mutable.ast.MutableConstant.MutableBooleanConstant;
import sqlancer.mutable.ast.MutableConstant.MutableDateConstant;
import sqlancer.mutable.ast.MutableConstant.MutableDatetimeConstant;
import sqlancer.mutable.ast.MutableConstant.MutableFloatingPointConstant;
import sqlancer.mutable.ast.MutableConstant.MutableIntegerConstant;
import sqlancer.mutable.ast.MutableConstant.MutableNullConstant;
import sqlancer.mutable.ast.MutableConstant.MutableStringLiteral;
import sqlancer.mutable.gen.MutableTypedExpressionGenerator;
import sqlancer.mutable.jdbc.MutableJDBCResultSet;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TestCaseGenerator {
    private final MutableGlobalState state;
    private final String file = "QUERIES.sql";
    private final String oracle;
    private MutableSchema SCHEMA;
    private Map<String, List<String>> DATA;
    private List<String> QUERIES;
    private List<String> DATA_STATEMENTS;

    public TestCaseGenerator(MutableGlobalState state, String oracle) {
        this.state = state;
        this.oracle = oracle;
        this.QUERIES = new ArrayList<>();
        this.DATA_STATEMENTS = new ArrayList<>();
        createSchema();
        createData();
//        System.out.println(QUERIES);
//        System.out.println(DATA);
    }

    private void createSchema() {
        System.out.println("Creating Schema");
        List<MutableTable> tables = new ArrayList<>();
        List<MutableColumn> columns1 = new ArrayList<>();
        List<MutableColumn> columns2 = new ArrayList<>();
        List<MutableColumn> columns3 = new ArrayList<>();
        
        columns1.add(new MutableColumn("c0", new MutableCompositeDataType(MutableDataType.CHAR, 128),    false, true,  false));
        columns1.add(new MutableColumn("c1", new MutableCompositeDataType(MutableDataType.VARCHAR, 128), false, false, true));
        columns1.add(new MutableColumn("c2", new MutableCompositeDataType(MutableDataType.INT, 4),       true,  true,  false));
        columns1.add(new MutableColumn("c3", new MutableCompositeDataType(MutableDataType.DECIMAL, 4,4), false, true,  false));
        columns1.add(new MutableColumn("c4", new MutableCompositeDataType(MutableDataType.BOOL, 0),      false, true,  false));
        columns1.add(new MutableColumn("c5", new MutableCompositeDataType(MutableDataType.DATE, 0),      false, false, false));
        columns1.add(new MutableColumn("c6", new MutableCompositeDataType(MutableDataType.DATETIME, 0),  false, true,  true));
        columns1.add(new MutableColumn("c7", new MutableCompositeDataType(MutableDataType.FLOAT, 0),     false, false, false));
        columns1.add(new MutableColumn("c8", new MutableCompositeDataType(MutableDataType.DOUBLE, 0),    false, false, false));
        
        columns2.add(new MutableColumn("c0", new MutableCompositeDataType(MutableDataType.CHAR, 128),    false, true,  false));
        columns2.add(new MutableColumn("c1", new MutableCompositeDataType(MutableDataType.VARCHAR, 128), false, false, true));
        columns2.add(new MutableColumn("c2", new MutableCompositeDataType(MutableDataType.INT, 4),       true,  true,  false));
        columns2.add(new MutableColumn("c3", new MutableCompositeDataType(MutableDataType.DECIMAL, 4,4), false, true,  false));
        columns2.add(new MutableColumn("c4", new MutableCompositeDataType(MutableDataType.BOOL, 0),      false, true,  false));
        columns2.add(new MutableColumn("c5", new MutableCompositeDataType(MutableDataType.DATE, 0),      false, false, false));
        columns2.add(new MutableColumn("c6", new MutableCompositeDataType(MutableDataType.DATETIME, 0),  false, true,  true));
        columns2.add(new MutableColumn("c7", new MutableCompositeDataType(MutableDataType.FLOAT, 0),     false, false, false));
        columns2.add(new MutableColumn("c8", new MutableCompositeDataType(MutableDataType.DOUBLE, 0),    false, false, false));

//        columns3.add(new MutableColumn("c0", new MutableCompositeDataType(MutableDataType.CHAR, 128), false, true, false));
//        columns3.add(new MutableColumn("c1", new MutableCompositeDataType(MutableDataType.VARCHAR, 128), false, false, true));
//        columns3.add(new MutableColumn("c2", new MutableCompositeDataType(MutableDataType.INT, 4), true, true, false));
//        columns3.add(new MutableColumn("c3", new MutableCompositeDataType(MutableDataType.DECIMAL, 4,4), false, true, false));
//        columns3.add(new MutableColumn("c4", new MutableCompositeDataType(MutableDataType.BOOL, 0), false, true, false));
//        columns3.add(new MutableColumn("c5", new MutableCompositeDataType(MutableDataType.DATE, 0), false, false, false));
//        columns3.add(new MutableColumn("c6", new MutableCompositeDataType(MutableDataType.DATETIME, 0), false, true, true));
//        columns3.add(new MutableColumn("c7", new MutableCompositeDataType(MutableDataType.FLOAT, 0), false, false, false));
//        columns3.add(new MutableColumn("c8", new MutableCompositeDataType(MutableDataType.DOUBLE, 0), false, false, false));
        tables.add(new MutableTable("R", columns1));
        tables.add(new MutableTable("S", columns2));
//        tables.add(new MutableTable("T", columns3));
        this.SCHEMA = new MutableSchema(tables);
        
        DATA_STATEMENTS.add("CREATE DATABASE D;");
        DATA_STATEMENTS.add("USE D;");
        DATA_STATEMENTS.add("CREATE TABLE R (c0 CHAR(128), "
                                  + "c1 VARCHAR(128) NOT NULL UNIQUE, "
                                  + "c2 INT(4) PRIMARY KEY, "
                                  + "c3 DECIMAL(4,4), "
                                  + "c4 BOOL, "
                                  + "c5 DATE NOT NULL, "
                                  + "c6 DATETIME UNIQUE,"
                                  + "c7 FLOAT NOT NULL, "
                                  + "c8 DOUBLE NOT NULL);");
        DATA_STATEMENTS.add("CREATE TABLE S (c0 CHAR(128), "
                                    + "c1 VARCHAR(128) NOT NULL UNIQUE, "
                                    + "c2 INT(4) PRIMARY KEY, "
                                    + "c3 DECIMAL(4,4), "
                                    + "c4 BOOL, "
                                    + "c5 DATE NOT NULL, "
                                    + "c6 DATETIME UNIQUE, "
                                    + "c7 FLOAT NOT NULL, "
                                    + "c8 DOUBLE NOT NULL);");
        //        QUERIES.add("CREATE TABLE T (c0 CHAR(128), c1 VARCHAR(128), c2 INT(4), c3 DECIMAL(4,4), c4 BOOL, c5 DATE, c6 DATETIME, c7 FLOAT, c8 DOUBLE);");
    }
    
    private void createData() {
    	System.out.println("Creating Data");
    	List<String> dataR = new ArrayList<String>();
    	List<String> dataS = new ArrayList<String>();
//    	List<String> dataT = new ArrayList<String>();
    	Map<String, List<String>> data = new HashMap<String, List<String>> ();
    	data.put("R", dataR);
    	data.put("S", dataS);
//    	data.put("T", dataT);

    	for (int i=0; i<20; i++) {
    		MutableTable table = SCHEMA.getRandomTable();
    		List<MutableColumn> columns = table.getColumns();
    		StringBuilder sb = new StringBuilder();
    		sb.append("INSERT INTO ");
            sb.append(table.getName());
            sb.append(" VALUES ");
            // InsertColumns
            for (int nrRows = 0; nrRows < Randomly.smallNumber() + 1; nrRows++) {
                if (nrRows != 0) {
                    sb.append(", ");
                }
                sb.append("(");
                StringBuilder row = new StringBuilder();
                for (int nrColumn = 0; nrColumn < columns.size(); nrColumn++) {
                    if (nrColumn != 0) {
                        sb.append(", ");
                        row.append(", ");
                    }
                    // Insert value
                    MutableColumn mutableColumn = columns.get(nrColumn);
                    String value;
                    if (mutableColumn.isNullable() && Randomly.getBooleanWithRatherLowProbability()) {
                		value = "NULL";
//                	} else if (Randomly.getBooleanWithRatherLowProbability()) {
//                		value = "DEFAULT";
                	} else {
                		value = MutableToStringVisitor.asString(new MutableTypedExpressionGenerator(state).generateConstant(mutableColumn.getType().getPrimitiveDataType()));
                	}
                    sb.append(value);
                    row.append(value);
                }
                sb.append(")");
                data.get(table.getName()).add(row.toString());
            }
            sb.append(";");
            DATA_STATEMENTS.add(sb.toString());
    	}
    	DATA = data;
    
    }

    private void writeToFile(List<String> queries, String f_id) {
        List<String> combined = new ArrayList<String>();
        combined.addAll(DATA_STATEMENTS);
        combined.addAll(queries);
        try {
            Path f = Paths.get("queries/" + f_id + ".sql");
//            Files.write(f, QUERIES, StandardCharsets.UTF_8, StandardOpenOption.APPEND);
            Files.write(f, combined, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new AssertionError(e.getMessage());
        }
    }
    
    private void printProgress(int i, int n) {
//    	int percent5 = n/20;
//    	if (i%percent5 == 0) {
//    		System.out.println(String.format("%d", (int) i*100/n) + "%");
//    	}
    }

    // Generate 'n' test cases
    public void generate(int n) {
        System.out.println("Generating QUERIES for " + oracle);
        switch(oracle) {
            case "NoREC": generateNoREC(n); break;
            case "NoRECPlus": generateNoRECPlus(n); break;
            case "TLPWHERE": generateTLP("WHERE", n); break;
            case "TLPGROUPBY": generateTLP("GROUPBY", n); break;
            case "TLPHAVING": generateTLP("HAVING", n); break;
            case "TLPAGGREGATE": generateTLP("AGGREGATE", n); break;
            case "PQS": generatePQS(n); break;
        }
        //System.out.println(QUERIES);
        //writeToFile();
    }

    public void generateNoREC(Integer n) {
        MutableTables tables = new MutableTables(SCHEMA.getDatabaseTables());
        List<MutableColumn> columns = tables.getColumns();
        MutableTypedExpressionGenerator gen = new MutableTypedExpressionGenerator(state).setColumns(columns);

        for (Integer i = 0; i < n; i++) {
        	printProgress(i, n);
        	
            // Generate a random WHERE condition
            MutableExpression randomWhereCondition = gen.generateExpression(MutableDataType.BOOL, 0);

            // Create a TableReferenceNode for each table
            List<MutableTableReference> tableList = tables.getTables().stream().map(MutableTableReference::new).collect(Collectors.toList());

            // unoptimized
            MutableSelect select = new MutableSelect();
            MutableExpression asText = new MutablePostFixTextNode(randomWhereCondition, "AS flag");
            select.setFetchColumns(List.of(asText));
            select.setFromList(new ArrayList<>(tableList));
            if (Randomly.getBooleanWithSmallProbability()) {
                select.setOrderByExpressions(gen.generateOrderBys());
            }
            String unoptimizedQueryString = "SELECT COUNT(*) FROM (" + MutableToStringVisitor.asString(select) + ") AS res WHERE flag=TRUE;";

            // optimized
            select = new MutableSelect();
            List<MutableExpression> allColumns = columns.stream().map(MutableColumnReference::new).collect(Collectors.toList());
            select.setFetchColumns(allColumns);
            select.setFromList(new ArrayList<>(tableList));
            select.setWhereClause(randomWhereCondition);
            if (Randomly.getBooleanWithSmallProbability()) {
                select.setOrderByExpressions(gen.generateOrderBys());
            }
            String optimizedQueryString = MutableToStringVisitor.asString(select) + ";";
            
            List<String> queries = Arrays.asList(unoptimizedQueryString, optimizedQueryString);

//            QUERIES.add(unoptimizedQueryString);
//            QUERIES.add(optimizedQueryString);
            writeToFile(queries, i.toString());
        }
    }
    
    public void generateNoRECPlus(int n) {
        MutableTables tables = new MutableTables(SCHEMA.getDatabaseTables());
        List<MutableColumn> columns = tables.getColumns();
        MutableTypedExpressionGenerator gen = new MutableTypedExpressionGenerator(state).setColumns(columns);

        for (int i = 0; i < n; i++) {
            printProgress(i, n);
            
            // Generate a random WHERE condition
            MutableExpression randomWhereCondition = gen.generateExpression(MutableDataType.BOOL, 0);

            // Create a TableReferenceNode for each table
            List<MutableTableReference> tableList = tables.getTables().stream().map(MutableTableReference::new).collect(Collectors.toList());

            // unoptimized
            MutableSelect select = new MutableSelect();
            MutableExpression asText = new MutablePostFixTextNode(randomWhereCondition, "AS flag");
            select.setFetchColumns(List.of(asText));
            select.setFromList(new ArrayList<>(tableList));
            if (Randomly.getBooleanWithSmallProbability()) {
                select.setOrderByExpressions(gen.generateOrderBys());
            }
            String unoptimizedQueryString1 = "SELECT COUNT(*) FROM (" + MutableToStringVisitor.asString(select) + ") AS res WHERE flag=TRUE;";
            String unoptimizedQueryString2 = "SELECT COUNT(*) FROM (" + MutableToStringVisitor.asString(select) + ") AS res WHERE flag=FALSE;";
            String unoptimizedQueryString3 = "SELECT COUNT(*) FROM (" + MutableToStringVisitor.asString(select) + ") AS res WHERE ISNULL(flag);";

            // optimized
            select = new MutableSelect();
            List<MutableExpression> allColumns = columns.stream().map(MutableColumnReference::new).collect(Collectors.toList());
            select.setFetchColumns(allColumns);
            select.setFromList(new ArrayList<>(tableList));
            select.setWhereClause(randomWhereCondition);
            if (Randomly.getBooleanWithSmallProbability()) {
                select.setOrderByExpressions(gen.generateOrderBys());
            }
            String optimizedQueryString1 = MutableToStringVisitor.asString(select) + ";";
            select.setWhereClause(gen.negatePredicate(randomWhereCondition));
            String optimizedQueryString2 = MutableToStringVisitor.asString(select) + ";";
            select.setWhereClause(gen.isNull(randomWhereCondition));
            String optimizedQueryString3 = MutableToStringVisitor.asString(select) + ";";

//            QUERIES.add(unoptimizedQueryString1);
//            QUERIES.add(unoptimizedQueryString2);
//            QUERIES.add(unoptimizedQueryString3);
//            QUERIES.add(optimizedQueryString1);
//            QUERIES.add(optimizedQueryString2);
//            QUERIES.add(optimizedQueryString3);
            
            List<String> queries = new ArrayList<String>();
            queries.add(unoptimizedQueryString1);
            queries.add(unoptimizedQueryString2);
            queries.add(unoptimizedQueryString3);
            queries.add(optimizedQueryString1);
            queries.add(optimizedQueryString2);
            queries.add(optimizedQueryString3);
            writeToFile(queries, String.format("%d", i));
        }
    }

    public void generateTLP(String subOracle, int n) {
        MutableTables tables = new MutableTables(SCHEMA.getDatabaseTables());
        MutableTypedExpressionGenerator gen = new MutableTypedExpressionGenerator(state).setColumns(tables.getColumns());

        for (int i=0; i<n; i++) {
        	printProgress(i, n);
        	
            // Create ternary predicate variants
            MutableExpression predicate = gen.generatePredicate();
            MutableExpression negatedPredicate = gen.negatePredicate(predicate);
            MutableExpression isNullPredicate = gen.isNull(predicate);

            // Create select
            MutableSelect select = new MutableSelect();
            List<MutableExpression> fetchColumns = new ArrayList<>();
            if (Randomly.getBoolean() && subOracle == "WHERE") {
                fetchColumns.add(new MutableColumnReference(MutableColumn.createDummy("*")));
            } else {
                fetchColumns = Randomly.nonEmptySubset(tables.getColumns()).stream()
                        .map(c -> new MutableColumnReference(c)).collect(Collectors.toList());
            }
            select.setFetchColumns(fetchColumns);
            List<MutableTableReference> tableList = tables.getTables().stream().map(t -> new MutableTableReference(t)).collect(Collectors.toList());
            select.setFromList(tableList.stream().collect(Collectors.toList()));
            select.setWhereClause(null);
            if (Randomly.getBooleanWithRatherLowProbability()) {
                select.setOrderByExpressions(gen.generateOrderBys());
            }

            String originalQueryString;
            String firstQueryString;
            String secondQueryString;
            String thirdQueryString;

            if (subOracle == "WHERE") {
                // Create QUERIES
                originalQueryString = MutableToStringVisitor.asString(select);
                select.setWhereClause(predicate);
                firstQueryString = MutableToStringVisitor.asString(select);
                select.setWhereClause(negatedPredicate);
                secondQueryString = MutableToStringVisitor.asString(select);
                select.setWhereClause(isNullPredicate);
                thirdQueryString = MutableToStringVisitor.asString(select);
            } else if (subOracle == "GROUPBY") {
                select.setOrderByExpressions(new ArrayList<MutableExpression>());
                select.setGroupByExpressions(select.getFetchColumns());
                List<MutableExpression> fetch = new ArrayList<>();
                fetch.add(new MutableColumnReference(MutableColumn.createDummy("*")));
                select.setFetchColumns(fetch);
                select.setWhereClause(null);
                originalQueryString = MutableToStringVisitor.asString(select);
                select.setWhereClause(predicate);
                firstQueryString = MutableToStringVisitor.asString(select);
                select.setWhereClause(negatedPredicate);
                secondQueryString = MutableToStringVisitor.asString(select);
                select.setWhereClause(isNullPredicate);
                thirdQueryString = MutableToStringVisitor.asString(select);
            } else if (subOracle == "HAVING") {
                select.setOrderByExpressions(new ArrayList<MutableExpression>());
                select.setGroupByExpressions(select.getFetchColumns());
                List<MutableExpression> fetch = new ArrayList<>();
                fetch.add(new MutableColumnReference(MutableColumn.createDummy("*")));
                select.setFetchColumns(fetch);
                select.setWhereClause(null);
                originalQueryString = MutableToStringVisitor.asString(select);
                select.setHavingClause(predicate);
                firstQueryString = MutableToStringVisitor.asString(select);
                select.setHavingClause(negatedPredicate);
                secondQueryString = MutableToStringVisitor.asString(select);
                select.setHavingClause(isNullPredicate);
                thirdQueryString = MutableToStringVisitor.asString(select);
            } else {
                fetchColumns = new ArrayList<>();
                MutableAggregate.MutableAggregateFunction aggregateFunction = MutableAggregate.MutableAggregateFunction.getRandom();
                MutableExpression aggregate = gen.generateAggregateForFunction(aggregateFunction);

                if (aggregateFunction == MutableAggregate.MutableAggregateFunction.AVG) {
                    // Need to add a COUNT(*) in this case to compute the correct result
                    MutableExpression withCount = new MutablePostFixTextNode(aggregate, ", COUNT(*)");
                    fetchColumns.add(withCount);
                    select.setFetchColumns(fetchColumns);
                } else {
                    fetchColumns.add(aggregate);
                }
                select.setFetchColumns(fetchColumns);

                // Create the different query strings
                originalQueryString = MutableToStringVisitor.asString(select);
                select.setWhereClause(predicate);
                firstQueryString = MutableToStringVisitor.asString(select);
                select.setWhereClause(negatedPredicate);
                secondQueryString = MutableToStringVisitor.asString(select);
                select.setWhereClause(isNullPredicate);
                thirdQueryString = MutableToStringVisitor.asString(select);
            }

//            QUERIES.add(originalQueryString);
//            QUERIES.add(firstQueryString);
//            QUERIES.add(secondQueryString);
//            QUERIES.add(thirdQueryString);
            
            List<String> queries = new ArrayList<String>();
            queries.add(originalQueryString + ";");
            queries.add(firstQueryString + ";");
            queries.add(secondQueryString + ";");
            queries.add(thirdQueryString + ";");
            writeToFile(queries, String.format("%d", i));
        }
    }

    public void generatePQS(int n) {
        for (int i=0; i<n; i++) {
        	printProgress(i, n);
        	
            // Get random tables to include in the query
            MutableTables randomFromTables = SCHEMA.getRandomTableNonEmptyTables();
            List<MutableColumn> fetchColumns = randomFromTables.getColumns();
            
//            MutableJDBCResultSet resultSet = new MutableJDBCResultSet(null, DATA.get(randomFromTables.getTables().get(0).getName()));
//        	System.out.println(DATA.get(randomFromTables.getTables().get(0).getName()));
            
            MutableJDBCResultSet resultSet;
            if (randomFromTables.getTables().size() == 1) {
            	resultSet = new MutableJDBCResultSet(null, DATA.get(randomFromTables.getTables().get(0).getName()));
            	//System.out.println(DATA.get(randomFromTables.getTables().get(0).getName()));
            } else {
            	List<String> joinedData = new ArrayList<String>();
            	for (String r : DATA.get(randomFromTables.getTables().get(0).getName())) {
            		for (String s : DATA.get(randomFromTables.getTables().get(1).getName())) {
            			joinedData.add(r + ", " + s);
            		}
            	}
            	resultSet = new MutableJDBCResultSet(null, joinedData);
            	//System.out.println(joinedData);
            }
        	
        	// Generate first query that fetches pivot
        	MutableSelect select = new MutableSelect();
        	List<MutableExpression> fc = new ArrayList<>();
        	fc.add(new MutableColumnReference(MutableColumn.createDummy("*")));
        	select.setFetchColumns(fc);
        	select.setFromList(randomFromTables.getTables().stream().map(MutableTableReference::new).collect(Collectors.toList()));
            QUERIES.add(MutableToStringVisitor.asString(select) + ";");
        	
            //--------------------------
            // Get the random pivot row
            List<Map<MutableColumn, MutableConstant>> allRows = new ArrayList<Map<MutableColumn, MutableConstant>>();
            try {
				if (!resultSet.next()) {
				    throw new AssertionError("Could not find any rows!\n");
				}

				while (resultSet.next()) {
					Map<MutableColumn, MutableConstant> currentRow = new HashMap<>();
					for (int j = 0; j < randomFromTables.getColumns().size(); j++) {
				        MutableColumn column = randomFromTables.getColumns().get(j);
				        int columnIndex = j + 1;
				        MutableConstant constant;
				        if (resultSet.getString(columnIndex) == null) {
				            constant = new MutableNullConstant();
				        } else {
				            switch (column.getType().getPrimitiveDataType()) {
				            case INT:
				                constant = new MutableIntegerConstant(resultSet.getLong(columnIndex));
				                break;
				            case FLOAT:
				            case DOUBLE:
				            case DECIMAL:
				                constant = new MutableFloatingPointConstant(resultSet.getDouble(columnIndex));
				                break;
				            case BOOL:
				                constant = new MutableBooleanConstant(resultSet.getBoolean(columnIndex));
				                break;
				            case CHAR:
				            case VARCHAR:
				                constant = new MutableStringLiteral(resultSet.getString(columnIndex));
				                break;
				            case DATE:
				            	constant = new MutableDateConstant(resultSet.getDate(columnIndex));
				            	break;
				            case DATETIME:
				            	constant = new MutableDatetimeConstant(resultSet.getTimestamp(columnIndex));
				            	break;
				            default:
				                throw new IgnoreMeException();
				            }
				        }
				        currentRow.put(column, constant);
				    }
					allRows.add(currentRow);
				}
				assert !resultSet.next();
			} catch (SQLException | AssertionError e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				System.exit(1);
			}

            Map<MutableColumn, MutableConstant> randomSelectedRow = Randomly.fromList(allRows);
            MutableRowValue pivotRow = new MutableRowValue(randomFromTables, randomSelectedRow);
       
        
            //--------------------------
            // Generate a query that must fetch the pivot row
            select = new MutableSelect();
            select.setFromList(randomFromTables.getTables().stream().map(
                    t -> new MutableTableReference(t)).collect(Collectors.toList()));
            select.setFetchColumns(fetchColumns.stream()
                    .map(c -> new MutableColumnValue(getFetchValueAliasedColumn(c), pivotRow.getValues().get(c)))
                    .collect(Collectors.toList()));
            MutableExpression whereClause = generateRectifiedExpression(fetchColumns, pivotRow);
            select.setWhereClause(whereClause);
            List<MutableExpression> orderBy = new MutableTypedExpressionGenerator(state).setColumns(fetchColumns)
                    .generateOrderBys();
            select.setOrderByExpressions(orderBy);

//            QUERIES.add(MutableToStringVisitor.asString(select));
            
            try {
				getContainmentCheckQuery(new SQLQueryAdapter(MutableToStringVisitor.asString(select)), fetchColumns, pivotRow);
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
            //System.out.println("rectifiedQuery: " + MutableToStringVisitor.asString(select));
            writeToFile(QUERIES, String.format("%d", i));
            QUERIES = new ArrayList<String>();
        }
    }
    
    private MutableColumn getFetchValueAliasedColumn(MutableColumn c) {
        MutableColumn aliasedColumn = new MutableColumn(
        		c.getName() + " AS " + c.getTable().getName() + c.getName(),
                c.getType(),
                c.isPrimaryKey(),
                c.isNullable(),
                c.isUnique());
        aliasedColumn.setTable(c.getTable());
        return aliasedColumn;
    }
    
    private MutableExpression generateRectifiedExpression(List<MutableColumn> columns, MutableRowValue pivotRow) {
    	MutableTypedExpressionGenerator gen = new MutableTypedExpressionGenerator(state).setColumns(columns);
    	gen = gen.setRowValue(pivotRow);
        MutableExpression expr = gen.generateExpressionWithExpectedResult(MutableDataType.BOOL);
        MutableExpression rectifiedPredicate;
        MutableConstant expectedValue = expr.getExpectedValue();
        if (expectedValue.isNullConstant()) {
        	rectifiedPredicate = gen.isNull(expr);
        } else {
        	if (expectedValue.asBoolean()) {
        		rectifiedPredicate = expr;
        	} else {
        		rectifiedPredicate = gen.negatePredicate(expr);
        	}
        }
        return rectifiedPredicate;
    }
    
    private void getContainmentCheckQuery(Query<?> query, List<MutableColumn> fetchColumns, MutableRowValue pivotRow) throws SQLException {
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT * FROM ("); // ANOTHER SELECT TO USE ORDER BY without restrictions
        sb.append(query.getUnterminatedQueryString());
        sb.append(") AS result WHERE ");
        int i = 0;
        for (MutableColumn c : fetchColumns) {
            if (i++ != 0) {
                sb.append(" AND ");
            }
            if (pivotRow.getValues().get(c).isNullConstant()) {
            	sb.append("ISNULL(");
            	sb.append("result.");
                sb.append(c.getTable().getName());
                sb.append(c.getName());
                sb.append(")");
            } else {
            	sb.append("result.");
                sb.append(c.getTable().getName());
                sb.append(c.getName());
                sb.append(" = ");
                String val = pivotRow.getValues().get(c).getTextRepresentation();
                if (val.charAt(0)=='"' && val.charAt(val.length()-1)=='"') {
                	val = val.substring(1, val.length()-1);
                }
                sb.append(val);
            }
        }
        String resultingQueryString = sb.toString();
        QUERIES.add(resultingQueryString + ";");
//        return new SQLQueryAdapter(resultingQueryString, errors);
    }

}
