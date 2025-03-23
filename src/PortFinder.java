import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Utilitaire pour trouver des ports disponibles sur le système
 */
public class PortFinder {
    private static final Random random = new Random();
    private static final List<Integer> reservedPorts = new ArrayList<>();

    /**
     * Plage des ports éphémères que nous allons utiliser
     */
    private static final int MIN_PORT = 10000;
    private static final int MAX_PORT = 65535;

    /**
     * Trouve un port disponible sur le système
     *
     * @return Un port disponible, ou -1 si aucun port n'est disponible
     */
    public static synchronized int findAvailablePort() {
        return findAvailablePort(MIN_PORT, MAX_PORT, 50);
    }

    /**
     * Trouve un port disponible dans une plage spécifiée
     *
     * @param minPort Port minimum
     * @param maxPort Port maximum
     * @param maxAttempts Nombre maximum de tentatives
     * @return Un port disponible, ou -1 si aucun port n'est disponible
     */
    public static synchronized int findAvailablePort(int minPort, int maxPort, int maxAttempts) {
        int attempts = 0;
        
        while (attempts < maxAttempts) {
            int candidatePort = getRandomPort(minPort, maxPort);
            
            if (reservedPorts.contains(candidatePort)) {
                attempts++;
                continue;
            }
            
            if (isPortAvailable(candidatePort)) {
                reservedPorts.add(candidatePort);
                return candidatePort;
            }
            
            attempts++;
        }
        
        return -1; // Aucun port disponible trouvé
    }

    /**
     * Libère un port précédemment réservé
     *
     * @param port Le port à libérer
     */
    public static synchronized void releasePort(int port) {
        reservedPorts.remove(Integer.valueOf(port));
    }

    /**
     * Vérifie si un port est disponible
     *
     * @param port Le port à vérifier
     * @return true si le port est disponible, false sinon
     */
    private static boolean isPortAvailable(int port) {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            serverSocket.setReuseAddress(true);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Génère un numéro de port aléatoire dans la plage spécifiée
     *
     * @param minPort Port minimum
     * @param maxPort Port maximum
     * @return Un port aléatoire
     */
    private static int getRandomPort(int minPort, int maxPort) {
        return minPort + random.nextInt(maxPort - minPort + 1);
    }
} 