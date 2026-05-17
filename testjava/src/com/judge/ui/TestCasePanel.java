package com.judge.ui;

import com.judge.ai.GeminiClient;
import com.judge.model.Problem;
import com.judge.model.TestCase;
import com.judge.util.Json;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.util.*;
import java.util.List;

public class TestCasePanel extends JPanel {

    private final Problem   problem;
    private final MainFrame frame;
    private final DefaultTableModel model;
    private final JTable  table;
    private final JTextArea inArea, expArea, noteArea;
    private final JProgressBar progress;
    private final JLabel  countLbl;
    private final JSpinner countSpin;

    private static final String[] COLS = {"#","Loại","Input (xem trước)","Expected (xem trước)","Verdict","ms","Ghi chú"};

    public TestCasePanel(Problem problem, MainFrame frame) {
        this.problem = problem; this.frame = frame;
        setLayout(new BorderLayout(6,6));
        setBorder(new EmptyBorder(10,10,10,10));

        // Table
        model = new DefaultTableModel(COLS, 0) { @Override public boolean isCellEditable(int r,int c){return false;} };
        table = new JTable(model);
        table.setFont(new Font("Segoe UI",Font.PLAIN,12)); table.setRowHeight(22);
        table.getTableHeader().setFont(new Font("Segoe UI",Font.BOLD,12));
        table.setDefaultRenderer(Object.class, new VR());
        int[] w={40,70,220,220,60,60,120};
        for(int i=0;i<w.length;i++) table.getColumnModel().getColumn(i).setPreferredWidth(w[i]);
        table.getSelectionModel().addListSelectionListener(e -> { if(!e.getValueIsAdjusting()) onSelect(); });

        // Toolbar
        JPanel tb = new JPanel(new FlowLayout(FlowLayout.LEFT,6,4));
        tb.setBorder(section("Sinh test cases"));
        tb.add(new JLabel("Số lượng:"));
        countSpin = new JSpinner(new SpinnerNumberModel(10,1,100,1));
        ((JSpinner.DefaultEditor)countSpin.getEditor()).getTextField().setColumns(3);
        tb.add(countSpin);

        JButton genBtn  = abtn("Sinh (AI)");
        JButton sampBtn = abtn("Tu de mau");
        JButton clrBtn  = btn("Xoa tat ca");
        tb.add(genBtn); tb.add(sampBtn); tb.add(clrBtn);

        countLbl = new JLabel("  0 test cases");
        countLbl.setFont(new Font("Segoe UI",Font.BOLD,12)); countLbl.setForeground(new Color(30,100,30));
        tb.add(countLbl);

        progress = new JProgressBar(); progress.setStringPainted(true);
        progress.setPreferredSize(new Dimension(180,20));
        tb.add(progress);

        genBtn.addActionListener(e  -> doGenerate());
        sampBtn.addActionListener(e -> addSamples());
        clrBtn.addActionListener(e  -> clearAll());

        // Editor
        inArea   = edArea(); expArea  = edArea(); noteArea = edArea();
        JPanel editor = new JPanel(new GridLayout(3,1,4,4));
        editor.add(labeled("Input:", inArea));
        editor.add(labeled("Expected:", expArea));
        editor.add(labeled("Ghi chú:", noteArea));
        editor.setBorder(section("Chinh sua"));
        JScrollPane edScroll = new JScrollPane(editor);

        JPanel edBtns = new JPanel(new FlowLayout(FlowLayout.LEFT,4,2));
        JButton saveBtn = btn("Luu"); JButton addBtn = btn("Them moi"); JButton delBtn = btn("Xoa");
        edBtns.add(saveBtn); edBtns.add(addBtn); edBtns.add(delBtn);
        saveBtn.addActionListener(e -> saveRow());
        addBtn.addActionListener(e  -> addNew());
        delBtn.addActionListener(e  -> delRow());

        JPanel bottom = new JPanel(new BorderLayout(4,4));
        bottom.add(edScroll,  BorderLayout.CENTER);
        bottom.add(edBtns,    BorderLayout.SOUTH);

        JPanel nav = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton nextBtn = abtn("Sang Chay & Kiem tra");
        nav.add(nextBtn); nextBtn.addActionListener(e -> frame.goExec());

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JScrollPane(table), bottom);
        split.setResizeWeight(0.65);

        add(tb,    BorderLayout.NORTH);
        add(split, BorderLayout.CENTER);
        add(nav,   BorderLayout.SOUTH);
    }

    private void doGenerate() {
        if (problem.getStatement().isEmpty() && problem.getInputFormat().isEmpty()) {
            warn("Hãy phân tích đề trước (Tab Nhập đề)."); return;
        }
        int count = (int) countSpin.getValue();
        progress.setIndeterminate(true); progress.setString("Đang sinh " + count + " TC...");
        new SwingWorker<Void, String>() {
            @Override protected Void doInBackground() throws Exception {
                String json = new GeminiClient().generateTestCases(problem, count);
                publish("Đang parse..."); parseAndAdd(Json.stripFences(json)); return null;
            }
            @Override protected void process(List<String> c) { progress.setString(c.get(c.size()-1)); }
            @Override protected void done() {
                progress.setIndeterminate(false);
                try { get(); refreshTable(); progress.setString("Xong!"); }
                catch (Exception ex) { progress.setString("Loi"); warn(ex.getMessage()); }
            }
        }.execute();
    }

    @SuppressWarnings("unchecked")
    private void parseAndAdd(String json) {
        try {
            List<Object> arr = (List<Object>) Json.parse(json);
            List<TestCase> list = new ArrayList<>(problem.getTestCases());
            int id = list.stream().mapToInt(TestCase::getId).max().orElse(0) + 1;
            for (Object el : arr) {
                Map<String,Object> obj = (Map<String,Object>) el;
                TestCase tc = new TestCase(id++, Json.str(obj,"input",""), Json.str(obj,"expected",""),
                        Json.str(obj,"category","RANDOM").toUpperCase());
                tc.setNote(Json.str(obj,"note",""));
                list.add(tc);
            }
            problem.setTestCases(list);
        } catch (Exception e) { throw new RuntimeException("Parse error: " + e.getMessage(), e); }
    }

    public void refreshFromProblem() {
        // prepend samples if not already there
        List<TestCase> list = new ArrayList<>(problem.getTestCases());
        for (TestCase s : problem.getSamples()) {
            if (list.stream().noneMatch(t -> t.isSample())) { list.add(0, s); }
        }
        problem.setTestCases(list);
        refreshTable();
    }

    public void refreshTable() {
        model.setRowCount(0);
        for (TestCase tc : problem.getTestCases()) {
            model.addRow(new Object[]{tc.getId(), tc.getCategory(), prev(tc.getInput()),
                    prev(tc.getExpected()), tc.getVerdict(), tc.getTimeMs()>=0?tc.getTimeMs():"", tc.getNote()});
        }
        countLbl.setText("  " + problem.getTestCases().size() + " test cases");
    }

    public List<TestCase> getAll() { return new ArrayList<>(problem.getTestCases()); }

    private void addSamples() {
        List<TestCase> list = new ArrayList<>(problem.getTestCases());
        for (TestCase s : problem.getSamples())
            if (list.stream().noneMatch(t -> t.getInput().equals(s.getInput()))) list.add(0, s);
        problem.setTestCases(list); refreshTable();
    }

    private void onSelect() {
        int r = table.getSelectedRow(); if (r < 0) return;
        List<TestCase> all = problem.getTestCases(); if (r >= all.size()) return;
        TestCase tc = all.get(r);
        inArea.setText(tc.getInput()); expArea.setText(tc.getExpected()); noteArea.setText(tc.getNote());
    }

    private void saveRow() {
        int r = table.getSelectedRow(); if (r < 0) return;
        List<TestCase> all = problem.getTestCases(); if (r >= all.size()) return;
        TestCase tc = all.get(r); tc.setInput(inArea.getText()); tc.setExpected(expArea.getText()); tc.setNote(noteArea.getText());
        refreshTable(); table.setRowSelectionInterval(r, r);
    }

    private void addNew() {
        List<TestCase> list = problem.getTestCases();
        int id = list.stream().mapToInt(TestCase::getId).max().orElse(0) + 1;
        TestCase tc = new TestCase(id, inArea.getText(), expArea.getText(), "MANUAL");
        tc.setNote(noteArea.getText()); list.add(tc); problem.setTestCases(list); refreshTable();
    }

    private void delRow() {
        int r = table.getSelectedRow(); if (r < 0) return;
        List<TestCase> list = new ArrayList<>(problem.getTestCases()); list.remove(r);
        problem.setTestCases(list); refreshTable();
    }

    private void clearAll() {
        if (JOptionPane.showConfirmDialog(this,"Xóa toàn bộ?","Xác nhận",JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION) {
            problem.setTestCases(new ArrayList<>()); refreshTable();
        }
    }

    private String prev(String s) {
        if (s == null || s.isEmpty()) return "";
        String r = s.replace("\n"," | ").replace("\r","");
        return r.length() > 55 ? r.substring(0,52) + "..." : r;
    }

    private JTextArea edArea() {
        JTextArea a = new JTextArea(3,30); a.setFont(new Font("Monospaced",Font.PLAIN,12)); a.setLineWrap(true); return a;
    }
    private JPanel labeled(String t, JTextArea a) {
        JPanel p = new JPanel(new BorderLayout(2,2)); JLabel l = new JLabel(t); l.setFont(new Font("Segoe UI",Font.BOLD,11));
        p.add(l,BorderLayout.NORTH); p.add(new JScrollPane(a),BorderLayout.CENTER); return p;
    }
    private JButton btn(String t) { JButton b=new JButton(t); b.setFont(new Font("Segoe UI",Font.PLAIN,12)); return b; }
    private JButton abtn(String t) {
        JButton b=btn(t); b.setBackground(new Color(30,100,200)); b.setForeground(Color.WHITE); b.setFocusPainted(false); return b;
    }
    private TitledBorder section(String t) {
        return BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(180,200,220),1),t,
                TitledBorder.LEFT,TitledBorder.TOP,new Font("Segoe UI",Font.BOLD,12),new Color(50,80,160));
    }
    private void warn(String m) { JOptionPane.showMessageDialog(this,m,"Lỗi",JOptionPane.WARNING_MESSAGE); }

    private static class VR extends DefaultTableCellRenderer {
        @Override public Component getTableCellRendererComponent(JTable t,Object v,boolean sel,boolean foc,int row,int col){
            Component c = super.getTableCellRendererComponent(t,v,sel,foc,row,col);
            if(col==4 && v!=null) switch(v.toString()){
                case "AC"->c.setForeground(new Color(0,140,0)); case "WA"->c.setForeground(Color.RED);
                case "TLE"->c.setForeground(new Color(180,100,0)); case "RE"->c.setForeground(new Color(120,0,180));
                default->c.setForeground(sel?t.getSelectionForeground():t.getForeground());
            } else c.setForeground(sel?t.getSelectionForeground():t.getForeground());
            return c;
        }
    }
}