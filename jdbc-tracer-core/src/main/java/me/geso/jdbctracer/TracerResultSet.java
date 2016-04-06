package me.geso.jdbctracer;

import lombok.ToString;
import me.geso.jdbctracer.util.ExceptionUtil;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

@ToString
class TracerResultSet implements InvocationHandler {
    private Connection connection;
    private Statement statement;
    private final ResultSet resultSet;
    private ResultSetListener resultSetListener;
    private boolean first;

    private TracerResultSet(Connection connection, Statement statement, ResultSet resultSet, ResultSetListener resultSetListener) {
        this.connection = connection;
        this.statement = statement;
        this.resultSet = resultSet;
        this.resultSetListener = resultSetListener;
        this.first = true;
    }

    static ResultSet newInstance(Connection connection, Statement statement, ResultSet resultSet, ResultSetListener resultSetListener) {
        return (ResultSet) Proxy.newProxyInstance(
                TracerPreparedStatement.class.getClassLoader(),
                new Class<?>[]{ResultSet.class},
                new TracerResultSet(connection, statement, resultSet, resultSetListener));
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] params) throws Throwable {
        try {
            if (Object.class.equals(method.getDeclaringClass())) {
                return method.invoke(this, params);
            }
            Object o = method.invoke(resultSet, params);
            if ("next".equals(method.getName())) {
                if (((Boolean) o)) {
                    if (resultSetListener != null) {
                        resultSetListener.trace(connection, statement, first, resultSet);
                        first = false;
                    }
                }
            }
            return o;
        } catch (Throwable t) {
            throw ExceptionUtil.unwrapThrowable(t);
        }
    }
}
