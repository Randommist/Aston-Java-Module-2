package org.example.apigateway;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Простой HTTP-сервер вместо настоящего сервиса: отвечает фиксированным JSON
 * и запоминает заголовки последнего запроса.
 */
class StubService implements AutoCloseable {

    private final HttpServer server;
    private final Map<String, String> lastRequestHeaders = new ConcurrentHashMap<>();

    StubService(String body) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            lastRequestHeaders.clear();
            exchange.getRequestHeaders().forEach((name, values) ->
                    lastRequestHeaders.put(name.toLowerCase(), String.join(",", values)));
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(bytes);
            }
        });
        server.start();
    }

    String url() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    String lastRequestHeader(String name) {
        return lastRequestHeaders.get(name.toLowerCase());
    }

    @Override
    public void close() {
        server.stop(0);
    }
}
