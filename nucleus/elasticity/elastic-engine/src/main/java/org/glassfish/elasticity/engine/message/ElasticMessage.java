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

    private String subComponentName;

    private String messageId;

    private String inResponseToMessageId;

    private boolean isResponseMessage;

    private Object data;

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

    public ElasticMessage setData(Object data) {
        this.data = data;
        return this;
    }
}
