package edu.ucalgary.oop.persistence;

import edu.ucalgary.oop.model.FamilyMember;
import edu.ucalgary.oop.model.ParentKind;
import edu.ucalgary.oop.service.FamilyStore;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * FamilyStore on the ParentChild and Partnership tables. The rules live in the database
 * (database/migrations/V2__family.sql); this class turns their SQL states into messages.
 * Each call opens a connection and runs one statement in autocommit.
 * Known limit: no connection pool. Add one if request volume grows.
 */
public final class JdbcFamilyStore implements FamilyStore {
    private final Database database;

    public JdbcFamilyStore(Database database) {
        this.database = database;
    }

    @Override
    public List<FamilyMember> persons() {
        try (Connection c = database.connect(); Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT victim_id, " + name("v") + " FROM DisasterVictim v ORDER BY victim_id")) {
            List<FamilyMember> out = new ArrayList<>();
            while (rs.next()) {
                out.add(new FamilyMember(rs.getLong(1), rs.getString(2)));
            }
            return out;
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void addParent(long parentId, long childId, ParentKind kind) {
        try (Connection c = database.connect()) {
            insertParentChild(c, parentId, childId, kind);
        } catch (SQLException e) {
            throw explainParentChild(e);
        }
    }

    @Override
    public void removeParent(long parentId, long childId) {
        try (Connection c = database.connect();
             PreparedStatement ps = c.prepareStatement("DELETE FROM ParentChild WHERE parent_id = ? AND child_id = ?")) {
            ps.setLong(1, parentId);
            ps.setLong(2, childId);
            if (ps.executeUpdate() == 0) {
                throw new IllegalArgumentException("Relation not found");
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Package private so a test can run it inside its own transaction. */
    static void insertParentChild(Connection c, long parentId, long childId, ParentKind kind) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("INSERT INTO ParentChild (parent_id, child_id, kind) VALUES (?, ?, ?)")) {
            ps.setLong(1, parentId);
            ps.setLong(2, childId);
            ps.setString(3, kind.name());
            ps.executeUpdate();
        }
    }

    // FC001 and FC002 are raised by the parentchild_guard trigger.
    private static RuntimeException explainParentChild(SQLException e) {
        String msg = String.valueOf(e.getMessage());
        return switch (String.valueOf(e.getSQLState())) {
            case "FC001" -> new IllegalArgumentException("Relation would create a cycle");
            case "FC002" -> new IllegalArgumentException("Child already has two biological parents");
            case "23505" -> new IllegalArgumentException(msg.contains("parentchild_bio_slot_uq")
                    ? "Child already has two biological parents" : "Relation already exists");
            case "23503" -> new IllegalArgumentException("Unknown person");
            case "23514" -> new IllegalArgumentException(msg.contains("parentchild_no_self")
                    ? "A person cannot be their own parent" : "Unknown relation kind");
            default -> new IllegalStateException(e);
        };
    }

    private static String name(String alias) {
        return alias + ".first_name || COALESCE(' ' || " + alias + ".last_name, '')";
    }
}
