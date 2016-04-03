package me.geso.jdbctracer;

import me.geso.jdbctracer.util.ExceptionUtil;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;

public class TracerStatement extends AbstractStatement implements InvocationHandler {
    private final ResultSetListener resultSetListener;
    private final Statement statement;

    public TracerStatement(Statement stmt, String query, PreparedStatementListener preparedStatementListener, ResultSetListener resultSetListener) {
        super(query, preparedStatementListener);
        this.statement = stmt;
        this.resultSetListener = resultSetListener;
    }

    public static Statement newInstance(Statement stmt, String query, PreparedStatementListener preparedStatementListener, ResultSetListener resultSetListener) {
        return (Statement) Proxy.newProxyInstance(
                TracerPreparedStatement.class.getClassLoader(),
                new Class<?>[]{Statement.class},
                new TracerStatement(stmt, query, preparedStatementListener, resultSetListener));
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] params) throws Throwable {
        try {
            if (Object.class.equals(method.getDeclaringClass())) {
                return method.invoke(this, params);
            }
            if (EXECUTE_METHODS.contains(method.getName())) {
                if ("executeQuery".equals(method.getName())) {
                    return trace(() -> {
                        ResultSet rs = (ResultSet) method.invoke(statement, params);
                        return rs == null ? null : TracerResultSet.newInstance(rs, resultSetListener);
                    });
                } else {
                    return trace(() -> {
                        return method.invoke(statement, params);
                    });
                }
            } else if ("getResultSet".equals(method.getName())) {
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

}
