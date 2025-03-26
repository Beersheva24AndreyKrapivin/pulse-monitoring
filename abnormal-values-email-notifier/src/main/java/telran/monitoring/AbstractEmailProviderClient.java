package telran.monitoring;

import telran.monitoring.logging.Logger;

public abstract class AbstractEmailProviderClient implements EmailProviderClient{
    protected  Logger logger;
    protected AbstractEmailProviderClient(Logger logger) {
        this.logger = logger;
    }
}
