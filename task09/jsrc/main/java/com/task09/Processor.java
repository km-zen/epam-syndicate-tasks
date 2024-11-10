package com.task09;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
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
import com.syndicate.deployment.model.TracingMode;
import com.syndicate.deployment.model.lambda.url.AuthType;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@LambdaHandler(
    lambdaName = "processor",
	roleName = "processor-role",
	isPublishVersion = true,
	aliasName = "${lambdas_alias_name}",
	tracingMode = TracingMode.Active,
	logsExpiration = RetentionSetting.SYNDICATE_ALIASES_SPECIFIED
)
@EnvironmentVariables(value = {
		@EnvironmentVariable(key = "region", value = "${region}"),
		@EnvironmentVariable(key = "target_table", value = "${target_table}")
})
@LambdaUrlConfig(
		authType = AuthType.NONE
)
public class Processor implements RequestHandler<APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse> {
	private final AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard()
			.withRegion(System.getenv("region"))
			.build();
	private final DynamoDB dynamoDB = new DynamoDB(client);

	private final Gson gson = new Gson();

	public APIGatewayV2HTTPResponse handleRequest(APIGatewayV2HTTPEvent event, Context context) {

		if ("GET".equals(event.getRequestContext().getHttp().getMethod()) && "/weather".equals(event.getRawPath())) {

			OpenMeteoApiClient weatherApiClient = new OpenMeteoApiClient();
			double latitude = 52.52;
			double longitude = 13.41;
			Map<String, Object> weatherData;
			try {
				weatherData = weatherApiClient.getWeatherData(latitude, longitude);

			} catch (IOException e) {
				throw new RuntimeException(e);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}

			String id = UUID.randomUUID().toString();

			Item item = new Item().withPrimaryKey("id", id)
					.withMap("forecast", new ValueMap()
							.withNumber("elevation", (Number) weatherData.get("elevation"))
							.withNumber("generationtime_ms", (Number) weatherData.get("generationtime_ms"))
							.withMap("hourly", new ValueMap()
									.withList("temperature_2m", (List<Number>) ((Map<String, Object>) weatherData.get("hourly")).get("temperature_2m"))
									.withList("time", (List<String>) ((Map<String, Object>) weatherData.get("hourly")).get("time"))
							)
							.withMap("hourly_units", new ValueMap()
									.withString("temperature_2m", (String) ((Map<String, Object>) weatherData.get("hourly_units")).get("temperature_2m"))
									.withString("time", (String) ((Map<String, Object>) weatherData.get("hourly_units")).get("time"))
							)
							.withNumber("latitude", latitude)
							.withNumber("longitude", longitude)
							.withString("timezone", (String) weatherData.get("timezone"))
							.withString("timezone_abbreviation", (String) weatherData.get("timezone_abbreviation"))
							.withNumber("utc_offset_seconds", (Number) weatherData.get("utc_offset_seconds"))
					);

			Table table = dynamoDB.getTable(System.getenv("target_table"));
			table.putItem(item);
			return buildResponse(200, weatherData);
		} else {
			return buildResponse(400, new HashMap<>());
		}
	}

	private APIGatewayV2HTTPResponse buildResponse(int statusCode, Map<String, Object> response) {
		Map<String, String> headers = new HashMap<>();
		headers.put("Content-Type", "application/json");


		return APIGatewayV2HTTPResponse.builder()
				.withStatusCode(statusCode)
				.withHeaders(headers)
				.withBody(gson.toJson(response))
				.build();
	}
}
