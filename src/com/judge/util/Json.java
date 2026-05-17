package com.judge.util;

import java.util.*;

/**
 * Minimal JSON parser – chỉ dùng Java thuần, đủ để parse kết quả từ Claude API.
 * Hỗ trợ: String, Number, Boolean, null, Object (Map), Array (List).
 */
public class Json {

    public static Object parse(String src) {
        return new Parser(src.trim()).parseValue();
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> obj(Object o) { return (Map<String, Object>) o; }

    @SuppressWarnings("unchecked")
    public static List<Object> arr(Object o) { return (List<Object>) o; }

    public static String str(Map<String,Object> m, String key, String def) {
        Object v = m == null ? null : m.get(key);
        return v != null ? v.toString() : def;
    }

    public static int num(Map<String,Object> m, String key, int def) {
        Object v = m == null ? null : m.get(key);
        if (v == null) return def;
        try { return ((Number)v).intValue(); } catch (Exception e) { return def; }
    }

    /** Strip markdown code fences from AI response */
    public static String stripFences(String text) {
        if (text == null) return "";
        text = text.trim();
        if (text.startsWith("```")) {
            int nl = text.indexOf('\n');
            if (nl > 0) text = text.substring(nl + 1);
            if (text.endsWith("```")) text = text.substring(0, text.lastIndexOf("```"));
        }
        return text.trim();
    }

    // ─── Parser ───

    private static class Parser {
        final String s;
        int i;
        Parser(String s) { this.s = s; }

        Object parseValue() {
            skip();
            if (i >= s.length()) return null;
            char c = s.charAt(i);
            if (c == '{') return parseObject();
            if (c == '[') return parseArray();
            if (c == '"') return parseString();
            if (c == 't') { i+=4; return Boolean.TRUE; }
            if (c == 'f') { i+=5; return Boolean.FALSE; }
            if (c == 'n') { i+=4; return null; }
            return parseNumber();
        }

        Map<String,Object> parseObject() {
            Map<String,Object> m = new LinkedHashMap<>();
            i++; // {
            skip();
            while (i < s.length() && s.charAt(i) != '}') {
                skip();
                String key = parseString();
                skip();
                if (i < s.length() && s.charAt(i) == ':') i++;
                skip();
                Object val = parseValue();
                m.put(key, val);
                skip();
                if (i < s.length() && s.charAt(i) == ',') i++;
                skip();
            }
            if (i < s.length()) i++; // }
            return m;
        }

        List<Object> parseArray() {
            List<Object> list = new ArrayList<>();
            i++; // [
            skip();
            while (i < s.length() && s.charAt(i) != ']') {
                list.add(parseValue());
                skip();
                if (i < s.length() && s.charAt(i) == ',') i++;
                skip();
            }
            if (i < s.length()) i++; // ]
            return list;
        }

        String parseString() {
            i++; // opening "
            StringBuilder sb = new StringBuilder();
            while (i < s.length()) {
                char c = s.charAt(i++);
                if (c == '"') break;
                if (c == '\\' && i < s.length()) {
                    char e = s.charAt(i++);
                    switch (e) {
                        case 'n' -> sb.append('\n');
                        case 'r' -> sb.append('\r');
                        case 't' -> sb.append('\t');
                        case '"' -> sb.append('"');
                        case '\\' -> sb.append('\\');
                        default -> { sb.append('\\'); sb.append(e); }
                    }
                } else sb.append(c);
            }
            return sb.toString();
        }

        Number parseNumber() {
            int start = i;
            while (i < s.length() && "0123456789.-+eE".indexOf(s.charAt(i)) >= 0) i++;
            String ns = s.substring(start, i);
            try { return Long.parseLong(ns); }
            catch (Exception e) { try { return Double.parseDouble(ns); } catch (Exception e2) { return 0; } }
        }

        void skip() {
            while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++;
        }
    }
}