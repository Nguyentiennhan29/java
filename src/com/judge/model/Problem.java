package com.judge.model;

import java.util.*;

public class Problem {
    private String title = "";
    private String statement = "";
    private String imageBase64 = null;
    private String imageMediaType = null;
    private String inputFormat = "";
    private String outputFormat = "";
    private String constraints = "";
    private String analysis = "";
    private String suggestedApproach = "";
    private String algorithmType = "OTHER";
    private String problemType = "UNKNOWN";
    private int    timeLimitMs = 2000;
    private int    memoryMb   = 256;
    private String language   = "C++";
    private String acCode  = "";
    private String waCode  = "";
    private String tleCode = "";
    private String checkerCode = "";
    private List<TestCase> testCases = new ArrayList<>();
    private List<TestCase> samples   = new ArrayList<>();

    public String getTitle()        { return title; }
    public void   setTitle(String v){ title = v; }
    public String getStatement()        { return statement; }
    public void   setStatement(String v){ statement = v; }
    public String getImageBase64()        { return imageBase64; }
    public void   setImageBase64(String v){ imageBase64 = v; }
    public String getImageMediaType()        { return imageMediaType; }
    public void   setImageMediaType(String v){ imageMediaType = v; }
    public boolean hasImage() { return imageBase64 != null && !imageBase64.isEmpty(); }

    /**
     * Bài toán "sẵn sàng" để sinh code nếu:
     * - Có text đề bài, HOẶC
     * - Có ảnh đề, HOẶC
     * - Đã được phân tích AI (có title/inputFormat)
     */
    public boolean isReady() {
        return !statement.isBlank()
            || hasImage()
            || !title.isBlank()
            || !inputFormat.isBlank();
    }
    public String getInputFormat()        { return inputFormat; }
    public void   setInputFormat(String v){ inputFormat = v; }
    public String getOutputFormat()        { return outputFormat; }
    public void   setOutputFormat(String v){ outputFormat = v; }
    public String getConstraints()        { return constraints; }
    public void   setConstraints(String v){ constraints = v; }
    public String getAnalysis()        { return analysis; }
    public void   setAnalysis(String v){ analysis = v; }
    public String getSuggestedApproach()        { return suggestedApproach; }
    public void   setSuggestedApproach(String v){ suggestedApproach = v; }
    public String getAlgorithmType()        { return algorithmType; }
    public void   setAlgorithmType(String v){ algorithmType = v; }
    public String getProblemType()        { return problemType; }
    public void   setProblemType(String v){ problemType = v; }
    public int    getTimeLimitMs()    { return timeLimitMs; }
    public void   setTimeLimitMs(int v){ timeLimitMs = v; }
    public int    getMemoryMb()       { return memoryMb; }
    public void   setMemoryMb(int v)  { memoryMb = v; }
    public String getLanguage()        { return language; }
    public void   setLanguage(String v){ language = v; }
    public String getAcCode()          { return acCode; }
    public void   setAcCode(String v)  { acCode = v; }
    public String getWaCode()          { return waCode; }
    public void   setWaCode(String v)  { waCode = v; }
    public String getTleCode()         { return tleCode; }
    public void   setTleCode(String v) { tleCode = v; }
    public String getCheckerCode()         { return checkerCode; }
    public void   setCheckerCode(String v) { checkerCode = v; }
    public List<TestCase> getTestCases()           { return testCases; }
    public void           setTestCases(List<TestCase> v){ testCases = v; }
    public List<TestCase> getSamples()             { return samples; }
    public void           setSamples(List<TestCase> v){ samples = v; }
}