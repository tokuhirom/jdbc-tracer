package me.geso.jdbctracer;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public interface ResultSetListener {
    void trace(boolean first, ResultSet resultSet) throws SQLException;
}
