# Start with a lightweight Java runtime
FROM eclipse-temurin:21-jdk-alpine

# Set working directory
WORKDIR /app

# Add JAR (you’ll build it before running docker-compose)
COPY build/libs/*.jar app.jar

# Expose port (match your application port)
EXPOSE 8080

# Run the jar
ENTRYPOINT ["java", "-jar", "app.jar"]
