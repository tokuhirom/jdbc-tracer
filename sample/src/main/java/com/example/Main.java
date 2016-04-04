package com.example;

import java.sql.*;

public class Main {
    public static void main(String[] args) throws ClassNotFoundException, IllegalAccessException, InstantiationException, SQLException {
        Class.forName("org.h2.Driver");

        // create fixture data.
        try (Connection fixtureConn = DriverManager.getConnection("jdbc:h2:mem:test")) {
            try (Statement stmt = fixtureConn.createStatement()) {
                stmt.executeUpdate("CREATE TABLE IF NOT EXISTS user (id int PRIMARY KEY, name varchar(255))");
                stmt.executeUpdate("INSERT INTO user (id, name) values (1, 'John'), (2, 'nick')");
            }
            doMain();
        }
    }

    private static void doMain() throws SQLException {
        try (Connection conn = DriverManager.getConnection("jdbc:tracer:ps=com.example.PSTracer:h2:mem:test")) {
            try (PreparedStatement stmt = conn.prepareStatement("SELECT * FROM user WHERE id=?")) {
                stmt.setInt(1, 1);
                try (ResultSet resultSet = stmt.executeQuery()) {
                }
            }
            try (PreparedStatement stmt = conn.prepareStatement("SELECT * FROM user")) {
                try (ResultSet resultSet = stmt.executeQuery()) {
                }
            }
        }
    }
}
