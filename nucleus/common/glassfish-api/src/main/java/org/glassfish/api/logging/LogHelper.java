/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */
package org.glassfish.api.logging;

import java.text.MessageFormat;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * Class providing helper APIs for logging purposes.
 *
 */
public final class LogHelper {

    /**
     * Logs a message with the given level, message, parameters and <code>Throwable</code>.
     * @param logger the <code>Logger</code> object to be used for logging the message.
     * @param level the <code>Level</code> of the message to be logged.
     * @param messageId the key in the resource bundle of the <code>Logger</code> containing the localized text.
     * @param thrown the <code>Throwable</code> associated with the message to be logged.
     * @param params the parameters to the localized text.
     */
    public static void log(Logger logger, Level level, String messageId,
            Throwable thrown, Object... params) {
        LogRecord rec = new LogRecord(level, messageId);
        rec.setLoggerName(logger.getName());
        rec.setResourceBundleName(logger.getResourceBundleName());
        rec.setResourceBundle(logger.getResourceBundle());
        rec.setParameters(params);
        rec.setThrown(thrown);
        logger.log(rec);
    }
    
    /**
     * Gets the formatted message given the message key and parameters.
     * The ResourceBundle associated with the logger is searched for the specified key.
     * @param logger
     * @param msgKey
     * @param params
     * @return
     */
    public static String getFormattedMessage(Logger logger, String msgKey, Object... params) {
        ResourceBundle rb = logger.getResourceBundle();
        if (rb != null) {
            try {
                return MessageFormat.format(rb.getString(msgKey),params);
            } catch (java.util.MissingResourceException e) {
                return msgKey;
            }
        }
        return msgKey;
    }
    
}
