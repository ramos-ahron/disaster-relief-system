package edu.ucalgary.oop.api;

import com.sun.net.httpserver.HttpServer;
import edu.ucalgary.oop.model.FamilyMember;
import edu.ucalgary.oop.model.ParentKind;
import edu.ucalgary.oop.service.FamilyStore;
import edu.ucalgary.oop.service.InMemoryFamilyStore;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import static org.junit.Assert.assertEquals;

/** Routing, status codes and JSON shape over a real HTTP socket, with an in-memory store. */
public class ApiServerTest {
    private InMemoryFamilyStore store;
    private HttpServer http;
    private boolean ready = true;
    private boolean broken;

    @Before
    public void start() throws IOException {
        store = new InMemoryFamilyStore().addPerson(1, "Ann \"A\" Lee").addPerson(2, "Bo").addPerson(3, "Cy");
        FamilyStore wrapped = new FamilyStore() {   // the real store, plus switches for failure cases
            public List<FamilyMember> persons() {
                return store.persons();
            }

            public void addParent(long p, long c, ParentKind k) {
                if (broken) {
                    throw new IllegalStateException("database down");
                }
                store.addParent(p, c, k);
            }

            public void removeParent(long p, long c) {
                store.removeParent(p, c);
            }

            public FamilyMember ancestry(long id, int d) {
                return store.ancestry(id, d);
            }

            public FamilyMember descendancy(long id, int d) {
                return store.descendancy(id, d);
            }

            public long addPartnership(long a, long b, LocalDate s, LocalDate e) {
                return store.addPartnership(a, b, s, e);
            }

            public boolean isReady() {
                return ready;
            }
        };
        http = new ApiServer(wrapped).start(0);
    }

    @After
    public void stop() {
        http.stop(0);
    }

    private String[] call(String method, String path, String form) throws IOException {
        HttpURLConnection c = (HttpURLConnection) URI.create("http://localhost:" + http.getAddress().getPort() + path)
                .toURL().openConnection();
        c.setRequestMethod(method);
        if (form != null) {
            c.setDoOutput(true);
            try (OutputStream out = c.getOutputStream()) {
                out.write(form.getBytes(StandardCharsets.UTF_8));
            }
        }
        int status = c.getResponseCode();
        try (InputStream in = status >= 400 ? c.getErrorStream() : c.getInputStream()) {
            return new String[] {String.valueOf(status), new String(in.readAllBytes(), StandardCharsets.UTF_8)};
        }
    }

    @Test
    public void healthAndReadiness() throws IOException {
        assertEquals("200", call("GET", "/health", null)[0]);
        assertEquals("200", call("GET", "/ready", null)[0]);
        ready = false;
        assertEquals("503", call("GET", "/ready", null)[0]);
    }

    @Test
    public void unknownRoute() throws IOException {
        assertEquals("404", call("GET", "/nope", null)[0]);
        assertEquals("404", call("PUT", "/persons", null)[0]);
    }
}
