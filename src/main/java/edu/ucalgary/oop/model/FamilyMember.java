package edu.ucalgary.oop.model;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * A person in the family graph. The graph is a DAG, not a tree: a person can have
 * several parents and ancestors can be shared.
 * Parent and child sides change only through link and unlink, which update both
 * members together.
 */
public final class FamilyMember {
    private final long id;
    private final String name;
    private final Map<FamilyMember, ParentKind> parents = new LinkedHashMap<>();
    private final Map<FamilyMember, ParentKind> children = new LinkedHashMap<>();

    public FamilyMember(long id, String name) {
        this.id = id;
        this.name = name;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Map<FamilyMember, ParentKind> getParents() {
        return Collections.unmodifiableMap(parents);
    }

    public Map<FamilyMember, ParentKind> getChildren() {
        return Collections.unmodifiableMap(children);
    }

    /** Makes parent a parent of child on both sides. Same rules as the ParentChild table. */
    public static void link(FamilyMember parent, FamilyMember child, ParentKind kind) {
        if (parent == null || child == null || kind == null) {
            throw new NullPointerException("parent, child and kind are required");
        }
        if (parent == child) {
            throw new IllegalArgumentException("A person cannot be their own parent");
        }
        if (parent.children.containsKey(child)) {
            throw new IllegalArgumentException("Relation already exists");
        }
        if (kind == ParentKind.BIOLOGICAL && child.biologicalParentCount() >= 2) {
            throw new IllegalArgumentException("Child already has two biological parents");
        }
        if (parent.ancestorsOrSelf().contains(child)) {
            throw new IllegalArgumentException("Relation would create a cycle");
        }
        parent.children.put(child, kind);
        child.parents.put(parent, kind);
    }

    /** Removes the link on both sides. */
    public static void unlink(FamilyMember parent, FamilyMember child) {
        if (parent.children.remove(child) == null) {
            throw new IllegalArgumentException("Relation not found");
        }
        child.parents.remove(parent);
    }

    private int biologicalParentCount() {
        int n = 0;
        for (ParentKind kind : parents.values()) {
            if (kind == ParentKind.BIOLOGICAL) {
                n++;
            }
        }
        return n;
    }

    private Set<FamilyMember> ancestorsOrSelf() {
        Set<FamilyMember> all = new HashSet<>(reach(true).keySet());
        all.add(this);
        return all;
    }

    // Breadth first, so the first visit of a member is its shortest distance.
    private Map<FamilyMember, Integer> reach(boolean up) {
        Map<FamilyMember, Integer> found = new LinkedHashMap<>();
        Deque<FamilyMember> queue = new ArrayDeque<>();
        queue.add(this);
        while (!queue.isEmpty()) {
            FamilyMember current = queue.remove();
            int depth = current == this ? 0 : found.get(current);
            for (FamilyMember next : (up ? current.parents : current.children).keySet()) {
                if (next != this && !found.containsKey(next)) {
                    found.put(next, depth + 1);
                    queue.add(next);
                }
            }
        }
        return found;
    }
}
