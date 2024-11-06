package com.task08;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import com.google.gson.Gson;
import com.kmzen.meteocustomsdk.WeatherFetcher;
import com.syndicate.deployment.annotations.lambda.LambdaHandler;
import com.syndicate.deployment.annotations.lambda.LambdaLayer;
import com.syndicate.deployment.annotations.lambda.LambdaUrlConfig;
import com.syndicate.deployment.model.Architecture;
import com.syndicate.deployment.model.ArtifactExtension;
import com.syndicate.deployment.model.DeploymentRuntime;
import com.syndicate.deployment.model.RetentionSetting;
import com.syndicate.deployment.model.lambda.url.AuthType;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

@LambdaHandler(
    lambdaName = "api_handler",
		layers = {"sdk_layer"},
	roleName = "api_handler-role",
	isPublishVersion = true,
	aliasName = "${lambdas_alias_name}",
	logsExpiration = RetentionSetting.SYNDICATE_ALIASES_SPECIFIED
)
@LambdaLayer(
		layerName = "sdk_layer",
		libraries = {"lib/my-custom-sdk-1.0.0.jar"},
		runtime = DeploymentRuntime.JAVA17,
		architectures = {Architecture.ARM64},
		artifactExtension = ArtifactExtension.ZIP
)
@LambdaUrlConfig(
		authType = AuthType.NONE
)
public class ApiHandler implements RequestHandler<APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse> {

	private final Gson gson = new Gson();
	public APIGatewayV2HTTPResponse handleRequest(APIGatewayV2HTTPEvent event, Context context) {
		if ("GET".equals(event.getRequestContext().getHttp().getMethod()) && "/hello".equals(event.getRawPath())) {
			WeatherFetcher fetcher = new WeatherFetcher();
			Map<String, Object> weatherData = fetcher.fetchWeather();
			return buildResponse(200, weatherData);
		} else {
			return buildResponse(400, new HashMap<>());
		}


	}

	private APIGatewayV2HTTPResponse buildResponse(int statusCode, Map<String, Object> event) {
		Map<String, String> headers = new HashMap<>();
		headers.put("Content-Type", "application/json");

		Map<String, Object> responseBody = new HashMap<>();
		responseBody.put("event", event);

		return APIGatewayV2HTTPResponse.builder()
				.withStatusCode(statusCode)
				.withHeaders(headers)
				.withBody(gson.toJson(responseBody))
				.build();
	}
}
