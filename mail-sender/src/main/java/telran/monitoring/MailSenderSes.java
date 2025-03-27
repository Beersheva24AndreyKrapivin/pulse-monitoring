package telran.monitoring;

import java.util.Map;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.Body;
import software.amazon.awssdk.services.ses.model.Content;
import software.amazon.awssdk.services.ses.model.Destination;
import software.amazon.awssdk.services.ses.model.Message;
import software.amazon.awssdk.services.ses.model.SendEmailRequest;
import software.amazon.awssdk.services.ses.model.SesException;
import telran.monitoring.logging.Logger;

public class MailSenderSes implements MailSender {
    private static final String DEFAULT_ADDRESS_PREFIX = "";
    private static final String DEFAULT_SENDER_EMAIL_ADDRESS = "kr_andr@mail.ru";
    private static final String DEFAULT_REGION_FOR_AWS = "us-east-1";
    SesClient sesClient;
    Logger logger = loggers[0];
    Map<String, String> env = System.getenv();
    String senderEmail = getSenderEmail();
    String addressPrefix = getAddressPrefix();
    Region region = getRegion();

    public MailSenderSes() {
        configLog();
        sesClient = SesClient.builder().region(region).build();
    }

    private void configLog() {
        logger.log("config", "emailAddressSender is " + senderEmail);
        logger.log("config", "addressPrefix is " + addressPrefix);
        logger.log("config", "region is " + region);
    }

    @Override
    public void sendMail(String subject, String recipientEmail, String text) {
        try {
            SendEmailRequest emailRequest = SendEmailRequest.builder()
                    .destination(Destination.builder().toAddresses(recipientEmail).build())
                    .message(Message.builder()
                            .subject(Content.builder().data(subject).build())
                            .body(Body.builder()
                                    .text(Content.builder().data(text).build())
                                    .build())
                            .build())
                    .source(senderEmail) // Must be the verified email
                    .build();

            // Send email
            var response = sesClient.sendEmail(emailRequest);
            logger.log("finest", "response " + response);

        } catch (SesException e) {
            logger.log("severe", "error of sending mail: " + e.awsErrorDetails().errorMessage());
            throw new RuntimeException(e);
        }
    }

    private String getAddressPrefix() {
        return env.getOrDefault("ADDRESS_PREFIX", DEFAULT_ADDRESS_PREFIX);
    }

    private String getSenderEmail() {
        return env.getOrDefault("SENDER_EMAIL_ADDRESS", DEFAULT_SENDER_EMAIL_ADDRESS);
    }

    private Region getRegion() {
        String regionStr = env.getOrDefault("REGION_FOR_AWS", DEFAULT_REGION_FOR_AWS);
        logger.log("finest", "region value of the REGION_FOR_AWS variable is " + regionStr);
        return Region.of(regionStr);
    }

}
