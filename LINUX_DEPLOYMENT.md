# 🐧 AMCP v1.5 - Linux Deployment Guide

Complete guide for deploying AMCP (Agent Mesh Communication Protocol) v1.5 on Linux distributions.

---

## Table of Contents

- [Supported Linux Distributions](#supported-linux-distributions)
- [Prerequisites Installation](#prerequisites-installation)
- [Quick Start on Linux](#quick-start-on-linux)
- [Distribution-Specific Setup](#distribution-specific-setup)
- [Docker Deployment on Linux](#docker-deployment-on-linux)
- [Systemd Service Configuration](#systemd-service-configuration)
- [Performance Tuning for Linux](#performance-tuning-for-linux)
- [Troubleshooting Linux Issues](#troubleshooting-linux-issues)

---

## Supported Linux Distributions

AMCP v1.5 is tested and supported on:

- ✅ **Ubuntu** 20.04 LTS, 22.04 LTS, 24.04 LTS
- ✅ **Debian** 11 (Bullseye), 12 (Bookworm)
- ✅ **Red Hat Enterprise Linux (RHEL)** 8, 9
- ✅ **CentOS Stream** 8, 9
- ✅ **Fedora** 38, 39, 40
- ✅ **Amazon Linux** 2, 2023
- ✅ **Arch Linux** (rolling release)
- ✅ **openSUSE** Leap 15.x, Tumbleweed

---

## Prerequisites Installation

### Ubuntu/Debian

```bash
# Update package list
sudo apt update

# Install Java 21 (OpenJDK)
sudo apt install -y openjdk-21-jdk openjdk-21-jre

# Install Maven
sudo apt install -y maven

# Install Git
sudo apt install -y git

# Install Docker (optional, for containerized deployment)
sudo apt install -y docker.io docker-compose
sudo systemctl enable --now docker
sudo usermod -aG docker $USER

# Verify installations
java -version    # Should show version 21
mvn -version     # Should show Maven 3.8+
docker --version
```

### RHEL/CentOS/Fedora

```bash
# Enable EPEL repository (RHEL/CentOS only)
sudo dnf install -y epel-release

# Install Java 21
sudo dnf install -y java-21-openjdk java-21-openjdk-devel

# Install Maven
sudo dnf install -y maven

# Install Git
sudo dnf install -y git

# Install Docker (optional)
sudo dnf install -y docker docker-compose
sudo systemctl enable --now docker
sudo usermod -aG docker $USER

# Verify installations
java -version
mvn -version
```

### Arch Linux

```bash
# Install Java 21
sudo pacman -S jdk21-openjdk

# Install Maven
sudo pacman -S maven

# Install Git
sudo pacman -S git

# Install Docker (optional)
sudo pacman -S docker docker-compose
sudo systemctl enable --now docker
sudo usermod -aG docker $USER

# Set Java 21 as default
sudo archlinux-java set java-21-openjdk
```

### Amazon Linux 2023

```bash
# Install Java 21
sudo dnf install -y java-21-amazon-corretto java-21-amazon-corretto-devel

# Install Maven
sudo dnf install -y maven

# Install Git
sudo dnf install -y git

# Install Docker
sudo dnf install -y docker
sudo systemctl enable --now docker
sudo usermod -aG docker $USER
```

---

## Quick Start on Linux

### 1. Clone and Build

```bash
# Clone the repository
git clone https://github.com/xaviercallens/amcp-v1.5-opensource.git
cd amcp-v1.5-opensource

# Run the Linux setup script
./setup-linux.sh

# Build the project
mvn clean install -DskipTests

# Make scripts executable
chmod +x amcp-cli
chmod +x *.sh
```

### 2. Launch AMCP CLI

```bash
# Build and launch
./amcp-cli --build

# Or just launch if already built
./amcp-cli
```

### 3. Configure Environment Variables

```bash
# Add to ~/.bashrc or ~/.profile
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64  # Ubuntu/Debian
# OR
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk        # RHEL/Fedora

export PATH="$JAVA_HOME/bin:$PATH"

# Optional: API keys for external services
export POLYGON_API_KEY="your_polygon_api_key"
export OPENWEATHER_API_KEY="your_openweather_api_key"

# Reload configuration
source ~/.bashrc
```

---

## Distribution-Specific Setup

### Ubuntu 20.04 LTS (Java 21 from PPA)

```bash
# Add PPA for Java 21 if not available in default repos
sudo add-apt-repository ppa:openjdk-r/ppa
sudo apt update
sudo apt install -y openjdk-21-jdk

# Set Java 21 as default
sudo update-alternatives --config java
```

### RHEL 8 (Enable CodeReady Builder)

```bash
# Enable CodeReady Builder repository
sudo subscription-manager repos --enable codeready-builder-for-rhel-8-x86_64-rpms

# Install Java 21
sudo dnf install -y java-21-openjdk java-21-openjdk-devel
```

### Debian 11 (Backports)

```bash
# Add backports repository
echo "deb http://deb.debian.org/debian bullseye-backports main" | \
  sudo tee /etc/apt/sources.list.d/backports.list

sudo apt update
sudo apt install -y -t bullseye-backports openjdk-21-jdk
```

---

## Docker Deployment on Linux

### Using Docker Compose

```bash
# Navigate to deployment directory
cd deploy/docker

# Start all services
docker-compose up -d

# Check service status
docker-compose ps

# View logs
docker-compose logs -f amcp-agent

# Stop services
docker-compose down
```

### Manual Docker Deployment

```bash
# Build Docker image
docker build -t amcp-v1.5:latest .

# Run AMCP container
docker run -d \
  --name amcp-agent \
  -p 8080:8080 \
  -p 8081:8081 \
  -e JAVA_OPTS="-Xmx2g -Xms512m" \
  -e AMCP_EVENT_BROKER_TYPE=memory \
  -v $(pwd)/logs:/app/logs \
  amcp-v1.5:latest

# Check logs
docker logs -f amcp-agent

# Stop container
docker stop amcp-agent
docker rm amcp-agent
```

---

## Systemd Service Configuration

Create a systemd service for production deployment:

### 1. Create Service File

```bash
sudo nano /etc/systemd/system/amcp.service
```

### 2. Service Configuration

```ini
[Unit]
Description=AMCP v1.5 Agent Mesh Communication Protocol
After=network.target

[Service]
Type=simple
User=amcp
Group=amcp
WorkingDirectory=/opt/amcp
ExecStart=/usr/bin/java -jar /opt/amcp/core/target/amcp-core-1.5.0.jar
Restart=on-failure
RestartSec=10
StandardOutput=journal
StandardError=journal

# Environment variables
Environment="JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64"
Environment="AMCP_EVENT_BROKER_TYPE=kafka"
Environment="AMCP_KAFKA_BOOTSTRAP_SERVERS=localhost:9092"

# Resource limits
LimitNOFILE=65536
LimitNPROC=4096

[Install]
WantedBy=multi-user.target
```

### 3. Setup and Start Service

```bash
# Create AMCP user
sudo useradd -r -s /bin/false amcp

# Create directories
sudo mkdir -p /opt/amcp
sudo cp -r /home/$USER/amcp-v1.5-opensource/* /opt/amcp/
sudo chown -R amcp:amcp /opt/amcp

# Reload systemd
sudo systemctl daemon-reload

# Enable and start service
sudo systemctl enable amcp
sudo systemctl start amcp

# Check status
sudo systemctl status amcp

# View logs
sudo journalctl -u amcp -f
```

---

## Performance Tuning for Linux

### JVM Tuning

```bash
# Add to service file or startup script
export JAVA_OPTS="-Xmx4g -Xms1g \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=200 \
  -XX:+UseStringDeduplication \
  -XX:+ParallelRefProcEnabled \
  -Djava.net.preferIPv4Stack=true"
```

### Kernel Parameters

```bash
# Edit /etc/sysctl.conf
sudo nano /etc/sysctl.conf

# Add these lines:
net.core.somaxconn = 4096
net.ipv4.tcp_max_syn_backlog = 4096
net.ipv4.ip_local_port_range = 10000 65535
net.ipv4.tcp_tw_reuse = 1
fs.file-max = 2097152

# Apply changes
sudo sysctl -p
```

### File Descriptor Limits

```bash
# Edit /etc/security/limits.conf
sudo nano /etc/security/limits.conf

# Add these lines:
amcp soft nofile 65536
amcp hard nofile 65536
amcp soft nproc 4096
amcp hard nproc 4096
```

### Huge Pages (Optional, for large deployments)

```bash
# Calculate required huge pages (for 4GB heap)
# Huge page size is typically 2MB
echo 2048 | sudo tee /proc/sys/vm/nr_hugepages

# Make permanent
echo "vm.nr_hugepages = 2048" | sudo tee -a /etc/sysctl.conf

# Add to JAVA_OPTS
-XX:+UseLargePages
```

---

## Troubleshooting Linux Issues

### Java Version Issues

```bash
# List installed Java versions
update-alternatives --list java  # Debian/Ubuntu
alternatives --list java         # RHEL/Fedora

# Set Java 21 as default
sudo update-alternatives --config java  # Debian/Ubuntu
sudo alternatives --config java         # RHEL/Fedora

# Verify
java -version
```

### Port Already in Use

```bash
# Find process using port 8080
sudo lsof -i :8080
# OR
sudo netstat -tulpn | grep 8080
# OR
sudo ss -tulpn | grep 8080

# Kill process
sudo kill -9 <PID>
```

### Permission Denied Errors

```bash
# Make scripts executable
chmod +x *.sh
chmod +x amcp-cli

# Fix ownership
sudo chown -R $USER:$USER /path/to/amcp-v1.5-opensource
```

### Out of Memory Errors

```bash
# Check available memory
free -h

# Increase JVM heap size
export JAVA_OPTS="-Xmx4g -Xms1g"

# Check swap
sudo swapon --show

# Add swap if needed (4GB example)
sudo fallocate -l 4G /swapfile
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
```

### Docker Permission Issues

```bash
# Add user to docker group
sudo usermod -aG docker $USER

# Logout and login again, or run:
newgrp docker

# Verify
docker ps
```

### Firewall Configuration

#### UFW (Ubuntu/Debian)

```bash
# Allow AMCP ports
sudo ufw allow 8080/tcp
sudo ufw allow 8081/tcp
sudo ufw reload
```

#### Firewalld (RHEL/CentOS/Fedora)

```bash
# Allow AMCP ports
sudo firewall-cmd --permanent --add-port=8080/tcp
sudo firewall-cmd --permanent --add-port=8081/tcp
sudo firewall-cmd --reload
```

### SELinux Issues (RHEL/CentOS/Fedora)

```bash
# Check SELinux status
sestatus

# Temporarily set to permissive (for testing)
sudo setenforce 0

# Make permanent (not recommended for production)
sudo sed -i 's/SELINUX=enforcing/SELINUX=permissive/' /etc/selinux/config

# Better: Create SELinux policy (advanced)
# Or run with proper context
sudo chcon -R -t bin_t /opt/amcp/*.sh
```

---

## Additional Resources

- **Main Documentation**: [README.md](README.md)
- **Quick Start Guide**: [QUICK_START.md](QUICK_START.md)
- **General Deployment**: [DEPLOYMENT.md](DEPLOYMENT.md)
- **Troubleshooting**: [BUILD_TROUBLESHOOTING.md](BUILD_TROUBLESHOOTING.md)
- **Contributing**: [CONTRIBUTING.md](CONTRIBUTING.md)

---

## Support

For Linux-specific issues:
- **GitHub Issues**: https://github.com/xaviercallens/amcp-v1.5-opensource/issues
- **Discussions**: https://github.com/xaviercallens/amcp-v1.5-opensource/discussions
- Tag issues with `linux` label for platform-specific problems

---

**Happy deploying on Linux! 🐧🚀**
