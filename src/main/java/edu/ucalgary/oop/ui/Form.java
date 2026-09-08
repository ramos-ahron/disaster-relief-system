package edu.ucalgary.oop.ui;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.Component;
import java.awt.GridLayout;
import java.util.LinkedHashMap;
import java.util.Map;

/** A labelled form shown in an OK/Cancel dialog. Fields are read back by their label. */
final class Form {
    private final Map<String, JComponent> fields = new LinkedHashMap<>();

    Form field(String label) {
        return field(label, "");
    }

    Form field(String label, String initial) {
        fields.put(label, new JTextField(initial, 20));
        return this;
    }

    Form check(String label) {
        fields.put(label, new JCheckBox());
        return this;
    }

    Form choice(String label, Object... options) {
        fields.put(label, new JComboBox<>(options));
        return this;
    }

    /** @return true if the user pressed OK */
    boolean show(Component parent, String title) {
        JPanel panel = new JPanel(new GridLayout(0, 2, 6, 6));
        fields.forEach((label, field) -> {
            panel.add(new JLabel(label));
            panel.add(field);
        });
        return JOptionPane.showConfirmDialog(parent, panel, title, JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION;
    }

    String text(String label) {
        return ((JTextField) fields.get(label)).getText().trim();
    }

    int number(String label) {
        try {
            return Integer.parseInt(text(label));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(label + " must be a whole number");
        }
    }

    boolean checked(String label) {
        return ((JCheckBox) fields.get(label)).isSelected();
    }

    Object chosen(String label) {
        return ((JComboBox<?>) fields.get(label)).getSelectedItem();
    }
}
