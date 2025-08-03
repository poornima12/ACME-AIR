#!/bin/bash

echo "🚀 Starting ACME-AIR Airline Reservation System..."
echo ""

# Start the services
echo "📦 Starting Docker containers..."
docker-compose up -d

# Wait for the application to be ready
echo "⏳ Waiting for application to start and create tables..."
echo "   This may take 30-60 seconds..."

# Check if app is ready
until curl -s http://localhost:8080/actuator/health >/dev/null 2>&1; do
    echo "   Still starting... (checking again in 5 seconds)"
    sleep 5
done

echo "✅ Application is ready!"

# Load sample data
echo "📊 Loading sample flight data..."
docker exec -i acme-air-db psql -U user -d acmeair < scripts/sample-data.sql

echo ""
echo "🎉 Setup complete! Your airline reservation system is ready."
echo ""
echo "📋 API Documentation: http://localhost:8080/swagger-ui/index.html"
echo "🗄️  Database: PostgreSQL running on localhost:5432"
echo ""
echo "🧪 Quick test:"
echo "curl --location 'http://localhost:8080/api/v1/flights/search?origin=AKL&destination=SYD&departureDate=2025-12-25&passengers=2%0A' \
      --header 'accept: application/json'"
echo ""
echo "📝 To stop: docker-compose down"