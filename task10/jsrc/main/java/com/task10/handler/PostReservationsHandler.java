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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.task10.dto.ReservationData;
import org.json.JSONObject;

import java.util.UUID;

public class PostReservationsHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private final AmazonDynamoDB dynamoDbClient;

    public PostReservationsHandler(AmazonDynamoDB dynamoDbClient) {
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

            ObjectMapper mapper = new ObjectMapper();
            ReservationData reservationData = mapper.readValue(requestEvent.getBody(), ReservationData.class);

            String reservationId = UUID.randomUUID().toString();
            DynamoDB dynamoDB = new DynamoDB(dynamoDbClient);
            Table table = dynamoDB.getTable(System.getenv("reservations_table"));

            Item item = new Item()
                    .withPrimaryKey("reservationId", reservationId)
                    .withNumber("tableNumber", reservationData.getTableNumber())
                    .withString("clientName", reservationData.getClientName())
                    .withString("phoneNumber", reservationData.getPhoneNumber())
                    .withString("date", reservationData.getDate())
                    .withString("slotTimeStart", reservationData.getSlotTimeStart())
                    .withString("slotTimeEnd", reservationData.getSlotTimeEnd());

            table.putItem(item);

            JSONObject responseBody = new JSONObject()
                    .put("reservationId", reservationId);

            response.withStatusCode(200)
                    .withBody(responseBody.toString());
        } catch (Exception e) {
            response.withStatusCode(400)
                    .withBody(new JSONObject().put("error", "Failed to process request: " + e.getMessage()).toString());
        }

        return response;
    }


}
