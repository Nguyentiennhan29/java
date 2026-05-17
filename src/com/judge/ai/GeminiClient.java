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

    private static final String URL_BASE   = "https://generativelanguage.googleapis.com/v1beta/models/";
    private static final String URL_SUFFIX = ":generateContent";

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    // ────────────────────────────────────
    // 1. Phân tích đề bài
    // ────────────────────────────────────
    public String analyzeProblem(Problem p) throws Exception {
        String stmtSection;
        if (p.getStatement() == null || p.getStatement().isBlank()) {
            stmtSection = p.hasImage()
                    ? "Đề bài nằm trong ảnh đính kèm. Hãy đọc toàn bộ nội dung từ ảnh và phân tích."
                    : "(Không có đề bài)";
        } else {
            stmtSection = "Đề bài:\n" + p.getStatement();
        }

        String prompt = """
Bạn là chuyên gia phân tích đề thi lập trình thi đấu IOI/ICPC.
Hãy phân tích bài toán sau và trả về JSON (CHÍNH XÁC cú pháp JSON, không thêm gì ngoài JSON):
{
  "title": "Tên bài toán",
  "problem_type": "IOI hoặc ICPC hoặc UNKNOWN",
  "algorithm_type": "DP hoặc GRAPH hoặc GREEDY hoặc MATH hoặc STRING hoặc GEOMETRY hoặc BRUTE_FORCE hoặc DATA_STRUCTURE hoặc OTHER",
  "time_limit_ms": 2000,
  "memory_mb": 256,
  "input_format": "Liệt kê từng dòng stdin theo thứ tự, dùng ký hiệu: 'Dòng 1: <nội dung>'. Chỉ mô tả những gì thực sự xuất hiện trong stdin — suy luận từ ví dụ mẫu của đề. Ví dụ đúng: 'Dòng 1: n số nguyên cách nhau bởi dấu cách. Dòng 2: target'. Ví dụ sai: 'nums = [1,2,3], target = 8' hoặc tự thêm dòng đọc n khi đề không yêu cầu.",
  "output_format": "Liệt kê từng dòng stdout theo thứ tự, dùng ký hiệu: 'Dòng 1: <nội dung>'. Chỉ mô tả những gì thực sự cần in ra stdout — suy luận từ ví dụ mẫu của đề. Ví dụ đúng: 'Dòng 1: hai số nguyên cách nhau bởi dấu cách'. Ví dụ sai: '[3,4]' hoặc '[-1,-1]' — không được dùng định dạng mảng/bracket trừ khi đề yêu cầu in đúng như vậy.",
  "constraints": "Các ràng buộc (N, M, giá trị...)",
  "key_observations": "Nhận xét quan trọng về bài toán",
  "suggested_approach": "Hướng giải đề xuất chi tiết",
  "samples": [
    {"input":  "stdin thuần túy, ví dụ: '5 7 7 8 8 10\n8' chứ không phải 'nums=[5,7,7,8,8,10], target=8'", "expected": "stdout thuần túy đúng format yêu cầu, ví dụ: '3 4' nếu output là hai số cách nhau bởi dấu cách. Không được tự thêm bracket hay format không có trong đề.", "note": "Giải thích"}
  ]
}

""" + stmtSection;

        return call(buildBody(prompt, p));
    }

    // ────────────────────────────────────
    // 2. Sinh test case
    // ────────────────────────────────────
    public String generateTestCases(Problem p, int count) throws Exception {
        String prompt = String.format("""
Bạn là chuyên gia sinh test case cho lập trình thi đấu. Nhiệm vụ: sinh CHÍNH XÁC %d test case.
PHẢI có đúng %d phần tử trong JSON array — đếm lại trước khi trả lời.

=== THÔNG TIN BÀI ===
- Tên: %s
- Input format: %s
- Output format: %s
- Ràng buộc: %s
- Loại thuật toán: %s

=== ĐỊNH DẠNG TRẢ VỀ ===
CHỈ trả về JSON array thuần túy, KHÔNG có markdown, KHÔNG có giải thích:
[
  {
    "id": 1,
    "category": "EDGE|RANDOM|STRESS|TLE_STRESS|ANTI_PATTERN",
    "input": "nội dung input HOÀN CHỈNH đúng định dạng",
    "expected": "output CHÍNH XÁC",
    "note": "mô tả mục đích test"
  }
]

=== PHÂN BỔ BẮT BUỘC (%d test case) ===
1. EDGE (15%%): Biên cực trị — N=1, N=0 nếu hợp lệ, N_max, giá trị min/max, mảng rỗng, một phần tử, tất cả phần tử bằng nhau, giá trị âm/dương biên.
2. RANDOM (35%%): Dữ liệu ngẫu nhiên hợp lệ nhiều kích thước khác nhau (nhỏ, trung, lớn). Không được sinh input trùng nhau.
3. STRESS (20%%): N/M sát max constraint, dữ liệu dày đặc, kiểm tra giới hạn bộ nhớ và tốc độ của giải đúng.
4. TLE_STRESS (15%%): Được thiết kế đặc biệt để phân biệt O(N log N) với O(N²).
5. ANTI_PATTERN (15%%): Bẫy các lỗi phổ biến — off-by-one, overflow, output format, corner case.

=== QUY TẮC CHẤT LƯỢNG ===
- PHẢI tính expected output CHÍNH XÁC — KHÔNG được đoán
- Input PHẢI đúng hoàn toàn định dạng bài (số dòng, khoảng cách, thứ tự)
- Input PHẢI là raw stdin thuần túy — TUYỆT ĐỐI KHÔNG được viết "s = ...", "n = ...", hay bất kỳ nhãn biến nào.
- Mỗi test case PHẢI khác nhau, không trùng input
- KHÔNG sinh test case có input hoặc expected rỗng
- PHẢI sinh đủ %d phần tử, KHÔNG dừng sớm
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

        // Quy tắc stdin/stdout theo ngôn ngữ
        String ioRule = switch (lang) {
            case "Java" -> """
- Dùng Scanner hoặc BufferedReader đọc từ System.in
- In kết quả ra System.out (println hoặc print)
- KHÔNG dùng JOptionPane, GUI, hay bất kỳ input nào khác
""";
            case "Python" -> """
- Dùng input() hoặc sys.stdin để đọc dữ liệu
- Dùng print() để in kết quả
- KHÔNG dùng input hardcoded hay vòng lặp in nhiều kết quả
""";
            default -> """
- Dùng cin / scanf để đọc dữ liệu từ stdin
- Dùng cout / printf để in kết quả ra stdout
- KHÔNG dùng clrscr(), getch(), system("pause"), conio.h, windows.h
- KHÔNG include thư viện không chuẩn
""";
        };

        String prompt = String.format("""
Viết code %s bằng %s cho bài toán sau.

=== THÔNG TIN BÀI ===
Tên bài: %s
Input format: %s
Output format: %s
Ràng buộc: %s
Hướng giải: %s

=== YÊU CẦU BẮT BUỘC VỀ I/O ===
Code PHẢI đọc input từ stdin và in output ra stdout theo đúng định dạng bài.
Chương trình chỉ xử lý MỘT test case duy nhất mỗi lần chạy (chương trình judge sẽ chạy nhiều lần).
%s
Output PHẢI KHÔNG có thêm bất kỳ dòng nào ngoài kết quả (ví dụ debug, log, giải thích đều KHÔNG ĐƯỢC IN ra).
Output PHẢI đúng CHÍNH XÁC như yêu cầu, KHÔNG được thêm dấu cách thừa, dòng trống, hay bất kỳ ký tự nào khác. ví dụ, nếu output yêu cầu là một số, code KHÔNG ĐƯỢC in "Kết quả: 42" mà phải in đúng "42".
và ví dụ code yêu cầu in "YES" hoặc "NO", code KHÔNG ĐƯỢC in "Output: YES" mà phải in đúng "YES". và output yêu cầu in "1 2 3", code KHÔNG ĐƯỢC in "1 2 3 " hay "1 2 3\n" mà phải in đúng "1 2 3". và output yêu cầu in {1,2,3}, code KHÔNG ĐƯỢC in "[1, 2, 3]" hay "1 2 3" hay "1,2,3" mà phải in đúng "{1,2,3}".
=== VÍ DỤ ĐÚNG (cấu trúc main) ===
// Với bài đọc số nguyên n và in kết quả:
// C++:   int main() { int n; cin >> n; cout << solve(n) << endl; return 0; }
// Java:  public static void main(String[] args) { Scanner sc = new Scanner(System.in); int n = sc.nextInt(); System.out.println(solve(n)); }
// Python: n = int(input()); print(solve(n))

=== VI DỤ SAI (TUYỆT ĐỐI KHÔNG LÀM) ===
// SAI — in hardcoded nhiều test case:
// cout << "Input: 123, Output: " << solve(123) << endl;
// cout << "Input: -123, Output: " << solve(-123) << endl;
// SAI — dùng thư viện không chuẩn: #include <conio.h>, clrscr(), getch()

=== YÊU CẦU CODE ===
- Dòng đầu tiên là comment: // CODE_TYPE: %s
- Code HOÀN CHỈNH, compile và chạy được ngay
- Có đầy đủ hàm main và closing brace
- CHỈ trả về code, KHÔNG giải thích thêm, KHÔNG markdown
""",
                desc, lang,
                p.getTitle(), p.getInputFormat(), p.getOutputFormat(),
                p.getConstraints(),
                !p.getSuggestedApproach().isBlank() ? p.getSuggestedApproach()
                        : (!p.getStatement().isBlank() ? p.getStatement()
                        : "Hãy tự suy luận hướng giải dựa trên tên bài, input/output và ràng buộc ở trên."),
                ioRule,
                kind.name());

        return Json.stripFences(call(buildBodyText(prompt)));
    }

    // ────────────────────────────────────
    // HTTP helpers
    // ────────────────────────────────────

    private String buildBody(String prompt, Problem p) {
        if (p.hasImage()) {
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
        return """
{"contents":[{"parts":[{"text":%s}]}],"generationConfig":{"maxOutputTokens":8192}}
""".formatted(jsonStr(prompt));
    }

    private String buildBodyTextLarge(String prompt) {
        return """
{"contents":[{"parts":[{"text":%s}]}],"generationConfig":{"maxOutputTokens":8192}}
""".formatted(jsonStr(prompt));
    }

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