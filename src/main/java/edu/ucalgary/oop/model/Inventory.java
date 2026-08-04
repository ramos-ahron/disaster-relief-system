package edu.ucalgary.oop.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Supply counts by type. Used for a location's stock and for a victim's belongings.
 * A type disappears when its count reaches zero, so counts are always positive.
 */
public final class Inventory {
    private final Map<String, Integer> counts = new LinkedHashMap<>();

    public void add(String type, int quantity) {
        counts.merge(checkType(type), checkQuantity(quantity), Integer::sum);
    }

    /** @throws IllegalArgumentException if there is less than quantity of the type; nothing changes then */
    public void remove(String type, int quantity) {
        int left = quantity(checkType(type)) - checkQuantity(quantity);
        if (left < 0) {
            throw new IllegalArgumentException("Only " + quantity(type) + " of " + type + " available");
        }
        if (left == 0) {
            counts.remove(type);
        } else {
            counts.put(type, left);
        }
    }

    /** Moves quantity of the type into another inventory, or does nothing if this one has too little. */
    public void transferTo(Inventory other, String type, int quantity) {
        remove(type, quantity);
        other.add(type, quantity);
    }

    public int quantity(String type) {
        return counts.getOrDefault(type, 0);
    }

    public boolean isEmpty() {
        return counts.isEmpty();
    }

    public Map<String, Integer> asMap() {
        return Collections.unmodifiableMap(counts);
    }

    private static String checkType(String type) {
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("Supply type is required");
        }
        return type;
    }

    private static int checkQuantity(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        return quantity;
    }
}
