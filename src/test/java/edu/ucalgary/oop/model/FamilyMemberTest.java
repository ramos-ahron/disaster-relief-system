package edu.ucalgary.oop.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class FamilyMemberTest {

    private static FamilyMember person(long id) {
        return new FamilyMember(id, "p" + id);
    }

    @Test
    public void linkAndUnlinkUpdateBothSides() {
        FamilyMember parent = person(1);
        FamilyMember child = person(2);
        FamilyMember.link(parent, child, ParentKind.ADOPTIVE);
        assertEquals(ParentKind.ADOPTIVE, parent.getChildren().get(child));
        assertEquals(ParentKind.ADOPTIVE, child.getParents().get(parent));
        FamilyMember.unlink(parent, child);
        assertTrue(parent.getChildren().isEmpty());
        assertTrue(child.getParents().isEmpty());
        assertThrows(IllegalArgumentException.class, () -> FamilyMember.unlink(parent, child));
    }

    @Test
    public void rejectsSelfDuplicateAndThirdBiologicalParent() {
        FamilyMember a = person(1);
        FamilyMember b = person(2);
        FamilyMember c = person(3);
        FamilyMember d = person(4);
        assertThrows(IllegalArgumentException.class, () -> FamilyMember.link(a, a, ParentKind.STEP));

        FamilyMember.link(a, d, ParentKind.BIOLOGICAL);
        assertThrows(IllegalArgumentException.class, () -> FamilyMember.link(a, d, ParentKind.ADOPTIVE));

        FamilyMember.link(b, d, ParentKind.BIOLOGICAL);
        assertThrows(IllegalArgumentException.class, () -> FamilyMember.link(c, d, ParentKind.BIOLOGICAL));
        FamilyMember.link(c, d, ParentKind.STEP);
        assertEquals(3, d.getParents().size());
    }

    @Test
    public void rejectsDirectAndTransitiveCycles() {
        FamilyMember a = person(1);
        FamilyMember b = person(2);
        FamilyMember c = person(3);
        FamilyMember.link(a, b, ParentKind.BIOLOGICAL);
        FamilyMember.link(b, c, ParentKind.BIOLOGICAL);
        assertThrows(IllegalArgumentException.class, () -> FamilyMember.link(b, a, ParentKind.STEP));
        assertThrows(IllegalArgumentException.class, () -> FamilyMember.link(c, a, ParentKind.STEP));
        assertTrue(c.getChildren().isEmpty());
        assertTrue(a.getParents().isEmpty());
    }
}
