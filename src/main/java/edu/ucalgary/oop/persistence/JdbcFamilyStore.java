package edu.ucalgary.oop.persistence;

import edu.ucalgary.oop.model.FamilyMember;
import edu.ucalgary.oop.service.FamilyStore;

import java.sql.Connection;
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

    private static String name(String alias) {
        return alias + ".first_name || COALESCE(' ' || " + alias + ".last_name, '')";
    }
}
