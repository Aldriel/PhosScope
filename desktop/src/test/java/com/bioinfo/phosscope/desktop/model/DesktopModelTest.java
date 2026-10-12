package com.bioinfo.phosscope.desktop.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bioinfo.phosscope.desktop.AppInfo;
import com.bioinfo.phosscope.desktop.prefs.UserPreferences;
import com.bioinfo.phosscope.example.SimulatedCohort;
import com.bioinfo.phosscope.model.Group;
import com.bioinfo.phosscope.model.ImputationMethod;
import com.bioinfo.phosscope.model.Language;
import com.bioinfo.phosscope.service.PhosService;

import org.junit.jupiter.api.Test;

import java.util.prefs.Preferences;

/** The desktop model without a window: opening, filters, fonts and the report. */
class DesktopModelTest {

    @Test
    void exampleCanBeFilteredExportedAndReported() {
        DesktopModel model = model();
        assertNull(model.report("title"));
        assertTrue(model.export().startsWith("\uFEFF"));
        assertEquals("NO_TABLE", model.setCleaning(true, true, true, 0.75, 2));
        assertEquals("NO_TABLE", model.setImputation(ImputationMethod.NONE, 1.8, 0.3, 1));
        assertEquals("NO_TABLE", model.setThresholds(0.05, 1));
        model.setGroup("Control_1", Group.DISEASE);
        model.openExample();
        assertTrue(model.simulated());
        assertEquals(SimulatedCohort.FILE_NAME, model.table().fileName());
        assertTrue(model.analysis().significantCount() > 0);
        model.setGroup("Control_1", Group.DISEASE);
        assertEquals(Group.DISEASE, model.settings().groups().get("Control_1"));
        assertNull(model.setThresholds(0.1, 1));
        assertEquals("FDR_RANGE", model.setThresholds(0, 1));
        assertEquals(0.1, model.settings().fdr(), 0);
        assertNull(model.setImputation(ImputationMethod.HALF_MINIMUM, 1, 0.5, 4));
        assertEquals("WIDTH_RANGE", model.setImputation(ImputationMethod.PERSEUS, 1.8, 0, 1));
        assertNull(model.setCleaning(false, false, false, 0, 2));
        assertEquals("MIN_VALID_RANGE", model.setCleaning(true, true, true, 0.5, 1));
        byte[] pdf = model.report("Desk");
        assertNotNull(pdf);
        assertTrue(pdf.length > 100);
        model.open(new byte[] {1, 1, 1, 1}, "bad.bin", false);
        assertEquals("NOT_TEXT", model.errorCode());
        assertNull(model.table());
        assertTrue(AppInfo.version().contains("0.1.0"));
        assertEquals(AppInfo.NAME, "PhosScope");
    }

    @Test
    void fontAndLanguageAreRemembered() {
        Preferences node =
                Preferences.userRoot().node("phosscope-test-" + java.util.UUID.randomUUID());
        UserPreferences preferences = new UserPreferences(node);
        preferences.language(null);
        assertEquals(Language.EN, preferences.language());
        preferences.language(Language.FR);
        preferences.fontSize(100);
        DesktopModel model = new DesktopModel(new PhosService(), preferences);
        assertEquals(Language.FR, model.language());
        assertEquals(UserPreferences.MAX_FONT, model.fontSize());
        model.changeFont(5);
        assertEquals(UserPreferences.MAX_FONT, model.fontSize());
        model.changeFont(-100);
        assertEquals(UserPreferences.MIN_FONT, model.fontSize());
        model.resetFont();
        assertEquals(UserPreferences.DEFAULT_FONT, model.fontSize());
        model.language(Language.EN);
        assertEquals(Language.EN, new UserPreferences(node).language());
        assertEquals(UserPreferences.MIN_FONT, UserPreferences.clamp(9));
        assertEquals(UserPreferences.MAX_FONT, UserPreferences.clamp(30));
    }

    private static DesktopModel model() {
        Preferences node =
                Preferences.userRoot().node("phosscope-test-" + java.util.UUID.randomUUID());
        return new DesktopModel(new PhosService(), new UserPreferences(node));
    }
}
