package me.geso.jdbctracer;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collections;

import static java.sql.Types.ARRAY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class TracerPreparedStatementTest {
    @Mock
    Connection connection;
    @Mock
    PreparedStatement stmt;
    @Mock
    PreparedStatementListener psl;
    @Mock
    ResultSetListener rsl;
    @Mock
    ResultSet rs;

    private PreparedStatement target;

    @Before
    public void before() {
        this.target = TracerPreparedStatement.newInstance(
                connection, PreparedStatement.class,
                stmt,
                "SELECT * FROM foo",
                psl,
                rsl
        );
    }

    @Test
    public void setNull() throws Exception {
        when(stmt.executeQuery()).thenReturn(rs);
        this.target.setNull(1, ARRAY);
        ResultSet rs = this.target.executeQuery();
        assertThat(rs.toString())
                .contains("TracerResultSet");

        verify(psl, times(1))
                .trace(eq(connection), anyLong(), eq("SELECT * FROM foo"), eq(Collections.singletonList(null)));

        verify(stmt, times(1))
                .executeQuery();
    }

    @Test
    public void executeQuery() throws Exception {
        when(stmt.executeQuery()).thenReturn(rs);
        this.target.setInt(1, 5963);
        ResultSet rs = this.target.executeQuery();
        assertThat(rs.toString())
                .contains("TracerResultSet");

        verify(psl, times(1))
                .trace(eq(connection), anyLong(), eq("SELECT * FROM foo"), eq(Collections.singletonList(5963)));

        verify(stmt, times(1))
                .executeQuery();
    }

    @Test
    public void execute() throws Exception {
        this.target.execute();

        verify(psl, times(1))
                .trace(eq(connection), anyLong(), eq("SELECT * FROM foo"), eq(Collections.emptyList()));
        verify(stmt, times(1))
                .execute();
    }

    @Test
    public void getResultSet() throws Exception {
        when(stmt.getResultSet()).thenReturn(rs);
        ResultSet got = this.target.getResultSet();

        assertThat(got)
                .isInstanceOf(Proxy.class);
    }

    @Test
    public void withoutPSListener() throws Exception {
        this.target = TracerPreparedStatement.newInstance(
                connection, PreparedStatement.class,
                stmt,
                "SELECT * FROM foo",
                null,
                rsl
        );

        when(stmt.executeQuery()).thenReturn(rs);
        ResultSet got = this.target.executeQuery();

        assertThat(got)
                .isInstanceOf(Proxy.class);
        verify(stmt).executeQuery();
    }

    @Test
    public void withoutRSListener() throws Exception {
        this.target = TracerPreparedStatement.newInstance(
                connection, PreparedStatement.class,
                stmt,
                "SELECT * FROM foo",
                psl,
                null
        );

        when(stmt.executeQuery()).thenReturn(rs);
        ResultSet got = this.target.executeQuery();

        assertThat(got)
                .isSameAs(rs);
        verify(stmt).executeQuery();
    }

}