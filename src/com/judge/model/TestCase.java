package com.judge.model;

public class TestCase {
    private int    id;
    private String input    = "";
    private String expected = "";
    private String actual   = "";
    private String note     = "";
    private String category = "RANDOM";
    private String verdict  = "";
    private long   timeMs   = -1;

    public TestCase() {}
    public TestCase(int id, String input, String expected, String category) {
        this.id = id; this.input = input; this.expected = expected; this.category = category;
    }

    public int    getId()           { return id; }
    public void   setId(int v)      { id = v; }
    public String getInput()        { return input; }
    public void   setInput(String v){ input = v; }
    public String getExpected()     { return expected; }
    public void   setExpected(String v){ expected = v; }
    public String getActual()       { return actual; }
    public void   setActual(String v){ actual = v; }
    public String getNote()         { return note; }
    public void   setNote(String v) { note = v; }
    public String getCategory()     { return category; }
    public void   setCategory(String v){ category = v; }
    public String getVerdict()      { return verdict; }
    public void   setVerdict(String v){ verdict = v; }
    public long   getTimeMs()       { return timeMs; }
    public void   setTimeMs(long v) { timeMs = v; }
    private boolean sample; // Thêm biến này nếu chưa có

// Thêm hàm getter mà file TestCasePanel.java đang tìm kiếm:
public boolean isSample() {
    return sample;
}

// Thêm hàm setter nếu sau này cần dùng để gán giá trị:
public void setSample(boolean sample) {
    this.sample = sample;
}

    public boolean isCorrect() {
        if (expected == null || actual == null) return false;
        String[] el = expected.trim().split("\\r?\\n");
        String[] al = actual.trim().split("\\r?\\n");
        if (el.length != al.length) return false;
        for (int i = 0; i < el.length; i++)
            if (!el[i].trim().equals(al[i].trim())) return false;
        return true;
    }
}