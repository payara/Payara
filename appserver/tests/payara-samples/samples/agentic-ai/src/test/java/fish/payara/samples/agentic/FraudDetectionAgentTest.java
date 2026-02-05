/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2026 Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").
 */
package fish.payara.samples.agentic;

import fish.payara.samples.agentic.model.FraudAnalysisResult;
import fish.payara.samples.agentic.model.Transaction;
import org.junit.Test;

import java.math.BigDecimal;

import static org.junit.Assert.*;

/**
 * Unit tests for the FraudDetection models demonstrating Jakarta Agentic AI data structures.
 *
 * These are unit tests that validate the model classes used by the FraudDetectionAgent.
 * Integration tests for the full agent workflow require a running Payara container with
 * the Jakarta Agentic AI implementation.
 */
public class FraudDetectionAgentTest {

    @Test
    public void testTransactionCreation() {
        Transaction transaction = new Transaction(
                "TXN-001",
                "ACC-12345",
                new BigDecimal("50.00"),
                "Coffee Shop",
                "food_and_beverage",
                "New York, NY"
        );

        assertNotNull("Transaction should be created", transaction);
        assertEquals("TXN-001", transaction.getTransactionId());
        assertEquals("ACC-12345", transaction.getAccountId());
        assertEquals(new BigDecimal("50.00"), transaction.getAmount());
        assertEquals("Coffee Shop", transaction.getMerchantName());
        assertEquals("food_and_beverage", transaction.getMerchantCategory());
        assertEquals("New York, NY", transaction.getLocation());
        assertNotNull("Timestamp should be set", transaction.getTimestamp());
        assertFalse("Should not be flagged by default", transaction.isFlaggedAsSuspicious());
    }

    @Test
    public void testHighValueTransaction() {
        Transaction transaction = new Transaction(
                "TXN-002",
                "ACC-12345",
                new BigDecimal("15000.00"),
                "Luxury Watches",
                "jewelry",
                "Foreign Location"
        );
        transaction.setFlaggedAsSuspicious(true);

        assertTrue("High value transaction should be flagged", transaction.isFlaggedAsSuspicious());
        assertTrue("Amount should be high", transaction.getAmount().compareTo(new BigDecimal("10000")) > 0);
    }

    @Test
    public void testFraudAnalysisResultCreation() {
        FraudAnalysisResult result = new FraudAnalysisResult("TXN-003");

        assertNotNull("Result should be created", result);
        assertEquals("TXN-003", result.getTransactionId());
        assertEquals(FraudAnalysisResult.RiskLevel.LOW, result.getRiskLevel());
        assertEquals(0.0, result.getRiskScore(), 0.001);
        assertFalse("Should not be fraudulent by default", result.isFraudulent());
        assertNotNull("Risk factors list should be initialized", result.getRiskFactors());
        assertTrue("Risk factors should be empty", result.getRiskFactors().isEmpty());
    }

    @Test
    public void testFraudAnalysisResultWithRiskFactors() {
        FraudAnalysisResult result = new FraudAnalysisResult("TXN-003");

        result.setRiskScore(0.75);
        result.addRiskFactor("High-value transaction");
        result.addRiskFactor("Unusual location");

        assertEquals(FraudAnalysisResult.RiskLevel.HIGH, result.getRiskLevel());
        assertEquals(2, result.getRiskFactors().size());
        assertTrue(result.getRiskFactors().contains("High-value transaction"));
        assertTrue(result.getRiskFactors().contains("Unusual location"));
    }

    @Test
    public void testLowRiskLevel() {
        FraudAnalysisResult result = new FraudAnalysisResult("TXN-LOW");
        result.setRiskScore(0.2);

        assertEquals("Score < 0.4 should be LOW",
                FraudAnalysisResult.RiskLevel.LOW, result.getRiskLevel());
    }

    @Test
    public void testMediumRiskLevel() {
        FraudAnalysisResult result = new FraudAnalysisResult("TXN-MED");
        result.setRiskScore(0.5);

        assertEquals("Score between 0.4 and 0.7 should be MEDIUM",
                FraudAnalysisResult.RiskLevel.MEDIUM, result.getRiskLevel());
    }

    @Test
    public void testHighRiskLevel() {
        FraudAnalysisResult result = new FraudAnalysisResult("TXN-HIGH");
        result.setRiskScore(0.8);

        assertEquals("Score between 0.7 and 0.9 should be HIGH",
                FraudAnalysisResult.RiskLevel.HIGH, result.getRiskLevel());
    }

    @Test
    public void testCriticalRiskLevel() {
        FraudAnalysisResult result = new FraudAnalysisResult("TXN-CRIT");
        result.setRiskScore(0.95);

        assertEquals("Score >= 0.9 should be CRITICAL",
                FraudAnalysisResult.RiskLevel.CRITICAL, result.getRiskLevel());
    }

    @Test
    public void testFraudAnalysisResultSetters() {
        FraudAnalysisResult result = new FraudAnalysisResult();

        result.setTransactionId("TXN-SET");
        result.setFraudulent(true);
        result.setRecommendation("BLOCK transaction");
        result.setLlmAnalysis("AI analysis indicates fraud patterns");

        assertEquals("TXN-SET", result.getTransactionId());
        assertTrue(result.isFraudulent());
        assertEquals("BLOCK transaction", result.getRecommendation());
        assertEquals("AI analysis indicates fraud patterns", result.getLlmAnalysis());
    }

    @Test
    public void testTransactionToString() {
        Transaction transaction = new Transaction(
                "TXN-STR",
                "ACC-123",
                new BigDecimal("100.00"),
                "Test Merchant",
                "retail",
                "Test Location"
        );

        String str = transaction.toString();
        assertTrue("toString should contain transaction ID", str.contains("TXN-STR"));
        assertTrue("toString should contain account ID", str.contains("ACC-123"));
        assertTrue("toString should contain merchant name", str.contains("Test Merchant"));
    }

    @Test
    public void testFraudAnalysisResultToString() {
        FraudAnalysisResult result = new FraudAnalysisResult("TXN-STR");
        result.setRiskScore(0.5);
        result.setRecommendation("FLAG for monitoring");

        String str = result.toString();
        assertTrue("toString should contain transaction ID", str.contains("TXN-STR"));
        assertTrue("toString should contain risk level", str.contains("MEDIUM"));
        assertTrue("toString should contain recommendation", str.contains("FLAG for monitoring"));
    }
}
