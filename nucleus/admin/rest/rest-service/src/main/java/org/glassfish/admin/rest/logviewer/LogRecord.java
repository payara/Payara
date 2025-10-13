/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2013 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
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
 *
 * Portions Copyright [2017-2021] Payara Foundation and/or affiliates
 */
package org.glassfish.admin.rest.logviewer;

import java.util.Date;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * internal REST wrapper for a log record will be used to emit Json easily
 *
 * @author ludo
 */
@XmlRootElement(name = "record")
public class LogRecord {

    private Long recordNumber;
    private Long loggedDateTimeInMS;
    private String loggedLevel;
    private String productName;
    private String loggerName;
    private String nameValuePairs;
    private String messageID;
    private String message;
    
    public LogRecord() {
        this.recordNumber = null;
        this.loggedDateTimeInMS = null;
        this.loggedLevel = null;
        this.productName = null;
        this.loggerName = null;
        this.nameValuePairs = null;
        this.messageID = null;
        this.message = null;
    }
    
    @XmlAttribute
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @XmlAttribute
    public long getLoggedDateTimeInMS() {
        return loggedDateTimeInMS;
    }

    public void setLoggedDateTime(Date loggedDateTime) {
        this.loggedDateTimeInMS = loggedDateTime.getTime();
    }

    @XmlAttribute
    public String getLoggedLevel() {
        return loggedLevel;
    }

    public void setLoggedLevel(String loggedLevel) {
        this.loggedLevel = loggedLevel;
    }

    @XmlAttribute
    public String getLoggerName() {
        return loggerName;
    }

    public void setLoggerName(String loggerName) {
        this.loggerName = loggerName;
    }

    @XmlAttribute
    public String getMessageID() {
        return messageID;
    }

    public void setMessageID(String messageID) {
        this.messageID = messageID;
    }

    @XmlAttribute
    public String getNameValuePairs() {
        return nameValuePairs;
    }

    public void setNameValuePairs(String nameValuePairs) {
        this.nameValuePairs = nameValuePairs;
    }

    @XmlAttribute
    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    @XmlAttribute
    public long getRecordNumber() {
        return recordNumber;
    }

    public void setRecordNumber(long recordNumber) {
        this.recordNumber = recordNumber;
    }
}
