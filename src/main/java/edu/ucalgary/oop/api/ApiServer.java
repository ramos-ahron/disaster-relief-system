package edu.ucalgary.oop.api;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import edu.ucalgary.oop.service.FamilyStore;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

/**
 * HTTP API over a FamilyStore, on the JDK's built-in HttpServer. Answers are JSON.
 * Known limit: no authentication or TLS. Keep it behind a cluster-internal Service
 * or an authenticating proxy.
 */
public final class ApiServer {
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
            String route = ex.getRequestMethod() + " " + ex.getRequestURI().getPath();
            if (route.equals("GET /health")) {
                body = "{\"status\":\"ok\"}";
            } else if (route.equals("GET /ready")) {
                boolean ready = store.isReady();
                status = ready ? 200 : 503;
                body = "{\"ready\":" + ready + "}";
            } else {
                status = 404;
                body = error("Not found");
            }
        } catch (IllegalArgumentException e) {
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
