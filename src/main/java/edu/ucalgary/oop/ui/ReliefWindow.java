package edu.ucalgary.oop.ui;

import edu.ucalgary.oop.model.Inquirer;
import edu.ucalgary.oop.service.CentralReliefService;
import edu.ucalgary.oop.service.ReliefRegistry;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import java.awt.GridLayout;

/**
 * The desktop app: one window, a tab per worker role. Each button opens a small form,
 * calls a service and shows the result. Rule violations from the model arrive as
 * IllegalArgumentException and are shown as an error message.
 */
public final class ReliefWindow {

    private final JFrame frame = new JFrame("Disaster Relief Management");
    private final CentralReliefService central;

    public ReliefWindow(ReliefRegistry registry) {
        this.central = new CentralReliefService(registry);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Central relief worker", centralTab());
        frame.add(tabs);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setSize(Math.max(frame.getWidth(), 460), frame.getHeight());   // room for both tab titles
        frame.setLocationRelativeTo(null);
    }

    public void show() {
        frame.setVisible(true);
    }

    private JPanel centralTab() {
        JPanel panel = buttonPanel();
        button(panel, "Log an inquiry", () -> {
            Form f = new Form().field("First name").field("Last name").field("Phone (000-000-0000)").field("Looking for");
            if (f.show(frame, "Log an inquiry")) {
                Inquirer who = central.logInquiry(f.text("First name"), f.text("Last name"),
                        f.text("Phone (000-000-0000)"), f.text("Looking for"));
                info("Logged. " + who.getFullName() + " has called " + who.getInquiries().size() + " time(s).");
            }
        });
        return panel;
    }

    // ---- helpers

    private static JPanel buttonPanel() {
        JPanel panel = new JPanel(new GridLayout(0, 1, 6, 6));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        return panel;
    }

    private void button(JPanel panel, String label, Runnable action) {
        JButton b = new JButton(label);
        b.addActionListener(e -> {
            try {
                action.run();
            } catch (IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(frame, ex.getMessage(), "Cannot do that", JOptionPane.ERROR_MESSAGE);
            }
        });
        panel.add(b);
    }

    private void info(String text) {
        JTextArea area = new JTextArea(text, Math.min(20, text.split("\n").length + 1), 50);
        area.setEditable(false);
        JOptionPane.showMessageDialog(frame, new JScrollPane(area), "Result", JOptionPane.INFORMATION_MESSAGE);
    }
}
