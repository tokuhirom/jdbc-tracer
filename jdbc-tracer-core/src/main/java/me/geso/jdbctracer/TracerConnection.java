package me.geso.jdbctracer;

import lombok.NonNull;
import me.geso.jdbctracer.util.ExceptionUtil;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Objects;

/**
 * Traced connection
 */
public class TracerConnection implements InvocationHandler {
    private Connection connection;
    private final PreparedStatementListener preparedStatementListener;
    private final ResultSetListener resultSetListener;

    TracerConnection(Connection connection, PreparedStatementListener preparedStatementListener, ResultSetListener resultSetListener) {
        this.connection = Objects.requireNonNull(connection);
        this.preparedStatementListener = preparedStatementListener;
        this.resultSetListener = resultSetListener;
    }

    /**
     * Create new instance of traced connection.
     *
     * @param connection Target JDBC connection
     * @param psl        Listener for PreparedStatement/Statement
     * @param rsl        Listener for ResultSet
     * @return Created connection object.
     */
    public static Connection newInstance(@NonNull Connection connection,
                                  @NonNull PreparedStatementListener psl,
                                  @NonNull ResultSetListener rsl) {
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
                return TracerPreparedStatement.newInstance(connection, PreparedStatement.class, stmt, (String) params[0], preparedStatementListener, resultSetListener);
            } else if ("prepareCall".equals(method.getName())) {
                PreparedStatement stmt = (PreparedStatement) method.invoke(connection, params);
                return TracerPreparedStatement.newInstance(connection, CallableStatement.class, stmt, (String) params[0], preparedStatementListener, resultSetListener);
            } else if ("createStatement".equals(method.getName())) {
                Statement stmt = (Statement) method.invoke(connection, params);
                stmt = TracerStatement.newInstance(connection, stmt, preparedStatementListener, resultSetListener);
                return stmt;
            } else {
                return method.invoke(connection, params);
            }
        } catch (Throwable t) {
            throw ExceptionUtil.unwrapThrowable(t);
        }
    }
}
