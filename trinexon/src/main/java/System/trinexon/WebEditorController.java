package System.trinexon;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.json.*;

public class WebEditorController {

    @FXML private TextArea htmlEditor;
    @FXML private WebView webPreview;
    @FXML private ListView<String> fileListView;
    @FXML private ComboBox<String> templateComboBox;
    @FXML private ComboBox<String> cssComboBox;
    @FXML private TextField searchField;
    @FXML private TextField aiPromptField;

    private final Path htmlDir = Path.of("src/main/resources/html");
    private final Path templatesDir = Path.of("src/main/resources/templates");
    private final Path cssDir = Path.of("src/main/resources/css");
    private Path currentFilePath;

    @FXML
    public void initialize() {
        createDirsIfNeeded();
        loadFileList();
        loadTemplates();
        loadCssOptions();

        fileListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                currentFilePath = htmlDir.resolve(newVal);
                loadHtmlFile();
            }
        });
    }

    private void createDirsIfNeeded() {
        try {
            Files.createDirectories(htmlDir);
            Files.createDirectories(templatesDir);
            Files.createDirectories(cssDir);
        } catch (IOException e) {
            showPopup("Nem sikerült a könyvtárak létrehozása: " + e.getMessage());
        }
    }

    private void loadFileList() {
        try {
            List<String> files = Files.list(htmlDir)
                    .filter(p -> p.toString().endsWith(".html"))
                    .map(p -> p.getFileName().toString())
                    .toList();
            fileListView.getItems().setAll(files);
        } catch (IOException e) {
            showPopup("Fájlok betöltése sikertelen: " + e.getMessage());
        }
    }

    private void loadTemplates() {
        try (Stream<Path> paths = Files.walk(templatesDir)) {
            List<String> templateNames = paths
                    .filter(Files::isRegularFile)
                    .map(p -> p.getFileName().toString())
                    .collect(Collectors.toList());
            templateComboBox.getItems().setAll(templateNames);
        } catch (IOException e) {
            templateComboBox.getItems().add("<nincs elérhető sablon>");
            showPopup("Sablonok betöltése sikertelen: " + e.getMessage());
        }
    }

    private void loadCssOptions() {
        try {
            List<String> cssFiles = Files.list(cssDir)
                    .filter(p -> p.toString().endsWith(".css"))
                    .map(p -> p.getFileName().toString())
                    .toList();
            cssComboBox.getItems().setAll(cssFiles);
        } catch (IOException e) {
            cssComboBox.getItems().add("<nincs css>");
            showPopup("CSS fájlok betöltése sikertelen: " + e.getMessage());
        }
    }

    private void loadHtmlFile() {
        try {
            String content = Files.readString(currentFilePath, StandardCharsets.UTF_8);
            htmlEditor.setText(content);
            loadPreview(content);
        } catch (IOException e) {
            htmlEditor.clear();
            showPopup("Nem sikerült a fájl betöltése: " + e.getMessage());
        }
    }

    private void loadPreview(String htmlContent) {
        webPreview.getEngine().loadContent(htmlContent, "text/html");
    }

    @FXML
    private void onSaveClicked() {
        if (currentFilePath == null) {
            showPopup("Mentéshez válassz ki egy fájlt.");
            return;
        }
        try {
            Files.writeString(currentFilePath, htmlEditor.getText(), StandardCharsets.UTF_8);
            loadPreview(htmlEditor.getText());
        } catch (IOException e) {
            showPopup("Nem sikerült menteni: " + e.getMessage());
        }
    }

    @FXML
    private void onPreviewClicked() {
        loadPreview(htmlEditor.getText());
    }

    @FXML
    private void onNewFileClicked() {
        TextInputDialog dialog = new TextInputDialog("ujoldal.html");
        dialog.setTitle("Új HTML fájl");
        dialog.setHeaderText("Adj meg egy fájlnevet:");
        dialog.setContentText("Fájlnév:");

        dialog.showAndWait().ifPresent(fileName -> {
            try {
                if (!fileName.endsWith(".html")) fileName += ".html";
                if (!isValidFileName(fileName)) {
                    showPopup("A fájlnév nem tartalmazhat speciális karaktereket.");
                    return;
                }
                Path newFile = htmlDir.resolve(fileName);
                if (!Files.exists(newFile)) {
                    Files.writeString(newFile, "<!-- Új HTML oldal -->", StandardCharsets.UTF_8);
                    loadFileList();
                    fileListView.getSelectionModel().select(fileName);
                } else {
                    showPopup("A fájl már létezik. Adj meg másik nevet.");
                }
            } catch (IOException e) {
                showPopup("Hiba a létrehozáskor: " + e.getMessage());
            }
        });
    }

    private boolean isValidFileName(String name) {
        return name.matches("[\\w\\-. ]+\\.html");
    }

    @FXML
    private void onDeleteFileClicked() {
        String selectedFile = fileListView.getSelectionModel().getSelectedItem();
        if (selectedFile == null) {
            showPopup("Válassz fájlt a törléshez.");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Törlés megerősítése");
        confirm.setHeaderText("Tényleg törlöd?");
        confirm.setContentText(selectedFile);

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    Path toDelete = htmlDir.resolve(selectedFile);
                    Files.deleteIfExists(toDelete);
                    htmlEditor.clear();
                    webPreview.getEngine().loadContent("", "text/html");
                    loadFileList();
                } catch (IOException e) {
                    showPopup("Hiba a törlés során: " + e.getMessage());
                }
            }
        });
    }

    @FXML
    private void onInsertTemplateClicked() {
        String selectedTemplate = templateComboBox.getValue();
        if (selectedTemplate != null) {
            try {
                Path templatePath = templatesDir.resolve(selectedTemplate);
                String content = Files.readString(templatePath, StandardCharsets.UTF_8);
                htmlEditor.insertText(htmlEditor.getCaretPosition(), content);
                showPopup("A(z) " + selectedTemplate + " sablon beillesztve.");
            } catch (IOException e) {
                showPopup("Hiba a sablon betöltéskor: " + e.getMessage());
            }
        } else {
            showPopup("Előbb válassz ki egy sablont a legördülő menüből.");
        }
    }

    @FXML
    private void onAttachCssClicked() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("CSS fájl kiválasztása");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSS fájlok", "*.css"));

        File selectedFile = fileChooser.showOpenDialog(null);
        if (selectedFile != null) {
            try {
                Path targetPath = cssDir.resolve(selectedFile.getName());
                if (!Files.exists(targetPath)) {
                    Files.copy(selectedFile.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);
                }
                String linkTag = "<link rel=\"stylesheet\" href=\"css/" + selectedFile.getName() + "\">\n";
                htmlEditor.insertText(htmlEditor.getCaretPosition(), linkTag);
                loadCssOptions();
                showPopup("A CSS fájl csatolva: " + selectedFile.getName());
            } catch (IOException e) {
                showPopup("Nem sikerült a CSS fájl másolása: " + e.getMessage());
            }
        }
    }

    @FXML
    private void onSearch() {
        String keyword = searchField.getText();
        if (keyword != null && !keyword.isBlank()) {
            int index = htmlEditor.getText().indexOf(keyword);
            if (index >= 0) {
                htmlEditor.selectRange(index, index + keyword.length());
            } else {
                showPopup("Nem található a megadott szöveg.");
            }
        }
    }

    @FXML
    private void onExportFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Exportálás mentése");
        fileChooser.setInitialFileName("export.html");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("HTML fájlok", "*.html"));

        File file = fileChooser.showSaveDialog(null);
        if (file != null) {
            try {
                Files.writeString(file.toPath(), htmlEditor.getText(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                showPopup("Mentési hiba: " + e.getMessage());
            }
        }
    }

    @FXML
    private void onAIGenerateClicked() {
        String prompt = aiPromptField.getText();
        if (prompt == null || prompt.isBlank()) {
            showPopup("Adj meg egy leírást az AI-nak.");
            return;
        }

        String apiKey = getOpenAiApiKey();
        if (apiKey == null || apiKey.isBlank()) return;

        try {
            HttpClient client = HttpClient.newHttpClient();
            String body = new JSONObject()
                    .put("model", "gpt-4")
                    .put("messages", new JSONArray()
                            .put(new JSONObject()
                                    .put("role", "user")
                                    .put("content", "Írj HTML kódot a következőhöz: " + prompt)))
                    .put("max_tokens", 500)
                    .toString();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.openai.com/v1/chat/completions"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> {
                        if (response.statusCode() != 200) {
                            javafx.application.Platform.runLater(() ->
                                    showPopup("AI válasz hiba: státuszkód: " + response.statusCode()));
                            return;
                        }
                        String result = extractMessageContentFromJson(response.body());
                        javafx.application.Platform.runLater(() -> {
                            htmlEditor.insertText(htmlEditor.getCaretPosition(), result);
                            loadPreview(htmlEditor.getText());
                            showPopup("A HTML szakasz beszúrva.");
                        });
                    })
                    .exceptionally(e -> {
                        javafx.application.Platform.runLater(() -> showPopup("AI hívás sikertelen: " + e.getMessage()));
                        return null;
                    });
        } catch (Exception e) {
            showPopup("Hiba: " + e.getMessage());
        }
    }

    private String extractMessageContentFromJson(String json) {
        try {
            JSONObject obj = new JSONObject(json);
            return obj.getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content");
        } catch (JSONException e) {
            return "[Hiba: válasz nem értelmezhető]";
        }
    }

    private String getOpenAiApiKey() {
        try (InputStream input = Files.newInputStream(Paths.get("src/main/resources/config.properties"))) {
            Properties prop = new Properties();
            prop.load(input);
            return prop.getProperty("openai.api.key");
        } catch (IOException e) {
            showPopup("Nem található vagy nem olvasható a config.properties.");
            return null;
        }
    }

    private void showPopup(String message) {
        Tooltip tip = new Tooltip(message);
        tip.setAutoHide(true);
        tip.show(htmlEditor.getScene().getWindow());
    }
}
