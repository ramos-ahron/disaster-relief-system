package edu.ucalgary.oop.persistence;

import edu.ucalgary.oop.model.DietaryRestriction;
import edu.ucalgary.oop.model.DisasterVictim;
import edu.ucalgary.oop.model.GenderOptions;
import edu.ucalgary.oop.model.Inquiry;
import edu.ucalgary.oop.model.Location;
import edu.ucalgary.oop.model.MedicalRecord;
import edu.ucalgary.oop.model.Relationship;
import edu.ucalgary.oop.model.RelationshipType;
import edu.ucalgary.oop.service.ReliefRegistry;

import java.sql.Connection;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * Reads every table once and builds a ReliefRegistry. Rows are matched by their database ids.
 * Known limit: read only. Changes made in the app are not written back.
 */
public final class RegistryLoader {
    private RegistryLoader() {
    }

    public static ReliefRegistry load(Database database) throws SQLException {
        try (Connection c = database.connect()) {
            return load(c);
        }
    }

    public static ReliefRegistry load(Connection c) throws SQLException {
        ReliefRegistry registry = new ReliefRegistry();
        Map<Integer, Location> locations = new HashMap<>();
        try (Statement st = c.createStatement()) {
            try (ResultSet rs = st.executeQuery("SELECT location_id, name, address, shelter FROM Location ORDER BY location_id")) {
                while (rs.next()) {
                    Location l = registry.addLocation(new Location(rs.getString(2), rs.getString(3), rs.getBoolean(4)));
                    locations.put(rs.getInt(1), l);
                }
            }
            try (ResultSet rs = st.executeQuery("SELECT victim_id, first_name, last_name, entry_date, gender, comments,"
                    + " date_of_birth, location_id FROM DisasterVictim ORDER BY victim_id")) {
                while (rs.next()) {
                    LocalDate entry = date(rs, 4);
                    DisasterVictim v = new DisasterVictim(rs.getInt(1), rs.getString(2), rs.getString(3),
                            entry == null ? LocalDate.now() : entry);
                    String gender = rs.getString(5);
                    v.setGender(gender != null && GenderOptions.isValid(gender) ? gender : null);
                    v.setComments(rs.getString(6));
                    v.setDateOfBirth(date(rs, 7));
                    v.setLocation(locations.get(rs.getInt(8)));
                    registry.addVictim(v);
                }
            }
            try (ResultSet rs = st.executeQuery("SELECT type, quantity, reference_type, reference_id FROM Supply")) {
                while (rs.next()) {
                    if (rs.getInt(2) <= 0) {
                        continue;
                    }
                    if ("Location".equalsIgnoreCase(rs.getString(3))) {
                        locations.get(rs.getInt(4)).getSupplies().add(rs.getString(1), rs.getInt(2));
                    } else {
                        registry.requireVictim(rs.getInt(4)).getBelongings().add(rs.getString(1), rs.getInt(2));
                    }
                }
            }
            try (ResultSet rs = st.executeQuery("SELECT victim_id, location_id, treatment_details, date_of_treatment"
                    + " FROM MedicalRecord WHERE victim_id IS NOT NULL ORDER BY record_id")) {
                while (rs.next()) {
                    registry.requireVictim(rs.getInt(1))
                            .addMedicalRecord(new MedicalRecord(locations.get(rs.getInt(2)), rs.getString(3), date(rs, 4)));
                }
            }
            try (ResultSet rs = st.executeQuery("SELECT relationship_type, victim1_id, victim2_id FROM Relationship")) {
                while (rs.next()) {
                    registry.relate(new Relationship(registry.requireVictim(rs.getInt(2)),
                            registry.requireVictim(rs.getInt(3)), RelationshipType.parse(rs.getString(1))));
                }
            }
            try (ResultSet rs = st.executeQuery("SELECT victim_id, restriction_name FROM DietaryRestriction"
                    + " WHERE victim_id IS NOT NULL")) {
                while (rs.next()) {
                    registry.requireVictim(rs.getInt(1)).addDietaryRestriction(DietaryRestriction.parse(rs.getString(2)));
                }
            }
            try (ResultSet rs = st.executeQuery("SELECT i.firstName, i.lastName, i.phoneNumber, l.callDate, l.details"
                    + " FROM INQUIRER i JOIN INQUIRY_LOG l ON l.inquirer = i.id ORDER BY l.callDate, l.id")) {
                while (rs.next()) {
                    registry.inquirer(rs.getString(1), rs.getString(2), rs.getString(3))
                            .log(new Inquiry(date(rs, 4), rs.getString(5)));
                }
            }
        }
        return registry;
    }

    private static LocalDate date(ResultSet rs, int column) throws SQLException {
        Date d = rs.getDate(column);
        return d == null ? null : d.toLocalDate();
    }
}
