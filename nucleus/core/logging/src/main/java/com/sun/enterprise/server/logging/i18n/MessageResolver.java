/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *  Copyright (c) 2019 Payara Foundation and/or its affiliates. All rights reserved.
 *
 *  The contents of this file are subject to the terms of either the GNU
 *  General Public License Version 2 only ("GPL") or the Common Development
 *  and Distribution License("CDDL") (collectively, the "License").  You
 *  may not use this file except in compliance with the License.  You can
 *  obtain a copy of the License at
 *  https://github.com/payara/Payara/blob/master/LICENSE.txt
 *  See the License for the specific
 *  language governing permissions and limitations under the License.
 *
 *  When distributing the software, include this License Header Notice in each
 *  file and include the License file at glassfish/legal/LICENSE.txt.
 *
 *  GPL Classpath Exception:
 *  The Payara Foundation designates this particular file as subject to the "Classpath"
 *  exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *  file that accompanied this code.
 *
 *  Modifications:
 *  If applicable, add the following below the License Header, with the fields
 *  enclosed by brackets [] replaced by your own identifying information:
 *  "Portions Copyright [year] [name of copyright owner]"
 *
 *  Contributor(s):
 *  If you wish your version of this file to be governed by only the CDDL or
 *  only the GPL Version 2, indicate your decision by adding "[Contributor]
 *  elects to include this software in this distribution under the [CDDL or GPL
 *  Version 2] license."  If you don't indicate a single choice of license, a
 *  recipient has the option to distribute your version of this file under
 *  either the CDDL, the GPL Version 2 or to extend the choice of license to
 *  its licensees as provided above.  However, if you add GPL Version 2 code
 *  and therefore, elected the GPL Version 2 license, then the option applies
 *  only if the new code is made subject to such option by the copyright
 *  holder.
 */

package com.sun.enterprise.server.logging.i18n;

import com.sun.enterprise.server.logging.EnhancedLogRecord;
import java.text.MessageFormat;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * This class is used to resolve {@link LogRecord}'s message:
 * <ol>
 * <li>to internationalize the message if the resource bundle is set
 * <li>to construct final message using record's parameters
 * <li>to return {@link EnhancedLogRecord} providing additional items usable in logs
 * </ol>
 *
 * @author David Matejcek
 */
public class MessageResolver {

    private final LogManager manager;


    /**
     * Searches for {@link LogManager} valid in current context.
     * Does not have any other internal state.
     */
    public MessageResolver() {
        this.manager = LogManager.getLogManager();
    }


    /**
     * Wraps the original record to {@link EnhancedLogRecord} and resolves the final log message.
     * Also sets resource bundle and parameters to null to avoid repeating same resolution again.
     * <br>
     * It also detects that the record is already resolved, then returns it without any change.
     *
     * @param record
     * @return {@link EnhancedLogRecord} with final log message.
     */
    public EnhancedLogRecord resolve(final LogRecord record) {
        final EnhancedLogRecord enhancedLogRecord = toEnhancedLogRecord(record);
        if (isAlreadyResolved(enhancedLogRecord)) {
            return enhancedLogRecord;
        }
        final ResolvedLogMessage message = resolveMessage(record);
        enhancedLogRecord.setMessageKey(message.key);
        enhancedLogRecord.setMessage(message.message);
        // values were used and they are not required any more.
        // not only this, it is good to even avoid their usage as backup mechanism does it in JUL
        // implementation. We also use them in isAlreadyResolved to avoid redundant work.
        enhancedLogRecord.setResourceBundle(null);
        enhancedLogRecord.setResourceBundleName(null);
        enhancedLogRecord.setParameters(null);
        return enhancedLogRecord;
    }


    private EnhancedLogRecord toEnhancedLogRecord(final LogRecord record) {
        if (EnhancedLogRecord.class.isInstance(record)) {
            return (EnhancedLogRecord) record;
        }
        return new EnhancedLogRecord(record);
    }


    private boolean isAlreadyResolved(final EnhancedLogRecord record) {
        return record.getResourceBundle() == null && record.getResourceBundleName() == null
            && record.getParameters() == null;
    }


    /**
     * This is a mechanism extracted from the StreamHandler and extended.
     * If the message is loggable should be decided before creation of this instance to avoid
     * resolving a message which would not be used. And it is in done - in
     * {@link Logger#log(LogRecord)} and in {@link #publish(LogRecord)}
     */
    private ResolvedLogMessage resolveMessage(final LogRecord record) {
        final String originalMessage = record.getMessage();
        if (originalMessage == null || originalMessage.isEmpty()) {
            return new ResolvedLogMessage(null, originalMessage);
        }
        final ResourceBundle bundle = getResourceBundle(record.getResourceBundle(), record.getLoggerName());
        final ResolvedLogMessage localizedTemplate = tryToLocalizeTemplate(originalMessage, bundle);
        final Object[] parameters = record.getParameters();
        if (parameters == null || parameters.length == 0) {
            return localizedTemplate;
        }
        final String localizedMessage = toMessage(localizedTemplate.message, parameters);
        return new ResolvedLogMessage(localizedTemplate.key, localizedMessage);
    }


    private ResourceBundle getResourceBundle(final ResourceBundle bundle, final String loggerName) {
        if (bundle != null) {
            return bundle;
        }
        final Logger logger = this.manager.getLogger(loggerName);
        return logger == null ? null : logger.getResourceBundle();
    }


    private ResolvedLogMessage tryToLocalizeTemplate(final String originalMessage, final ResourceBundle bundle) {
        if (bundle == null) {
            return new ResolvedLogMessage(null, originalMessage);
        }
        try {
            final String localizedMessage = bundle.getString(originalMessage);
            return new ResolvedLogMessage(originalMessage, localizedMessage);
        } catch (final MissingResourceException e) {
            return new ResolvedLogMessage(null, originalMessage);
        }
    }


    private String toMessage(final String template, final Object[] parameters) {
        try {
            return MessageFormat.format(template, parameters);
        } catch (final Exception e) {
            return template;
        }
    }
}
