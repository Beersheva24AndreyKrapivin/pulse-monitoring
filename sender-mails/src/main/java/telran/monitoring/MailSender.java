package telran.monitoring;
import java.lang.reflect.Constructor;

import telran.monitoring.logging.*;
public interface MailSender {
    static Logger [] loggers = new Logger[1];
    void sendMail(String subject, String recipientAddress, String text);
    static MailSender getMailSender(String mailSenderClassName, Logger logger) {
        loggers[0] = logger;
        logger.log("finest", "class name from MailSender interface is " + mailSenderClassName);
        try {
            @SuppressWarnings("unchecked")
            Class<MailSender> clazz = (Class<MailSender>) Class.forName(mailSenderClassName);
            logger.log("finest", "class in MailSender created");
            Constructor<MailSender> constructor = clazz.getConstructor();
            logger.log("finest", "constructor in MailSender created");
            MailSender instance = constructor.newInstance();
            logger.log("finest", "instance in MailSender created");
            return instance;
        } catch(Exception e) {

            throw new RuntimeException(e);
        }
    }
}