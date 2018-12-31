package net.dnjo;

import java.util.Map;
import java.util.TreeMap;

public class MapUtils {
    @SuppressWarnings("unchecked")
    public static TreeMap<String, Object> buildCaseInsensitiveMap(final Map map) {
        final TreeMap<String, Object> treeMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        treeMap.putAll(map);
        return treeMap;
    }
}
