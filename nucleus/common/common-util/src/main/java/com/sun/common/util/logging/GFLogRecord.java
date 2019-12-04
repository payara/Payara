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

// Portions Copyright [2016 - 2019] [Payara Foundation]

package com.sun.common.util.logging;

import java.util.logging.LogRecord;
import java.util.logging.Level;

/**
 * This class provides additional attributes not supported by JUL LogRecord
 *
 * @author rinamdar
 * @author David Matejcek refactoring
 */
public class GFLogRecord extends LogRecord {

    private static final long serialVersionUID = -818792012235891720L;

    private final String threadName;


    /**
     * Creates new record.
     *
     * @param level
     * @param msg
     */
    public GFLogRecord(Level level, String msg) {
        super(level, msg);
        this.threadName = Thread.currentThread().getName();
    }

    /**
     * Coypies the log record.
     *
     * FIXME: wrap it instead, faster!
     *
     * @param record
     */
    public GFLogRecord(final LogRecord record) {
        super(record.getLevel(), record.getMessage());
        setLoggerName(record.getLoggerName());
        setMillis(record.getMillis());
        setParameters(record.getParameters());
        setResourceBundle(record.getResourceBundle());
        setResourceBundleName(record.getResourceBundleName());
        setSequenceNumber(record.getSequenceNumber());
        setSourceClassName(record.getSourceClassName());
        setSourceMethodName(record.getSourceMethodName());
        setThrown(record.getThrown());
        setThreadID(record.getThreadID());

        this.threadName = Thread.currentThread().getName();
    }


    /**
     * @return name of the thread which created this log record.
     */
    public String getThreadName() {
        return threadName;
    }

    /**
     * Returns only a message.
     */
    @Override
    public String toString() {
        return getMessage();
    }
}
