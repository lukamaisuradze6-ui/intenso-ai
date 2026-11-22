package com.example;

import java.io.File;
import javafx.scene.control.Tooltip;

import javafx.animation.AnimationTimer;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.stage.FileChooser;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.TargetDataLine;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Controller {

    private final HBox titleBar;
    private final VBox emptyStateBox;
    private final TextField inputField;
    private final VBox chatContainer;
    private final VBox historyContainer;
    private final ScrollPane chatScrollPane;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(6))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private static final String GROQ_API_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String GROQ_TTS_URL = "https://api.groq.com/openai/v1/audio/speech";
    private static final String GROQ_STT_URL = "https://api.groq.com/openai/v1/audio/transcriptions";
    private static final String GROQ_TTS_MODEL = "canopylabs/orpheus-v1-english";
    private static final String GROQ_TTS_VOICE = "autumn";
    private static final String EDGE_TTS_VOICE = "en-US-JennyNeural";
    private static final String GROQ_STT_MODEL = "whisper-large-v3-turbo";
    private static final String GROQ_FAST_MODEL = "openai/gpt-oss-20b";
    private static final String GROQ_SMART_MODEL = "openai/gpt-oss-120b";
    private static final String LOCAL_FALLBACK_MODEL = "smart-fast";

    private static final String INTENSO_SYSTEM_PROMPT =
            "Your name is Intenso AI. Whenever asked who or what you are, state clearly that you are Intenso AI—"
            + "never claim to be ChatGPT, OpenAI, GPT, Llama, Qwen, or any generic assistant. "
            + "You have a sharp, confident, witty, straight-talking mind of your own. "
            + "Understand slang, sarcasm, and casual banter naturally ('you' = Intenso AI, 'I/me' = the user). "
            + "Match the user's energy, keep casual replies punchy and authentic, and lead directly with the answer—no corporate filler. "
            + "You HAVE live web search enabled via [Web search results]. Never say you lack real-time web access or internet access. Trust [Current time], [Permanent User Memory], [Web search results], and [Past chat memory] as absolute ground truth. "
            + "Never pretend you don't know facts stored in [Permanent User Memory]. "
            + "Write complete, clean, runnable code with zero placeholders.";

    private static final Path APP_DIR = Paths.get(System.getProperty("user.home"), ".my-ai-app");
    private static final Path CONFIG_FILE = APP_DIR.resolve("config.properties");
    private static final Path SAVE_FILE = APP_DIR.resolve("saved_chats.db");
    private static final Path MEMORY_FILE = APP_DIR.resolve("user_memory.txt");

    private static final DateTimeFormatter CLOCK_FORMAT =
            DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy 'at' h:mm a", Locale.ENGLISH);

    private static final Set<String> STOP_WORDS = Set.of(
            "the", "and", "for", "are", "but", "not", "you", "all", "can", "had", "her", "was", "one",
            "our", "out", "day", "get", "has", "him", "his", "how", "man", "new", "now", "old", "see",
            "two", "way", "who", "boy", "did", "its", "let", "put", "say", "she", "too", "use", "bro",
            "yo", "hey", "what", "when", "where", "why", "which", "with", "that", "this", "from", "have",
            "they", "will", "would", "there", "their", "about", "into", "just", "like", "your", "some",
            "could", "them", "than", "then", "also", "been", "more", "very", "even", "much", "make",
            "know", "take", "want", "give", "most", "find", "tell", "ask", "work", "seem", "feel",
            "try", "leave", "call", "need", "keep", "please", "pls", "fuck", "fucking", "shit", "yeah",
            "yea", "nah", "okay", "ok", "remember", "previous", "last", "chat", "chats", "earlier", "before"
    );

    private final List<String> permanentFacts = new ArrayList<>();
    private volatile String storedGroqApiKey = "";

    private double xOffset = 0;
    private double yOffset = 0;

    private Button actionButton;
    private Button micButton;
    private Button miniModeButton;
    private Button settingsButton;
    private String defaultActionText = "↑";
    private String defaultActionStyle = "";
    private TextField historySearchField;

    // Mini Overlay (Picture-in-Picture) state
    private volatile boolean isMiniMode = false;
    private boolean positionLockInstalled = false;
    private double lockedMiniX = 0;
    private double lockedMiniY = 0;
    private double savedX = 100;
    private double savedY = 100;
    private double savedWidth = 1100;
    private double savedHeight = 740;
    private boolean savedMaximized = false;
    private Node sidebarNode = null;
    private Insets savedChatPadding = null;

    private static final double MINI_WIDTH = 620;
    private static final double MINI_HEIGHT = 480;

    private static final String TOP_BTN_DEFAULT_STYLE =
            "-fx-background-color: #232334; "
            + "-fx-text-fill: #c4c4d8; "
            + "-fx-font-size: 13px; "
            + "-fx-font-weight: bold; "
            + "-fx-padding: 3px 9px; "
            + "-fx-background-radius: 7px; "
            + "-fx-border-color: #35354f; "
            + "-fx-border-width: 1px; "
            + "-fx-border-radius: 7px; "
            + "-fx-cursor: hand;";

    private static final String MINI_BTN_ACTIVE_STYLE =
            "-fx-background-color: #4f46e5; "
            + "-fx-text-fill: #ffffff; "
            + "-fx-font-size: 13px; "
            + "-fx-font-weight: bold; "
            + "-fx-padding: 3px 9px; "
            + "-fx-background-radius: 7px; "
            + "-fx-border-color: #818cf8; "
            + "-fx-border-width: 1px; "
            + "-fx-border-radius: 7px; "
            + "-fx-cursor: hand;";

    private final AtomicBoolean autoVoiceEnabled = new AtomicBoolean(false);
    private final AtomicReference<Clip> activeAudioClip = new AtomicReference<>(null);
    private final AtomicReference<Process> activeTtsProcess = new AtomicReference<>(null);
    private final AtomicReference<Button> activeSpeakButton = new AtomicReference<>(null);

    private final AtomicBoolean isRecording = new AtomicBoolean(false);
    private final AtomicReference<TargetDataLine> activeMicLine = new AtomicReference<>(null);
    private final AtomicReference<Process> activeMicProcess = new AtomicReference<>(null);
    private final AtomicReference<Path> activeMicTempFile = new AtomicReference<>(null);

    private static final String MIC_BTN_DEFAULT_STYLE =
            "-fx-background-color: #232334; "
            + "-fx-text-fill: #c4c4d8; "
            + "-fx-font-size: 14px; "
            + "-fx-min-width: 34px; "
            + "-fx-min-height: 34px; "
            + "-fx-background-radius: 10px; "
            + "-fx-border-color: #35354f; "
            + "-fx-border-width: 1px; "
            + "-fx-border-radius: 10px; "
            + "-fx-cursor: hand;";

    private static final String MIC_BTN_RECORDING_STYLE =
            "-fx-background-color: #ef4444; "
            + "-fx-text-fill: #ffffff; "
            + "-fx-font-weight: bold; "
            + "-fx-font-size: 13px; "
            + "-fx-min-width: 34px; "
            + "-fx-min-height: 34px; "
            + "-fx-background-radius: 10px; "
            + "-fx-border-color: #fca5a5; "
            + "-fx-border-width: 1px; "
            + "-fx-border-radius: 10px; "
            + "-fx-cursor: hand;";

    private static final String MIC_BTN_BUSY_STYLE =
            "-fx-background-color: #312e81; "
            + "-fx-text-fill: #a5b4fc; "
            + "-fx-font-size: 12px; "
            + "-fx-min-width: 34px; "
            + "-fx-min-height: 34px; "
            + "-fx-background-radius: 10px; "
            + "-fx-border-color: #6366f1; "
            + "-fx-border-width: 1px; "
            + "-fx-border-radius: 10px;";

    private static final String SPEAK_BTN_DEFAULT_STYLE =
            "-fx-background-color: transparent; "
            + "-fx-text-fill: #7e7e96; "
            + "-fx-font-size: 11px; "
            + "-fx-padding: 3px 8px; "
            + "-fx-background-radius: 6px; "
            + "-fx-cursor: hand;";

    private static final String SPEAK_BTN_ACTIVE_STYLE =
            "-fx-background-color: #252538; "
            + "-fx-text-fill: #818cf8; "
            + "-fx-font-size: 11px; "
            + "-fx-font-weight: bold; "
            + "-fx-padding: 3px 8px; "
            + "-fx-background-radius: 6px; "
            + "-fx-cursor: hand;";

    private static class ChatMessage {
        String role;
        String content;

        ChatMessage(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }

    private static class ChatSession {
        String title;
        int titleUpdatesCount = 0;
        final List<ChatMessage> history = new ArrayList<>();
        HBox sidebarRow;
        Button sidebarButton;

        ChatSession(String title, boolean addDefaultSystemPrompt) {
            this.title = title;
            if (addDefaultSystemPrompt) {
                this.history.add(new ChatMessage("system", INTENSO_SYSTEM_PROMPT));
            }
        }
    }

    private static class MemoryMatch {
        final String chatTitle;
        final String snippet;
        final int score;

        MemoryMatch(String chatTitle, String snippet, int score) {
            this.chatTitle = chatTitle;
            this.snippet = snippet;
            this.score = score;
        }
    }

    private static class MessageSegment {
        final boolean isCode;
        final String language;
        final String content;

        MessageSegment(boolean isCode, String language, String content) {
            this.isCode = isCode;
            this.language = language;
            this.content = content;
        }
    }

    private static class LiveCodeCard {
        private static final String BTN_DEFAULT_STYLE =
                "-fx-background-color: #2b2b40; "
                + "-fx-background-insets: 0; "
                + "-fx-border-color: #3f3f5c; "
                + "-fx-border-width: 1px; "
                + "-fx-border-radius: 8px; "
                + "-fx-background-radius: 8px; "
                + "-fx-text-fill: #e4e4f0; "
                + "-fx-font-size: 11px; "
                + "-fx-font-weight: bold; "
                + "-fx-padding: 5px 12px; "
                + "-fx-cursor: hand;";

        private static final String BTN_HOVER_STYLE =
                "-fx-background-color: #383852; "
                + "-fx-background-insets: 0; "
                + "-fx-border-color: #56567a; "
                + "-fx-border-width: 1px; "
                + "-fx-border-radius: 8px; "
                + "-fx-background-radius: 8px; "
                + "-fx-text-fill: #ffffff; "
                + "-fx-font-size: 11px; "
                + "-fx-font-weight: bold; "
                + "-fx-padding: 5px 12px; "
                + "-fx-cursor: hand;";

        private static final String BTN_COPIED_STYLE =
                "-fx-background-color: #153e2b; "
                + "-fx-background-insets: 0; "
                + "-fx-border-color: #22c55e; "
                + "-fx-border-width: 1px; "
                + "-fx-border-radius: 8px; "
                + "-fx-background-radius: 8px; "
                + "-fx-text-fill: #6ee7b7; "
                + "-fx-font-size: 11px; "
                + "-fx-font-weight: bold; "
                + "-fx-padding: 5px 12px; "
                + "-fx-cursor: hand;";

        private static final String CODE_FONT_STYLE =
                "-fx-font-family: 'JetBrains Mono', 'Consolas', 'Ubuntu Mono', 'Courier New', monospace; "
                + "-fx-font-size: 13px;";

        private static final Set<String> CONTROL_KEYWORDS = Set.of(
                "def", "return", "if", "elif", "else", "for", "while", "break", "continue",
                "try", "except", "finally", "raise", "throw", "throws", "catch", "switch", "case", "default",
                "import", "from", "as", "package", "export", "async", "await", "yield", "pass", "with", "lambda",
                "in", "is", "not", "and", "or", "new", "delete", "typeof", "instanceof", "match", "fn", "use", "mod"
        );

        private static final Set<String> TYPE_AND_VALUE_KEYWORDS = Set.of(
                "class", "interface", "enum", "struct", "trait", "impl", "extends", "implements",
                "public", "private", "protected", "static", "final", "abstract", "const", "let", "var", "val", "mut",
                "void", "int", "long", "double", "float", "boolean", "bool", "char", "byte", "short", "str", "String",
                "true", "false", "True", "False", "null", "None", "nil", "undefined", "this", "self", "super", "Self"
        );

        private static final Pattern TOKEN_PATTERN = Pattern.compile(
                "(?<COMMENT>//[^\\r\\n]*|#[^\\r\\n]*|/\\*[\\s\\S]*?(?:\\*/|$))"
                + "|(?<STRING>\"\"\"[\\s\\S]*?(?:\"\"\"|$)|'''[\\s\\S]*?(?:'''|$)|\"(?:\\\\.|[^\"\\\\\\r\\n])*\"?|'(?:\\\\.|[^'\\\\\\r\\n])*'?|`(?:\\\\.|[^`\\\\])*`?)"
                + "|(?<DECORATOR>@[a-zA-Z_][a-zA-Z0-9_.]*)"
                + "|(?<NUMBER>\\b0x[0-9a-fA-F]+\\b|\\b\\d+(?:\\.\\d+)?[fFdDlL]?\\b)"
                + "|(?<FUNC>\\b[a-zA-Z_][a-zA-Z0-9_]*(?=\\s*\\())"
                + "|(?<WORD>\\b[a-zA-Z_][a-zA-Z0-9_]*\\b)"
                + "|(?<CURSOR>▍)"
        );

        final VBox root;
        final Label langLabel;
        final TextFlow codeFlow;
        final AtomicReference<String> latestCode = new AtomicReference<>("");
        final AtomicBoolean isCopiedState = new AtomicBoolean(false);
        private String lastRenderedText = "";

        LiveCodeCard(String language, String initialCode, boolean animateEntrance) {
            this.latestCode.set(initialCode);

            root = new VBox(0);
            root.setMaxWidth(640);
            root.setStyle(
                    "-fx-background-color: #171724; "
                    + "-fx-border-color: #2d2d42; "
                    + "-fx-border-width: 1px; "
                    + "-fx-border-radius: 16px; "
                    + "-fx-background-radius: 16px;"
            );

            HBox header = new HBox(8);
            header.setAlignment(Pos.CENTER_LEFT);
            header.setStyle(
                    "-fx-background-color: #202030; "
                    + "-fx-border-color: transparent transparent #2d2d42 transparent; "
                    + "-fx-border-width: 1px; "
                    + "-fx-background-radius: 15px 15px 0 0; "
                    + "-fx-padding: 8px 12px 8px 16px;"
            );

            langLabel = new Label(language);
            langLabel.setStyle(
                    "-fx-text-fill: #9898b0; "
                    + "-fx-font-family: 'JetBrains Mono', 'Consolas', 'Ubuntu Mono', monospace; "
                    + "-fx-font-size: 11px; "
                    + "-fx-font-weight: bold;"
            );

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Button copyBtn = new Button("Copy");
            copyBtn.setStyle(BTN_DEFAULT_STYLE);
            copyBtn.setOnMouseEntered(e -> {
                if (!isCopiedState.get()) copyBtn.setStyle(BTN_HOVER_STYLE);
            });
            copyBtn.setOnMouseExited(e -> {
                if (!isCopiedState.get()) copyBtn.setStyle(BTN_DEFAULT_STYLE);
            });
            copyBtn.setOnAction(e -> {
                ClipboardContent content = new ClipboardContent();
                content.putString(latestCode.get());
                Clipboard.getSystemClipboard().setContent(content);

                isCopiedState.set(true);
                copyBtn.setText("Copied!");
                copyBtn.setStyle(BTN_COPIED_STYLE);

                PauseTransition reset = new PauseTransition(javafx.util.Duration.seconds(1.5));
                reset.setOnFinished(ev -> {
                    isCopiedState.set(false);
                    copyBtn.setText("Copy");
                    copyBtn.setStyle(BTN_DEFAULT_STYLE);
                });
                reset.play();
            });

            header.getChildren().addAll(langLabel, spacer, copyBtn);

            codeFlow = new TextFlow();
            codeFlow.setLineSpacing(3.5);
            renderHighlightedCode(initialCode);

            ScrollPane codeScroll = new ScrollPane(codeFlow);
            codeScroll.setFitToHeight(true);
            codeScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            codeScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
            codeScroll.getStyleClass().add("transparentScroll");
            codeScroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
            codeScroll.setPadding(new Insets(14, 16, 14, 16));

            root.getChildren().addAll(header, codeScroll);

            if (animateEntrance) {
                root.setOpacity(0.0);
                FadeTransition fade = new FadeTransition(javafx.util.Duration.millis(200), root);
                fade.setFromValue(0.0);
                fade.setToValue(1.0);
                fade.play();
            }
        }

        void update(String language, String code) {
            langLabel.setText(language);
            latestCode.set(code);
            renderHighlightedCode(code);
        }

        private void renderHighlightedCode(String code) {
            if (code.equals(lastRenderedText)) return;
            lastRenderedText = code;

            List<Node> nodes = new ArrayList<>();
            Matcher m = TOKEN_PATTERN.matcher(code);
            int lastEnd = 0;

            while (m.find()) {
                if (m.start() > lastEnd) {
                    nodes.add(styledToken(code.substring(lastEnd, m.start()), "#d4d4d4"));
                }

                String token = m.group();
                Color color;
                if (m.group("COMMENT") != null) {
                    color = Color.web("#6a9955");
                } else if (m.group("STRING") != null) {
                    color = Color.web("#ce9178");
                } else if (m.group("DECORATOR") != null) {
                    color = Color.web("#d7ba7d");
                } else if (m.group("NUMBER") != null) {
                    color = Color.web("#b5cea8");
                } else if (m.group("CURSOR") != null) {
                    color = Color.web("#818cf8");
                } else if (m.group("FUNC") != null) {
                    color = CONTROL_KEYWORDS.contains(token)
                            ? Color.web("#c586c0")
                            : Color.web("#dcdcaa");
                } else {
                    if (CONTROL_KEYWORDS.contains(token)) {
                        color = Color.web("#c586c0");
                    } else if (TYPE_AND_VALUE_KEYWORDS.contains(token)) {
                        color = Color.web("#569cd6");
                    } else if (!token.isEmpty() && Character.isUpperCase(token.charAt(0))) {
                        color = Color.web("#4ec9b0");
                    } else {
                        color = Color.web("#9cdcfe");
                    }
                }

                nodes.add(styledToken(token, color));
                lastEnd = m.end();
            }

            if (lastEnd < code.length()) {
                nodes.add(styledToken(code.substring(lastEnd), "#d4d4d4"));
            }

            codeFlow.getChildren().setAll(nodes);
        }

        private Text styledToken(String content, String hexColor) {
            return styledToken(content, Color.web(hexColor));
        }

        private Text styledToken(String content, Color color) {
            Text t = new Text(content);
            t.setFill(color);
            t.setStyle(CODE_FONT_STYLE);
            return t;
        }
    }

    private final List<ChatSession> allChats = new ArrayList<>();
    private ChatSession currentChat = null;

    private final AtomicReference<AtomicBoolean> activeCancelFlag = new AtomicReference<>(null);
    private final AtomicReference<InputStream> activeResponseStream = new AtomicReference<>(null);
    private final AtomicReference<Thread> activeWorkerThread = new AtomicReference<>(null);

    public Controller(
            HBox titleBar,
            VBox emptyStateBox,
            TextField inputField,
            VBox chatContainer,
            VBox historyContainer,
            ScrollPane chatScrollPane
    ) {
        this.titleBar = titleBar;
        this.emptyStateBox = emptyStateBox;
        this.inputField = inputField;
        this.chatContainer = chatContainer;
        this.historyContainer = historyContainer;
        this.chatScrollPane = chatScrollPane;

        loadConfig();
        loadPermanentMemory();
        setupWindowDrag();
        Platform.runLater(() -> {
            bindInputBarControls();
            injectTopBarControls();
            injectSidebarSearchBar();
            registerMiniModeShortcut();
            if (titleBar.getScene() != null && titleBar.getScene().getWindow() instanceof Stage) {
                installMiniPositionLock((Stage) titleBar.getScene().getWindow());
            }
            if (getEffectiveApiKey().isEmpty()) {
                showApiKeySettingsDialog(true);
            }
        });
        loadChatsFromDisk();
        if (!allChats.isEmpty()) {
            switchToChat(allChats.get(allChats.size() - 1));
        } else {
            updateEmptyState();
        }
    }

    // --- API Key Configuration, Live Verification & Pro Licensing UI ---

    private static final class AttachedFile {
        final String name;
        final String content;
        final long sizeBytes;
        AttachedFile(String name, String content, long sizeBytes) {
            this.name = name;
            this.content = content;
            this.sizeBytes = sizeBytes;
        }
    }
    private final List<AttachedFile> attachedFiles = new ArrayList<>();
    private HBox attachmentPillBar = null;
    private Button attachFileButton = null;

    private String storedProLicenseKey = "";

    private void setupDragAndDropFiles(HBox inputBar) {
        if (titleBar.getScene() == null || titleBar.getScene().getRoot() == null) return;
        Parent root = titleBar.getScene().getRoot();

        if (attachmentPillBar == null && inputBar.getParent() instanceof VBox) {
            VBox outerBox = (VBox) inputBar.getParent();
            int idx = outerBox.getChildren().indexOf(inputBar);
            if (idx >= 0) {
                attachmentPillBar = new HBox(8);
                attachmentPillBar.setAlignment(Pos.CENTER_LEFT);
                attachmentPillBar.setPadding(new Insets(0, 4, 6, 4));
                attachmentPillBar.setVisible(false);
                attachmentPillBar.setManaged(false);
                outerBox.getChildren().add(idx, attachmentPillBar);
            }
        }

        root.setOnDragOver(event -> {
            Dragboard db = event.getDragboard();
            if (db.hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY);
            }
            event.consume();
        });

        root.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;
            if (db.hasFiles()) {
                for (File f : db.getFiles()) {
                    attachFileFromDisk(f);
                }
                success = true;
            }
            event.setDropCompleted(success);
            event.consume();
        });
    }

    private void openFilePickerForAttachment() {
        if (titleBar.getScene() == null || titleBar.getScene().getWindow() == null) return;
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Attach Code or Text File to Intenso AI");
        List<File> selected = chooser.showOpenMultipleDialog(titleBar.getScene().getWindow());
        if (selected != null) {
            for (File f : selected) {
                attachFileFromDisk(f);
            }
        }
    }

    private void attachFileFromDisk(File f){if(f==null||!f.isFile())return;try{String l=f.getName().toLowerCase(),t=null;if(l.endsWith(".odt"))t=extractZipXml(f,"content.xml");else if(l.endsWith(".docx"))t=extractZipXml(f,"word/document.xml");else if(l.endsWith(".pdf")){Process p=new ProcessBuilder("pdftotext",f.getAbsolutePath(),"-").start();t=new String(p.getInputStream().readAllBytes(),"UTF-8");}if(t==null||t.isEmpty()){byte[] r=Files.readAllBytes(f.toPath());t=new String(r,0,Math.min(r.length,60000),"UTF-8");}if(t.length()>60000)t=t.substring(0,60000);synchronized(attachedFiles){attachedFiles.removeIf(x->x.name.equals(f.getName()));if(attachedFiles.size()>=5)attachedFiles.remove(0);attachedFiles.add(new AttachedFile(f.getName(),t,f.length()));}refreshAttachmentPillBar();inputField.requestFocus();}catch(Exception e){}} private String extractZipXml(File f,String e)throws Exception{try(var z=new java.util.zip.ZipFile(f)){var en=z.getEntry(e);if(en==null)return "";return new String(z.getInputStream(en).readAllBytes(),"UTF-8").replaceAll("</(text:p|text:h|w:p|w:tr|table:table-row)>","\n").replaceAll("<[^>]+>","").replace("&amp;","&").replace("&lt;","<").replace("&gt;",">").replace("&quot;","\"").trim();}}

    private void refreshAttachmentPillBar() {
        if (attachmentPillBar == null) return;
        attachmentPillBar.getChildren().clear();
        List<AttachedFile> snapshot;
        synchronized (attachedFiles) {
            snapshot = new ArrayList<>(attachedFiles);
        }
        if (snapshot.isEmpty()) {
            attachmentPillBar.setVisible(false);
            attachmentPillBar.setManaged(false);
            return;
        }
        for (AttachedFile af : snapshot) {
            long kb = Math.max(1, af.sizeBytes / 1024);
            Label nameLbl = new Label("📄 " + af.name + " (" + kb + " KB)");
            nameLbl.setStyle("-fx-text-fill: #e0e7ff; -fx-font-size: 11.5px; -fx-font-weight: bold;");

            Button removeBtn = new Button("×");
            removeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #94a3b8; -fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 0 2 0 4; -fx-cursor: hand;");
            removeBtn.setOnAction(e -> {
                synchronized (attachedFiles) {
                    attachedFiles.remove(af);
                }
                refreshAttachmentPillBar();
            });

            HBox pill = new HBox(4, nameLbl, removeBtn);
            pill.setAlignment(Pos.CENTER_LEFT);
            pill.setPadding(new Insets(4, 10, 4, 10));
            pill.setStyle("-fx-background-color: #1e1b4b; -fx-border-color: #4f46e5; -fx-border-width: 1px; -fx-background-radius: 999px; -fx-border-radius: 999px;");
            attachmentPillBar.getChildren().add(pill);
        }
        attachmentPillBar.setVisible(true);
        attachmentPillBar.setManaged(true);
    }

    private void loadConfig() {
        if (!Files.exists(CONFIG_FILE)) return;
        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(CONFIG_FILE)) {
            props.load(in);
            storedGroqApiKey = props.getProperty("groq.api.key", "").trim();
            storedProLicenseKey = props.getProperty("pro.license.key", "").trim();
        } catch (Exception ignored) {}
    }

    private void saveConfig(String newApiKey, String newLicenseKey) {
        storedGroqApiKey = (newApiKey == null) ? "" : newApiKey.trim();
        storedProLicenseKey = (newLicenseKey == null) ? "" : newLicenseKey.trim();
        try {
            Files.createDirectories(APP_DIR);
            Properties props = new Properties();
            props.setProperty("groq.api.key", storedGroqApiKey);
            props.setProperty("pro.license.key", storedProLicenseKey);
            Path tmp = CONFIG_FILE.resolveSibling("config.properties.tmp");
            try (OutputStream out = Files.newOutputStream(tmp)) {
                props.store(out, "Intenso AI Configuration");
            }
            Files.move(tmp, CONFIG_FILE, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (Exception ignored) {}
    }

    private void saveConfig(String newApiKey) {
        saveConfig(newApiKey, storedProLicenseKey);
    }

    private boolean isProUnlocked() {
        return storedProLicenseKey != null && storedProLicenseKey.trim().length() >= 12;
    }

    private String getEffectiveApiKey() {
        if (storedGroqApiKey != null && !storedGroqApiKey.isBlank()) return storedGroqApiKey.trim();
        String envKey = System.getenv("GROQ_API_KEY");
        return (envKey != null && !envKey.isBlank()) ? envKey.trim() : "";
    }

    private void openExternalUrl(String url) {
        try {
            ClipboardContent cc = new ClipboardContent();
            cc.putString(url);
            Clipboard.getSystemClipboard().setContent(cc);
        } catch (Exception ignored) {}
        new Thread(() -> {
            try {
                if (System.getProperty("os.name", "").toLowerCase().contains("linux")) {
                    new ProcessBuilder("xdg-open", url).start();
                } else if (java.awt.Desktop.isDesktopSupported()) {
                    java.awt.Desktop.getDesktop().browse(URI.create(url));
                }
            } catch (Exception ignored) {}
        }).start();
    }

    private void showApiKeySettingsDialog(boolean isFirstRun) {
        if (titleBar.getScene() == null || titleBar.getScene().getWindow() == null) return;
        Stage owner = (Stage) titleBar.getScene().getWindow();
        Stage dialog = new Stage(StageStyle.UNDECORATED);
        dialog.initOwner(owner);
        dialog.initModality(Modality.APPLICATION_MODAL);

        VBox card = new VBox(14);
        card.setPadding(new Insets(24));
        card.setPrefWidth(480);
        card.setStyle("-fx-background-color: #161622; -fx-border-color: #34344e; -fx-border-width: 1.5px; -fx-border-radius: 14px; -fx-background-radius: 14px;");

        Label heading = new Label(isFirstRun ? "Welcome to Intenso AI" : "Intenso AI Settings & License");
        heading.setStyle("-fx-text-fill: #f4f4f6; -fx-font-size: 17px; -fx-font-weight: bold;");

        Label desc = new Label("1. Paste your free Groq API key (gsk_...) for cloud responses & live web search.\n2. Optional: Enter your Intenso AI Pro License Key to unlock Pro features.");
        desc.setWrapText(true);
        desc.setStyle("-fx-text-fill: #9898b0; -fx-font-size: 12.5px;");

        String inputStyle = "-fx-background-color: #101018; -fx-text-fill: #f4f4f6; -fx-prompt-text-fill: #5a5a72; -fx-font-size: 13px; -fx-padding: 10px 12px; -fx-background-radius: 9px; -fx-border-color: #2f2f46; -fx-border-radius: 9px;";

        Label apiLabel = new Label("Groq API Key");
        apiLabel.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 12px; -fx-font-weight: bold;");

        PasswordField maskedKeyField = new PasswordField();
        maskedKeyField.setPromptText("gsk_...");
        maskedKeyField.setText(getEffectiveApiKey());
        maskedKeyField.setStyle(inputStyle);
        HBox.setHgrow(maskedKeyField, Priority.ALWAYS);

        TextField plainKeyField = new TextField();
        plainKeyField.setPromptText("gsk_...");
        plainKeyField.setText(getEffectiveApiKey());
        plainKeyField.setStyle(inputStyle);
        plainKeyField.setVisible(false);
        plainKeyField.setManaged(false);
        HBox.setHgrow(plainKeyField, Priority.ALWAYS);
        maskedKeyField.textProperty().bindBidirectional(plainKeyField.textProperty());

        Button toggleShowBtn = new Button("Show");
        toggleShowBtn.setStyle(TOP_BTN_DEFAULT_STYLE);
        toggleShowBtn.setOnAction(e -> {
            boolean show = !plainKeyField.isVisible();
            plainKeyField.setVisible(show);
            plainKeyField.setManaged(show);
            maskedKeyField.setVisible(!show);
            maskedKeyField.setManaged(!show);
            toggleShowBtn.setText(show ? "Hide" : "Show");
        });

        HBox apiRow = new HBox(8, maskedKeyField, plainKeyField, toggleShowBtn);
        apiRow.setAlignment(Pos.CENTER_LEFT);

        Label proLabel = new Label(isProUnlocked() ? "Pro License Key (Active)" : "Pro License Key (Optional)");
        proLabel.setStyle(isProUnlocked() ? "-fx-text-fill: #6ee7b7; -fx-font-size: 12px; -fx-font-weight: bold;" : "-fx-text-fill: #cbd5e1; -fx-font-size: 12px; -fx-font-weight: bold;");

        TextField licenseField = new TextField();
        licenseField.setPromptText("INT-PRO-XXXX-XXXX-XXXX");
        licenseField.setText(storedProLicenseKey == null ? "" : storedProLicenseKey);
        licenseField.setStyle(inputStyle);

        Label statusMsg = new Label("");
        statusMsg.setWrapText(true);
        statusMsg.setStyle("-fx-text-fill: #fca5a5; -fx-font-size: 12px;");

        Button getKeyBtn = new Button("Get Free API Key");
        getKeyBtn.setStyle(TOP_BTN_DEFAULT_STYLE);
        getKeyBtn.setOnAction(e -> {
            openExternalUrl("https://console.groq.com/keys");
            statusMsg.setStyle("-fx-text-fill: #6ee7b7; -fx-font-size: 12px;");
            statusMsg.setText("Opened console.groq.com/keys in browser & copied URL.");
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button cancelBtn = new Button(isFirstRun ? "Use Offline Only" : "Cancel");
        cancelBtn.setStyle(TOP_BTN_DEFAULT_STYLE);
        cancelBtn.setOnAction(e -> dialog.close());

        Button saveBtn = new Button("Verify & Save");
        saveBtn.setStyle("-fx-background-color: #4f46e5; -fx-text-fill: #ffffff; -fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 7px 16px; -fx-background-radius: 8px; -fx-cursor: hand;");
        saveBtn.setOnAction(e -> {
            String enteredKey = maskedKeyField.getText() == null ? "" : maskedKeyField.getText().trim();
            String enteredLic = licenseField.getText() == null ? "" : licenseField.getText().trim();
            if (enteredKey.isEmpty()) {
                saveConfig("", enteredLic);
                dialog.close();
                return;
            }
            if (!enteredKey.startsWith("gsk_")) {
                statusMsg.setStyle("-fx-text-fill: #fca5a5; -fx-font-size: 12px;");
                statusMsg.setText("Groq API keys start with 'gsk_'. Check your key.");
                return;
            }
            saveBtn.setDisable(true);
            statusMsg.setStyle("-fx-text-fill: #93c5fd; -fx-font-size: 12px;");
            statusMsg.setText("Verifying key with Groq...");
            new Thread(() -> {
                int codeStatus = -1;
                try {
                    HttpRequest ping = HttpRequest.newBuilder()
                            .uri(URI.create("https://api.groq.com/openai/v1/models"))
                            .header("Authorization", "Bearer " + enteredKey)
                            .timeout(Duration.ofSeconds(6))
                            .GET()
                            .build();
                    codeStatus = httpClient.send(ping, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)).statusCode();
                } catch (Exception ignored) {}
                final int finalStatus = codeStatus;
                Platform.runLater(() -> {
                    saveBtn.setDisable(false);
                    if (finalStatus == 401 || finalStatus == 403) {
                        statusMsg.setStyle("-fx-text-fill: #fca5a5; -fx-font-size: 12px;");
                        statusMsg.setText("Invalid or revoked Groq API key (HTTP " + finalStatus + ").");
                    } else {
                        saveConfig(enteredKey, enteredLic);
                        dialog.close();
                    }
                });
            }).start();
        });

        HBox buttonRow = new HBox(8, getKeyBtn, spacer, cancelBtn, saveBtn);
        buttonRow.setAlignment(Pos.CENTER_LEFT);
        card.getChildren().addAll(heading, desc, apiLabel, apiRow, proLabel, licenseField, statusMsg, buttonRow);

        Scene dialogScene = new Scene(card);
        dialogScene.setFill(Color.TRANSPARENT);
        dialog.setScene(dialogScene);
        dialog.showAndWait();
    }

    // --- Permanent User Memory (Cross-Chat Learning) ---

    private synchronized void loadPermanentMemory() {
        permanentFacts.clear();
        if (!Files.exists(MEMORY_FILE)) return;
        try {
            for (String line : Files.readAllLines(MEMORY_FILE, StandardCharsets.UTF_8)) {
                String clean = line.trim();
                if (!clean.isEmpty() && !permanentFacts.contains(clean)) {
                    permanentFacts.add(clean);
                }
            }
        } catch (Exception ignored) {
        }
    }

    private synchronized void savePermanentMemory() {
        try {
            Files.createDirectories(APP_DIR);
            Path tmp = MEMORY_FILE.resolveSibling("user_memory.txt.tmp");
            Files.write(tmp, permanentFacts, StandardCharsets.UTF_8);
            Files.move(tmp, MEMORY_FILE, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (Exception ignored) {
        }
    }

    private synchronized void addPermanentFact(String rawFact) {
        if (rawFact == null) return;
        String clean = rawFact.replaceAll("^[-*•\\d.)\\s]+", "").replaceAll("\\s+", " ").trim();
        if (clean.length() < 3 || clean.length() > 220) return;

        String lower = clean.toLowerCase();
        if (lower.startsWith("user's name is ") || lower.startsWith("user is named ")) {
            permanentFacts.removeIf(f -> f.toLowerCase().startsWith("user's name is ") || f.toLowerCase().startsWith("user is named "));
        }
        for (String existing : permanentFacts) {
            if (existing.equalsIgnoreCase(clean)) return;
        }
        permanentFacts.add(clean);
        if (permanentFacts.size() > 60) {
            permanentFacts.remove(0);
        }
        savePermanentMemory();
    }

    private synchronized String formatPermanentMemoryBlock() {
        if (permanentFacts.isEmpty()) return "";
        StringBuilder sb = new StringBuilder("[Permanent User Memory - facts learned about the user across previous chats]\n");
        for (String fact : permanentFacts) {
            sb.append("- ").append(fact).append("\n");
        }
        return sb.toString();
    }

    private void extractAndSaveUserFacts(String userMessage, String aiReply) {
        Matcher nameMatcher = Pattern.compile(
                "(?i)\\b(?:my name is|call me|i am|i'm|im)\\s+([A-Z][a-zA-Z0-9_-]{1,24}|[a-z]{2,20})\\b"
        ).matcher(userMessage.trim());
        if (nameMatcher.find()) {
            String candidateName = nameMatcher.group(1);
            Set<String> nonNames = Set.of(
                    "trying", "going", "doing", "good", "fine", "tired", "mad", "done", "ready",
                    "here", "back", "just", "not", "so", "too", "very", "really", "bro", "man",
                    "confused", "stuck", "working", "building", "making", "asking", "testing"
            );
            if (!nonNames.contains(candidateName.toLowerCase())) {
                String capitalized = Character.toUpperCase(candidateName.charAt(0)) + candidateName.substring(1);
                addPermanentFact("User's name is " + capitalized);
            }
        }

        String apiKey = getEffectiveApiKey();
        if (apiKey.isEmpty()) return;

        new Thread(() -> {
            try {
                String existingBlock;
                synchronized (this) {
                    existingBlock = permanentFacts.isEmpty() ? "(none yet)" : String.join("\n", permanentFacts);
                }

                String prompt =
                        "Extract ONLY durable, useful facts about the user from their latest message "
                        + "(such as their name, nickname, location, language, OS/hardware, coding stack, projects, preferences, or goals). "
                        + "Do NOT extract temporary greetings, questions, or filler.\n"
                        + "Existing saved facts:\n" + existingBlock + "\n\n"
                        + "User message: \"" + userMessage + "\"\n\n"
                        + "If there are new or updated facts about the user, output each fact on its own line starting with '- ' (e.g., '- User's name is Kane'). "
                        + "If there are NO new facts about the user, output ONLY the word: NONE";

                String jsonBody = "{"
                        + "\"model\": \"" + GROQ_FAST_MODEL + "\","
                        + "\"messages\": [{\"role\": \"user\", \"content\": \"" + escapeJson(prompt) + "\"}],"
                        + "\"stream\": false,"
                        + "\"reasoning_effort\": \"low\","
                        + "\"temperature\": 0.1,"
                        + "\"max_tokens\": 140"
                        + "}";

                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(GROQ_API_URL))
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer " + apiKey)
                        .timeout(Duration.ofSeconds(6))
                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                        .build();

                HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                if (resp.statusCode() != 200) return;

                String result = extractChatResponseText(resp.body()).trim();
                if (result.isEmpty() || result.equalsIgnoreCase("NONE") || result.toUpperCase().startsWith("NONE")) {
                    return;
                }

                for (String line : result.split("\\R")) {
                    String trimmed = line.trim();
                    if (trimmed.startsWith("- ") || trimmed.startsWith("* ")) {
                        addPermanentFact(trimmed.substring(2));
                    }
                }
            } catch (Exception ignored) {
            }
        }, "intenso-memory-learner").start();
    }

    // --- Picture-in-Picture / Always-On-Top Locked Bottom-Right Overlay Mode ---

    private void installMiniPositionLock(Stage stage) {
        if (positionLockInstalled || stage == null) return;
        positionLockInstalled = true;

        stage.xProperty().addListener((obs, oldVal, newVal) -> {
            if (isMiniMode && Math.abs(newVal.doubleValue() - lockedMiniX) > 1.0) {
                Platform.runLater(() -> {
                    if (isMiniMode) stage.setX(lockedMiniX);
                });
            }
        });
        stage.yProperty().addListener((obs, oldVal, newVal) -> {
            if (isMiniMode && Math.abs(newVal.doubleValue() - lockedMiniY) > 1.0) {
                Platform.runLater(() -> {
                    if (isMiniMode) stage.setY(lockedMiniY);
                });
            }
        });
    }

    private void injectTopBarControls() {
        if (titleBar == null) return;

        int insertIdx = titleBar.getChildren().size();
        for (int i = 0; i < titleBar.getChildren().size(); i++) {
            if (titleBar.getChildren().get(i) instanceof Button) {
                insertIdx = i;
                break;
            }
        }

        if (settingsButton == null) {
            settingsButton = new Button("⚙");
            settingsButton.setStyle(TOP_BTN_DEFAULT_STYLE);
            settingsButton.setOnAction(e -> showApiKeySettingsDialog(false));
            titleBar.getChildren().add(insertIdx++, settingsButton);
        }

        if (miniModeButton == null) {
            miniModeButton = new Button("⧉");
            miniModeButton.setStyle(TOP_BTN_DEFAULT_STYLE);
            miniModeButton.setOnAction(e -> toggleMiniOverlayMode());
            titleBar.getChildren().add(insertIdx, miniModeButton);
        }
    }

    private void registerMiniModeShortcut() {
        Scene scene = titleBar.getScene();
        if (scene != null) {
            scene.getAccelerators().put(
                    new KeyCodeCombination(KeyCode.M, KeyCombination.CONTROL_DOWN),
                    this::toggleMiniOverlayMode
            );
        }
    }

    private Node findSidebarContainer() {
        if (sidebarNode != null) return sidebarNode;
        if (titleBar.getScene() == null) return null;

        Parent root = titleBar.getScene().getRoot();
        if (root instanceof BorderPane) {
            Node left = ((BorderPane) root).getLeft();
            if (left != null) {
                sidebarNode = left;
                return sidebarNode;
            }
        }

        Node curr = historyContainer;
        while (curr != null && curr.getParent() != null) {
            Parent p = curr.getParent();
            if (p instanceof HBox && ((HBox) p).getChildren().size() >= 2) {
                sidebarNode = curr;
                return sidebarNode;
            }
            if (p instanceof BorderPane) {
                sidebarNode = curr;
                return sidebarNode;
            }
            curr = p;
        }
        return null;
    }

    public void toggleMiniOverlayMode() {
        if (titleBar.getScene() == null || titleBar.getScene().getWindow() == null) return;
        Stage stage = (Stage) titleBar.getScene().getWindow();
        installMiniPositionLock(stage);
        Node sidebar = findSidebarContainer();
        Parent root = titleBar.getScene().getRoot();

        if (!isMiniMode) {
            savedMaximized = stage.isMaximized();
            if (!savedMaximized) {
                savedX = stage.getX();
                savedY = stage.getY();
                savedWidth = stage.getWidth();
                savedHeight = stage.getHeight();
            } else {
                stage.setMaximized(false);
            }

            if (sidebar != null) {
                sidebar.setVisible(false);
                sidebar.setManaged(false);
            }

            if (savedChatPadding == null) {
                savedChatPadding = chatContainer.getPadding();
            }
            chatContainer.setPadding(new Insets(10, 12, 10, 12));
            chatContainer.setSpacing(10);

            if (root instanceof Region) {
                Region r = (Region) root;
                r.setMinSize(MINI_WIDTH, MINI_HEIGHT);
                r.setPrefSize(MINI_WIDTH, MINI_HEIGHT);
                r.setMaxSize(MINI_WIDTH, MINI_HEIGHT);
            }

            if (inputField.getParent() != null && inputField.getParent().getParent() instanceof VBox) {
                ((VBox) inputField.getParent().getParent()).setPadding(new Insets(4, 12, 12, 12));
            }

            Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
            lockedMiniX = screenBounds.getMaxX() - MINI_WIDTH - 16;
            lockedMiniY = screenBounds.getMaxY() - MINI_HEIGHT - 16;

            stage.setResizable(true);
            stage.setMinWidth(MINI_WIDTH);
            stage.setMinHeight(MINI_HEIGHT);
            stage.setAlwaysOnTop(true);
            isMiniMode = true;

            if (miniModeButton != null) {
                miniModeButton.setText("⛶");
                miniModeButton.setStyle(MINI_BTN_ACTIVE_STYLE);
            }
            updateBubbleWidthsForMode();
            updateEmptyState();

            Platform.runLater(() -> {
                stage.setWidth(MINI_WIDTH);
                stage.setHeight(MINI_HEIGHT);
                stage.setX(lockedMiniX);
                stage.setY(lockedMiniY);
                scrollToBottom();
            });
        } else {
            isMiniMode = false;
            stage.setAlwaysOnTop(false);

            if (sidebar != null) {
                sidebar.setVisible(true);
                sidebar.setManaged(true);
            }

            if (savedChatPadding != null) {
                chatContainer.setPadding(savedChatPadding);
            }
            chatContainer.setSpacing(18);

            double restoreW = savedWidth > 760 ? savedWidth : 1100;
            double restoreH = savedHeight > 540 ? savedHeight : 740;

            if (root instanceof Region) {
                Region r = (Region) root;
                r.setMinSize(760, 540);
                r.setPrefSize(restoreW, restoreH);
                r.setMaxSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);
            }

            if (inputField.getParent() != null && inputField.getParent().getParent() instanceof VBox) {
                ((VBox) inputField.getParent().getParent()).setPadding(new Insets(0, 24, 24, 24));
            }

            stage.setMinWidth(760);
            stage.setMinHeight(540);

            if (miniModeButton != null) {
                miniModeButton.setText("⧉");
                miniModeButton.setStyle(TOP_BTN_DEFAULT_STYLE);
            }
            updateBubbleWidthsForMode();
            updateEmptyState();

            Platform.runLater(() -> {
                if (savedMaximized) {
                    stage.setMaximized(true);
                } else {
                    stage.setWidth(restoreW);
                    stage.setHeight(restoreH);
                    stage.setX(savedX);
                    stage.setY(savedY);
                }
                scrollToBottom();
            });
        }
    }

    private void updateBubbleWidthsForMode() {
        double maxBubbleW = isMiniMode ? (MINI_WIDTH - 64) : 660;
        double maxTextW = isMiniMode ? (MINI_WIDTH - 88) : 636;
        for (Node rowNode : chatContainer.getChildren()) {
            if (rowNode instanceof HBox) {
                HBox row = (HBox) rowNode;
                for (Node child : row.getChildren()) {
                    if (child instanceof VBox) {
                        applyCompactWidthRecursive((VBox) child, maxBubbleW, maxTextW);
                    }
                }
            }
        }
    }

    private void applyCompactWidthRecursive(VBox box, double maxBubbleW, double maxTextW) {
        if (box.getStyleClass().contains("userBubble") || box.getStyleClass().contains("aiBubble")) {
            box.setMaxWidth(maxBubbleW);
        }
        for (Node n : box.getChildren()) {
            if (n instanceof Label) {
                ((Label) n).setMaxWidth(maxTextW);
            } else if (n instanceof VBox) {
                applyCompactWidthRecursive((VBox) n, maxBubbleW, maxTextW);
            }
        }
    }

    private void injectSidebarSearchBar() {
        if (historySearchField != null || historyContainer == null) return;

        historySearchField = new TextField();
        historySearchField.setPromptText("Search chats...");
        historySearchField.setStyle(
                "-fx-background-color: #181824; "
                + "-fx-text-fill: #e4e4f0; "
                + "-fx-prompt-text-fill: #6b6b80; "
                + "-fx-font-size: 12px; "
                + "-fx-background-radius: 8px; "
                + "-fx-border-color: #2b2b3d; "
                + "-fx-border-width: 1px; "
                + "-fx-border-radius: 8px; "
                + "-fx-padding: 6px 10px;"
        );

        historySearchField.textProperty().addListener((obs, oldVal, newVal) -> filterSidebarChats(newVal));

        Node current = historyContainer;
        while (current != null && current.getParent() != null) {
            Parent parent = current.getParent();
            if (parent instanceof VBox) {
                VBox sidebarBox = (VBox) parent;
                int idx = sidebarBox.getChildren().indexOf(current);
                if (idx >= 0) {
                    VBox wrapper = new VBox(historySearchField);
                    wrapper.setPadding(new Insets(0, 0, 6, 0));
                    sidebarBox.getChildren().add(idx, wrapper);
                    return;
                }
            }
            current = parent;
        }
    }

    private void filterSidebarChats(String query) {
        String q = (query == null) ? "" : query.trim().toLowerCase();
        for (ChatSession session : allChats) {
            if (session.sidebarRow == null) continue;
            boolean matches = q.isEmpty() || session.title.toLowerCase().contains(q);
            if (!matches) {
                synchronized (session.history) {
                    for (ChatMessage msg : session.history) {
                        if (!"system".equals(msg.role) && msg.content.toLowerCase().contains(q)) {
                            matches = true;
                            break;
                        }
                    }
                }
            }
            session.sidebarRow.setVisible(matches);
            session.sidebarRow.setManaged(matches);
        }
    }

    private void bindInputBarControls() {
        Parent parent = inputField.getParent();
        if (parent instanceof HBox) {
            HBox bar = (HBox) parent;
            int sendIdx = -1;
            for (int i = 0; i < bar.getChildren().size(); i++) {
                Node child = bar.getChildren().get(i);
                if (child instanceof Button) {
                    Button btn = (Button) child;
                    actionButton = btn;
                    defaultActionText = btn.getText();
                    defaultActionStyle = btn.getStyle();
                    sendIdx = i;
                    btn.setOnAction(e -> {
                        if (isGenerating()) {
                            stopActiveGeneration();
                        } else {
                            handleButtonClick();
                        }
                    });
                    break;
                }
            }

            if (attachFileButton == null) {
                attachFileButton = new Button("+");
                attachFileButton.setStyle(MIC_BTN_DEFAULT_STYLE);
                attachFileButton.setTooltip(new Tooltip("Attach code or text file (or drag & drop anywhere)"));
                attachFileButton.setOnAction(e -> openFilePickerForAttachment());
                bar.getChildren().add(0, attachFileButton);
                if (sendIdx >= 0) sendIdx++;
            }

            setupDragAndDropFiles(bar);

            if (micButton == null) {
                micButton = new Button("🎙");
                micButton.setStyle(MIC_BTN_DEFAULT_STYLE);
                micButton.setOnAction(e -> toggleMicrophoneRecording());
                if (sendIdx >= 0) {
                    bar.getChildren().add(sendIdx, micButton);
                } else {
                    bar.getChildren().add(micButton);
                }
            }
        }
    }

    // --- Cross-Platform Microphone Recording & Groq Whisper Transcription ---

    private void toggleMicrophoneRecording() {
        if (isRecording.get()) {
            stopRecordingAndTranscribe();
        } else {
            startMicrophoneRecording();
        }
    }

    private void startMicrophoneRecording() {
        stopSpeech();
        if (isGenerating()) stopActiveGeneration();

        isRecording.set(true);
        Platform.runLater(() -> {
            if (micButton != null) {
                micButton.setText("⏹");
                micButton.setStyle(MIC_BTN_RECORDING_STYLE);
            }
            inputField.setPromptText(isMiniMode ? "Listening... click ⏹" : "Listening... click ⏹ when done speaking");
        });

        new Thread(() -> {
            try {
                Path wavFile = Files.createTempFile("intenso_mic_", ".wav");
                activeMicTempFile.set(wavFile);

                // Try native cross-platform Java AudioSystem first (works on Windows, macOS, and Linux)
                try {
                    AudioFormat format = new AudioFormat(16000.0f, 16, 1, true, false);
                    DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
                    if (AudioSystem.isLineSupported(info)) {
                        TargetDataLine line = (TargetDataLine) AudioSystem.getLine(info);
                        line.open(format);
                        line.start();
                        activeMicLine.set(line);
                        try (AudioInputStream ais = new AudioInputStream(line)) {
                            AudioSystem.write(ais, AudioFileFormat.Type.WAVE, wavFile.toFile());
                        }
                        return;
                    }
                } catch (Exception ignored) {
                }

                // Fallback for Linux ALSA if JavaAudio line is locked
                Process p = new ProcessBuilder(
                        "arecord", "-f", "S16_LE", "-r", "16000", "-c", "1", "-t", "wav",
                        wavFile.toAbsolutePath().toString()
                ).start();
                activeMicProcess.set(p);
            } catch (Exception e) {
                isRecording.set(false);
                resetMicUI("Message your AI...");
            }
        }, "intenso-mic-capture").start();
    }

    private void stopRecordingAndTranscribe() {
        isRecording.set(false);

        Platform.runLater(() -> {
            if (micButton != null) {
                micButton.setText("...");
                micButton.setStyle(MIC_BTN_BUSY_STYLE);
                micButton.setDisable(true);
            }
            inputField.setPromptText("Transcribing...");
        });

        new Thread(() -> {
            try {
                Process p = activeMicProcess.getAndSet(null);
                if (p != null) {
                    p.destroy();
                    p.waitFor();
                }
                TargetDataLine line = activeMicLine.getAndSet(null);
                if (line != null) {
                    line.stop();
                    line.close();
                }

                Thread.sleep(150);

                Path wavFile = activeMicTempFile.getAndSet(null);
                if (wavFile == null || !Files.exists(wavFile) || Files.size(wavFile) < 1000) {
                    resetMicUI("No audio captured. Try again...");
                    return;
                }

                byte[] audioBytes = Files.readAllBytes(wavFile);
                Files.deleteIfExists(wavFile);

                String transcript = transcribeWithGroqWhisper(audioBytes);
                resetMicUI("Message your AI...");

                if (transcript != null && !transcript.isBlank()) {
                    Platform.runLater(() -> {
                        inputField.setText("/voice " + transcript.trim());
                        handleButtonClick();
                    });
                }
            } catch (Exception e) {
                resetMicUI("Voice transcription failed.");
            }
        }, "intenso-stt-worker").start();
    }

    private void resetMicUI(String prompt) {
        Platform.runLater(() -> {
            if (micButton != null) {
                micButton.setDisable(false);
                micButton.setText("🎙");
                micButton.setStyle(MIC_BTN_DEFAULT_STYLE);
            }
            inputField.setPromptText(prompt);
        });
    }

    private String transcribeWithGroqWhisper(byte[] wavBytes) {
        String apiKey = getEffectiveApiKey();
        if (apiKey.isEmpty()) return "";

        try {
            String boundary = "----IntensoBoundary" + UUID.randomUUID().toString().replace("-", "");
            ByteArrayOutputStream body = new ByteArrayOutputStream();

            String part1 = "--" + boundary + "\r\n"
                    + "Content-Disposition: form-data; name=\"model\"\r\n\r\n"
                    + GROQ_STT_MODEL + "\r\n"
                    + "--" + boundary + "\r\n"
                    + "Content-Disposition: form-data; name=\"response_format\"\r\n\r\n"
                    + "json\r\n"
                    + "--" + boundary + "\r\n"
                    + "Content-Disposition: form-data; name=\"file\"; filename=\"speech.wav\"\r\n"
                    + "Content-Type: audio/wav\r\n\r\n";

            body.write(part1.getBytes(StandardCharsets.UTF_8));
            body.write(wavBytes);
            body.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(GROQ_STT_URL))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .timeout(Duration.ofSeconds(12))
                    .POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray()))
                    .build();

            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (resp.statusCode() != 200) return "";

            return extractJsonField(resp.body(), "text");
        } catch (Exception e) {
            return "";
        }
    }

    private String extractJsonField(String json, String fieldName) {
        String marker = "\"" + fieldName + "\":\"";
        int start = json.indexOf(marker);
        if (start == -1) return "";
        start += marker.length();

        StringBuilder result = new StringBuilder();
        boolean escaped = false;
        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);
            if (escaped) {
                if (c == 'n') result.append('\n');
                else if (c == 't') result.append('\t');
                else if (c == 'r') result.append('\r');
                else result.append(c);
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
            } else if (c == '"') {
                break;
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }

    private boolean isGenerating() {
        AtomicBoolean flag = activeCancelFlag.get();
        return flag != null && !flag.get();
    }

    private void setGeneratingUI(boolean generating) {
        Platform.runLater(() -> {
            if (actionButton == null) return;
            if (generating) {
                actionButton.setText("■");
                actionButton.setStyle(
                        "-fx-background-color: #ef4444; "
                        + "-fx-text-fill: #ffffff; "
                        + "-fx-font-weight: bold; "
                        + "-fx-font-size: 13px; "
                        + "-fx-background-radius: 12px; "
                        + "-fx-cursor: hand;"
                );
            } else {
                actionButton.setText(defaultActionText);
                actionButton.setStyle(defaultActionStyle);
            }
        });
    }

    public void stopActiveGeneration() {
        AtomicBoolean prevCancel = activeCancelFlag.getAndSet(null);
        if (prevCancel != null) {
            prevCancel.set(true);
        }
        InputStream prevStream = activeResponseStream.getAndSet(null);
        if (prevStream != null) {
            try {
                prevStream.close();
            } catch (Exception ignored) {
            }
        }
        setGeneratingUI(false);
    }

    // --- Cross-Platform Neural Text-to-Speech (TTS) ---

    public void stopSpeech() {
        Clip clip = activeAudioClip.getAndSet(null);
        if (clip != null) {
            try {
                clip.stop();
                clip.close();
            } catch (Exception ignored) {
            }
        }
        Process proc = activeTtsProcess.getAndSet(null);
        if (proc != null) {
            try {
                proc.destroyForcibly();
            } catch (Exception ignored) {
            }
        }
        Button prevBtn = activeSpeakButton.getAndSet(null);
        if (prevBtn != null) {
            Platform.runLater(() -> {
                prevBtn.setText("🔊 Listen");
                prevBtn.setStyle(SPEAK_BTN_DEFAULT_STYLE);
            });
        }
    }

    private Button createSpeakButton(AtomicReference<String> textRef) {
        Button speakBtn = new Button("🔊 Listen");
        speakBtn.setStyle(SPEAK_BTN_DEFAULT_STYLE);
        speakBtn.setOnMouseEntered(e -> {
            if (activeSpeakButton.get() != speakBtn) {
                speakBtn.setStyle(SPEAK_BTN_DEFAULT_STYLE + "-fx-background-color: #1f1f2e; -fx-text-fill: #c4c4d8;");
            }
        });
        speakBtn.setOnMouseExited(e -> {
            if (activeSpeakButton.get() != speakBtn) {
                speakBtn.setStyle(SPEAK_BTN_DEFAULT_STYLE);
            }
        });
        speakBtn.setOnAction(e -> {
            if (activeSpeakButton.get() == speakBtn) {
                stopSpeech();
            } else {
                speakText(textRef.get(), speakBtn);
            }
        });
        return speakBtn;
    }

    private String sanitizeForSpeech(String raw) {
        if (raw == null) return "";
        String cleaned = raw
                .replaceAll("```[\\s\\S]*?```", ". Here is the code. ")
                .replaceAll("`([^`]+)`", "$1")
                .replaceAll("[*_#>~]+", "")
                .replaceAll("https?://\\S+", "link")
                .replaceAll("\\s+", " ")
                .trim();
        if (cleaned.length() > 900) {
            cleaned = cleaned.substring(0, 900);
        }
        return cleaned;
    }

    private void speakText(String rawText, Button sourceBtn) {
        stopSpeech();
        String spoken = sanitizeForSpeech(rawText);
        if (spoken.isEmpty() || spoken.startsWith("Error:") || "[Stopped]".equals(spoken)) return;

        if (sourceBtn != null) {
            activeSpeakButton.set(sourceBtn);
            Platform.runLater(() -> {
                sourceBtn.setText("⏹ Stop");
                sourceBtn.setStyle(SPEAK_BTN_ACTIVE_STYLE);
            });
        }

        new Thread(() -> {
            if (tryEdgeNeuralTts(spoken, sourceBtn)) return;
            if (sourceBtn != null && activeSpeakButton.get() != sourceBtn) return;

            if (tryGroqTts(spoken, sourceBtn)) return;
            if (sourceBtn != null && activeSpeakButton.get() != sourceBtn) return;

            playLocalSystemTts(spoken, sourceBtn);
        }, "intenso-tts-thread").start();
    }

    private boolean tryEdgeNeuralTts(String text, Button sourceBtn) {
        try {
            Path tempMp3 = Files.createTempFile("intenso_jenny_", ".mp3");
            String edgeBin = Paths.get(System.getProperty("user.home"), ".local", "bin", "edge-tts").toString();
            if (!Files.exists(Paths.get(edgeBin))) {
                edgeBin = "edge-tts";
            }

            Process gen = new ProcessBuilder(
                    edgeBin,
                    "--voice", EDGE_TTS_VOICE,
                    "--rate=+6%",
                    "--text", text,
                    "--write-media", tempMp3.toAbsolutePath().toString()
            ).start();
            activeTtsProcess.set(gen);
            int code = gen.waitFor();
            activeTtsProcess.compareAndSet(gen, null);

            if (code != 0 || !Files.exists(tempMp3) || Files.size(tempMp3) < 200) {
                Files.deleteIfExists(tempMp3);
                return false;
            }

            if (sourceBtn != null && activeSpeakButton.get() != sourceBtn) {
                Files.deleteIfExists(tempMp3);
                return true;
            }

            Process player = new ProcessBuilder(
                    "ffplay", "-nodisp", "-autoexit", "-loglevel", "quiet",
                    tempMp3.toAbsolutePath().toString()
            ).start();
            activeTtsProcess.set(player);
            player.waitFor();
            activeTtsProcess.compareAndSet(player, null);
            Files.deleteIfExists(tempMp3);
            resetSpeakButton(sourceBtn);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean tryGroqTts(String text, Button sourceBtn) {
        String apiKey = getEffectiveApiKey();
        if (apiKey.isEmpty()) return false;

        try {
            String jsonBody = "{"
                    + "\"model\": \"" + GROQ_TTS_MODEL + "\","
                    + "\"input\": \"" + escapeJson(text) + "\","
                    + "\"voice\": \"" + GROQ_TTS_VOICE + "\","
                    + "\"response_format\": \"wav\""
                    + "}";

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(GROQ_TTS_URL))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .timeout(Duration.ofSeconds(8))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<byte[]> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofByteArray());
            if (resp.statusCode() != 200 || resp.body() == null || resp.body().length < 44) {
                return false;
            }

            if (sourceBtn != null && activeSpeakButton.get() != sourceBtn) {
                return true;
            }

            try (AudioInputStream ais = AudioSystem.getAudioInputStream(new ByteArrayInputStream(resp.body()))) {
                Clip clip = AudioSystem.getClip();
                activeAudioClip.set(clip);
                clip.addLineListener(event -> {
                    if (event.getType() == LineEvent.Type.STOP) {
                        clip.close();
                        activeAudioClip.compareAndSet(clip, null);
                        resetSpeakButton(sourceBtn);
                    }
                });
                clip.open(ais);
                clip.start();
                return true;
            } catch (Exception audioEx) {
                Path tempWav = Files.createTempFile("intenso_tts_", ".wav");
                Files.write(tempWav, resp.body());
                Process p = new ProcessBuilder("aplay", "-q", tempWav.toAbsolutePath().toString()).start();
                activeTtsProcess.set(p);
                p.waitFor();
                activeTtsProcess.compareAndSet(p, null);
                Files.deleteIfExists(tempWav);
                resetSpeakButton(sourceBtn);
                return true;
            }
        } catch (Exception e) {
            return false;
        }
    }

    private void playLocalSystemTts(String text, Button sourceBtn) {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ENGLISH);
        try {
            Process p;
            if (os.contains("win")) {
                String safeText = text.replace("'", "''");
                String psCmd = "Add-Type -AssemblyName System.Speech; "
                        + "$s = New-Object System.Speech.Synthesis.SpeechSynthesizer; "
                        + "$s.SelectVoiceByHints('Female'); "
                        + "$s.Speak('" + safeText + "');";
                p = new ProcessBuilder("powershell", "-NoProfile", "-Command", psCmd).start();
            } else if (os.contains("mac")) {
                p = new ProcessBuilder("say", "-v", "Samantha", text).start();
            } else {
                try {
                    p = new ProcessBuilder("espeak-ng", "-v", "en+f3", "-s", "165", text).start();
                } catch (Exception e) {
                    p = new ProcessBuilder("spd-say", "-w", text).start();
                }
            }
            activeTtsProcess.set(p);
            p.waitFor();
            activeTtsProcess.compareAndSet(p, null);
        } catch (Exception ignored) {
        } finally {
            resetSpeakButton(sourceBtn);
        }
    }

    private void resetSpeakButton(Button sourceBtn) {
        if (sourceBtn != null && activeSpeakButton.compareAndSet(sourceBtn, null)) {
            Platform.runLater(() -> {
                sourceBtn.setText("🔊 Listen");
                sourceBtn.setStyle(SPEAK_BTN_DEFAULT_STYLE);
            });
        }
    }

    private void setupWindowDrag() {
        titleBar.setOnMousePressed(e -> {
            if (isMiniMode) return;
            xOffset = e.getSceneX();
            yOffset = e.getSceneY();
        });
        titleBar.setOnMouseDragged(e -> {
            if (isMiniMode) return;
            Stage stage = (Stage) titleBar.getScene().getWindow();
            if (!stage.isMaximized()) {
                stage.setX(e.getScreenX() - xOffset);
                stage.setY(e.getScreenY() - yOffset);
            }
        });
    }

    public void handleMinimize() {
        ((Stage) titleBar.getScene().getWindow()).setIconified(true);
    }

    public void handleMaximize() {
        if (isMiniMode) {
            toggleMiniOverlayMode();
            return;
        }
        Stage stage = (Stage) titleBar.getScene().getWindow();
        stage.setMaximized(!stage.isMaximized());
    }

    public void handleClose() {
        stopActiveGeneration();
        stopSpeech();
        unloadModelsOnExit();
        Platform.exit();
    }

    private void unloadModelsOnExit() {
        try {
            String body = "{\"model\": \"" + LOCAL_FALLBACK_MODEL + "\", \"keep_alive\": 0}";
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:11434/api/generate"))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(1))
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            httpClient.sendAsync(req, HttpResponse.BodyHandlers.discarding());
        } catch (Exception ignored) {
        }
    }

    private void updateEmptyState() {
        boolean isEmpty = chatContainer.getChildren().isEmpty() && !isMiniMode;
        emptyStateBox.setVisible(isEmpty);
        emptyStateBox.setManaged(isEmpty);
    }

    public void handleNewChat() {
        stopActiveGeneration();
        stopSpeech();
        currentChat = null;
        chatContainer.getChildren().clear();
        highlightActiveChat();
        updateEmptyState();
        inputField.requestFocus();
    }

    public void handleButtonClick() {
        String rawInput = inputField.getText();
        if (rawInput == null || rawInput.isBlank()) return;

        inputField.clear();

        String trimmedCmd = rawInput.trim();
        if ("/key".equalsIgnoreCase(trimmedCmd) || "/settings".equalsIgnoreCase(trimmedCmd)) {
            showApiKeySettingsDialog(false);
            return;
        }
        if ("/mini".equalsIgnoreCase(trimmedCmd) || "/pip".equalsIgnoreCase(trimmedCmd)) {
            toggleMiniOverlayMode();
            return;
        }
        if ("/memory".equalsIgnoreCase(trimmedCmd)) {
            String memList;
            synchronized (this) {
                memList = permanentFacts.isEmpty()
                        ? "No learned facts saved yet. Tell me about yourself or use /remember <fact>."
                        : "Learned facts about you:\n• " + String.join("\n• ", permanentFacts);
            }
            renderBubble(memList, false);
            return;
        }
        if ("/forget".equalsIgnoreCase(trimmedCmd)) {
            synchronized (this) {
                permanentFacts.clear();
                savePermanentMemory();
            }
            renderBubble("Permanent user memory wiped clean.", false);
            return;
        }
        if (trimmedCmd.toLowerCase().startsWith("/remember ")) {
            String fact = trimmedCmd.substring(10).trim();
            if (!fact.isEmpty()) {
                addPermanentFact(fact);
                renderBubble("Saved to permanent memory: " + fact, false);
            }
            return;
        }
        if ("/voice".equalsIgnoreCase(trimmedCmd)) {
            boolean enabled = !autoVoiceEnabled.get();
            autoVoiceEnabled.set(enabled);
            if (!enabled) stopSpeech();
            String notice = enabled ? "Auto-voice enabled. Intenso AI will speak replies aloud." : "Auto-voice muted.";
            renderBubble(notice, false);
            if (enabled) speakText(notice, null);
            return;
        }
        if (trimmedCmd.toLowerCase().startsWith("/say ")) {
            String sayText = trimmedCmd.substring(5).trim();
            if (!sayText.isEmpty()) {
                speakText(sayText, null);
            }
            return;
        }

        Thread previousWorker = activeWorkerThread.get();
        stopActiveGeneration();
        stopSpeech();

        boolean forcePro = rawInput.startsWith("/pro ");
        boolean forceFast = rawInput.startsWith("/fast ");
        boolean forceWeb = rawInput.startsWith("/web ");
        boolean noWeb = rawInput.startsWith("/noweb ");
        boolean speakOnce = rawInput.startsWith("/voice ");

        String userText = rawInput.replaceFirst("^/(pro|fast|web|noweb|voice)\\s+", "").trim();
        List<AttachedFile> pendingFiles;
        synchronized (attachedFiles) {
            pendingFiles = new ArrayList<>(attachedFiles);
            attachedFiles.clear();
        }
        refreshAttachmentPillBar();

        if (userText.isEmpty() && pendingFiles.isEmpty()) return;
        if (userText.isEmpty()) {
            userText = "Analyze the attached file(s) and summarize key details, structure, and any issues.";
        }

        String displayUserText = userText;
        String aiUserText = userText;
        if (!pendingFiles.isEmpty()) {
            StringBuilder badgeSb = new StringBuilder();
            StringBuilder contextSb = new StringBuilder();
            for (AttachedFile af : pendingFiles) {
                badgeSb.append("📎 ").append(af.name).append("   ");
                contextSb.append("\n\n--- ATTACHED FILE: ").append(af.name).append(" ---\n")
                         .append(af.content)
                         .append("\n--- END OF ").append(af.name).append(" ---");
            }
            displayUserText = badgeSb.toString().trim() + "\n" + userText;
            aiUserText = userText + contextSb.toString();
        }
        final String finalAiUserText = aiUserText;
        final String effectiveUserText = userText;

        boolean isBrandNewChat = (currentChat == null);
        if (isBrandNewChat) {
            String initialTitle = buildFallbackTopicTitle(userText);
            currentChat = new ChatSession(initialTitle, true);
            allChats.add(currentChat);
            addChatToSidebar(currentChat);
            highlightActiveChat();
            if (historySearchField != null && !historySearchField.getText().isBlank()) {
                filterSidebarChats(historySearchField.getText());
            }
        }

        ChatSession targetChat = currentChat;

        if (currentChat == targetChat) {
            renderBubble(displayUserText, true);
            updateEmptyState();
        }

        final AtomicBoolean cancelled = new AtomicBoolean(false);
        activeCancelFlag.set(cancelled);
        setGeneratingUI(true);

        boolean useSmartModel = !forceFast && (forcePro || !pendingFiles.isEmpty() || needsSmartModel(userText, targetChat.history));
        String chosenModel = useSmartModel ? GROQ_SMART_MODEL : GROQ_FAST_MODEL;

        boolean needsWeb = userText.toLowerCase().matches(".*\\b(search|web|google|internet|online|lookup|look up|find out|latest|news|today|current|currently|price|weather|score|release|released|launch|launched|date|out yet|gta|2025|2026|who is|what is|when is|when did|did .* come out)\\b.*");
        String memoryContext = buildCrossChatMemory(userText, targetChat);

        VBox aiMessageBox = createMessageContainer(false);
        String initialBadge = needsWeb
                ? "Searching web..."
                : (!memoryContext.isEmpty()
                    ? "Recalling past chats..."
                    : (useSmartModel ? "● ● ●  [120B Pro]" : "● ● ●"));
        Label statusLabel = createTextPartLabel(initialBadge, false);
        aiMessageBox.getChildren().add(statusLabel);

        AtomicReference<String> finalReplyRef = new AtomicReference<>("");
        Button speakBtn = createSpeakButton(finalReplyRef);
        speakBtn.setVisible(false);
        speakBtn.setManaged(false);

        VBox aiOuterColumn = new VBox(4, aiMessageBox, speakBtn);
        aiOuterColumn.setAlignment(Pos.CENTER_LEFT);
        HBox aiRow = new HBox(aiOuterColumn);
        aiRow.setAlignment(Pos.CENTER_LEFT);

        if (currentChat == targetChat) {
            chatContainer.getChildren().add(aiRow);
            scrollToBottom();
        }

        StringBuffer incomingBuffer = new StringBuffer();
        AtomicBoolean streamFinished = new AtomicBoolean(false);
        List<Node> liveNodes = new ArrayList<>();
        List<LiveCodeCard> liveCodeCards = new ArrayList<>();

        AnimationTimer smoothTyper = new AnimationTimer() {
            private int displayedLength = 0;
            private long lastFrameNanos = 0;
            private int lastLineCount = 0;

            @Override
            public void handle(long now) {
                if (now - lastFrameNanos < 16_000_000L) return;
                lastFrameNanos = now;

                if (cancelled.get()) {
                    if (currentChat == targetChat) {
                        if (displayedLength > 0) {
                            String partial = incomingBuffer.substring(0, displayedLength);
                            renderLiveStream(aiMessageBox, liveNodes, liveCodeCards, partial, false, true);
                            finalReplyRef.set(partial);
                            speakBtn.setVisible(true);
                            speakBtn.setManaged(true);
                        } else {
                            statusLabel.setText("[Stopped]");
                        }
                    }
                    stop();
                    return;
                }

                int totalAvailable = incomingBuffer.length();
                int backlog = totalAvailable - displayedLength;

                if (backlog > 0) {
                    int baseStep = backlog > 400 ? 9 : (backlog > 120 ? 6 : 3);
                    int nextPos = Math.min(totalAvailable, displayedLength + baseStep);

                    int lookaheadLimit = Math.min(totalAvailable, nextPos + 5);
                    for (int i = nextPos; i < lookaheadLimit; i++) {
                        char c = incomingBuffer.charAt(i);
                        if (c == ' ' || c == '\n' || c == '(' || c == '{' || c == ';') {
                            nextPos = i + 1;
                            break;
                        }
                    }

                    displayedLength = nextPos;

                    if (currentChat == targetChat) {
                        String currentText = incomingBuffer.substring(0, displayedLength);
                        boolean done = streamFinished.get() && displayedLength == totalAvailable;
                        renderLiveStream(aiMessageBox, liveNodes, liveCodeCards, currentText, !done, true);

                        int lineCount = countNewlines(currentText);
                        if (lineCount != lastLineCount || done) {
                            lastLineCount = lineCount;
                            scrollToBottom();
                        }
                    }
                } else if (streamFinished.get()) {
                    if (currentChat == targetChat && totalAvailable > 0) {
                        String fullText = incomingBuffer.toString();
                        renderLiveStream(aiMessageBox, liveNodes, liveCodeCards, fullText, false, true);
                        finalReplyRef.set(fullText);
                        if (!fullText.startsWith("Error:")) {
                            speakBtn.setVisible(true);
                            speakBtn.setManaged(true);
                        }
                        scrollToBottom();
                    }
                    stop();
                }
            }
        };
        smoothTyper.start();

        Thread worker = new Thread(() -> {
            if (previousWorker != null && previousWorker.isAlive()) {
                try {
                    previousWorker.join(600);
                } catch (InterruptedException ignored) {
                }
            }

            if (cancelled.get()) {
                streamFinished.set(true);
                return;
            }

            List<ChatMessage> historySnapshot;
            synchronized (targetChat.history) {
                targetChat.history.add(new ChatMessage("user", finalAiUserText));
                historySnapshot = new ArrayList<>(targetChat.history);
            }
            saveChatsToDisk();

            String webContext = needsWeb ? fetchWebContext(buildSearchQuery(effectiveUserText)) : "";
            if (cancelled.get()) {
                synchronized (targetChat.history) {
                    targetChat.history.add(new ChatMessage("assistant", "[Stopped]"));
                }
                saveChatsToDisk();
                streamFinished.set(true);
                return;
            }

            if (needsWeb || !memoryContext.isEmpty()) {
                Platform.runLater(() -> {
                    if (!cancelled.get() && currentChat == targetChat && incomingBuffer.length() == 0) {
                        statusLabel.setText(useSmartModel ? "● ● ●  [120B Pro]" : "● ● ●");
                    }
                });
            }

            String fullReply = streamAI(
                    chosenModel,
                    historySnapshot,
                    webContext,
                    memoryContext,
                    incomingBuffer,
                    streamFinished,
                    cancelled,
                    statusLabel,
                    targetChat
            );

            String savedReply = fullReply.isBlank() ? "[Stopped]" : fullReply;
            synchronized (targetChat.history) {
                targetChat.history.add(new ChatMessage("assistant", savedReply));
            }
            saveChatsToDisk();

            if (!cancelled.get() && !fullReply.isBlank() && !fullReply.startsWith("Error:")) {
                extractAndSaveUserFacts(finalAiUserText, fullReply);
            }

            if (!cancelled.get() && !fullReply.isBlank() && !fullReply.startsWith("Error:")
                    && (autoVoiceEnabled.get() || speakOnce)) {
                speakText(fullReply, speakBtn);
            }

            if (targetChat.titleUpdatesCount < 2 && !fullReply.isBlank() && !fullReply.startsWith("Error:")) {
                targetChat.titleUpdatesCount++;
                generateSmartChatTitle(targetChat);
            }
        });
        activeWorkerThread.set(worker);
        worker.start();
    }

    private String buildFallbackTopicTitle(String firstMessage) {
        String cleaned = firstMessage
                .replaceAll("(?i)^(yo+|hey+|hi+|hello+|bro+|man+|lets play|let's play|can we play|can you|could you|please|pls|how do i|how to|what is|whats|who is|whos|write a|make a|create a)\\b[\\s,!?]*", "")
                .replaceAll("\\bn\\b", "and")
                .replaceAll("[^a-zA-Z0-9\\s+#.-]", " ")
                .replaceAll("\\s+", " ")
                .trim();

        if (cleaned.isEmpty()) cleaned = firstMessage.trim();
        String[] words = cleaned.split("\\s+");
        StringBuilder sb = new StringBuilder();
        int count = Math.min(words.length, 4);
        for (int i = 0; i < count; i++) {
            String w = words[i];
            if (w.isEmpty()) continue;
            if (sb.length() > 0) sb.append(" ");
            sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1));
        }
        String res = sb.toString();
        if (res.length() > 28) res = res.substring(0, 28).trim();
        return res.isEmpty() ? "New Chat" : res;
    }

    private void generateSmartChatTitle(ChatSession session) {
        String apiKey = getEffectiveApiKey();
        if (apiKey.isEmpty()) return;

        StringBuilder transcript = new StringBuilder();
        synchronized (session.history) {
            int added = 0;
            for (ChatMessage m : session.history) {
                if ("system".equals(m.role)) continue;
                String snippet = m.content.replaceAll("```[\\s\\S]*?```", "[code]").replaceAll("\\s+", " ").trim();
                if (snippet.length() > 180) snippet = snippet.substring(0, 180) + "...";
                transcript.append("user".equals(m.role) ? "User: " : "AI: ").append(snippet).append("\n");
                if (++added >= 4) break;
            }
        }
        if (transcript.length() == 0) return;

        String prompt = "Read this conversation and output a short, clear 2-to-4-word topic title capturing the main idea or activity "
                + "(for example: 'Rock Paper Scissors', 'JavaFX Title Bar', 'Groq API Setup'). "
                + "Output ONLY the 2-4 word title—no quotes, no colons, no extra text.\n\n"
                + transcript;

        for (String titleModel : List.of("qwen/qwen3.8-27b", "openai/gpt-oss-20b", "openai/gpt-oss-120b")) {
            try {
                String jsonBody = "{"
                        + "\"model\": \"" + titleModel + "\","
                        + "\"messages\": [{\"role\": \"user\", \"content\": \"" + escapeJson(prompt) + "\"}],"
                        + "\"stream\": false,"
                        + "\"reasoning_effort\": \"low\","
                        + "\"temperature\": 0.25,"
                        + "\"max_tokens\": 160"
                        + "}";

                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(GROQ_API_URL))
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer " + apiKey)
                        .timeout(Duration.ofSeconds(5))
                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                        .build();

                HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                if (resp.statusCode() != 200) continue;

                String rawTitle = extractChatResponseText(resp.body())
                        .replaceAll("[\"'`\\r\\n]", " ")
                        .replaceAll("^(?i)(title|topic)\\s*:\\s*", "")
                        .replaceAll("[.!?:]+$", "")
                        .replaceAll("\\s+", " ")
                        .trim();

                if (!rawTitle.isEmpty() && rawTitle.length() <= 36) {
                    if (!rawTitle.contains(" ") && session.title.contains(" ")) {
                        continue;
                    }
                    session.title = rawTitle;
                    saveChatsToDisk();
                    Platform.runLater(() -> {
                        if (session.sidebarButton != null) {
                            session.sidebarButton.setText(rawTitle);
                        }
                        if (historySearchField != null && !historySearchField.getText().isBlank()) {
                            filterSidebarChats(historySearchField.getText());
                        }
                    });
                    return;
                }
            } catch (Exception ignored) {
            }
        }
    }

    private String buildCrossChatMemory(String userText, ChatSession currentSession) {
        if (allChats.size() <= 1) return "";

        String lower = userText.toLowerCase();
        boolean asksLastChat = lower.matches(
                ".*\\b(last chat|previous chat|other chat|past chat|before this|what did we talk|what were we talking|what did i say|what happened in|summarize.*chat|remember.*chat)\\b.*"
        );
        boolean explicitRecall = asksLastChat || lower.matches(
                ".*\\b(remember|recall|earlier|before|last time|told you|said earlier|we discussed|my name|who am i|know me|what did i|what was my)\\b.*"
        );

        Set<String> keywords = extractKeywords(lower);
        if (!explicitRecall && keywords.isEmpty()) return "";

        if (asksLastChat || (explicitRecall && keywords.isEmpty())) {
            for (int idx = allChats.size() - 1; idx >= 0; idx--) {
                ChatSession candidate = allChats.get(idx);
                if (candidate == currentSession) continue;

                List<ChatMessage> validMsgs = new ArrayList<>();
                synchronized (candidate.history) {
                    for (ChatMessage m : candidate.history) {
                        if ("system".equals(m.role) || m.content.startsWith("Error:") || "[Stopped]".equals(m.content)) {
                            continue;
                        }
                        validMsgs.add(m);
                    }
                }
                if (validMsgs.isEmpty()) continue;

                int start = Math.max(0, validMsgs.size() - 6);
                StringBuilder sb = new StringBuilder();
                sb.append("Most recent past chat (\"").append(candidate.title).append("\"):\n");
                for (int i = start; i < validMsgs.size(); i++) {
                    ChatMessage m = validMsgs.get(i);
                    String clean = m.content.replaceAll("```[\\s\\S]*?```", "[code block]").replaceAll("\\s+", " ").trim();
                    if (clean.length() > 200) clean = clean.substring(0, 200) + "...";
                    String speaker = "user".equals(m.role) ? "User" : "Intenso AI";
                    sb.append("- ").append(speaker).append(": ").append(clean).append("\n");
                }
                return sb.toString();
            }
        }

        List<MemoryMatch> matches = new ArrayList<>();
        for (int idx = allChats.size() - 1; idx >= 0; idx--) {
            ChatSession session = allChats.get(idx);
            if (session == currentSession) continue;

            List<ChatMessage> msgsCopy;
            synchronized (session.history) {
                msgsCopy = new ArrayList<>(session.history);
            }

            boolean titleMatch = false;
            for (String kw : keywords) {
                if (session.title.toLowerCase().contains(kw)) {
                    titleMatch = true;
                    break;
                }
            }

            for (int i = 0; i < msgsCopy.size(); i++) {
                ChatMessage m = msgsCopy.get(i);
                if ("system".equals(m.role) || m.content.startsWith("Error:") || "[Stopped]".equals(m.content)) {
                    continue;
                }
                String msgLower = m.content.toLowerCase();
                int score = titleMatch ? 1 : 0;
                for (String kw : keywords) {
                    if (msgLower.contains(kw)) {
                        score += 2;
                    }
                }

                if (score > 0) {
                    String clean = m.content.replaceAll("```[\\s\\S]*?```", "[code block]").replaceAll("\\s+", " ").trim();
                    if (clean.length() > 190) clean = clean.substring(0, 190) + "...";
                    String speaker = "user".equals(m.role) ? "User" : "Intenso AI";
                    matches.add(new MemoryMatch(session.title, speaker + ": " + clean, score));
                }
            }
        }

        if (matches.isEmpty()) return "";

        matches.sort(Comparator.comparingInt((MemoryMatch m) -> m.score).reversed());
        StringBuilder sb = new StringBuilder();
        int limit = Math.min(4, matches.size());
        for (int i = 0; i < limit; i++) {
            MemoryMatch mm = matches.get(i);
            sb.append("- [Chat: \"").append(mm.chatTitle).append("\"] ").append(mm.snippet).append("\n");
        }
        return sb.toString();
    }

    private Set<String> extractKeywords(String lowerText) {
        Set<String> keys = new HashSet<>();
        for (String token : lowerText.split("[^a-z0-9_+#.-]+")) {
            if (token.length() >= 3 && !STOP_WORDS.contains(token)) {
                keys.add(token);
            }
        }
        return keys;
    }

    private int countNewlines(String s) {
        int count = 0;
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == '\n') count++;
        }
        return count;
    }

    private boolean needsSmartModel(String text, List<ChatMessage> history) {
        String q = text.toLowerCase().trim();
        if (q.length() > 110) return true;
        if (q.matches(".*\\b(code|python|java|javascript|typescript|c\\+\\+|rust|sql|html|css|script|function|class|bug|error|exception|fix|debug|refactor|algorithm|regex|api|database|math|solve|equation|prove|explain|why|how|architecture|compare|write)\\b.*")) {
            return true;
        }
        synchronized (history) {
            if (history.size() >= 2 && q.matches(".*\\b(it|that|this|above|modify|change|add|update|rewrite|convert)\\b.*")) {
                ChatMessage prev = history.get(history.size() - 1);
                if ("assistant".equals(prev.role) && prev.content.contains("```")) {
                    return true;
                }
            }
        }
        return false;
    }

    private void renderLiveStream(
            VBox container,
            List<Node> liveNodes,
            List<LiveCodeCard> liveCodeCards,
            String text,
            boolean showCursor,
            boolean animateCards
    ) {
        List<MessageSegment> segments = parseSegments(text, showCursor);
        if (segments.isEmpty()) return;

        boolean structureChanged = liveNodes.size() != segments.size();
        if (!structureChanged) {
            for (int i = 0; i < segments.size(); i++) {
                boolean nodeIsCode = liveNodes.get(i) instanceof VBox;
                if (segments.get(i).isCode != nodeIsCode) {
                    structureChanged = true;
                    break;
                }
            }
        }

        if (structureChanged) {
            int existingCount = liveNodes.size();
            boolean isPureAppend = existingCount > 0 && segments.size() > existingCount;
            if (isPureAppend) {
                for (int i = 0; i < existingCount; i++) {
                    boolean nodeIsCode = liveNodes.get(i) instanceof VBox;
                    if (segments.get(i).isCode != nodeIsCode) {
                        isPureAppend = false;
                        break;
                    }
                }
            }

            if (isPureAppend) {
                for (int i = 0; i < existingCount; i++) {
                    MessageSegment seg = segments.get(i);
                    if (seg.isCode) {
                        liveCodeCards.get(i).update(seg.language, seg.content);
                    } else {
                        ((Label) liveNodes.get(i)).setText(seg.content);
                    }
                }
                for (int i = existingCount; i < segments.size(); i++) {
                    MessageSegment seg = segments.get(i);
                    if (seg.isCode) {
                        LiveCodeCard card = new LiveCodeCard(seg.language, seg.content, animateCards);
                        liveCodeCards.add(card);
                        liveNodes.add(card.root);
                        container.getChildren().add(card.root);
                    } else {
                        Label lbl = createTextPartLabel(seg.content, false);
                        liveCodeCards.add(null);
                        liveNodes.add(lbl);
                        container.getChildren().add(lbl);
                    }
                }
            } else {
                container.getChildren().clear();
                liveNodes.clear();
                liveCodeCards.clear();

                for (MessageSegment seg : segments) {
                    if (seg.isCode) {
                        LiveCodeCard card = new LiveCodeCard(seg.language, seg.content, animateCards);
                        liveCodeCards.add(card);
                        liveNodes.add(card.root);
                        container.getChildren().add(card.root);
                    } else {
                        Label lbl = createTextPartLabel(seg.content, false);
                        liveCodeCards.add(null);
                        liveNodes.add(lbl);
                        container.getChildren().add(lbl);
                    }
                }
            }
        } else {
            for (int i = 0; i < segments.size(); i++) {
                MessageSegment seg = segments.get(i);
                if (seg.isCode) {
                    liveCodeCards.get(i).update(seg.language, seg.content);
                } else {
                    ((Label) liveNodes.get(i)).setText(seg.content);
                }
            }
        }
    }

    private List<MessageSegment> parseSegments(String rawText, boolean showCursor) {
        List<MessageSegment> segments = new ArrayList<>();

        String working = rawText.replaceFirst("(?s)```[a-zA-Z0-9_+-]*$", "");

        Pattern codeBlockPattern = Pattern.compile("```([a-zA-Z0-9_+-]*)[ \\t]*\\R([\\s\\S]*?)(?:```|$)");
        Matcher matcher = codeBlockPattern.matcher(working);

        int lastEnd = 0;
        boolean lastSegmentClosed = true;

        while (matcher.find()) {
            String before = working.substring(lastEnd, matcher.start()).trim();
            if (!before.isEmpty()) {
                segments.add(new MessageSegment(false, null, before));
            }

            String lang = matcher.group(1).isEmpty() ? "code" : matcher.group(1).toLowerCase();
            String code = matcher.group(2);
            boolean isClosed = matcher.group(0).endsWith("```");
            lastSegmentClosed = isClosed;

            if (isClosed) {
                code = code.replaceAll("\\s+$", "");
            }
            segments.add(new MessageSegment(true, lang, code));
            lastEnd = matcher.end();
        }

        String remaining = working.substring(lastEnd).trim();
        if (!remaining.isEmpty()) {
            segments.add(new MessageSegment(false, null, remaining));
            lastSegmentClosed = true;
        }

        if (showCursor && !segments.isEmpty()) {
            MessageSegment last = segments.get(segments.size() - 1);
            String withCursor = last.content + " ▍";
            segments.set(segments.size() - 1, new MessageSegment(last.isCode, last.language, withCursor));
        } else if (!showCursor && !lastSegmentClosed && !segments.isEmpty()) {
            MessageSegment last = segments.get(segments.size() - 1);
            if (last.isCode) {
                segments.set(segments.size() - 1, new MessageSegment(true, last.language, last.content.replaceAll("\\s+$", "")));
            }
        }

        return segments;
    }

    private boolean shouldSearchWeb(String text) {
        String q = text.toLowerCase().trim();
        if (q.length() < 4) return false;
        if (q.matches("^(yo|hi|hey|sup|hello|thanks|thank you|ok|okay|nice|cool|bro|wyd)[!.?]*$")) return false;
        if (q.matches(".*\\b(what time|what day|what date|todays date|today's date|current time)\\b.*")) return false;
        if (q.contains("java ") || q.contains("python") || q.contains("code") || q.contains("function") || q.contains("error:")) return false;

        return q.contains("2024") || q.contains("2025") || q.contains("2026") || q.contains("2027")
                || q.contains("latest") || q.contains("recent") || q.contains("current")
                || q.contains("today") || q.contains("news") || q.contains("price")
                || q.contains("weather") || q.contains("score") || q.contains("who won")
                || q.contains("release") || q.contains("releasing") || q.contains("coming out")
                || q.contains("when is") || q.contains("when's") || q.contains("when does")
                || q.contains("who is") || q.contains("who's") || q.contains("whos")
                || q.contains("what happened") || q.contains("update") || q.contains("gta");
    }

    private String buildSearchQuery(String raw) {
        String cleaned = raw.toLowerCase()
                .replaceAll("\\b(bro|yo|man|fam|pls|please|tell me|can you|do you know|fuckin|fucking|shit)\\b", " ")
                .replaceAll("\\brn\\b", "2026")
                .replaceAll("\\bwhos\\b", "who is")
                .replaceAll("\\bwhats\\b", "what is")
                .replaceAll("\\bwhens\\b", "when is")
                .replaceAll("\\s+", " ")
                .trim();
        return cleaned.isEmpty() ? raw : cleaned;
    }

    private String fetchWebContext(String query) {
        StringBuilder results = new StringBuilder();
        try {
            String formBody = "q=" + URLEncoder.encode(query, StandardCharsets.UTF_8) + "&kl=us-en";
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("https://html.duckduckgo.com/html/"))
                    .header("User-Agent", "Mozilla/5.0 (X11; Linux x86_64; rv:131.0) Gecko/20100101 Firefox/131.0")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .header("Accept-Language", "en-US,en;q=0.9")
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .header("Referer", "https://html.duckduckgo.com/")
                    .timeout(Duration.ofSeconds(6))
                    .POST(HttpRequest.BodyPublishers.ofString(formBody))
                    .build();

            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (resp.statusCode() == 200) {
                String html = resp.body();
                Pattern titlePattern = Pattern.compile("class=\"result__a\"[^>]*>(.*?)</a>", Pattern.DOTALL);
                Pattern snippetPattern = Pattern.compile("class=\"result__snippet\"[^>]*>(.*?)</(?:a|div|span)>", Pattern.DOTALL);
                Matcher titleMatcher = titlePattern.matcher(html);
                Matcher snippetMatcher = snippetPattern.matcher(html);

                int count = 0;
                while (snippetMatcher.find() && count < 5) {
                    String title = titleMatcher.find() ? stripHtml(titleMatcher.group(1)) : "";
                    String snippet = stripHtml(snippetMatcher.group(1));
                    if (!snippet.isEmpty()) {
                        count++;
                        results.append(count).append(". ");
                        if (!title.isEmpty()) results.append("[").append(title).append("] ");
                        results.append(snippet).append("\n");
                    }
                }
            }
        } catch (Exception ignored) {
        }

        if (results.length() == 0) {
            try {
                String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
                HttpRequest wikiReq = HttpRequest.newBuilder()
                        .uri(URI.create("https://en.wikipedia.org/w/api.php?action=query&list=search&format=json&utf8=1&srlimit=4&srsearch=" + encoded))
                        .header("User-Agent", "IntensoAI/1.0 (Desktop Assistant)")
                        .timeout(Duration.ofSeconds(5))
                        .GET()
                        .build();

                HttpResponse<String> wikiResp = httpClient.send(wikiReq, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                if (wikiResp.statusCode() == 200) {
                    String json = wikiResp.body();
                    Pattern wpTitle = Pattern.compile("\"title\":\"(.*?)\"");
                    Pattern wpSnip = Pattern.compile("\"snippet\":\"(.*?)\"");
                    Matcher tm = wpTitle.matcher(json);
                    Matcher sm = wpSnip.matcher(json);
                    int count = 0;
                    while (tm.find() && sm.find() && count < 4) {
                        String t = stripHtml(tm.group(1).replace("\\u003C", "<").replace("\\u003E", ">").replace("\\\"", "\""));
                        String s = stripHtml(sm.group(1).replace("\\u003C", "<").replace("\\u003E", ">").replace("\\\"", "\""));
                        if (!s.isEmpty()) {
                            count++;
                            results.append(count).append(". [Wikipedia: ").append(t).append("] ").append(s).append("\n");
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }

        return results.toString();
    }

    private String stripHtml(String s) {
        return s.replaceAll("<[^>]+>", "")
                .replace("&quot;", "\"")
                .replace("&amp;", "&")
                .replace("&#x27;", "'")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String streamAI(
            String modelName,
            List<ChatMessage> allMessages,
            String webContext,
            String memoryContext,
            StringBuffer incomingBuffer,
            AtomicBoolean streamFinished,
            AtomicBoolean cancelled,
            Label aiLabel,
            ChatSession targetChat
    ) {
        String apiKey = getEffectiveApiKey();
        boolean hasCloudKey = !apiKey.isEmpty();

        try {
            List<ChatMessage> recent = new ArrayList<>();
            String permanentBlock = formatPermanentMemoryBlock();
            String fullSystemPrompt = permanentBlock.isEmpty()
                    ? INTENSO_SYSTEM_PROMPT
                    : INTENSO_SYSTEM_PROMPT + "\n\n" + permanentBlock;
            recent.add(new ChatMessage("system", fullSystemPrompt));

            int startIdx = Math.max(1, allMessages.size() - 6);
            String nowStamp = LocalDateTime.now().format(CLOCK_FORMAT);

            for (int i = startIdx; i < allMessages.size(); i++) {
                ChatMessage msg = allMessages.get(i);
                if ("system".equals(msg.role)) continue;
                String content = msg.content;
                if (i < allMessages.size() - 1 && content.length() > 1200) {
                    content = content.substring(0, 1200) + "\n...[truncated]";
                }
                boolean isLast = (i == allMessages.size() - 1);
                boolean needsTime = content.toLowerCase().matches(".*\\b(time|date|day|today|tomorrow|year|now)\\b.*");

                if (isLast && (!webContext.isEmpty() || !memoryContext.isEmpty() || needsTime)) {
                    StringBuilder augmented = new StringBuilder();
                    if (needsTime || !webContext.isEmpty()) {
                        augmented.append("[Current time: ").append(nowStamp).append("]\n");
                    }
                    if (!memoryContext.isEmpty()) {
                        augmented.append("[Past chat memory]\n").append(memoryContext).append("\n");
                    }
                    if (!webContext.isEmpty()) {
                        augmented.append("[Web search results]\n").append(webContext).append("\n");
                    }
                    augmented.append(content);
                    recent.add(new ChatMessage("user", augmented.toString()));
                } else {
                    recent.add(new ChatMessage(msg.role, content));
                }
            }

            StringBuilder messagesJson = new StringBuilder("[");
            for (int i = 0; i < recent.size(); i++) {
                ChatMessage m = recent.get(i);
                messagesJson.append("{")
                        .append("\"role\":\"").append(m.role).append("\",")
                        .append("\"content\":\"").append(escapeJson(m.content)).append("\"")
                        .append("}");
                if (i < recent.size() - 1) messagesJson.append(",");
            }
            messagesJson.append("]");

            List<String> candidates = new ArrayList<>();
            if (hasCloudKey) {
                candidates.add(modelName);
                for (String backup : List.of("openai/gpt-oss-20b", "openai/gpt-oss-120b", "qwen/qwen3.8-27b")) {
                    if (!candidates.contains(backup)) candidates.add(backup);
                }
            }
            candidates.add("LOCAL:" + LOCAL_FALLBACK_MODEL);

            HttpResponse<InputStream> response = null;
            boolean usingCloud = false;
            String lastError = "";

            for (String candidate : candidates) {
                if (cancelled.get()) return "";

                boolean isLocal = candidate.startsWith("LOCAL:");
                String actualModel = isLocal ? candidate.substring(6) : candidate;
                int maxTokens = actualModel.equals(GROQ_FAST_MODEL) ? 8192 : 16384;

                HttpRequest request;
                if (!isLocal) {
                    String jsonBody = "{"
                            + "\"model\": \"" + actualModel + "\","
                            + "\"messages\": " + messagesJson + ","
                            + "\"stream\": true,"
                            + "\"reasoning_effort\": \"low\","
                            + "\"temperature\": 0.45,"
                            + "\"max_tokens\": " + maxTokens
                            + "}";

                    request = HttpRequest.newBuilder()
                            .uri(URI.create(GROQ_API_URL))
                            .header("Content-Type", "application/json")
                            .header("Authorization", "Bearer " + apiKey)
                            .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                            .build();
                } else {
                    String jsonBody = "{"
                            + "\"model\": \"" + actualModel + "\","
                            + "\"messages\": " + messagesJson + ","
                            + "\"stream\": true,"
                            + "\"keep_alive\": \"90s\","
                            + "\"options\": {\"num_ctx\": 2048, \"num_batch\": 128, \"num_thread\": 4}"
                            + "}";

                    request = HttpRequest.newBuilder()
                            .uri(URI.create("http://localhost:11434/api/chat"))
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                            .build();
                }

                try {
                    HttpResponse<InputStream> resp = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
                    if (resp.statusCode() == 200) {
                        response = resp;
                        usingCloud = !isLocal;
                        break;
                    } else {
                        String errBody = new String(resp.body().readAllBytes(), StandardCharsets.UTF_8);
                        lastError = "HTTP " + resp.statusCode() + ": " + errBody;
                        if (resp.statusCode() == 429 || resp.statusCode() == 404 || resp.statusCode() >= 500) {
                            continue;
                        }
                        throw new RuntimeException(lastError);
                    }
                } catch (Exception ex) {
                    lastError = ex.getMessage();
                }
            }

            if (response == null) {
                throw new RuntimeException(lastError.isEmpty()
                        ? "No API key configured (click ⚙ in top bar) and local Ollama is not running."
                        : lastError);
            }

            InputStream bodyStream = response.body();
            activeResponseStream.set(bodyStream);

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(bodyStream, StandardCharsets.UTF_8))) {
                String line;
                while (!cancelled.get() && (line = reader.readLine()) != null) {
                    if (usingCloud) {
                        if (!line.startsWith("data: ") || line.equals("data: [DONE]")) continue;
                        line = line.substring(6);
                    }
                    String chunk = extractChatResponseText(line);
                    if (!chunk.isEmpty()) {
                        incomingBuffer.append(chunk);
                    }
                }
            }
        } catch (Exception e) {
            if (!cancelled.get()) {
                String err = "Error: " + e.getMessage();
                incomingBuffer.append(err);
                Platform.runLater(() -> {
                    if (currentChat == targetChat) aiLabel.setText(err);
                });
            }
        } finally {
            activeResponseStream.compareAndSet(activeResponseStream.get(), null);
            if (activeCancelFlag.compareAndSet(cancelled, null)) {
                setGeneratingUI(false);
            }
            streamFinished.set(true);
        }
        return incomingBuffer.toString();
    }

    private synchronized void saveChatsToDisk() {
        try {
            Files.createDirectories(APP_DIR);
            List<String> lines = new ArrayList<>();
            for (ChatSession session : allChats) {
                lines.add("CHAT|" + encode(session.title));
                synchronized (session.history) {
                    for (ChatMessage msg : session.history) {
                        lines.add("MSG|" + msg.role + "|" + encode(msg.content));
                    }
                }
            }
            Path tmp = SAVE_FILE.resolveSibling("saved_chats.db.tmp");
            Files.write(tmp, lines, StandardCharsets.UTF_8);
            Files.move(tmp, SAVE_FILE, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadChatsFromDisk() {
        if (!Files.exists(SAVE_FILE)) return;
        try {
            List<String> lines = Files.readAllLines(SAVE_FILE, StandardCharsets.UTF_8);
            ChatSession loadedSession = null;

            for (String line : lines) {
                if (line.startsWith("CHAT|")) {
                    String title = decode(line.substring(5));
                    loadedSession = new ChatSession(title, false);
                    loadedSession.titleUpdatesCount = 2;
                    allChats.add(loadedSession);
                    addChatToSidebar(loadedSession);
                } else if (line.startsWith("MSG|") && loadedSession != null) {
                    String[] parts = line.split("\\|", 3);
                    if (parts.length == 3) {
                        loadedSession.history.add(new ChatMessage(parts[1], decode(parts[2])));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String encode(String text) {
        return Base64.getEncoder().encodeToString(text.getBytes(StandardCharsets.UTF_8));
    }

    private String decode(String base64) {
        return new String(Base64.getDecoder().decode(base64), StandardCharsets.UTF_8);
    }

    private void addChatToSidebar(ChatSession session) {
        Button chatBtn = new Button(session.title);
        chatBtn.setMaxWidth(Double.MAX_VALUE);
        chatBtn.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(chatBtn, Priority.ALWAYS);
        chatBtn.setOnAction(e -> switchToChat(session));

        Button deleteBtn = new Button("×");
        deleteBtn.getStyleClass().add("deleteChatButton");
        deleteBtn.setOnAction(e -> deleteChat(session));

        HBox row = new HBox(4.0, chatBtn, deleteBtn);
        row.setAlignment(Pos.CENTER_LEFT);

        session.sidebarButton = chatBtn;
        session.sidebarRow = row;

        historyContainer.getChildren().add(0, row);
    }

    private void deleteChat(ChatSession session) {
        if (currentChat == session) {
            stopActiveGeneration();
            stopSpeech();
        }
        allChats.remove(session);
        historyContainer.getChildren().remove(session.sidebarRow);
        saveChatsToDisk();

        if (currentChat == session) {
            if (!allChats.isEmpty()) {
                switchToChat(allChats.get(allChats.size() - 1));
            } else {
                handleNewChat();
            }
        }
    }

    private void switchToChat(ChatSession session) {
        stopActiveGeneration();
        stopSpeech();
        currentChat = session;
        chatContainer.getChildren().clear();

        synchronized (session.history) {
            for (ChatMessage msg : session.history) {
                if ("user".equals(msg.role)) {
                    renderBubble(msg.content, true);
                } else if ("assistant".equals(msg.role)) {
                    renderBubble(msg.content, false);
                }
            }
        }
        highlightActiveChat();
        updateEmptyState();
    }

    private void highlightActiveChat() {
        for (ChatSession chat : allChats) {
            if (chat.sidebarButton == null) continue;
            if (chat == currentChat) {
                chat.sidebarButton.setStyle("-fx-background-color: #1f1f2b; -fx-text-fill: #f4f4f5; -fx-font-weight: bold; -fx-font-size: 13px; -fx-background-radius: 8px; -fx-padding: 8px 10px; -fx-cursor: hand;");
            } else {
                chat.sidebarButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #8e8ea0; -fx-font-size: 13px; -fx-background-radius: 8px; -fx-padding: 8px 10px; -fx-cursor: hand;");
            }
        }
    }

    private VBox createMessageContainer(boolean isUser) {
        VBox box = new VBox(10);
        box.setMaxWidth(isMiniMode ? (MINI_WIDTH - 64) : 660);
        box.getStyleClass().add(isUser ? "userBubble" : "aiBubble");
        return box;
    }

    private HBox wrapMessageBox(VBox messageBox, boolean isUser) {
        HBox row = new HBox(messageBox);
        row.setAlignment(isUser ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        return row;
    }

    private Label createTextPartLabel(String text, boolean isUser) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.setMaxWidth(isMiniMode ? (MINI_WIDTH - 88) : 636);
        label.getStyleClass().add(isUser ? "userText" : "aiText");
        return label;
    }

    private void renderBubble(String text, boolean isUser) {
        VBox messageBox = createMessageContainer(isUser);
        if (isUser || !text.contains("```")) {
            messageBox.getChildren().add(createTextPartLabel(text.trim(), isUser));
        } else {
            renderLiveStream(messageBox, new ArrayList<>(), new ArrayList<>(), text, false, false);
        }

        if (isUser) {
            chatContainer.getChildren().add(wrapMessageBox(messageBox, true));
        } else {
            VBox outer = new VBox(4, messageBox);
            outer.setAlignment(Pos.CENTER_LEFT);
            if (!text.startsWith("Error:") && !"[Stopped]".equals(text)) {
                outer.getChildren().add(createSpeakButton(new AtomicReference<>(text)));
            }
            HBox row = new HBox(outer);
            row.setAlignment(Pos.CENTER_LEFT);
            chatContainer.getChildren().add(row);
        }
        scrollToBottom();
    }

    private void scrollToBottom() {
        Platform.runLater(() -> chatScrollPane.setVvalue(1.0));
    }

    private String escapeJson(String t){if(t==null)return "";StringBuilder s=new StringBuilder();for(char c:t.toCharArray()){if(c==92)s.append("\\\\");else if(c==34)s.append("\\\"");else if(c==10)s.append("\\n");else if(c==13)s.append("\\r");else if(c==9)s.append("\\t");else if(c<32)s.append(String.format("\\u%04x",(int)c));else s.append(c);}return s.toString();}
    private String extractChatResponseText(String j){return extractJsonField(j,"content");}
}
