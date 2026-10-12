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

import com.bioinfo.phosscope.model.Group;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Guesses whether a sample name is a control or a disease sample.
 *
 * <p>The name is split into words, so {@code normalisation} is not read as {@code normal}. Accents
 * are ignored ({@code témoin} and {@code temoin} are the same word). A disease word wins when a
 * name contains both, which keeps a column such as {@code Disease_vs_control} on the disease side.
 */
public final class GroupGuesser {

    private static final Pattern WORD = Pattern.compile("[a-z0-9]+");

    private static final Set<String> DISEASE =
            Set.of(
                    "disease", "patient", "tumour", "tumor", "malade", "maladie", "cancer", "sick",
                    "case");

    private static final Set<String> CONTROL =
            Set.of(
                    "control",
                    "ctrl",
                    "ctl",
                    "healthy",
                    "temoin",
                    "normal",
                    "wildtype",
                    "wt",
                    "vehicle",
                    "untreated",
                    "sham",
                    "dmso");

    private GroupGuesser() {}

    /**
     * Guesses the group of one sample.
     *
     * @param sampleName column name, or {@code null}
     * @return the guessed group, or {@link Group#UNASSIGNED} when no word is recognized
     */
    public static Group guess(String sampleName) {
        if (sampleName == null || sampleName.isBlank()) {
            return Group.UNASSIGNED;
        }
        String plain =
                Normalizer.normalize(sampleName, Normalizer.Form.NFD)
                        .replaceAll("\\p{M}", "")
                        .toLowerCase(Locale.ROOT);
        boolean disease = false;
        boolean control = false;
        var matcher = WORD.matcher(plain);
        while (matcher.find()) {
            String word = matcher.group().replaceAll("\\d+$", "");
            disease = disease || DISEASE.contains(word);
            control = control || CONTROL.contains(word);
        }
        if (disease) {
            return Group.DISEASE;
        }
        if (control) {
            return Group.CONTROL;
        }
        return Group.UNASSIGNED;
    }
}
