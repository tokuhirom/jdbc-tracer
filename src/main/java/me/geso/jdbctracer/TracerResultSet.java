package me.geso.jdbctracer;

import me.geso.jdbctracer.util.ExceptionUtil;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.ResultSet;

class TracerResultSet implements InvocationHandler {
    private final ResultSet resultSet;
    private ResultSetListener resultSetListener;
    private boolean first;

    private TracerResultSet(ResultSet resultSet, ResultSetListener resultSetListener) {
        this.resultSet = resultSet;
        this.resultSetListener = resultSetListener;
        this.first = true;
    }

    static ResultSet newInstance(ResultSet resultSet, ResultSetListener resultSetListener) {
        return (ResultSet) Proxy.newProxyInstance(
                TracerPreparedStatement.class.getClassLoader(),
                new Class<?>[]{ResultSet.class},
                new TracerResultSet(resultSet, resultSetListener));
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
                        resultSetListener.trace(first, resultSet);
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
