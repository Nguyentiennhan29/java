package com.judge.core;

import com.judge.Config;

import javax.tools.*;
import java.io.*;
import java.nio.file.*;
import java.util.concurrent.*;

public class CompilerRunner {

    public enum Lang { CPP, JAVA, PYTHON }

    public static class Result {
        public enum Status { OK, WA, TLE, RE, CE }
        public Status status = Status.OK;
        public String output = "";
        public String error  = "";
        public long   timeMs = 0;
        public String compileError = "";
        @Override public String toString() { return status + " (" + timeMs + "ms)"; }
    }

    // ─── Compile ───

    public static String compile(String code, Lang lang, File dir) throws IOException {
        dir.mkdirs();
        return switch (lang) {
            case CPP    -> compileCpp(code, dir);
            case JAVA   -> compileJava(code, dir);
            case PYTHON -> { savePython(code, dir); yield ""; }
        };
    }

    private static String compileCpp(String code, File dir) throws IOException {
        Files.writeString(new File(dir, "sol.cpp").toPath(), code);
        String exe = new File(dir, isWin() ? "sol.exe" : "sol").getAbsolutePath();
        ProcessBuilder pb = new ProcessBuilder(
                Config.getInstance().getGppPath(), "-O2", "-std=c++17",
                "-o", exe, new File(dir, "sol.cpp").getAbsolutePath());
        pb.directory(dir).redirectErrorStream(true);
        Process p = pb.start();
        String out = new String(p.getInputStream().readAllBytes());
        try { return p.waitFor() == 0 ? "" : out; }
        catch (InterruptedException e) { return "interrupted"; }
    }

    private static String compileJava(String code, File dir) {
        // Rename class to Solution
        code = code.replaceAll("public\\s+class\\s+\\w+", "public class Solution");
        code = code.replaceAll("class\\s+Main\\b", "class Solution");
        try {
            Files.writeString(new File(dir, "Solution.java").toPath(), code);
            JavaCompiler jc = ToolProvider.getSystemJavaCompiler();
            if (jc == null) return "JDK not found (need JDK, not JRE)";
            DiagnosticCollector<JavaFileObject> dc = new DiagnosticCollector<>();
            try (StandardJavaFileManager fm = jc.getStandardFileManager(dc, null, null)) {
                Iterable<? extends JavaFileObject> units = fm.getJavaFileObjects(new File(dir, "Solution.java"));
                if (!jc.getTask(null, fm, dc, null, null, units).call()) {
                    StringBuilder sb = new StringBuilder();
                    for (var d : dc.getDiagnostics())
                        sb.append(d.getKind()).append(" L").append(d.getLineNumber())
                          .append(": ").append(d.getMessage(null)).append("\n");
                    return sb.toString();
                }
            }
            return "";
        } catch (IOException e) { return e.getMessage(); }
    }

    private static void savePython(String code, File dir) throws IOException {
        Files.writeString(new File(dir, "sol.py").toPath(), code);
    }

    // ─── Run ───

    public static Result run(Lang lang, File dir, String input, int timeoutMs) throws IOException {
        String[] cmd = switch (lang) {
            case CPP    -> new String[]{ new File(dir, isWin() ? "sol.exe" : "sol").getAbsolutePath() };
            case JAVA   -> new String[]{ "java", "-cp", dir.getAbsolutePath(), "Solution" };
            case PYTHON -> new String[]{ Config.getInstance().getPythonPath(), new File(dir,"sol.py").getAbsolutePath() };
        };
        return exec(cmd, input, dir, timeoutMs);
    }

    private static Result exec(String[] cmd, String input, File dir, int timeoutMs) throws IOException {
        Result res = new Result();
        long start = System.currentTimeMillis();

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(dir);
        Process proc = pb.start();

        // Feed stdin
        ExecutorService feed = Executors.newSingleThreadExecutor();
        feed.submit(() -> {
            try (OutputStream os = proc.getOutputStream()) {
                if (input != null && !input.isEmpty()) os.write(input.getBytes());
            } catch (IOException ignored) {}
        });
        feed.shutdown();

        // Read stdout/stderr
        ExecutorService io = Executors.newFixedThreadPool(2);
        Future<byte[]> out = io.submit(() -> proc.getInputStream().readAllBytes());
        Future<byte[]> err = io.submit(() -> proc.getErrorStream().readAllBytes());

        try {
            boolean fin = proc.waitFor(timeoutMs, TimeUnit.MILLISECONDS);
            res.timeMs = System.currentTimeMillis() - start;
            if (!fin) {
                proc.destroyForcibly();
                res.status = Result.Status.TLE;
                res.error  = "TLE (" + timeoutMs + "ms)";
            } else {
                res.output = new String(out.get(2, TimeUnit.SECONDS));
                res.error  = new String(err.get(2, TimeUnit.SECONDS));
                res.status = proc.exitValue() == 0 ? Result.Status.OK : Result.Status.RE;
            }
        } catch (Exception e) {
            proc.destroyForcibly();
            res.status = Result.Status.RE;
            res.error  = e.getMessage();
            res.timeMs = System.currentTimeMillis() - start;
        } finally {
            io.shutdownNow();
        }
        return res;
    }

    // ─── Checker ───

    public static String runChecker(Lang lang, File checkerDir,
            String inData, String expData, String actData) throws IOException {
        File fi = File.createTempFile("ci", ".txt"); fi.deleteOnExit();
        File fe = File.createTempFile("ce", ".txt"); fe.deleteOnExit();
        File fa = File.createTempFile("ca", ".txt"); fa.deleteOnExit();
        Files.writeString(fi.toPath(), inData);
        Files.writeString(fe.toPath(), expData);
        Files.writeString(fa.toPath(), actData);

        String[] cmd = switch (lang) {
            case CPP    -> new String[]{ new File(checkerDir, isWin()?"checker.exe":"checker").getAbsolutePath(),
                                         fi.getAbsolutePath(), fe.getAbsolutePath(), fa.getAbsolutePath() };
            case JAVA   -> new String[]{ "java", "-cp", checkerDir.getAbsolutePath(), "Checker",
                                         fi.getAbsolutePath(), fe.getAbsolutePath(), fa.getAbsolutePath() };
            case PYTHON -> new String[]{ Config.getInstance().getPythonPath(),
                                         new File(checkerDir,"checker.py").getAbsolutePath(),
                                         fi.getAbsolutePath(), fe.getAbsolutePath(), fa.getAbsolutePath() };
        };
        return exec(cmd, "", checkerDir, 5000).output.trim();
    }

    // ─── Utils ───

    public static boolean isWin() {
        return System.getProperty("os.name","").toLowerCase().contains("win");
    }

    public static Lang fromStr(String s) {
        return switch (s.toUpperCase()) {
            case "JAVA"             -> Lang.JAVA;
            case "PYTHON","PYTHON3" -> Lang.PYTHON;
            default                 -> Lang.CPP;
        };
    }

    public static File tmpDir(String prefix) throws IOException {
        File d = Files.createTempDirectory(prefix).toFile();
        d.deleteOnExit();
        return d;
    }
}