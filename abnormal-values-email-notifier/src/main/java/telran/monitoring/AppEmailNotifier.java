package telran.monitoring;

import java.util.Map;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent.DynamodbStreamRecord;
import com.amazonaws.services.lambda.runtime.events.models.dynamodb.AttributeValue;
import telran.monitoring.api.AbnormalPulseValue;
import telran.monitoring.logging.*;

public class AppEmailNotifier {
    private static final String DEFAULT_EMAIL_PROVIDER_CLASS_NAME = "telran.monitoring.DataProviderClientHttp";
    private static final String DEFAULT_EMAIL_SENDER_CLASS_NAME = "telran.monitoring.MailSenderSes";
    private static final String DEFAULT_EMAIL_SUBJECT = "Abnormal pulse value patient ";
    private static final String DEFAULT_ADDRESS_PREFIX = "yuriaws25+";
    Logger logger = new LoggerStandard("email-notifier");
    Map<String, String> env = System.getenv();
    String mailSenderClassName = getMailSenderClassName();
    String addressPrefix = getAddressPrefix();
    String providerClientClassName = getProviderClientClassName();
    String subject = getSubject();
    DataProviderClient providerClient;
    MailSender mailSender;
    private String providerClientConnectionString = getProviderClientConectionString();

    public AppEmailNotifier() {
        configLog();
        try {
            providerClient = DataProviderClient.getDataProviderClient(providerClientClassName,
                    logger, providerClientConnectionString);
            mailSender = MailSender.getMailSender(mailSenderClassName, logger);
        } catch (Exception e) {
            logger.log("severe", "error: new_code " + e);
            throw new RuntimeException(e);
        }
    }

    private String getMailSenderClassName() {
        return env.getOrDefault("EMAIL_SENDER_CLASS_NAME", DEFAULT_EMAIL_SENDER_CLASS_NAME);
    }

    private String getSubject() {
        return env.getOrDefault("EMAIL_SUBJECT",
                DEFAULT_EMAIL_SUBJECT);
    }

    private String getAddressPrefix() {
        return env.getOrDefault("ADDRESS_PREFIX", DEFAULT_ADDRESS_PREFIX);
    }

    private void configLog() {
        logger.log("config", "providerClientClassName is " + providerClientClassName);
        logger.log("FINEST", "address prefix is " + addressPrefix);
        logger.log("config", "email subject " + subject);
        logger.log("config", "Email Sender Class Name is " + mailSenderClassName);
    }

    private String getProviderClientClassName() {
        return env.getOrDefault("EMAIL_PROVIDER_CLASS_NAME",
                DEFAULT_EMAIL_PROVIDER_CLASS_NAME);
    }

    public void handleRequest(final DynamodbEvent event, final Context context) {
        event.getRecords().forEach(this::sensorDataProcessing);

    }

    private void sensorDataProcessing(DynamodbStreamRecord record) {
        String eventName = record.getEventName();
        if (eventName.equalsIgnoreCase("INSERT")) {
            Map<String, AttributeValue> map = record.getDynamodb().getNewImage();
            if (map != null) {
                AbnormalPulseValue abnormalPulseValue = getAbnormalPulseValue(map);
                logger.log("finest", abnormalPulseValue.toString());
                sendMail(abnormalPulseValue);

            } else {
                logger.log("severe", "no new image found in event");
            }

        } else {
            logger.log("severe", eventName + " not supposed for processing");
        }
    }

    private void sendMail(AbnormalPulseValue abnormalPulseValue) {
        long patientId = abnormalPulseValue.patientId();
        String recipientEmail = addressPrefix + providerClient.getDataForPatient(patientId);
        mailSender.sendMail(subject + patientId, recipientEmail, getEmailText(abnormalPulseValue));

    }

    private String getProviderClientConectionString() {
        String res = env.get("PROVIDER_CLIENT_CONNECTION_STRING");
        if (res == null) {
            logger.log("severe", "error: PROVIDER_CLIENT_CONNECTION_STRING env. variable must exist");
            throw new RuntimeException("PROVIDER_CLIENT_CONNECTION_STRING env. variable must exist");
        }
        return res;
    }

    private String getEmailText(AbnormalPulseValue abnormalPulseValue) {
        String text = String.format("Notification about abnormal pulse value\n"
                + "Patient with id %d has pulse value %d\n"
                + "the patient belongs to group with\n"
                + "minimal pulse value - %d\n"
                + "maximal pulse value - %d",
                abnormalPulseValue.patientId(),
                abnormalPulseValue.value(),
                abnormalPulseValue.min_value(),
                abnormalPulseValue.max_value());
        logger.log("finest", "text: " + text);
        return text;
    }

    private AbnormalPulseValue getAbnormalPulseValue(Map<String, AttributeValue> map) {
        long patientId = Long.parseLong(map.get("patientId").getN());
        int value = Integer.parseInt(map.get("value").getN());
        long timestamp = Long.parseLong(map.get("timestamp").getN());
        int minValue = Integer.parseInt(map.get("min_value").getN());
        int maxValue = Integer.parseInt(map.get("max_value").getN());
        ;
        AbnormalPulseValue abnormalPulseValue = new AbnormalPulseValue(patientId, value, minValue, maxValue, timestamp);
        return abnormalPulseValue;
    }

}