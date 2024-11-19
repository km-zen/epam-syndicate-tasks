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

public class GetTableByIdHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final AmazonDynamoDB dynamoDbClient;

    public GetTableByIdHandler(AmazonDynamoDB dynamoDbClient) {
        this.dynamoDbClient = dynamoDbClient;
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent requestEvent, Context context) {
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();

        try {
            String authorizationHeader = requestEvent.getHeaders().get("Authorization");
            if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
                throw new IllegalArgumentException("Missing or invalid Authorization header.");
            }

            String accessToken = authorizationHeader.substring("Bearer ".length());
            // Optionally validate the token with Cognito

            String tableId = requestEvent.getPathParameters().get("tableId");
            DynamoDB dynamoDB = new DynamoDB(dynamoDbClient);
            Table table = dynamoDB.getTable(System.getenv("tables_table"));

            Item item = table.getItem("id", Integer.parseInt(tableId));
            if (item == null) {
                throw new Exception("Table not found for ID: " + tableId);
            }

            JSONObject responseBody = new JSONObject()
                    .put("id", item.getInt("id"))
                    .put("number", item.getInt("number"))
                    .put("places", item.getInt("places"))
                    .put("isVip", item.getBoolean("isVip"));

            if (item.isPresent("minOrder")) {
                responseBody.put("minOrder", item.getInt("minOrder"));
            }

            response.withStatusCode(200).withBody(responseBody.toString());
        } catch (NumberFormatException e) {
            response.withStatusCode(400)
                    .withBody(new JSONObject().put("error", "Invalid table ID format").toString());
        } catch (IllegalArgumentException e) {
            response.withStatusCode(400)
                    .withBody(new JSONObject().put("error", e.getMessage()).toString());
        } catch (Exception e) {
            response.withStatusCode(400)
                    .withBody(new JSONObject().put("error", "Failed to process request: " + e.getMessage()).toString());
        }

        return response;
    }
}
