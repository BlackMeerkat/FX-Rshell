#!/bin/bash

set -e

echo "[*] Detecting Linux distribution..."

# Detect distro
if [ -f /etc/os-release ]; then
    . /etc/os-release
    DISTRO=$ID
else
    echo "Unsupported system"
    exit 1
fi

echo "[*] Detected distro: $DISTRO"

# Install Go
echo "[*] Installing Go..."
case "$DISTRO" in
    ubuntu|debian)
        sudo apt update
        sudo apt install -y golang-go
        ;;
    fedora)
        sudo dnf install -y golang
        ;;
    arch)
        sudo pacman -Sy --noconfirm go
        ;;
    *)
        echo "Unsupported distro for Go"
        ;;
esac

# Install GCC and 32-bit support
echo "[*] Installing GCC and 32-bit libraries..."
case "$DISTRO" in
    ubuntu|debian)
        sudo apt install -y gcc gcc-multilib
        ;;
    fedora)
        sudo dnf install -y gcc glibc-devel.i686 libgcc.i686
        ;;
    arch)
        sudo pacman -Sy --noconfirm gcc lib32-gcc-libs
        ;;
    *)
        echo "Unsupported distro for GCC"
        ;;
esac

# Install MinGW for cross-compilation
echo "[*] Installing MinGW cross-compiler for Windows payloads..."
case "$DISTRO" in
    ubuntu|debian)
        sudo apt install -y mingw-w64
        ;;
    fedora)
        sudo dnf install -y mingw64-gcc mingw32-gcc
        ;;
    arch)
        sudo pacman -Sy --noconfirm mingw-w64-gcc
        ;;
    *)
        echo "Unsupported distro for MinGW"
        ;;
esac

# Install Rust
if ! command -v cargo &> /dev/null; then
    echo "[*] Installing Rust toolchain..."
    curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs | sh -s -- -y
    source "$HOME/.cargo/env"
else
    echo "[*] Rust already installed."
fi

# Add Rust cross-compilation targets
echo "[*] Adding Rust cross-compilation targets..."
rustup target add i686-unknown-linux-gnu
rustup target add x86_64-pc-windows-gnu
rustup target add i686-pc-windows-gnu

# For 32-bit builds on Debian-based systems
if [[ "$DISTRO" == "ubuntu" || "$DISTRO" == "debian" ]]; then
    echo "[*] Ensuring gcc-multilib is installed for 32-bit Rust targets..."
    sudo apt install -y gcc-multilib
fi

echo "[✔] All dependencies installed successfully."
