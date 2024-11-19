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
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Iterator;

public class GetTablesHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final AmazonDynamoDB dynamoDbClient;

    public GetTablesHandler(AmazonDynamoDB dynamoDbClient) {
        this.dynamoDbClient = dynamoDbClient;
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent requestEvent, Context context) {
        try {


            DynamoDB dynamoDB = new DynamoDB(dynamoDbClient);
            Table table = dynamoDB.getTable(System.getenv("tables_table"));
            ScanSpec scanSpec = new ScanSpec(); // Customize scan with filtering if needed
            Iterator<Item> items = table.scan(scanSpec).iterator();

            JSONArray jsonArray = new JSONArray();
            while (items.hasNext()) {
                Item item = items.next();
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("id", item.getString("id"));
                jsonObject.put("number", item.getInt("number"));
                jsonObject.put("places", item.getInt("places"));
                jsonObject.put("isVip", item.getBoolean("isVip"));
                if (item.isPresent("minOrder")) {
                    jsonObject.put("minOrder", item.getInt("minOrder"));
                }
                jsonArray.put(jsonObject);
            }

            JSONObject responseBody = new JSONObject().put("tables", jsonArray);

            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(200)
                    .withBody(responseBody.toString());
        } catch (Exception e) {
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(400)
                    .withBody(new JSONObject().put("error", "Failed to process request: " + e.getMessage()).toString());
        }
    }

}
