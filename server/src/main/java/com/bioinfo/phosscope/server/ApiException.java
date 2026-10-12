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

/** Error translated into an HTTP status. {@link #kind()} is the token the web client translates. */
public final class ApiException extends RuntimeException {

    private final int status;
    private final String kind;
    private final String code;

    private ApiException(int status, String kind, String code, String message) {
        super(message);
        this.status = status;
        this.kind = kind;
        this.code = code;
    }

    /** 400, the file could not be read. */
    public static ApiException parsing(String code, String message) {
        return new ApiException(400, "PARSING", code, message);
    }

    /** 400, a setting or a parameter is invalid. */
    public static ApiException invalid(String code, String message) {
        return new ApiException(400, "INVALID_INPUT", code, message);
    }

    /** 404, the path or the workspace does not exist. */
    public static ApiException notFound(String message) {
        return new ApiException(404, "NOT_FOUND", "NOT_FOUND", message);
    }

    /** 405, the method is not served on this path. */
    public static ApiException method(String message) {
        return new ApiException(405, "METHOD", "METHOD", message);
    }

    /** 410, the workspace was forgotten after 30 minutes. */
    public static ApiException expired(String message) {
        return new ApiException(410, "EXPIRED", "EXPIRED", message);
    }

    /** 413, the upload is too large. */
    public static ApiException tooLarge(String message) {
        return new ApiException(413, "TOO_LARGE", "TOO_LARGE", message);
    }

    /** HTTP status. */
    public int status() {
        return status;
    }

    /** Category translated by the client. */
    public String kind() {
        return kind;
    }

    /** Finer code, for example {@code FDR_RANGE} or {@code NO_COLUMNS}. */
    public String code() {
        return code;
    }
}
