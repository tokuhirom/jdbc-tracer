package me.geso.jdbctracer;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public interface ResultSetListener {
    void trace(Connection connection, Statement statement, boolean first, ResultSet resultSet) throws SQLException;
}
