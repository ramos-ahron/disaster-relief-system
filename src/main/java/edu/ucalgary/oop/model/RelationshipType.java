package edu.ucalgary.oop.model;

/**
 * Kinds of social relationship between two victims. All are symmetric.
 * Directed parent and child links live in the family graph (FamilyMember) instead.
 */
public enum RelationshipType {
    GUARDIAN_WARD("Guardian-Ward"),
    PARENT_CHILD("Parent-Child"),
    SIBLING("Sibling"),
    FRIEND("Friend"),
    COUPLE("Couple");

    private final String label;

    RelationshipType(String label) {
        this.label = label;
    }

    /** Accepts the label ("Guardian-Ward") or the constant name ("GUARDIAN_WARD"), any case. */
    public static RelationshipType parse(String text) {
        for (RelationshipType t : values()) {
            if (t.label.equalsIgnoreCase(text.trim()) || t.name().equalsIgnoreCase(text.trim())) {
                return t;
            }
        }
        throw new IllegalArgumentException("Unknown relationship type: " + text);
    }

    @Override
    public String toString() {
        return label;
    }
}
