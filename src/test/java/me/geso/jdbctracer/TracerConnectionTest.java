package me.geso.jdbctracer;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.lang.reflect.Proxy;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

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
        this.target = (Connection) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{Connection.class},
                new TracerConnection(this.connection, psl, rsl));
    }

    @Test
    public void prepareStatement() throws Exception {
        PreparedStatement preparedStatement = target.prepareStatement("SELECT * FROM a");
        assertThat(preparedStatement.toString())
                .contains("TracerPreparedStatement");
        verify(connection, times(1))
                .prepareStatement("SELECT * FROM a");
    }

    @Test
    public void prepareCall() throws Exception {
        CallableStatement statement = target.prepareCall("foo");
        assertThat(statement.toString())
                .contains("TracerPreparedStatement");
        verify(connection, times(1))
                .prepareCall("foo");
    }

    @Test
    public void createStatement() throws Exception {
        Statement statement = target.createStatement();
        assertThat(statement.toString())
                .contains("TracerStatement");
        verify(connection, times(1))
                .createStatement();
    }

}