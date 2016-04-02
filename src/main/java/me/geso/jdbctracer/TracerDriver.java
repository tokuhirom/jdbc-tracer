package me.geso.jdbctracer;

import com.mysql.jdbc.interceptors.ResultSetScannerInterceptor;

import java.lang.reflect.Proxy;
import java.sql.*;
import java.util.Enumeration;
import java.util.Objects;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TracerDriver implements java.sql.Driver {
    private static final Pattern URI_PATTERN = Pattern.compile("jdbc:tracer:([^:]*):(.*)");

    static {
        try {
            java.sql.DriverManager.registerDriver(new TracerDriver());
        } catch (SQLException e) {
            throw new RuntimeException("Can't register jdbcproxy driver!");
        }
    }

    private static PreparedStatementListener preparedStatementListener;
    private static ResultSetListener resultSetListener;

    public static void setPreparedStatementListener(PreparedStatementListener preparedStatementListener) {
        TracerDriver.preparedStatementListener = preparedStatementListener;
    }

    public static void setResultSetListener(ResultSetListener resultSetListener) {
        TracerDriver.resultSetListener = resultSetListener;
    }

    @Override
    public Connection connect(String url, Properties info) throws SQLException {
        URLParseResult urlParseResult = parseURL(url);
        if (urlParseResult != null) {
            Driver underlyingDriver = getUnderlyingDriver(urlParseResult.underlingUri);
            if (underlyingDriver == null) {
                return null;
            }
            PreparedStatementListener ps = preparedStatementListener;
            ResultSetListener rs = resultSetListener;
            try {
                if (urlParseResult.preparedStatementListener != null) {
                    Class<?> aClass = Class.forName(urlParseResult.preparedStatementListener);
                    ps = (PreparedStatementListener)aClass.newInstance();
                }
                if (urlParseResult.resultSetListener != null) {
                    Class<?> aClass = Class.forName(urlParseResult.resultSetListener);
                    rs = (ResultSetListener)aClass.newInstance();
                }
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
                throw new SQLException(e);
            }

            Connection connection = Objects.requireNonNull(underlyingDriver.connect(urlParseResult.underlingUri, info));
            return (Connection) Proxy.newProxyInstance(
                    getClass().getClassLoader(),
                    new Class[]{Connection.class},
                    new TracerConnectionInter(connection, ps, rs));
        } else {
            return null;
        }
    }

    @Override
    public boolean acceptsURL(String url) throws SQLException {
        URLParseResult urlParseResult = parseURL(url);
        if (urlParseResult != null) {
            return getUnderlyingDriver(urlParseResult.underlingUri) != null;
        } else {
            return false;
        }
    }

    @Override
    public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException {
        Driver underlyingDriver = getUnderlyingDriver(url);
        if (underlyingDriver == null) {
            return new DriverPropertyInfo[0];
        }
        return underlyingDriver.getPropertyInfo(url, info);
    }

    @Override
    public int getMajorVersion() {
        return 0;
    }

    @Override
    public int getMinorVersion() {
        return 0;
    }

    @Override
    public boolean jdbcCompliant() {
        return false;
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        throw new SQLFeatureNotSupportedException();
    }

    private URLParseResult parseURL(String url) throws InvalidJDBCTracerURL {
        Matcher matcher = URI_PATTERN.matcher(url);
        if (matcher.matches()) {
            URLParseResult urlParseResult = new URLParseResult();
            String opts = matcher.group(1);
            urlParseResult.underlingUri = "jdbc:" + matcher.group(2);
            String[] split = opts.split("&");
            for (String part : split) {
                if (part.length() == 0) {
                    continue;
                }
                String[] kv = part.split("=");
                if (kv.length != 2) {
                    throw new InvalidJDBCTracerURL(url);
                }
                String klassName = kv[1];
                switch (kv[0]) {
                    case "rs":
                        urlParseResult.resultSetListener = klassName;
                        break;
                    case "ps":
                        urlParseResult.preparedStatementListener = klassName;
                        break;
                }
            }
            return urlParseResult;
        } else {
            return null;
        }
    }

    private static class InvalidJDBCTracerURL extends SQLException {
        InvalidJDBCTracerURL(String url) {
            super("Invalid JDBC tracer URL: " + url);
        }
    }

    private static class URLParseResult {
        String underlingUri;
        String resultSetListener;
        String preparedStatementListener;
    }

    private Driver getUnderlyingDriver(String underlingUri) throws SQLException {
        Enumeration e = DriverManager.getDrivers();

        Driver d;
        while (e.hasMoreElements()) {
            d = (Driver) e.nextElement();

            if (d.acceptsURL(underlingUri)) {
                return d;
            }
        }
        return null;
    }

}
