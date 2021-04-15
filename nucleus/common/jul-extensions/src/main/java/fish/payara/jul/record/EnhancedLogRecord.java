/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *    Copyright (c) 2019-2021 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.jul.record;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ResourceBundle;
import java.util.logging.ErrorManager;
import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 * This class provides additional attributes not supported by JUL LogRecord
 *
 * @author David Matejcek
 */
public class EnhancedLogRecord extends LogRecord {

    private static final long serialVersionUID = -818792012235891720L;
    private static final ZoneId TIME_ZONE = ZoneId.systemDefault();

    private final LogRecord record;
    private final String threadName;
    private String messageKey;


    /**
     * Creates new record. Source class and method will be autodetected.
     *
     * @param level
     * @param message
     */
    public EnhancedLogRecord(final Level level, final String message) {
        this(new LogRecord(level, message), true);
    }


    /**
     * Creates new record. Source class and method will be autodetected now or set after this
     * constructor ends.
     *
     * @param level
     * @param message
     * @param autodetectSource
     */
    public EnhancedLogRecord(final Level level, final String message, final boolean autodetectSource) {
        this(new LogRecord(level, message), autodetectSource);
    }


    /**
     * Wraps the log record.
     *
     * @param record
     * @param autodetectSource
     */
    public EnhancedLogRecord(final LogRecord record, final boolean autodetectSource) {
        super(record.getLevel(), null);
        this.threadName = Thread.currentThread().getName();
        this.record = record;
        if (autodetectSource) {
            detectClassAndMethod(record);
        }
    }


    /**
     * @return the message identifier (generally not unique, may be null)
     */
    public String getMessageKey() {
        return messageKey;
    }


    /**
     * This is called just to remember the original message value after it was translated using
     * the resource bundle.
     *
     * @param messageKey the message identifier (generally not unique, may be null)
     */
    void setMessageKey(final String messageKey) {
        this.messageKey = messageKey;
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
    public void setSourceClassName(final String className) {
        this.record.setSourceClassName(className == null || className.isEmpty() ? null : className);
    }


    @Override
    public String getSourceMethodName() {
        return this.record.getSourceMethodName();
    }


    @Override
    public void setSourceMethodName(final String methodName) {
        this.record.setSourceMethodName(methodName == null || methodName.isEmpty() ? null : methodName);
    }


    @Override
    public String getMessage() {
        return this.record.getMessage();
    }


    @Override
    public void setMessage(final String message) {
        this.record.setMessage(message == null || message.isEmpty() ? null : message);
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


    /**
     * @return {@link #getMillis()} converted to {@link OffsetDateTime} in local time zone.
     */
    public OffsetDateTime getTime() {
        return OffsetDateTime.ofInstant(Instant.ofEpochMilli(record.getMillis()), TIME_ZONE);
    }


    /**
     * @return printed stacktrace of {@link #getThrown()} or null
     */
    public String getThrownStackTrace() {
        if (getThrown() == null) {
            return null;
        }
        try (StringWriter sw = new StringWriter(); PrintWriter pw = new PrintWriter(sw)) {
            getThrown().printStackTrace(pw);
            return sw.toString();
        } catch (IOException e) {
            new ErrorManager().error("Cannot print stacktrace!", e, ErrorManager.FORMAT_FAILURE);
            return null;
        }
    }


    @Override
    public String toString() {
        return getMessage();
    }


    private boolean detectClassAndMethod(final LogRecord wrappedRecord) {
        final StackTraceElement[] stack = new Throwable().getStackTrace();
        boolean found = false;
        for (StackTraceElement element : stack) {
            final String className = element.getClassName();
            if (!found) {
                found = isIgnoredStackTraceElement(className);
                continue;
            }
            if (!isIgnoredStackTraceElement(className)) {
                wrappedRecord.setSourceClassName(className);
                wrappedRecord.setSourceMethodName(element.getMethodName());
                return true;
            }
        }
        // don't try it again.
        return true;
    }


    /**
     * @param sourceClassName usually class which created this record
     * @return if true the class will not be used as a source.
     */
    protected boolean isIgnoredStackTraceElement(final String sourceClassName) {
        // TODO: make it configurable from logging.properties, allow to use custom loggers. This is fragile.
        return "fish.payara.jul.PayaraLogger".equals(sourceClassName)
            // see LogDomains in Payara sources
            || "com.sun.logging.LogDomainsLogger".equals(sourceClassName)
            // remaining classes are in JDK
            || "java.util.logging.Logger".equals(sourceClassName)
            // FIXME: are those following necessary? (The less the better)
            || sourceClassName.startsWith("java.util.logging.LoggingProxyImpl")
            || sourceClassName.startsWith("java.lang.reflect.")
            || sourceClassName.startsWith("sun.util.logging.")
            || sourceClassName.startsWith("sun.reflect.");
    }
}
