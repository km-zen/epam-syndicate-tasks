package com.task09;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.util.*;

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
	private static final Log log = LogFactory.getLog(Processor.class);
	public APIGatewayV2HTTPResponse handleRequest(APIGatewayV2HTTPEvent event, Context context) {
		log.info("In handleRequest method, before GET /");
		if ("GET".equals(event.getRequestContext().getHttp().getMethod()) && "/".equals(event.getRawPath())) {
			log.info("after calling GET /");
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

			Map<String, Object> hourlyData = (Map<String, Object>) weatherData.get("hourly");
			Map<String, Object> limitedHourlyData = new HashMap<>();
			limitedHourlyData.put("time", hourlyData.get("time"));
			limitedHourlyData.put("temperature_2m", hourlyData.get("temperature_2m"));

			Map<String, Object> hourlyUnits = (Map<String, Object>) weatherData.get("hourly_units");
			Map<String, Object> limitedHourlyUnits = new HashMap<>();
			limitedHourlyUnits.put("time", hourlyUnits.get("time"));
			limitedHourlyUnits.put("temperature_2m", hourlyUnits.get("temperature_2m"));

			Map<String, Object> forecastMap = new HashMap<>();
			forecastMap.put("elevation", weatherData.get("elevation"));
			forecastMap.put("generationtime_ms", weatherData.get("generationtime_ms"));
			forecastMap.put("hourly", limitedHourlyData);
			forecastMap.put("hourly_units", limitedHourlyUnits);
			forecastMap.put("latitude", weatherData.get("latitude"));
			forecastMap.put("longitude", weatherData.get("longitude"));
			forecastMap.put("timezone", weatherData.get("timezone"));
			forecastMap.put("timezone_abbreviation", weatherData.get("timezone_abbreviation"));
			forecastMap.put("utc_offset_seconds", weatherData.get("utc_offset_seconds"));
			Object forecastMapObj = forecastMap;
			Item item = new Item()
					.withPrimaryKey("id", id)
					.with("forecast", forecastMapObj);

			Table table = dynamoDB.getTable(System.getenv("target_table"));
			table.putItem(item);
			return buildResponse(200, weatherData);
		} else {
			log.info("Error, wrong endpoint");
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
