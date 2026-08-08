package edu.ucalgary.oop.model;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * A person affected by the disaster. The id is the DisasterVictim.victim_id of the row,
 * or a new id handed out by ReliefRegistry for people entered in the app.
 */
public final class DisasterVictim extends Person {
    private final int id;
    private final LocalDate entryDate;
    private final Inventory belongings = new Inventory();
    private final List<MedicalRecord> medicalRecords = new ArrayList<>();
    private final Set<DietaryRestriction> dietaryRestrictions = EnumSet.noneOf(DietaryRestriction.class);
    private LocalDate dateOfBirth;
    private String gender;
    private String comments;
    private Location location;

    public DisasterVictim(int id, String firstName, String lastName, LocalDate entryDate) {
        super(firstName, lastName);
        if (id <= 0) {
            throw new IllegalArgumentException("Victim id must be positive");
        }
        this.id = id;
        this.entryDate = Dates.notInFuture(entryDate, "Entry date");
    }

    public int getId() {
        return id;
    }

    public LocalDate getEntryDate() {
        return entryDate;
    }

    /** @return the date of birth, or null if unknown */
    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        if (dateOfBirth != null && Dates.notInFuture(dateOfBirth, "Date of birth").isBefore(LocalDate.now().minusYears(150))) {
            throw new IllegalArgumentException("Date of birth is more than 150 years ago");
        }
        this.dateOfBirth = dateOfBirth;
    }

    /** @return age in whole years, or -1 if the date of birth is unknown */
    public int getAge() {
        return dateOfBirth == null ? -1 : Period.between(dateOfBirth, LocalDate.now()).getYears();
    }

    public String getGender() {
        return gender;
    }

    /** @param gender one of GenderOptions.all(), or null */
    public void setGender(String gender) {
        if (gender != null && !GenderOptions.isValid(gender)) {
            throw new IllegalArgumentException("Unknown gender option: " + gender);
        }
        this.gender = gender;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    /** @return where the victim is now, or null if nobody knows */
    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    /** Missing means not known to be at a shelter. */
    public boolean isMissing() {
        return location == null || !location.isShelter();
    }

    public Inventory getBelongings() {
        return belongings;
    }

    public List<MedicalRecord> getMedicalRecords() {
        return Collections.unmodifiableList(medicalRecords);
    }

    public void addMedicalRecord(MedicalRecord record) {
        if (medicalRecords.contains(record)) {
            throw new IllegalArgumentException("Medical record already exists");
        }
        medicalRecords.add(record);
    }

    public Set<DietaryRestriction> getDietaryRestrictions() {
        return Collections.unmodifiableSet(dietaryRestrictions);
    }

    public void addDietaryRestriction(DietaryRestriction restriction) {
        if (!dietaryRestrictions.add(restriction)) {
            throw new IllegalArgumentException("Dietary restriction already recorded");
        }
    }

    public void removeDietaryRestriction(DietaryRestriction restriction) {
        if (!dietaryRestrictions.remove(restriction)) {
            throw new IllegalArgumentException("Dietary restriction not recorded");
        }
    }

    /** One line for lists: id, name and location. */
    public String summary() {
        return "#" + id + " " + getFullName() + " @ " + (location == null ? "unknown" : location);
    }
}
