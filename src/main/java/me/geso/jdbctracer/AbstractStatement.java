package me.geso.jdbctracer;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by tokuhirom on 4/2/16.
 */
public abstract class AbstractStatement {
    protected static final Set<String> SET_METHODS = new HashSet<>();
    protected static final Set<String> EXECUTE_METHODS = new HashSet<>();
    private final ColumnValues columnValues = new ColumnValues();


    static {
        SET_METHODS.add("setString");
        SET_METHODS.add("setNString");
        SET_METHODS.add("setInt");
        SET_METHODS.add("setByte");
        SET_METHODS.add("setShort");
        SET_METHODS.add("setLong");
        SET_METHODS.add("setDouble");
        SET_METHODS.add("setFloat");
        SET_METHODS.add("setTimestamp");
        SET_METHODS.add("setDate");
        SET_METHODS.add("setTime");
        SET_METHODS.add("setArray");
        SET_METHODS.add("setBigDecimal");
        SET_METHODS.add("setAsciiStream");
        SET_METHODS.add("setBinaryStream");
        SET_METHODS.add("setBlob");
        SET_METHODS.add("setBoolean");
        SET_METHODS.add("setBytes");
        SET_METHODS.add("setCharacterStream");
        SET_METHODS.add("setNCharacterStream");
        SET_METHODS.add("setClob");
        SET_METHODS.add("setNClob");
        SET_METHODS.add("setObject");
        SET_METHODS.add("setNull");

        EXECUTE_METHODS.add("execute");
        EXECUTE_METHODS.add("executeUpdate");
        EXECUTE_METHODS.add("executeQuery");
        EXECUTE_METHODS.add("addBatch");
    }

    private String query;
    private PreparedStatementListener preparedStatementListener;

    public AbstractStatement(String query, PreparedStatementListener preparedStatementListener) {
        this.query = query;
        this.preparedStatementListener = preparedStatementListener;
    }

    protected void setColumn(int pos, Object value) {
        this.columnValues.put(pos, value);
    }

    protected <T> T trace(TraceSupplier<T> supplier) throws InvocationTargetException, IllegalAccessException {
        List<Object> params = this.columnValues.values();
        this.columnValues.clear();
        if (preparedStatementListener != null) {
            long start = System.nanoTime();
            T retval = supplier.get();
            long finished = System.nanoTime();
            preparedStatementListener.trace(finished - start, query, params);
            return retval;
        } else {
            return supplier.get();
        }
    }

    protected interface TraceSupplier<T> {
        T get() throws IllegalAccessException, IllegalArgumentException,
                InvocationTargetException;
    }

}
