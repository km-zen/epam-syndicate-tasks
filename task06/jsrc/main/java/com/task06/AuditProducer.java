package com.task06;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.PutItemOutcome;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import com.amazonaws.services.lambda.runtime.events.models.dynamodb.AttributeValue;
import com.syndicate.deployment.annotations.environment.EnvironmentVariable;
import com.syndicate.deployment.annotations.environment.EnvironmentVariables;
import com.syndicate.deployment.annotations.events.DynamoDbTriggerEventSource;
import com.syndicate.deployment.annotations.lambda.LambdaHandler;
import com.syndicate.deployment.annotations.resources.DependsOn;
import com.syndicate.deployment.model.ResourceType;
import com.syndicate.deployment.model.RetentionSetting;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static javax.swing.event.DocumentEvent.EventType.INSERT;

@LambdaHandler(
    lambdaName = "audit_producer",
	roleName = "audit_producer-role",
	isPublishVersion = true,
	aliasName = "${lambdas_alias_name}",
	logsExpiration = RetentionSetting.SYNDICATE_ALIASES_SPECIFIED
)
@DynamoDbTriggerEventSource(targetTable = "Configuration", batchSize = 1)
@DependsOn(name = "Configuration", resourceType = ResourceType.DYNAMODB_TABLE)
@EnvironmentVariables(value = {
		@EnvironmentVariable(key = "region", value = "${region}"),
		@EnvironmentVariable(key = "target_table", value = "${target_table}")
})
public class AuditProducer implements RequestHandler<DynamodbEvent, Void> {
	private final AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard()
			.withRegion(System.getenv("region"))
			.build();
	private final DynamoDB dynamoDB = new DynamoDB(client);
	String id = UUID.randomUUID().toString();
	String modificationTime = java.time.Instant.now().toString();

	public Void handleRequest(DynamodbEvent dynamodbEvent, Context context) {

		String targetTable = System.getenv("target_table");
		for (DynamodbEvent.DynamodbStreamRecord record : dynamodbEvent.getRecords()) {
			if ("INSERT".equals(record.getEventName())) {
				// Pobieramy dane z nowego obrazu (new image)
				Map<String, AttributeValue> newImage = record.getDynamodb().getNewImage();

				// Wyciągamy klucz i wartość
				String key = newImage.get("key").getS().toString();
				int value = Integer.parseInt(newImage.get("value").getN());
				Map<String, Object> newValue = new HashMap<>();
				newValue.put("key", key);
				newValue.put("value", value);
				Item item = new Item().withPrimaryKey("key", id)
						.with("itemKey", key)
						.with("modificationTime", modificationTime)
						.withMap("newValue", newValue);

				Table table = dynamoDB.getTable(targetTable);
				table.putItem(item);
			}
			// Obsługa zmiany (MODIFY)
			if ("MODIFY".equals(record.getEventName())) {
				// Pobieramy nowy obraz (new image) i stary obraz (old image)
				Map<String, AttributeValue> newImage = record.getDynamodb().getNewImage();
				Map<String, AttributeValue> oldImage = record.getDynamodb().getOldImage();

				// Sprawdzamy zmiany dla klucza "value"
				if (newImage.containsKey("value") && oldImage.containsKey("value")) {
					int newValue = Integer.parseInt(newImage.get("value").getN());
					int oldValue = Integer.parseInt(oldImage.get("value").getN());

					// Jeśli wartości są różne, zapisujemy do tabeli auditowej
					if (newValue != oldValue) {
						String id = UUID.randomUUID().toString();
						String modificationTime = java.time.Instant.now().toString();
						String key = newImage.get("key").getS();  // Zakładamy, że klucz nie zmienia się.

						// Tworzymy element do zapisu w tabeli auditowej
						Item item = new Item()
								.withPrimaryKey("key", id)
								.with("itemKey", key)
								.with("modificationTime", modificationTime)
								.with("updatedAttribute", "value")
								.with("oldValue", oldValue)
								.with("newValue", newValue);

						// Zapisujemy element do tabeli auditowej
						Table table = dynamoDB.getTable(targetTable);
						PutItemOutcome outcome = table.putItem(item);
						System.out.println("Zaktualizowano element w tabeli auditowej: " + outcome);
					}
				}
			}

		}
		return null;
	}
}
