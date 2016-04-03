package me.geso.jdbctracer;

import me.geso.jdbctracer.util.ExceptionUtil;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Objects;

class TracerConnection implements InvocationHandler {
    private Connection connection;
    private final PreparedStatementListener preparedStatementListener;
    private final ResultSetListener resultSetListener;

    TracerConnection(Connection connection, PreparedStatementListener preparedStatementListener, ResultSetListener resultSetListener) {
        this.connection = Objects.requireNonNull(connection);
        this.preparedStatementListener = preparedStatementListener;
        this.resultSetListener = resultSetListener;
    }

    static Connection newInstance(Connection connection,
                                  PreparedStatementListener psl,
                                  ResultSetListener rsl) {
        return (Connection) Proxy.newProxyInstance(
                TracerConnection.class.getClassLoader(),
                new Class<?>[]{Connection.class},
                new TracerConnection(connection, psl, rsl));
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] params) throws Throwable {
        try {
            if (Object.class.equals(method.getDeclaringClass())) {
                return method.invoke(this, params);
            }
            if ("prepareStatement".equals(method.getName())) {
                PreparedStatement stmt = (PreparedStatement) method.invoke(connection, params);
                return TracerPreparedStatement.newInstance(PreparedStatement.class, stmt, (String) params[0], preparedStatementListener, resultSetListener);
            } else if ("prepareCall".equals(method.getName())) {
                PreparedStatement stmt = (PreparedStatement) method.invoke(connection, params);
                stmt = TracerPreparedStatement.newInstance(CallableStatement.class, stmt, (String) params[0], preparedStatementListener, resultSetListener);
                return stmt;
            } else if ("createStatement".equals(method.getName())) {
                Statement stmt = (Statement) method.invoke(connection, params);
                stmt = TracerStatement.newInstance(stmt, preparedStatementListener, resultSetListener);
                return stmt;
            } else {
                return method.invoke(connection, params);
            }
        } catch (Throwable t) {
            throw ExceptionUtil.unwrapThrowable(t);
        }
    }
}
