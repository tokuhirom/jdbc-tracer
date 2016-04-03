package me.geso.jdbctracer;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.stream.Collectors;

abstract class AbstractStatement {
    protected static final Set<String> EXECUTE_METHODS = buildExecuteMethods();
    private final ColumnValues columnValues = new ColumnValues();

    private static Set<String> buildExecuteMethods() {
        Set<String> exec = new HashSet<>();
        exec.add("execute");
        exec.add("executeUpdate");
        exec.add("executeQuery");
        exec.add("addBatch");
        return Collections.unmodifiableSet(exec);
    }

    private String query;
    private PreparedStatementListener preparedStatementListener;

    AbstractStatement(String query, PreparedStatementListener preparedStatementListener) {
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
