package com.task10.handler;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.*;
import com.amazonaws.services.dynamodbv2.document.spec.QuerySpec;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.task10.dto.ReservationData;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONObject;

import java.util.UUID;

public class PostReservationsHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final Log log = LogFactory.getLog(PostReservationsHandler.class);
    private final AmazonDynamoDB dynamoDbClient;

    public PostReservationsHandler(AmazonDynamoDB dynamoDbClient) {
        this.dynamoDbClient = dynamoDbClient;
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent requestEvent, Context context) {
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();

        try {
            ObjectMapper mapper = new ObjectMapper();
            log.info("Post reservations request received: " + requestEvent.getBody());
            ReservationData reservationData = mapper.readValue(requestEvent.getBody(), ReservationData.class);

            DynamoDB dynamoDB = new DynamoDB(dynamoDbClient);
            Table table = dynamoDB.getTable(System.getenv("tables_table"));

            // Sprawdzamy czy stół istnieje
            Item tableItem = table.getItem("id", reservationData.getTableNumber());
            if (tableItem == null) {
                throw new Exception("Table does not exist");
            }

            // Sprawdzamy kolidujące rezerwacje
            Table reservationsTable = dynamoDB.getTable(System.getenv("reservations_table"));
            QuerySpec querySpec = new QuerySpec()
                    .withKeyConditionExpression("tableNumber = :v_id and slotTimeStart <= :v_end and slotTimeEnd >= :v_start")
                    .withValueMap(new ValueMap()
                            .withNumber(":v_id", reservationData.getTableNumber())
                            .withString(":v_start", reservationData.getSlotTimeStart())
                            .withString(":v_end", reservationData.getSlotTimeEnd()));
            ItemCollection<QueryOutcome> items = reservationsTable.query(querySpec);
            if (items.iterator().hasNext()) {
                throw new Exception("Reservation conflicts with an existing reservation.");
            }

            String reservationId = UUID.randomUUID().toString();

            Item item = new Item()
                    .withPrimaryKey("id", reservationId)
                    .withNumber("tableNumber", reservationData.getTableNumber())
                    .withString("clientName", reservationData.getClientName())
                    .withString("phoneNumber", reservationData.getPhoneNumber())
                    .withString("date", reservationData.getDate())
                    .withString("slotTimeStart", reservationData.getSlotTimeStart())
                    .withString("slotTimeEnd", reservationData.getSlotTimeEnd());

            reservationsTable.putItem(item);

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
