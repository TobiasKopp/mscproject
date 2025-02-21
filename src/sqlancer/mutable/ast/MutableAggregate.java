package sqlancer.mutable.ast;

import sqlancer.Randomly;
import sqlancer.common.ast.FunctionNode;
import sqlancer.mutable.MutableSchema.MutableDataType;
import sqlancer.mutable.ast.MutableAggregate.MutableAggregateFunction;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class MutableAggregate extends FunctionNode<MutableAggregateFunction, MutableExpression>
		implements MutableExpression {

	public enum MutableAggregateFunction {
        AVG(MutableDataType.FLOAT, MutableDataType.DOUBLE, MutableDataType.DECIMAL),
        COUNT(MutableDataType.INT),
        SUM(MutableDataType.INT, MutableDataType.FLOAT, MutableDataType.DOUBLE, MutableDataType.DECIMAL),
        MAX(MutableDataType.INT, MutableDataType.FLOAT, MutableDataType.DOUBLE, MutableDataType.DECIMAL),
        MIN(MutableDataType.INT, MutableDataType.FLOAT, MutableDataType.DOUBLE, MutableDataType.DECIMAL);

        private MutableDataType[] supportedReturnTypes;

        MutableAggregateFunction(MutableDataType... supportedReturnTypes) {
            this.supportedReturnTypes = supportedReturnTypes.clone();
        }

        public List<MutableDataType> getTypes(MutableDataType returnType) {
            return Arrays.asList(returnType);
        }

        public boolean supportsReturnType(MutableDataType returnType) {
            return Arrays.asList(supportedReturnTypes).stream().anyMatch(t -> t == returnType)
                    || supportedReturnTypes.length == 0;
        }

        public static List<MutableAggregateFunction> getAggregates(MutableDataType type) {
            return Arrays.asList(values()).stream().filter(p -> p.supportsReturnType(type))
                    .collect(Collectors.toList());
        }

        public MutableDataType getRandomReturnType() {
            if (supportedReturnTypes.length == 0) {
                return Randomly.fromOptions(MutableDataType.values());
            } else {
                return Randomly.fromOptions(supportedReturnTypes);
            }
        }

        public static MutableAggregateFunction getRandom() {
        	return Randomly.fromOptions(values());
        }

    }

    public MutableAggregate(MutableAggregateFunction func, List<MutableExpression> args) {
        super(func, args);
    }
}
