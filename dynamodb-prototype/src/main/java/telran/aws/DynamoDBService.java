package telran.aws;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest.*;
import telran.monitoring.api.SensorData;

import java.util.HashMap;

public class DynamoDBService {
    private String tableName = "";
    private final DynamoDbClient client;

    public DynamoDBService(String tableName) {
        this.client = DynamoDbClient.builder().build();
        this.tableName = tableName;
    }

    public void saveToDynamoDB(SensorData sensorData) {
        Builder request = PutItemRequest.builder();
        request = request.tableName(tableName);
        client.putItem(request.item(getMap(sensorData)).build());
    }

    private HashMap<String, AttributeValue> getMap(SensorData sensorData) {
        HashMap<String, AttributeValue> map = new HashMap<>(){{
            put("patientId", AttributeValue.builder().n(sensorData.patientId() + "").build());
            put("value", AttributeValue.builder().n(sensorData.value() + "").build());
            put("timestamp", AttributeValue.builder().n(sensorData.timestamp() + "").build());
        }};

        return map;
    }
}
