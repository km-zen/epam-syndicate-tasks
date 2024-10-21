package com.task05;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import com.google.gson.Gson;
import com.syndicate.deployment.annotations.environment.EnvironmentVariable;
import com.syndicate.deployment.annotations.environment.EnvironmentVariables;
import com.syndicate.deployment.annotations.lambda.LambdaHandler;
import com.syndicate.deployment.annotations.lambda.LambdaUrlConfig;
import com.syndicate.deployment.model.RetentionSetting;
import com.syndicate.deployment.model.lambda.url.AuthType;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@LambdaHandler(
        lambdaName = "api_handler",
        roleName = "api_handler-role",
        isPublishVersion = true,
        aliasName = "${lambdas_alias_name}",
        logsExpiration = RetentionSetting.SYNDICATE_ALIASES_SPECIFIED
)
@EnvironmentVariables(value = {
        @EnvironmentVariable(key = "region", value = "${region}"),
        @EnvironmentVariable(key = "target_table", value = "${target_table}")
})
@LambdaUrlConfig(
        authType = AuthType.NONE
)
public class ApiHandler implements RequestHandler<APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse> {
    private static final Log log = LogFactory.getLog(ApiHandler.class);
    private final AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard()
            .withRegion(System.getenv("region"))
            .build();
    private final DynamoDB dynamoDB = new DynamoDB(client);
    private final Gson gson = new Gson();

    @Override
    public APIGatewayV2HTTPResponse handleRequest(APIGatewayV2HTTPEvent requestEvent, Context context) {
        log.info("Start");
        log.info(requestEvent.getBody());

        String id = UUID.randomUUID().toString();
        String createdAt = java.time.Instant.now().toString();

        EventRequest eventRequest = gson.fromJson(requestEvent.getBody(), EventRequest.class);
        int principalId = eventRequest.getPrincipalId();
        Map<String, String> body = eventRequest.getContent();
        log.info(principalId);
        log.info(eventRequest.getContent());
        Item item = new Item().withPrimaryKey("id", id)
                .withNumber("principalId", principalId)
                .with("createdAt", createdAt)
                .withMap("body", body);
        Table table = dynamoDB.getTable(System.getenv("target_table"));
        table.putItem(item);
        EventResponse event = new EventResponse(id, principalId, createdAt, body);

        Map<String, Object> eventMap = gson.fromJson(gson.toJson(event), Map.class);
        log.info(eventMap);

        log.info(buildResponse(201, eventMap));
        return buildResponse(201, eventMap);
    }

    private APIGatewayV2HTTPResponse buildResponse(int statusCode, Map<String, Object> event) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("statusCode", statusCode);
        responseBody.put("event", event);

        return APIGatewayV2HTTPResponse.builder()
                .withStatusCode(statusCode)
                .withHeaders(headers)
                .withBody(gson.toJson(responseBody))
                .build();
    }
}
