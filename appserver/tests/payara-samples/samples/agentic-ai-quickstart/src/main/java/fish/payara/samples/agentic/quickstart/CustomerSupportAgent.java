/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2026 Payara Foundation and/or its affiliates. All rights reserved.
 */
package fish.payara.samples.agentic.quickstart;

import fish.payara.samples.agentic.quickstart.model.ChatMessage;

import jakarta.ai.agent.Agent;
import jakarta.ai.agent.LargeLanguageModel;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A customer support AI agent that uses a real LLM provider.
 * <p>
 * This agent demonstrates how to use Jakarta Agentic AI with real LLM providers
 * like Claude, OpenAI, Gemini, or Ollama. Configure your preferred provider in
 * microprofile-config.properties.
 * <p>
 * The agent maintains conversation history per session to provide contextual responses.
 */
@Agent(name = "CustomerSupportAgent",
       description = "An AI-powered customer support assistant")
@ApplicationScoped
public class CustomerSupportAgent {

    private static final Logger logger = Logger.getLogger(CustomerSupportAgent.class.getName());

    private static final String SYSTEM_PROMPT = """
            You are a helpful and friendly customer support assistant. Your role is to:
            - Answer questions clearly and concisely
            - Help users solve problems
            - Provide accurate information
            - Be polite and professional

            If you don't know the answer to something, say so honestly rather than making up information.
            Keep your responses focused and helpful.
            """;

    @Inject
    private LargeLanguageModel llm;

    // Session-based conversation history
    private final Map<String, List<ChatMessage>> conversationHistory = new ConcurrentHashMap<>();

    /**
     * Process a user message and generate an AI response.
     *
     * @param sessionId the session identifier for conversation context
     * @param userMessage the user's message
     * @return the AI assistant's response
     */
    public String chat(String sessionId, String userMessage) {
        logger.log(Level.FINE, "Processing message for session {0}: {1}",
                new Object[]{sessionId, userMessage});

        // Get or create conversation history for this session
        List<ChatMessage> history = conversationHistory.computeIfAbsent(
                sessionId, k -> new ArrayList<>());

        // Add user message to history
        history.add(ChatMessage.user(userMessage));

        // Build the prompt with conversation context
        String prompt = buildPromptWithHistory(history);

        try {
            // Call the LLM
            String response = llm.query(prompt);

            // Add assistant response to history
            history.add(ChatMessage.assistant(response));

            // Keep history manageable (last 20 messages)
            if (history.size() > 20) {
                history.subList(0, history.size() - 20).clear();
            }

            logger.log(Level.FINE, "Generated response for session {0}", sessionId);
            return response;

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error generating response", e);
            throw new RuntimeException("Failed to generate response: " + e.getMessage(), e);
        }
    }

    /**
     * Build a prompt that includes conversation history for context.
     */
    private String buildPromptWithHistory(List<ChatMessage> history) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("System: ").append(SYSTEM_PROMPT).append("\n\n");

        for (ChatMessage message : history) {
            switch (message.getRole()) {
                case USER -> prompt.append("User: ").append(message.getContent()).append("\n");
                case ASSISTANT -> prompt.append("Assistant: ").append(message.getContent()).append("\n");
                case SYSTEM -> prompt.append("System: ").append(message.getContent()).append("\n");
            }
        }

        prompt.append("Assistant: ");
        return prompt.toString();
    }

    /**
     * Clear conversation history for a session.
     *
     * @param sessionId the session to clear
     */
    public void clearHistory(String sessionId) {
        conversationHistory.remove(sessionId);
        logger.log(Level.INFO, "Cleared conversation history for session {0}", sessionId);
    }

    /**
     * Get the number of messages in a session's history.
     *
     * @param sessionId the session identifier
     * @return the number of messages, or 0 if no history exists
     */
    public int getHistorySize(String sessionId) {
        List<ChatMessage> history = conversationHistory.get(sessionId);
        return history != null ? history.size() : 0;
    }

    /**
     * Get the agent's name.
     *
     * @return the agent name
     */
    public String getName() {
        return "CustomerSupportAgent";
    }
}
