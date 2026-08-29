package edu.ucalgary.oop.service;

import edu.ucalgary.oop.model.FamilyMember;
import edu.ucalgary.oop.model.ParentKind;

import java.util.List;

/**
 * Parent-child links and partnerships between victims.
 * Every rule violation (self link, duplicate, third biological parent, cycle, unknown person,
 * bad dates) is an IllegalArgumentException with a message fit to show a user.
 * Implementations: JdbcFamilyStore (PostgreSQL) and InMemoryFamilyStore (tests, demos).
 */
public interface FamilyStore {
    List<FamilyMember> persons();

    void addParent(long parentId, long childId, ParentKind kind);

    void removeParent(long parentId, long childId);

    /** The person with ancestors linked above, at most maxDepth generations. */
    FamilyMember ancestry(long personId, int maxDepth);

    /** The person with descendants linked below, at most maxDepth generations. */
    FamilyMember descendancy(long personId, int maxDepth);
}
