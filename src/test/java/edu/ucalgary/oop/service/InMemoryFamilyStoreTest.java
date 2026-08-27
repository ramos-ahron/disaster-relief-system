package edu.ucalgary.oop.service;

public class InMemoryFamilyStoreTest extends FamilyStoreContract {
    @Override
    protected FamilyStore emptyStore() {
        InMemoryFamilyStore s = new InMemoryFamilyStore();
        long next = 1;
        for (String name : new String[] {"G", "P1", "P2", "Other", "C", "S", "X"}) {
            s.addPerson(next++, name);
        }
        return s;
    }
}
