package telran.monitoring;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import telran.monitoring.api.SensorData;
import telran.monitoring.logging.Logger;

public class LatestDataSaverS3 extends AbstractDataSaverLogger {
    private String bucketName = System.getenv("S3_BUCKET_NAME");
    private S3Client s3Client;

    public LatestDataSaverS3(Logger logger) {
        super(logger);
        this.s3Client = S3Client.create();
    }

    @Override
    public void addValue(SensorData sensorData) {
        String key = sensorData.patientId() + ".json";
        List<SensorData> dataList = getAllValues(sensorData.patientId());

        dataList.add(sensorData);
        saveToS3(key, dataList);
    }

    @Override
    public List<SensorData> getAllValues(long patientId) {
        String key = patientId + ".json";
        try {
            GetObjectRequest request = GetObjectRequest.builder().bucket(bucketName).key(key).build();
            String jsonContent = s3Client.getObjectAsBytes(request).asUtf8String();
            return parseSensorDataList(jsonContent);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    @Override
    public SensorData getLastValue(long patientId) {
        List<SensorData> dataList = getAllValues(patientId);
        return dataList.isEmpty() ? null : dataList.get(dataList.size() - 1);
    }

    @Override
    public void clearValues(long patientId) {
        String key = patientId + ".json";
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucketName).key(key).build());
        } catch (Exception e) {
            logger.log("severe", "Failed to delete S3 file: " + e.getMessage());
        }
    }

    @Override
    public void clearAndAddValue(long patientId, SensorData sensorData) {
        String key = patientId + ".json";
        saveToS3(key, List.of(sensorData));
    }

    @Override
    public List<SensorData> getNLastValues(long patientId, int n) {
        List<SensorData> dataList = getAllValues(patientId);
        int start = Math.max(0, dataList.size() - n);
        return dataList.subList(start, dataList.size());
    }

    private void saveToS3(String key, List<SensorData> dataList) {
        String jsonContent = new JSONArray(dataList).toString();
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType("application/json")
                .build();

        s3Client.putObject(request, software.amazon.awssdk.core.sync.RequestBody.fromString(jsonContent));
    }

    private List<SensorData> parseSensorDataList(String jsonContent) {
        List<SensorData> result = new ArrayList<>();
        JSONArray jsonArray = new JSONArray(jsonContent);

        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject obj = jsonArray.getJSONObject(i);
            result.add(new SensorData(
                    obj.getLong("patientId"),
                    obj.getInt("value"),
                    obj.getLong("timestamp")
            ));
        }
        return result;
    }

}
