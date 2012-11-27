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

import java.util.HashMap;
import java.util.Map;
import java.util.logging.LogRecord;

/**
 * @author sanshriv
 *
 */
public class LogEventImpl implements LogEvent {

    private String componentId = "";
    private String ecId = "";
    private String level = "";
    private int levelValue = 0;
    private String logger = "";
    private String message = "";
    private String messageId = "";
    private Map<String,Object> suppAttrs = new HashMap<String,Object>();
    private long threadId = 0L;
    private String threadName = "";
    private long timeMillis = 0L;
    private String timestamp = "";
    private String user = "";

    public LogEventImpl() {}
    
    public LogEventImpl(LogRecord rec) {
        level = rec.getLevel().getName();
        logger = rec.getLoggerName();
        message = rec.getMessage();
        threadId = rec.getThreadID();
        timeMillis = rec.getMillis();
        levelValue = rec.getLevel().intValue();
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
    void setComponentId(String componentId) {
        this.componentId = componentId;
    }

    /**
     * @param ecId the ecId to set
     */
    void setECId(String ecId) {
        this.ecId = ecId;
    }

    /**
     * @param level the level to set
     */
    void setLevel(String level) {
        this.level = level;
    }

    /**
     * @param levelValue the levelValue to set
     */
    void setLevelValue(int levelValue) {
        this.levelValue = levelValue;
    }

    /**
     * @param logger the logger to set
     */
    void setLogger(String logger) {
        this.logger = logger;
    }

    /**
     * @param message the message to set
     */
    void setMessage(String message) {
        this.message = message;
    }

    /**
     * @param messageId the messageId to set
     */
    void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    /**
     * @param threadId the threadId to set
     */
    void setThreadId(long threadId) {
        this.threadId = threadId;
    }

    /**
     * @param threadName the threadName to set
     */
    void setThreadName(String threadName) {
        this.threadName = threadName;
    }

    /**
     * @param timeMillis the timeMillis to set
     */
    void setTimeMillis(long timeMillis) {
        this.timeMillis = timeMillis;
    }

    /**
     * @param timestamp the timestamp to set
     */
    void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * @param user the user to set
     */
    void setUser(String user) {
        this.user = user;
    }

}
