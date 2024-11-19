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
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Iterator;

public class GetReservationsHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final AmazonDynamoDB dynamoDbClient;

    public GetReservationsHandler(AmazonDynamoDB dynamoDbClient) {
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

            DynamoDB dynamoDB = new DynamoDB(dynamoDbClient);
            Table table = dynamoDB.getTable(System.getenv("reservations_table"));

            Iterator<Item> items = table.scan().iterator();
            JSONArray reservationsArray = new JSONArray();

            while (items.hasNext()) {
                Item item = items.next();
                JSONObject reservationObject = new JSONObject()
                        .put("tableNumber", item.getInt("tableNumber"))
                        .put("clientName", item.getString("clientName"))
                        .put("phoneNumber", item.getString("phoneNumber"))
                        .put("date", item.getString("date"))
                        .put("slotTimeStart", item.getString("slotTimeStart"))
                        .put("slotTimeEnd", item.getString("slotTimeEnd"));
                reservationsArray.put(reservationObject);
            }

            JSONObject responseBody = new JSONObject()
                    .put("reservations", reservationsArray);

            response.withStatusCode(200)
                    .withBody(responseBody.toString());
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
