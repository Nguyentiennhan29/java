# Competitive Programming Judge — AI TestCase Generator

Công cụ hỗ trợ **ra đề thi lập trình thi đấu** (IOI / ICPC / Codeforces) bằng AI.  
Tự động phân tích đề, sinh test case, sinh code AC/WA/TLE và chạy kiểm tra — tất cả trong một giao diện.

---

## Yêu cầu hệ thống

| Thành phần | Phiên bản tối thiểu |
|---|---|
| Java (JDK) | 17 trở lên |
| g++ (GCC) | 11 trở lên (nếu dùng C++) |
| Python | 3.8 trở lên (nếu dùng Python) |
| Hệ điều hành | Windows 10/11, macOS, Linux |

---

## Cài đặt môi trường

### 1. Cài Java (JDK 17+)

### 2. Cài g++ (để chạy code C++)

### 3. Lấy Gemini API Key (miễn phí)

> API Key miễn phí có giới hạn số request/phút. Nếu bị lỗi 429 thì chờ vài giây rồi thử lại.

---

## Biên dịch và chạy dự án

### Cách 1: Dùng IDE (IntelliJ IDEA — khuyến nghị) Hoặc dùng VSCode

1. Mở IntelliJ IDEA → **Open** → chọn thư mục `testjava`
2. Đảm bảo Project SDK là **Java 17**:
   - File → Project Structure → Project → SDK → chọn 17
3. Nhấn chuột phải vào `Main.java` → **Run 'Main.main()'**
---
### Nếu dùng VSCode

1. Mở VSCode → **Open Folder** → chọn thư mục `testjava`
2. Mở file `Main.java`, đợi extension Java tải xong
3. Bấm vào nút **Run** xuất hiện phía trên dòng:
```java

### Cách 2: Biên dịch thủ công bằng Command Line

**Bước 1:** Di chuyển vào thư mục project:
```bash
cd testjava
```

**Bước 2:** Tạo thư mục output:
```bash
mkdir -p out
```

**Bước 3:** Biên dịch toàn bộ source:

*Windows (CMD):*
```cmd
javac -encoding UTF-8 -d out -sourcepath src ^
  src\com\judge\Main.java ^
  src\com\judge\Config.java ^
  src\com\judge\model\*.java ^
  src\com\judge\util\*.java ^
  src\com\judge\core\*.java ^
  src\com\judge\ai\*.java ^
  src\com\judge\ui\*.java
```

*macOS / Linux:*
```bash
javac -encoding UTF-8 -d out -sourcepath src \
  src/com/judge/Main.java \
  src/com/judge/Config.java \
  src/com/judge/model/*.java \
  src/com/judge/util/*.java \
  src/com/judge/core/*.java \
  src/com/judge/ai/*.java \
  src/com/judge/ui/*.java
```

**Bước 4:** Chạy chương trình:
```bash
java -cp out com.judge.Main
```

---

### Cách 3: Đóng gói thành file JAR (chạy 1 click)

Sau khi biên dịch xong (bước 1-3 ở trên):

```bash
jar cfe CJudge.jar com.judge.Main -C out .
```

Chạy:
```bash
java -jar CJudge.jar
```

---

## Cấu hình lần đầu

Khi mở chương trình lần đầu, vào tab **Cài đặt** để thiết lập:

| Mục | Mô tả |
|---|---|
| **API Key** | Dán Gemini API Key vào đây |
| **Model** | Mặc định `gemini-2.0-flash` (nhanh, miễn phí). Dùng `gemini-2.5-pro` để kết quả tốt hơn |
| **g++ path** | Đường dẫn tới g++ nếu không nhận tự động (ví dụ `C:\mingw64\bin\g++.exe`) |
| **Python path** | Đường dẫn tới python nếu cần (ví dụ `python` hoặc `python3`) |

Nhấn **Lưu cài đặt** sau khi điền xong. Cấu hình được lưu tại `~/.cj_config.properties`.

---

## Hướng dẫn sử dụng nhanh

### Quy trình chuẩn (5 bước):

```
Nhập đề  →  Phân tích  →  Test Cases  →  Sinh Code  →  Chạy & Kiểm tra
```

**1. Tab Nhập đề**
- Dán đề bài vào ô text bên trái, hoặc
- Upload ảnh chụp đề bài (hỗ trợ PNG, JPG)
- Chọn ngôn ngữ (C++ / Java / Python)
- Nhấn **Phân tích de voi AI**

**2. Tab Phân tích**
- Xem kết quả AI phân tích: input/output format, ràng buộc, thuật toán gợi ý
- Kiểm tra lại test mẫu từ đề

**3. Tab Test Cases**
- Nhấn **Sinh (AI)** để tự động sinh test cases (mặc định 10 test)
- Có thể chỉnh số lượng, thêm/sửa/xóa thủ công
- Nhấn **Tu de mau** để thêm test mẫu từ đề vào

**4. Tab Sinh Code**
- Nhấn **AC Code** — code giải đúng
- Nhấn **WA Code** — code có bug tinh vi (để test)
- Nhấn **TLE Code** — code đúng nhưng chậm
- Hoặc nhấn ** Sinh Tất Cả** để sinh cả 3 cùng lúc

**5. Tab Chạy & Kiểm tra**
- Chọn loại code muốn chạy (AC / WA / TLE)
- Nhấn **Chay Tat Ca**
- Click vào từng dòng để xem **Expected vs Actual output** chi tiết
- Nhấn **Kiem tra do manh** để đánh giá bộ test có đủ mạnh không

---

## Cấu trúc thư mục
testjava/
└── src/
    └── com/judge/
        ├── Main.java              # Entry point
        ├── Config.java            # Quản lý cấu hình
        ├── ai/
        │   └── GeminiClient.java  # Gọi Google Gemini API
        ├── core/
        │   ├── CompilerRunner.java # Biên dịch & chạy code
        │   └── TestValidator.java  # So sánh output
        ├── model/
        │   ├── Problem.java        # Model bài toán
        │   └── TestCase.java       # Model test case
        ├── ui/
        │   ├── MainFrame.java      # Cửa sổ chính
        │   ├── ProblemInputPanel.java
        │   ├── AnalysisPanel.java
        │   ├── TestCasePanel.java
        │   └── OtherPanels.java    # CodeGen + Execution + Settings
        └── util/
            └── Json.java           # JSON parser đơn giản

## Các lỗi thường gặp

| Lỗi | Nguyên nhân | Cách sửa |
|---|---|---|
| `API Key chưa được cấu hình` | Chưa nhập key | Vào tab Cài đặt → nhập API Key |
| `API Error 429` | Vượt giới hạn request | Chờ 10-30 giây rồi thử lại |
| `Cannot run program "g++"` | g++ không có trong PATH | Cài MinGW và thêm vào PATH, hoặc điền đường dẫn đầy đủ trong Cài đặt |
| Code sinh ra không đầy đủ | Token limit | Thử model `gemini-2.5-pro` |
| Sinh 10 TC nhưng chỉ ra 3-5 | API trả thiếu | Bấm Sinh lại, hoặc tăng số lượng lên 15 để bù |

