package com.task07;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.syndicate.deployment.annotations.environment.EnvironmentVariable;
import com.syndicate.deployment.annotations.environment.EnvironmentVariables;
import com.syndicate.deployment.annotations.events.RuleEventSource;
import com.syndicate.deployment.annotations.lambda.LambdaHandler;
import com.syndicate.deployment.model.RetentionSetting;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@LambdaHandler(
    lambdaName = "uuid_generator",
	roleName = "uuid_generator-role",
	isPublishVersion = true,
	aliasName = "${lambdas_alias_name}",
	logsExpiration = RetentionSetting.SYNDICATE_ALIASES_SPECIFIED
)
@EnvironmentVariables(value = {
		@EnvironmentVariable(key = "region", value = "${region}"),
		@EnvironmentVariable(key = "target_bucket", value = "${target_bucket}")
})
@RuleEventSource(
		targetRule = "uuid_trigger"
)
public class UuidGenerator implements RequestHandler<Object, Map<String, Object>> {

	// Klient S3 do zapisywania plików
	private final AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
			.withRegion(System.getenv("region"))
			.build();

	@Override
	public Map<String, Object> handleRequest(Object request, Context context) {

		// Pobranie czasu rozpoczęcia w formacie ISO8601
		String executionTime = Instant.now().toString();
		String bucketName = System.getenv("target_bucket");  // Pobieramy nazwę bucketu z zmiennych środowiskowych

		// Wygenerowanie 10 UUID
		List<String> uuidList = generateUUIDs(10);

		// Tworzenie struktury JSON
		Map<String, Object> jsonMap = new HashMap<>();
		jsonMap.put("ids", uuidList);

		// Konwertowanie mapy do JSON
		String jsonContent = convertMapToJson(jsonMap);

		// Zapisanie pliku w S3 (nazwa pliku to czas uruchomienia w formacie ISO 8601)
		s3Client.putObject(bucketName, executionTime, jsonContent);

		// Zwracamy informację o sukcesie operacji
		Map<String, Object> response = new HashMap<>();
		response.put("statusCode", 200);
		response.put("body", "File created: " + executionTime);

		return response;
	}

	// Funkcja generująca listę UUID
	private List<String> generateUUIDs(int count) {
		return IntStream.range(0, count)
				.mapToObj(i -> UUID.randomUUID().toString())
				.collect(Collectors.toList());
	}

	// Funkcja konwertująca mapę do formatu JSON
	private String convertMapToJson(Map<String, Object> map) {
		try {
			ObjectMapper objectMapper = new ObjectMapper();
			return objectMapper.writeValueAsString(map);
		} catch (Exception e) {
			throw new RuntimeException("Failed to convert map to JSON", e);
		}
	}
}
