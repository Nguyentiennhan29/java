package com.judge.ai;
import com.judge.Config;
import com.judge.model.Problem;
import com.judge.util.Json;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.*;

/**
 * Giao tiếp với Google Gemini API qua Java 11+ HttpClient.
 * Không phụ thuộc thư viện ngoài.
 */
public class GeminiClient {

    // Gemini API endpoint (model name appended at call time)
    private static final String URL_BASE = "https://generativelanguage.googleapis.com/v1beta/models/";
    private static final String URL_SUFFIX = ":generateContent";

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    // ────────────────────────────────────
    // 1. Phân tích đề bài
    // ────────────────────────────────────
    public String analyzeProblem(Problem p) throws Exception {
        String prompt = """
Bạn là chuyên gia phân tích đề thi lập trình thi đấu IOI/ICPC.
Hãy phân tích bài toán sau và trả về JSON (CHÍNH XÁC cú pháp JSON, không thêm gì ngoài JSON):
{
  "title": "Tên bài toán",
  "problem_type": "IOI hoặc ICPC hoặc UNKNOWN",
  "algorithm_type": "DP hoặc GRAPH hoặc GREEDY hoặc MATH hoặc STRING hoặc GEOMETRY hoặc BRUTE_FORCE hoặc DATA_STRUCTURE hoặc OTHER",
  "time_limit_ms": 2000,
  "memory_mb": 256,
  "input_format": "Mô tả định dạng input",
  "output_format": "Mô tả định dạng output",
  "constraints": "Các ràng buộc (N, M, giá trị...)",
  "key_observations": "Nhận xét quan trọng về bài toán",
  "suggested_approach": "Hướng giải đề xuất chi tiết",
  "samples": [
    {"input": "...", "expected": "...", "note": "Giải thích"}
  ]
}

Đề bài:
""" + p.getStatement();

        return call(buildBody(prompt, p));
    }

    // ────────────────────────────────────
    // 2. Sinh test case
    // ────────────────────────────────────
    public String generateTestCases(Problem p, int count) throws Exception {
        String prompt = String.format("""
Sinh CHÍNH XÁC %d test case cho bài lập trình thi đấu sau. PHẢI có đủ %d phần tử trong JSON array, không được thiếu.
Thông tin bài:
- Tên: %s
- Input format: %s
- Output format: %s
- Ràng buộc: %s
- Thuật toán: %s

Trả về JSON array (CHỈ JSON array, không có gì khác, KHÔNG giải thích):
[
  {
    "id": 1,
    "category": "EDGE hoặc RANDOM hoặc SAMPLE",
    "input": "nội dung input",
    "expected": "output đúng",
    "note": "test case này kiểm tra điều gì"
  }
]

Yêu cầu BẮT BUỘC:
- PHẢI sinh đủ %d test case, đếm lại trước khi trả lời
- 20%% edge case (N=1, N nhỏ nhất, N lớn nhất, giá trị âm nếu có...)
- 50%% random (giá trị ngẫu nhiên hợp lệ trong ràng buộc)
- 30%% stress (input lớn gần max constraint)
- phải sinh testcase bị tle nếu bài có thuật toán O(N^2) hoặc hơn
- PHẢI điền đầy đủ "input" và "expected" cho MỌI test case
- Tính expected output CHÍNH XÁC theo logic bài
- Input/output đúng định dạng bài
- KHÔNG sinh test case nào có input rỗng hoặc expected rỗng
- KHÔNG dừng sớm, phải sinh đủ %d phần tử
""", count, count, p.getTitle(), p.getInputFormat(), p.getOutputFormat(),
                p.getConstraints(), p.getAlgorithmType(), count, count);

        return call(buildBodyTextLarge(prompt));
    }

    // ────────────────────────────────────
    // 3. Sinh code
    // ────────────────────────────────────
    public enum CodeKind { AC, WA, TLE }

    public String generateCode(Problem p, CodeKind kind, String lang) throws Exception {
        String desc = switch (kind) {
            case AC  -> "giải ĐÚNG hoàn toàn, tối ưu";
            case WA  -> "có bug tinh vi (sai trên một số case đặc biệt nhưng pass sample)";
            case TLE -> "đúng logic nhưng CHẬM (brute force, O(N^2) hoặc hơn)";
        };

        String prompt = String.format("""
Viết code %s bằng %s cho bài toán sau.

Tên bài: %s
Input: %s
Output: %s
Ràng buộc: %s
Hướng giải: %s

Yêu cầu:
- Viết code HOÀN CHỈNH, không được cắt bớt hay dừng giữa chừng
- Code phải compile và chạy được ngay, có đầy đủ hàm main và closing brace
- Dòng đầu tiên là comment: // CODE_TYPE: %s
- CHỈ trả về code, không giải thích thêm
""", desc, lang,
                p.getTitle(), p.getInputFormat(), p.getOutputFormat(),
                p.getConstraints(),
                p.getSuggestedApproach().isEmpty() ? p.getStatement() : p.getSuggestedApproach(),
                kind.name());

        return Json.stripFences(call(buildBodyText(prompt)));
    }

    // ────────────────────────────────────
    // HTTP helpers
    // ────────────────────────────────────

    private String buildBody(String prompt, Problem p) {
        if (p.hasImage()) {
            // Gemini inline image format — escape ALL string values with jsonStr()
            String mimeType  = jsonStr(p.getImageMediaType());
            String imageData = jsonStr(p.getImageBase64());
            String text      = jsonStr(prompt);
            return """
{"contents":[{"parts":[
{"inline_data":{"mime_type":%s,"data":%s}},
{"text":%s}
]}],"generationConfig":{"maxOutputTokens":8192}}""".formatted(mimeType, imageData, text);
        }
        return buildBodyText(prompt);
    }

    private String buildBodyText(String prompt) {
        // Gemini format: {"contents":[{"parts":[{"text":"..."}]}]}
        return """
{"contents":[{"parts":[{"text":%s}]}],"generationConfig":{"maxOutputTokens":8192}}
""".formatted(jsonStr(prompt));
    }

    private String buildBodyTextLarge(String prompt) {
        // Higher token limit for test case generation
        return """
{"contents":[{"parts":[{"text":%s}]}],"generationConfig":{"maxOutputTokens":8192}}
""".formatted(jsonStr(prompt));
    }

    /** Wrap a Java string as a JSON string literal */
    private String jsonStr(String s) {
        StringBuilder sb = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"'  -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default   -> sb.append(c);
            }
        }
        sb.append('"');
        return sb.toString();
    }

    private String call(String body) throws Exception {
        String key = Config.getInstance().getApiKey();
        if (key == null || key.isBlank())
            throw new Exception("API Key chưa được cấu hình!\nVào tab ⚙ Cài đặt để nhập API Key của Google AI Studio.");

        // Gemini: key passed as query param ?key=...
        String model = Config.getInstance().getModel();
        String url = URL_BASE + model + URL_SUFFIX + "?key=" + key;

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .timeout(Duration.ofSeconds(120))
                .build();

        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());

        if (resp.statusCode() != 200) {
            String rb = resp.body();
            try {
                @SuppressWarnings("unchecked")
                Map<String,Object> err = (Map<String,Object>) Json.parse(rb);
                @SuppressWarnings("unchecked")
                Map<String,Object> errObj = (Map<String,Object>) err.get("error");
                if (errObj != null) throw new Exception("API Error " + resp.statusCode() + ": " + errObj.get("message"));
            } catch (Exception e) {
                if (e.getMessage() != null && e.getMessage().startsWith("API Error")) throw e;
            }
            throw new Exception("API Error " + resp.statusCode() + ": " + rb);
        }

        // Parse Gemini response: candidates[0].content.parts[0].text
        @SuppressWarnings("unchecked")
        Map<String,Object> respObj = (Map<String,Object>) Json.parse(resp.body());
        @SuppressWarnings("unchecked")
        List<Object> candidates = (List<Object>) respObj.get("candidates");
        @SuppressWarnings("unchecked")
        Map<String,Object> candidate = (Map<String,Object>) candidates.get(0);
        @SuppressWarnings("unchecked")
        Map<String,Object> content = (Map<String,Object>) candidate.get("content");
        @SuppressWarnings("unchecked")
        List<Object> parts = (List<Object>) content.get("parts");
        @SuppressWarnings("unchecked")
        Map<String,Object> part = (Map<String,Object>) parts.get(0);
        String text = (String) part.get("text");
        return unescapeUnicode(text);
    }

    /** Convert \u003e style sequences from Gemini JSON to real characters */
    private String unescapeUnicode(String s) {
        if (s == null) return s;
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c == 92 && i + 5 < s.length() && s.charAt(i+1) == 'u') {
                String hex = s.substring(i+2, i+6);
                boolean allHex = true;
                for (char h : hex.toCharArray()) {
                    if (!((h>='0'&&h<='9')||(h>='a'&&h<='f')||(h>='A'&&h<='F'))) { allHex=false; break; }
                }
                if (allHex) {
                    sb.append((char) Integer.parseInt(hex, 16));
                    i += 6;
                    continue;
                }
            }
            sb.append(c);
            i++;
        }
        return sb.toString();
    }
}
