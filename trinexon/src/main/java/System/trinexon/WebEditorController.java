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
            showAlert(Alert.AlertType.ERROR, "Könyvtár hiba", "Nem sikerült a könyvtárak létrehozása: " + e.getMessage());
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
            showAlert(Alert.AlertType.ERROR, "Hiba", "Fájlok betöltése sikertelen: " + e.getMessage());
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
        }
    }

    private void loadHtmlFile() {
        try {
            String content = Files.readString(currentFilePath, StandardCharsets.UTF_8);
            htmlEditor.setText(content);
            loadPreview(content);
        } catch (IOException e) {
            htmlEditor.clear();
            showAlert(Alert.AlertType.ERROR, "Fájlhiba", "Nem sikerült a fájl betöltése: " + e.getMessage());
        }
    }

    private void loadPreview(String htmlContent) {
        webPreview.getEngine().loadContent(htmlContent, "text/html");
    }

    @FXML
    private void onSaveClicked() {
        try {
            if (currentFilePath != null) {
                Files.writeString(currentFilePath, htmlEditor.getText(), StandardCharsets.UTF_8);
                loadPreview(htmlEditor.getText());
            }
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Mentési hiba", "Nem sikerült menteni: " + e.getMessage());
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
                    showAlert(Alert.AlertType.WARNING, "Érvénytelen fájlnév", "A fájlnév nem tartalmazhat speciális karaktereket.");
                    return;
                }

                Path newFile = htmlDir.resolve(fileName);
                if (!Files.exists(newFile)) {
                    Files.writeString(newFile, "<!-- Új HTML oldal -->", StandardCharsets.UTF_8);
                    loadFileList();
                    fileListView.getSelectionModel().select(fileName);
                } else {
                    showAlert(Alert.AlertType.WARNING, "A fájl már létezik", "Adj meg másik nevet.");
                }
            } catch (IOException e) {
                showAlert(Alert.AlertType.ERROR, "Hiba a létrehozáskor", e.getMessage());
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
            showAlert(Alert.AlertType.INFORMATION, "Nincs fájl kiválasztva", "Válassz fájlt a törléshez.");
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
                    showAlert(Alert.AlertType.ERROR, "Hiba a törlés során", e.getMessage());
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
                showAlert(Alert.AlertType.INFORMATION, "Sablon beszúrva", "A(z) " + selectedTemplate + " sablon beillesztve.");
            } catch (IOException e) {
                showAlert(Alert.AlertType.ERROR, "Hiba a sablon betöltéskor", e.getMessage());
            }
        } else {
            showAlert(Alert.AlertType.WARNING, "Nincs sablon kiválasztva", "Előbb válassz ki egy sablont a legördülő menüből.");
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
                showAlert(Alert.AlertType.INFORMATION, "CSS csatolva", "A következő fájl hozzáadva: " + selectedFile.getName());
                loadCssOptions();

            } catch (IOException e) {
                showAlert(Alert.AlertType.ERROR, "Hiba", "Nem sikerült a CSS fájl másolása: " + e.getMessage());
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
                showAlert(Alert.AlertType.INFORMATION, "Nincs találat", "Nem található a megadott szöveg.");
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
                showAlert(Alert.AlertType.ERROR, "Mentési hiba", e.getMessage());
            }
        }
    }
    @FXML
    private void onAIGenerateClicked() {
        String prompt = aiPromptField.getText();
        if (prompt == null || prompt.isBlank()) {
            showAlert(Alert.AlertType.WARNING, "Hiányzó bemenet", "Adj meg egy leírást az AI-nak.");
            return;
        }

        String apiKey = getOpenAiApiKey();
        if (apiKey == null || apiKey.isBlank()) return;

        try {
            HttpClient client = HttpClient.newHttpClient();
            String body = "{" +
                    "\"model\":\"gpt-3.5-turbo-instruct\"," +
                    "\"prompt\":\"Írj HTML kódot a következőhöz: " + prompt.replace("\"", "\\\"") + "\"," +
                    "\"max_tokens\":500" +
                    "}";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.openai.com/v1/completions"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(HttpResponse::body)
                    .thenAccept(response -> {
                        String result = response.replaceAll(".*?\"text\"\\s*:\\s*\"(.*?)\".*", "$1")
                                .replace("\\n", "\n")
                                .replace("\\\"", "\"")
                                .trim();

                        javafx.application.Platform.runLater(() -> {
                            htmlEditor.insertText(htmlEditor.getCaretPosition(), result);
                            loadPreview(htmlEditor.getText());
                            showAlert(Alert.AlertType.INFORMATION, "AI generálás kész", "A HTML szakasz beszúrva.");
                        });
                    })
                    .exceptionally(e -> {
                        javafx.application.Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Hiba", "AI hívás sikertelen: " + e.getMessage()));
                        return null;
                    });

        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Hiba", e.getMessage());
        }
    }

    private String getOpenAiApiKey() {
        try (InputStream input = Files.newInputStream(Paths.get("src/main/resources/config.properties"))) {
            Properties prop = new Properties();
            prop.load(input);
            return prop.getProperty("openai.api.key");
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Konfigurációs hiba", "Nem található vagy nem olvasható a config.properties.");
            return null;
        }
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
