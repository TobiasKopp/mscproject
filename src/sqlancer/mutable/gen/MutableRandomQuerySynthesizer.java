package sqlancer.mutable.gen;


// TODO even need this class?
public final class MutableRandomQuerySynthesizer {

    private MutableRandomQuerySynthesizer() {
    }

//    public static MutableSelect generateSelect(MutableProvider.MutableGlobalState globalState, int nrColumns) {
//        MutableTables targetTables = globalState.getSchema().getRandomTableNonEmptyTables();
//       // MutableExpressionGenerator gen = new MutableExpressionGenerator(globalState)
//               // .setColumns(targetTables.getColumns());
//        MutableSelect select = new MutableSelect();
//        // TODO: distinct
//        // select.setDistinct(Randomly.getBoolean());
//        // boolean allowAggregates = Randomly.getBooleanWithSmallProbability();
//        List<Node<MutableExpression>> columns = new ArrayList<>();
//        for (int i = 0; i < nrColumns; i++) {
//            // if (allowAggregates && Randomly.getBoolean()) {
//            Node<MutableExpression> expression = gen.generateExpression();
//            columns.add(expression);
//            // } else {
//            // columns.add(gen());
//            // }
//        }
//        select.setFetchColumns(columns);
//        List<MutableTable> tables = targetTables.getTables();
//        List<TableReferenceNode<MutableExpression, MutableTable>> tableList = tables.stream()
//                .map(t -> new TableReferenceNode<MutableExpression, MutableTable>(t)).collect(Collectors.toList());
//        List<Node<MutableExpression>> joins = MutableJoin.getJoins(tableList, globalState);
//        select.setJoinList(joins.stream().collect(Collectors.toList()));
//        select.setFromList(tableList.stream().collect(Collectors.toList()));
//        if (Randomly.getBoolean()) {
//            select.setWhereClause(gen.generateExpression());
//        }
//        if (Randomly.getBoolean()) {
//            select.setOrderByExpressions(gen.generateOrderBys());
//        }
//        if (Randomly.getBoolean()) {
//            select.setGroupByExpressions(gen.generateExpressions(Randomly.smallNumber() + 1));
//        }
//
//        if (Randomly.getBoolean()) {
//            select.setLimitClause(MutableConstant.createIntConstant(Randomly.getNotCachedInteger(0, Integer.MAX_VALUE)));
//        }
//        if (Randomly.getBoolean()) {
//            select.setOffsetClause(
//                    MutableConstant.createIntConstant(Randomly.getNotCachedInteger(0, Integer.MAX_VALUE)));
//        }
//        if (Randomly.getBoolean()) {
//            select.setHavingClause(gen.generateHavingClause());
//        }
//        return select;
//    }

}
