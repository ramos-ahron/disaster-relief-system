package edu.ucalgary.oop.persistence;

import edu.ucalgary.oop.model.FamilyMember;
import edu.ucalgary.oop.model.ParentKind;
import edu.ucalgary.oop.service.FamilyStore;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * FamilyStore on the ParentChild and Partnership tables. The rules live in the database
 * (database/migrations/V2__family.sql); this class turns their SQL states into messages.
 * Each call opens a connection and runs one statement in autocommit.
 * Known limit: no connection pool. Add one if request volume grows.
 */
public final class JdbcFamilyStore implements FamilyStore {
    // %1$s is the column matched against the current person, %2$s the column that is followed.
    // UNION drops repeated rows, so shared ancestors are expanded once per depth.
    // The depth limit also ends the walk if bad data contains a cycle.
    private static final String WALK =
            "WITH RECURSIVE walk(parent_id, child_id, kind, depth) AS ("
            + " SELECT parent_id, child_id, kind, 1 FROM ParentChild WHERE %1$s = ?"
            + " UNION"
            + " SELECT pc.parent_id, pc.child_id, pc.kind, walk.depth + 1"
            + " FROM ParentChild pc JOIN walk ON pc.%1$s = walk.%2$s WHERE walk.depth < ?)"
            + " SELECT w.parent_id, w.child_id, w.kind, MIN(w.depth) AS depth,"
            + " " + name("p") + " AS parent_name, " + name("c") + " AS child_name"
            + " FROM walk w"
            + " JOIN DisasterVictim p ON p.victim_id = w.parent_id"
            + " JOIN DisasterVictim c ON c.victim_id = w.child_id"
            + " GROUP BY w.parent_id, w.child_id, w.kind, p.first_name, p.last_name, c.first_name, c.last_name"
            + " ORDER BY depth, w.parent_id, w.child_id";
    private static final String UP = String.format(WALK, "child_id", "parent_id");
    private static final String DOWN = String.format(WALK, "parent_id", "child_id");

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

    @Override
    public FamilyMember ancestry(long personId, int maxDepth) {
        return graph(personId, maxDepth, UP);
    }

    @Override
    public FamilyMember descendancy(long personId, int maxDepth) {
        return graph(personId, maxDepth, DOWN);
    }

    @Override
    public long addPartnership(long a, long b, LocalDate start, LocalDate end) {
        if (a == b) {
            throw new IllegalArgumentException("A person cannot partner themselves");
        }
        try (Connection c = database.connect(); PreparedStatement ps = c.prepareStatement(
                "INSERT INTO Partnership (person1_id, person2_id, start_date, end_date) VALUES (?, ?, ?, ?)"
                        + " RETURNING partnership_id")) {
            ps.setLong(1, Math.min(a, b));
            ps.setLong(2, Math.max(a, b));
            ps.setDate(3, Date.valueOf(start));
            ps.setDate(4, end == null ? null : Date.valueOf(end));
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            throw switch (String.valueOf(e.getSQLState())) {
                case "23505" -> new IllegalArgumentException("Partnership already exists");
                case "23503" -> new IllegalArgumentException("Unknown person");
                case "23514" -> new IllegalArgumentException("End date is before start date");
                default -> new IllegalStateException(e);
            };
        }
    }

    @Override
    public boolean isReady() {
        try (Connection c = database.connect()) {
            return c.isValid(2);
        } catch (SQLException e) {
            return false;
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

    private FamilyMember graph(long personId, int maxDepth, String sql) {
        try (Connection c = database.connect()) {
            Map<Long, FamilyMember> byId = new HashMap<>();
            try (PreparedStatement ps = c.prepareStatement("SELECT " + name("v") + " FROM DisasterVictim v WHERE victim_id = ?")) {
                ps.setLong(1, personId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        throw new IllegalArgumentException("Unknown person");
                    }
                    byId.put(personId, new FamilyMember(personId, rs.getString(1)));
                }
            }
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setLong(1, personId);
                ps.setInt(2, maxDepth);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String parentName = rs.getString("parent_name");
                        String childName = rs.getString("child_name");
                        FamilyMember parent = byId.computeIfAbsent(rs.getLong("parent_id"), id -> new FamilyMember(id, parentName));
                        FamilyMember child = byId.computeIfAbsent(rs.getLong("child_id"), id -> new FamilyMember(id, childName));
                        try {
                            FamilyMember.link(parent, child, ParentKind.valueOf(rs.getString("kind")));
                        } catch (IllegalArgumentException bad) {
                            throw new IllegalStateException("Stored family data is inconsistent: " + bad.getMessage());
                        }
                    }
                }
            }
            return byId.get(personId);
        } catch (SQLException e) {
            throw new IllegalStateException(e);
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
