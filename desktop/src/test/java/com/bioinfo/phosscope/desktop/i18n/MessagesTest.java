package com.bioinfo.phosscope.desktop.i18n;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bioinfo.phosscope.model.Language;

import org.junit.jupiter.api.Test;

/** French and English expose the same keys, and the guide is loaded. */
class MessagesTest {

    @Test
    void bothLanguagesHaveTheSameKeys() {
        Messages english = Messages.of(Language.EN);
        Messages french = Messages.of(Language.FR);
        assertEquals(english.keys(), french.keys());
        assertTrue(english.keys().contains("footer.kofi"));
        assertEquals("!missing!", english.get("missing"));
        assertTrue(english.guide().contains("PhosScope"));
        assertTrue(french.guide().contains("PhosScope"));
        assertEquals(Language.EN, Messages.of(null).language());
    }
}
