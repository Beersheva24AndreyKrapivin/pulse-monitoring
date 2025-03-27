package telran.monitoring;

import telran.monitoring.logging.Logger;

public class MailSenderTest implements MailSender{
    Logger logger = loggers[0];
    @Override
    public void sendMail(String subject, String recipientAddress, String text) {
        logger.log("finest", String.format("subject: %s, recipientAddress: %s, text: %s", subject, recipientAddress, text));
    }
}
