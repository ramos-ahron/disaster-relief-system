package edu.ucalgary.oop.persistence;

import org.junit.Assume;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.stream.Stream;

/**
 * The PostgreSQL database for integration tests, from TEST_DB_URL, TEST_DB_USER and
 * TEST_DB_PASSWORD. Tests that need it are skipped when TEST_DB_URL is not set.
 * The database is wiped: point it at a throwaway database only.
 */
final class TestDatabase {
    private TestDatabase() {
    }

    static Database require() {
        String url = System.getenv("TEST_DB_URL");
        Assume.assumeTrue("TEST_DB_URL not set, skipping database tests", url != null && !url.isBlank());
        return new Database(url, System.getenv("TEST_DB_USER"), System.getenv("TEST_DB_PASSWORD"));
    }

    /** Drops everything, then applies every migration twice (they must be replayable). */
    static void recreateSchema(Database db) throws SQLException, IOException {
        try (Connection c = db.connect(); Statement st = c.createStatement()) {
            st.execute("DROP SCHEMA public CASCADE; CREATE SCHEMA public");
            for (int pass = 0; pass < 2; pass++) {
                try (Stream<Path> files = Files.list(Paths.get("database", "migrations"))) {
                    for (Path f : files.filter(p -> p.getFileName().toString().matches("V\\d+__.*\\.sql")).sorted().toList()) {
                        st.execute(Files.readString(f));
                    }
                }
            }
        }
    }

    static void run(Database db, String sql) throws SQLException {
        try (Connection c = db.connect(); Statement st = c.createStatement()) {
            st.execute(sql);
        }
    }

    static void runFile(Database db, String path) throws SQLException, IOException {
        run(db, Files.readString(Paths.get(path)));
    }

    static final String EMPTY_ALL = "TRUNCATE INQUIRY_LOG, INQUIRER, Supply, MedicalRecord, Relationship,"
            + " DietaryRestriction, ParentChild, Partnership, DisasterVictim, Location RESTART IDENTITY CASCADE";
}
