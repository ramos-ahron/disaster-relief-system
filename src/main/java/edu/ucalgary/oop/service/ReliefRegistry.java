package edu.ucalgary.oop.service;

import edu.ucalgary.oop.model.DisasterVictim;
import edu.ucalgary.oop.model.Inquirer;
import edu.ucalgary.oop.model.Location;
import edu.ucalgary.oop.model.Relationship;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Everything the relief workers know, held in memory. One instance per running app;
 * the workers' services share it by reference, not through static fields.
 */
public final class ReliefRegistry {
    private final List<Location> locations = new ArrayList<>();
    private final List<DisasterVictim> victims = new ArrayList<>();
    private final List<Inquirer> inquirers = new ArrayList<>();
    private final Set<Relationship> relationships = new LinkedHashSet<>();
    private int highestVictimId;

    // ---- locations

    public Location addLocation(Location location) {
        if (findLocation(location.getName(), location.getAddress()).isPresent()) {
            throw new IllegalArgumentException("Location already exists: " + location);
        }
        locations.add(location);
        return location;
    }

    public Optional<Location> findLocation(String name, String address) {
        return locations.stream().filter(l -> l.isAt(name, address)).findFirst();
    }

    public Location requireLocation(String name, String address) {
        return findLocation(name, address)
                .orElseThrow(() -> new IllegalArgumentException("No location " + name + ", " + address));
    }

    public List<Location> getLocations() {
        return Collections.unmodifiableList(locations);
    }

    public List<DisasterVictim> occupantsOf(Location location) {
        return victims.stream().filter(v -> v.getLocation() == location).toList();
    }

    // ---- victims

    /** The id to give the next victim entered in the app. */
    public int nextVictimId() {
        return highestVictimId + 1;
    }

    public DisasterVictim addVictim(DisasterVictim victim) {
        if (findVictim(victim.getId()).isPresent()) {
            throw new IllegalArgumentException("Victim #" + victim.getId() + " already exists");
        }
        victims.add(victim);
        highestVictimId = Math.max(highestVictimId, victim.getId());
        return victim;
    }

    /** Removes the victim and every relationship they are part of. */
    public void removeVictim(DisasterVictim victim) {
        if (!victims.remove(victim)) {
            throw new IllegalArgumentException("Victim #" + victim.getId() + " is not registered");
        }
        relationships.removeIf(r -> r.involves(victim));
        victim.setLocation(null);
    }

    public Optional<DisasterVictim> findVictim(int id) {
        return victims.stream().filter(v -> v.getId() == id).findFirst();
    }

    public DisasterVictim requireVictim(int id) {
        return findVictim(id).orElseThrow(() -> new IllegalArgumentException("No victim #" + id));
    }

    public List<DisasterVictim> getVictims() {
        return Collections.unmodifiableList(victims);
    }

    public List<DisasterVictim> missingPeople() {
        return victims.stream().filter(DisasterVictim::isMissing).toList();
    }

    // ---- relationships

    /** @return false if the relationship was already recorded */
    public boolean relate(Relationship relationship) {
        return relationships.add(relationship);
    }

    public List<Relationship> relationshipsOf(DisasterVictim victim) {
        return relationships.stream().filter(r -> r.involves(victim)).toList();
    }

    // ---- inquirers

    /** The known caller with this first name and phone number, or a new one. */
    public Inquirer inquirer(String firstName, String lastName, String phoneNumber) {
        for (Inquirer known : inquirers) {
            if (known.isSameCaller(firstName, phoneNumber)) {
                return known;
            }
        }
        Inquirer created = new Inquirer(firstName, lastName, phoneNumber);
        inquirers.add(created);
        return created;
    }

    public List<Inquirer> getInquirers() {
        return Collections.unmodifiableList(inquirers);
    }
}
