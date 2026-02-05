/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2026 Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").
 */
package fish.payara.samples.agentic.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Result of fraud analysis performed by the FraudDetectionAgent.
 */
public class FraudAnalysisResult {

    public enum RiskLevel {
        LOW, MEDIUM, HIGH, CRITICAL
    }

    private String transactionId;
    private RiskLevel riskLevel;
    private double riskScore;
    private boolean fraudulent;
    private List<String> riskFactors;
    private String recommendation;
    private String llmAnalysis;

    public FraudAnalysisResult() {
        this.riskFactors = new ArrayList<>();
        this.riskLevel = RiskLevel.LOW;
        this.riskScore = 0.0;
    }

    public FraudAnalysisResult(String transactionId) {
        this();
        this.transactionId = transactionId;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public RiskLevel getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(RiskLevel riskLevel) {
        this.riskLevel = riskLevel;
    }

    public double getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(double riskScore) {
        this.riskScore = riskScore;
        // Automatically set risk level based on score
        if (riskScore >= 0.9) {
            this.riskLevel = RiskLevel.CRITICAL;
        } else if (riskScore >= 0.7) {
            this.riskLevel = RiskLevel.HIGH;
        } else if (riskScore >= 0.4) {
            this.riskLevel = RiskLevel.MEDIUM;
        } else {
            this.riskLevel = RiskLevel.LOW;
        }
    }

    public boolean isFraudulent() {
        return fraudulent;
    }

    public void setFraudulent(boolean fraudulent) {
        this.fraudulent = fraudulent;
    }

    public List<String> getRiskFactors() {
        return riskFactors;
    }

    public void addRiskFactor(String factor) {
        this.riskFactors.add(factor);
    }

    public String getRecommendation() {
        return recommendation;
    }

    public void setRecommendation(String recommendation) {
        this.recommendation = recommendation;
    }

    public String getLlmAnalysis() {
        return llmAnalysis;
    }

    public void setLlmAnalysis(String llmAnalysis) {
        this.llmAnalysis = llmAnalysis;
    }

    @Override
    public String toString() {
        return "FraudAnalysisResult{" +
                "transactionId='" + transactionId + '\'' +
                ", riskLevel=" + riskLevel +
                ", riskScore=" + riskScore +
                ", fraudulent=" + fraudulent +
                ", riskFactors=" + riskFactors +
                ", recommendation='" + recommendation + '\'' +
                '}';
    }
}
