package telran.monitoring.logging;

import java.util.HashMap;
import java.util.logging.*;

import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsClient;
import software.amazon.awssdk.services.cloudwatchlogs.model.CreateLogGroupRequest;
import software.amazon.awssdk.services.cloudwatchlogs.model.CreateLogStreamRequest;
import software.amazon.awssdk.services.cloudwatchlogs.model.InputLogEvent;
import software.amazon.awssdk.services.cloudwatchlogs.model.PutLogEventsRequest;
import software.amazon.awssdk.services.cloudwatchlogs.model.PutLogEventsResponse;
import software.amazon.awssdk.services.cloudwatchlogs.model.ResourceAlreadyExistsException;

public class CloudWatchLogger implements Logger {

    private java.util.logging.Logger logger;
    private CloudWatchLogsClient cloudWatchLogsClient;
    private String logGroupName;
    private String logStreamName;
    private String sequenceToken;

    static String defaultValue = Logger.defaultValue;

    static HashMap<String, String> levelsMap = new HashMap<>() {{
        put("debug", "config");
        put("trace", "fine");
        put("error", "severe");
    }};

    public CloudWatchLogger(String loggerName, String logGroupName, String logStreamName) {
        this.logger = java.util.logging.Logger.getLogger(loggerName);
        this.cloudWatchLogsClient = CloudWatchLogsClient.create();
        this.logGroupName = logGroupName;
        this.logStreamName = logStreamName;

        setupLogger();
    }

    private void setupLogger() {
        LogManager.getLogManager().reset(); 
        String level = System.getenv("LOGGER_LEVEL");
        if (level == null) {
            level = defaultValue;
        }
        String javaLevel = levelsMap.get(level);
        if (javaLevel != null) {
            level = javaLevel;
        }
        Level loggerLevel = Level.parse(level.toUpperCase());
        logger.setLevel(loggerLevel);
        Handler consoleHandler = new ConsoleHandler();
        consoleHandler.setLevel(loggerLevel);
        logger.addHandler(consoleHandler);

        createLogGroupAndStream(); 
    }

    private void createLogGroupAndStream() {
        try {
            CreateLogGroupRequest createLogGroupRequest = CreateLogGroupRequest.builder()
                    .logGroupName(logGroupName)
                    .build();
            cloudWatchLogsClient.createLogGroup(createLogGroupRequest);
        } catch (ResourceAlreadyExistsException e) {
        }

        try {
            CreateLogStreamRequest createLogStreamRequest = CreateLogStreamRequest.builder()
                    .logGroupName(logGroupName)
                    .logStreamName(logStreamName)
                    .build();
            cloudWatchLogsClient.createLogStream(createLogStreamRequest);
        } catch (ResourceAlreadyExistsException e) {
        }
    }

    @Override
    public void log(String level, String message) {
        String javaLevel = levelsMap.get(level);
        if (javaLevel != null) {
            level = javaLevel;
        }

        logToCloudWatch(level, message);

        logger.log(Level.parse(level.toUpperCase()), message);
    }

    private void logToCloudWatch(String level, String message) {
        try {
            InputLogEvent logEvent = InputLogEvent.builder()
                    .message(message)
                    .timestamp(System.currentTimeMillis())
                    .build();

            PutLogEventsRequest putLogEventsRequest = PutLogEventsRequest.builder()
                    .logGroupName(logGroupName)
                    .logStreamName(logStreamName)
                    .logEvents(logEvent)
                    .sequenceToken(sequenceToken) 
                    .build();

            PutLogEventsResponse response = cloudWatchLogsClient.putLogEvents(putLogEventsRequest);
            sequenceToken = response.nextSequenceToken();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error logging to CloudWatch: " + e.getMessage());
        }
    }

}
