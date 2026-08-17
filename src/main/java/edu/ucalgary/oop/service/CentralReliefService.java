package edu.ucalgary.oop.service;

import edu.ucalgary.oop.model.DisasterVictim;
import edu.ucalgary.oop.model.Inquirer;
import edu.ucalgary.oop.model.Inquiry;
import edu.ucalgary.oop.model.Location;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

/** What a central relief worker does: take calls, search for people, manage locations and stock. */
public final class CentralReliefService {
    private final ReliefRegistry registry;

    public CentralReliefService(ReliefRegistry registry) {
        this.registry = registry;
    }

    /** Logs a call today. A repeat caller (same first name and phone) gets the inquiry added to their log. */
    public Inquirer logInquiry(String firstName, String lastName, String phoneNumber, String details) {
        Inquiry inquiry = new Inquiry(LocalDate.now(), details);
        Inquirer inquirer = registry.inquirer(firstName, lastName, phoneNumber);
        inquirer.log(inquiry);
        return inquirer;
    }

    /** Victims whose first or last name contains the text, ignoring case. */
    public List<DisasterVictim> search(String text) {
        String needle = text.trim().toLowerCase(Locale.ROOT);
        if (needle.isEmpty()) {
            throw new IllegalArgumentException("Enter part of a name to search for");
        }
        return registry.getVictims().stream()
                .filter(v -> v.getFullName().toLowerCase(Locale.ROOT).contains(needle))
                .toList();
    }

    public Location addLocation(String name, String address, boolean shelter) {
        return registry.addLocation(new Location(name, address, shelter));
    }

    /** Adds stock to a shelter. Other locations do not keep supplies. */
    public void addSupplies(Location location, String type, int quantity) {
        if (!location.isShelter()) {
            throw new IllegalArgumentException(location.getName() + " is not a shelter");
        }
        location.getSupplies().add(type, quantity);
    }
}
