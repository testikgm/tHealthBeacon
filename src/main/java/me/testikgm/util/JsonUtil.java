package me.testikgm.util;

import java.util.*;

public class JsonUtil {

    public static String escapeJson(String str) {
        if (str == null) return "null";
        
        StringBuilder sb = new StringBuilder();
        for (char c : str.toCharArray()) {
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }

    public static String mapToJson(Map<String, Object> map) {
        if (map == null || map.isEmpty()) {
            return "{}";
        }
        
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!first) sb.append(",");
            sb.append("\"").append(escapeJson(entry.getKey())).append("\":");
            sb.append(valueToJson(entry.getValue()));
            first = false;
        }
        sb.append("}");
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private static String valueToJson(Object value) {
        if (value == null) {
            return "null";
        } else if (value instanceof String) {
            return "\"" + escapeJson(value.toString()) + "\"";
        } else if (value instanceof Number || value instanceof Boolean) {
            return value.toString();
        } else if (value instanceof Map) {
            return mapToJson((Map<String, Object>) value);
        } else if (value instanceof List) {
            return listToJson((List<?>) value);
        } else if (value instanceof Object[]) {
            return listToJson(Arrays.asList((Object[]) value));
        } else {
            return "\"" + escapeJson(value.toString()) + "\"";
        }
    }

    private static String listToJson(List<?> list) {
        if (list == null || list.isEmpty()) {
            return "[]";
        }
        
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        for (Object item : list) {
            if (!first) sb.append(",");
            sb.append(valueToJson(item));
            first = false;
        }
        sb.append("]");
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    public static String toJson(Object obj) {
        if (obj instanceof Map) {
            return mapToJson((Map<String, Object>) obj);
        } else if (obj instanceof List) {
            return listToJson((List<?>) obj);
        } else {
            return valueToJson(obj);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T fromJson(String json, Class<T> clazz) {

        if (json == null || json.isEmpty()) {
            return null;
        }
        
        try {

            int contentIndex = json.indexOf("\"content\":\"");
            if (contentIndex > 0) {
                int start = contentIndex + 11;
                int end = json.indexOf("\"", start);
                if (end > start) {
                    String content = json.substring(start, end);

                    content = content.replace("\\n", "\n").replace("\\\"", "\"").replace("\\\\", "\\");
                    if (clazz == String.class) {
                        return (T) content;
                    }
                }
            }

            if (clazz == Map.class) {
                Map<String, Object> result = new HashMap<>();

                return (T) result;
            }
        } catch (Exception e) {

        }
        
        return null;
    }
}

