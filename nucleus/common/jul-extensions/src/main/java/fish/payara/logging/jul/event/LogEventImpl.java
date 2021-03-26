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
// Portions Copyright [2020] [Payara Foundation and/or its affiliates]

package fish.payara.logging.jul.event;

import fish.payara.logging.jul.record.EnhancedLogRecord;

import java.util.HashMap;
import java.util.Map;

/**
 * @author sanshriv
 */
public class LogEventImpl implements LogEvent {

    private final Map<String,Object> suppAttrs = new HashMap<>();
    private String componentId;
    private String ecId;
    private String level;
    private int levelValue;
    private String logger;
    private String message;
    private String messageId;
    private long threadId;
    private String threadName;
    private long timeMillis;
    private String timestamp;
    private String user;

    public LogEventImpl() {
        this.logger = "";
        this.level = "";
        this.levelValue = 0;
        this.message = "";
        this.messageId = "";
        this.threadId = 0L;
        this.threadName = "";
        this.timeMillis = 0L;
        this.timestamp = "";

        this.componentId = "";
        this.ecId = "";
        this.user = "";
    }

    public LogEventImpl(final EnhancedLogRecord rec) {
        this.logger = rec.getLoggerName();
        this.level = rec.getLevel().getName();
        this.levelValue = rec.getLevel().intValue();
        this.message = rec.getMessage();
        this.messageId = rec.getMessageKey();
        this.threadId = rec.getThreadID();
        this.threadName = rec.getThreadName();
        this.timeMillis = rec.getMillis();

        this.componentId = "";
        this.ecId = "";
        this.user = "";
    }

    @Override
    public String getComponentId() {
        return componentId;
    }

    @Override
    public String getECId() {
        return ecId;
    }

    @Override
    public String getLevel() {
        return level;
    }

    @Override
    public int getLevelValue() {
        return levelValue;
    }

    @Override
    public String getLogger() {
        return logger;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public String getMessageId() {
        return messageId;
    }

    @Override
    public Map<String,Object> getSupplementalAttributes() {
        return suppAttrs;
    }

    public void addSupplementalAttribute(final String key, final Object value) {
        if (key != null && value != null) {
            this.suppAttrs.put(key, value);
        }
    }

    @Override
    public long getThreadId() {
        return threadId;
    }

    @Override
    public String getThreadName() {
        return threadName;
    }

    @Override
    public long getTimeMillis() {
        return timeMillis;
    }

    @Override
    public String getTimestamp() {
        return timestamp;
    }

    @Override
    public String getUser() {
        return user;
    }

    /**
     * @param componentId the componentId to set
     */
    public void setComponentId(final String componentId) {
        this.componentId = componentId;
    }

    /**
     * @param ecId the ecId to set
     */
    public void setECId(final String ecId) {
        this.ecId = ecId;
    }

    /**
     * @param level the level to set
     */
    public void setLevel(final String level) {
        this.level = level;
    }

    /**
     * @param levelValue the levelValue to set
     */
    public void setLevelValue(final int levelValue) {
        this.levelValue = levelValue;
    }

    /**
     * @param logger the logger to set
     */
    public void setLogger(final String logger) {
        this.logger = logger;
    }

    /**
     * @param message the message to set
     */
    public void setMessage(final String message) {
        this.message = message;
    }

    /**
     * @param messageId the messageId to set
     */
    public void setMessageId(final String messageId) {
        this.messageId = messageId;
    }

    /**
     * @param threadId the threadId to set
     */
    public void setThreadId(final long threadId) {
        this.threadId = threadId;
    }

    /**
     * @param threadName the threadName to set
     */
    public void setThreadName(final String threadName) {
        this.threadName = threadName;
    }

    /**
     * @param timeMillis the timeMillis to set
     */
    public void setTimeMillis(final long timeMillis) {
        this.timeMillis = timeMillis;
    }

    /**
     * @param timestamp the timestamp to set
     */
    public void setTimestamp(final String timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * @param user the user to set
     */
    public void setUser(final String user) {
        this.user = user;
    }

}
