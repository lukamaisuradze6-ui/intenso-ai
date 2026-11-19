package com.example;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.InputStream;

public class App extends Application {

    @Override
    public void start(Stage stage) {
        try {
            Font.loadFont(getClass().getResourceAsStream("/com/example/Fonts/CW_GRANB.ttf"), 16);
        } catch (Exception ignored) {
        }

        VBox root = new VBox();
        root.getStyleClass().add("appRoot");
        root.getStylesheets().add(getClass().getResource("style.css").toExternalForm());

        HBox titleBar = new HBox(10);
        titleBar.setAlignment(Pos.CENTER_LEFT);
        titleBar.setPadding(new Insets(10, 14, 10, 18));
        titleBar.getStyleClass().add("titleBar");

        Label statusDot = new Label("●");
        statusDot.getStyleClass().add("statusDot");

        Label appTitle = new Label("INTENSO AI");
        appTitle.getStyleClass().add("appTitle");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button minBtn = new Button("—");
        minBtn.getStyleClass().add("winBtn");

        Button maxBtn = new Button("□");
        maxBtn.getStyleClass().add("winBtn");

        Button closeBtn = new Button("✕");
        closeBtn.getStyleClass().add("winBtnClose");

        titleBar.getChildren().addAll(statusDot, appTitle, spacer, minBtn, maxBtn, closeBtn);

        HBox bodyContainer = new HBox();
        VBox.setVgrow(bodyContainer, Priority.ALWAYS);

        VBox sidebar = new VBox(14);
        sidebar.setPrefWidth(260);
        sidebar.setPadding(new Insets(18, 14, 18, 14));
        sidebar.getStyleClass().add("sidebar");

        Button newChatBtn = new Button("+  New conversation");
        newChatBtn.setMaxWidth(Double.MAX_VALUE);
        newChatBtn.getStyleClass().add("newChatButton");

        Label recentLabel = new Label("RECENT");
        recentLabel.getStyleClass().add("sectionLabel");

        VBox historyContainer = new VBox(4);
        ScrollPane historyScroll = new ScrollPane(historyContainer);
        historyScroll.setFitToWidth(true);
        historyScroll.getStyleClass().add("transparentScroll");
        VBox.setVgrow(historyScroll, Priority.ALWAYS);

        sidebar.getChildren().addAll(newChatBtn, recentLabel, historyScroll);

        VBox mainStage = new VBox();
        mainStage.setAlignment(Pos.CENTER);
        mainStage.getStyleClass().add("mainStage");
        HBox.setHgrow(mainStage, Priority.ALWAYS);

        StackPane centerStack = new StackPane();
        centerStack.setMaxWidth(820);
        VBox.setVgrow(centerStack, Priority.ALWAYS);

        VBox emptyStateBox = new VBox(10);
        emptyStateBox.setAlignment(Pos.CENTER);
        Label heroTitle = new Label("What's on your mind?");
        heroTitle.getStyleClass().add("heroTitle");
        Label heroSubtitle = new Label("Fast • Unfiltered • Persistent Memory");
        heroSubtitle.getStyleClass().add("heroSubtitle");
        emptyStateBox.getChildren().addAll(heroTitle, heroSubtitle);

        VBox chatContainer = new VBox(18);
        chatContainer.setPadding(new Insets(24));

        ScrollPane chatScrollPane = new ScrollPane(chatContainer);
        chatScrollPane.setFitToWidth(true);
        chatScrollPane.getStyleClass().add("transparentScroll");

        centerStack.getChildren().addAll(emptyStateBox, chatScrollPane);

        VBox inputWrapper = new VBox();
        inputWrapper.setAlignment(Pos.CENTER);
        inputWrapper.setMaxWidth(820);
        inputWrapper.setPadding(new Insets(0, 24, 24, 24));

        HBox floatingInputBar = new HBox(10);
        floatingInputBar.setAlignment(Pos.CENTER);
        floatingInputBar.setPadding(new Insets(8, 8, 8, 20));
        floatingInputBar.getStyleClass().add("floatingInputBar");

        TextField inputField = new TextField();
        inputField.setPromptText("Message your AI...");
        inputField.setPrefHeight(44);
        inputField.getStyleClass().add("inputText");
        HBox.setHgrow(inputField, Priority.ALWAYS);

        SVGPath sendIcon = new SVGPath();
        sendIcon.setContent("M2.01 21L23 12 2.01 3 2 10l15 2-15 2z");
        sendIcon.getStyleClass().add("sendIcon");

        Button sendBtn = new Button();
        sendBtn.setPrefSize(40, 40);
        sendBtn.setGraphic(sendIcon);
        sendBtn.getStyleClass().add("sendButton");

        floatingInputBar.getChildren().addAll(inputField, sendBtn);
        inputWrapper.getChildren().add(floatingInputBar);

        mainStage.getChildren().addAll(centerStack, inputWrapper);
        bodyContainer.getChildren().addAll(sidebar, mainStage);
        root.getChildren().addAll(titleBar, bodyContainer);

        Controller controller = new Controller(
                titleBar,
                emptyStateBox,
                inputField,
                chatContainer,
                historyContainer,
                chatScrollPane
        );

        minBtn.setOnAction(e -> controller.handleMinimize());
        maxBtn.setOnAction(e -> controller.handleMaximize());
        closeBtn.setOnAction(e -> controller.handleClose());
        newChatBtn.setOnAction(e -> controller.handleNewChat());
        inputField.setOnAction(e -> controller.handleButtonClick());
        sendBtn.setOnAction(e -> controller.handleButtonClick());

        Scene scene = new Scene(root, 1100, 740);
        scene.setFill(Color.web("#0d0d12"));

        InputStream iconStream = getClass().getResourceAsStream("/icon.png");
        if (iconStream != null) {
            stage.getIcons().add(new Image(iconStream));
        }

        stage.setTitle("Intenso AI");
        stage.initStyle(StageStyle.UNDECORATED);
        stage.setResizable(true);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}