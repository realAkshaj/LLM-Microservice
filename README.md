# LLM Conversational Agent with AWS Lambda and Ollama

This project implements a conversational agent system that combines AWS Bedrock (via Lambda) and Ollama to create an interactive chat experience. The system uses a microservices architecture with Akka HTTP for the server implementation and supports Docker deployment.

## Architecture Overview

The system consists of several key components:
1. **Akka HTTP Server**: Handles incoming HTTP requests and manages the conversation flow
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
curl -X POST http://localhost:8080/conversation \
  -H "Content-Type: application/json" \
  -d '{"text": "How do cats express love?"}'
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

## License

This project is licensed under the MIT License.
