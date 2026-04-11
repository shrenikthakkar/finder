#!/bin/bash

echo "📦 Updating packages..."
sudo yum update -y


# === Java 21 ===
echo "☕ Checking Java..."
if type -p java >/dev/null 2>&1; then
  JAVA_VER=$(java -version 2>&1 | head -n 1)
  echo "✅ Java already installed: $JAVA_VER"
else
  echo "⬇️ Installing Java 21..."
  sudo yum install -y java-21-amazon-corretto-devel
  JAVA_VER=$(java -version 2>&1 | head -n 1)
  echo "✅ Installed: $JAVA_VER"
fi


# === Maven ===
echo "🛠️ Checking Maven..."
if type -p mvn >/dev/null 2>&1; then
  MVN_VER=$(mvn -version 2>/dev/null | head -n 1)
  echo "✅ Maven already installed: $MVN_VER"
else
  echo "⬇️ Installing Maven..."
  sudo yum install -y maven
  MVN_VER=$(mvn -version 2>/dev/null | head -n 1)
  echo "✅ Installed: $MVN_VER"
fi

# === Git ===
echo "🔧 Checking Git..."
if type -p git >/dev/null 2>&1; then
  GIT_VER=$(git --version)
  echo "✅ Git already installed: $GIT_VER"
else
  echo "⬇️ Installing Git..."
  sudo yum install -y git
  GIT_VER=$(git --version)
fi


# === Final Summary ===
echo ""
echo "📊 === Installed Software Summary ==="
echo "Java:     $JAVA_VER"
echo "Maven:    $MVN_VER"
echo "Git:      $GIT_VER"
echo ""
echo "🎉✅✅✅ EC2 instance is fully ready for Spring Boot deployment!"
