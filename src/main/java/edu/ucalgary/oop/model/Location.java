package edu.ucalgary.oop.model;

/**
 * A place where victims are. Only shelters keep supplies and only people at a shelter
 * count as found. Who is at a location is tracked on the victim (see DisasterVictim.getLocation).
 */
public final class Location {
    private final String name;
    private final String address;
    private final boolean shelter;
    private final Inventory supplies = new Inventory();

    public Location(String name, String address, boolean shelter) {
        if (name == null || name.isBlank() || address == null || address.isBlank()) {
            throw new IllegalArgumentException("Location name and address are required");
        }
        this.name = name.trim();
        this.address = address.trim();
        this.shelter = shelter;
    }

    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }

    public boolean isShelter() {
        return shelter;
    }

    public Inventory getSupplies() {
        return supplies;
    }

    public boolean isAt(String name, String address) {
        return this.name.equalsIgnoreCase(name.trim()) && this.address.equalsIgnoreCase(address.trim());
    }

    @Override
    public String toString() {
        return name + ", " + address;
    }
}
