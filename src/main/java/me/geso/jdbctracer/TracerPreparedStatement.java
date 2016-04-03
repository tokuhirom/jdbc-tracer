package me.geso.jdbctracer;

import me.geso.jdbctracer.util.ExceptionUtil;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class TracerPreparedStatement extends AbstractStatement implements InvocationHandler {
    private final PreparedStatement statement;
    private final ResultSetListener resultSetListener;

    protected static final Set<String> SET_METHODS = buildSetMethods();

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

    public TracerPreparedStatement(PreparedStatement statement, String query, PreparedStatementListener preparedStatementListener, ResultSetListener resultSetListener) {
        super(query, preparedStatementListener);
        this.statement = statement;
        this.resultSetListener = resultSetListener;
    }

    public static PreparedStatement newInstance(PreparedStatement stmt, String query, PreparedStatementListener preparedStatementListener, ResultSetListener resultSetListener) {
        return (PreparedStatement) Proxy.newProxyInstance(
                TracerPreparedStatement.class.getClassLoader(),
                new Class<?>[]{PreparedStatement.class},
                new TracerPreparedStatement(stmt, query, preparedStatementListener, resultSetListener));
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
                        return rs == null ? null : TracerResultSet.newInstance(rs, resultSetListener);
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
                return rs == null ? null : TracerResultSet.newInstance(rs, resultSetListener);
            } else if ("getUpdateCount".equals(method.getName())) {
                return (Integer) method.invoke(statement, params);
            } else {
                return method.invoke(statement, params);
            }
        } catch (Throwable t) {
            throw ExceptionUtil.unwrapThrowable(t);
        }
    }
}
