package me.geso.jdbctracer;

import me.geso.jdbctracer.util.ExceptionUtil;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Collections;

class TracerStatement implements InvocationHandler {
    private final ResultSetListener resultSetListener;
    private final Statement statement;
    private final PreparedStatementListener preparedStatementListener;

    private TracerStatement(Statement stmt, PreparedStatementListener preparedStatementListener, ResultSetListener resultSetListener) {
        this.preparedStatementListener = preparedStatementListener;
        this.statement = stmt;
        this.resultSetListener = resultSetListener;
    }

    static Statement newInstance(Statement stmt, PreparedStatementListener preparedStatementListener, ResultSetListener resultSetListener) {
        return (Statement) Proxy.newProxyInstance(
                TracerPreparedStatement.class.getClassLoader(),
                new Class<?>[]{Statement.class},
                new TracerStatement(stmt, preparedStatementListener, resultSetListener));
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] params) throws Throwable {
        try {
            if (Object.class.equals(method.getDeclaringClass())) {
                return method.invoke(this, params);
            }
            if ("executeQuery".equals(method.getName())) {
                String query = (String) params[0];
                return trace(query, () -> {
                    ResultSet rs = (ResultSet) method.invoke(statement, params);
                    return rs == null ? null : TracerResultSet.newInstance(rs, resultSetListener);
                });
            } else if ("execute".equals(method.getName())
                    || "executeUpdate".equals(method.getName())
                    || "addBatch".equals(method.getName())) {
                String query = (String) params[0];
                return trace(query, () -> method.invoke(statement, params));
            } else if ("getResultSet".equals(method.getName()) && resultSetListener != null) {
                ResultSet rs = (ResultSet) method.invoke(statement, params);
                return rs == null ? null : TracerResultSet.newInstance(rs, resultSetListener);
            } else if ("equals".equals(method.getName())) {
                Object ps = params[0];
                return ps instanceof Proxy && proxy == ps;
            } else if ("hashCode".equals(method.getName())) {
                return proxy.hashCode();
            } else {
                return method.invoke(statement, params);
            }
        } catch (IllegalAccessException | IllegalArgumentException |
                InvocationTargetException t) {
            throw ExceptionUtil.unwrapThrowable(t);
        }
    }

    private <T> T trace(String query, TraceSupplier<T> supplier) throws InvocationTargetException, IllegalAccessException {
        if (preparedStatementListener != null) {
            long start = System.nanoTime();
            T retval = supplier.get();
            long finished = System.nanoTime();
            preparedStatementListener.trace(finished - start, query, Collections.emptyList());
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
