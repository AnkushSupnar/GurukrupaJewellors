#\!/bin/bash

# Update package list
sudo apt update

# Install required packages
sudo apt install -y wget apt-transport-https

# Create directory for Oracle Java
sudo mkdir -p /usr/lib/jvm

# Download Oracle JDK 17 (you may need to accept license on Oracle website first)
# Visit: https://www.oracle.com/java/technologies/downloads/#java17
echo "Please download Oracle JDK 17 tar.gz from Oracle website and place it in current directory"
echo "Download link: https://www.oracle.com/java/technologies/downloads/#java17"
echo "Choose: Linux x64 Compressed Archive (tar.gz)"
echo ""
echo "After downloading, run these commands:"
echo ""
echo "# Extract Oracle JDK"
echo "sudo tar -xzf jdk-17_linux-x64_bin.tar.gz -C /usr/lib/jvm/"
echo ""
echo "# Set up environment variables"
echo "sudo update-alternatives --install /usr/bin/java java /usr/lib/jvm/jdk-17.0.*/bin/java 1"
echo "sudo update-alternatives --install /usr/bin/javac javac /usr/lib/jvm/jdk-17.0.*/bin/javac 1"
echo ""
echo "# Configure JAVA_HOME permanently"
echo "echo 'export JAVA_HOME=/usr/lib/jvm/jdk-17.0.13' >> ~/.bashrc"
echo "echo 'export PATH=\$JAVA_HOME/bin:\$PATH' >> ~/.bashrc"
echo "source ~/.bashrc"
echo ""
echo "# Verify installation"
echo "java -version"
