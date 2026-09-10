package edu.ucalgary.oop.ui;

import edu.ucalgary.oop.model.Dates;
import edu.ucalgary.oop.model.DisasterVictim;
import edu.ucalgary.oop.model.GenderOptions;
import edu.ucalgary.oop.model.Inquirer;
import edu.ucalgary.oop.model.Inventory;
import edu.ucalgary.oop.model.Location;
import edu.ucalgary.oop.model.MedicalRecord;
import edu.ucalgary.oop.model.Relationship;
import edu.ucalgary.oop.service.CentralReliefService;
import edu.ucalgary.oop.service.LocationReliefService;
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
import java.time.LocalDate;
import java.util.List;
import java.util.function.Function;

/**
 * The desktop app: one window, a tab per worker role. Each button opens a small form,
 * calls a service and shows the result. Rule violations from the model arrive as
 * IllegalArgumentException and are shown as an error message.
 */
public final class ReliefWindow {
    private static final String NO_GENDER = "(not given)";

    private final JFrame frame = new JFrame("Disaster Relief Management");
    private final ReliefRegistry registry;
    private final CentralReliefService central;
    private final LocationReliefService local;

    public ReliefWindow(ReliefRegistry registry) {
        this.registry = registry;
        this.central = new CentralReliefService(registry);
        this.local = new LocationReliefService(registry);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Central relief worker", centralTab());
        tabs.addTab("Location relief worker", locationTab());
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

    private JPanel locationTab() {
        JPanel panel = buttonPanel();
        button(panel, "Missing people", () ->
                info(lines(registry.missingPeople(), DisasterVictim::summary, "Nobody is missing.")));
        button(panel, "People at a location", () -> {
            Form f = new Form().choice("Location", locations());
            if (f.show(frame, "People at a location")) {
                info(lines(registry.occupantsOf(requireChosen(f, "Location")), DisasterVictim::summary, "Nobody is there."));
            }
        });
        button(panel, "Register a victim", () -> {
            Object[] genders = new Object[GenderOptions.all().size() + 1];
            genders[0] = NO_GENDER;
            System.arraycopy(GenderOptions.all().toArray(), 0, genders, 1, genders.length - 1);
            Form f = new Form().field("First name").field("Last name").field("Entry date", LocalDate.now().toString())
                    .field("Date of birth (optional)").choice("Gender", genders).field("Comments").choice("Location", locations());
            if (f.show(frame, "Register a victim")) {
                DisasterVictim v = local.register(f.text("First name"), f.text("Last name"),
                        Dates.parse(f.text("Entry date"), "Entry date"), requireChosen(f, "Location"));
                try {
                    String dob = f.text("Date of birth (optional)");
                    v.setDateOfBirth(dob.isEmpty() ? null : Dates.parse(dob, "Date of birth"));
                    v.setGender(NO_GENDER.equals(f.chosen("Gender")) ? null : (String) f.chosen("Gender"));
                    v.setComments(f.text("Comments"));
                } catch (IllegalArgumentException e) {
                    registry.removeVictim(v);   // all or nothing
                    throw e;
                }
                info("Registered " + v.summary());
            }
        });
        button(panel, "Victim details", () -> {
            Form f = new Form().field("Victim id");
            if (f.show(frame, "Victim details")) {
                info(details(registry.requireVictim(f.number("Victim id"))));
            }
        });
        button(panel, "Move a victim", () -> {
            Form f = new Form().field("Victim id").choice("New location", locations());
            if (f.show(frame, "Move a victim")) {
                DisasterVictim v = registry.requireVictim(f.number("Victim id"));
                local.move(v, requireChosen(f, "New location"));
                info(v.summary());
            }
        });
        button(panel, "Discharge a victim", () -> {
            Form f = new Form().field("Victim id");
            if (f.show(frame, "Discharge a victim")) {
                DisasterVictim v = registry.requireVictim(f.number("Victim id"));
                local.discharge(v);
                info("Removed " + v.getFullName() + ".");
            }
        });
        button(panel, "Supplies at a shelter", () -> {
            Form f = new Form().choice("Shelter", shelters());
            if (f.show(frame, "Supplies at a shelter")) {
                info(stock(requireChosen(f, "Shelter").getSupplies()));
            }
        });
        button(panel, "Give supplies", () -> {
            Form f = new Form().field("Victim id").field("Type").field("Quantity");
            if (f.show(frame, "Give supplies")) {
                DisasterVictim v = registry.requireVictim(f.number("Victim id"));
                local.giveSupplies(v, f.text("Type"), f.number("Quantity"));
                info(v.getFullName() + " now has:\n" + stock(v.getBelongings()));
            }
        });
        button(panel, "Record a treatment", () -> {
            Form f = new Form().field("Victim id").field("Treatment");
            if (f.show(frame, "Record a treatment")) {
                DisasterVictim v = registry.requireVictim(f.number("Victim id"));
                local.treat(v, f.text("Treatment"));
                info(details(v));
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

    private Object[] locations() {
        return registry.getLocations().toArray();
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

    private static String details(DisasterVictim v) {
        StringBuilder sb = new StringBuilder(v.summary()).append('\n');
        sb.append("Entered: ").append(v.getEntryDate()).append('\n');
        if (v.getDateOfBirth() != null) {
            sb.append("Born: ").append(v.getDateOfBirth()).append(" (age ").append(v.getAge()).append(")\n");
        }
        if (v.getGender() != null) {
            sb.append("Gender: ").append(v.getGender()).append('\n');
        }
        if (v.getComments() != null && !v.getComments().isEmpty()) {
            sb.append("Comments: ").append(v.getComments()).append('\n');
        }
        v.getDietaryRestrictions().forEach(d -> sb.append("Diet: ").append(d).append('\n'));
        sb.append("Belongings:\n").append(stock(v.getBelongings())).append('\n');
        for (MedicalRecord m : v.getMedicalRecords()) {
            sb.append("Treated ").append(m.dateOfTreatment()).append(m.location() == null ? "" : " at " + m.location().getName())
                    .append(": ").append(m.treatmentDetails()).append('\n');
        }
        return sb.toString().trim();
    }
}
