package telran.monitoring;

import telran.monitoring.logging.Logger;

public interface DataSource {
    String getData(long patientId);
    static Logger [] loggers = new Logger[1];
}
