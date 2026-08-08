package edu.ucalgary.oop.model;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/** The allowed gender values, read once from gender-options.txt on the classpath. */
public final class GenderOptions {
    private static final List<String> ALL = load();

    private GenderOptions() {
    }

    public static List<String> all() {
        return ALL;
    }

    public static boolean isValid(String gender) {
        return ALL.contains(gender);
    }

    private static List<String> load() {
        try (InputStream in = GenderOptions.class.getResourceAsStream("/gender-options.txt")) {
            if (in == null) {
                throw new IllegalStateException("gender-options.txt is missing from the classpath");
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            return reader.lines().map(String::trim).filter(s -> !s.isEmpty()).toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
