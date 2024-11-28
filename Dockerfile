# Build stage
FROM hseeberger/scala-sbt:17.0.2_1.6.2_2.13.8 as builder

WORKDIR /app

# Copy project dependencies first for better caching
COPY project ./project
COPY build.sbt .

# Copy source files and protobuf definitions
COPY src ./src

# Build the application with assembly
RUN sbt clean assembly

# Runtime stage
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Install required packages for gRPC
RUN apk add --no-cache libc6-compat

# Copy the assembled jar from builder stage
COPY --from=builder /app/target/scala-2.13/bedrock-grpc-client-assembly-*.jar ./app.jar

# Create directory for conversations
RUN mkdir -p /root/conversations

# Expose the port the app runs on
EXPOSE 8080

# Set environment variables (can be overridden at runtime)
ENV JAVA_OPTS="-Xmx512m"

# Command to run the application
# Using shell form to allow environment variable expansion
CMD java $JAVA_OPTS -jar app.jar