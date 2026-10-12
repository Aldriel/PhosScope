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

package com.bioinfo.phosscope.desktop.ui;

import com.bioinfo.phosscope.desktop.AppInfo;
import com.bioinfo.phosscope.desktop.i18n.Messages;
import com.bioinfo.phosscope.desktop.model.DesktopModel;
import com.bioinfo.phosscope.desktop.view.DocumentSaver;
import com.bioinfo.phosscope.desktop.view.FilePicker;
import com.bioinfo.phosscope.desktop.view.PickedFile;
import com.bioinfo.phosscope.desktop.view.VolcanoGeometry;
import com.bioinfo.phosscope.model.AnalysisStatus;
import com.bioinfo.phosscope.model.Group;
import com.bioinfo.phosscope.model.Language;
import com.bioinfo.phosscope.model.SiteStatistic;
import com.bioinfo.phosscope.service.Analysis;

import javafx.scene.Parent;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.shape.Circle;

import java.util.Locale;
import java.util.function.Consumer;

/**
 * Connects {@link MainView} to {@link DesktopModel}. The view is filled again after every action.
 */
public final class MainController {

    private final DesktopModel model;
    private final FilePicker files;
    private final DocumentSaver saver;
    private final Consumer<String> openUrl;
    private final Runnable quit;
    final MainView view = new MainView();
    private boolean filling;

    /**
     * Builds the controller.
     *
     * @param model application state
     * @param files how a table is chosen
     * @param saver how a report is saved
     * @param openUrl opens a web address
     * @param quit closes the application
     */
    public MainController(
            DesktopModel model,
            FilePicker files,
            DocumentSaver saver,
            Consumer<String> openUrl,
            Runnable quit) {
        this.model = model;
        this.files = files;
        this.saver = saver;
        this.openUrl = openUrl;
        this.quit = quit;
        wire();
        refresh();
    }

    /** Root node, ready to put in a scene. */
    public Parent root() {
        return view.root();
    }

    private void wire() {
        view.openItem.setOnAction(event -> openFile());
        view.exampleItem.setOnAction(
                event -> {
                    model.openExample();
                    refresh();
                });
        view.exportItem.setOnAction(event -> saver.saveText("PhosScope.tsv", model.export()));
        view.quitItem.setOnAction(event -> quit.run());
        view.frenchItem.setOnAction(
                event -> {
                    if (!filling) {
                        language(Language.FR);
                    }
                });
        view.englishItem.setOnAction(
                event -> {
                    if (!filling) {
                        language(Language.EN);
                    }
                });
        view.smallerItem.setOnAction(event -> font(-1));
        view.largerItem.setOnAction(event -> font(1));
        view.resetFontItem.setOnAction(
                event -> {
                    model.resetFont();
                    refresh();
                });
        view.guideItem.setOnAction(event -> toggleHelp());
        view.kofiButton.setOnAction(event -> openUrl.accept(AppInfo.KOFI));
        view.websiteButton.setOnAction(event -> openUrl.accept(website()));
        view.contaminants.setOnAction(event -> applyCleaning());
        view.reverse.setOnAction(event -> applyCleaning());
        view.onlySite.setOnAction(event -> applyCleaning());
        view.localization.setOnAction(event -> applyCleaning());
        view.minValid.setOnAction(event -> applyCleaning());
        view.imputation.setOnAction(event -> applyImputation());
        view.downshift.setOnAction(event -> applyImputation());
        view.width.setOnAction(event -> applyImputation());
        view.seed.setOnAction(event -> applyImputation());
        view.fdr.setOnAction(event -> applyThresholds());
        view.fold.setOnAction(event -> applyThresholds());
        view.reportButton.setOnAction(event -> buildReport());
        view.root().addEventFilter(KeyEvent.KEY_PRESSED, this::shortcut);
        view.geneColumn.setCellValueFactory(
                cell ->
                        javafx.beans.binding.Bindings.createStringBinding(
                                () -> cell.getValue().gene()));
        view.siteColumn.setCellValueFactory(
                cell ->
                        javafx.beans.binding.Bindings.createStringBinding(
                                () -> cell.getValue().siteLabel()));
        view.fcColumn.setCellValueFactory(cell -> text(cell.getValue().log2FoldChange()));
        view.pColumn.setCellValueFactory(cell -> text(cell.getValue().pValue()));
        view.qColumn.setCellValueFactory(cell -> text(cell.getValue().qValue()));
        view.callColumn.setCellValueFactory(
                cell ->
                        javafx.beans.binding.Bindings.createStringBinding(
                                () -> call(cell.getValue())));
    }

    private void shortcut(KeyEvent event) {
        if (!event.isShortcutDown()) {
            return;
        }
        if (event.getCode() == KeyCode.MINUS) {
            font(-1);
        } else if (event.getCode() == KeyCode.EQUALS || event.getCode() == KeyCode.PLUS) {
            font(1);
        } else if (event.getCode() == KeyCode.DIGIT0) {
            model.resetFont();
            refresh();
        } else if (event.getCode() == KeyCode.DIGIT1) {
            view.dataTab.getTabPane().getSelectionModel().select(view.dataTab);
        } else if (event.getCode() == KeyCode.DIGIT2) {
            view.cleaningTab.getTabPane().getSelectionModel().select(view.cleaningTab);
        } else if (event.getCode() == KeyCode.DIGIT3) {
            view.imputationTab.getTabPane().getSelectionModel().select(view.imputationTab);
        } else if (event.getCode() == KeyCode.DIGIT4) {
            view.resultsTab.getTabPane().getSelectionModel().select(view.resultsTab);
        } else if (event.getCode() == KeyCode.DIGIT5) {
            view.reportTab.getTabPane().getSelectionModel().select(view.reportTab);
        } else {
            return;
        }
        event.consume();
    }

    private void openFile() {
        PickedFile file = files.open();
        if (file != null) {
            model.open(file.content(), file.name(), false);
            refresh();
        }
    }

    private void language(Language language) {
        model.language(language);
        refresh();
    }

    private void font(int delta) {
        model.changeFont(delta);
        refresh();
    }

    private void toggleHelp() {
        boolean show = !view.help.isVisible();
        view.help.setVisible(show);
        view.help.setManaged(show);
        view.help.setText(messages().guide());
    }

    private void applyCleaning() {
        if (filling || model.settings() == null) {
            return;
        }
        String error =
                model.setCleaning(
                        view.contaminants.isSelected(),
                        view.reverse.isSelected(),
                        view.onlySite.isSelected(),
                        number(view.localization.getText(), model.settings().minLocalization()),
                        integer(view.minValid.getText(), model.settings().minValidPerGroup()));
        showError(error);
        refresh();
    }

    private void applyImputation() {
        if (filling || model.settings() == null || view.imputation.getValue() == null) {
            return;
        }
        String error =
                model.setImputation(
                        view.imputation.getValue(),
                        number(view.downshift.getText(), model.settings().downshift()),
                        number(view.width.getText(), model.settings().width()),
                        (long) integer(view.seed.getText(), (int) model.settings().seed()));
        showError(error);
        refresh();
    }

    private void applyThresholds() {
        if (filling || model.settings() == null) {
            return;
        }
        String error =
                model.setThresholds(
                        number(view.fdr.getText(), model.settings().fdr()),
                        number(view.fold.getText(), model.settings().minAbsLog2FoldChange()));
        showError(error);
        refresh();
    }

    private void buildReport() {
        byte[] pdf = model.report(view.reportTitle.getText());
        if (pdf == null) {
            return;
        }
        saver.saveBytes("PhosScope.pdf", pdf);
        view.status.setText(messages().get("report.ready"));
    }

    private void refresh() {
        filling = true;
        Messages text = messages();
        applyTexts(text);
        view.root().setStyle("-fx-font-size: " + model.fontSize() + "px;");
        view.frenchItem.setSelected(model.language() == Language.FR);
        view.englishItem.setSelected(model.language() == Language.EN);
        fillData(text);
        fillSettings();
        fillResults(text);
        if (model.errorCode() != null) {
            view.status.setText(explain(text, model.errorCode()));
        }
        filling = false;
    }

    private void applyTexts(Messages text) {
        view.fileMenu.setText(text.get("menu.file"));
        view.openItem.setText(text.get("menu.open"));
        view.exampleItem.setText(text.get("menu.example"));
        view.exportItem.setText(text.get("menu.export"));
        view.quitItem.setText(text.get("menu.quit"));
        view.optionsMenu.setText(text.get("menu.options"));
        view.languageMenu.setText(text.get("menu.language"));
        view.frenchItem.setText(text.get("menu.french"));
        view.englishItem.setText(text.get("menu.english"));
        view.fontMenu.setText(text.get("menu.font"));
        view.smallerItem.setText(text.get("menu.smaller"));
        view.largerItem.setText(text.get("menu.larger"));
        view.resetFontItem.setText(text.get("menu.reset"));
        view.helpMenu.setText(text.get("menu.help"));
        view.guideItem.setText(text.get("menu.guide"));
        view.dataTab.setText(text.get("tab.data"));
        view.cleaningTab.setText(text.get("tab.cleaning"));
        view.imputationTab.setText(text.get("tab.imputation"));
        view.resultsTab.setText(text.get("tab.results"));
        view.reportTab.setText(text.get("tab.report"));
        view.intro.setText(text.get("data.intro"));
        view.contaminants.setText(text.get("cleaning.contaminants"));
        view.reverse.setText(text.get("cleaning.reverse"));
        view.onlySite.setText(text.get("cleaning.onlySite"));
        view.imputationNote.setText(text.get("imputation.note"));
        view.reportButton.setText(text.get("report.build"));
        view.kofiButton.setText(text.get("footer.kofi"));
        view.author.setText(text.get("footer.author"));
        view.availability.setText(text.get("footer.availability"));
        view.websiteButton.setText(text.get("footer.license"));
        view.geneColumn.setText(text.get("results.gene"));
        view.siteColumn.setText(text.get("results.site"));
        view.fcColumn.setText(text.get("results.fc"));
        view.pColumn.setText(text.get("results.p"));
        view.qColumn.setText(text.get("results.q"));
        view.callColumn.setText(text.get("results.call"));
        view.localization.setPromptText(text.get("cleaning.localization"));
        view.minValid.setPromptText(text.get("cleaning.minValid"));
        view.downshift.setPromptText(text.get("imputation.downshift"));
        view.width.setPromptText(text.get("imputation.width"));
        view.seed.setPromptText(text.get("imputation.seed"));
        view.fdr.setPromptText(text.get("results.fdr"));
        view.fold.setPromptText(text.get("results.fold"));
        view.reportTitle.setPromptText(text.get("report.title"));
    }

    private void fillData(Messages text) {
        view.samples.getChildren().clear();
        if (model.table() == null) {
            view.fileLabel.setText(text.get("data.empty"));
            view.formatLabel.setText("");
            view.simulatedLabel.setText("");
            return;
        }
        view.fileLabel.setText(text.get("data.file") + ": " + model.table().fileName());
        view.formatLabel.setText(text.get("data.format") + ": " + model.table().format());
        view.simulatedLabel.setText(model.simulated() ? text.get("data.simulated") : "");
        for (String sample : model.table().sampleNames()) {
            ComboBox<Group> groups = new ComboBox<>();
            groups.getItems().setAll(Group.CONTROL, Group.DISEASE, Group.UNASSIGNED);
            groups.setOnAction(
                    event -> {
                        if (!filling && groups.getValue() != null) {
                            model.setGroup(sample, groups.getValue());
                            refresh();
                        }
                    });
            groups.setValue(model.settings().groups().getOrDefault(sample, Group.UNASSIGNED));
            view.samples.getChildren().add(new HBox(8, new Label(sample), groups));
        }
    }

    private void fillSettings() {
        if (model.settings() == null) {
            return;
        }
        var settings = model.settings();
        view.contaminants.setSelected(settings.dropContaminants());
        view.reverse.setSelected(settings.dropReverse());
        view.onlySite.setSelected(settings.dropOnlyBySite());
        view.localization.setText(Double.toString(settings.minLocalization()));
        view.minValid.setText(Integer.toString(settings.minValidPerGroup()));
        view.imputation.setValue(settings.imputation());
        view.downshift.setText(Double.toString(settings.downshift()));
        view.width.setText(Double.toString(settings.width()));
        view.seed.setText(Long.toString(settings.seed()));
        view.fdr.setText(Double.toString(settings.fdr()));
        view.fold.setText(Double.toString(settings.minAbsLog2FoldChange()));
    }

    private void fillResults(Messages text) {
        Analysis analysis = model.analysis();
        view.table.getItems().clear();
        view.volcano.getChildren().clear();
        if (analysis == null) {
            view.resultsSummary.setText(text.get("results.empty"));
            view.cleaningSummary.setText("");
            view.status.setText(text.get("status.ready"));
            return;
        }
        view.cleaningSummary.setText(
                String.format(
                        text.get("cleaning.summary"),
                        analysis.sitesRead(),
                        analysis.droppedContaminant(),
                        analysis.droppedReverse(),
                        analysis.droppedOnlyBySite(),
                        analysis.droppedLocalization(),
                        analysis.droppedMissing()));
        view.resultsSummary.setText(
                String.format(
                        text.get("results.summary"),
                        analysis.sites().size(),
                        analysis.significantCount(),
                        analysis.imputedValues()));
        view.table.getItems().addAll(analysis.sites());
        drawVolcano(analysis);
        view.status.setText(status(text, analysis.status()));
    }

    private void drawVolcano(Analysis analysis) {
        for (SiteStatistic site : analysis.sites()) {
            Circle point = new Circle(4);
            point.setLayoutX(VolcanoGeometry.x(site.log2FoldChange(), 500));
            point.setLayoutY(VolcanoGeometry.y(site.negLog10P(), 260));
            point.getStyleClass().add(styleOf(site));
            point.setUserData(site.gene());
            view.volcano.getChildren().add(point);
        }
    }

    private static String styleOf(SiteStatistic site) {
        if (!site.significant()) {
            return "point-none";
        }
        return site.log2FoldChange() >= 0 ? "point-up" : "point-down";
    }

    private String call(SiteStatistic site) {
        Messages text = messages();
        if (!site.significant()) {
            return text.get("results.no");
        }
        return site.log2FoldChange() >= 0 ? text.get("results.up") : text.get("results.down");
    }

    private String status(Messages text, AnalysisStatus status) {
        if (status == AnalysisStatus.NEED_GROUPS) {
            return text.get("status.needGroups");
        }
        if (status == AnalysisStatus.NEED_REPLICATES) {
            return text.get("status.needReplicates");
        }
        if (status == AnalysisStatus.NO_SITE) {
            return text.get("status.noSite");
        }
        return text.get("status.ready");
    }

    private void showError(String code) {
        if (code != null) {
            view.status.setText(explain(messages(), code));
        }
    }

    private String explain(Messages text, String code) {
        String key = "error." + code;
        String message = text.get(key);
        return message.startsWith("!") ? text.get("error.generic") : message;
    }

    private Messages messages() {
        return Messages.of(model.language());
    }

    private String website() {
        return model.language() == Language.FR ? AppInfo.WEBSITE_FR : AppInfo.WEBSITE_EN;
    }

    private static double number(String raw, double fallback) {
        try {
            return Double.parseDouble(raw.trim());
        } catch (NumberFormatException | NullPointerException e) {
            return fallback;
        }
    }

    private static int integer(String raw, int fallback) {
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException | NullPointerException e) {
            return fallback;
        }
    }

    static javafx.beans.value.ObservableValue<String> text(double value) {
        String rendered = Double.isFinite(value) ? String.format(Locale.ROOT, "%.4g", value) : "";
        return javafx.beans.binding.Bindings.createStringBinding(() -> rendered);
    }
}
