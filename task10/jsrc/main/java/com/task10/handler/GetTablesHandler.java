package com.task10.handler;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.spec.ScanSpec;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;

public class GetTablesHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayV2HTTPResponse> {

    private final Gson gson = new Gson();
    private final AmazonDynamoDB dynamoDbClient;

    public GetTablesHandler(AmazonDynamoDB dynamoDbClient) {
        this.dynamoDbClient = dynamoDbClient;
    }

    @Override
    public APIGatewayV2HTTPResponse handleRequest(APIGatewayProxyRequestEvent requestEvent, Context context) {
        try {


            DynamoDB dynamoDB = new DynamoDB(dynamoDbClient);
            Table table = dynamoDB.getTable(System.getenv("tables_table"));
            ScanSpec scanSpec = new ScanSpec(); // Customize scan with filtering if needed

            Iterator<Item> items = table.scan(scanSpec).iterator();
            List<Map<String, Object>> tables = new ArrayList<>();

            while (items.hasNext()) {
                Item item = items.next();
                Map<String, Object> tableData = new HashMap<>();
                tableData.put("id", item.getString("id"));
                tableData.put("number", item.getInt("number"));
                tableData.put("places", item.getInt("places"));
                tableData.put("isVip", item.getBoolean("isVip"));
                if (item.isPresent("minOrder")) {
                    tableData.put("minOrder", item.getInt("minOrder"));
                }
                tables.add(tableData);
            }


            return buildResponse(200, Collections.singletonMap("tables", tables));
        } catch (Exception e) {
            return buildResponse(400, Collections.singletonMap("error", "Failed to process request: " + e.getMessage()));
        }
    }

    private APIGatewayV2HTTPResponse buildResponse(int statusCode, Map<String, Object> response) {
        return APIGatewayV2HTTPResponse.builder()
                .withStatusCode(statusCode)
                .withHeaders(Collections.singletonMap("Content-Type", "application/json"))
                .withBody(gson.toJson(response))
                .build();
    }

}
