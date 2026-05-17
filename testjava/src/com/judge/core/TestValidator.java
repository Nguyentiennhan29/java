package com.judge.core;
import com.judge.model.TestCase;
import java.io.File;
import java.util.*;
import java.util.function.Consumer;

public class TestValidator {

    public static class Report {
        public String  codeType     = "";
        public String  compileError = "";
        public boolean compiled     = false;
        public int     total, ac, wa, tle, re;
        public long    avgMs, maxMs;
        public List<TestCase> results = new ArrayList<>();

        public boolean passAll() { return compiled && ac == total; }
        public String summary() {
            if (!compiled) return "COMPILE ERROR";
            return "AC:%d/%d  WA:%d  TLE:%d  RE:%d  avg:%dms  max:%dms"
                    .formatted(ac, total, wa, tle, re, avgMs, maxMs);
        }
    }

    public static Report validate(String code, String langStr, String codeType,
            List<TestCase> tcs, int timeoutMs,
            boolean useChecker, String checkerCode,
            Consumer<String> log) throws Exception {

        Report rep = new Report();
        rep.codeType = codeType;
        rep.total    = tcs.size();

        CompilerRunner.Lang lang = CompilerRunner.fromStr(langStr);
        File workDir = CompilerRunner.tmpDir("judge_");

        if (log != null) log.accept("Biên dịch " + codeType + " [" + langStr + "]...");
        String ce = CompilerRunner.compile(code, lang, workDir);
        if (!ce.isEmpty()) { rep.compileError = ce; return rep; }
        rep.compiled = true;

        // Optional checker
        File checkerDir = null;
        if (useChecker && checkerCode != null && !checkerCode.isBlank()) {
            try {
                checkerDir = CompilerRunner.tmpDir("chk_");
                String che = CompilerRunner.compile(checkerCode, lang, checkerDir);
                if (!che.isEmpty()) { checkerDir = null; if (log != null) log.accept("⚠ Checker CE, dùng so sánh mặc định"); }
            } catch (Exception e) { checkerDir = null; }
        }

        long sumMs = 0, maxMs = 0;
        for (int i = 0; i < tcs.size(); i++) {
            TestCase tc = tcs.get(i);
            if (log != null) log.accept("Chạy TC #%d/%d [%s]...".formatted(i+1, tcs.size(), tc.getCategory()));

         CompilerRunner.Result res = CompilerRunner.run(lang, workDir, tc.getInput(), timeoutMs);
            tc.setActual(res.output);
            tc.setTimeMs(res.timeMs);

            String v;
            if (res.status == CompilerRunner.Result.Status.TLE) { v = "TLE"; rep.tle++; }
            else if (res.status == CompilerRunner.Result.Status.RE) { v = "RE"; rep.re++; }
            else {
                boolean correct;
                if (checkerDir != null) {
                    try {
                        String cr = CompilerRunner.runChecker(lang, checkerDir, tc.getInput(), tc.getExpected(), res.output);
                        correct = cr.startsWith("AC");
                    } catch (Exception e) { correct = tc.isCorrect(); }
                } else { correct = tc.isCorrect(); }

                if (correct) { v = "AC"; rep.ac++; } else { v = "WA"; rep.wa++; }
            }
            tc.setVerdict(v);
            rep.results.add(tc);
            sumMs += res.timeMs;
            maxMs = Math.max(maxMs, res.timeMs);
        }
        rep.avgMs = tcs.isEmpty() ? 0 : sumMs / tcs.size();
        rep.maxMs = maxMs;
        return rep;
    }

    /** Stress test: find input where AC ≠ WA */
    public static TestCase findCounterExample(String acCode, String waCode,
            String langStr, List<TestCase> candidates, int timeoutMs) throws Exception {

        CompilerRunner.Lang lang = CompilerRunner.fromStr(langStr);
        File acDir = CompilerRunner.tmpDir("ac_");
        File waDir = CompilerRunner.tmpDir("wa_");
        CompilerRunner.compile(acCode, lang, acDir);
        CompilerRunner.compile(waCode, lang, waDir);

        for (TestCase tc : candidates) {
            CompilerRunner.Result ar = CompilerRunner.run(lang, acDir, tc.getInput(), timeoutMs);
            CompilerRunner.Result wr = CompilerRunner.run(lang, waDir, tc.getInput(), timeoutMs);
            if (ar.status == CompilerRunner.Result.Status.OK
                    && wr.status == CompilerRunner.Result.Status.OK) {
                tc.setExpected(ar.output);
                tc.setActual(wr.output);
                if (!norm(ar.output).equals(norm(wr.output))) {
                    tc.setVerdict("COUNTER_EXAMPLE");
                    return tc;
                }
            }
        }
        return null;
    }

    private static String norm(String s) {
        if (s == null) return "";
        return Arrays.stream(s.trim().split("\\r?\\n"))
                .map(String::trim).reduce("", (a,b) -> a+"\n"+b).trim();
    }
} 
