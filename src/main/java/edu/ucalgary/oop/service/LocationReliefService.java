package edu.ucalgary.oop.service;

import edu.ucalgary.oop.model.DisasterVictim;
import edu.ucalgary.oop.model.Location;
import edu.ucalgary.oop.model.MedicalRecord;

import java.time.LocalDate;

/** What a worker at a location does: register, move and discharge people, hand out supplies, log treatment. */
public final class LocationReliefService {
    private final ReliefRegistry registry;

    public LocationReliefService(ReliefRegistry registry) {
        this.registry = registry;
    }

    /** Registers a new victim at a location, with the next free id. */
    public DisasterVictim register(String firstName, String lastName, LocalDate entryDate, Location location) {
        DisasterVictim victim = new DisasterVictim(registry.nextVictimId(), firstName, lastName, entryDate);
        victim.setLocation(location);
        return registry.addVictim(victim);
    }

    public void move(DisasterVictim victim, Location to) {
        if (victim.getLocation() == to) {
            throw new IllegalArgumentException(victim.getFullName() + " is already at " + to.getName());
        }
        victim.setLocation(to);
    }

    public void discharge(DisasterVictim victim) {
        registry.removeVictim(victim);
    }

    /** Moves supplies from the victim's shelter to the victim. */
    public void giveSupplies(DisasterVictim victim, String type, int quantity) {
        Location here = victim.getLocation();
        if (victim.isMissing()) {
            throw new IllegalArgumentException(victim.getFullName() + " is not at a shelter");
        }
        here.getSupplies().transferTo(victim.getBelongings(), type, quantity);
    }

    /** Records a treatment given today at the victim's current location. */
    public MedicalRecord treat(DisasterVictim victim, String details) {
        MedicalRecord record = new MedicalRecord(victim.getLocation(), details, LocalDate.now());
        victim.addMedicalRecord(record);
        return record;
    }
}
