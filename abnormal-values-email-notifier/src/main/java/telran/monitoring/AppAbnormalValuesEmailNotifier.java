package telran.monitoring;

import java.util.Map;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent.DynamodbStreamRecord;
import com.amazonaws.services.lambda.runtime.events.models.dynamodb.AttributeValue;

import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.Body;
import software.amazon.awssdk.services.ses.model.Destination;
import software.amazon.awssdk.services.ses.model.Message;
import software.amazon.awssdk.services.ses.model.SendEmailRequest;
import software.amazon.awssdk.services.ses.model.SendEmailResponse;
import software.amazon.awssdk.services.ses.model.SesException;
import software.amazon.awssdk.services.ses.model.Content;
import telran.monitoring.api.AbnormalPulseValue;
import telran.monitoring.api.NotificationData;
import telran.monitoring.logging.Logger;
import telran.monitoring.logging.LoggerStandard;

public class AppAbnormalValuesEmailNotifier {
    private static final String DEFAULT_STREAM_NAME = "abnormal-values-email-notifier";
    private static final String DEFAULT_STREAM_CLASS_NAME = "telran.monitoring.DynamoDbStreamNotificationData";
    private static final String DEFAULT_EMAIL_PROVIDER_CLASS = "telran.monitoring.EmailProviderClientHttp";
    private static final String FROM_EMAIL = "kr_andr@mail.ru";

    private Map<String, String> env = System.getenv();
    String providerClientClassName = getProviderClientClassName();
    private String streamName = getStreamName();
    Logger logger = new LoggerStandard(streamName);

    private String streamClassName = getStreamClassName();

    MiddlewareDataStream<NotificationData> dataStream;
    EmailProviderClient providerClient;
    //SesClient client = SesClient.builder().build();

    @SuppressWarnings("unchecked")
    public AppAbnormalValuesEmailNotifier() {
        logger.log("config", "Stream name is " + streamName);
        try {

            dataStream = (MiddlewareDataStream<NotificationData>) MiddlewareDataStreamFactory.getStream(
                    streamClassName,
                    streamName);
            providerClient = (EmailProviderClient) Class.forName(providerClientClassName).getConstructor(Logger.class)
                    .newInstance(logger);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void handleRequest(final DynamodbEvent event, final Context context) {
        event.getRecords().forEach(r -> {
            sensorDataProcessing(r);
        });

    }

    private String getProviderClientClassName() {
        String res = env.getOrDefault("EMAIL_PROVIDER_CLASS", DEFAULT_EMAIL_PROVIDER_CLASS);
        return res;
    }

    private String getStreamName() {
        String result = env.getOrDefault("STREAM_NAME", DEFAULT_STREAM_NAME);

        return result;
    }

    private String getStreamClassName() {
        String result = env.getOrDefault("STREAM_CLASS_NAME", DEFAULT_STREAM_CLASS_NAME);
        logger.log("config", "Stream class name is " + result);
        return result;
    }

    private void sensorDataProcessing(DynamodbStreamRecord r) {
        String eventName = r.getEventName();
        if (eventName.equalsIgnoreCase("INSERT")) {
            Map<String, AttributeValue> map = r.getDynamodb().getNewImage();
            if (map != null) {
                AbnormalPulseValue abnormalPulseValue = getAbnormalPulseValue(map);
                logger.log("finest", abnormalPulseValue.toString());
                NotificationData notificationData = getEnotificationData(abnormalPulseValue);
                if (notificationData != null) {
                    dataStream.publish(notificationData);
                    logger.log("debug", "Published Abnormal notification data: " + notificationData);
                    sendEmail(notificationData.email(), "Abnormal pulse value", notificationData.notificationText());

                }
            } else {
                logger.log("severe", "no new image found in event");
            }

        } else {
            logger.log("severe", eventName + " not supposed for processing");
        }
    }

    private void sendEmail(String email, String subject, String notificationText) {
        try (SesClient sesClient = SesClient.create()) {
            SendEmailRequest request = SendEmailRequest.builder()
                    .source(FROM_EMAIL)
                    .destination(d -> d.toAddresses(email))
                    .message(m -> m
                            .subject(s -> s.data(subject))
                            .body(b -> b.text(t -> t.data(notificationText))))
                    .build();

            SendEmailResponse response = sesClient.sendEmail(request);
            logger.log("finest", "Email response: " + response.toString());
        } catch (SesException e) {
            logger.log("severe", "Email not sent. Error message: " + e.getMessage());
        }
    }

    private NotificationData getEnotificationData(AbnormalPulseValue abnormalPulseValue) {
        NotificationData notificationData = null;

        if (abnormalPulseValue != null) {
            try {
                String email = providerClient.getEmail(abnormalPulseValue.patientId());
                logger.log("finest", "returned from Email provider client: " + email);
                notificationData = new NotificationData(abnormalPulseValue.patientId(), email,
                        "Patient " + abnormalPulseValue.patientId() + " has abnormal pulse value: "
                                + abnormalPulseValue.value(),
                        abnormalPulseValue.timestamp());
            } catch (Exception e) {
                logger.log("severe", "error - " + e.toString());
            }
        }

        return notificationData;
    }

    private AbnormalPulseValue getAbnormalPulseValue(Map<String, AttributeValue> map) {
        long patientId = Long.parseLong(map.get("patientId").getN());
        int value = Integer.parseInt(map.get("value").getN());
        int min = Integer.parseInt(map.get("min").getN());
        int max = Integer.parseInt(map.get("max").getN());
        long timestamp = Long.parseLong(map.get("timestamp").getN());
        AbnormalPulseValue abnormalPulseValue = new AbnormalPulseValue(patientId, value, min, max, timestamp);
        return abnormalPulseValue;
    }
}
