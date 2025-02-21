package sqlancer.mutable.ast;

import sqlancer.Randomly;
import sqlancer.mutable.MutableSchema.MutableDataType;

public class MutableFunctionExpression implements MutableExpression {

    private final String func;
    private final MutableFunction function;
    private final MutableExpression[] args;
    private final MutableDataType returnType;

    public MutableFunctionExpression(MutableFunction func, MutableDataType returnType, MutableExpression... args) {
        this.func = func.getName();
        this.function = func;
        this.returnType = returnType;
        this.args = args.clone();
    }

    public String getFunctionName() {
        return func;
    }

    public MutableExpression[] getArguments() {
        return args.clone();
    }

    public enum MutableFunction {
    	// Currently, only the function `ISNULL(...)` is supported in mutable
        ISNULL(1, "ISNULL") {
            @Override
            public MutableConstant apply(MutableConstant[] evaluatedArgs, MutableExpression... args) {
            	return MutableConstant.createBooleanConstant(evaluatedArgs[0].isNullConstant());
            }

            @Override
            public boolean supportsReturnType(MutableDataType type) {
                return type == MutableDataType.BOOL;
            }

//            @Override
//            public MutableDataType[] getInputTypesForReturnType(MutableDataType returnType, int nrArguments) {
//                return new MutableDataType[] { returnType };
//            }

        };

        private String functionName;
        final int nrArgs;
        private final boolean variadic;

        public MutableDataType[] getRandomTypes(int nr) {
            MutableDataType[] types = new MutableDataType[nr];
            for (int i = 0; i < types.length; i++) {
                types[i] = Randomly.fromOptions(MutableDataType.values());
            }
            return types;
        }

        MutableFunction(int nrArgs, String functionName) {
            this.nrArgs = nrArgs;
            this.functionName = functionName;
            this.variadic = false;
        }

        /**
         * Gets the number of arguments if the function is non-variadic. If the function is variadic, the minimum number
         * of arguments is returned.
         *
         * @return the number of arguments
         */
        public int getNrArgs() {
            return nrArgs;
        }

        public abstract MutableConstant apply(MutableConstant[] evaluatedArgs, MutableExpression... args);

        @Override
        public String toString() {
            return functionName;
        }

        public boolean isVariadic() {
            return variadic;
        }

        public String getName() {
            return functionName;
        }

        public abstract boolean supportsReturnType(MutableDataType type);

//        public abstract MutableDataType[] getInputTypesForReturnType(MutableDataType returnType, int nrArguments);
//
//        public boolean checkArguments(MutableExpression... constants) {
//            return true;
//        }

    }

    @Override
    public MutableConstant getExpectedValue() {
        if (function == null) {
            return null;
        }
        MutableConstant[] constants = new MutableConstant[args.length];
        for (int i = 0; i < constants.length; i++) {
            constants[i] = args[i].getExpectedValue();
            if (constants[i] == null) {
                return null;
            }
        }
        return function.apply(constants, args);
    }

    @Override
    public MutableDataType getExpressionType() {
        return returnType;
    }

}
