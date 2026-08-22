# Stage 1: Build the application
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Production Container
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Create a non-root group and user
RUN addgroup -S lmsgroup && adduser -S lmsuser -G lmsgroup

# Copy application jar from builder stage
COPY --from=builder /app/target/library-management-system-1.0.0.jar app.jar

USER lmsuser:lmsgroup

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
