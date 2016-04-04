package com.example;

import lombok.extern.slf4j.Slf4j;
import me.geso.jdbctracer.PreparedStatementListener;

import java.sql.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class PSTracer implements PreparedStatementListener {
    private static final Pattern RE = Pattern.compile("(\\?)");

    @Override
    public void trace(Connection connection, long elapsed, String query, List<Object> args) throws SQLException {
        log.trace("QUERY: {}, {}", query, args);
        if (log.isDebugEnabled()) {
            if (!query.startsWith("EXPLAIN")) {
                String binded = bind(query, args);

                log.debug("EXPLAIN: {}", query);
                try (PreparedStatement preparedStatement = connection.prepareStatement("EXPLAIN " + binded)) {
                    try (ResultSet resultSet = preparedStatement.executeQuery()) {
                        ResultSetMetaData metaData = resultSet.getMetaData();
                        int columnCount = metaData.getColumnCount();
                        while (resultSet.next()) {
                            for (int i = 1; i <= columnCount; ++i) {
                                log.debug("{}: {}",
                                        metaData.getColumnName(i),
                                        resultSet.getString(i));
                            }
                        }
                    }
                }
            }
        }
    }

    private String bind(String query, List<Object> binds) {
        int idx = 0;
        Matcher matcher = RE.matcher(query);
        boolean result = matcher.find();
        if (result) {
            StringBuffer sb = new StringBuffer();
            do {
                matcher.appendReplacement(sb, String.valueOf(binds.get(idx++)));
                result = matcher.find();
            } while (result);
            matcher.appendTail(sb);
            return sb.toString();
        }
        return query.toString();
    }
}
