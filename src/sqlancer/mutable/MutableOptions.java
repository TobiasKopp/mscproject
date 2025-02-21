package sqlancer.mutable;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import sqlancer.DBMSSpecificOptions;
import sqlancer.OracleFactory;
import sqlancer.common.oracle.CompositeTestOracle;
import sqlancer.common.oracle.TestOracle;
import sqlancer.mutable.MutableOptions.MutableOracleFactory;
import sqlancer.mutable.MutableProvider.MutableGlobalState;
import sqlancer.mutable.oracle.MutablePQSOracle;
import sqlancer.mutable.oracle.norec.MutableNoRECOracle;
import sqlancer.mutable.oracle.norec.MutableNoRECPlusOracle;
import sqlancer.mutable.oracle.tlp.MutableTLPAggregateOracle;
import sqlancer.mutable.oracle.tlp.MutableTLPGroupByOracle;
import sqlancer.mutable.oracle.tlp.MutableTLPHavingOracle;
import sqlancer.mutable.oracle.tlp.MutableTLPWhereOracle;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Parameters(commandDescription = "Mutable")
public class MutableOptions implements DBMSSpecificOptions<MutableOracleFactory> {

	@Parameter(names = "--binary", description = "Path to the mutable binary. Default /mutable/build/debug/bin/shell", arity = 1)
    public String mutableBinaryPath = "/mutable/build/debug/bin/shell";

	@Parameter(names = "--debug", description = "Print debug statements to console")
    public boolean debug = false;

	@Parameter(names = "--debugJDBC", description = "Print debug statements of JDBC driver to console")
    public boolean debugJDBC = false;

    @Parameter(names = "--onlyGenerateQueries", description = "Only generate queries without executing them.")
    public String onlyGen = "";
    
    @Parameter(names = "--measureExprGen", description = "Measure the time it takes for the expression generator to generate queries", arity = 1)
    public int measureExprGen = -1;

    @Parameter(names = "--test-default-values", description = "Allow generating DEFAULT values in tables", arity = 1)
    public boolean testDefaultValues = true;

    @Parameter(names = "--test-not-null", description = "Allow generating NOT NULL constraints in tables", arity = 1)
    public boolean testNotNullConstraints = true;

    @Parameter(names = "--test-unique", description = "Allow generating UNIQUE constraints in tables", arity = 1)
    public boolean testUniqueConstraints = true;

    @Parameter(names = "--test-primary-key", description = "Allow generating PRIMARY KEY constraints in tables", arity = 1)
    public boolean testPrimaryKeyConstraints = true;

    @Parameter(names = "--test-boolean-constants", description = "Allow generating BOOL constants", arity = 1)
    public boolean testBooleanConstants = true;

    @Parameter(names = "--test-integer-constants", description = "Allow generating INTEGER constants", arity = 1)
    public boolean testIntegerConstants = true;

    @Parameter(names = "--test-string-constants", description = "Allow generating VARCHAR and CHAR constants", arity = 1)
    public boolean testStringConstants = true;

    @Parameter(names = "--test-float-constants", description = "Allow generating FLOAT constants", arity = 1)
    public boolean testFloatConstants = true;

    @Parameter(names = "--test-double-constants", description = "Allow generating DOUBLE constants", arity = 1)
    public boolean testDoubleConstants = true;

    @Parameter(names = "--test-decimal-constants", description = "Allow generating DECIMAL constants", arity = 1)
    public boolean testDecimalConstants = true;

    @Parameter(names = "--test-date-constants", description = "Allow generating DATE constants", arity = 1)
    public boolean testDateConstants = true;

    @Parameter(names = "--test-datetime-constants", description = "Allow generating DATETIME constants", arity = 1)
    public boolean testDatetimeConstants = true;

    @Parameter(names = "--max-num-deletes", description = "The maximum number of DELETE statements that are issued for a database", arity = 1)
    public int maxNumDeletes = 1;

    @Parameter(names = "--max-num-updates", description = "The maximum number of UPDATE statements that are issued for a database", arity = 1)
    public int maxNumUpdates = 5;

    @Parameter(names = "--oracle")
    public List<MutableOracleFactory> oracles = List.of(MutableOracleFactory.NOREC);


    public enum MutableOracleFactory implements OracleFactory<MutableGlobalState> {
        NOREC {
            @Override
            public TestOracle<MutableGlobalState> create(MutableGlobalState globalState) throws SQLException {
                return new MutableNoRECOracle(globalState);
            }
        },
        NORECPLUS {
            @Override
            public TestOracle<MutableGlobalState> create(MutableGlobalState globalState) throws SQLException {
                return new MutableNoRECPlusOracle(globalState);
            }
        },

        WHERE {
	        @Override
	        public TestOracle<MutableGlobalState> create(MutableGlobalState globalState) throws SQLException {
	            return new MutableTLPWhereOracle(globalState);
	        }
        },
        GROUP_BY {
            @Override
            public TestOracle<MutableGlobalState> create(MutableGlobalState globalState) throws SQLException {
                return new MutableTLPGroupByOracle(globalState);
            }
        },
        HAVING {
            @Override
            public TestOracle<MutableGlobalState> create(MutableGlobalState globalState) throws SQLException {
                return new MutableTLPHavingOracle(globalState);
            }
        },
        AGGREGATE {
            @Override
            public TestOracle<MutableGlobalState> create(MutableGlobalState globalState) throws SQLException {
                return new MutableTLPAggregateOracle(globalState);
            }
        },
        TLP {
            @Override
            public TestOracle<MutableGlobalState> create(MutableGlobalState globalState) throws SQLException {
                List<TestOracle<MutableGlobalState>> oracles = new ArrayList<>();
                oracles.add(new MutableTLPWhereOracle(globalState));
                oracles.add(new MutableTLPGroupByOracle(globalState));
                oracles.add(new MutableTLPHavingOracle(globalState));
                oracles.add(new MutableTLPAggregateOracle(globalState));
                return new CompositeTestOracle<MutableGlobalState>(oracles, globalState);
            }
        },

        PQS {
        	@Override
            public TestOracle<MutableGlobalState> create(MutableGlobalState globalState) throws SQLException {
                return new MutablePQSOracle(globalState);
            }
        },

        ALL {
        	@Override
            public TestOracle<MutableGlobalState> create(MutableGlobalState globalState) throws SQLException {
                List<TestOracle<MutableGlobalState>> oracles = new ArrayList<>();
                oracles.add(new MutableNoRECOracle(globalState));
                oracles.add(new MutableTLPWhereOracle(globalState));
                oracles.add(new MutableTLPGroupByOracle(globalState));
                oracles.add(new MutableTLPHavingOracle(globalState));
                oracles.add(new MutableTLPAggregateOracle(globalState));
                oracles.add(new MutablePQSOracle(globalState));
                return new CompositeTestOracle<MutableGlobalState>(oracles, globalState);
            }
        };

    }

    @Override
    public List<MutableOracleFactory> getTestOracleFactory() {
        return oracles;
    }

}
