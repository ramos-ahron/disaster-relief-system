package edu.ucalgary.oop.model;

/** Airline meal codes. The database stores either the code or the description. */
public enum DietaryRestriction {
    AVML("Asian vegetarian meal"),
    DBML("Diabetic meal"),
    GFML("Gluten intolerant meal"),
    KSML("Kosher meal"),
    LSML("Low salt meal"),
    MOML("Muslim meal"),
    PFML("Peanut-free meal"),
    VGML("Vegan meal"),
    VJML("Vegetarian Jain meal");

    private final String description;

    DietaryRestriction(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /** Accepts the code ("AVML") or the description ("Asian vegetarian meal"), any case. */
    public static DietaryRestriction parse(String text) {
        for (DietaryRestriction r : values()) {
            if (r.name().equalsIgnoreCase(text.trim()) || r.description.equalsIgnoreCase(text.trim())) {
                return r;
            }
        }
        throw new IllegalArgumentException("Unknown dietary restriction: " + text);
    }

    @Override
    public String toString() {
        return name() + " (" + description + ")";
    }
}
