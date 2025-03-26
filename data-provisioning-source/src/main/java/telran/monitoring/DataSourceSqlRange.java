package telran.monitoring;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.NoSuchElementException;

import telran.monitoring.api.Range;

public class DataSourceSqlRange extends DataSourceSQL {

    private static final String MIN_PULSE_VALUE = "min_pulse_value";
    private static final String MAX_PULSE_VALUE = "max_pulse_value";
    private static final String GROUP_ID = "group_id";
    private static final String PATIENT_ID = "patient_id";
    private static final String STATEMENT_STRING = String.format("select %s, %s groups " +
            "where id = (select % from patients where %s = ?)",
            MIN_PULSE_VALUE, MAX_PULSE_VALUE, GROUP_ID, PATIENT_ID);

    protected DataSourceSqlRange() {
        super(STATEMENT_STRING);
    }

    @Override
    protected String resultSetProcessing(ResultSet resultSet) {
        try {
            if (resultSet.next()) {
                int min = resultSet.getInt(MIN_PULSE_VALUE);
                int max = resultSet.getInt(MAX_PULSE_VALUE);
                return new Range(min, max).toString();
            } else {
                throw new NoSuchElementException("no data provider for patient");
            }
        } catch (Exception e) {
            logger.log("sever", "error: " + e);
            throw new RuntimeException(e);
        }
    }

}
