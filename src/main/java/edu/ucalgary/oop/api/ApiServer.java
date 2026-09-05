package edu.ucalgary.oop.api;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import edu.ucalgary.oop.model.FamilyMember;
import edu.ucalgary.oop.model.ParentKind;
import edu.ucalgary.oop.service.FamilyStore;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * HTTP API over a FamilyStore, on the JDK's built-in HttpServer. Answers are JSON.
 * Known limit: no authentication or TLS. Keep it behind a cluster-internal Service
 * or an authenticating proxy.
 */
public final class ApiServer {
    private static final Pattern WALK = Pattern.compile("/persons/(\\d+)/(ancestors|descendants)");
    private static final int DEFAULT_DEPTH = 50;
    private static final int MAX_BODY = 4096;

    private final FamilyStore store;

    public ApiServer(FamilyStore store) {
        this.store = store;
    }

    /** Starts listening; port 0 picks a free port (read it back with getAddress). */
    public HttpServer start(int port) throws IOException {
        HttpServer http = HttpServer.create(new InetSocketAddress(port), 0);
        http.createContext("/", this::handle);
        http.start();
        return http;
    }

    private void handle(HttpExchange ex) throws IOException {
        int status = 200;
        String body;
        try {
            Map<String, String> params = params(ex);
            String route = ex.getRequestMethod() + " " + ex.getRequestURI().getPath();
            Matcher walk = WALK.matcher(ex.getRequestURI().getPath());
            if (route.equals("GET /health")) {
                body = "{\"status\":\"ok\"}";
            } else if (route.equals("GET /ready")) {
                boolean ready = store.isReady();
                status = ready ? 200 : 503;
                body = "{\"ready\":" + ready + "}";
            } else if (route.equals("GET /persons")) {
                StringJoiner list = new StringJoiner(",", "[", "]");
                store.persons().forEach(m -> list.add(node(m, null)));
                body = list.toString();
            } else if (ex.getRequestMethod().equals("GET") && walk.matches()) {
                int depth = params.containsKey("maxDepth") ? Integer.parseInt(params.get("maxDepth")) : DEFAULT_DEPTH;
                long id = Long.parseLong(walk.group(1));
                boolean up = walk.group(2).equals("ancestors");
                body = graph(up ? store.ancestry(id, depth) : store.descendancy(id, depth), up);
            } else if (route.equals("POST /parent-child")) {
                store.addParent(number(params, "parent"), number(params, "child"), kind(required(params, "kind")));
                status = 201;
                body = "{\"created\":true}";
            } else if (route.equals("DELETE /parent-child")) {
                store.removeParent(number(params, "parent"), number(params, "child"));
                body = "{\"deleted\":true}";
            } else if (route.equals("POST /partnerships")) {
                String end = params.get("end");
                long id = store.addPartnership(number(params, "person1"), number(params, "person2"),
                        LocalDate.parse(required(params, "start")), end == null || end.isEmpty() ? null : LocalDate.parse(end));
                status = 201;
                body = "{\"id\":" + id + "}";
            } else {
                status = 404;
                body = error("Not found");
            }
        } catch (IllegalArgumentException | DateTimeParseException e) {   // includes NumberFormatException
            status = 400;
            body = error(e.getMessage());
        } catch (RuntimeException e) {
            e.printStackTrace();
            status = 500;
            body = error("Internal error");
        }
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = ex.getResponseBody()) {
            out.write(bytes);
        }
    }

    // Flat graph: nodes with their distance from the root, and the links between them.
    // A nested tree would repeat shared ancestors.
    private static String graph(FamilyMember root, boolean up) {
        Map<FamilyMember, Integer> nodes = new LinkedHashMap<>();
        nodes.put(root, 0);
        nodes.putAll(up ? root.ancestors() : root.descendants());
        StringJoiner nodeList = new StringJoiner(",", "[", "]");
        StringJoiner edgeList = new StringJoiner(",", "[", "]");
        nodes.forEach((member, depth) -> {
            nodeList.add(node(member, depth));
            (up ? member.getParents() : member.getChildren()).forEach((other, kind) -> {
                FamilyMember parent = up ? other : member;
                FamilyMember child = up ? member : other;
                edgeList.add("{\"parent\":" + parent.getId() + ",\"child\":" + child.getId() + ",\"kind\":\"" + kind + "\"}");
            });
        });
        return "{\"root\":" + root.getId() + ",\"nodes\":" + nodeList + ",\"edges\":" + edgeList + "}";
    }

    private static String node(FamilyMember m, Integer depth) {
        return "{\"id\":" + m.getId() + ",\"name\":" + quote(m.getName()) + (depth == null ? "" : ",\"depth\":" + depth) + "}";
    }

    private static Map<String, String> params(HttpExchange ex) throws IOException {
        Map<String, String> out = new HashMap<>();
        parse(ex.getRequestURI().getRawQuery(), out);
        try (InputStream in = ex.getRequestBody()) {
            byte[] body = in.readNBytes(MAX_BODY + 1);
            if (body.length > MAX_BODY) {
                throw new IllegalArgumentException("Request body too large");
            }
            parse(new String(body, StandardCharsets.UTF_8), out);   // form bodies only
        }
        return out;
    }

    private static void parse(String encoded, Map<String, String> into) {
        if (encoded == null || encoded.isEmpty()) {
            return;
        }
        for (String pair : encoded.split("&")) {
            int eq = pair.indexOf('=');
            if (eq > 0) {
                into.put(URLDecoder.decode(pair.substring(0, eq), StandardCharsets.UTF_8),
                        URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8));
            }
        }
    }

    private static String required(Map<String, String> params, String name) {
        String v = params.get(name);
        if (v == null || v.isEmpty()) {
            throw new IllegalArgumentException("Missing parameter: " + name);
        }
        return v;
    }

    private static long number(Map<String, String> params, String name) {
        try {
            return Long.parseLong(required(params, name));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(name + " must be a number");
        }
    }

    private static ParentKind kind(String text) {
        try {
            return ParentKind.valueOf(text.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("kind must be BIOLOGICAL, ADOPTIVE or STEP");
        }
    }

    private static String error(String message) {
        return "{\"error\":" + quote(message) + "}";
    }

    static String quote(String s) {
        StringBuilder sb = new StringBuilder("\"");
        for (char ch : String.valueOf(s).toCharArray()) {
            if (ch == '"' || ch == '\\') {
                sb.append('\\').append(ch);
            } else if (ch < 0x20) {
                sb.append(String.format("\\u%04x", (int) ch));
            } else {
                sb.append(ch);
            }
        }
        return sb.append('"').toString();
    }
}
