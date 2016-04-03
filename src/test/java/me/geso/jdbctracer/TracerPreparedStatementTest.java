package me.geso.jdbctracer;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.lang.reflect.Proxy;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class TracerPreparedStatementTest {
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
                PreparedStatement.class,
                stmt,
                "SELECT * FROM foo",
                psl,
                rsl
        );
    }

    @Test
    public void executeQuery() throws Exception {
        when(stmt.executeQuery()).thenReturn(rs);
        this.target.setInt(1, 5963);
        ResultSet rs = this.target.executeQuery();
        assertThat(rs.toString())
                .contains("TracerResultSet");

        verify(psl, times(1))
                .trace(anyLong(), eq("SELECT * FROM foo"), eq(Collections.singletonList(5963)));

        verify(stmt, times(1))
                .executeQuery();
    }

    @Test
    public void execute() throws Exception {
        this.target.execute();

        verify(psl, times(1))
                .trace(anyLong(), eq("SELECT * FROM foo"), eq(Collections.emptyList()));
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

}