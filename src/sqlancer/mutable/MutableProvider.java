package sqlancer.mutable;

import com.google.auto.service.AutoService;
import sqlancer.*;
import sqlancer.common.query.SQLQueryAdapter;
import sqlancer.common.query.SQLQueryProvider;
import sqlancer.mutable.MutableProvider.MutableGlobalState;
import sqlancer.mutable.gen.MutableInsertGenerator;
import sqlancer.mutable.gen.MutableTableGenerator;
import sqlancer.mutable.jdbc.MutableJDBCDriver;
import sqlancer.mutable.utils.ExprGenMeasurerer;
import sqlancer.mutable.utils.TestCaseGenerator;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

@AutoService(DatabaseProvider.class)
public class MutableProvider extends SQLProviderAdapter<MutableGlobalState, MutableOptions> {

    public MutableProvider() {
        super(MutableGlobalState.class, MutableOptions.class);
        new MutableJDBCDriver();
    }

    public enum Action implements AbstractAction<MutableGlobalState> {

        INSERT(MutableInsertGenerator::getQuery);
        //UPDATE(MutableUpdateGenerator::getQuery),
        //DELETE(MutableDeleteGenerator::generate);

        private final SQLQueryProvider<MutableGlobalState> sqlQueryProvider;

        Action(SQLQueryProvider<MutableGlobalState> sqlQueryProvider) {
            this.sqlQueryProvider = sqlQueryProvider;
        }

        @Override
        public SQLQueryAdapter getQuery(MutableGlobalState state) throws Exception {
            return sqlQueryProvider.getQuery(state);
        }
    }

    private static int mapActions(MutableGlobalState globalState, Action a) {
        Randomly r = globalState.getRandomly();
        switch (a) {
        case INSERT:
            return r.getInteger(0, globalState.getOptions().getMaxNumberInserts());
//        case UPDATE:
//            return r.getInteger(0, globalState.getDbmsSpecificOptions().maxNumUpdates);
//        case DELETE:
//            return r.getInteger(0, globalState.getDbmsSpecificOptions().maxNumDeletes);
        default:
            throw new AssertionError(a);
        }
    }

    public static class MutableGlobalState extends SQLGlobalState<MutableOptions, MutableSchema> {

        @Override
        protected MutableSchema readSchema() throws SQLException {
            return MutableSchema.fromConnection(getConnection(), getDatabaseName());
        }

    }

    @Override
    public void generateDatabase(MutableGlobalState globalState) throws Exception {
        for (int i = 0; i < Randomly.fromOptions(1, 2); i++) {
            boolean success;
            do {
                SQLQueryAdapter qt = new MutableTableGenerator().getQuery(globalState);
                success = globalState.executeStatement(qt);
            } while (!success);
        }

        if (globalState.getSchema().getDatabaseTables().isEmpty()) {
            throw new IgnoreMeException(); // TODO
        }
        StatementExecutor<MutableGlobalState, Action> se = new StatementExecutor<>(globalState, Action.values(),
                MutableProvider::mapActions, (q) -> {
                    if (globalState.getSchema().getDatabaseTables().isEmpty()) {
                        throw new IgnoreMeException();
                    }
                });
        se.executeStatements();
    }


    @Override
    public SQLConnection createDatabase(MutableGlobalState globalState) throws SQLException {
        // Just for experiments
        if (!globalState.getDbmsSpecificOptions().onlyGen.isEmpty()) {
            TestCaseGenerator g = new TestCaseGenerator(globalState, globalState.getDbmsSpecificOptions().onlyGen);
            g.generate(1000000);
            System.exit(0);
        }
        // Just for experiments
        if (globalState.getDbmsSpecificOptions().measureExprGen > 0) {
            ExprGenMeasurerer.measure(globalState, globalState.getDbmsSpecificOptions().measureExprGen);
            System.exit(0);
        }


    	String binary = globalState.getDbmsSpecificOptions().mutableBinaryPath;
    	String debug = globalState.getDbmsSpecificOptions().debugJDBC ? "debug:" : "";
        String url = "jdbc:mutable:" + debug + binary;
        Connection conn = DriverManager.getConnection(url);

        String databaseName = globalState.getDatabaseName();

        String drop_stmt = "DROP DATABASE IF EXISTS " + databaseName + ";";
        String create_stmt = "CREATE DATABASE " + databaseName + ";";
        String use_stmt = "USE " + databaseName + ";";

        globalState.getState().logStatement(drop_stmt);
        try (Statement s = conn.createStatement()) {
            s.execute(drop_stmt);
        }

        globalState.getState().logStatement(create_stmt);
        try (Statement s = conn.createStatement()) {
            s.execute(create_stmt);
        }

        globalState.getState().logStatement(use_stmt);
        try (Statement s = conn.createStatement()) {
            s.execute(use_stmt);
        }
        
        if (globalState.getDbmsSpecificOptions().debug) {
        	System.out.println(drop_stmt);
        	System.out.println(create_stmt);
        	System.out.println(use_stmt);
        }

        return new SQLConnection(conn);
    }

    @Override
    public String getDBMSName() {
        return "mutable";
    }

}
