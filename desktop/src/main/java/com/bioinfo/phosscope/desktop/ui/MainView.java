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

import com.bioinfo.phosscope.model.ImputationMethod;
import com.bioinfo.phosscope.model.SiteStatistic;

import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

/**
 * JavaFX view: menus, five tabs, the footer and the status line. It displays state and exposes the
 * controls; it does not analyze anything.
 */
public final class MainView {

    final MenuItem openItem = new MenuItem();
    final MenuItem exampleItem = new MenuItem();
    final MenuItem exportItem = new MenuItem();
    final MenuItem quitItem = new MenuItem();
    final RadioMenuItem frenchItem = new RadioMenuItem();
    final RadioMenuItem englishItem = new RadioMenuItem();
    final MenuItem smallerItem = new MenuItem();
    final MenuItem largerItem = new MenuItem();
    final MenuItem resetFontItem = new MenuItem();
    final MenuItem guideItem = new MenuItem();
    final Menu fileMenu = new Menu();
    final Menu optionsMenu = new Menu();
    final Menu languageMenu = new Menu();
    final Menu fontMenu = new Menu();
    final Menu helpMenu = new Menu();
    final Label intro = new Label();
    final Label fileLabel = new Label();
    final Label formatLabel = new Label();
    final Label simulatedLabel = new Label();
    final VBox samples = new VBox(6);
    final CheckBox contaminants = new CheckBox();
    final CheckBox reverse = new CheckBox();
    final CheckBox onlySite = new CheckBox();
    final TextField localization = field();
    final TextField minValid = field();
    final Label cleaningSummary = new Label();
    final ComboBox<ImputationMethod> imputation = new ComboBox<>();
    final TextField downshift = field();
    final TextField width = field();
    final TextField seed = field();
    final Label imputationNote = new Label();
    final TextField fdr = field();
    final TextField fold = field();
    final Label resultsSummary = new Label();
    final TableView<SiteStatistic> table = new TableView<>();
    final TableColumn<SiteStatistic, String> geneColumn = new TableColumn<>();
    final TableColumn<SiteStatistic, String> siteColumn = new TableColumn<>();
    final TableColumn<SiteStatistic, String> fcColumn = new TableColumn<>();
    final TableColumn<SiteStatistic, String> pColumn = new TableColumn<>();
    final TableColumn<SiteStatistic, String> qColumn = new TableColumn<>();
    final TableColumn<SiteStatistic, String> callColumn = new TableColumn<>();
    final Pane volcano = new Pane();
    final TextField reportTitle = new TextField();
    final Button reportButton = new Button();
    final TextArea help = new TextArea();
    final Label status = new Label();
    final Button kofiButton = new Button();
    final Button websiteButton = new Button();
    final Label availability = new Label();
    final Label author = new Label();
    final Tab dataTab = new Tab();
    final Tab cleaningTab = new Tab();
    final Tab imputationTab = new Tab();
    final Tab resultsTab = new Tab();
    final Tab reportTab = new Tab();
    private final BorderPane root = new BorderPane();

    /** Builds the window contents. */
    public MainView() {
        ToggleGroup languages = new ToggleGroup();
        frenchItem.setToggleGroup(languages);
        englishItem.setToggleGroup(languages);
        fileMenu.getItems().addAll(openItem, exampleItem, exportItem, quitItem);
        languageMenu.getItems().addAll(frenchItem, englishItem);
        fontMenu.getItems().addAll(smallerItem, largerItem, resetFontItem);
        optionsMenu.getItems().addAll(languageMenu, fontMenu);
        helpMenu.getItems().add(guideItem);
        MenuBar bar = new MenuBar(fileMenu, optionsMenu, helpMenu);
        TabPane tabs = new TabPane(dataTab, cleaningTab, imputationTab, resultsTab, reportTab);
        for (Tab tab : tabs.getTabs()) {
            tab.setClosable(false);
        }
        dataTab.setContent(dataPane());
        cleaningTab.setContent(cleaningPane());
        imputationTab.setContent(imputationPane());
        resultsTab.setContent(resultsPane());
        reportTab.setContent(reportPane());
        help.setEditable(false);
        help.setWrapText(true);
        help.setVisible(false);
        help.setManaged(false);
        help.setId("helpText");
        status.setId("status");
        table.setId("results");
        volcano.setId("volcano");
        volcano.setPrefSize(520, 280);
        kofiButton.setId("kofiButton");
        websiteButton.setId("websiteButton");
        exampleItem.setId("exampleItem");
        root.setTop(bar);
        root.setCenter(new VBox(tabs, help));
        root.setBottom(footer());
        root.getStyleClass().add("phos-root");
        root.setId("root");
    }

    /** Root node of the window. */
    public Parent root() {
        return root;
    }

    private VBox dataPane() {
        samples.setId("samples");
        VBox box = column(intro, fileLabel, formatLabel, simulatedLabel, samples);
        box.setId("dataTab");
        return box;
    }

    private VBox cleaningPane() {
        VBox box = column(contaminants, reverse, onlySite, localization, minValid, cleaningSummary);
        box.setId("cleaningTab");
        return box;
    }

    private VBox imputationPane() {
        imputation.getItems().setAll(ImputationMethod.values());
        imputation.setId("imputation");
        imputationNote.setWrapText(true);
        VBox box = column(imputation, downshift, width, seed, imputationNote);
        box.setId("imputationTab");
        return box;
    }

    private VBox resultsPane() {
        table.getColumns().addAll(geneColumn, siteColumn, fcColumn, pColumn, qColumn, callColumn);
        table.setPrefHeight(220);
        VBox box = column(fdr, fold, resultsSummary, table, volcano);
        box.setId("resultsTab");
        return box;
    }

    private VBox reportPane() {
        reportTitle.setId("reportTitle");
        reportButton.setId("reportButton");
        VBox box = column(reportTitle, reportButton);
        box.setId("reportTab");
        return box;
    }

    private VBox footer() {
        author.setId("author");
        availability.setId("availability");
        HBox box = new HBox(12, author, websiteButton, availability, kofiButton);
        box.setPadding(new Insets(8, 12, 8, 12));
        box.getStyleClass().add("footer");
        return new VBox(status, box);
    }

    private static VBox column(javafx.scene.Node... nodes) {
        VBox box = new VBox(8, nodes);
        box.setPadding(new Insets(12));
        return box;
    }

    private static TextField field() {
        TextField field = new TextField();
        field.setMaxWidth(220);
        return field;
    }
}
