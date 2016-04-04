package me.geso.jdbctracer;

import java.sql.ResultSet;
import java.sql.SQLException;

public interface ResultSetListener {
    void trace(boolean first, ResultSet resultSet) throws SQLException;
}
