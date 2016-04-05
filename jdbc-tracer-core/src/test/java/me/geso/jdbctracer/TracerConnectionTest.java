package me.geso.jdbctracer;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.sql.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class TracerConnectionTest {
    @Mock
    PreparedStatementListener psl;

    @Mock
    ResultSetListener rsl;

    @Mock
    Connection connection;

    Connection target;

    @Before
    public void initTarget() {
        this.target = TracerConnection.newInstance(
                this.connection,
                psl, rsl
        );
    }

    @Test
    public void testToString() throws Exception {
        String s = this.target.toString();
        assertThat(s).contains("TracerConnection");
        this.target.close();
    }

    @Test
    public void close() throws SQLException {
        when(this.connection.isClosed()).thenReturn(true);
        this.target.close();
        assertThat(this.target.isClosed()).isTrue();
    }

    @Test
    public void exception() throws SQLException {
        when(this.connection.isClosed()).thenThrow(
                new MyException()
        );
        assertThatThrownBy(() -> this.target.isClosed())
                .isInstanceOf(MyException.class);
    }

    public static class MyException extends RuntimeException {
    }

    @Test
    public void prepareStatement() throws Exception {
        when(connection.prepareStatement("SELECT * FROM a")).thenReturn(mock(PreparedStatement.class));
        try (PreparedStatement preparedStatement = target.prepareStatement("SELECT * FROM a")) {
            assertThat(preparedStatement.toString())
                    .contains("TracerPreparedStatement");
            verify(connection, times(1))
                    .prepareStatement("SELECT * FROM a");
        }
    }

    @Test
    public void prepareCall() throws Exception {
        when(connection.prepareCall("foo")).thenReturn(mock(CallableStatement.class));
        try (CallableStatement statement = target.prepareCall("foo");) {
            assertThat(statement.toString())
                    .contains("TracerPreparedStatement");
            verify(connection, times(1))
                    .prepareCall("foo");
        }
    }

    @Test
    public void createStatement() throws Exception {
        when(connection.createStatement()).thenReturn(mock(Statement.class));
        try (Statement statement = target.createStatement()) {
            assertThat(statement.toString())
                    .contains("TracerStatement");
            verify(connection, times(1))
                    .createStatement();
        }
    }

}