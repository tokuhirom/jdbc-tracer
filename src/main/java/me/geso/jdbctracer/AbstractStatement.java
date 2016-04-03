package me.geso.jdbctracer;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.stream.Collectors;

public abstract class AbstractStatement {
    protected static final Set<String> SET_METHODS = buildSetMethods();
    protected static final Set<String> EXECUTE_METHODS = buildExecuteMethods();
    private final ColumnValues columnValues = new ColumnValues();

    private static Set<String> buildExecuteMethods() {
        Set exec = new HashSet<>();
        exec.add("execute");
        exec.add("executeUpdate");
        exec.add("executeQuery");
        exec.add("addBatch");
        return Collections.unmodifiableSet(exec);
    }

    private static Set<String> buildSetMethods() {
        Set<String> set = new HashSet<>();
        set.add("setString");
        set.add("setNString");
        set.add("setInt");
        set.add("setByte");
        set.add("setShort");
        set.add("setLong");
        set.add("setDouble");
        set.add("setFloat");
        set.add("setTimestamp");
        set.add("setDate");
        set.add("setTime");
        set.add("setArray");
        set.add("setBigDecimal");
        set.add("setAsciiStream");
        set.add("setBinaryStream");
        set.add("setBlob");
        set.add("setBoolean");
        set.add("setBytes");
        set.add("setCharacterStream");
        set.add("setNCharacterStream");
        set.add("setClob");
        set.add("setNClob");
        set.add("setObject");
        set.add("setNull");
        return Collections.unmodifiableSet(set);
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
