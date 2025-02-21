package sqlancer.mutable.jdbc;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Date;
import java.sql.*;
import java.util.*;


public class MutableJDBCResultSet implements ResultSet {

	/*******************************************************************************
     * FIELDS
     *******************************************************************************/

	private String SEPARATOR = ",";				    // Separator between the values of a row
    private Iterator<String> iterator;				// Iterator over the rows
    private List<String> row = null;				// Current row
    private boolean wasNull = false;				// Whether the last column read had a value of SQL {@code NULL}.
    private int nRows;								// Total number of rows of this result set
    private boolean isClosed = false;				// Whether this ResultSet is closed
    private List<String> columnLabels;				// Column labels of this ResultSet
    private MutableJDBCAbstractStatement statement;	// The statement that produced this ResultSet



    /*******************************************************************************
     * CONSTRUCTORS & GETTERS
     *******************************************************************************/

    public MutableJDBCResultSet(MutableJDBCAbstractStatement statement, List<String> rows, List<String> columnLabels) {
        iterator = rows.iterator();
        this.columnLabels = columnLabels;
        this.statement = statement;
        this.nRows = rows.size();
    }

    public MutableJDBCResultSet(MutableJDBCAbstractStatement statement, List<String> rows) {
        iterator = rows.iterator();
        this.statement = statement;
        this.nRows = rows.size();
    }

    //public MutableJDBCResultSet() {}

    public int getNRows() {
    	return nRows;
    }
    
    public void setSeparator(String sep) {
    	this.SEPARATOR = sep;
    }



    /*******************************************************************************
     * VALUEPARSERS
     *******************************************************************************/

    private abstract class ValueParser<T> {
    	public abstract T parse(String s) throws SQLException;
    	public abstract T onSQL_NULL(String s);

        public T parseValueOfColumn(int columnIndex) throws SQLException {
        	if (row == null) {
        		throw new SQLException("ResultSet is not pointing to a row. If this is the first access, call `next()` before.");
        	}
        	if (columnIndex == 0) {
        		throw new SQLException("Accessing placeholder (columnIndex 0). The first element is at columndIndex 1.");
        	}
        	try {
        		return parseValue(row.get(columnIndex));
        	} catch (IndexOutOfBoundsException e) {
        		throw new SQLException(e.getMessage());
        	}
        }

        public T parseValueOfColumn(String columnLabel) throws SQLException {
        	if (row == null) {
        		throw new SQLException("ResultSet is not pointing to a row. If this is the first access, call `next()` before.");
        	}
        	if (columnLabels == null) {
        		throw new SQLException("Unable to find columnLabel.");
        	}
    		int idx = columnLabels.indexOf(columnLabel);
    		if (idx < 0) {
        		throw new SQLException("Unable to find columnLabel.");
    		}
    		return parseValue(row.get(idx));
        }

        public T parseValue(String str) throws SQLException {
        	if (isClosed) throw new SQLException("ResultSet already closed.");

    	    if (str.equals("NULL")) {
    	    	wasNull = true;
    	    	return onSQL_NULL(str);
    	    } else {
    	    	wasNull = false;
    	    	return parse(str);
    	    }
        }
    }

    private class StringParser extends ValueParser<String> {
		@Override
		public String parse(String s) { return s; }
		@Override
		public String onSQL_NULL(String s) { return null; }
    }

    private class BooleanParser extends ValueParser<Boolean> {
		@Override
		public Boolean parse(String s) { return Boolean.parseBoolean(s); }
		@Override
		public Boolean onSQL_NULL(String s) { return false; }
    }

    private class ByteParser extends ValueParser<Byte> {
		@Override
		public Byte parse(String s) { return Byte.parseByte(s); }
		@Override
		public Byte onSQL_NULL(String s) { return 0; }
    }

    private class ByteArrayParser extends ValueParser<byte[]> {
		@Override
		public byte[] parse(String s) { return s.getBytes(); }
		@Override
		public byte[] onSQL_NULL(String s) { return null; }
    }

    private class ShortParser extends ValueParser<Short> {
		@Override
		public Short parse(String s) { return Short.parseShort(s); }
		@Override
		public Short onSQL_NULL(String s) { return 0; }
    }

    private class IntParser extends ValueParser<Integer> {
		@Override
		public Integer parse(String s) { return Integer.parseInt(s); }
		@Override
		public Integer onSQL_NULL(String s) { return 0; }
    }

    private class LongParser extends ValueParser<Long> {
		@Override
		public Long parse(String s) { return Long.parseLong(s); }
		@Override
		public Long onSQL_NULL(String s) { return (long) 0; }
    }

    private class FloatParser extends ValueParser<Float> {
		@Override
		public Float parse(String s) { return Float.parseFloat(s); }
		@Override
		public Float onSQL_NULL(String s) { return (float) 0; }
    }

    private class DoubleParser extends ValueParser<Double> {
		@Override
		public Double parse(String s) { return Double.parseDouble(s); }
		@Override
		public Double onSQL_NULL(String s) { return (double) 0; }
    }

    private class DateParser extends ValueParser<Date> {
		@Override
		public Date parse(String s) throws SQLException {
			// Check whether string has format d' ... '
			if (s.startsWith("d'") && s.endsWith("'")) {
				StringBuilder sb = new StringBuilder(s);
				sb.deleteCharAt(s.length() - 1);
				sb.deleteCharAt(1);
				sb.deleteCharAt(0);
				s = sb.toString();
			}
			try {
				return Date.valueOf(s);
			} catch (IllegalArgumentException e) {
				throw new SQLException("Unable to parse date: " + s);
			}
		}
		@Override
		public Date onSQL_NULL(String s) { return null; }
    }

    private class TimestampParser extends ValueParser<Timestamp> {
		@Override
		public Timestamp parse(String s) throws SQLException  {
			// Check whether string has format d' ... '
			if (s.startsWith("d'") && s.endsWith("'")) {
				StringBuilder sb = new StringBuilder(s);
				sb.deleteCharAt(s.length() - 1);
				sb.deleteCharAt(1);
				sb.deleteCharAt(0);
				s = sb.toString();
			}
			try {
				return Timestamp.valueOf(s);
			} catch (IllegalArgumentException e) {
				throw new SQLException("Unable to parse date: " + s);
			}
		}
		@Override
		public Timestamp onSQL_NULL(String s) { return null; }
    }



    /*******************************************************************************
     * OVERRIDES
     *******************************************************************************/

	@Override
	public <T> T unwrap(Class<T> iface) throws SQLException {
		throw new UnsupportedOperationException();
	}
	@Override
	public boolean isWrapperFor(Class<?> iface) throws SQLException {
		throw new UnsupportedOperationException();
	}


    @Override
    public boolean next() throws SQLException {
        boolean retVal = iterator.hasNext();
        row = null;

        if (iterator.hasNext()) {
        	row = new ArrayList<String>();
        	row.add("placeholder");				 	// Insert a placeholder at index 0 because
													// "columnIndex: the first column is 1, the second is 2, ..."
        	row.addAll(List.of(iterator.next().split(SEPARATOR)));
        	for (int i=0; i<row.size(); i++) {
        	    row.set(i, row.get(i).strip());
        	}
        }

        return retVal;
    }
	@Override
	public void close() throws SQLException {
		isClosed = true;
	}
	@Override
	public boolean isClosed() throws SQLException {
		return isClosed;
	}
	@Override
	public boolean wasNull() throws SQLException {
		return wasNull;
	}

	@Override
	public Statement getStatement() throws SQLException {
		return this.statement;
	}



	// Methods for accessing results by column index

	@Override
	public String getString(int columnIndex) throws SQLException {
		return new StringParser().parseValueOfColumn(columnIndex);
	}
	@Override
	public boolean getBoolean(int columnIndex) throws SQLException {
		return new BooleanParser().parseValueOfColumn(columnIndex);
	}
	@Override
	public byte getByte(int columnIndex) throws SQLException {
		return new ByteParser().parseValueOfColumn(columnIndex);
	}
	@Override
	public short getShort(int columnIndex) throws SQLException {
		return new ShortParser().parseValueOfColumn(columnIndex);
	}
	@Override
	public int getInt(int columnIndex) throws SQLException {
		return new IntParser().parseValueOfColumn(columnIndex);
	}
	@Override
	public long getLong(int columnIndex) throws SQLException {
		return new LongParser().parseValueOfColumn(columnIndex);
	}
	@Override
	public float getFloat(int columnIndex) throws SQLException {
		return new FloatParser().parseValueOfColumn(columnIndex);
	}
	@Override
	public double getDouble(int columnIndex) throws SQLException {
		return new DoubleParser().parseValueOfColumn(columnIndex);
	}
	@Override
	@Deprecated
	public BigDecimal getBigDecimal(int columnIndex, int scale) throws SQLException {
		return null;
	}
	@Override
	public byte[] getBytes(int columnIndex) throws SQLException {
		return new ByteArrayParser().parseValueOfColumn(columnIndex);
	}
	@Override
	public Date getDate(int columnIndex) throws SQLException {
		return new DateParser().parseValueOfColumn(columnIndex);
	}
	@Override
	public Time getTime(int columnIndex) throws SQLException {
		throw new SQLException("mutable does not support datatype TIME.");
	}
	@Override
	public Timestamp getTimestamp(int columnIndex) throws SQLException {
		return new TimestampParser().parseValueOfColumn(columnIndex);
	}
	@Override
	public InputStream getAsciiStream(int columnIndex) throws SQLException {
		throw new UnsupportedOperationException();
	}
	@Override
	@Deprecated
	public InputStream getUnicodeStream(int columnIndex) throws SQLException {
		return null;
	}
	@Override
	public InputStream getBinaryStream(int columnIndex) throws SQLException {
		throw new UnsupportedOperationException();
	}



	// Methods for accessing results by column label

	@Override
	public String getString(String columnLabel) throws SQLException {
		return new StringParser().parseValueOfColumn(columnLabel);
	}
	@Override
	public boolean getBoolean(String columnLabel) throws SQLException {
		return new BooleanParser().parseValueOfColumn(columnLabel);
	}
	@Override
	public byte getByte(String columnLabel) throws SQLException {
		return new ByteParser().parseValueOfColumn(columnLabel);
	}
	@Override
	public short getShort(String columnLabel) throws SQLException {
		return new ShortParser().parseValueOfColumn(columnLabel);
	}
	@Override
	public int getInt(String columnLabel) throws SQLException {
		return new IntParser().parseValueOfColumn(columnLabel);
	}
	@Override
	public long getLong(String columnLabel) throws SQLException {
		return new LongParser().parseValueOfColumn(columnLabel);
	}
	@Override
	public float getFloat(String columnLabel) throws SQLException {
		return new FloatParser().parseValueOfColumn(columnLabel);
	}
	@Override
	public double getDouble(String columnLabel) throws SQLException {
		return new DoubleParser().parseValueOfColumn(columnLabel);
	}
	@Override
	@Deprecated
	public BigDecimal getBigDecimal(String columnLabel, int scale) throws SQLException {
		return null;
	}
	@Override
	public byte[] getBytes(String columnLabel) throws SQLException {
		return new ByteArrayParser().parseValueOfColumn(columnLabel);
	}
	@Override
	public Date getDate(String columnLabel) throws SQLException {
		return new DateParser().parseValueOfColumn(columnLabel);
	}
	@Override
	public Time getTime(String columnLabel) throws SQLException {
		throw new SQLException("mutable does not support datatype TIME.");
	}
	@Override
	public Timestamp getTimestamp(String columnLabel) throws SQLException {
		return new TimestampParser().parseValueOfColumn(columnLabel);
	}
	@Override
	public InputStream getAsciiStream(String columnLabel) throws SQLException {
		throw new UnsupportedOperationException();
	}
	@Override
	@Deprecated
	public InputStream getUnicodeStream(String columnLabel) throws SQLException {
		return null;
	}
	@Override
	public InputStream getBinaryStream(String columnLabel) throws SQLException {
		throw new UnsupportedOperationException();
	}


	// Everything below is unimplemented

	// Advanced features

	@Override
	public SQLWarning getWarnings() throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public void clearWarnings() throws SQLException {
		// TODO Auto-generated method stub

	}
	@Override
	public String getCursorName() throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public ResultSetMetaData getMetaData() throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public Object getObject(int columnIndex) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public Object getObject(String columnLabel) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public int findColumn(String columnLabel) throws SQLException {
		// TODO Auto-generated method stub
		return 0;
	}
	@Override
	public Reader getCharacterStream(int columnIndex) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public Reader getCharacterStream(String columnLabel) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public BigDecimal getBigDecimal(int columnIndex) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public BigDecimal getBigDecimal(String columnLabel) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public boolean isBeforeFirst() throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}
	@Override
	public boolean isAfterLast() throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}
	@Override
	public boolean isFirst() throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}
	@Override
	public boolean isLast() throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}
	@Override
	public void beforeFirst() throws SQLException {
		// TODO Auto-generated method stub

	}
	@Override
	public void afterLast() throws SQLException {
		// TODO Auto-generated method stub

	}
	@Override
	public boolean first() throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}
	@Override
	public boolean last() throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}
	@Override
	public int getRow() throws SQLException {
		// TODO Auto-generated method stub
		return 0;
	}
	@Override
	public boolean absolute(int row) throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}
	@Override
	public boolean relative(int rows) throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}
	@Override
	public boolean previous() throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}
	@Override
	public void setFetchDirection(int direction) throws SQLException {
		// TODO Auto-generated method stub

	}
	@Override
	public int getFetchDirection() throws SQLException {
		// TODO Auto-generated method stub
		return 0;
	}
	@Override
	public void setFetchSize(int rows) throws SQLException {
		// TODO Auto-generated method stub

	}
	@Override
	public int getFetchSize() throws SQLException {
		// TODO Auto-generated method stub
		return 0;
	}
	@Override
	public int getType() throws SQLException {
		// TODO Auto-generated method stub
		return 0;
	}
	@Override
	public int getConcurrency() throws SQLException {
		// TODO Auto-generated method stub
		return 0;
	}
	@Override
	public boolean rowUpdated() throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}
	@Override
	public boolean rowInserted() throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}
	@Override
	public boolean rowDeleted() throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}
	@Override
	public void updateNull(int columnIndex) throws SQLException {
		// TODO Auto-generated method stub

	}
	@Override
	public void updateBoolean(int columnIndex, boolean x) throws SQLException {
		// TODO Auto-generated method stub

	}
	@Override
	public void updateByte(int columnIndex, byte x) throws SQLException {
		// TODO Auto-generated method stub

	}
	@Override
	public void updateShort(int columnIndex, short x) throws SQLException {
		// TODO Auto-generated method stub

	}
	@Override
	public void updateInt(int columnIndex, int x) throws SQLException {
		// TODO Auto-generated method stub

	}
	@Override
	public void updateLong(int columnIndex, long x) throws SQLException {
		// TODO Auto-generated method stub

	}
	@Override
	public void updateFloat(int columnIndex, float x) throws SQLException {
		// TODO Auto-generated method stub

	}
	@Override
	public void updateDouble(int columnIndex, double x) throws SQLException {
		// TODO Auto-generated method stub

	}
	@Override
	public void updateBigDecimal(int columnIndex, BigDecimal x) throws SQLException {
		// TODO Auto-generated method stub

	}
	@Override
	public void updateString(int columnIndex, String x) throws SQLException {
		// TODO Auto-generated method stub

	}
	@Override
	public void updateBytes(int columnIndex, byte[] x) throws SQLException {
		// TODO Auto-generated method stub

	}
	@Override
	public void updateDate(int columnIndex, Date x) throws SQLException {
		// TODO Auto-generated method stub

	}
	@Override
	public void updateTime(int columnIndex, Time x) throws SQLException {
		// TODO Auto-generated method stub

	}
	@Override
	public void updateTimestamp(int columnIndex, Timestamp x) throws SQLException {
		// TODO Auto-generated method stub

	}
	@Override
	public void updateAsciiStream(int columnIndex, InputStream x, int length) throws SQLException {
		// TODO Auto-generated method stub

	}
	@Override
	public void updateBinaryStream(int columnIndex, InputStream x, int length) throws SQLException {
		// TODO Auto-generated method stub

	}
	@Override
	public void updateCharacterStream(int columnIndex, Reader x, int length) throws SQLException {
		// TODO Auto-generated method stub

	}
	@Override
	public void updateObject(int columnIndex, Object x, int scaleOrLength) throws SQLException {
		// TODO Auto-generated method stub

	}
	@Override
	public void updateObject(int columnIndex, Object x) throws SQLException {
		// TODO Auto-generated method stub

	}
	@Override
	public void updateNull(String columnLabel) throws SQLException {
		// TODO Auto-generated method stub

	}
	@Override
	public void updateBoolean(String columnLabel, boolean x) throws SQLException {
		// TODO Auto-generated method stub

	}
	@Override
	public void updateByte(String columnLabel, byte x) throws SQLException {
		// TODO Auto-generated method stub

	}
	@Override
	public void updateShort(String columnLabel, short x) throws SQLException {
		// TODO Auto-generated method stub

	}
	@Override
	public void updateInt(String columnLabel, int x) throws SQLException {
		// TODO Auto-generated method stub

	}
	@Override
	public void updateLong(String columnLabel, long x) throws SQLException {
		// TODO Auto-generated method stub

	}
	@Override
	public void updateFloat(String columnLabel, float x) throws SQLException {
		// TODO Auto-generated method stub

	}
	@Override
	public void updateDouble(String columnLabel, double x) throws SQLException {
		// TODO Auto-generated method stub

	}
	@Override
	public void updateBigDecimal(String columnLabel, BigDecimal x) throws SQLException {
		// TODO Auto-generated method stub

	}
	@Override
	public void updateString(String columnLabel, String x) throws SQLException {
		// TODO Auto-generated method stub

	}
	@Override
	public void updateBytes(String columnLabel, byte[] x) throws SQLException {
		// TODO Auto-generated method stub

	}
	@Override
	public void updateDate(String columnLabel, Date x) throws SQLException {
		// TODO Auto-generated method stub

	}
	@Override
	public void updateTime(String columnLabel, Time x) throws SQLException {
		// TODO Auto-generated method stub

	}
	@Override
	public void updateTimestamp(String columnLabel, Timestamp x) throws SQLException {
		// TODO Auto-generated method stub

	}
	@Override
	public void updateAsciiStream(String columnLabel, InputStream x, int length) throws SQLException {
		// TODO Auto-generated method stub

	}
	@Override
	public void updateBinaryStream(String columnLabel, InputStream x, int length) throws SQLException {
		// TODO Auto-generated method stub

	}
	@Override
	public void updateCharacterStream(String columnLabel, Reader reader, int length) throws SQLException {
		// TODO Auto-generated method stub

	}
	@Override
	public void updateObject(String columnLabel, Object x, int scaleOrLength) throws SQLException {
		// TODO Auto-generated method stub

	}
	@Override
	public void updateObject(String columnLabel, Object x) throws SQLException {
		// TODO Auto-generated method stub

	}
	@Override
	public void insertRow() throws SQLException {
		// TODO Auto-generated method stub

	}
	@Override
	public void updateRow() throws SQLException {
		// TODO Auto-generated method stub

	}
	@Override
	public void deleteRow() throws SQLException {
		// TODO Auto-generated method stub

	}
	@Override
	public void refreshRow() throws SQLException {
		// TODO Auto-generated method stub

	}
	@Override
	public void cancelRowUpdates() throws SQLException {
		// TODO Auto-generated method stub

	}
	@Override
	public void moveToInsertRow() throws SQLException {
		// TODO Auto-generated method stub

	}
	@Override
	public void moveToCurrentRow() throws SQLException {
		// TODO Auto-generated method stub

	}

	@Override
	public Object getObject(int columnIndex, Map<String, Class<?>> map) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public Ref getRef(int columnIndex) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public Blob getBlob(int columnIndex) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public Clob getClob(int columnIndex) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public Array getArray(int columnIndex) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public Object getObject(String columnLabel, Map<String, Class<?>> map) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public Ref getRef(String columnLabel) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public Blob getBlob(String columnLabel) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public Clob getClob(String columnLabel) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public Array getArray(String columnLabel) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public Date getDate(int columnIndex, Calendar cal) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public Date getDate(String columnLabel, Calendar cal) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public Time getTime(int columnIndex, Calendar cal) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public Time getTime(String columnLabel, Calendar cal) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public Timestamp getTimestamp(int columnIndex, Calendar cal) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public Timestamp getTimestamp(String columnLabel, Calendar cal) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public URL getURL(int columnIndex) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public URL getURL(String columnLabel) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public void updateRef(int columnIndex, Ref x) throws SQLException {
		// TODO Auto-generated method stub

	}
	@Override
	public void updateRef(String columnLabel, Ref x) throws SQLException {
		// TODO Auto-generated method stub

	}
	@Override
	public void updateBlob(int columnIndex, Blob x) throws SQLException {
		// TODO Auto-generated method stub

	}
	@Override
	public void updateBlob(String columnLabel, Blob x) throws SQLException {
		// TODO Auto-generated method stub

	}
	@Override
	public void updateClob(int columnIndex, Clob x) throws SQLException {
		// TODO Auto-generated method stub

	}
	@Override
	public void updateClob(String columnLabel, Clob x) throws SQLException {
		// TODO Auto-generated method stub

	}
	@Override
	public void updateArray(int columnIndex, Array x) throws SQLException {
		// TODO Auto-generated method stub

	}
	@Override
	public void updateArray(String columnLabel, Array x) throws SQLException {
		// TODO Auto-generated method stub

	}
	@Override
	public RowId getRowId(int columnIndex) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public RowId getRowId(String columnLabel) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public void updateRowId(int columnIndex, RowId x) throws SQLException {
		// TODO Auto-generated method stub

	}
	@Override
	public void updateRowId(String columnLabel, RowId x) throws SQLException {
		// TODO Auto-generated method stub

	}
	@Override
	public int getHoldability() throws SQLException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void updateNString(int columnIndex, String nString) throws SQLException {
		// TODO Auto-generated method stub

	}
	@Override
	public void updateNString(String columnLabel, String nString) throws SQLException {
		// TODO Auto-generated method stub

	}
	@Override
	public void updateNClob(int columnIndex, NClob nClob) throws SQLException {
		// TODO Auto-generated method stub

	}
	@Override
	public void updateNClob(String columnLabel, NClob nClob) throws SQLException {
		// TODO Auto-generated method stub

	}
	@Override
	public NClob getNClob(int columnIndex) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public NClob getNClob(String columnLabel) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public SQLXML getSQLXML(int columnIndex) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public SQLXML getSQLXML(String columnLabel) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public void updateSQLXML(int columnIndex, SQLXML xmlObject) throws SQLException {
		// TODO Auto-generated method stub

	}
	@Override
	public void updateSQLXML(String columnLabel, SQLXML xmlObject) throws SQLException {
		// TODO Auto-generated method stub

	}
	@Override
	public String getNString(int columnIndex) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public String getNString(String columnLabel) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public Reader getNCharacterStream(int columnIndex) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public Reader getNCharacterStream(String columnLabel) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public void updateNCharacterStream(int columnIndex, Reader x, long length) throws SQLException {
		// TODO Auto-generated method stub

	}
	@Override
	public void updateNCharacterStream(String columnLabel, Reader reader, long length) throws SQLException {
		// TODO Auto-generated method stub

	}
	@Override
	public void updateAsciiStream(int columnIndex, InputStream x, long length) throws SQLException {
		// TODO Auto-generated method stub

	}
	@Override
	public void updateBinaryStream(int columnIndex, InputStream x, long length) throws SQLException {
		// TODO Auto-generated method stub

	}
	@Override
	public void updateCharacterStream(int columnIndex, Reader x, long length) throws SQLException {
		// TODO Auto-generated method stub

	}
	@Override
	public void updateAsciiStream(String columnLabel, InputStream x, long length) throws SQLException {
		// TODO Auto-generated method stub

	}
	@Override
	public void updateBinaryStream(String columnLabel, InputStream x, long length) throws SQLException {
		// TODO Auto-generated method stub

	}
	@Override
	public void updateCharacterStream(String columnLabel, Reader reader, long length) throws SQLException {
		// TODO Auto-generated method stub

	}
	@Override
	public void updateBlob(int columnIndex, InputStream inputStream, long length) throws SQLException {
		// TODO Auto-generated method stub

	}
	@Override
	public void updateBlob(String columnLabel, InputStream inputStream, long length) throws SQLException {
		// TODO Auto-generated method stub

	}
	@Override
	public void updateClob(int columnIndex, Reader reader, long length) throws SQLException {
		// TODO Auto-generated method stub

	}
	@Override
	public void updateClob(String columnLabel, Reader reader, long length) throws SQLException {
		// TODO Auto-generated method stub

	}
	@Override
	public void updateNClob(int columnIndex, Reader reader, long length) throws SQLException {
		// TODO Auto-generated method stub

	}
	@Override
	public void updateNClob(String columnLabel, Reader reader, long length) throws SQLException {
		// TODO Auto-generated method stub

	}
	@Override
	public void updateNCharacterStream(int columnIndex, Reader x) throws SQLException {
		// TODO Auto-generated method stub

	}
	@Override
	public void updateNCharacterStream(String columnLabel, Reader reader) throws SQLException {
		// TODO Auto-generated method stub

	}
	@Override
	public void updateAsciiStream(int columnIndex, InputStream x) throws SQLException {
		// TODO Auto-generated method stub

	}
	@Override
	public void updateBinaryStream(int columnIndex, InputStream x) throws SQLException {
		// TODO Auto-generated method stub

	}
	@Override
	public void updateCharacterStream(int columnIndex, Reader x) throws SQLException {
		// TODO Auto-generated method stub

	}
	@Override
	public void updateAsciiStream(String columnLabel, InputStream x) throws SQLException {
		// TODO Auto-generated method stub

	}
	@Override
	public void updateBinaryStream(String columnLabel, InputStream x) throws SQLException {
		// TODO Auto-generated method stub

	}
	@Override
	public void updateCharacterStream(String columnLabel, Reader reader) throws SQLException {
		// TODO Auto-generated method stub

	}
	@Override
	public void updateBlob(int columnIndex, InputStream inputStream) throws SQLException {
		// TODO Auto-generated method stub

	}
	@Override
	public void updateBlob(String columnLabel, InputStream inputStream) throws SQLException {
		// TODO Auto-generated method stub

	}
	@Override
	public void updateClob(int columnIndex, Reader reader) throws SQLException {
		// TODO Auto-generated method stub

	}
	@Override
	public void updateClob(String columnLabel, Reader reader) throws SQLException {
		// TODO Auto-generated method stub

	}
	@Override
	public void updateNClob(int columnIndex, Reader reader) throws SQLException {
		// TODO Auto-generated method stub

	}
	@Override
	public void updateNClob(String columnLabel, Reader reader) throws SQLException {
		// TODO Auto-generated method stub

	}
	@Override
	public <T> T getObject(int columnIndex, Class<T> type) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public <T> T getObject(String columnLabel, Class<T> type) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

}
