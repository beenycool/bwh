#!/bin/bash

# Setup script for BedWars Mod
echo "Setting up Forge 1.8.9 mod environment..."

# Create folder for downloads
mkdir -p downloads
cd downloads

# Download Forge installer
echo "Downloading Forge installer..."
curl -o forge-installer.jar https://maven.minecraftforge.net/net/minecraftforge/forge/1.8.9-11.15.1.2318-1.8.9/forge-1.8.9-11.15.1.2318-1.8.9-installer.jar

# Return to main directory
cd ..

# Set up gradle wrapper
echo "Setting up Gradle wrapper..."
mkdir -p gradle/wrapper

# Use Gradle 4.10.3 which works with Java 11
cat > gradle/wrapper/gradle-wrapper.properties << EOF
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-4.10.3-bin.zip
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
EOF

curl -o gradle/wrapper/gradle-wrapper.jar https://raw.githubusercontent.com/gradle/gradle/v4.10.3/gradle/wrapper/gradle-wrapper.jar
curl -o gradlew https://raw.githubusercontent.com/gradle/gradle/v4.10.3/gradlew
curl -o gradlew.bat https://raw.githubusercontent.com/gradle/gradle/v4.10.3/gradlew.bat
chmod +x gradlew

# Create gradle properties file
echo "Creating gradle properties file..."
cat > gradle.properties << EOF
org.gradle.jvmargs=-Xmx3G
org.gradle.daemon=false
EOF

echo "Setup complete! You can now build your mod with './gradlew build'"