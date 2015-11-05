// Portions Copyright [2015] [C2B2 Consulting Limited]

package org.glassfish.jdbc.util;

import java.util.logging.Logger;

import com.sun.logging.LogDomains;

/**
 * Always use this factory to generate loggers in this library. The reason is
 * that we use classloader from another library to load correct resource bundle
 * - we use messages from there.
 *
 * @author David Matějček
 */
public class LoggerFactory {

    /**
     * @param clazz
     * @return logger using resource bundle same as the connectors-runtime.jar
     */
    public static Logger getLogger(final Class clazz) {
        return LogDomains.getLogger(clazz, LogDomains.RSR_LOGGER, Thread.currentThread().getContextClassLoader());
    }
}
