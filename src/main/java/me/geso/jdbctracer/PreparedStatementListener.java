package me.geso.jdbctracer;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public interface PreparedStatementListener {
    void trace(long elapsed, String query, List<Object> args);
}
