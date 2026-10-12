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

package com.bioinfo.phosscope.model;

import java.util.Locale;

/** Language of the interface and of the PDF report. */
public enum Language {
    /** French. */
    FR,
    /** English. */
    EN;

    /**
     * Chooses the language of a locale. Anything other than French is English, which is also the
     * fallback when the locale is missing.
     *
     * @param locale the locale, or {@code null}
     * @return the supported language
     */
    public static Language fromLocale(Locale locale) {
        if (locale != null && Locale.FRENCH.getLanguage().equals(locale.getLanguage())) {
            return FR;
        }
        return EN;
    }

    /**
     * Reads a language code sent by a client ({@code fr}, {@code en}, or a locale tag).
     *
     * @param code the code, or {@code null}
     * @return the supported language; unknown codes become English
     */
    public static Language parse(String code) {
        if (code == null || code.isBlank()) {
            return EN;
        }
        String language = code.trim().toLowerCase(Locale.ROOT);
        if (language.startsWith("fr")) {
            return FR;
        }
        return EN;
    }
}
