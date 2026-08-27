package edu.ucalgary.oop.service;

import edu.ucalgary.oop.model.FamilyMember;
import edu.ucalgary.oop.model.ParentKind;

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

    @Override
    public void addParent(long parentId, long childId, ParentKind kind) {
        FamilyMember.link(person(parentId), person(childId), kind);
    }

    @Override
    public void removeParent(long parentId, long childId) {
        FamilyMember.unlink(person(parentId), person(childId));
    }

    private FamilyMember person(long id) {
        FamilyMember m = people.get(id);
        if (m == null) {
            throw new IllegalArgumentException("Unknown person");
        }
        return m;
    }
}
