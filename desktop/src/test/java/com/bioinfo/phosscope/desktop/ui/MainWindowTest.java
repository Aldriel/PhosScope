package com.bioinfo.phosscope.desktop.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bioinfo.phosscope.desktop.AppInfo;
import com.bioinfo.phosscope.desktop.FxThread;
import com.bioinfo.phosscope.desktop.model.DesktopModel;
import com.bioinfo.phosscope.desktop.prefs.UserPreferences;
import com.bioinfo.phosscope.desktop.view.DocumentSaver;
import com.bioinfo.phosscope.desktop.view.FilePicker;
import com.bioinfo.phosscope.desktop.view.PickedFile;
import com.bioinfo.phosscope.service.PhosService;

import javafx.scene.Scene;
import javafx.scene.control.TableView;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.prefs.Preferences;

/** The window, driven like a user on headless JavaFX. */
class MainWindowTest {

    @BeforeAll
    static void javaFx() {
        FxThread.start();
    }

    @Test
    void aVisitorOpensTheExampleChangesLanguageAndSavesTheReport() {
        AtomicReference<PickedFile> picked = new AtomicReference<>();
        List<String> urls = new ArrayList<>();
        List<String> texts = new ArrayList<>();
        List<byte[]> bytes = new ArrayList<>();
        boolean[] quit = {false};
        Preferences node =
                Preferences.userRoot().node("phosscope-ui-" + java.util.UUID.randomUUID());
        DesktopModel model = new DesktopModel(new PhosService(), new UserPreferences(node));
        FilePicker files = () -> picked.get();
        DocumentSaver saver =
                new DocumentSaver() {
                    @Override
                    public void saveText(String suggestedName, String content) {
                        texts.add(content);
                    }

                    @Override
                    public void saveBytes(String suggestedName, byte[] content) {
                        bytes.add(content);
                    }
                };
        FxThread.run(
                () -> {
                    MainController controller =
                            new MainController(
                                    model, files, saver, urls::add, () -> quit[0] = true);
                    Stage stage = new Stage();
                    stage.setScene(new Scene(controller.root(), 1100, 760));
                    stage.show();
                    controller.view.reportButton.fire();
                    controller.view.contaminants.fire();
                    controller.view.fdr.fireEvent(new javafx.event.ActionEvent());
                    controller.view.imputation.fireEvent(new javafx.event.ActionEvent());
                    controller.view.openItem.fire();
                    controller.view.exampleItem.fire();
                    TableView<?> table = (TableView<?>) stage.getScene().lookup("#results");
                    assertTrue(table.getItems().size() > 0);
                    controller.view.englishItem.fire();
                    String author =
                            ((javafx.scene.control.Labeled) stage.getScene().lookup("#author"))
                                    .getText();
                    assertTrue(author.contains("Biocomputing"));
                    controller.view.largerItem.fire();
                    assertTrue(controller.root().getStyle().contains("14px"));
                    controller.view.kofiButton.fire();
                    controller.view.websiteButton.fire();
                    controller.view.guideItem.fire();
                    assertTrue(stage.getScene().lookup("#helpText").isVisible());
                    controller.view.exportItem.fire();
                    controller.view.reportTitle.setText("Window");
                    controller.view.reportButton.fire();
                    for (int row = 0; row < table.getItems().size(); row++) {
                        controller.view.geneColumn.getCellData(row);
                        controller.view.siteColumn.getCellData(row);
                        controller.view.fcColumn.getCellData(row);
                        controller.view.pColumn.getCellData(row);
                        controller.view.qColumn.getCellData(row);
                        controller.view.callColumn.getCellData(row);
                    }
                    controller.view.fdr.setText("0");
                    controller.view.fdr.fireEvent(new javafx.event.ActionEvent());
                    controller.view.fold.setText("nope");
                    controller.view.fold.fireEvent(new javafx.event.ActionEvent());
                    controller.view.contaminants.fire();
                    controller.view.reverse.fire();
                    controller.view.onlySite.fire();
                    controller.view.localization.setText("nope");
                    controller.view.localization.fireEvent(new javafx.event.ActionEvent());
                    controller.view.localization.setText("1");
                    controller.view.localization.fireEvent(new javafx.event.ActionEvent());
                    controller.view.minValid.setText("100");
                    controller.view.minValid.fireEvent(new javafx.event.ActionEvent());
                    controller.view.imputation.setValue(
                            com.bioinfo.phosscope.model.ImputationMethod.NONE);
                    controller.view.downshift.setText("1");
                    controller.view.downshift.fireEvent(new javafx.event.ActionEvent());
                    controller.view.width.setText("x");
                    controller.view.width.fireEvent(new javafx.event.ActionEvent());
                    controller.view.seed.setText("x");
                    controller.view.seed.fireEvent(new javafx.event.ActionEvent());
                    javafx.scene.layout.HBox first =
                            (javafx.scene.layout.HBox) controller.view.samples.getChildren().get(0);
                    @SuppressWarnings("unchecked")
                    javafx.scene.control.ComboBox<com.bioinfo.phosscope.model.Group> groups =
                            (javafx.scene.control.ComboBox<com.bioinfo.phosscope.model.Group>)
                                    first.getChildren().get(1);
                    groups.setValue(com.bioinfo.phosscope.model.Group.UNASSIGNED);
                    groups.getOnAction().handle(new javafx.event.ActionEvent());
                    groups.setValue(null);
                    groups.getOnAction().handle(new javafx.event.ActionEvent());
                    MainController.text(Double.NaN).getValue();
                    try {
                        java.lang.reflect.Method explain =
                                MainController.class.getDeclaredMethod(
                                        "explain",
                                        com.bioinfo.phosscope.desktop.i18n.Messages.class,
                                        String.class);
                        explain.setAccessible(true);
                        explain.invoke(
                                controller,
                                com.bioinfo.phosscope.desktop.i18n.Messages.of(
                                        com.bioinfo.phosscope.model.Language.EN),
                                "NOT_A_CODE");
                    } catch (ReflectiveOperationException e) {
                        throw new IllegalStateException(e);
                    }
                    for (String sample : model.table().sampleNames()) {
                        model.setGroup(sample, com.bioinfo.phosscope.model.Group.CONTROL);
                    }
                    controller.view.smallerItem.fire();
                    model.setGroup(
                            model.table().sampleNames().get(0),
                            com.bioinfo.phosscope.model.Group.DISEASE);
                    controller.view.smallerItem.fire();
                    controller.view.imputation.setValue(null);
                    controller.view.frenchItem.fire();
                    controller.view.websiteButton.fire();
                    controller.view.smallerItem.fire();
                    controller.view.resetFontItem.fire();
                    controller.view.guideItem.fire();
                    press(stage, KeyCode.MINUS, true);
                    press(stage, KeyCode.EQUALS, true);
                    press(stage, KeyCode.PLUS, true);
                    press(stage, KeyCode.DIGIT0, true);
                    press(stage, KeyCode.DIGIT1, true);
                    press(stage, KeyCode.DIGIT3, true);
                    press(stage, KeyCode.DIGIT4, true);
                    press(stage, KeyCode.DIGIT5, true);
                    press(stage, KeyCode.A, true);
                    press(stage, KeyCode.A, false);
                    new UserPreferences();
                    picked.set(null);
                    controller.view.openItem.fire();
                    picked.set(new PickedFile("bad.bin", new byte[] {1, 1, 1, 1}));
                    controller.view.openItem.fire();
                    String real =
                            "Proteins\tAmino acid\tPositions within proteins\tGene names\t"
                                    + "Localization prob\tIntensity Control_1\t"
                                    + "Intensity Control_2\t"
                                    + "Intensity Disease_1\tIntensity Disease_2\n"
                                    + "P\tS\t1\tGENE\t0.9\t8\t8\t4\t4\n";
                    picked.set(
                            new PickedFile(
                                    "real.txt",
                                    real.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
                    controller.view.openItem.fire();
                    controller.view.reportButton.fire();
                    controller.view.fdr.fireEvent(new javafx.event.ActionEvent());
                    controller.view.contaminants.fire();
                    controller.view.imputation.fireEvent(new javafx.event.ActionEvent());
                    controller.view.quitItem.fire();
                    stage.getScene()
                            .getRoot()
                            .fireEvent(
                                    new javafx.scene.input.KeyEvent(
                                            javafx.scene.input.KeyEvent.KEY_PRESSED,
                                            "",
                                            "",
                                            KeyCode.DIGIT2,
                                            false,
                                            true,
                                            false,
                                            false));
                    stage.close();
                });
        assertFalse(urls.isEmpty());
        assertTrue(urls.get(0).contains("ko-fi.com"));
        assertTrue(urls.get(1).contains("maximeethier.com"));
        assertTrue(texts.get(0).contains("AKT1") || texts.get(0).contains("Gene"));
        assertFalse(bytes.isEmpty());
        assertTrue(quit[0]);
        assertEquals(AppInfo.KOFI, urls.get(0));
        assertTrue(urls.contains(AppInfo.WEBSITE_FR));
    }

    private static void press(Stage stage, KeyCode code, boolean shortcut) {
        stage.getScene()
                .getRoot()
                .fireEvent(
                        new javafx.scene.input.KeyEvent(
                                javafx.scene.input.KeyEvent.KEY_PRESSED,
                                "",
                                "",
                                code,
                                false,
                                shortcut,
                                false,
                                false));
    }
}
