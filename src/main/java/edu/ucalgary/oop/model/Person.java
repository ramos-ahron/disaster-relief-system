package edu.ucalgary.oop.model;

/** Anyone the system knows by name. The last name is optional. */
public abstract class Person {
    private final String firstName;
    private final String lastName;

    protected Person(String firstName, String lastName) {
        if (firstName == null || firstName.isBlank()) {
            throw new IllegalArgumentException("First name is required");
        }
        this.firstName = firstName.trim();
        this.lastName = lastName == null || lastName.isBlank() ? null : lastName.trim();
    }

    public String getFirstName() {
        return firstName;
    }

    /** @return the last name, or null if none was given */
    public String getLastName() {
        return lastName;
    }

    public String getFullName() {
        return lastName == null ? firstName : firstName + " " + lastName;
    }

    @Override
    public String toString() {
        return getFullName();
    }
}
