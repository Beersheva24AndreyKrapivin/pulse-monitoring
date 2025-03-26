package telran.monitoring;

public class MailSenderTest implements MailSender{
    @Override
    void sendMail(String subject, String recipientAddress, String text) {
        logger.log("finest", String.format("subject: %s, recipientAddress: %s, text: %s", subject, recipientAddress, text));
    }
}
