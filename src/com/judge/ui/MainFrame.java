package com.judge.ui;

import com.judge.model.Problem;
import java.awt.*;
import javax.swing.*;

public class MainFrame extends JFrame {

    private final Problem problem = new Problem();
    private final JTabbedPane tabs;
    private final ProblemInputPanel inputPanel;
    private final AnalysisPanel     analysisPanel;
    private final TestCasePanel     testCasePanel;
    private final CodeGenPanel      codeGenPanel;
    private final ExecutionPanel    execPanel;
    private final ReportPanel       reportPanel;
    private final SettingsPanel     settingsPanel;

    public MainFrame() {
        setTitle("Competitive Programming Judge — AI TestCase Generator");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1300, 820);
        setMinimumSize(new Dimension(960, 600));
        setLocationRelativeTo(null);

        add(buildHeader(), BorderLayout.NORTH);

        tabs = new JTabbedPane(JTabbedPane.TOP);
        tabs.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        inputPanel    = new ProblemInputPanel(problem, this);
        analysisPanel = new AnalysisPanel(problem, this);
        testCasePanel = new TestCasePanel(problem, this);
        codeGenPanel  = new CodeGenPanel(problem, this);
        execPanel     = new ExecutionPanel(problem, this);
        reportPanel   = new ReportPanel(this);
        settingsPanel = new SettingsPanel();

        tabs.addTab("Nhap de",        inputPanel);
        tabs.addTab("Phan tich",       analysisPanel);
        tabs.addTab("Test Cases",      testCasePanel);
        tabs.addTab("Sinh Code",       codeGenPanel);
        tabs.addTab("Chay & Kiem tra", execPanel);
         tabs.addTab("Bao cao", reportPanel);
        tabs.addTab("Cai dat",         settingsPanel);

        add(tabs, BorderLayout.CENTER);

        JLabel status = new JLabel("  San sang   |   Powered by Google Gemini   |   Phien ban 1.0");
        status.setBorder(BorderFactory.createEtchedBorder());
        status.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        add(status, BorderLayout.SOUTH);
    }

    private JPanel buildHeader() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(new Color(25, 70, 150));
        p.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));

        JLabel t = new JLabel("  Competitive Programming Judge");
        t.setFont(new Font("Segoe UI", Font.BOLD, 21));
        t.setForeground(Color.WHITE);

        JLabel s = new JLabel("  AI-Powered Testcase Generator & Auto-Judge — IOI / ICPC / Codeforces");
        s.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        s.setForeground(new Color(180, 210, 255));

        Box box = Box.createVerticalBox();
        box.add(t); box.add(Box.createVerticalStrut(3)); box.add(s);
        p.add(box, BorderLayout.WEST);


        return p;
    }

    // Tab navigation (Bao cao tab removed)
    public void goAnalysis()  { tabs.setSelectedIndex(1); }
    public void goTestCases() { tabs.setSelectedIndex(2); }
    public void goCodeGen()   { tabs.setSelectedIndex(3); }
    public void goExec()      { tabs.setSelectedIndex(4); }
    public void goReport()    { tabs.setSelectedIndex(5); } // redirect to Exec tab

    // Accessors
    public Problem          getProblem()       { return problem; }
    public AnalysisPanel    getAnalysisPanel() { return analysisPanel; }
    public TestCasePanel    getTestCasePanel() { return testCasePanel; }
    public CodeGenPanel     getCodeGenPanel()  { return codeGenPanel; }
    public ExecutionPanel   getExecPanel()     { return execPanel; }
    public ReportPanel      getReportPanel()   { return reportPanel; }
}
