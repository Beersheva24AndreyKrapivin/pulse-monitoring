package telran.monitoring.api;

import java.lang.reflect.Constructor;
import java.util.List;

import telran.monitoring.logging.Logger;

public interface LatestValuesSaver {
    void addValue(SensorData sensorData);

    List<SensorData> getAllValues(long patientId);

    SensorData getLastValue(long patientId);

    void clearValues(long patientId);

    void clearAndAddValue(long patientId, SensorData sensorData);

    List<SensorData> getNLastValues(long patientId, int n);

    static LatestValuesSaver getLatestValuesSaver(String latestValuesSaverClassName, Logger logger) {
        try {
            // Class<LatestValuesSaver> clazz = (Class<LatestValuesSaver>)Class.forName(latestValuesSaverClassName);
            // Constructor<LatestValuesSaver> constructor = clazz.getConstructor(String.class);
            // LatestValuesSaver res = constructor.newInstance(latestValuesSaverClassName);
            // return res;
            LatestValuesSaver res = null;
            res = (LatestValuesSaver) Class.forName(latestValuesSaverClassName).getConstructor(Logger.class).newInstance(logger);
            return res;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
