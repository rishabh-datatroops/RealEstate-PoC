#!/bin/bash

# Real Estate POC Startup Script

echo "Starting Real Estate POC..."
echo "=========================="

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo "Error: Docker is not running. Please start Docker first."
    exit 1
fi

# Start infrastructure
echo "1. Starting infrastructure (PostgreSQL, Kafka, Zookeeper)..."
docker-compose up -d

# Wait for services to be ready
echo "2. Waiting for services to be ready..."
sleep 30

# Initialize database
echo "3. Initializing database..."
./init-db.sh

# Run tests
echo "4. Running tests..."
sbt test

# Start the application
echo "5. Starting the application..."
echo "The application will be available at http://localhost:8080"
echo "Press Ctrl+C to stop the application"
echo ""

sbt run

