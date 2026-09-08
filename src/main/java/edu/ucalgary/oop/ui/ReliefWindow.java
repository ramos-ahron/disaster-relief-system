package edu.ucalgary.oop.ui;

import edu.ucalgary.oop.model.DisasterVictim;
import edu.ucalgary.oop.model.Inquirer;
import edu.ucalgary.oop.model.Inventory;
import edu.ucalgary.oop.model.Location;
import edu.ucalgary.oop.model.Relationship;
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
import java.util.List;
import java.util.function.Function;

/**
 * The desktop app: one window, a tab per worker role. Each button opens a small form,
 * calls a service and shows the result. Rule violations from the model arrive as
 * IllegalArgumentException and are shown as an error message.
 */
public final class ReliefWindow {

    private final JFrame frame = new JFrame("Disaster Relief Management");
    private final ReliefRegistry registry;
    private final CentralReliefService central;

    public ReliefWindow(ReliefRegistry registry) {
        this.registry = registry;
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
        button(panel, "Find a victim", () -> {
            Form f = new Form().field("Part of a name");
            if (f.show(frame, "Find a victim")) {
                info(lines(central.search(f.text("Part of a name")), DisasterVictim::summary, "Nobody matches."));
            }
        });
        button(panel, "Show relationships", () -> {
            Form f = new Form().field("Victim id");
            if (f.show(frame, "Show relationships")) {
                DisasterVictim v = registry.requireVictim(f.number("Victim id"));
                info(lines(registry.relationshipsOf(v), r -> describe(v, r), v.getFullName() + " has no recorded relationships."));
            }
        });
        button(panel, "Add a location", () -> {
            Form f = new Form().field("Name").field("Address").check("Shelter");
            if (f.show(frame, "Add a location")) {
                info("Added " + central.addLocation(f.text("Name"), f.text("Address"), f.checked("Shelter")) + ".");
            }
        });
        button(panel, "Add supplies to a shelter", () -> {
            Form f = new Form().choice("Shelter", shelters()).field("Type").field("Quantity");
            if (f.show(frame, "Add supplies")) {
                Location shelter = requireChosen(f, "Shelter");
                central.addSupplies(shelter, f.text("Type"), f.number("Quantity"));
                info(stock(shelter.getSupplies()));
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

    private Object[] shelters() {
        return registry.getLocations().stream().filter(Location::isShelter).toArray();
    }

    private static Location requireChosen(Form f, String label) {
        Object chosen = f.chosen(label);
        if (chosen == null) {
            throw new IllegalArgumentException("Add a location first");
        }
        return (Location) chosen;
    }

    private static <T> String lines(List<T> items, Function<T, String> format, String whenEmpty) {
        return items.isEmpty() ? whenEmpty : String.join("\n", items.stream().map(format).toList());
    }

    private static String describe(DisasterVictim from, Relationship r) {
        return from.getFullName() + " - " + r.type() + " - " + r.otherThan(from).summary();
    }

    private static String stock(Inventory inventory) {
        if (inventory.isEmpty()) {
            return "Nothing in stock.";
        }
        StringBuilder sb = new StringBuilder();
        inventory.asMap().forEach((type, n) -> sb.append(n).append(" x ").append(type).append('\n'));
        return sb.toString().trim();
    }
}
