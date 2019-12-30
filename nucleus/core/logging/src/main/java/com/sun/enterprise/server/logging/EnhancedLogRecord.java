/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *    Copyright (c) 2019 Payara Foundation and/or its affiliates. All rights reserved.
 *
 *     The contents of this file are subject to the terms of either the GNU
 *     General Public License Version 2 only ("GPL") or the Common Development
 *     and Distribution License("CDDL") (collectively, the "License").  You
 *     may not use this file except in compliance with the License.  You can
 *     obtain a copy of the License at
 *     https://github.com/payara/Payara/blob/master/LICENSE.txt
 *     See the License for the specific
 *     language governing permissions and limitations under the License.
 *
 *     When distributing the software, include this License Header Notice in each
 *     file and include the License file at glassfish/legal/LICENSE.txt.
 *
 *     GPL Classpath Exception:
 *     The Payara Foundation designates this particular file as subject to the "Classpath"
 *     exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *     file that accompanied this code.
 *
 *     Modifications:
 *     If applicable, add the following below the License Header, with the fields
 *     enclosed by brackets [] replaced by your own identifying information:
 *     "Portions Copyright [year] [name of copyright owner]"
 *
 *     Contributor(s):
 *     If you wish your version of this file to be governed by only the CDDL or
 *     only the GPL Version 2, indicate your decision by adding "[Contributor]
 *     elects to include this software in this distribution under the [CDDL or GPL
 *     Version 2] license."  If you don't indicate a single choice of license, a
 *     recipient has the option to distribute your version of this file under
 *     either the CDDL, the GPL Version 2 or to extend the choice of license to
 *     its licensees as provided above.  However, if you add GPL Version 2 code
 *     and therefore, elected the GPL Version 2 license, then the option applies
 *     only if the new code is made subject to such option by the copyright
 *     holder.
 */
package com.sun.enterprise.server.logging;

import java.text.MessageFormat;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * This class provides additional attributes not supported by JUL LogRecord
 *
 * @author David Matejcek refactoring
 */
public class EnhancedLogRecord extends LogRecord {

    private static final long serialVersionUID = -818792012235891720L;

    private final String threadName;
    private final LogRecord record;
    private final String messageKey;


    /**
     * Creates new record.
     *
     * @param level
     * @param msg
     */
    public EnhancedLogRecord(final Level level, final String msg) {
        super(level, msg);
        this.messageKey = null;
        this.threadName = Thread.currentThread().getName();
        this.record = new LogRecord(level, msg);
        // this is to force invocation of record.inferCaller()
        this.record.getSourceClassName();
    }


    /**
     * Coypies the log record.
     *
     * @param record
     */
    public EnhancedLogRecord(final LogRecord record) {
        super(record.getLevel(), null);
        this.threadName = Thread.currentThread().getName();
        this.record = record;
        // this is to force invocation of record.inferCaller()
        this.record.getSourceClassName();
        // values were used and they are not required any more.
        final MessageElements messageElements = resolveMessage(record);
        this.messageKey = messageElements.key;
        this.record.setMessage(messageElements.message);
        this.record.setResourceBundle(null);
        this.record.setParameters(null);
    }


    /**
     * @return the message identifier (generally not unique)
     */
    public String getMessageKey() {
        return messageKey;
    }


    /**
     * @return name of the thread which created this log record.
     */
    public String getThreadName() {
        return threadName;
    }


    @Override
    public Level getLevel() {
        return this.record.getLevel();
    }


    @Override
    public void setLevel(final Level level) {
        this.record.setLevel(level);
    }


    @Override
    public long getSequenceNumber() {
        return this.record.getSequenceNumber();
    }


    @Override
    public void setSequenceNumber(final long seq) {
        this.record.setSequenceNumber(seq);
    }

    @Override
    public String getLoggerName() {
        return this.record.getLoggerName();
    }


    @Override
    public void setLoggerName(final String name) {
        this.record.setLoggerName(name);
    }

    @Override
    public String getSourceClassName() {
        return this.record.getSourceClassName();
    }


    @Override
    public void setSourceClassName(final String sourceClassName) {
        this.record.setSourceClassName(sourceClassName);
    }


    @Override
    public String getSourceMethodName() {
        return this.record.getSourceMethodName();
    }


    @Override
    public void setSourceMethodName(final String sourceMethodName) {
        this.record.setSourceMethodName(sourceMethodName);
    }


    @Override
    public String getMessage() {
        return this.record.getMessage();
    }


    @Override
    public void setMessage(final String message) {
        this.record.setMessage(message);
    }


    @Override
    public Object[] getParameters() {
        return this.record.getParameters();
    }


    @Override
    public void setParameters(final Object[] parameters) {
        this.record.setParameters(parameters);
    }


    @Override
    public int getThreadID() {
        return this.record.getThreadID();
    }


    @Override
    public void setThreadID(final int threadID) {
        this.record.setThreadID(threadID);
    }


    @Override
    public long getMillis() {
        return this.record.getMillis();
    }


    @Override
    public void setMillis(final long millis) {
        this.record.setMillis(millis);
    }


    @Override
    public Throwable getThrown() {
        return this.record.getThrown();
    }


    @Override
    public void setThrown(final Throwable thrown) {
        this.record.setThrown(thrown);
    }


    @Override
    public ResourceBundle getResourceBundle() {
        return this.record.getResourceBundle();
    }


    @Override
    public void setResourceBundle(final ResourceBundle bundle) {
        this.record.setResourceBundle(bundle);
    }


    @Override
    public String getResourceBundleName() {
        return this.record.getResourceBundleName();
    }


    @Override
    public void setResourceBundleName(final String name) {
        this.record.setResourceBundleName(name);
    }


    @Override
    public String toString() {
        return getMessage();
    }


    /**
     * This is a mechanism extracted from the StreamHandler.
     * If the message is loggable should be decided before creation of this instance to avoid
     * resolving a message which would not be used. And it is - in {@link Logger#log(LogRecord)}.
     */
    private static MessageElements resolveMessage(final LogRecord record) {
        final MessageElements localizedTemplate = tryToLocalizeTemplate(record);
        final Object[] parameters = record.getParameters();
        if (parameters == null || parameters.length == 0) {
            return localizedTemplate;
        }
        final String localizedMessage = toMessage(localizedTemplate.message, parameters);
        return new MessageElements(localizedTemplate.key, localizedMessage);
    }


    private static String toMessage(final String template, final Object[] parameters) {
        try {
            return MessageFormat.format(template, parameters);
        } catch (final Exception e) {
            return template;
        }
    }


// key, recrb, loggername
    private static MessageElements tryToLocalizeTemplate(final LogRecord record) {
        final ResourceBundle bundle = getResourceBundle(record);
        final String originalMessage = record.getMessage();
        if (bundle == null) {
            return new MessageElements(null, originalMessage);
        }
        try {
            final String localizedMessage = bundle.getString(originalMessage);
            return new MessageElements(originalMessage, localizedMessage);
        } catch (final MissingResourceException e) {
            return new MessageElements(null, originalMessage);
        }
    }

// loggername, recrb
    private static ResourceBundle getResourceBundle(final LogRecord record) {
        final ResourceBundle bundle = record.getResourceBundle();
        if (bundle != null) {
            return bundle;
        }
        final LogManager logManager = LogManager.getLogManager();
        final Logger logger = logManager.getLogger(record.getLoggerName());
        return logger == null ? null : logger.getResourceBundle();
    }


    private static class MessageElements {

        final String key;
        final String message;

        MessageElements(final String key, final String message) {
            this.key = key;
            this.message = message;
        }
    }
}
