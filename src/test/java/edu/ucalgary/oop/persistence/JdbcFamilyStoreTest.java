package edu.ucalgary.oop.persistence;

import edu.ucalgary.oop.service.FamilyStore;
import edu.ucalgary.oop.service.FamilyStoreContract;
import org.junit.BeforeClass;

import java.sql.SQLException;

/** The shared FamilyStore contract plus what only a real database can show: the trigger lock and bad stored data. */
public class JdbcFamilyStoreTest extends FamilyStoreContract {
    private static Database db;

    @BeforeClass
    public static void schema() throws Exception {
        db = TestDatabase.require();
        TestDatabase.recreateSchema(db);
    }

    @Override
    protected FamilyStore emptyStore() throws SQLException {
        TestDatabase.run(db, TestDatabase.EMPTY_ALL);
        TestDatabase.run(db, "INSERT INTO Location (name, address, shelter) VALUES ('Hall', '1 Main St', true);"
                + " INSERT INTO DisasterVictim (first_name, location_id) VALUES"
                + " ('G', 1), ('P1', 1), ('P2', 1), ('Other', 1), ('C', 1), ('S', 1), ('X', 1)");
        return new JdbcFamilyStore(db);
    }
}
