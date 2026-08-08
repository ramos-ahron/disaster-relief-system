package edu.ucalgary.oop.model;

/**
 * A symmetric relationship between two different victims. The pair is stored with the
 * smaller id first, so Relationship(a, b, t) equals Relationship(b, a, t).
 */
public record Relationship(DisasterVictim first, DisasterVictim second, RelationshipType type) {
    public Relationship {
        if (first == null || second == null || type == null) {
            throw new IllegalArgumentException("Both people and the type are required");
        }
        if (first.getId() == second.getId()) {
            throw new IllegalArgumentException("A person cannot be related to themselves");
        }
        if (first.getId() > second.getId()) {
            DisasterVictim swap = first;
            first = second;
            second = swap;
        }
    }

    public boolean involves(DisasterVictim victim) {
        return first == victim || second == victim;
    }

    /** The other person, seen from one side. */
    public DisasterVictim otherThan(DisasterVictim victim) {
        return first == victim ? second : first;
    }
}
