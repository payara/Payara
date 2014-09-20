/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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
package com.sun.enterprise.server.logging;

import java.util.Map;

/**
 * Event describing a log record being written to the log file.
 * This event is issued from the GFFileHandler. Interested parties
 * may register a LogEventListener with the GFFileHandler instance.
 * The GFFileHandler instance may be injected using hk2 mechanisms
 * to get a reference which can be used to register a LogEventListener.
 */
public interface LogEvent {

    /**
     * The formatted timestamp in the log event.
     * @return
     */
    public abstract String getTimestamp();

    /**
     * The message body including the stack trace of the associated Exception.
     * @return
     */
    public abstract String getMessage();

    /**
     * The name of the Level for this event. 
     * @return
     */
    public abstract String getLevel();

    /**
     * Logger name identifying the source of this event.
     * @return
     */
    public abstract String getLogger();

    /**
     * 
     * @return
     */
    public abstract int getLevelValue();

    /**
     * Integer value of the log Level.
     * @return
     */
    public abstract String getComponentId();

    /**
     * Raw timestamp in milliseconds.
     * @return
     */
    public abstract long getTimeMillis();

    /**
     * The message id for this log event.
     * @return
     */
    public abstract String getMessageId();

    /**
     * The thread ID where this log event originated.
     * @return
     */
    public abstract long getThreadId();

    /**
     * Thread name from where this log event originated.
     * @return
     */
    public abstract String getThreadName();

    /**
     * Current user Id executing this request during this log event.
     * @return
     */
    public abstract String getUser();

    /**
     * ECId for the current request for this log event.
     * @return
     */
    public abstract String getECId();

    /**
     * Optional name-value pairs associated with this log event.
     * @return
     */
    public abstract Map<String,Object> getSupplementalAttributes();

}