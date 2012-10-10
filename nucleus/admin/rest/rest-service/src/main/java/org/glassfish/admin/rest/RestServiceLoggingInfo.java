/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.glassfish.admin.rest;

import java.util.logging.Logger;
import org.glassfish.logging.annotation.LogMessageInfo;
import org.glassfish.logging.annotation.LogMessagesResourceBundle;
import org.glassfish.logging.annotation.LoggerInfo;

/**
 *
 * @author jdlee
 */
public class RestServiceLoggingInfo {
//    private RestServiceLoggingInfo() { }

    @LogMessagesResourceBundle
    public static final String SHARED_LOGMESSAGE_RESOURCE = "org.glassfish.admin.rest.LogMessages";

    @LoggerInfo(subsystem = "REST", description = "Main REST Logger", publish = true)
    public static final String REST_MAIN_LOGGER = "javax.enterprise.admin.rest";

    public static final Logger restLogger = Logger.getLogger(REST_MAIN_LOGGER, SHARED_LOGMESSAGE_RESOURCE);

    @LogMessageInfo(
            message = "Listening to REST requests at context: {0}/domain.",
            level = "INFO")
    public static final String REST_INTERFACE_INITIALIZED = "NCLS-REST-00001";
}
