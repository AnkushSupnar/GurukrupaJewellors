#\!/bin/bash

# Install OpenJDK 17 (easiest method)
echo "Installing OpenJDK 17..."
sudo apt update
sudo apt install -y openjdk-17-jdk

# Set JAVA_HOME
echo "Setting up JAVA_HOME..."
echo 'export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64' >> ~/.bashrc
echo 'export PATH=$JAVA_HOME/bin:$PATH' >> ~/.bashrc

# Apply changes
source ~/.bashrc

# Verify installation
echo "Java version:"
java -version
echo ""
echo "JAVA_HOME: $JAVA_HOME"
