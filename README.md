# FX-Rshell

FX-Rshell is a JavaFX-based command-and-control interface for managing remote shells in a tabbed, centralized environment.

## Features

### Listener Management
- Create TCP listeners on custom ports
- View and manage active listeners
- Automatically handle incoming connections

### Shell Management
- Track multiple connected shells in a centralized table
- View connection details (IP, username, hostname, OS)
- Interact with shells in individual tabs
- Kill shells with a right-click

### Payloads
- Generate reverse shell payloads in multiple languages (Go, C, Rust)
- Configure payloads for different operating systems (Windows/Linux)
- Select target architecture (32/64 bits)
- Compile directly from the application

### Console
- Tabbed interface for each shell session
- Command history per session
- Up/down arrow key navigation through command history
- Visual separators between commands and their output

## Setup & Running

### Prerequisites
- Java 11+
- JavaFX

### Compiler Dependencies
To generate and compile payloads, you'll need:

#### For Go Payloads
- Go compiler (1.13+)
  - Ubuntu/Debian: `sudo apt install golang-go`
  - Fedora: `sudo dnf install golang`
  - Arch: `sudo pacman -S go`
  - Windows: Download from https://golang.org/dl/

#### For C Payloads
- GCC compiler
  - Ubuntu/Debian: `sudo apt install gcc gcc-multilib`
  - Fedora: `sudo dnf install gcc glibc-devel.i686 libgcc.i686`
  - Arch: `sudo pacman -S gcc lib32-gcc-libs`
  - Windows: MinGW (http://mingw-w64.org/doku.php/download)

- For Windows cross-compilation on Linux:
  - Ubuntu/Debian: `sudo apt install mingw-w64`
  - Fedora: `sudo dnf install mingw64-gcc mingw32-gcc`
  - Arch: `sudo pacman -S mingw-w64-gcc`

#### For Rust Payloads
- Rust toolchain (cargo, rustc)
  - All platforms: `curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs | sh`
  - Windows: Download from https://www.rust-lang.org/tools/install

- For cross-compilation support:
  ```bash
  # Add 32-bit Linux target
  rustup target add i686-unknown-linux-gnu
  
  # Add Windows targets
  rustup target add x86_64-pc-windows-gnu
  rustup target add i686-pc-windows-gnu
  
  # For 32-bit Linux target on Debian/Ubuntu
  sudo apt install gcc-multilib
  ```

### Building from Source
```bash
# Compile the Java files
javac -d bin src/*.java

# Run the application
java -cp bin Main
```

## Usage

1. Start the application
2. Create a listener (Listeners > Create Listener)
3. Generate a payload (Payloads > Choose language)
4. Execute the payload on a target system
5. Once connected, the shell will appear in the left panel
6. Double-click or right-click > Interact to open a console tab
7. Enter commands in the console tab's input field

## Shell Commands
The shell supports all standard commands available in the target system's shell:
- Windows: cmd.exe commands
- Linux: bash/sh commands

## Notes

- All payloads are generated in the `output` directory
- Compilation output provides error messages and success indicators
- All connections are unencrypted by default
- Each shell includes metadata about the connected system 