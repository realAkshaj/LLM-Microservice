# LLM Conversational Agent with AWS Lambda and Ollama

This project implements a conversational agent system that combines AWS Bedrock (via Lambda) and Ollama to create an interactive chat experience. The system uses a microservices architecture with Akka HTTP for the server implementation and supports Docker deployment.

Video link - https://youtu.be/zWOe9C6i96g
A brief overview of the project

## Architecture Overview

The system consists of several key components:
1. **Akka HTTP Server**: Handles incoming requests and manages the conversation flow
2. **AWS Lambda Integration**: Processes queries using Amazon Bedrock's Titan Text Lite model
3. **Ollama Integration**: Local LLM service for generating follow-up responses
4. **Conversation Logger**: Records all interactions in timestamped files

## Prerequisites

- Java 11 or later
- Scala 2.13.10
- SBT
- Docker and Docker Compose
- AWS Account with configured credentials
- Ollama installed locally (for development)

## Environment Variables

The following environment variables need to be set:
```
AWS_ACCESS_KEY_ID=your_access_key
AWS_SECRET_ACCESS_KEY=your_secret_key
AWS_REGION=your_region (e.g., us-east-1)
```

## Local Development Setup

1. Clone the repository

2. Install Ollama:
   ```bash
   # Follow Ollama installation instructions for your OS
   # After installation, pull the required model:
   ollama pull llama3.2:1b
   ```

3. Build the project:
   ```bash
   sbt clean compile
   ```

4. Run the application:
   ```bash
   sbt run
   ```

The server will start on `http://localhost:8080`

## Docker Deployment

1. Build the Docker image:
   ```bash
   docker build -t llm-microservice .
   ```

2. Run with Docker Compose:
   ```bash
   docker-compose up
   ```

This will start both the application server and Ollama service in containers.

## API Usage

Send POST requests to `/conversation` endpoint:

```bash
curl -X POST http://localhost:8080/conversation -H "Content-Type: application/json" -d "{\"text\": \"How do cats express love?\"}"'
```

The system will:
1. Process the initial query through AWS Bedrock
2. Generate a follow-up response using Ollama
3. Continue the conversation for up to 5 turns
4. Log all interactions

## Conversation Logs

Conversations are automatically logged to:
- Local development: `~/conversations/`
- Docker: mounted volume `conversation_logs`

Each conversation is saved in a timestamped file with the format:
```
conversation_YYYY-MM-DD_HH-mm-ss.txt
```

## AWS Lambda Setup

The Lambda function (`lambda_function.py`) is configured to use Amazon Bedrock's Titan Text Lite model. Ensure you have:
1. Created the Lambda function in AWS
2. Set up appropriate IAM roles with Bedrock access
3. Configured the API Gateway for the Lambda function

## Architecture Details

- **Main.scala**: Core application logic, HTTP server, and conversation management
- **ConversationLogger.scala**: Handles conversation persistence
- **lambda_function.py**: AWS Lambda implementation for Bedrock integration
- **Docker** and **Docker Compose** files for containerization

## Error Handling

The system implements robust error handling:
- Automatic retries for model availability checks
- Graceful degradation on service failures
- Comprehensive logging for debugging
- Timeout configurations for long-running conversations

## Limitations

- Maximum 5 conversation turns per session
- Response timeouts set to 5 minutes
- Requires stable connection to AWS services
- Ollama must be available for follow-up responses

## Troubleshooting

1. **Ollama Connection Issues**:
   - Check if Ollama service is running
   - Verify model is downloaded
   - Check logs in Docker Compose

2. **AWS Connectivity**:
   - Verify AWS credentials
   - Check Lambda function logs
   - Ensure proper IAM permissions

3. **Server Issues**:
   - Check application logs
   - Verify port availability
   - Monitor resource usage

## Running Tests

```bash
sbt test
```

## Detailed Docker Installation and Setup

### Prerequisites
- An AWS EC2 instance running Amazon Linux 2
- AWS CLI installed
- AWS credentials with ECR access
- Open port 8080 in your security group

### Step-by-Step Installation

1. **Update System and Install Docker**
   ```bash
   # Update the package manager
   sudo yum update -y
   
   # Install Docker
   sudo yum install docker -y
   
   # Start Docker service
   sudo service docker start
   
   # Add your user to docker group (requires re-login to take effect)
   sudo usermod -a -G docker ec2-user
   ```

2. **Install Docker Compose**
   ```bash
   # Download Docker Compose binary
   sudo curl -L "https://github.com/docker/compose/releases/download/v2.24.1/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
   
   # Make it executable
   sudo chmod +x /usr/local/bin/docker-compose
   
   # Enable Docker service on boot
   sudo systemctl start docker
   sudo systemctl enable docker
   ```

3. **Configure AWS Credentials**
   ```bash
   aws configure
   ```
   You'll be prompted to enter:
   - AWS Access Key ID
   - AWS Secret Access Key
   - Default region (enter `us-east-1`)
   - Output format (enter `json`)

4. **Login to Amazon ECR**
   ```bash
   aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin 637423449380.dkr.ecr.us-east-1.amazonaws.com
   ```

5. **Create and Setup Project Directory**
   ```bash
   mkdir llm-service
   cd llm-service
   ```

6. **Deploy the Application**
   ```bash
   # Pull the latest images
   docker-compose pull
   
   # Start the services in detached mode
   docker-compose up -d
   
   # Verify containers are running
   docker ps
   
   # Check application logs
   docker logs llm-microservice
   ```

### Testing the Deployment

You can test the deployment using curl. Replace `YOUR_EC2_PUBLIC_IP` with your EC2 instance's public DNS or IP:

```bash
# Using IP address
curl -X POST http://YOUR_EC2_PUBLIC_IP:8080/conversation ^
  -H "Content-Type: application/json" ^
  -d '{"text":"How do cats express love?"}'

# Using EC2 public DNS (example)
curl -X POST http://ec2-18-212-178-224.compute-1.amazonaws.com:8080/conversation ^
  -H "Content-Type: application/json" ^
  -d '{"text": "How do cats express love?"}'
```

### Troubleshooting Docker Installation

1. **If Docker fails to start:**
   ```bash
   sudo systemctl status docker
   # Check logs for issues
   sudo journalctl -u docker
   ```

2. **If permission issues occur:**
   ```bash
   # Re-login to refresh group membership
   exit
   # SSH back in
   ```

3. **If ECR login fails:**
   - Verify AWS credentials
   - Check IAM permissions
   - Ensure correct region is set

4. **If containers fail to start:**
   ```bash
   # Check detailed container logs
   docker-compose logs
   
   # Check individual service logs
   docker-compose logs server
   docker-compose logs ollama
   ```

5. **Network Issues:**
   - Verify security group settings
   - Check if port 8080 is open
   - Ensure no firewall is blocking traffic

### Stopping the Application

```bash
# Stop the services
docker-compose down

# To also remove volumes
docker-compose down -v
```

# Docker Containerization and AWS Deployment Implementation

## 1. Application Containerization
I successfully containerized both parts of the application:

### Server Component
- Created a multi-stage Dockerfile utilizing `hseeberger/scala-sbt` for building and `eclipse-temurin:17-jre-alpine` for runtime
- Optimized image size by:
  - Using Alpine-based images
  - Implementing multi-stage build to exclude build tools from final image
  - Only copying necessary files
- Exposed port 8080 for communication
- Included all required dependencies through proper SBT configuration

### Service Component (Ollama)
- Utilized official Ollama image
- Implemented health checks for reliability
- Configured volume mounting for model persistence
- Exposed port 11434 for internal communication

## 2. AWS Deployment
Successfully deployed the application to AWS:
- Server image pushed to ECR repository: `637423449380.dkr.ecr.us-east-1.amazonaws.com/llm-microservice`
- Implemented proper AWS credential management through environment variables
- The server component communicates with AWS Lambda for text generation
- Ollama service runs alongside the main service for local LLM processing

## 3. Networking
Implemented comprehensive networking setup:
- Created Docker Compose configuration linking all services
- Configured internal network for container communication
- Set up volume sharing for logs and model data
- Implemented health checks to ensure service availability

## 4. Testing and Verification
The application demonstrates successful integration:
- Server successfully processes incoming HTTP requests
- Lambda integration works for text generation
- Ollama service provides local LLM capabilities
- Conversation logs are properly stored
- All components communicate seamlessly

## 5. Documentation and Code Quality
- Provided detailed Dockerfile and docker-compose.yml
- Implemented comprehensive logging
- Created detailed setup instructions
- Used best practices for container configuration
- Optimized image sizes through multi-stage builds

## 6. Additional Features
- Implemented conversation logging
- Added robust error handling
- Created health check mechanisms
- Configured proper timeout handling
- Added AWS credential management

## Challenges and Solutions
1. **Timeout Issues**
   - Solution: Implemented proper timeout configurations
   - Added retry mechanisms for model loading

2. **Container Communication**
   - Solution: Created proper networking configuration
   - Implemented service dependencies

3. **AWS Integration**
   - Solution: Used environment variables for credentials
   - Implemented proper error handling for AWS services

4. **Image Optimization**
   - Solution: Used multi-stage builds
   - Implemented Alpine-based images

## Conclusion
The implementation successfully meets all requirements by providing a containerized, scalable solution that effectively integrates AWS services with local LLM capabilities while maintaining proper separation of concerns and following Docker best practices.
## License

This project is licensed under the MIT License.
