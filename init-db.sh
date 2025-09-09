#!/bin/bash

# Database initialization script for Real Estate POC

echo "Initializing database..."

# Wait for PostgreSQL to be ready
echo "Waiting for PostgreSQL to be ready..."
until pg_isready -h localhost -p 5432 -U realestate; do
  echo "PostgreSQL is unavailable - sleeping"
  sleep 1
done

echo "PostgreSQL is ready!"

# Create database if it doesn't exist
echo "Creating database if it doesn't exist..."
psql -h localhost -p 5432 -U realestate -d postgres -c "CREATE DATABASE realestate;" 2>/dev/null || echo "Database already exists"

# Run schema
echo "Running schema..."
psql -h localhost -p 5432 -U realestate -d realestate -f src/main/resources/schema.sql

echo "Database initialization complete!"

