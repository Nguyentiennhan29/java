package com.judge.ui;

import com.judge.model.Problem;
import com.judge.model.TestCase;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.util.List;

public class AnalysisPanel extends JPanel {
    private final Problem   p;
    private final MainFrame frame;
    private final JLabel titleLbl, typeLbl, algLbl, tlLbl, memLbl;
    private final JTextArea inFmt, outFmt, cons, analysis, samples;

    public AnalysisPanel(Problem p, MainFrame frame) {
        this.p = p; this.frame = frame;
        setLayout(new BorderLayout(8,8));
        setBorder(new EmptyBorder(10,10,10,10));

        // Metadata row
        JPanel meta = new JPanel(new GridBagLayout());
        meta.setBorder(section("Thong tin bai"));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(4,8,4,8); g.anchor = GridBagConstraints.WEST;

        titleLbl = lbl("—"); titleLbl.setFont(new Font("Segoe UI",Font.BOLD,14));
        titleLbl.setForeground(new Color(30,80,160));
        typeLbl  = lbl("—"); algLbl = lbl("—"); algLbl.setForeground(new Color(180,60,0));
        tlLbl    = lbl("—"); memLbl = lbl("—");

        Object[][] rows = {{"Tên bài:",titleLbl},{"Loại đề:",typeLbl},{"Thuật toán:",algLbl},
                           {"Giới hạn TG:",tlLbl},{"Bộ nhớ:",memLbl}};
        for (int i = 0; i < rows.length; i++) {
            g.gridx = i*2; g.gridy = 0; meta.add(bold(rows[i][0].toString()), g);
            g.gridx = i*2+1; meta.add((JLabel)rows[i][1], g);
        }

        // Content
        inFmt    = area(); outFmt   = area();
        cons     = area(); analysis = area(); samples  = area();

        JPanel left  = new JPanel(new GridLayout(3,1,4,4));
        left.add(wrap("Input Format", inFmt, 90));
        left.add(wrap("Output Format", outFmt, 90));
        left.add(wrap("Rang buoc", cons, 90));

        JPanel right = new JPanel(new BorderLayout(4,4));
        right.add(wrap("Phan tich & Huong giai (AI)", analysis, 200), BorderLayout.CENTER);
        right.add(wrap("Test mau tu de", samples, 140), BorderLayout.SOUTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right);
        split.setResizeWeight(0.4); split.setDividerSize(6);

        JPanel nav = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        nav.add(navBtn("Test Cases", () -> frame.goTestCases()));
        nav.add(navBtn("Sinh Code",  () -> frame.goCodeGen()));

        add(meta,  BorderLayout.NORTH);
        add(split, BorderLayout.CENTER);
        add(nav,   BorderLayout.SOUTH);
    }

    public void refresh() {
        titleLbl.setText(p.getTitle().isEmpty() ? "—" : p.getTitle());
        typeLbl.setText(p.getProblemType());
        algLbl.setText(p.getAlgorithmType());
        tlLbl.setText(p.getTimeLimitMs() + " ms");
        memLbl.setText(p.getMemoryMb() + " MB");
        inFmt.setText(p.getInputFormat());
        outFmt.setText(p.getOutputFormat());
        cons.setText(p.getConstraints());
        analysis.setText(p.getAnalysis() + "\n\n" + p.getSuggestedApproach());
        analysis.setCaretPosition(0);

        StringBuilder sb = new StringBuilder();
        int i = 1;
        for (TestCase tc : p.getSamples()) {
            sb.append("=== Ví dụ ").append(i++).append(" ===\n");
            sb.append("Input:\n").append(tc.getInput().trim()).append("\n");
            sb.append("Output:\n").append(tc.getExpected().trim()).append("\n");
            if (!tc.getNote().isEmpty()) sb.append("Ghi chú: ").append(tc.getNote()).append("\n");
            sb.append("\n");
        }
        samples.setText(sb.toString()); samples.setCaretPosition(0);
    }

    private JTextArea area() {
        JTextArea a = new JTextArea(); a.setEditable(false);
        a.setFont(new Font("Monospaced",Font.PLAIN,12)); a.setLineWrap(true); a.setWrapStyleWord(true);
        return a;
    }
    private JPanel wrap(String title, JTextArea a, int h) {
        JPanel p = new JPanel(new BorderLayout(2,2)); p.setBorder(section(title));
        JScrollPane sc = new JScrollPane(a); sc.setPreferredSize(new Dimension(100,h));
        p.add(sc); return p;
    }
    private JLabel lbl(String t) { return new JLabel(t); }
    private JLabel bold(String t) { JLabel l=lbl(t); l.setFont(new Font("Segoe UI",Font.BOLD,12)); return l; }
    private JButton navBtn(String t, Runnable r) {
        JButton b=new JButton(t); b.setFont(new Font("Segoe UI",Font.BOLD,12));
        b.setBackground(new Color(30,100,200)); b.setForeground(Color.WHITE); b.setFocusPainted(false);
        b.addActionListener(e->r.run()); return b;
    }
    private TitledBorder section(String t) {
        return BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(180,200,220),1),t,
                TitledBorder.LEFT,TitledBorder.TOP,new Font("Segoe UI",Font.BOLD,12),new Color(50,80,160));
    }
}