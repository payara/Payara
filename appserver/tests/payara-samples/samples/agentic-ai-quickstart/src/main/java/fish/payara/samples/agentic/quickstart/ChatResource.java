/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2026 Payara Foundation and/or its affiliates. All rights reserved.
 */
package fish.payara.samples.agentic.quickstart;

import fish.payara.samples.agentic.quickstart.model.ChatRequest;
import fish.payara.samples.agentic.quickstart.model.ChatResponse;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * REST API endpoint for the customer support chat agent.
 * <p>
 * Provides endpoints for:
 * <ul>
 *     <li>POST /api/chat - Send a message and get a response</li>
 *     <li>DELETE /api/chat/{sessionId} - Clear conversation history</li>
 * </ul>
 * <p>
 * Example usage with curl:
 * <pre>
 * # Start a new conversation
 * curl -X POST http://localhost:8080/agentic-ai-quickstart/api/chat \
 *   -H "Content-Type: application/json" \
 *   -d '{"message": "Hello, I need help with my order"}'
 *
 * # Continue conversation with session ID
 * curl -X POST http://localhost:8080/agentic-ai-quickstart/api/chat \
 *   -H "Content-Type: application/json" \
 *   -d '{"message": "My order number is 12345", "sessionId": "your-session-id"}'
 *
 * # Clear conversation history
 * curl -X DELETE http://localhost:8080/agentic-ai-quickstart/api/chat/your-session-id
 * </pre>
 */
@Path("/chat")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ChatResource {

    private static final Logger logger = Logger.getLogger(ChatResource.class.getName());

    @Inject
    private CustomerSupportAgent agent;

    /**
     * Process a chat message and return an AI-generated response.
     *
     * @param request the chat request containing the message and optional session ID
     * @return the chat response with the AI's reply
     */
    @POST
    public Response chat(ChatRequest request) {
        long startTime = System.currentTimeMillis();

        // Validate request
        if (request == null || request.getMessage() == null || request.getMessage().isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ChatResponse.error("Message is required"))
                    .build();
        }

        // Generate or use provided session ID
        String sessionId = request.getSessionId();
        if (sessionId == null || sessionId.isBlank()) {
            sessionId = UUID.randomUUID().toString();
        }

        logger.log(Level.INFO, "Chat request - session: {0}, message length: {1}",
                new Object[]{sessionId, request.getMessage().length()});

        try {
            // Get response from the agent
            String response = agent.chat(sessionId, request.getMessage());

            // Build success response
            ChatResponse chatResponse = ChatResponse.success(response, agent.getName());
            chatResponse.setSessionId(sessionId);
            chatResponse.setProcessingTimeMs(System.currentTimeMillis() - startTime);

            logger.log(Level.INFO, "Chat response generated in {0}ms for session {1}",
                    new Object[]{chatResponse.getProcessingTimeMs(), sessionId});

            return Response.ok(chatResponse).build();

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error processing chat request", e);

            ChatResponse errorResponse = ChatResponse.error(
                    "Failed to process your request: " + e.getMessage());
            errorResponse.setSessionId(sessionId);
            errorResponse.setProcessingTimeMs(System.currentTimeMillis() - startTime);

            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(errorResponse)
                    .build();
        }
    }

    /**
     * Clear the conversation history for a session.
     *
     * @param sessionId the session ID to clear
     * @return 204 No Content on success
     */
    @DELETE
    @Path("/{sessionId}")
    public Response clearSession(@PathParam("sessionId") String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ChatResponse.error("Session ID is required"))
                    .build();
        }

        agent.clearHistory(sessionId);
        logger.log(Level.INFO, "Cleared session: {0}", sessionId);

        return Response.noContent().build();
    }
}
