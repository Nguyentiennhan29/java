package com.judge.ui;

import com.judge.ai.GeminiClient;
import com.judge.core.TestValidator;
import com.judge.model.Problem;
import com.judge.model.TestCase;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.io.*;
import java.nio.file.*;
import java.time.*;
import java.time.format.*;
import java.util.*;
import java.util.List;

// ════════════════════════════════════════════════════════════════
// CodeGenPanel
// ════════════════════════════════════════════════════════════════
class CodeGenPanel extends JPanel {
    private final Problem   problem;
    private final MainFrame frame;
    private final JTextArea acArea, waArea, tleArea;
    private final JComboBox<String> langCombo;
    private final JProgressBar progress;

    CodeGenPanel(Problem problem, MainFrame frame) {
        this.problem = problem; this.frame = frame;
        setLayout(new BorderLayout(6,6));
        setBorder(new EmptyBorder(10,10,10,10));

        // Toolbar
        JPanel tb = new JPanel(new FlowLayout(FlowLayout.LEFT,6,4));
        tb.setBorder(section("Sinh Code bang AI"));
        tb.add(new JLabel("Ngôn ngữ:"));
        langCombo = new JComboBox<>(new String[]{"C++","Java","Python"});
        langCombo.setFont(new Font("Segoe UI",Font.PLAIN,13));
        tb.add(langCombo);

        JButton acBtn  = cbtn("AC Code", new Color(0,140,0));
        JButton waBtn  = cbtn("WA Code", new Color(200,30,30));
        JButton tleBtn = cbtn("TLE Code", new Color(180,110,0));
        JButton allBtn = cbtn("🚀 Sinh Tất Cả", new Color(30,80,160));
        tb.add(acBtn); tb.add(waBtn); tb.add(tleBtn); tb.add(Box.createHorizontalStrut(8)); tb.add(allBtn);

        progress = new JProgressBar(); progress.setStringPainted(true);
        progress.setPreferredSize(new Dimension(200,20)); tb.add(progress);

        acBtn.addActionListener(e  -> gen(GeminiClient.CodeKind.AC));
        waBtn.addActionListener(e  -> gen(GeminiClient.CodeKind.WA));
        tleBtn.addActionListener(e -> gen(GeminiClient.CodeKind.TLE));
        allBtn.addActionListener(e -> genAll());

        // Tabs with editors
        acArea = codeArea(); waArea = codeArea(); tleArea = codeArea();
        JTabbedPane ct = new JTabbedPane();
        ct.addTab("AC Code",  wrapCode(acArea,  "AC",  new Color(0,180,0)));
        ct.addTab("WA Code",  wrapCode(waArea,  "WA",  Color.RED));
        ct.addTab("TLE Code", wrapCode(tleArea, "TLE", new Color(200,130,0)));

        // Nav
        JPanel nav = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton next = cbtn("Sang Chay & Kiem tra", new Color(30,100,200));
        nav.add(next);
        next.addActionListener(e -> { sync(); frame.goExec(); });

        add(tb,  BorderLayout.NORTH);
        add(ct,  BorderLayout.CENTER);
        add(nav, BorderLayout.SOUTH);
    }

    private void gen(GeminiClient.CodeKind kind) {
        if (!problem.isReady()) { warn("Chưa có đề bài. Vui lòng nhập text hoặc ảnh rồi bấm Phân tích trước."); return; }
        String lang = (String) langCombo.getSelectedItem();
        progress.setIndeterminate(true); progress.setString("Đang sinh " + kind.name() + "...");
        new SwingWorker<String,Void>() {
            @Override protected String doInBackground() throws Exception {
                return new GeminiClient().generateCode(problem, kind, lang);
            }
            @Override protected void done() {
                progress.setIndeterminate(false);
                try {
                    String code = get();
                    JTextArea a = switch(kind){ case AC->acArea; case WA->waArea; case TLE->tleArea; };
                    a.setText(code); a.setCaretPosition(0);
                    sync(); // lưu ngay vào problem sau mỗi lần sinh
                    progress.setString("Da sinh " + kind.name());
                } catch (Exception ex) { progress.setString("Loi"); warn(ex.getMessage()); }
            }
        }.execute();
    }

    private void genAll() {
        if (!problem.isReady()) { warn("Chưa có đề bài. Vui lòng nhập text hoặc ảnh rồi bấm Phân tích trước."); return; }
        String lang = (String) langCombo.getSelectedItem();
        progress.setIndeterminate(true); progress.setString("Sinh tất cả...");
        new SwingWorker<Void,String>() {
            @Override protected Void doInBackground() throws Exception {
                GeminiClient cl = new GeminiClient();
                publish("Đang sinh AC..."); acArea.setText(cl.generateCode(problem, GeminiClient.CodeKind.AC, lang));
                publish("Đang sinh WA..."); waArea.setText(cl.generateCode(problem, GeminiClient.CodeKind.WA, lang));
                publish("Đang sinh TLE..."); tleArea.setText(cl.generateCode(problem, GeminiClient.CodeKind.TLE, lang));
                return null;
            }
            @Override protected void process(List<String> c) { progress.setString(c.get(c.size()-1)); }
            @Override protected void done() {
                progress.setIndeterminate(false);
                try { get(); sync(); progress.setString("Da sinh tat ca!"); }
                catch (Exception ex) { progress.setString("Loi"); warn(ex.getMessage()); }
            }
        }.execute();
    }

    private void sync() {
        problem.setAcCode(acArea.getText()); problem.setWaCode(waArea.getText());
        problem.setTleCode(tleArea.getText());
        problem.setLanguage((String) langCombo.getSelectedItem());
    }

    private JTextArea codeArea() {
        JTextArea a = new JTextArea(); a.setFont(new Font("Monospaced",Font.PLAIN,13));
        a.setTabSize(4); a.setBackground(new Color(30,30,40)); a.setForeground(new Color(210,230,210));
        a.setCaretColor(Color.WHITE); a.setSelectionColor(new Color(80,120,200)); return a;
    }

    private JPanel wrapCode(JTextArea a, String lbl, Color c) {
        JPanel p = new JPanel(new BorderLayout(2,2));
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT,4,2));
        JLabel l = new JLabel("● " + lbl); l.setForeground(c); l.setFont(new Font("Segoe UI",Font.BOLD,12));
        top.add(l);
        JButton copy = btn("Copy"); JButton save = btn("Luu"); JButton clr = btn("Xoa");
        top.add(copy); top.add(save); top.add(clr);
        copy.addActionListener(e -> { a.selectAll(); a.copy(); a.select(0,0); });
        save.addActionListener(e -> saveCode(a, lbl.toLowerCase()));
        clr.addActionListener(e -> a.setText(""));
        p.add(top, BorderLayout.NORTH);
        p.add(new JScrollPane(a), BorderLayout.CENTER); return p;
    }

    private void saveCode(JTextArea a, String prefix) {
        String ext = switch ((String) langCombo.getSelectedItem()) {
            case "Java" -> ".java"; case "Python" -> ".py"; default -> ".cpp";
        };
        JFileChooser fc = new JFileChooser(); fc.setSelectedFile(new File(prefix + ext));
        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION)
            try { Files.writeString(fc.getSelectedFile().toPath(), a.getText()); }
            catch (IOException e) { warn(e.getMessage()); }
    }

    private JButton cbtn(String t, Color bg) {
        JButton b = new JButton(t); b.setFont(new Font("Segoe UI",Font.BOLD,12));
        b.setBackground(bg); b.setForeground(Color.WHITE); b.setFocusPainted(false); return b;
    }
    private JButton btn(String t) { JButton b=new JButton(t); b.setFont(new Font("Segoe UI",Font.PLAIN,11)); return b; }
    private TitledBorder section(String t) {
        return BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(180,200,220),1),t,
                TitledBorder.LEFT,TitledBorder.TOP,new Font("Segoe UI",Font.BOLD,12),new Color(50,80,160));
    }
    private void warn(String m) { JOptionPane.showMessageDialog(this,m,"Cảnh báo",JOptionPane.WARNING_MESSAGE); }
}


// ════════════════════════════════════════════════════════════════
// ExecutionPanel
// ════════════════════════════════════════════════════════════════
class ExecutionPanel extends JPanel {
    private final Problem   problem;
    private final MainFrame frame;
    private final JCheckBox acCb, waCb, tleCb;
    private final JProgressBar progress;
    private final JLabel   statusLbl;
    private final DefaultTableModel model;
    private final JTable   table;
    private final JTextArea logArea;
    private final JTextArea detailArea;
    // store last run results for click lookup
    private final Map<Integer, Map<String,TestCase>> lastResults = new LinkedHashMap<>();

    private static final String[] COLS = {"TC#","Loại","AC","AC ms","WA","WA ms","TLE","TLE ms","Input"};

    ExecutionPanel(Problem problem, MainFrame frame) {
        this.problem = problem; this.frame = frame;
        setLayout(new BorderLayout(6,6));
        setBorder(new EmptyBorder(10,10,10,10));

        JPanel tb = new JPanel(new FlowLayout(FlowLayout.LEFT,6,4));
        tb.setBorder(section("Tuy chon chay"));
        acCb  = new JCheckBox("AC",  true);
        waCb  = new JCheckBox("WA",  true);
        tleCb = new JCheckBox("TLE", true);
        tb.add(new JLabel("Chạy:")); tb.add(acCb); tb.add(waCb); tb.add(tleCb);
        tb.add(new JSeparator(SwingConstants.VERTICAL));

        JButton runBtn = cbtn("Chay Tat Ca", new Color(0,140,0));
        tb.add(runBtn);

        progress  = new JProgressBar(0,100); progress.setStringPainted(true);
        progress.setPreferredSize(new Dimension(220,20)); tb.add(progress);
        statusLbl = new JLabel("  Sẵn sàng"); statusLbl.setFont(new Font("Segoe UI",Font.BOLD,12)); tb.add(statusLbl);

        runBtn.addActionListener(e -> runAll());

        model = new DefaultTableModel(COLS, 0) { @Override public boolean isCellEditable(int r,int c){return false;} };
        table = new JTable(model);
        table.setFont(new Font("Segoe UI",Font.PLAIN,12)); table.setRowHeight(22);
        table.getTableHeader().setFont(new Font("Segoe UI",Font.BOLD,12));
        table.setDefaultRenderer(Object.class, new VR());
        int[] w={40,70,80,60,80,60,80,60,200};
        for(int i=0;i<w.length;i++) table.getColumnModel().getColumn(i).setPreferredWidth(w[i]);

        // Detail panel - shows expected vs actual on row click
        detailArea = new JTextArea(5, 80); detailArea.setEditable(false);
        detailArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        detailArea.setBackground(new Color(20,20,30)); detailArea.setForeground(new Color(220,220,180));
        detailArea.setText("← Click vào một dòng để xem Expected vs Actual output");
        JScrollPane detailSc = new JScrollPane(detailArea);
        detailSc.setBorder(section("Chi tiet (Expected vs Actual)"));

        table.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) return;
            int row = table.getSelectedRow();
            if (row < 0 || lastResults.isEmpty()) return;
            Object idObj = model.getValueAt(row, 0);
            if (idObj == null) return;
            int id = (int) idObj;
            Map<String,TestCase> rowMap = lastResults.get(id);
            if (rowMap == null) { detailArea.setText("Không có dữ liệu."); return; }
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String,TestCase> e2 : rowMap.entrySet()) {
                TestCase tc = e2.getValue();
                sb.append("══ ").append(e2.getKey()).append(" code — TC#").append(id)
                  .append(" [").append(tc.getVerdict()).append("] ══\n");
                sb.append("INPUT:\n").append(tc.getInput().trim()).append("\n\n");
                sb.append("EXPECTED:\n").append(tc.getExpected().trim()).append("\n\n");
                sb.append("ACTUAL:\n").append(
                    (tc.getActual() == null || tc.getActual().isBlank()) ? "(trống)" : tc.getActual().trim()
                ).append("\n\n");
            }
            detailArea.setText(sb.toString());
            detailArea.setCaretPosition(0);
        });

        logArea = new JTextArea(5, 80); logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced",Font.PLAIN,12));
        logArea.setBackground(new Color(30,30,30)); logArea.setForeground(new Color(0,220,0));
        JScrollPane logSc = new JScrollPane(logArea); logSc.setBorder(section("Log"));

        // 3-way vertical split: table | detail | log
        JSplitPane topSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JScrollPane(table), detailSc);
        topSplit.setResizeWeight(0.5);
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, topSplit, logSc);
        split.setResizeWeight(0.75);

        add(tb,    BorderLayout.NORTH);
        add(split, BorderLayout.CENTER);
        add(buildStrengthPanel(), BorderLayout.SOUTH);
    }

    private JPanel buildStrengthPanel() {
        JPanel p = new JPanel(new BorderLayout(6, 4));
        p.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(180,200,220),1),
            "Kiem tra bo test du manh chua?",
            javax.swing.border.TitledBorder.LEFT,
            javax.swing.border.TitledBorder.TOP,
            new Font("Segoe UI", Font.BOLD, 12),
            new Color(50,80,160)));
        p.setBackground(new Color(245,248,255));

        JTextArea resultArea = new JTextArea(3, 60);
        resultArea.setEditable(false);
        resultArea.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        resultArea.setBackground(new Color(245,248,255));
        resultArea.setText("Nhan 'Kiem tra' sau khi chay xong de biet bo test co du manh phat hien loi hay khong.");
        resultArea.setLineWrap(true);
        resultArea.setWrapStyleWord(true);

        JButton checkBtn = new JButton("Kiem tra do manh");
        checkBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        checkBtn.setBackground(new Color(30,100,200));
        checkBtn.setForeground(Color.WHITE);
        checkBtn.setFocusPainted(false);

        checkBtn.addActionListener(e -> {
            java.util.List<com.judge.model.TestCase> all = frame.getTestCasePanel().getAll();
            if (all.isEmpty()) {
                resultArea.setText("[!] Chua co test case nao. Hay sinh test truoc.");
                return;
            }
            long acCount  = all.stream().filter(t -> "AC".equals(t.getVerdict())).count();
            long waCount  = all.stream().filter(t -> "WA".equals(t.getVerdict())).count();
            long tleCount = all.stream().filter(t -> "TLE".equals(t.getVerdict())).count();
            long reCount  = all.stream().filter(t -> "RE".equals(t.getVerdict())).count();
            long total    = all.size();
            long ran      = all.stream().filter(t -> t.getVerdict() != null && !t.getVerdict().isEmpty()).count();

            if (ran == 0) {
                resultArea.setText("[!] Chua chay test case nao. Hay bam Chay Tat Ca truoc.");
                return;
            }

            StringBuilder sb = new StringBuilder();
            sb.append("Tong: ").append(total).append(" test  |  Da chay: ").append(ran)
              .append("  |  AC: ").append(acCount).append("  WA: ").append(waCount)
              .append("  TLE: ").append(tleCount).append("  RE: ").append(reCount).append("\n");

            if (waCount > 0 || tleCount > 0) {
                sb.append("[MANH] Bo test DA phat hien loi:\n");
                if (waCount  > 0) sb.append("  - Phat hien ").append(waCount).append(" test WA (code sai bi bat).\n");
                if (tleCount > 0) sb.append("  - Phat hien ").append(tleCount).append(" test TLE (code cham bi bat).\n");
                sb.append("  => Bo test du manh de kiem tra chuong trinh.");
            } else if (acCount == ran) {
                sb.append("[YEU] Tat ca code deu pass het bo test!\n");
                sb.append("  => Bo test CHUA du manh: WA code va TLE code cung pass.\n");
                sb.append("  => Goi y: Sinh them test EDGE CASE va STRESS TEST co gia tri lon hon.");
            } else {
                sb.append("[?] Khong du thong tin. Hay chay tat ca code (AC + WA + TLE) roi kiem tra lai.");
            }
            resultArea.setText(sb.toString());
        });

        p.add(checkBtn, BorderLayout.WEST);
        p.add(new JScrollPane(resultArea), BorderLayout.CENTER);
        return p;
    }

    private void runAll() {
        List<TestCase> tcs = frame.getTestCasePanel().getAll();
        if (tcs.isEmpty()) { warn("Chưa có test case. Hãy sinh test case trước."); return; }

        Map<String,String> codes = new LinkedHashMap<>();
        if (acCb.isSelected()  && !problem.getAcCode().isBlank())  codes.put("AC",  problem.getAcCode());
        if (waCb.isSelected()  && !problem.getWaCode().isBlank())  codes.put("WA",  problem.getWaCode());
        if (tleCb.isSelected() && !problem.getTleCode().isBlank()) codes.put("TLE", problem.getTleCode());
        if (codes.isEmpty()) { warn("Chưa có code. Hãy sinh code ở tab Sinh Code trước."); return; }

        int timeout   = Math.max(problem.getTimeLimitMs()*2, 4000);

        model.setRowCount(0); logArea.setText(""); progress.setValue(0); statusLbl.setText("  Đang chạy...");

        new SwingWorker<Map<String,TestValidator.Report>, String>() {
            @Override protected Map<String,TestValidator.Report> doInBackground() throws Exception {
                Map<String,TestValidator.Report> reps = new LinkedHashMap<>();
                int tot = codes.size(), done = 0;
                for (Map.Entry<String,String> e : codes.entrySet()) {
                    publish("🔄 Chạy " + e.getKey() + "...");
                    TestValidator.Report r = TestValidator.validate(e.getValue(), problem.getLanguage(),
                            e.getKey(), new ArrayList<>(tcs), timeout, false, "",
                            msg -> publish(msg));
                    reps.put(e.getKey(), r); done++;
                    final int pct = done*100/tot;
                    SwingUtilities.invokeLater(() -> progress.setValue(pct));
                    publish(e.getKey() + ": " + r.summary());
                }
                return reps;
            }
            @Override protected void process(List<String> c) { for(String m:c) log(m); }
            @Override protected void done() {
                try {
                    Map<String,TestValidator.Report> reps = get();
                    buildTable(tcs, reps);
                    statusLbl.setText("  Hoan tat!"); progress.setValue(100);
                    frame.getReportPanel().addReports(problem.getTitle(), reps);
                } catch (Exception ex) { statusLbl.setText("  Loi"); log("Loi: " + ex.getMessage()); }
            }
        }.execute();
    }

    private void buildTable(List<TestCase> tcs, Map<String,TestValidator.Report> reps) {
        model.setRowCount(0);
        lastResults.clear();
        detailArea.setText("← Click vào một dòng để xem Expected vs Actual output");
        Map<Integer,Map<String,TestCase>> byId = new LinkedHashMap<>();
        for (Map.Entry<String,TestValidator.Report> e : reps.entrySet())
            for (TestCase tc : e.getValue().results)
                byId.computeIfAbsent(tc.getId(), k -> new LinkedHashMap<>()).put(e.getKey(), tc);
        lastResults.putAll(byId);

        for (TestCase orig : tcs) {
            Map<String,TestCase> row = byId.getOrDefault(orig.getId(), new HashMap<>());
            TestCase ac=row.get("AC"), wa=row.get("WA"), tle=row.get("TLE");
            model.addRow(new Object[]{orig.getId(), orig.getCategory(),
                    ac!=null?ac.getVerdict():"—", ac!=null?ac.getTimeMs()+"ms":"—",
                    wa!=null?wa.getVerdict():"—", wa!=null?wa.getTimeMs()+"ms":"—",
                    tle!=null?tle.getVerdict():"—", tle!=null?tle.getTimeMs()+"ms":"—",
                    prev(orig.getInput())});
        }
    }

    private void log(String m) { SwingUtilities.invokeLater(() -> { logArea.append(m+"\n"); logArea.setCaretPosition(logArea.getDocument().getLength()); }); }
    private String prev(String s) { if(s==null)return""; String r=s.replace("\n","↵").replace("\r",""); return r.length()>50?r.substring(0,47)+"...":r; }
    private JButton cbtn(String t,Color bg) {
        JButton b=new JButton(t); b.setFont(new Font("Segoe UI",Font.BOLD,12));
        b.setBackground(bg); b.setForeground(Color.WHITE); b.setFocusPainted(false); return b;
    }
    private TitledBorder section(String t) {
        return BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(180,200,220),1),t,
                TitledBorder.LEFT,TitledBorder.TOP,new Font("Segoe UI",Font.BOLD,12),new Color(50,80,160));
    }
    private void warn(String m) { JOptionPane.showMessageDialog(this,m,"Cảnh báo",JOptionPane.WARNING_MESSAGE); }

    private static class VR extends DefaultTableCellRenderer {
        @Override public Component getTableCellRendererComponent(JTable t,Object v,boolean sel,boolean foc,int row,int col){
            Component c=super.getTableCellRendererComponent(t,v,sel,foc,row,col);
            if((col==2||col==4||col==6)&&v!=null) switch(v.toString()){
                case "AC"->c.setForeground(new Color(0,140,0)); case "WA"->c.setForeground(Color.RED);
                case "TLE"->c.setForeground(new Color(180,100,0)); case "RE"->c.setForeground(new Color(120,0,180));
                default->c.setForeground(sel?t.getSelectionForeground():t.getForeground());
            } else c.setForeground(sel?t.getSelectionForeground():t.getForeground());
            return c;
        }
    }
}


// ════════════════════════════════════════════════════════════════
// ReportPanel
// ════════════════════════════════════════════════════════════════
class ReportPanel extends JPanel {
    private final JTextArea area;
    private final StringBuilder data = new StringBuilder();
    private int cnt = 0;

    ReportPanel(MainFrame frame) {
        setLayout(new BorderLayout(6,6));
        setBorder(new EmptyBorder(10,10,10,10));

        area = new JTextArea(); area.setEditable(false);
        area.setFont(new Font("Monospaced",Font.PLAIN,13));
        area.setBackground(new Color(252,253,255));
        area.setText(empty());

        JPanel top = new JPanel(new BorderLayout());
        top.setBorder(section("Bao cao Ket qua Danh gia"));
        JLabel title = new JLabel("  Evaluation Report");
        title.setFont(new Font("Segoe UI",Font.BOLD,14)); title.setForeground(new Color(30,80,160));
        top.add(title, BorderLayout.WEST);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT,4,2));
        JButton clr = new JButton("Xoa"); JButton exp = new JButton("Xuat .txt");
        btns.add(clr); btns.add(exp); top.add(btns, BorderLayout.EAST);

        clr.addActionListener(e -> { data.setLength(0); cnt=0; area.setText(empty()); });
        exp.addActionListener(e -> exportTxt());

        add(top,  BorderLayout.NORTH);
        add(new JScrollPane(area), BorderLayout.CENTER);
    }

    public void addReports(String problemTitle, Map<String,TestValidator.Report> reports) {
        cnt++;
        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        StringBuilder sb = new StringBuilder();
        sb.append("╔══════════════════════════════════════════════════════════════╗\n");
        sb.append("║  ĐỀ #").append(cnt).append(": ").append(trunc(problemTitle, 54)).append("║\n");
        sb.append("║  ").append(ts).append("                          ║\n");
        sb.append("╠══════════════════════════════════════════════════════════════╣\n");

        for (Map.Entry<String,TestValidator.Report> e : reports.entrySet()) {
            String ct = e.getKey(); TestValidator.Report r = e.getValue();
            String icon = switch(ct){ case "AC"->"[AC]"; case "WA"->"[WA]"; case "TLE"->"[TLE]"; default->"[-]"; };
            sb.append("║  ").append(icon).append(" ").append(ct).append(" Code [").append(r.codeType).append("]\n");
            if (!r.compiled) {
                sb.append("║    → COMPILE ERROR:\n");
                for (String l : r.compileError.split("\n")) sb.append("║      ").append(trunc(l,58)).append("\n");
            } else {
                sb.append("║    → ").append(r.summary()).append("\n");
                sb.append("║    → Đánh giá: ");
                if ("AC".equals(ct))       sb.append(r.passAll()   ? "Code AC pass toàn bộ [OK]" : "AC con loi!");
                else if ("WA".equals(ct))  sb.append(r.wa > 0      ? "WA bi phat hien - TC du manh" : "WA code pass toan bo! TC chua manh");
                else if ("TLE".equals(ct)) sb.append(r.tle > 0     ? "TLE bi phat hien" : "TLE khong bi phat hien");
                sb.append("\n║\n");
            }
        }
        sb.append("╚══════════════════════════════════════════════════════════════╝\n\n");
        data.append(sb);
        area.setText(data.toString()); area.setCaretPosition(0);
    }

    private void exportTxt() {
        if (data.isEmpty()) { JOptionPane.showMessageDialog(this,"Chưa có báo cáo.","Thông báo",JOptionPane.INFORMATION_MESSAGE); return; }
        JFileChooser fc = new JFileChooser(); fc.setSelectedFile(new File("report_" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + ".txt"));
        if (fc.showSaveDialog(this)==JFileChooser.APPROVE_OPTION)
            try { Files.writeString(fc.getSelectedFile().toPath(), data.toString()); JOptionPane.showMessageDialog(this,"Đã xuất báo cáo!"); }
            catch (IOException e) { JOptionPane.showMessageDialog(this,"Lỗi: "+e.getMessage(),"Lỗi",JOptionPane.ERROR_MESSAGE); }
    }

    private String empty() {
        return """
╔══════════════════════════════════════════════════════════════╗
║         COMPETITIVE PROGRAMMING JUDGE — REPORT              ║
║                                                              ║
║  Hướng dẫn:                                                  ║
║  1. [Nhập đề]    → Nhập text hoặc ảnh đề bài               ║
║  2. [Phân tích]  → AI phân tích đề tự động                 ║
║  3. [Test Cases] → Sinh test case                           ║
║  4. [Sinh Code]  → Sinh code AC / WA / TLE                 ║
║  5. [Chạy]       → Biên dịch & kiểm tra                    ║
║  (Xem kết quả ngay trong tab này)                          ║
║                                                              ║
║   Cấu hình API Key tại tab Cài đặt trước!                 ║
╚══════════════════════════════════════════════════════════════╝
""";
    }
    private String trunc(String s, int max) {
        if (s==null) s=""; s=s.replace("\n"," ");
        return s.length()<=max ? s+" ".repeat(max-s.length()) : s.substring(0,max-3)+"...";
    }
    private TitledBorder section(String t) {
        return BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(180,200,220),1),t,
                TitledBorder.LEFT,TitledBorder.TOP,new Font("Segoe UI",Font.BOLD,12),new Color(50,80,160));
    }
}


// ════════════════════════════════════════════════════════════════
// SettingsPanel
// ════════════════════════════════════════════════════════════════
class SettingsPanel extends JPanel {
    private final JPasswordField apiKey;
    private final JComboBox<String> modelCb;
    private final JSpinner timeout;
    private final JTextField gpp, python;
    private final JLabel status;

    SettingsPanel() {
        setLayout(new BorderLayout(8,8));
        setBorder(new EmptyBorder(16,16,16,16));

        com.judge.Config cfg = com.judge.Config.getInstance();

        JPanel api = new JPanel(new GridBagLayout());
        api.setBorder(section("Google Gemini API"));
        GridBagConstraints g = new GridBagConstraints(); g.insets=new Insets(6,8,6,8); g.anchor=GridBagConstraints.WEST; g.fill=GridBagConstraints.HORIZONTAL;

        g.gridx=0; g.gridy=0; g.weightx=0; api.add(bold("API Key:"), g);
        g.gridx=1; g.weightx=1; apiKey=new JPasswordField(cfg.getApiKey(),40); apiKey.setFont(new Font("Monospaced",Font.PLAIN,13)); api.add(apiKey, g);
        g.gridx=2; g.weightx=0; JButton eye=new JButton("👁"); eye.addActionListener(e-> apiKey.setEchoChar(apiKey.getEchoChar()==0?'•':'\0')); api.add(eye,g);

        g.gridx=0; g.gridy=1; api.add(bold("Model:"),g);
        g.gridx=1; g.gridwidth=2; modelCb=new JComboBox<>(new String[]{"gemini-2.0-flash","gemini-2.0-flash-lite","gemini-2.5-flash","gemini-2.5-pro"});
        modelCb.setSelectedItem(cfg.getModel()); modelCb.setFont(new Font("Segoe UI",Font.PLAIN,13)); api.add(modelCb,g);

        g.gridx=0; g.gridy=2; g.gridwidth=3;
        api.add(new JLabel("<html><i style='color:gray'>Lấy API Key tại: https://aistudio.google.com/app/apikey</i></html>"),g);

        JPanel exec = new JPanel(new GridBagLayout());
        exec.setBorder(section("Cau hinh thuc thi"));
        GridBagConstraints g2=new GridBagConstraints(); g2.insets=new Insets(6,8,6,8); g2.anchor=GridBagConstraints.WEST;

        g2.gridx=0; g2.gridy=0; exec.add(bold("Timeout (ms):"),g2);
        g2.gridx=1; timeout=new JSpinner(new SpinnerNumberModel(cfg.getTimeout(),500,30000,500)); timeout.setPreferredSize(new Dimension(100,28)); exec.add(timeout,g2);

        g2.gridx=0; g2.gridy=1; exec.add(bold("g++ path:"),g2);
        g2.gridx=1; g2.gridwidth=2; g2.fill=GridBagConstraints.HORIZONTAL; gpp=new JTextField(cfg.getGppPath(),30); exec.add(gpp,g2);

        g2.gridx=0; g2.gridy=2; g2.gridwidth=1; g2.fill=GridBagConstraints.NONE; exec.add(bold("python3 path:"),g2);
        g2.gridx=1; g2.gridwidth=2; g2.fill=GridBagConstraints.HORIZONTAL; python=new JTextField(cfg.getPythonPath(),30); exec.add(python,g2);

        g2.gridx=0; g2.gridy=3; g2.gridwidth=3; g2.fill=GridBagConstraints.NONE;
        exec.add(new JLabel("<html><i style='color:gray'>Windows: C:\\mingw64\\bin\\g++.exe | Linux: g++ | macOS: g++-14</i></html>"),g2);

        JButton save=new JButton("Luu Cai dat");
        save.setFont(new Font("Segoe UI",Font.BOLD,14));
        save.setBackground(new Color(30,130,70)); save.setForeground(Color.WHITE);
        save.setFocusPainted(false); save.setPreferredSize(new Dimension(200,40));
        save.addActionListener(e -> doSave());

        status=new JLabel(" "); status.setFont(new Font("Segoe UI",Font.BOLD,12));
        JPanel sav=new JPanel(new FlowLayout(FlowLayout.LEFT,12,8)); sav.add(save); sav.add(status);

        JPanel center=new JPanel(); center.setLayout(new BoxLayout(center,BoxLayout.Y_AXIS));
        center.add(api); center.add(Box.createVerticalStrut(12)); center.add(exec);

        add(center, BorderLayout.CENTER);
        add(sav,    BorderLayout.SOUTH);
    }

    private void doSave() {
        com.judge.Config cfg = com.judge.Config.getInstance();
        cfg.setApiKey(new String(apiKey.getPassword()));
        cfg.setModel((String)modelCb.getSelectedItem());
        cfg.setTimeout((int)timeout.getValue());
        cfg.setGppPath(gpp.getText().trim());
        cfg.setPythonPath(python.getText().trim());
        cfg.save();
        status.setText("Da luu!"); status.setForeground(new Color(0,140,0));
        javax.swing.Timer t = new javax.swing.Timer(3000, e -> status.setText(" "));
t.setRepeats(false);
t.start();
    }

    private JLabel bold(String t) { JLabel l=new JLabel(t); l.setFont(new Font("Segoe UI",Font.BOLD,12)); return l; }
    private TitledBorder section(String t) {
        return BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(180,200,220),1),t,
                TitledBorder.LEFT,TitledBorder.TOP,new Font("Segoe UI",Font.BOLD,12),new Color(50,80,160));
    }
}