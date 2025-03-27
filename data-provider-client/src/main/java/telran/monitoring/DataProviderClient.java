package telran.monitoring;

import telran.monitoring.logging.Logger;

public interface DataProviderClient {
    String getDataForPatient(long patientId);
    static Logger[] loggers = new Logger[1];
    static DataProviderClient getDataProviderClient(String className, Logger logger, String provisioningServiceConnectionString) {
        loggers[0] = logger;
        try {
            return (DataProviderClient) Class.forName(className).getConstructor(String.class).newInstance(provisioningServiceConnectionString);
        } catch (Exception e) {
            logger.log("severe", "error: " + e);
            throw new RuntimeException(e);
        }
    }
}
