/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2012 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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

// Portions Copyright [2016-2024] [Payara Foundation and/or its affiliates]

package com.sun.common.util.logging;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;

/**
 * This class provides additional attributes not supported by JUL LogRecord
 * @author rinamdar
 */
public class GFLogRecord extends LogRecord {

    private static final String FAST_LOGGER_PROPERTY = "com.sun.enterprise.server.logging.GFFileHandler.fastLogging";
    /**
     * The deprecated fastLogging attribute
     *
     * DO NOT USE! Retained for semantic versioning. Replaced by {@link GFLogRecord#fastLoggingAtomic}.
     */
    @Deprecated(forRemoval = true, since = "6.21.0")
    public static Boolean fastLogging = Boolean.parseBoolean(LogManager.getLogManager().getProperty(FAST_LOGGER_PROPERTY));

    public static final AtomicBoolean fastLoggingAtomic = new AtomicBoolean(Boolean.parseBoolean(LogManager.getLogManager().getProperty(FAST_LOGGER_PROPERTY)));

    /**
     * SVUID for serialization compatibility
     */
    private static final long serialVersionUID = -818792012235891720L;

    private String threadName;

    public GFLogRecord(Level level, String msg) {
        super(level, msg);
    }

    public GFLogRecord(LogRecord record) {
        this(record.getLevel(), record.getMessage());

        this.setLoggerName(record.getLoggerName());
        this.setMillis(record.getMillis());
        this.setParameters(transformParameters(record.getParameters()));
        this.setResourceBundle(record.getResourceBundle());
        this.setResourceBundleName(record.getResourceBundleName());
        this.setSequenceNumber(record.getSequenceNumber());
        this.setSourceClassName(record.getSourceClassName());
        this.setSourceMethodName(record.getSourceMethodName());
        this.setThreadID(record.getThreadID());
        this.setThrown(record.getThrown());
    }

    public String getThreadName() {
        return threadName;
    }

    public void setThreadName(String threadName) {
        this.threadName = threadName;
    }

    /**
     * wrap log record with {@link GFLogRecord} if not already
     * if setThreadName is true, sets thread name to current
     *
     * @param record
     * @param setThreadName
     * @return wrapped record
     */
    public static GFLogRecord wrap(LogRecord record, boolean setThreadName) {
        GFLogRecord wrappedRecord;
        if (record instanceof GFLogRecord) {
            wrappedRecord = (GFLogRecord)record;
        } else {
            wrappedRecord = new GFLogRecord(record);
        }
        // Check there is actually a set thread name
        if (setThreadName && wrappedRecord.getThreadName() == null) {
            wrappedRecord.setThreadName(Thread.currentThread().getName());
        }

        return wrappedRecord;
    }

    /**
     * CUSTOM-55
     * in case of an object passed as a parameter, call it's toString() method
     * to resolve it's values in the current thread, instead of waiting for queues / etc
     * so there is no possibility of state change of the object between threads
     * Append the original parameters at the end, as they are used for by some logging formatters,
     * such as JSON logging formatter for context
     *
     * FISH-5703
     * Add the option to skip the toString() method as it can have force a JPA entity to result
     * in database access, causing a performance impact.
     *
     * @param params
     * @return parameter array
     */
    private static Object[] transformParameters(Object[] params) {
        if (params == null) {
            return null;
        }
        if (fastLoggingAtomic.get()) {
            return params;
        }
        Object[] result = new Object[params.length * 2];
        System.arraycopy(params, 0, result, params.length, params.length);
        for (int stringParamsIndex = 0, originalParamsIndex = params.length;
                stringParamsIndex < params.length;
                ++stringParamsIndex, ++originalParamsIndex) {
            Object param = params[stringParamsIndex];
            if (param != null) {
                result[stringParamsIndex] = param.toString();
            }
        }
        return result;
    }
}
