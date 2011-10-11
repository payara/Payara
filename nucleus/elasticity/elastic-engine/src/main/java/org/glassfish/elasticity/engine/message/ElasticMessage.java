/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.elasticity.engine.message;

import java.io.Serializable;

/**
 * @author Mahesh.Kannan@Oracle.Com
 */
public class ElasticMessage
    implements Serializable {

    private String sourceMemberName;

    private String targetMemberName;

    private String serviceName;

    @Override
    public String toString() {
        return "ElasticMessage{" +
                "sourceMemberName='" + sourceMemberName + '\'' +
                ", targetMemberName='" + targetMemberName + '\'' +
                ", serviceName='" + serviceName + '\'' +
                ", subComponentName='" + subComponentName + '\'' +
                ", messageId='" + messageId + '\'' +
                ", inResponseToMessageId='" + inResponseToMessageId + '\'' +
                ", isResponseMessage=" + isResponseMessage +
                ", responseRequired=" + responseRequired +
                ", data=" + data +
                ", exception=" + exception +
                '}';
    }

    private String subComponentName;

    private String messageId;

    private String inResponseToMessageId;

    private boolean isResponseMessage;

    private boolean responseRequired;

    private Object data;

    private Exception exception;

    public String getSourceMemberName() {
        return sourceMemberName;
    }

    public ElasticMessage setSourceMemberName(String sourceMemberName) {
        this.sourceMemberName = sourceMemberName;
        return this;
    }

    public String getTargetMemberName() {
        return targetMemberName;
    }

    public ElasticMessage setTargetMemberName(String targetMemberName) {
        this.targetMemberName = targetMemberName;
        return this;
    }

    public String getServiceName() {
        return serviceName;
    }

    public ElasticMessage setServiceName(String serviceName) {
        this.serviceName = serviceName;
        return this;
    }

    public String getSubComponentName() {
        return subComponentName;
    }

    public ElasticMessage setSubComponentName(String subComponentName) {
        this.subComponentName = subComponentName;
        return this;
    }

    public String getMessageId() {
        return messageId;
    }

    public ElasticMessage setMessageId(String messageId) {
        this.messageId = messageId;
        return this;
    }

    public Object getData() {
        return data;
    }

    public String getInResponseToMessageId() {
        return inResponseToMessageId;
    }

    public ElasticMessage setInResponseToMessageId(String inResponseToMessageId) {
        this.inResponseToMessageId = inResponseToMessageId;
        return this;
    }

    public boolean isResponseMessage() {
        return isResponseMessage;
    }

    public ElasticMessage setIsResponseMessage(boolean responseMessage) {
        isResponseMessage = responseMessage;
        return this;
    }

    public boolean isResponseRequired() {
        return responseRequired;
    }

    public ElasticMessage setResponseRequired(boolean responseRequired) {
        this.responseRequired = responseRequired;
        return this;
    }

    public ElasticMessage setData(Object data) {
        this.data = data;
        return this;
    }

    public Exception getException() {
        return exception;
    }

    public ElasticMessage setException(Exception exception) {
        this.exception = exception;
        return this;
    }

    public boolean isValidData() {
        return !hasException();
    }

    public boolean hasException() {
        return getException() != null;
    }
}
