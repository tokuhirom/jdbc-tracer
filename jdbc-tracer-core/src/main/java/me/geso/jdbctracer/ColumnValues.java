package me.geso.jdbctracer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

class ColumnValues {
    private final Map<Integer, Object> columnValues;

    ColumnValues() {
        columnValues = new HashMap<>();
    }

    void put(Integer key, Object value) {
        columnValues.put(key, value);
    }

    void clear() {
        columnValues.clear();
    }

    @Override
    public String toString() {
        return "ColumnValues{" +
                "columnValues=" + columnValues +
                '}';
    }

    List<Object> values() {
        return columnValues.entrySet()
                .stream()
                .sorted((a, b) -> a.getKey() - b.getKey())
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());
    }

}
