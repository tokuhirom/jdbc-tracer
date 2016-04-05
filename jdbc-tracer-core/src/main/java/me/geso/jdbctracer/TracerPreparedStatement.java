package me.geso.jdbctracer;

import lombok.NonNull;
import me.geso.jdbctracer.util.ExceptionUtil;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

class TracerPreparedStatement implements InvocationHandler {
    private static final Set<String> EXECUTE_METHODS = buildExecuteMethods();
    private static final Set<String> SET_METHODS = buildSetMethods();
    private final PreparedStatement statement;
    private final ResultSetListener resultSetListener;
    private final ColumnValues columnValues = new ColumnValues();
    private final Connection connection;
    private final String query;
    private final PreparedStatementListener preparedStatementListener;

    private TracerPreparedStatement(@NonNull Connection connection, @NonNull PreparedStatement statement, String query, PreparedStatementListener preparedStatementListener, ResultSetListener resultSetListener) {
        this.connection = connection;
        this.query = query;
        this.preparedStatementListener = preparedStatementListener;
        this.statement = statement;
        this.resultSetListener = resultSetListener;
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

    static PreparedStatement newInstance(Connection connection, Class<?> klass, @NonNull PreparedStatement stmt, String query, PreparedStatementListener preparedStatementListener, ResultSetListener resultSetListener) {
        return (PreparedStatement) Proxy.newProxyInstance(
                TracerPreparedStatement.class.getClassLoader(),
                new Class<?>[]{klass},
                new TracerPreparedStatement(connection, stmt, query, preparedStatementListener, resultSetListener));
    }

    private static Set<String> buildExecuteMethods() {
        Set<String> exec = new HashSet<>();
        exec.add("execute");
        exec.add("executeUpdate");
        exec.add("executeQuery");
        exec.add("addBatch");
        return Collections.unmodifiableSet(exec);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] params) throws Throwable {
        try {
            if (Object.class.equals(method.getDeclaringClass())) {
                return method.invoke(this, params);
            }
            if (EXECUTE_METHODS.contains(method.getName())) {
                if ("executeQuery".equals(method.getName()) && resultSetListener != null) {
                    return trace(() -> {
                        ResultSet rs = (ResultSet) method.invoke(statement, params);
                        return rs == null ? null : TracerResultSet.newInstance(connection, statement, rs, resultSetListener);
                    });
                } else {
                    return trace(() -> {
                        return method.invoke(statement, params);
                    });
                }
            } else if (SET_METHODS.contains(method.getName())) {
                if ("setNull".equals(method.getName())) {
                    setColumn((int) params[0], null);
                } else {
                    setColumn((int) params[0], params[1]);
                }
                return method.invoke(statement, params);
            } else if ("getResultSet".equals(method.getName())) {
                ResultSet rs = (ResultSet) method.invoke(statement, params);
                return rs == null ? null : TracerResultSet.newInstance(connection, statement, rs, resultSetListener);
            } else if ("getUpdateCount".equals(method.getName())) {
                return method.invoke(statement, params);
            } else {
                return method.invoke(statement, params);
            }
        } catch (Throwable t) {
            throw ExceptionUtil.unwrapThrowable(t);
        }
    }

    private void setColumn(int pos, Object value) {
        this.columnValues.put(pos, value);
    }

    private <T> T trace(TraceSupplier<T> supplier) throws InvocationTargetException, IllegalAccessException, SQLException {
        List<Object> params = this.columnValues.values();
        this.columnValues.clear();
        if (preparedStatementListener != null) {
            long start = System.nanoTime();
            T retval = supplier.get();
            long finished = System.nanoTime();
            preparedStatementListener.trace(connection, finished - start, query, params);
            return retval;
        } else {
            return supplier.get();
        }
    }

    private interface TraceSupplier<T> {
        T get() throws IllegalAccessException, IllegalArgumentException,
                InvocationTargetException;
    }
}
