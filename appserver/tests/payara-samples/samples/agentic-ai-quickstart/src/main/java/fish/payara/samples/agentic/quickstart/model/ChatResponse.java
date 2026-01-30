/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2026 Payara Foundation and/or its affiliates. All rights reserved.
 */
package fish.payara.samples.agentic.quickstart.model;

import java.time.LocalDateTime;

/**
 * Response payload for the chat API.
 */
public class ChatResponse {

    private String response;
    private String sessionId;
    private String agentName;
    private LocalDateTime timestamp;
    private long processingTimeMs;
    private boolean success;
    private String error;

    public ChatResponse() {
        this.timestamp = LocalDateTime.now();
        this.success = true;
    }

    public static ChatResponse success(String response, String agentName) {
        ChatResponse r = new ChatResponse();
        r.setResponse(response);
        r.setAgentName(agentName);
        return r;
    }

    public static ChatResponse error(String errorMessage) {
        ChatResponse r = new ChatResponse();
        r.setSuccess(false);
        r.setError(errorMessage);
        return r;
    }

    // Getters and setters
    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getAgentName() {
        return agentName;
    }

    public void setAgentName(String agentName) {
        this.agentName = agentName;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public long getProcessingTimeMs() {
        return processingTimeMs;
    }

    public void setProcessingTimeMs(long processingTimeMs) {
        this.processingTimeMs = processingTimeMs;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}
