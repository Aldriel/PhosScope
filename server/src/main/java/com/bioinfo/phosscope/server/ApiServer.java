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

import com.bioinfo.phosscope.service.PhosService;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.time.Clock;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

/**
 * REST API consumed by the React application. The JDK HTTP server and virtual threads are enough:
 * each visitor has a workspace kept in memory, and the shared {@link PhosService} does the
 * analysis.
 */
public final class ApiServer {

    /** Port used when neither an argument nor the {@code PORT} variable gives one. */
    public static final int DEFAULT_PORT = 7073;

    /** Largest uploaded table, in bytes. */
    public static final int MAX_UPLOAD_BYTES = 40 * 1024 * 1024;

    /** Version written by the Maven build. */
    public static final String VERSION = readVersion();

    private static final Logger LOG = Logger.getLogger(ApiServer.class.getName());

    private ApiServer() {}

    /**
     * Creates a server that is not started yet.
     *
     * @param host address to bind
     * @param port port, or 0 for a free port
     * @param service model
     * @param store workspaces
     * @return the server
     * @throws IOException when the address cannot be bound
     */
    public static HttpServer create(
            String host, int port, PhosService service, WorkspaceStore store) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(host, port), 0);
        server.createContext("/api", new ApiHandler(service, store, VERSION));
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        return server;
    }

    /**
     * Starts the API on {@code 127.0.0.1}.
     *
     * @param args optional port; otherwise the {@code PORT} variable, otherwise 7073
     * @throws IOException when the port is already in use
     */
    public static void main(String[] args) throws IOException {
        String port = args.length > 0 ? args[0] : System.getenv("PORT");
        int portNumber = port == null || port.isBlank() ? DEFAULT_PORT : Integer.parseInt(port);
        HttpServer server =
                create(
                        "127.0.0.1",
                        portNumber,
                        new PhosService(),
                        new WorkspaceStore(Clock.systemUTC()));
        server.start();
        LOG.info(
                () ->
                        "PhosScope API "
                                + VERSION
                                + " on http://127.0.0.1:"
                                + server.getAddress().getPort()
                                + "/api");
    }

    private static String readVersion() {
        Properties properties = new Properties();
        try (InputStream in = ApiServer.class.getResourceAsStream("server.properties")) {
            if (in == null) {
                return "dev";
            }
            properties.load(in);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return properties.getProperty("version", "dev");
    }
}
