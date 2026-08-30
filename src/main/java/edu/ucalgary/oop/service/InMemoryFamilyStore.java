package edu.ucalgary.oop.service;

import edu.ucalgary.oop.model.FamilyMember;
import edu.ucalgary.oop.model.ParentKind;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * FamilyStore without a database, for tests and demos. FamilyMember.link enforces the same
 * rules as the ParentChild table. Not thread safe.
 */
public final class InMemoryFamilyStore implements FamilyStore {
    private final Map<Long, FamilyMember> people = new LinkedHashMap<>();
    private final Set<List<Object>> partnerships = new HashSet<>();

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

    @Override
    public FamilyMember ancestry(long personId, int maxDepth) {
        return copy(personId, maxDepth, true);
    }

    @Override
    public FamilyMember descendancy(long personId, int maxDepth) {
        return copy(personId, maxDepth, false);
    }

    @Override
    public long addPartnership(long a, long b, LocalDate start, LocalDate end) {
        person(a);
        person(b);
        if (a == b) {
            throw new IllegalArgumentException("A person cannot partner themselves");
        }
        if (end != null && end.isBefore(start)) {
            throw new IllegalArgumentException("End date is before start date");
        }
        if (!partnerships.add(List.of(Math.min(a, b), Math.max(a, b), start))) {
            throw new IllegalArgumentException("Partnership already exists");
        }
        return partnerships.size();
    }

    @Override
    public boolean isReady() {
        return true;
    }

    private FamilyMember person(long id) {
        FamilyMember m = people.get(id);
        if (m == null) {
            throw new IllegalArgumentException("Unknown person");
        }
        return m;
    }

    // A detached copy cut at maxDepth, so callers see the same shape JdbcFamilyStore returns.
    private FamilyMember copy(long personId, int maxDepth, boolean up) {
        FamilyMember source = person(personId);
        Map<FamilyMember, Integer> reach = up ? source.ancestors() : source.descendants();
        Map<Long, FamilyMember> copies = new HashMap<>();
        FamilyMember root = new FamilyMember(source.getId(), source.getName());
        copies.put(root.getId(), root);
        for (Map.Entry<FamilyMember, Integer> e : reach.entrySet()) {
            if (e.getValue() <= maxDepth) {
                copies.put(e.getKey().getId(), new FamilyMember(e.getKey().getId(), e.getKey().getName()));
            }
        }
        for (FamilyMember original : people.values()) {
            for (Map.Entry<FamilyMember, ParentKind> child : original.getChildren().entrySet()) {
                FamilyMember p = copies.get(original.getId());
                FamilyMember c = copies.get(child.getKey().getId());
                if (p != null && c != null) {
                    FamilyMember.link(p, c, child.getValue());
                }
            }
        }
        return root;
    }
}
