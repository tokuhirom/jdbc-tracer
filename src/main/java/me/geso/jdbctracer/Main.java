package me.geso.jdbctracer;

import com.mysql.jdbc.Driver;
import lombok.extern.slf4j.Slf4j;

import java.io.PrintWriter;
import java.sql.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by tokuhirom on 4/2/16.
 */
// TODO remove
@Slf4j
public class Main {
    public static void main(String[] args) throws SQLException, ClassNotFoundException, IllegalAccessException, InstantiationException {
        Class.forName("com.mysql.jdbc.Driver").newInstance();
        Class.forName("me.geso.jdbctracer.TracerDriver").newInstance();

        TracerDriver.setPreparedStatementListener(
                (elapsed, query, params)
                        -> log.info("prepareStatement: {}, {} {}", elapsed, query, params)
        );
        TracerDriver.setResultSetListener(
                (first, resultSet) -> {
                    ResultSetMetaData metaData = resultSet.getMetaData();
                    int columnCount = metaData.getColumnCount();
                    if (first) {
                        List<String> columnNames = new ArrayList<String>();
                        for (int i = 1; i <= columnCount; ++i) {
                            columnNames.add(metaData.getColumnName(i));
                        }
                        log.info("header: {}", columnNames.stream().collect(Collectors.joining(",")));
                    }
                    List<Object> values = new ArrayList<>();
                    for (int i = 1; i <= columnCount; ++i) {
                        values.add(resultSet.getObject(i));
                    }
                    log.info("GOT RESULT SET: {}", values);

                }
        );

        try (Connection connection = DriverManager.getConnection("jdbc:tracer::mysql://localhost/test", "root", "")) {
            try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM mysql.user")) {
                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    while (resultSet.next()) {
                    }
                }
            }

            try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM mysql.user WHERE user=?")) {
                preparedStatement.setString(1, "root");
                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    while (resultSet.next()) {
                    }
                }
            }
        }
    }
}
