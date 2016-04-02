package me.geso.jdbctracer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ColumnValues {
    private Map<Integer, Object> columnValues = new HashMap<>();

    protected void put(Integer key, Object value) {
        columnValues.put(key, value);
    }

    protected void clear() {
        columnValues.clear();
    }

    protected List<Object> values() {
        return columnValues.entrySet()
                .stream()
                .sorted((a, b) -> a.getKey() - b.getKey())
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());
    }

}
