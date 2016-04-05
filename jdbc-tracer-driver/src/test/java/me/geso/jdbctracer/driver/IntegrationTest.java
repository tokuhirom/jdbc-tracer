package me.geso.jdbctracer.driver;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import me.geso.jdbctracer.PreparedStatementListener;
import me.geso.jdbctracer.ResultSetListener;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class IntegrationTest {
    private static Connection fixtureConn;

    @BeforeClass
    public static void beforeClass() throws ClassNotFoundException, IllegalAccessException, InstantiationException, SQLException {
        fixtureConn = DriverManager.getConnection("jdbc:h2:mem:test");
        call(fixtureConn, "CREATE TABLE IF NOT EXISTS user (id int, name varchar(255))");
        call(fixtureConn, "DELETE FROM user");
        call(fixtureConn, "INSERT INTO user (id, name) values (1,'john')");
        call(fixtureConn, "INSERT INTO user (id, name) values (2,'nick')");
    }

    @AfterClass
    public static void cleanup() throws SQLException {
        if (fixtureConn != null) {
            fixtureConn.close();
        }
    }

    private static int call(Connection conn, String stmt) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(stmt)) {
            return ps.executeUpdate();
        }
    }

    @Test
    public void psUrl() throws SQLException {
        try (Connection connection = DriverManager.
                getConnection("jdbc:tracer:ps=me.geso.jdbctracer.driver.IntegrationTest$PSListener:h2:mem:test")) {
            List<String> names = new ArrayList<>();
            try (PreparedStatement ps = connection.prepareStatement("SELECT name FROM user WHERE id=?")) {
                ps.setInt(1, 2);
                try (ResultSet resultSet = ps.executeQuery()) {
                    while (resultSet.next()) {
                        String name = resultSet.getString(1);
                        names.add(name);
                    }
                }
            }

            // tracing data
            List<PSListener.PSResult> results = PSListener.getResults();
            assertThat(results)
                    .hasSize(1);
            assertThat(results.get(0).getElapsed())
                    .isNotEqualTo(0);
            assertThat(results.get(0).getQuery())
                    .isEqualTo("SELECT name FROM user WHERE id=?");
            assertThat(results.get(0).getArgs())
                    .isEqualTo(Collections.singletonList(2));

            // fetched data
            assertThat(names)
                    .hasSize(1);
            assertThat(names.get(0))
                    .isEqualTo("nick");
        }
    }

    @Test
    public void statement() throws SQLException {
        try (Connection connection = DriverManager.
                getConnection("jdbc:tracer:ps=me.geso.jdbctracer.driver.IntegrationTest$PSListener:h2:mem:test")) {
            List<String> names = new ArrayList<>();
            try (Statement statement = connection.createStatement()) {
                try (ResultSet resultSet = statement.executeQuery("SELECT name FROM user WHERE id=2")) {
                    while (resultSet.next()) {
                        String name = resultSet.getString(1);
                        names.add(name);
                    }
                }
            }

            // tracing data
            List<PSListener.PSResult> results = PSListener.getResults();
            assertThat(results)
                    .hasSize(1);
            assertThat(results.get(0).getElapsed())
                    .isNotEqualTo(0);
            assertThat(results.get(0).getQuery())
                    .isEqualTo("SELECT name FROM user WHERE id=2");
            assertThat(results.get(0).getArgs())
                    .isEqualTo(Collections.emptyList());

            // fetched data
            assertThat(names)
                    .hasSize(1);
            assertThat(names.get(0))
                    .isEqualTo("nick");
        }
    }

    @Test
    public void rsUrl() throws SQLException {
        try (Connection connection = DriverManager.
                getConnection("jdbc:tracer:rs=me.geso.jdbctracer.driver.IntegrationTest$RSListener:h2:mem:test")) {
            List<String> names = new ArrayList<>();
            try (PreparedStatement ps = connection.prepareStatement("SELECT id, name FROM user ORDER BY id")) {
                try (ResultSet resultSet = ps.executeQuery()) {
                    while (resultSet.next()) {
                        String name = resultSet.getString(2);
                        names.add(name);
                    }
                }
            }

            // tracing data
            List<RSListener.RSResult> results = RSListener.getResults();
            assertThat(results)
                    .hasSize(2);
            assertThat(results.get(0).isFirst())
                    .isTrue();
            assertThat(results.get(0).getValues())
                    .isEqualTo(Arrays.asList(1, "john"));
            assertThat(results.get(1).isFirst())
                    .isFalse();
            assertThat(results.get(1).getValues())
                    .isEqualTo(Arrays.asList(2, "nick"));

            // fetched data
            assertThat(names)
                    .hasSize(2);
            assertThat(names.get(0))
                    .isEqualTo("john");
            assertThat(names.get(1))
                    .isEqualTo("nick");
        }
    }

    @Before
    public void clear() {
        PSListener.clear();
        RSListener.clear();
    }

    public static class PSListener implements PreparedStatementListener {
        private static List<PSResult> results = new ArrayList<>();

        static List<PSResult> getResults() {
            return Collections.unmodifiableList(results);
        }

        static void clear() {
            results.clear();
        }

        @Override
        public void trace(Connection connection, long elapsed, String query, List<Object> args) {
            results.add(new PSResult(elapsed, query, args));
        }

        @AllArgsConstructor
        @Getter
        @ToString
        static class PSResult {
            private long elapsed;
            private String query;
            private List<Object> args;
        }
    }

    public static class RSListener implements ResultSetListener {
        private static List<RSResult> results = new ArrayList<>();

        static List<RSResult> getResults() {
            return Collections.unmodifiableList(results);
        }

        private static void clear() {
            results.clear();
        }

        @Override
        public void trace(Connection connection, Statement statement, boolean first, ResultSet resultSet) throws SQLException {
            ResultSetMetaData metaData = resultSet.getMetaData();
            int columnCount = metaData.getColumnCount();
            List<Object> values = new ArrayList<>();
            for (int i = 1; i <= columnCount; ++i) {
                values.add(resultSet.getObject(i));
            }
            results.add(new RSResult(first, values));
        }

        @AllArgsConstructor
        @Getter
        @ToString
        public static class RSResult {
            private boolean first;
            private List<Object> values;
        }
    }
}