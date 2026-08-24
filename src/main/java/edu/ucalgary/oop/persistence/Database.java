package edu.ucalgary.oop.persistence;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/** Where the PostgreSQL database is. Each call to connect opens a new connection. */
public record Database(String url, String user, String password) {

    /**
     * Reads DB_URL, DB_USER and DB_PASSWORD. Missing values fall back to the local
     * development database from docker-compose.yml.
     */
    public static Database fromEnvironment() {
        return new Database(
                env("DB_URL", "jdbc:postgresql://localhost:5432/ensf380project"),
                env("DB_USER", "oop"),
                env("DB_PASSWORD", "ucalgary"));
    }

    public Connection connect() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    @Override
    public String toString() {
        return "Database[" + url + " as " + user + "]";   // never print the password
    }

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }
}
