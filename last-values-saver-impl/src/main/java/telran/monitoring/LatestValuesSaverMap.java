package telran.monitoring;

import java.util.*;

import telran.monitoring.api.*;
import telran.monitoring.logging.Logger;

public class LatestValuesSaverMap extends AbstractDataSaverLogger{
    public LatestValuesSaverMap(Logger logger) {
        super(logger);
    }

    private HashMap<Long, List<SensorData>> history = new HashMap<>();

    @Override
    public void addValue(SensorData sensorData) {
       history.computeIfAbsent(sensorData.patientId(), (k) -> new LinkedList<SensorData>()).add(sensorData);
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<SensorData> getAllValues(long patientId) {
        return history.getOrDefault(patientId, Collections.EMPTY_LIST);
    }

    @Override
    public SensorData getLastValue(long patientId) {
        List<SensorData>patientHistory = history.getOrDefault(patientId, List.of());
        SensorData res = null;
        if(!patientHistory.isEmpty()) {
            res = patientHistory.getLast();
        }
        return res;
    }

    @Override
    public void clearValues(long patientId) {
        List<SensorData> patientHistory = history.get(patientId);
        if(patientHistory != null) {
            patientHistory.clear();
        }
    }

    @Override
    public void clearAndAddValue(long patientId, SensorData sensorData) {
       List<SensorData> patientHistory = history.computeIfAbsent(patientId, k -> new LinkedList<>());
       patientHistory.clear();
       patientHistory.add(sensorData);
    }

    @Override
    public List<SensorData> getNLastValues(long patientId, int n) {
        List<SensorData> list = history.getOrDefault(patientId, Collections.emptyList());
        int size = list.size();
        return list.subList(Math.max(0, size - n), size);
    }

}