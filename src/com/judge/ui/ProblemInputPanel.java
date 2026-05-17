package com.judge.ui;

import com.judge.ai.GeminiClient;
import com.judge.model.Problem;
import com.judge.model.TestCase;
import com.judge.util.Json;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.dnd.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.List;

public class ProblemInputPanel extends JPanel {

    private final Problem   problem;
    private final MainFrame frame;
    private final JTextArea stmtArea;
    private final JLabel    imgLabel;
    private final JLabel    imgInfo;
    private final JComboBox<String> langCombo;
    private final JProgressBar      progress;

    public ProblemInputPanel(Problem problem, MainFrame frame) {
        this.problem = problem; this.frame = frame;
        setLayout(new BorderLayout(8, 8));
        setBorder(new EmptyBorder(12, 12, 12, 12));

        // ── LEFT: text ──
        stmtArea = new JTextArea(
                "-- Dán đề bài vào đây --\n\nVí dụ:\n" +
                "Cho mảng N phần tử nguyên, tìm tổng dãy con liên tiếp lớn nhất.\n" +
                "Input: N rồi N số nguyên.\nOutput: Tổng lớn nhất.\nRàng buộc: 1≤N≤10^5.");
        stmtArea.setFont(new Font("Monospaced", Font.PLAIN, 13));
        stmtArea.setLineWrap(true); stmtArea.setWrapStyleWord(true);
        JScrollPane textScroll = new JScrollPane(stmtArea);

        JPanel textTop = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        for (String[] b : new String[][]{{"Xoa","CLEAR"},{"Dan","PASTE"}}) {
            JButton btn = new JButton(b[0]);
            btn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            switch (b[1]) {
                case "CLEAR" -> btn.addActionListener(e -> stmtArea.setText(""));
                case "PASTE" -> btn.addActionListener(e -> stmtArea.paste());
            }
            textTop.add(btn);
        }
        JPanel leftPanel = new JPanel(new BorderLayout(4,4));
        leftPanel.setBorder(section("De bai (Text)"));
        leftPanel.add(textTop, BorderLayout.NORTH);
        leftPanel.add(textScroll, BorderLayout.CENTER);

        // ── RIGHT: image ──
        imgLabel = new JLabel("<html><center>Kéo thả ảnh vào đây<br>hoặc nhấn nút bên dưới</center></html>",
                SwingConstants.CENTER);
        imgLabel.setPreferredSize(new Dimension(360, 300));
        imgLabel.setOpaque(true);
        imgLabel.setBackground(new Color(245, 248, 252));
        imgLabel.setBorder(BorderFactory.createDashedBorder(Color.GRAY, 4, 4));
        imgLabel.setForeground(Color.GRAY);

        imgInfo = new JLabel(" ", SwingConstants.CENTER);
        imgInfo.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        imgInfo.setForeground(new Color(100,120,180));

        JPanel imgBtns = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
        JButton uploadBtn = btn("Chon anh");
        JButton pasteBtn  = btn("Dan anh");
        JButton clearBtn  = btn("Xoa anh");
        imgBtns.add(uploadBtn); imgBtns.add(pasteBtn); imgBtns.add(clearBtn);
        uploadBtn.addActionListener(e -> chooseImg());
        pasteBtn.addActionListener(e  -> pasteImg());
        clearBtn.addActionListener(e  -> clearImg());

        JPanel rightPanel = new JPanel(new BorderLayout(4,4));
        rightPanel.setBorder(section("De bai (Anh / Screenshot)"));
        rightPanel.add(imgLabel, BorderLayout.CENTER);
        rightPanel.add(imgInfo,  BorderLayout.SOUTH);

        JPanel rightWrap = new JPanel(new BorderLayout(4,4));
        rightWrap.add(rightPanel, BorderLayout.CENTER);
        rightWrap.add(imgBtns,   BorderLayout.SOUTH);

        // drag-drop
        new DropTarget(imgLabel, new DropTargetAdapter() {
            @Override public void drop(DropTargetDropEvent e) {
                try {
                    e.acceptDrop(DnDConstants.ACTION_COPY);
                    @SuppressWarnings("unchecked")
                    List<File> files = (List<File>) e.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
                    if (!files.isEmpty()) loadImgFile(files.get(0));
                } catch (Exception ex) { err(ex.getMessage()); }
            }
        });

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightWrap);
        split.setResizeWeight(0.62); split.setDividerSize(6);

        // ── OPTIONS ──
        JPanel opts = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 4));
        opts.setBorder(section("Tuy chon"));
        opts.add(new JLabel("Ngôn ngữ:"));
        langCombo = new JComboBox<>(new String[]{"C++","Java","Python"});
        langCombo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        opts.add(langCombo);
        JLabel hint = new JLabel("  Co the ket hop text + anh de AI phan tich tot hon");
        hint.setForeground(new Color(100,100,200));
        hint.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        opts.add(hint);

        JPanel center = new JPanel(new BorderLayout(4,4));
        center.add(opts, BorderLayout.NORTH);
        center.add(split, BorderLayout.CENTER);

        // ── ACTION ──
        JButton analyzeBtn = new JButton("  Phan tich de voi AI  ");
        analyzeBtn.setFont(new Font("Segoe UI", Font.BOLD, 15));
        analyzeBtn.setBackground(new Color(30, 130, 70));
        analyzeBtn.setForeground(Color.WHITE);
        analyzeBtn.setFocusPainted(false);
        analyzeBtn.setPreferredSize(new Dimension(320, 44));

        progress = new JProgressBar();
        progress.setStringPainted(true); progress.setString("");
        progress.setPreferredSize(new Dimension(220, 24));

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 6));
        bottom.add(analyzeBtn); bottom.add(progress);

        analyzeBtn.addActionListener(e -> doAnalyze(analyzeBtn));

        add(center, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);
    }

    // ──────────────────────────────────────────────

    private void doAnalyze(JButton btn) {
        String text = stmtArea.getText().trim();
        if (text.isEmpty() && !problem.hasImage()) {
            err("Vui lòng nhập đề bài (text hoặc ảnh) trước."); return;
        }
        problem.setStatement(text);
        problem.setLanguage((String) langCombo.getSelectedItem());

        btn.setEnabled(false);
        progress.setIndeterminate(true);
        progress.setString("Đang gửi đề tới Gemini AI...");

        new SwingWorker<Void,String>() {
            @Override protected Void doInBackground() throws Exception {
                String json = new GeminiClient().analyzeProblem(problem);
                publish("Đang xử lý kết quả...");
                parseResult(Json.stripFences(json));
                return null;
            }
            @Override protected void process(List<String> c) { progress.setString(c.get(c.size()-1)); }
            @Override protected void done() {
                btn.setEnabled(true);
                progress.setIndeterminate(false);
                try {
                    get();
                    progress.setString("Phan tich xong!");
                    frame.getAnalysisPanel().refresh();
                    frame.getTestCasePanel().refreshFromProblem();
                    frame.goAnalysis();
                } catch (Exception ex) {
                    progress.setString("Loi");
                    err("Lỗi phân tích:\n" + rootMsg(ex));
                }
            }
        }.execute();
    }

    @SuppressWarnings("unchecked")
    private void parseResult(String json) {
        try {
            Map<String,Object> obj = (Map<String,Object>) Json.parse(json);
            problem.setTitle(Json.str(obj, "title", "Bài toán"));
            problem.setInputFormat(Json.str(obj, "input_format", ""));
            problem.setOutputFormat(Json.str(obj, "output_format", ""));
            problem.setConstraints(Json.str(obj, "constraints", ""));
            problem.setAnalysis(Json.str(obj, "key_observations", ""));
            problem.setSuggestedApproach(Json.str(obj, "suggested_approach", ""));
            problem.setAlgorithmType(Json.str(obj, "algorithm_type", "OTHER").toUpperCase());
            problem.setProblemType(Json.str(obj, "problem_type", "UNKNOWN").toUpperCase());
            problem.setTimeLimitMs(Json.num(obj, "time_limit_ms", 2000));
            problem.setMemoryMb(Json.num(obj, "memory_mb", 256));

            List<TestCase> samples = new ArrayList<>();
            Object sarr = obj.get("samples");
            if (sarr instanceof List<?> list) {
                int idx = 1;
                for (Object el : list) {
                    Map<String,Object> tc = (Map<String,Object>) el;
                    TestCase t = new TestCase(idx++,
                            Json.str(tc,"input",""), Json.str(tc,"expected",""), "SAMPLE");
                    t.setNote(Json.str(tc,"note",""));
                    samples.add(t);
                }
            }
            problem.setSamples(samples);
        } catch (Exception e) {
            problem.setAnalysis(json); // store raw if parse fails
        }
    }

    // ── Image ──

    private void chooseImg() {
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new FileNameExtensionFilter("Ảnh","png","jpg","jpeg","bmp"));
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) loadImgFile(fc.getSelectedFile());
    }

    private void loadImgFile(File f) {
        try {
            BufferedImage img = ImageIO.read(f);
            if (img == null) { err("Không đọc được ảnh."); return; }
            showImg(img, f.getName());
            encodeImg(img, Files.probeContentType(f.toPath()));
        } catch (IOException e) { err("Lỗi: " + e.getMessage()); }
    }

    private void pasteImg() {
        try {
            Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
            if (cb.isDataFlavorAvailable(DataFlavor.imageFlavor)) {
                Image img = (Image) cb.getData(DataFlavor.imageFlavor);
                BufferedImage bi = toBuf(img);
                showImg(bi, "clipboard");
                encodeImg(bi, "image/png");
            } else err("Clipboard không có ảnh. Hãy chụp màn hình trước.");
        } catch (Exception e) { err("Lỗi dán ảnh: " + e.getMessage()); }
    }

    private void clearImg() {
        imgLabel.setIcon(null);
        imgLabel.setText("<html><center>Kéo thả ảnh vào đây<br>hoặc nhấn nút bên dưới</center></html>");
        imgInfo.setText(" ");
        problem.setImageBase64(null);
        problem.setImageMediaType(null);
    }

    private void showImg(BufferedImage img, String name) {
        int mw = Math.max(imgLabel.getWidth()-8, 320);
        int mh = Math.max(imgLabel.getHeight()-8, 280);
        double sc = Math.min((double)mw/img.getWidth(), (double)mh/img.getHeight());
        ImageIcon ic = new ImageIcon(img.getScaledInstance((int)(img.getWidth()*sc),(int)(img.getHeight()*sc), Image.SCALE_SMOOTH));
        imgLabel.setIcon(ic); imgLabel.setText("");
        imgInfo.setText("%s  (%dx%d)".formatted(name, img.getWidth(), img.getHeight()));
    }

    private void encodeImg(BufferedImage img, String mime) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(img, (mime!=null&&mime.contains("png"))?"PNG":"JPEG", baos);
            problem.setImageBase64(java.util.Base64.getEncoder().encodeToString(baos.toByteArray()));
            problem.setImageMediaType(mime != null ? mime : "image/png");
        } catch (IOException e) { err("Lỗi encode ảnh: " + e.getMessage()); }
    }

    private BufferedImage toBuf(Image img) {
        if (img instanceof BufferedImage bi) return bi;
        BufferedImage bi = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = bi.createGraphics(); g.drawImage(img,0,0,null); g.dispose();
        return bi;
    }

    private JButton btn(String t) {
        JButton b = new JButton(t); b.setFont(new Font("Segoe UI",Font.PLAIN,12)); return b;
    }
    private TitledBorder section(String t) {
        return BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(180,200,220),1), t,
                TitledBorder.LEFT, TitledBorder.TOP,
                new Font("Segoe UI",Font.BOLD,12), new Color(50,80,160));
    }
    private void err(String m) { JOptionPane.showMessageDialog(this, m,"Lỗi",JOptionPane.ERROR_MESSAGE); }
    private String rootMsg(Throwable t) { while(t.getCause()!=null) t=t.getCause(); return t.getMessage()!=null?t.getMessage():t.toString(); }
}