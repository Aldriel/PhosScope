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

package com.bioinfo.phosscope.parse;

/**
 * The uploaded file is not a quantitative table PhosScope can read.
 *
 * <p>{@link #code()} is a stable token ({@code EMPTY}, {@code NO_COLUMNS}, {@code NOT_TEXT}, {@code
 * TOO_LARGE}) translated by the interfaces. {@link #getMessage()} is an English explanation for
 * logs and for a client that does not know the token.
 */
public final class ParseException extends Exception {

    /** The file has no bytes. */
    public static final String EMPTY = "EMPTY";

    /** No intensity column was found. */
    public static final String NO_COLUMNS = "NO_COLUMNS";

    /** The bytes are not a text table. */
    public static final String NOT_TEXT = "NOT_TEXT";

    /** The table, once decompressed, is larger than the allowed limit. */
    public static final String TOO_LARGE = "TOO_LARGE";

    private final String code;

    /**
     * Creates the exception.
     *
     * @param code stable token, one of the constants of this class
     * @param message English explanation
     */
    public ParseException(String code, String message) {
        super(message);
        this.code = code;
    }

    /**
     * Stable token the interfaces translate.
     *
     * @return the code passed to the constructor
     */
    public String code() {
        return code;
    }
}
