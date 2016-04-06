package me.geso.jdbctracer;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;


@RunWith(MockitoJUnitRunner.class)
public class TracerStatementTest {
    @Mock
    Connection connection;
    @Mock
    Statement stmt;
    @Mock
    PreparedStatementListener psl;
    @Mock
    ResultSetListener rsl;
    @Mock
    ResultSet rs;

    private Statement target;

    @Before
    public void before() {
        this.target = TracerStatement.newInstance(
                connection,
                stmt,
                psl,
                rsl
        );
    }

    @Test
    public void executeQuery() throws Exception {
        when(stmt.executeQuery("foo")).thenReturn(rs);
        ResultSet rs = this.target.executeQuery("foo");
        assertThat(rs.toString())
                .contains("TracerResultSet");

        verify(psl, times(1))
                .trace(eq(connection), anyLong(), eq("foo"), eq(Collections.emptyList()));

        verify(stmt, times(1))
                .executeQuery("foo");
    }

    @Test
    public void execute() throws Exception {
        this.target.execute("foo");

        verify(psl, times(1))
                .trace(eq(connection), anyLong(), eq("foo"), eq(Collections.emptyList()));
        verify(stmt, times(1))
                .execute("foo");
    }

    @Test
    public void testEquals() throws Exception {
        assertThat(this.target.equals(3)).isFalse();
        assertThat(this.target.equals(this.target)).isTrue();
    }

    @Test
    public void testHashCode() throws Exception {
        assertThat(this.target.hashCode()).isNotEqualTo(stmt.hashCode());
    }

    @Test
    public void getResultSet() throws Exception {
        when(stmt.getResultSet()).thenReturn(rs);
        ResultSet got = this.target.getResultSet();

        assertThat(got.toString())
                .contains("TracerResultSet");
    }

}