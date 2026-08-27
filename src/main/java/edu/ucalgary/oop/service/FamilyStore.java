package edu.ucalgary.oop.service;

import edu.ucalgary.oop.model.FamilyMember;

import java.util.List;

/**
 * Parent-child links and partnerships between victims.
 * Every rule violation (self link, duplicate, third biological parent, cycle, unknown person,
 * bad dates) is an IllegalArgumentException with a message fit to show a user.
 * Implementations: JdbcFamilyStore (PostgreSQL) and InMemoryFamilyStore (tests, demos).
 */
public interface FamilyStore {
    List<FamilyMember> persons();
}
