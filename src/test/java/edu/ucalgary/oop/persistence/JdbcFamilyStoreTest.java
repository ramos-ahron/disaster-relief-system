package edu.ucalgary.oop.persistence;

import edu.ucalgary.oop.model.ParentKind;
import edu.ucalgary.oop.service.FamilyStore;
import edu.ucalgary.oop.service.FamilyStoreContract;
import org.junit.BeforeClass;
import org.junit.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;

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

    @Test
    public void walkStopsOnStoredCycleAndReportsIt() throws SQLException {
        TestDatabase.run(db, "ALTER TABLE ParentChild DISABLE TRIGGER parentchild_guard;"
                + " INSERT INTO ParentChild (parent_id, child_id, kind) VALUES (" + id.get("P1") + ", " + id.get("P2")
                + ", 'STEP'), (" + id.get("P2") + ", " + id.get("P1") + ", 'STEP');"
                + " ALTER TABLE ParentChild ENABLE TRIGGER parentchild_guard");
        assertThrows(IllegalStateException.class, () -> store.ancestry(id.get("P1"), 50));
    }

    @Test
    public void oppositeHalvesOfACycleCannotBothCommit() throws Exception {
        CountDownLatch firstInserted = new CountDownLatch(1);
        CountDownLatch secondStarted = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        Future<String> first = pool.submit(() -> {
            try (Connection c = db.connect()) {
                c.setAutoCommit(false);
                JdbcFamilyStore.insertParentChild(c, id.get("P1"), id.get("P2"), ParentKind.STEP);
                firstInserted.countDown();
                secondStarted.await();
                Thread.sleep(500);   // let the other transaction reach the lock
                c.commit();
                return null;
            }
        });
        Future<String> second = pool.submit(() -> {
            try (Connection c = db.connect()) {
                c.setAutoCommit(false);
                firstInserted.await();
                secondStarted.countDown();
                try {
                    JdbcFamilyStore.insertParentChild(c, id.get("P2"), id.get("P1"), ParentKind.STEP);
                    c.commit();
                    return null;
                } catch (SQLException e) {
                    c.rollback();
                    return e.getSQLState();
                }
            }
        });
        assertNull(first.get(30, TimeUnit.SECONDS));
        assertEquals("FC001", second.get(30, TimeUnit.SECONDS));
        pool.shutdown();
        try (Connection c = db.connect(); Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT count(*) FROM ParentChild")) {
            rs.next();
            assertEquals(1, rs.getInt(1));
        }
    }

    @Test
    public void notReadyWhenTheDatabaseIsUnreachable() {
        assertFalse(new JdbcFamilyStore(new Database("jdbc:postgresql://127.0.0.1:1/none", "x", "x")).isReady());
    }
}
