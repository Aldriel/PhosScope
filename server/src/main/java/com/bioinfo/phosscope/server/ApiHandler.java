/*
 * Copyright 2026 Maxime Ethier - Consultant en Bioinformatique/Biocomputing Consultant
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bioinfo.phosscope.server;

import com.bioinfo.phosscope.example.SimulatedCohort;
import com.bioinfo.phosscope.model.AnalysisSettings;
import com.bioinfo.phosscope.model.Language;
import com.bioinfo.phosscope.model.QuantTable;
import com.bioinfo.phosscope.parse.ParseException;
import com.bioinfo.phosscope.service.Analysis;
import com.bioinfo.phosscope.service.PhosService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Routes of {@code /api}. A workspace lives only in memory and is dropped after 30 idle minutes.
 */
public final class ApiHandler implements HttpHandler {

    private static final Logger LOG = Logger.getLogger(ApiHandler.class.getName());
    private static final int MAX_JSON = 1024 * 1024;

    private final PhosService service;
    private final WorkspaceStore store;
    private final String version;
    private final ObjectMapper json = new ObjectMapper();

    /**
     * Creates the handler.
     *
     * @param service model
     * @param store workspaces
     * @param version application version reported by {@code /api/health}
     */
    public ApiHandler(PhosService service, WorkspaceStore store, String version) {
        this.service = service;
        this.store = store;
        this.version = version;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                send(exchange, 204, new byte[0], "text/plain");
                return;
            }
            dispatch(exchange);
        } catch (ApiException e) {
            sendJson(exchange, e.status(), error(e.kind(), e.code(), e.getMessage()));
        } catch (IllegalArgumentException e) {
            sendJson(exchange, 400, error("INVALID_INPUT", e.getMessage(), e.getMessage()));
        } catch (RuntimeException e) {
            LOG.log(Level.SEVERE, "Unexpected API error", e);
            sendJson(exchange, 500, error("UNEXPECTED", "UNEXPECTED", "Unexpected error."));
        }
    }

    private void dispatch(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        Map<String, String> query = query(exchange.getRequestURI().getRawQuery());
        if ("/api/health".equals(path) && "GET".equals(method)) {
            sendJson(
                    exchange, 200, Map.of("status", "ok", "name", "PhosScope", "version", version));
            return;
        }
        if ("/api/examples".equals(path) && "GET".equals(method)) {
            sendJson(exchange, 200, Map.of("examples", List.of(example())));
            return;
        }
        if ("/api/workspaces".equals(path) && "POST".equals(method)) {
            sendJson(exchange, 201, view(openWorkspace(exchange)));
            return;
        }
        if (!path.startsWith("/api/workspaces/")) {
            throw ApiException.notFound("Unknown route.");
        }
        String[] parts = path.substring("/api/workspaces/".length()).split("/");
        String id = parts[0];
        if (id.isBlank()) {
            throw ApiException.notFound("Unknown route.");
        }
        if (parts.length == 1) {
            workspace(exchange, method, id);
            return;
        }
        if (parts.length == 2) {
            action(exchange, method, id, parts[1], query);
            return;
        }
        throw ApiException.notFound("Unknown route.");
    }

    private void workspace(HttpExchange exchange, String method, String id) throws IOException {
        if ("GET".equals(method)) {
            sendJson(exchange, 200, view(store.require(id)));
        } else if ("DELETE".equals(method)) {
            store.delete(id);
            send(exchange, 204, new byte[0], "text/plain");
        } else {
            throw ApiException.method("Use GET or DELETE on a workspace.");
        }
    }

    private void action(
            HttpExchange exchange,
            String method,
            String id,
            String action,
            Map<String, String> query)
            throws IOException {
        switch (action) {
            case "table" -> upload(exchange, method, id, query);
            case "settings" -> updateSettings(exchange, method, id);
            case "export" -> export(exchange, method, id, query);
            case "report" -> report(exchange, method, id, query);
            default -> throw ApiException.notFound("Unknown route.");
        }
    }

    private void upload(HttpExchange exchange, String method, String id, Map<String, String> query)
            throws IOException {
        if (!"POST".equals(method)) {
            throw ApiException.method("Use POST to upload a table.");
        }
        Workspace workspace = store.require(id);
        byte[] body = read(exchange, ApiServer.MAX_UPLOAD_BYTES);
        String name = query.getOrDefault("name", "upload.txt");
        try {
            QuantTable table = service.read(body, name);
            workspace.table(
                    table, AnalysisSettings.defaultsFor(table.sampleNames()), false, store.now());
        } catch (ParseException e) {
            throw ApiException.parsing(e.code(), e.getMessage());
        }
        sendJson(exchange, 200, view(workspace));
    }

    private void updateSettings(HttpExchange exchange, String method, String id)
            throws IOException {
        if (!"PUT".equals(method)) {
            throw ApiException.method("Use PUT to replace the settings.");
        }
        Workspace workspace = store.require(id);
        if (workspace.table() == null) {
            throw ApiException.invalid("NO_TABLE", "Open a table before changing the settings.");
        }
        AnalysisSettings settings = readSettings(read(exchange, MAX_JSON));
        workspace.settings(settings, store.now());
        sendJson(exchange, 200, view(workspace));
    }

    private void export(HttpExchange exchange, String method, String id, Map<String, String> query)
            throws IOException {
        if (!"GET".equals(method)) {
            throw ApiException.method("Use GET to export the results.");
        }
        Workspace workspace = store.require(id);
        Analysis analysis = analysis(workspace);
        if (analysis == null) {
            throw ApiException.invalid("NO_TABLE", "Open a table before exporting.");
        }
        byte[] body =
                service.exportTable(analysis, Language.parse(query.get("language")))
                        .getBytes(StandardCharsets.UTF_8);
        send(exchange, 200, body, "text/tab-separated-values; charset=utf-8");
    }

    private void report(HttpExchange exchange, String method, String id, Map<String, String> query)
            throws IOException {
        if (!"POST".equals(method)) {
            throw ApiException.method("Use POST to build the report.");
        }
        Workspace workspace = store.require(id);
        Analysis analysis = analysis(workspace);
        if (analysis == null) {
            throw ApiException.invalid("NO_TABLE", "Open a table before the report.");
        }
        byte[] pdf =
                service.report(
                        analysis,
                        Language.parse(query.get("language")),
                        query.get("title"),
                        workspace.simulated());
        exchange.getResponseHeaders()
                .set("Content-Disposition", "attachment; filename=\"PhosScope.pdf\"");
        send(exchange, 200, pdf, "application/pdf");
    }

    private Workspace openWorkspace(HttpExchange exchange) throws IOException {
        byte[] body = read(exchange, MAX_JSON);
        Workspace workspace = store.create();
        if (body.length == 0) {
            return workspace;
        }
        JsonNode node = readTree(body);
        JsonNode example = node.get("example");
        if (example == null || example.asText().isBlank()) {
            return workspace;
        }
        if (!SimulatedCohort.ID.equals(example.asText())) {
            throw ApiException.invalid("UNKNOWN_EXAMPLE", "Unknown example.");
        }
        try {
            QuantTable table = service.read(SimulatedCohort.bytes(), SimulatedCohort.FILE_NAME);
            workspace.table(
                    table, AnalysisSettings.defaultsFor(table.sampleNames()), true, store.now());
        } catch (ParseException e) {
            throw ApiException.parsing(e.code(), e.getMessage());
        }
        return workspace;
    }

    private AnalysisSettings readSettings(byte[] body) {
        try {
            return json.readValue(body, AnalysisSettings.class);
        } catch (IOException e) {
            if (e.getCause() instanceof IllegalArgumentException invalid) {
                throw invalid;
            }
            throw ApiException.invalid("JSON", "The settings are not valid JSON.");
        }
    }

    private JsonNode readTree(byte[] body) {
        try {
            return json.readTree(body);
        } catch (IOException e) {
            throw ApiException.invalid("JSON", "The request is not valid JSON.");
        }
    }

    private Analysis analysis(Workspace workspace) {
        if (workspace.table() == null) {
            return null;
        }
        return service.analyze(workspace.table(), workspace.settings());
    }

    private static Map<String, Object> example() {
        Map<String, Object> example = new LinkedHashMap<>();
        example.put("id", SimulatedCohort.ID);
        example.put("fileName", SimulatedCohort.FILE_NAME);
        example.put("simulated", true);
        return example;
    }

    private WorkspaceView view(Workspace workspace) {
        return new WorkspaceView(
                workspace.id(),
                workspace.table() == null ? null : workspace.table().fileName(),
                workspace.table() == null ? null : workspace.table().format().name(),
                workspace.simulated(),
                analysis(workspace));
    }

    private Map<String, String> error(String kind, String code, String message) {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("error", message);
        body.put("kind", kind);
        body.put("code", code);
        return body;
    }

    private byte[] read(HttpExchange exchange, int max) throws IOException {
        try (InputStream in = exchange.getRequestBody()) {
            byte[] body = in.readNBytes(max + 1);
            if (body.length > max) {
                throw ApiException.tooLarge("The request is too large.");
            }
            return body;
        }
    }

    private void sendJson(HttpExchange exchange, int status, Object body) throws IOException {
        send(exchange, status, json.writeValueAsBytes(body), "application/json; charset=utf-8");
    }

    private static void send(HttpExchange exchange, int status, byte[] body, String type)
            throws IOException {
        exchange.getResponseHeaders().set("Content-Type", type);
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders()
                .set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
        int length = status == 204 ? -1 : body.length;
        exchange.sendResponseHeaders(status, length);
        if (status != 204) {
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        } else {
            exchange.close();
        }
    }

    private static Map<String, String> query(String raw) {
        Map<String, String> query = new LinkedHashMap<>();
        if (raw == null || raw.isBlank()) {
            return query;
        }
        for (String pair : raw.split("&")) {
            int eq = pair.indexOf('=');
            String key = decode(eq < 0 ? pair : pair.substring(0, eq));
            String value = decode(eq < 0 ? "" : pair.substring(eq + 1));
            query.putIfAbsent(key, value);
        }
        return query;
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }
}
