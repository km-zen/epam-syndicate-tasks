package com.task02;

import com.amazonaws.services.lambda.runtime.Context;
import com.syndicate.deployment.annotations.lambda.LambdaHandler;
import com.syndicate.deployment.annotations.lambda.LambdaUrlConfig;
import com.syndicate.deployment.model.lambda.url.AuthType;
import com.syndicate.deployment.model.RetentionSetting;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@LambdaHandler(
    lambdaName = "hello_world",
	roleName = "hello_world-role",
	isPublishVersion = false,
	aliasName = "${lambdas_alias_name}",
	logsExpiration = RetentionSetting.SYNDICATE_ALIASES_SPECIFIED
)
@LambdaUrlConfig(
		authType = AuthType.NONE
)
public class HelloWorld implements RequestHandler<APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse> {

	private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

	@Override
	public APIGatewayV2HTTPResponse handleRequest(APIGatewayV2HTTPEvent event, Context context) {
		if ("GET".equals(event.getRequestContext().getHttp().getMethod()) && "/hello".equals(event.getRawPath())) {
			return buildResponse(200, "Hello from lambda");
		} else {
			return buildResponse(404, "Not Found");
		}
	}

	private APIGatewayV2HTTPResponse buildResponse(int statusCode, String message) {
		Map<String, String> headers = new HashMap<>();
		headers.put("Content-Type", "application/json");

		Map<String, Object> responseBody = new HashMap<>();
		responseBody.put("message", message);

		return APIGatewayV2HTTPResponse.builder()
				.withStatusCode(statusCode)
				.withHeaders(headers)
				.withBody(gson.toJson(responseBody))
				.build();
	}
}
