package com.task10.handler;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import org.json.JSONObject;

public class PostTablesHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final AmazonDynamoDB dynamoDbClient;

    public PostTablesHandler(AmazonDynamoDB dynamoDbClient) {
        this.dynamoDbClient = dynamoDbClient;
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent requestEvent, Context context) {
        try {
            String authorizationHeader = requestEvent.getHeaders().get("Authorization");
            if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
                throw new IllegalArgumentException("Missing or invalid Authorization header.");
            }

            String accessToken = authorizationHeader.substring("Bearer ".length());
            // Optionally validate the token with Cognito

            JSONObject requestBody = new JSONObject(requestEvent.getBody());
            int id = requestBody.getInt("id");
            int number = requestBody.getInt("number");
            int places = requestBody.getInt("places");
            boolean isVip = requestBody.getBoolean("isVip");
            Integer minOrder = requestBody.has("minOrder") ? requestBody.getInt("minOrder") : null;

            DynamoDB dynamoDB = new DynamoDB(dynamoDbClient);
            Table table = dynamoDB.getTable(System.getenv("tables_table"));
            Item item = new Item()
                    .withPrimaryKey("id", id)
                    .withNumber("number", number)
                    .withNumber("places", places)
                    .withBoolean("isVip", isVip);
            if (minOrder != null) {
                item.withNumber("minOrder", minOrder);
            }
            table.putItem(item);

            JSONObject responseBody = new JSONObject();
            responseBody.put("id", id);

            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(200)
                    .withBody(responseBody.toString());
        } catch (Exception e) {
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(400)
                    .withBody(new JSONObject().put("error", "Failed to process request: " + e.getMessage()).toString());
        }
    }

    private void validateAccessToken(String accessToken) {
        // Implement token validation logic here
    }
}
