#!/bin/bash

echo "🚀 Starting Deployment..."

# Step 1: Pull the latest code from Git
echo "🔄 Pulling latest code from Git..."
git pull origin main

# Step 2: Build the project
echo "🔨 Building the project..."
mvn clean install -DskipTests

# Step 3: Stop the process running on port 8080
echo "🛑 Stopping any running instance on port 8080..."
PID=$(lsof -t -i:8080)
if [ ! -z "$PID" ]; then
    kill -9 $PID
    echo "✅ Process on port 8080 stopped."
else
    echo "❌ No process found on port 8080."
fi

# Step 4: Start the application
echo "🚀 Starting the application..."
nohup java -jar target/letscheck-0.0.1-SNAPSHOT.jar > output.log 2>&1 &

# Step 5: Monitor logs continuously
echo "📜 Tailing logs..."
tail -f output.log