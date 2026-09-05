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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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
    public void listsPersonsWithEscapedNames() throws IOException {
        String[] r = call("GET", "/persons", null);
        assertEquals("200", r[0]);
        assertEquals("[{\"id\":1,\"name\":\"Ann \\\"A\\\" Lee\"},{\"id\":2,\"name\":\"Bo\"},{\"id\":3,\"name\":\"Cy\"}]", r[1]);
    }

    @Test
    public void addThenWalkAncestorsAndDescendants() throws IOException {
        assertEquals("201", call("POST", "/parent-child", "parent=2&child=3&kind=biological")[0]);
        assertEquals("201", call("POST", "/parent-child", "parent=1&child=2&kind=STEP")[0]);
        String[] up = call("GET", "/persons/3/ancestors", null);
        assertEquals("200", up[0]);
        assertEquals("{\"root\":3,\"nodes\":[{\"id\":3,\"name\":\"Cy\",\"depth\":0},{\"id\":2,\"name\":\"Bo\",\"depth\":1},"
                + "{\"id\":1,\"name\":\"Ann \\\"A\\\" Lee\",\"depth\":2}],"
                + "\"edges\":[{\"parent\":2,\"child\":3,\"kind\":\"BIOLOGICAL\"},{\"parent\":1,\"child\":2,\"kind\":\"STEP\"}]}", up[1]);
        String[] shallow = call("GET", "/persons/1/descendants?maxDepth=1", null);
        assertEquals("{\"root\":1,\"nodes\":[{\"id\":1,\"name\":\"Ann \\\"A\\\" Lee\",\"depth\":0},{\"id\":2,\"name\":\"Bo\",\"depth\":1}],"
                + "\"edges\":[{\"parent\":1,\"child\":2,\"kind\":\"STEP\"}]}", shallow[1]);
        assertEquals("200", call("DELETE", "/parent-child?parent=1&child=2", null)[0]);
        assertEquals("400", call("DELETE", "/parent-child?parent=1&child=2", null)[0]);
    }

    @Test
    public void partnerships() throws IOException {
        assertEquals("201", call("POST", "/partnerships", "person1=1&person2=2&start=2020-01-01")[0]);
        assertEquals("400", call("POST", "/partnerships", "person1=2&person2=1&start=2020-01-01")[0]);
        assertEquals("400", call("POST", "/partnerships", "person1=1&person2=3&start=yesterday")[0]);
    }

    @Test
    public void badInputIsBadRequestAndInternalErrorsAreHidden() throws IOException {
        String[] self = call("POST", "/parent-child", "parent=3&child=3&kind=STEP");
        assertEquals("400", self[0]);
        assertTrue(self[1].contains("own parent"));
        assertEquals("400", call("POST", "/parent-child", "parent=1&child=x&kind=STEP")[0]);
        assertEquals("400", call("POST", "/parent-child", "parent=1&child=2&kind=FOSTER")[0]);
        assertEquals("400", call("POST", "/parent-child", "parent=1&kind=STEP")[0]);
        assertEquals("400", call("GET", "/persons/99/ancestors", null)[0]);
        assertEquals("400", call("POST", "/parent-child", "x=" + "a".repeat(5000))[0]);
        broken = true;
        String[] boom = call("POST", "/parent-child", "parent=1&child=2&kind=STEP");
        assertEquals("500", boom[0]);
        assertFalse(boom[1].contains("database down"));
    }

    @Test
    public void unknownRoute() throws IOException {
        assertEquals("404", call("GET", "/nope", null)[0]);
        assertEquals("404", call("PUT", "/persons", null)[0]);
    }
}
