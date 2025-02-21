package sqlancer.mutable.ast;

import sqlancer.mutable.MutableSchema.MutableDataType;

import java.sql.Date;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;

// TODO implement DOUBLE / DECIMAL ?
// TODO split CHAR / VARCHAR ?
public abstract class MutableConstant implements MutableExpression {

	public abstract String getTextRepresentation();
	
	@Override
	public String toString() {
	    return getTextRepresentation();
	}

	@Override
    public MutableConstant getExpectedValue() {
        return this;
    }


	/* -------------------------------------------------------------------------------- *
	 * 		GETTERS
	 * -------------------------------------------------------------------------------- */
	public boolean isNullConstant() {
        return false;
    }

	public boolean isBooleanConstant() {
        return this.getExpressionType() == MutableDataType.BOOL;
    }

	public boolean isIntConstant() {
        return this.getExpressionType() == MutableDataType.INT;
    }

	public boolean isFloatingPointConstant() {
        return this.getExpressionType() == MutableDataType.FLOAT
        		|| this.getExpressionType() == MutableDataType.DOUBLE
        		|| this.getExpressionType() == MutableDataType.DECIMAL;
    }

//	public boolean isDoubleConstant() {
//        return this.getExpressionType() == MutableDataType.DOUBLE;
//    }

	public boolean isStringConstant() {
        return this.getExpressionType() == MutableDataType.CHAR;
    }

	public boolean isDateConstant() {
        return this.getExpressionType() == MutableDataType.DATE;
    }

	public boolean isDatetimeConstant() {
        return this.getExpressionType() == MutableDataType.DATETIME;
    }

	public long asInt() {
        throw new UnsupportedOperationException(this.getExpressionType().toString());
    }

    public double asDouble() {
        throw new UnsupportedOperationException(this.getExpressionType().toString());
    }

    public String asString() {
        throw new UnsupportedOperationException(this.getExpressionType().toString());
    }

    public boolean asBoolean() {
        throw new UnsupportedOperationException(this.getExpressionType().toString());
    }

    public String asDate() {
        throw new UnsupportedOperationException(this.getExpressionType().toString());
    }

    public String asDatetime() {
        throw new UnsupportedOperationException(this.getExpressionType().toString());
    }

    // Should only return BooleanConstants! (or NullConstant)
    public MutableConstant isEquals(MutableConstant rightVal) {
    	return null;
    };

    // Should only return BooleanConstants! (or NullConstant)
    protected MutableConstant isLessThan(MutableConstant rightVal) {
    	return null;
    };



    /* -------------------------------------------------------------------------------- *
	 * 		CONSTANTS
	 * -------------------------------------------------------------------------------- */

    public static class MutableNullConstant extends MutableConstant {

        @Override
        public String getTextRepresentation() {
            return "NULL";
        }

        @Override
        public MutableDataType getExpressionType() {
            return null;
        }

        @Override
        public boolean isNullConstant() {
            return true;
        }

        @Override
        public MutableConstant isEquals(MutableConstant rightVal) {
            return MutableConstant.createNullConstant();
        }

        @Override
        protected MutableConstant isLessThan(MutableConstant rightVal) {
            return MutableConstant.createNullConstant();
        }

    }

    public static class MutableBooleanConstant extends MutableConstant {

        private final boolean value;

        public MutableBooleanConstant(boolean value) {
            this.value = value;
        }

        public boolean getValue() {
            return value;
        }

        @Override
        public boolean asBoolean() {
            return value;
        }

        @Override
        public String getTextRepresentation() { return value ? "TRUE" : "FALSE"; }

        @Override
        public MutableDataType getExpressionType() {
            return MutableDataType.BOOL;
        }

        @Override
        public MutableConstant isEquals(MutableConstant rightVal) {
            if (rightVal.isNullConstant()) {
                return MutableConstant.createNullConstant();
            } else if (rightVal.isBooleanConstant()) {
                return MutableConstant.createBooleanConstant(value == rightVal.asBoolean());
            } else {
                throw new AssertionError(rightVal);
            }
        }
    }

    // CHAR + VARCHAR
    public static class MutableStringLiteral extends MutableConstant {

        private final String value;

        public MutableStringLiteral(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        @Override
        public String asString() {
            return value;
        }

        @Override
        public String getTextRepresentation() { return "\"" + value + "\""; }

        @Override
        public MutableDataType getExpressionType() {
            return MutableDataType.CHAR;
        }

        @Override
        public MutableConstant isEquals(MutableConstant rightVal) {
            if (rightVal.isNullConstant()) {
                return MutableConstant.createNullConstant();
            } else if (rightVal.isStringConstant()) {
                return MutableConstant.createBooleanConstant(value.equals(rightVal.asString()));
            } else {
                throw new AssertionError(rightVal);
            }
        }

        @Override
        protected MutableConstant isLessThan(MutableConstant rightVal) {
        	if (rightVal.isNullConstant()) {
                return MutableConstant.createNullConstant();
            } else if (rightVal.isStringConstant()) {
                return MutableConstant.createBooleanConstant(value.compareTo(rightVal.asString()) < 0);
            } else {
                throw new AssertionError(rightVal);
            }
        }

    }

    public static class MutableIntegerConstant extends MutableConstant {

        private final long value;

        public MutableIntegerConstant(long value) {
            this.value = value;
        }

        @Override
        public String getTextRepresentation() {
            return String.valueOf(value);
        }

        public long getValue() {
            return value;
        }

        @Override
        public long asInt() {
            return value;
        }

        @Override
        public double asDouble() {
            return (double) value;
        }

        @Override
        public MutableDataType getExpressionType() {
            return MutableDataType.INT;
        }

        @Override
        public MutableConstant isEquals(MutableConstant rightVal) {
            if (rightVal.isNullConstant()) {
                return MutableConstant.createNullConstant();
            } else if (rightVal.isIntConstant()) {
                return MutableConstant.createBooleanConstant(value == rightVal.asInt());
            } else if (rightVal.isFloatingPointConstant()) {
                return MutableConstant.createBooleanConstant((double) value == rightVal.asDouble());
            } else {
                throw new AssertionError(rightVal);
            }
        }

        @Override
        protected MutableConstant isLessThan(MutableConstant rightVal) {
        	if (rightVal.isNullConstant()) {
                return MutableConstant.createNullConstant();
            } else if (rightVal.isIntConstant()) {
                return MutableConstant.createBooleanConstant(value < rightVal.asInt());
            } else if (rightVal.isFloatingPointConstant()) {
                return MutableConstant.createBooleanConstant((double) value < rightVal.asDouble());
            } else {
                throw new AssertionError(rightVal);
            }
        }

    }

    public static class MutableFloatingPointConstant extends MutableConstant {

        private final double value;

        public MutableFloatingPointConstant(double value) {
            this.value = value;
        }

        public double getValue() {
            return value;
        }

        @Override
        public double asDouble() {
            return value;
        }

        @Override
        public String getTextRepresentation() {
            // TODO inf ?
            if (value == Double.POSITIVE_INFINITY) {
                return "1000.0";
            } else if (value == Double.NEGATIVE_INFINITY) {
                return "-1000.0";
            }
            return String.valueOf(value);
        }

        @Override
        public MutableDataType getExpressionType() {
            return MutableDataType.FLOAT;
        }

        @Override
        public MutableConstant isEquals(MutableConstant rightVal) {
            if (rightVal.isNullConstant()) {
                return MutableConstant.createNullConstant();
            } else if (rightVal.isFloatingPointConstant() || rightVal.isIntConstant()) {
                return MutableConstant.createBooleanConstant(value == rightVal.asDouble());
            } else {
                throw new AssertionError(rightVal);
            }
        }

        @Override
        protected MutableConstant isLessThan(MutableConstant rightVal) {
        	if (rightVal.isNullConstant()) {
                return MutableConstant.createNullConstant();
            } else if (rightVal.isFloatingPointConstant() || rightVal.isIntConstant()) {
                return MutableConstant.createBooleanConstant(value < rightVal.asDouble());
            } else {
                throw new AssertionError(rightVal);
            }
        }

    }

    public static class MutableDateConstant extends MutableConstant {

        public String textRepr;

        public MutableDateConstant(long val) {
            Timestamp timestamp = new Timestamp(val);
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            textRepr = dateFormat.format(timestamp);
        }

        public MutableDateConstant(Date date) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            textRepr = dateFormat.format(date);
        }

        public String getValue() {
            return textRepr;
        }

        @Override
        public String asDate() {
            return textRepr;
        }


        @Override
        public String getTextRepresentation() {
            return String.format("d\'%s\'", textRepr);
        }

        @Override
        public MutableDataType getExpressionType() {
            return MutableDataType.DATE;
        }

        @Override
        public MutableConstant isEquals(MutableConstant rightVal) {
            if (rightVal.isNullConstant()) {
                return MutableConstant.createNullConstant();
            } else if (rightVal.isDateConstant()) {
                return MutableConstant.createBooleanConstant(textRepr.equals(rightVal.asDate()));
            } else {
                throw new AssertionError(rightVal);
            }
        }

        @Override
        protected MutableConstant isLessThan(MutableConstant rightVal) {
        	if (rightVal.isNullConstant()) {
                return MutableConstant.createNullConstant();
            } else if (rightVal.isDateConstant()) {
                return MutableConstant.createBooleanConstant(textRepr.compareTo(rightVal.asDate()) < 0);
            } else {
                throw new AssertionError(rightVal);
            }
        }

    }

    public static class MutableDatetimeConstant extends MutableConstant {

        public String textRepr;

        public MutableDatetimeConstant(long val) {
            Timestamp timestamp = new Timestamp(val);
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            textRepr = dateFormat.format(timestamp);
        }

        public MutableDatetimeConstant(Timestamp t) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            textRepr = dateFormat.format(t);
        }

        public String getValue() {
            return textRepr;
        }

        @Override
        public String asDatetime() {
            return textRepr;
        }

        @Override
        public String getTextRepresentation() {
            return String.format("d\'%s\'", textRepr);
        }

        @Override
        public MutableDataType getExpressionType() {
            return MutableDataType.DATETIME;
        }

        @Override
        public MutableConstant isEquals(MutableConstant rightVal) {
            if (rightVal.isNullConstant()) {
                return MutableConstant.createNullConstant();
            } else if (rightVal.isDatetimeConstant()) {
                return MutableConstant.createBooleanConstant(textRepr.equals(rightVal.asDatetime()));
            } else {
                throw new AssertionError(rightVal);
            }
        }

        @Override
        protected MutableConstant isLessThan(MutableConstant rightVal) {
        	if (rightVal.isNullConstant()) {
                return MutableConstant.createNullConstant();
            } else if (rightVal.isDatetimeConstant()) {
                return MutableConstant.createBooleanConstant(textRepr.compareTo(rightVal.asDatetime()) < 0);
            } else {
                throw new AssertionError(rightVal);
            }
        }

    }


    public static MutableConstant createStringConstant(String text) {
        return new MutableStringLiteral(text);
    }

    public static MutableConstant createFloatingPointConstant(double val) {
    	return new MutableFloatingPointConstant(val);
    }

    public static MutableConstant createIntegerConstant(long val) {
        return new MutableIntegerConstant(val);
    }

    public static MutableConstant createNullConstant() {
        return new MutableNullConstant();
    }

    public static MutableConstant createBooleanConstant(boolean val) {
        return new MutableBooleanConstant(val);
    }

    public static MutableConstant createDateConstant(long integer) {
        return new MutableDateConstant(integer);
    }

    public static MutableConstant createDatetimeConstant(long integer) {
    	return new MutableDatetimeConstant(integer);
    }

}
