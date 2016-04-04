package me.geso.jdbctracer;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public interface PreparedStatementListener {
    void trace(Connection connection, long elapsed, String query, List<Object> args) throws SQLException;
}
