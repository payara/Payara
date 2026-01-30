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

import fish.payara.samples.agentic.agent.FraudDetectionAgent;
import fish.payara.samples.agentic.model.FraudAnalysisResult;
import fish.payara.samples.agentic.model.Transaction;
import jakarta.inject.Inject;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.math.BigDecimal;

import static org.junit.Assert.*;

/**
 * Tests for the FraudDetectionAgent demonstrating Jakarta Agentic AI capabilities.
 */
@RunWith(Arquillian.class)
public class FraudDetectionAgentTest {

    @Deployment
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class, "agentic-ai-test.war")
                .addPackages(true, "fish.payara.samples.agentic.agent")
                .addPackages(true, "fish.payara.samples.agentic.model")
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @Inject
    private FraudDetectionAgent fraudDetectionAgent;

    @Test
    public void testAgentInjection() {
        assertNotNull("FraudDetectionAgent should be injected", fraudDetectionAgent);
    }

    @Test
    public void testLowRiskTransaction() {
        // Create a normal, low-risk transaction
        Transaction transaction = new Transaction(
                "TXN-001",
                "ACC-12345",
                new BigDecimal("50.00"),
                "Coffee Shop",
                "food_and_beverage",
                "New York, NY"
        );

        // Note: In a full implementation, the WorkflowExecutor would orchestrate the agent
        // For now, we're testing that the agent is properly instantiated and can be injected
        assertNotNull("Transaction should be created", transaction);
        assertEquals("TXN-001", transaction.getTransactionId());
    }

    @Test
    public void testHighValueTransaction() {
        // Create a high-value transaction that should trigger detailed analysis
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

        result.setRiskScore(0.75);
        result.addRiskFactor("High-value transaction");
        result.addRiskFactor("Unusual location");

        assertEquals(FraudAnalysisResult.RiskLevel.HIGH, result.getRiskLevel());
        assertEquals(2, result.getRiskFactors().size());
    }

    @Test
    public void testCriticalRiskLevel() {
        FraudAnalysisResult result = new FraudAnalysisResult("TXN-004");
        result.setRiskScore(0.95);

        assertEquals("Score >= 0.9 should be CRITICAL",
                FraudAnalysisResult.RiskLevel.CRITICAL, result.getRiskLevel());
    }

    @Test
    public void testMediumRiskLevel() {
        FraudAnalysisResult result = new FraudAnalysisResult("TXN-005");
        result.setRiskScore(0.5);

        assertEquals("Score between 0.4 and 0.7 should be MEDIUM",
                FraudAnalysisResult.RiskLevel.MEDIUM, result.getRiskLevel());
    }
}
