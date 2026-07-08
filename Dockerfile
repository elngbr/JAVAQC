# Multi-stage Dockerfile for JavaQC - Quantum Computing in Java
# Build stage: compile with Maven
FROM maven:3.9.11-eclipse-temurin-25 AS builder

WORKDIR /app
COPY pom.xml .
COPY src ./src

# Build the JAR without running tests
RUN mvn -q -DskipTests clean package

# Runtime stage: minimal JRE with the compiled JAR
FROM eclipse-temurin:25-jre-alpine

WORKDIR /app

# Copy the compiled fat JAR from builder (includes all dependencies)
COPY --from=builder /app/target/javaqc.jar app.jar

# Create a non-root user for security
RUN addgroup -S quantum && adduser -S quantum -G quantum
USER quantum

# Set environment variables for cloud providers (empty by default)
ENV IBM_QUANTUM_TOKEN=""
ENV PASQAL_API_KEY=""
ENV PASQAL_DEVICE_ID="default"
ENV QMWARE_API_KEY=""
ENV DWAVE_API_KEY=""
ENV DWAVE_SOLVER_ID="DW_2000Q_6"
ENV XANADU_API_KEY=""
ENV XANADU_DEVICE="strawberryfields.fock"
ENV GOOGLE_API_KEY=""
ENV GOOGLE_DEVICE_ID="simulator-google"
ENV AZURE_QUANTUM_SUBSCRIPTION_ID=""
ENV AZURE_QUANTUM_RESOURCE_GROUP=""
ENV AZURE_QUANTUM_WORKSPACE_NAME=""
ENV AZURE_QUANTUM_API_KEY=""

# Health check: verify the app can print help
HEALTHCHECK --interval=30s --timeout=10s --start-period=5s --retries=3 \
    CMD java -jar app.jar --format plain 2>/dev/null | head -1 || exit 1

# Default entrypoint: run TextDemo CLI
ENTRYPOINT ["java", "-jar", "app.jar"]
CMD ["--help"]
