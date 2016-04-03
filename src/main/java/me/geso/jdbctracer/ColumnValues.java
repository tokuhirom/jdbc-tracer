package me.geso.jdbctracer;

import lombok.ToString;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@ToString
class ColumnValues {
    private Map<Integer, Object> columnValues = new HashMap<>();

    void put(Integer key, Object value) {
        columnValues.put(key, value);
    }

    void clear() {
        columnValues.clear();
    }

    List<Object> values() {
        return columnValues.entrySet()
                .stream()
                .sorted((a, b) -> a.getKey() - b.getKey())
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());
    }

}
