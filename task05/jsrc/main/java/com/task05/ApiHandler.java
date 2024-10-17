package com.task05;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.model.PutItemResult;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.*;
import com.amazonaws.services.lambda.runtime.events.models.dynamodb.AttributeValue;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.syndicate.deployment.annotations.environment.EnvironmentVariable;
import com.syndicate.deployment.annotations.environment.EnvironmentVariables;
import com.syndicate.deployment.annotations.lambda.LambdaHandler;
import com.syndicate.deployment.model.ResourceType;
import com.syndicate.deployment.model.RetentionSetting;
import com.syndicate.deployment.annotations.resources.DependsOn;
import com.syndicate.deployment.annotations.events.DynamoDbTriggerEventSource;



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
@DynamoDbTriggerEventSource(targetTable = "Events", batchSize = 1)
@DependsOn(name = "Events", resourceType = ResourceType.DYNAMODB_TABLE)
@EnvironmentVariables(value = {
		@EnvironmentVariable(key = "region", value = "${region}"),
		@EnvironmentVariable(key = "target_table", value = "${target_table}")
})
public class ApiHandler implements RequestHandler<APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse> {
	private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
	private final AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard()
			.withRegion("us-east-1")
			.build();
	private final DynamoDB dynamoDB = new DynamoDB(client);

	@Override
	public APIGatewayV2HTTPResponse handleRequest(APIGatewayV2HTTPEvent event, Context context) {
		if ("POST".equals(event.getRequestContext().getHttp().getMethod()) && "/events".equals(event.getRawPath())) {
			EventRequest eventRequest = gson.fromJson(event.getBody(), EventRequest.class);
			String id = UUID.randomUUID().toString();
			String createdAt = java.time.Instant.now().toString();

			Item item = new Item().withPrimaryKey("id", id)
					.withString("createdAt", createdAt)
					.withInt("principalId", eventRequest.getPrincipalId())
					.withMap("body", eventRequest.getContent());

			Table table = dynamoDB.getTable("Events");
			try {
				table.putItem(item);
				return buildResponse(201, gson.toJson(Map.of("event", item.toJSON())));
			} catch (Exception e) {
				return buildResponse(500, "Internal Server Error: " + e.getMessage());
			}
		} else {
			return buildResponse(400, "Bad request syntax or unsupported method");
		}
	}

	private APIGatewayV2HTTPResponse buildResponse(int statusCode, String message) {
		Map<String, String> headers = new HashMap<>();
		headers.put("Content-Type", "application/json");

		return APIGatewayV2HTTPResponse.builder()
				.withStatusCode(statusCode)
				.withHeaders(headers)
				.withBody(message)
				.build();
	}
}
