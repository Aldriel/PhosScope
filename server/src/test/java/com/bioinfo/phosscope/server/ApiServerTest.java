package com.bioinfo.phosscope.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bioinfo.phosscope.example.SimulatedCohort;
import com.bioinfo.phosscope.service.PhosService;
import com.sun.net.httpserver.HttpServer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/** The routes a visitor uses, including an expired workspace. */
class ApiServerTest {

    private final MutableClock clock = new MutableClock();
    private HttpServer server;
    private HttpClient client;
    private String root;

    @BeforeEach
    void start() throws Exception {
        server = ApiServer.create("127.0.0.1", 0, new PhosService(), new WorkspaceStore(clock));
        server.start();
        root = "http://127.0.0.1:" + server.getAddress().getPort();
        client = HttpClient.newHttpClient();
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    @Test
    void healthExamplesAndTheSimulatedWorkspace() throws Exception {
        HttpResponse<String> health = send("GET", "/api/health", null);
        assertEquals(200, health.statusCode());
        assertTrue(health.body().contains("PhosScope"));
        assertTrue(health.body().contains("0.1.0"));
        assertEquals(200, send("GET", "/api/examples", null).statusCode());
        HttpResponse<String> created =
                send("POST", "/api/workspaces", "{\"example\":\"simulated-tumor\"}");
        assertEquals(201, created.statusCode());
        assertTrue(created.body().contains("AKT1"));
        assertTrue(created.body().contains("\"simulated\":true"));
        String id = idOf(created.body());
        assertEquals(
                200,
                send("GET", "/api/workspaces/" + id + "/export?language=fr", null).statusCode());
        HttpResponse<byte[]> pdf =
                client.send(
                        request(
                                "POST",
                                "/api/workspaces/"
                                        + id
                                        + "/report?language=en&title=Demo%20report")
                                .build(),
                        HttpResponse.BodyHandlers.ofByteArray());
        assertEquals(200, pdf.statusCode());
        assertEquals("application/pdf", pdf.headers().firstValue("Content-Type").orElseThrow());
        assertTrue(pdf.body().length > 100);
        assertEquals(204, send("DELETE", "/api/workspaces/" + id, null).statusCode());
        assertEquals(404, send("GET", "/api/workspaces/" + id, null).statusCode());
    }

    @Test
    void uploadSettingsAndTheErrorCodes() throws Exception {
        HttpResponse<String> empty = send("POST", "/api/workspaces", "");
        assertEquals(201, empty.statusCode());
        String id = idOf(empty.body());
        assertTrue(send("GET", "/api/workspaces/" + id, null).body().contains("\"analysis\":null"));
        assertEquals(400, send("GET", "/api/workspaces/" + id + "/export", null).statusCode());
        assertEquals(400, send("POST", "/api/workspaces/" + id + "/report", null).statusCode());
        HttpResponse<String> uploaded =
                client.send(
                        HttpRequest.newBuilder(
                                        URI.create(
                                                root
                                                        + "/api/workspaces/"
                                                        + id
                                                        + "/table?name=sites.txt"))
                                .POST(
                                        HttpRequest.BodyPublishers.ofByteArray(
                                                SimulatedCohort.bytes()))
                                .build(),
                        HttpResponse.BodyHandlers.ofString());
        assertEquals(200, uploaded.statusCode());
        assertTrue(uploaded.body().contains("sites.txt"));
        assertEquals(400, send("PUT", "/api/workspaces/" + id + "/settings", "{").statusCode());
        assertEquals(
                400,
                send("PUT", "/api/workspaces/" + id + "/settings", "{\"fdr\":0}").statusCode());
        assertEquals(405, send("GET", "/api/workspaces/" + id + "/table", null).statusCode());
        assertEquals(405, send("POST", "/api/workspaces/" + id, null).statusCode());
        assertEquals(404, send("GET", "/api/nope", null).statusCode());
        assertEquals(404, send("POST", "/api/workspaces/", null).statusCode());
        assertEquals(404, send("GET", "/api/workspaces/" + id + "/nope", null).statusCode());
        assertEquals(
                400, send("POST", "/api/workspaces", "{\"example\":\"missing\"}").statusCode());
        assertEquals(400, send("POST", "/api/workspaces", "{").statusCode());
        assertEquals(201, send("POST", "/api/workspaces", "{}").statusCode());
        assertEquals(204, send("OPTIONS", "/api/health", null).statusCode());
        byte[] big = new byte[1024 * 1024 + 1];
        HttpResponse<String> tooBig =
                client.send(
                        HttpRequest.newBuilder(
                                        URI.create(root + "/api/workspaces/" + id + "/settings"))
                                .PUT(HttpRequest.BodyPublishers.ofByteArray(big))
                                .build(),
                        HttpResponse.BodyHandlers.ofString());
        assertEquals(413, tooBig.statusCode());
        assertEquals(
                400,
                send("POST", "/api/workspaces/" + id + "/table", "not a table".getBytes())
                        .statusCode());
        assertEquals(404, send("POST", "/api/health", null).statusCode());
        assertEquals(404, send("POST", "/api/examples", null).statusCode());
        assertEquals(404, send("GET", "/api/workspaces", null).statusCode());
        assertEquals(404, send("GET", "/api/workspaces//table", null).statusCode());
        assertEquals(
                404, send("GET", "/api/workspaces/" + id + "/table/extra", null).statusCode());
        assertEquals(405, send("POST", "/api/workspaces/" + id + "/export", null).statusCode());
        assertEquals(405, send("GET", "/api/workspaces/" + id + "/report", null).statusCode());
        assertEquals(405, send("GET", "/api/workspaces/" + id + "/settings", null).statusCode());
        assertEquals(200, send("GET", "/api/workspaces/" + id + "/export?", null).statusCode());
        assertEquals(400, send("PUT", "/api/workspaces/" + id + "/settings", "{}").statusCode());
        HttpResponse<String> named =
                send("POST", "/api/workspaces/" + id + "/table?language&language=fr", new byte[0]);
        assertEquals(400, named.statusCode());
        String bare = idOf(send("POST", "/api/workspaces", "").body());
        assertEquals(
                400, send("PUT", "/api/workspaces/" + bare + "/settings", settings()).statusCode());
        assertEquals(
                200, send("PUT", "/api/workspaces/" + id + "/settings", settings()).statusCode());
        assertEquals(201, send("POST", "/api/workspaces", "{\"example\":\" \"}").statusCode());
    }

    private static String settings() {
        return "{\"groups\":{\"Control_1\":\"CONTROL\",\"Control_2\":\"CONTROL\","
                + "\"Control_3\":\"CONTROL\",\"Disease_1\":\"DISEASE\",\"Disease_2\":\"DISEASE\","
                + "\"Disease_3\":\"DISEASE\"},\"dropContaminants\":true,\"dropReverse\":true,"
                + "\"dropOnlyBySite\":true,\"minLocalization\":0.75,\"minValidPerGroup\":2,"
                + "\"imputation\":\"PERSEUS\",\"downshift\":1.8,\"width\":0.3,\"seed\":1,"
                + "\"fdr\":0.05,\"minAbsLog2FoldChange\":1}";
    }

    @Test
    void anIdleWorkspaceExpires() {
        WorkspaceStore store = new WorkspaceStore(clock);
        Workspace workspace = store.create();
        clock.plus(Duration.ofMinutes(31));
        store.create();
        ApiException purged =
                assertThrows(ApiException.class, () -> store.require(workspace.id()));
        assertEquals(404, purged.status());
        Workspace fresh = store.create();
        clock.plus(Duration.ofMinutes(31));
        ApiException expired =
                assertThrows(ApiException.class, () -> store.require(fresh.id()));
        assertEquals(410, expired.status());
        assertEquals("EXPIRED", expired.kind());
        ApiException missing =
                assertThrows(ApiException.class, () -> store.require(fresh.id()));
        assertEquals(404, missing.status());
        store.delete("absent");
        assertEquals("NOT_FOUND", ApiException.notFound("x").code());
        assertEquals("TOO_LARGE", ApiException.tooLarge("x").kind());
    }

    private HttpResponse<String> send(String method, String path, Object body) throws Exception {
        HttpRequest.Builder builder = request(method, path);
        if ("POST".equals(method) || "PUT".equals(method)) {
            if (body instanceof byte[] bytes) {
                builder.method(method, HttpRequest.BodyPublishers.ofByteArray(bytes));
            } else {
                builder.method(
                        method,
                        HttpRequest.BodyPublishers.ofString(body == null ? "" : body.toString()));
            }
        } else {
            builder.method(method, HttpRequest.BodyPublishers.noBody());
        }
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpRequest.Builder request(String method, String path) {
        return HttpRequest.newBuilder(URI.create(root + path))
                .method(method, HttpRequest.BodyPublishers.noBody());
    }

    private static String idOf(String json) {
        int start = json.indexOf("\"id\":\"") + 6;
        return json.substring(start, json.indexOf('"', start));
    }

    /** Clock whose time the test moves forward. */
    private static final class MutableClock extends Clock {
        private Instant instant = Instant.parse("2026-01-01T00:00:00Z");

        void plus(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
