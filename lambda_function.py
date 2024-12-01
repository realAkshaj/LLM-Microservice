import boto3
import json
import logging

# Set up logging
logger = logging.getLogger()
logger.setLevel(logging.INFO)

def lambda_handler(event, context):
    # Log the incoming event for debugging
    logger.info(f"Received event: {json.dumps(event)}")

    try:
        # Extract query from the request body
        body = json.loads(event.get('body', '{}'))
        query = body.get('query')

        if not query:
            logger.error("No query found in request")
            return {
                'statusCode': 400,
                'headers': {
                    'Content-Type': 'application/json',
                    'Access-Control-Allow-Origin': '*'
                },
                'body': json.dumps({'error': 'Missing query parameter'})
            }

        # Initialize Bedrock client
        bedrock = boto3.client('bedrock-runtime')

        # Create the request body for Titan model
        request_body = {
            "inputText": query,
            "textGenerationConfig": {
                "maxTokenCount": 50,
                "temperature": 0.3,
                "topP": 1,
                "stopSequences": []
            }
        }

        # Call Bedrock Titan model
        logger.info(f"Sending request to Bedrock: {json.dumps(request_body)}")
        response = bedrock.invoke_model(
            modelId='amazon.titan-text-lite-v1',
            body=json.dumps(request_body),
            contentType='application/json',
            accept='application/json'
        )

        # Parse response
        response_body = json.loads(response.get('body').read())
        logger.info(f"Bedrock response: {response_body}")

        # Extract the generated text from Titan's response
        generated_text = response_body.get('results', [{}])[0].get('outputText', '')

        return {
            'statusCode': 200,
            'headers': {
                'Content-Type': 'application/json',
                'Access-Control-Allow-Origin': '*'
            },
            'body': json.dumps({
                'response': generated_text,
                'model': 'amazon.titan-text-lite-v1'
            })
        }

    except Exception as e:
        logger.error(f"Error processing request: {str(e)}")
        import traceback
        logger.error(traceback.format_exc())
        return {
            'statusCode': 500,
            'headers': {
                'Content-Type': 'application/json',
                'Access-Control-Allow-Origin': '*'
            },
            'body': json.dumps({
                'error': str(e)
            })
        }
