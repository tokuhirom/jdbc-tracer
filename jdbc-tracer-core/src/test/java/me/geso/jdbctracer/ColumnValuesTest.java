package me.geso.jdbctracer;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;


public class ColumnValuesTest {
    @Test
    public void testAll() throws Exception {
        ColumnValues columnValues = new ColumnValues();
        columnValues.put(1, "hoge");
        columnValues.put(2, "fuga");
        List<Object> values = columnValues.values();
        assertThat(values)
                .isEqualTo(Arrays.asList("hoge", "fuga"));
        columnValues.clear();
        assertThat(columnValues.values())
                .isEmpty();
    }

    @Test
    public void testToString() throws Exception {
        ColumnValues columnValues = new ColumnValues();
        assertThat(columnValues.toString())
                .isEqualTo("ColumnValues{columnValues={}}");
    }
}
