/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2026 Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").
 */
package fish.payara.samples.agentic.agent;

import fish.payara.samples.agentic.model.FraudAnalysisResult;
import fish.payara.samples.agentic.model.Transaction;
import jakarta.ai.agent.Action;
import jakarta.ai.agent.Agent;
import jakarta.ai.agent.Decision;
import jakarta.ai.agent.HandleException;
import jakarta.ai.agent.LargeLanguageModel;
import jakarta.ai.agent.Outcome;
import jakarta.ai.agent.Result;
import jakarta.ai.agent.Trigger;
import jakarta.ai.agent.WorkflowContext;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An AI Agent for detecting fraudulent transactions.
 * <p>
 * This agent demonstrates the Jakarta Agentic AI workflow:
 * <ol>
 *     <li>Trigger: Receives a transaction for analysis</li>
 *     <li>Decision: Determines if the transaction needs detailed analysis</li>
 *     <li>Action: Analyzes transaction patterns and consults LLM</li>
 *     <li>Outcome: Returns the fraud analysis result</li>
 * </ol>
 *
 * @author Luis Neto
 */
@Agent(name = "fraudDetectionAgent", description = "Analyzes transactions for potential fraud using AI")
public class FraudDetectionAgent {

    private static final Logger logger = Logger.getLogger(FraudDetectionAgent.class.getName());

    private static final BigDecimal HIGH_VALUE_THRESHOLD = new BigDecimal("10000");
    private static final BigDecimal MEDIUM_VALUE_THRESHOLD = new BigDecimal("1000");

    @Inject
    private LargeLanguageModel llm;

    @Inject
    private WorkflowContext context;

    /**
     * Trigger method - entry point for the fraud detection workflow.
     * Called when a transaction needs to be analyzed.
     */
    @Trigger
    public void analyzeTransaction(Transaction transaction) {
        logger.log(Level.INFO, "Starting fraud analysis for transaction: {0}", transaction.getTransactionId());

        // Store transaction in workflow context for use by other methods
        context.setAttribute("transaction", transaction);
        context.setAttribute("result", new FraudAnalysisResult(transaction.getTransactionId()));
    }

    /**
     * Decision method - determines if detailed analysis is needed.
     * Returns true to continue with actions, false to skip to outcome.
     */
    @Decision
    public Result shouldPerformDetailedAnalysis() {
        Transaction transaction = (Transaction) context.getAttribute("transaction");
        FraudAnalysisResult result = (FraudAnalysisResult) context.getAttribute("result");

        // Basic risk assessment
        double riskScore = 0.0;

        // High-value transactions are always suspicious
        if (transaction.getAmount().compareTo(HIGH_VALUE_THRESHOLD) > 0) {
            riskScore += 0.3;
            result.addRiskFactor("High-value transaction: " + transaction.getAmount());
        } else if (transaction.getAmount().compareTo(MEDIUM_VALUE_THRESHOLD) > 0) {
            riskScore += 0.1;
        }

        // Check for suspicious merchant categories
        String category = transaction.getMerchantCategory();
        if (category != null && (category.contains("gambling") || category.contains("crypto"))) {
            riskScore += 0.2;
            result.addRiskFactor("High-risk merchant category: " + category);
        }

        // Already flagged transactions
        if (transaction.isFlaggedAsSuspicious()) {
            riskScore += 0.4;
            result.addRiskFactor("Pre-flagged as suspicious");
        }

        result.setRiskScore(riskScore);
        context.setAttribute("result", result);

        // Perform detailed analysis if risk score warrants it
        boolean needsDetailedAnalysis = riskScore >= 0.2;
        logger.log(Level.INFO, "Initial risk score: {0}, detailed analysis needed: {1}",
                new Object[]{riskScore, needsDetailedAnalysis});

        return new Result(needsDetailedAnalysis, result);
    }

    /**
     * Action method - performs rule-based pattern analysis.
     */
    @Action
    public void performPatternAnalysis() {
        Transaction transaction = (Transaction) context.getAttribute("transaction");
        FraudAnalysisResult result = (FraudAnalysisResult) context.getAttribute("result");

        logger.log(Level.INFO, "Performing pattern analysis for: {0}", transaction.getTransactionId());

        double additionalRisk = 0.0;

        // Location-based analysis
        if (transaction.getLocation() != null) {
            String location = transaction.getLocation().toLowerCase();
            if (location.contains("foreign") || location.contains("overseas")) {
                additionalRisk += 0.15;
                result.addRiskFactor("Foreign transaction location: " + transaction.getLocation());
            }
        }

        // Time-based analysis (simplified)
        int hour = transaction.getTimestamp().getHour();
        if (hour >= 0 && hour < 6) {
            additionalRisk += 0.1;
            result.addRiskFactor("Unusual transaction time: " + hour + ":00");
        }

        // Update risk score
        result.setRiskScore(result.getRiskScore() + additionalRisk);
        context.setAttribute("result", result);
    }

    /**
     * Action method - consults the LLM for advanced analysis.
     */
    @Action
    public void consultLLMForAnalysis() {
        Transaction transaction = (Transaction) context.getAttribute("transaction");
        FraudAnalysisResult result = (FraudAnalysisResult) context.getAttribute("result");

        logger.log(Level.INFO, "Consulting LLM for transaction: {0}", transaction.getTransactionId());

        // Build prompt for LLM
        String prompt = String.format(
                "Analyze this transaction for potential fraud indicators:\n" +
                        "- Amount: %s\n" +
                        "- Merchant: %s (%s)\n" +
                        "- Location: %s\n" +
                        "- Current risk factors: %s\n" +
                        "Provide a brief assessment.",
                transaction.getAmount(),
                transaction.getMerchantName(),
                transaction.getMerchantCategory(),
                transaction.getLocation(),
                result.getRiskFactors()
        );

        try {
            String llmResponse = llm.query(prompt);
            result.setLlmAnalysis(llmResponse);
            logger.log(Level.INFO, "LLM analysis: {0}", llmResponse);

            // Adjust risk based on LLM response (simplified)
            if (llmResponse != null && llmResponse.toLowerCase().contains("fraud")) {
                result.setRiskScore(result.getRiskScore() + 0.2);
                result.addRiskFactor("LLM flagged potential fraud indicators");
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "LLM consultation failed, continuing with rule-based analysis", e);
            result.setLlmAnalysis("LLM analysis unavailable");
        }

        context.setAttribute("result", result);
    }

    /**
     * Outcome method - finalizes the fraud analysis result.
     */
    @Outcome
    public FraudAnalysisResult finalizeAnalysis() {
        FraudAnalysisResult result = (FraudAnalysisResult) context.getAttribute("result");

        // Determine if transaction is fraudulent based on final risk score
        result.setFraudulent(result.getRiskScore() >= 0.7);

        // Generate recommendation
        String recommendation;
        switch (result.getRiskLevel()) {
            case CRITICAL:
                recommendation = "BLOCK transaction immediately and alert security team";
                break;
            case HIGH:
                recommendation = "HOLD transaction for manual review";
                break;
            case MEDIUM:
                recommendation = "FLAG for monitoring but allow transaction";
                break;
            default:
                recommendation = "APPROVE transaction";
        }
        result.setRecommendation(recommendation);

        logger.log(Level.INFO, "Fraud analysis complete: {0}", result);
        return result;
    }

    /**
     * Exception handler - handles any errors during the workflow.
     */
    @HandleException
    public FraudAnalysisResult handleError(Exception exception, WorkflowContext ctx) {
        logger.log(Level.SEVERE, "Error in fraud detection workflow", exception);

        Transaction transaction = (Transaction) ctx.getAttribute("transaction");
        FraudAnalysisResult result = new FraudAnalysisResult(
                transaction != null ? transaction.getTransactionId() : "unknown"
        );
        result.setRiskLevel(FraudAnalysisResult.RiskLevel.HIGH);
        result.setRecommendation("HOLD - Analysis failed, requires manual review");
        result.addRiskFactor("Workflow error: " + exception.getMessage());

        return result;
    }
}
