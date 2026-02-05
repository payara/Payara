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

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Represents a financial transaction for fraud detection analysis.
 */
public class Transaction {

    private String transactionId;
    private String accountId;
    private BigDecimal amount;
    private String merchantName;
    private String merchantCategory;
    private String location;
    private LocalDateTime timestamp;
    private boolean flaggedAsSuspicious;

    public Transaction() {
        this.timestamp = LocalDateTime.now();
    }

    public Transaction(String transactionId, String accountId, BigDecimal amount,
                       String merchantName, String merchantCategory, String location) {
        this();
        this.transactionId = transactionId;
        this.accountId = accountId;
        this.amount = amount;
        this.merchantName = merchantName;
        this.merchantCategory = merchantCategory;
        this.location = location;
    }

    // Getters and setters
    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getMerchantName() {
        return merchantName;
    }

    public void setMerchantName(String merchantName) {
        this.merchantName = merchantName;
    }

    public String getMerchantCategory() {
        return merchantCategory;
    }

    public void setMerchantCategory(String merchantCategory) {
        this.merchantCategory = merchantCategory;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isFlaggedAsSuspicious() {
        return flaggedAsSuspicious;
    }

    public void setFlaggedAsSuspicious(boolean flaggedAsSuspicious) {
        this.flaggedAsSuspicious = flaggedAsSuspicious;
    }

    @Override
    public String toString() {
        return "Transaction{" +
                "transactionId='" + transactionId + '\'' +
                ", accountId='" + accountId + '\'' +
                ", amount=" + amount +
                ", merchantName='" + merchantName + '\'' +
                ", location='" + location + '\'' +
                ", flaggedAsSuspicious=" + flaggedAsSuspicious +
                '}';
    }
}
