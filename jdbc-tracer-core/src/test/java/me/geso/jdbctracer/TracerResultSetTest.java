package me.geso.jdbctracer;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class TracerResultSetTest {
    @Mock
    ResultSet resultSet;
    @Mock
    ResultSetListener rsl;
    @Mock
    Connection connection;
    @Mock
    Statement statement;

    private ResultSet target;

    @Before
    public void before() {
        this.target = TracerResultSet.newInstance(
                connection, statement, resultSet,
                rsl
        );
    }

    @Test
    public void test() throws SQLException {
        when(resultSet.next()).thenReturn(true);
        target.next();
        verify(rsl, times(1)).trace(connection, statement, true, resultSet);
        target.next();
        verify(rsl, times(1)).trace(connection, statement, false, resultSet);
    }

    @Test
    public void testWithoutListener() throws SQLException {
        this.target = TracerResultSet.newInstance(
                connection, statement, resultSet,
                null
        );
        when(resultSet.next()).thenReturn(true);
        target.next();
        verify(resultSet, times(1)).next();
        target.next();
        verify(resultSet, times(2)).next();
    }

    @Test
    public void exception() throws SQLException {
        when(resultSet.next()).thenThrow(new MyException());
        Assertions.assertThatThrownBy(() -> target.next())
                .isInstanceOf(MyException.class);
    }

    public static class MyException extends RuntimeException {
    }
}