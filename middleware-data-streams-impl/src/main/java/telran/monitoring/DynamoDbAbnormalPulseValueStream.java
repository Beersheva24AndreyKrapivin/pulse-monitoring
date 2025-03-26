package telran.monitoring;

import java.util.HashMap;
import java.util.Map;

import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import telran.monitoring.api.AbnormalPulseValue;

public class DynamoDbAbnormalPulseValueStream extends DynamoDbStream<AbnormalPulseValue>{

    public DynamoDbAbnormalPulseValueStream(String table) {
        super(table);
    }

    @Override
    Map<String, AttributeValue> getMap(AbnormalPulseValue data) {
        HashMap<String, AttributeValue> map = new HashMap<>(){{
            put("patientId",AttributeValue.builder().n(data.patientId() + "").build());
            put("value",AttributeValue.builder().n(data.value() + "").build());
            put("min_value",AttributeValue.builder().n(data.min_value() + "").build());
            put("max_value",AttributeValue.builder().n(data.max_value() + "").build());
            put("timestamp",AttributeValue.builder().n(data.timestamp() + "").build());
        }};
        return map;
    }
}
