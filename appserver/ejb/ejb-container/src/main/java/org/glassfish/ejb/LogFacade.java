package org.glassfish.ejb;

import org.glassfish.logging.annotation.LoggerInfo;
import org.glassfish.logging.annotation.LogMessagesResourceBundle;

import java.util.logging.Logger;

public final class LogFacade {

    @LoggerInfo(subsystem = "GlassFish-EJB", description = "GlassFish EJB Container Logger", publish = true)
    private static final String EJB_LOGGER_NAME = "javax.enterprise.ejb.container";

    @LogMessagesResourceBundle
    private static final String EJB_LOGGER_RB = "org.glassfish.ejb.LogMessages";

    private static final Logger LOGGER = Logger.getLogger(EJB_LOGGER_NAME, EJB_LOGGER_RB);

    private LogFacade() {}

    public static Logger getLogger() {
        return LOGGER;
    }
    
}
