package com.task08;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import com.google.gson.Gson;
import com.kmzen.WeatherApiClient;
import com.syndicate.deployment.annotations.lambda.LambdaHandler;
import com.syndicate.deployment.annotations.lambda.LambdaLayer;
import com.syndicate.deployment.annotations.lambda.LambdaUrlConfig;
import com.syndicate.deployment.model.Architecture;
import com.syndicate.deployment.model.ArtifactExtension;
import com.syndicate.deployment.model.DeploymentRuntime;
import com.syndicate.deployment.model.RetentionSetting;
import com.syndicate.deployment.model.lambda.url.AuthType;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


@LambdaHandler(
    lambdaName = "api_handler",
		layers = {"sdk_layer"},
	roleName = "api_handler-role",
		runtime = DeploymentRuntime.JAVA11,
		architecture = Architecture.ARM64,
	isPublishVersion = true,
	aliasName = "${lambdas_alias_name}",
	logsExpiration = RetentionSetting.SYNDICATE_ALIASES_SPECIFIED
)
@LambdaLayer(
		layerName = "sdk_layer",
		libraries = {"lib/java-client-meteo-sdk-1.0-SNAPSHOT.jar"},
		architectures = {Architecture.ARM64},
		artifactExtension = ArtifactExtension.ZIP
)
@LambdaUrlConfig(
		authType = AuthType.NONE
)
public class ApiHandler implements RequestHandler<APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse> {

	private final Gson gson = new Gson();
	public APIGatewayV2HTTPResponse handleRequest(APIGatewayV2HTTPEvent event, Context context) {
		if ("GET".equals(event.getRequestContext().getHttp().getMethod()) && "/weather".equals(event.getRawPath())) {

			WeatherApiClient weatherApiClient = new WeatherApiClient();
			double latitude = 52.52;
			double longitude = 13.41;
            Map<String, Object> weatherData;
            try {
                weatherData = weatherApiClient.getWeatherData(latitude,longitude);

            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

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
