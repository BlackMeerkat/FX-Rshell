import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.InetAddress;
import java.io.IOException;
import java.io.InputStreamReader;

public class ShellSession {
    private Socket clientSocket;
    private String ip;
    private String connectIp; // L'adresse IP locale qui a été utilisée pour la connexion
    private String username;
    private String hostname;
    private String os;
    private int listenerPort;
    private BufferedReader in;
    private PrintWriter out;
    private String payloadType;
    private ObservableList<String> commandHistory = FXCollections.observableArrayList();
    private StringProperty status = new SimpleStringProperty("Online");
    
    // Dracula theme colors
    private static final String DRACULA_GREEN = "#50fa7b";
    private static final String DRACULA_RED = "#ff5555";
    private static final String DRACULA_ORANGE = "#ffb86c";
    private static final String DRACULA_YELLOW = "#f1fa8c";
    
    // Statuts avec code couleur
    private static final String STATUS_ONLINE = "Online";
    private static final String STATUS_OFFLINE = "Offline";
    private static final String STATUS_BUSY = "Busy";
    private static final String STATUS_WAITING = "Waiting";
    
    public ShellSession(Socket clientSocket, int listenerPort, BufferedReader in, PrintWriter out) {
        this.clientSocket = clientSocket;
        this.listenerPort = listenerPort;
        this.in = in;
        this.out = out;
        this.ip = clientSocket.getInetAddress().getHostAddress();
        this.payloadType = "Unknown";
        
        // Vérifier que les flux sont correctement initialisés
        if (in == null || out == null) {
            System.err.println("[ERREUR CRITIQUE] Les flux d'E/S sont null pour la connexion: " + ip);
            try {
                // Essayer de les créer si ils sont null
                if (in == null) {
                    this.in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                }
                if (out == null) {
                    this.out = new PrintWriter(clientSocket.getOutputStream(), true);
                }
            } catch (IOException e) {
                System.err.println("[ERREUR FATALE] Impossible de créer les flux: " + e.getMessage());
            }
        }
        
        // Initialiser l'adresse de connexion avec l'adresse locale du socket
        this.connectIp = clientSocket.getLocalAddress().getHostAddress();
        if (this.connectIp.equals("0.0.0.0")) {
            // Si l'adresse est 0.0.0.0, essayer de trouver une meilleure adresse
            try {
                this.connectIp = InetAddress.getLocalHost().getHostAddress();
            } catch (Exception e) {
                // En cas d'erreur, conserver l'adresse originale
                this.connectIp = this.ip;
            }
        }
    }

    public void setMetadata(String username, String hostname, String os) {
        this.username = username;
        this.hostname = hostname;
        this.os = os;
    }
    
    public void setMetadata(String username, String hostname, String os, String payloadType) {
        this.username = username;
        this.hostname = hostname;
        this.os = os;
        this.payloadType = payloadType;
    }

    public void setPayloadType(String payloadType) {
        this.payloadType = payloadType;
    }
    
    public void setStatus(String status) {
        this.status.set(status);
    }
    
    public String getStatus() {
        return status.get();
    }
    
    public StringProperty statusProperty() {
        return status;
    }
    
    // Retourne le style CSS pour le statut
    public String getStatusStyle() {
        String statusText = status.get();
        if (STATUS_ONLINE.equals(statusText)) {
            return "-fx-text-fill: " + DRACULA_GREEN + "; -fx-font-weight: bold;";
        } else if (STATUS_OFFLINE.equals(statusText)) {
            return "-fx-text-fill: " + DRACULA_RED + "; -fx-font-weight: bold;";
        } else if (STATUS_BUSY.equals(statusText)) {
            return "-fx-text-fill: " + DRACULA_ORANGE + "; -fx-font-weight: bold;";
        } else if (STATUS_WAITING.equals(statusText)) {
            return "-fx-text-fill: " + DRACULA_YELLOW + "; -fx-font-weight: bold;";
        }
        return ""; // Style par défaut
    }

    public void addToHistory(String command) {
        commandHistory.add(command);
    }

    public Socket getClientSocket() {
        return clientSocket;
    }

    public String getIp() {
        return ip;
    }

    /**
     * Retourne l'adresse IP qui a servi à se connecter au client.
     * Cette méthode est utile pour le transfert de fichiers afin d'utiliser
     * l'adresse IP correcte dans les URLs.
     * 
     * @return L'adresse IP utilisée pour la connexion
     */
    public String getOriginalConnectIp() {
        return connectIp;
    }

    public String getUsername() {
        return username;
    }

    public String getHostname() {
        return hostname;
    }

    public String getOs() {
        return os;
    }
    
    public String getPayloadType() {
        return payloadType;
    }

    public int getListenerPort() {
        return listenerPort;
    }

    public BufferedReader getIn() {
        // Vérifier que le flux n'est pas fermé
        if (in == null || clientSocket.isClosed()) {
            System.err.println("[ERREUR] Tentative d'accès à un flux d'entrée fermé ou null");
            return null;
        }
        return in;
    }

    public PrintWriter getOut() {
        // Vérifier que le flux n'est pas fermé
        if (out == null || clientSocket.isClosed()) {
            System.err.println("[ERREUR] Tentative d'accès à un flux de sortie fermé ou null");
            return null;
        }
        return out;
    }

    public ObservableList<String> getCommandHistory() {
        return commandHistory;
    }

    public void close() {
        try {
            // Mettre le statut à Offline avant de fermer les connexions
            setStatus(STATUS_OFFLINE);
            
            if (out != null) {
                out.close();
            }
            if (in != null) {
                in.close();
            }
            if (clientSocket != null && !clientSocket.isClosed()) {
                clientSocket.close();
            }
        } catch (Exception e) {
            System.err.println("Error closing shell session: " + e.getMessage());
        }
    }
    
    @Override
    public String toString() {
        return ip + " (" + hostname + ") [" + payloadType + "] - " + getStatus();
    }

    /**
     * Attends une seule ligne de réponse du client
     * 
     * @return La ligne de réponse
     * @throws IOException En cas d'erreur de lecture
     */
    public String waitForSingleResponse() throws IOException {
        return in.readLine();
    }
    
    /**
     * Envoie une commande brute au client (sans traitement particulier)
     * 
     * @param command La commande à envoyer
     */
    public void sendRawCommand(String command) {
        out.println(command);
        out.flush();
    }

    /**
     * Récupère le socket de cette session
     * 
     * @return Socket de la session
     */
    public Socket getSocket() {
        return clientSocket;
    }

    // Méthode d'aide pour vérifier si la connexion est active
    public boolean isConnected() {
        return clientSocket != null && !clientSocket.isClosed() && clientSocket.isConnected();
    }
} 