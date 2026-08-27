package edu.ucalgary.oop.service;

import edu.ucalgary.oop.model.FamilyMember;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * FamilyStore without a database, for tests and demos. FamilyMember.link enforces the same
 * rules as the ParentChild table. Not thread safe.
 */
public final class InMemoryFamilyStore implements FamilyStore {
    private final Map<Long, FamilyMember> people = new LinkedHashMap<>();

    public InMemoryFamilyStore addPerson(long id, String name) {
        people.put(id, new FamilyMember(id, name));
        return this;
    }

    @Override
    public List<FamilyMember> persons() {
        return new ArrayList<>(people.values());
    }
}
