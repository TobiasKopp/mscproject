package sqlancer.mutable.utils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import sqlancer.mutable.MutableProvider.MutableGlobalState;
import sqlancer.mutable.MutableSchema;
import sqlancer.mutable.MutableSchema.MutableTables;
import sqlancer.mutable.MutableToStringVisitor;
import sqlancer.mutable.MutableSchema.MutableTable;
import sqlancer.mutable.MutableSchema.MutableCompositeDataType;
import sqlancer.mutable.MutableSchema.MutableDataType;
import sqlancer.mutable.MutableSchema.MutableColumn;
import sqlancer.mutable.ast.*;
import sqlancer.mutable.gen.MutableTypedExpressionGenerator;

public class ExprGenMeasurerer {
    
    private static void clearFile() {
        try {
            Path f = Paths.get("timings.txt");
            Files.write(f, Arrays.asList(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new AssertionError(e.getMessage());
        }
    }
    
    private static void writeToFile(String str) {
        try {
            Path f = Paths.get("timings.txt");
            Files.write(f, Arrays.asList(str), StandardCharsets.UTF_8, StandardOpenOption.APPEND);
//            Files.write(f, QUERIES, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new AssertionError(e.getMessage());
        }
    }
    
    private static String createString(double time, long depth, long nodes) {
        return String.format("%f | %d | %d", time, depth, nodes);
    }
    
    private static long computeDepth(MutableExpression e) {
        if (e instanceof MutableConstant || e instanceof MutableColumnReference || e instanceof MutableColumnValue) {
            return 0;
        } else if (e instanceof MutableAggregate) {
            return 1 + computeDepth(((MutableAggregate) e).getArgs().get(0));
        } else if (e instanceof MutableFunctionExpression) {
            return 1 + computeDepth(((MutableFunctionExpression) e).getArguments()[0]);
        } else if (e instanceof MutablePostFixTextNode) {
            return 1 + computeDepth(((MutablePostFixTextNode) e).getExpr());
        } else if (e instanceof MutablePrefixExpression) {
            return 1 + computeDepth(((MutablePrefixExpression) e).getExpression());
        } else if (e instanceof MutableBinaryArithmeticExpression) {
            return 1 + Math.max(
                    computeDepth(((MutableBinaryArithmeticExpression) e).getLeft()),
                    computeDepth(((MutableBinaryArithmeticExpression) e).getRight()));
        } else if (e instanceof MutableBinaryComparisonExpression) {
            return 1 + Math.max(
                    computeDepth(((MutableBinaryComparisonExpression) e).getLeft()),
                    computeDepth(((MutableBinaryComparisonExpression) e).getRight()));
        } else if (e instanceof MutableBinaryLogicalExpression) {
            return 1 + Math.max(
                    computeDepth(((MutableBinaryLogicalExpression) e).getLeft()),
                    computeDepth(((MutableBinaryLogicalExpression) e).getRight()));
        }
        return 0;
    }
    
    private static long computeNrNodes(MutableExpression e) {
        if (e instanceof MutableConstant || e instanceof MutableColumnReference || e instanceof MutableColumnValue) {
            return 1;
        } else if (e instanceof MutableAggregate) {
            return 1 + computeNrNodes(((MutableAggregate) e).getArgs().get(0));
        } else if (e instanceof MutableFunctionExpression) {
            return 1 + computeNrNodes(((MutableFunctionExpression) e).getArguments()[0]);
        } else if (e instanceof MutablePostFixTextNode) {
            return 1 + computeNrNodes(((MutablePostFixTextNode) e).getExpr());
        } else if (e instanceof MutablePrefixExpression) {
            return 1 + computeNrNodes(((MutablePrefixExpression) e).getExpression());
        } else if (e instanceof MutableBinaryArithmeticExpression) {
            return 1 + computeNrNodes(((MutableBinaryArithmeticExpression) e).getLeft())
                     + computeNrNodes(((MutableBinaryArithmeticExpression) e).getRight());
        } else if (e instanceof MutableBinaryComparisonExpression) {
            return 1 + computeNrNodes(((MutableBinaryComparisonExpression) e).getLeft())
                     + computeNrNodes(((MutableBinaryComparisonExpression) e).getRight());
        } else if (e instanceof MutableBinaryLogicalExpression) {
            return 1 + computeNrNodes(((MutableBinaryLogicalExpression) e).getLeft())
                     + computeNrNodes(((MutableBinaryLogicalExpression) e).getRight());
        }
        return 0;
    }
    
    private static MutableSchema createSchema() {
        List<MutableTable> tables = new ArrayList<>();
        List<MutableColumn> columns1 = new ArrayList<>();
        List<MutableColumn> columns2 = new ArrayList<>();
        
        columns1.add(new MutableColumn("c0", new MutableCompositeDataType(MutableDataType.CHAR, 128),    false, true,  false));
        columns1.add(new MutableColumn("c1", new MutableCompositeDataType(MutableDataType.VARCHAR, 128), false, false, true));
        columns1.add(new MutableColumn("c2", new MutableCompositeDataType(MutableDataType.INT, 4),       true,  true,  false));
        columns1.add(new MutableColumn("c3", new MutableCompositeDataType(MutableDataType.DECIMAL, 4,4), false, true,  false));
        columns1.add(new MutableColumn("c4", new MutableCompositeDataType(MutableDataType.BOOL, 0),      false, true,  false));
        columns1.add(new MutableColumn("c5", new MutableCompositeDataType(MutableDataType.DATE, 0),      false, false, false));
        columns1.add(new MutableColumn("c6", new MutableCompositeDataType(MutableDataType.DATETIME, 0),  false, true,  true));
        columns1.add(new MutableColumn("c7", new MutableCompositeDataType(MutableDataType.FLOAT, 0),     false, false, false));
        columns1.add(new MutableColumn("c8", new MutableCompositeDataType(MutableDataType.DOUBLE, 0),    false, false, false));
        
        columns2.add(new MutableColumn("c0", new MutableCompositeDataType(MutableDataType.CHAR, 128),    false, true, false));
        columns2.add(new MutableColumn("c1", new MutableCompositeDataType(MutableDataType.VARCHAR, 128), false, false, true));
        columns2.add(new MutableColumn("c2", new MutableCompositeDataType(MutableDataType.INT, 4),       true, true, false));
        columns2.add(new MutableColumn("c3", new MutableCompositeDataType(MutableDataType.DECIMAL, 4,4), false, true, false));
        columns2.add(new MutableColumn("c4", new MutableCompositeDataType(MutableDataType.BOOL, 0),      false, true, false));
        columns2.add(new MutableColumn("c5", new MutableCompositeDataType(MutableDataType.DATE, 0),      false, false, false));
        columns2.add(new MutableColumn("c6", new MutableCompositeDataType(MutableDataType.DATETIME, 0),  false, true, true));
        columns2.add(new MutableColumn("c7", new MutableCompositeDataType(MutableDataType.FLOAT, 0),     false, false, false));
        columns2.add(new MutableColumn("c8", new MutableCompositeDataType(MutableDataType.DOUBLE, 0),    false, false, false));

        tables.add(new MutableTable("R", columns1));
        tables.add(new MutableTable("S", columns2));
        return new MutableSchema(tables);
    }
    
    // Measure time for 'n' expressions
    public static void measure(MutableGlobalState state, int n) {
        clearFile();
        MutableTypedExpressionGenerator gen = new MutableTypedExpressionGenerator(state);
        gen.setColumns(new MutableTables(createSchema().getDatabaseTables()).getColumns());
        double sumTime = 0;
        long sumDepth = 0;
        long sumNodes = 0;
        for (int i=0; i<n; i++) {
            long startTime = System.nanoTime();
            MutableExpression e = gen.generatePredicate();
            long endTime = System.nanoTime();
            double time = ((double)endTime - startTime)/1000.0;
            long depth = (long) computeDepth(e);
            long nodes = (long) computeNrNodes(e);
            sumTime += time;
            sumDepth += depth;
            sumNodes += nodes;
            writeToFile(createString(time, depth, nodes));
        }
        System.out.println(sumTime/n);
        System.out.println(((double)sumDepth)/n);
        System.out.println(((double)sumNodes)/n);
    }
}
