/*
 DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 Copyright (c) 2016 Payara Foundation. All rights reserved.
 The contents of this file are subject to the terms of the Common Development
 and Distribution License("CDDL") (collectively, the "License").  You
 may not use this file except in compliance with the License.  You can
 obtain a copy of the License at
 https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 or packager/legal/LICENSE.txt.  See the License for the specific
 language governing permissions and limitations under the License.
 When distributing the software, include this License Header Notice in each
 file and include the License file at packager/legal/LICENSE.txt.
 */
package fish.payara.nucleus.healthcheck.entity;

/**
 * @author mertcaliskan
 */
public class ThreadTimes {

    private long id;
    private String name;
    private long initialStartCpuTime;
    private long startCpuTime;
    private long initialStartUserTime;
    private long startUserTime;
    private long endCpuTime;
    private long endUserTime;
    private int retryCount;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getStartCpuTime() {
        return startCpuTime;
    }

    public long getInitialStartCpuTime() {
        return initialStartCpuTime;
    }

    public void setInitialStartCpuTime(long initialStartCpuTime) {
        this.initialStartCpuTime = initialStartCpuTime;
    }

    public void setStartCpuTime(long startCpuTime) {
        this.startCpuTime = startCpuTime;
    }

    public long getInitialStartUserTime() {
        return initialStartUserTime;
    }

    public void setInitialStartUserTime(long initialStartUserTime) {
        this.initialStartUserTime = initialStartUserTime;
    }

    public long getStartUserTime() {
        return startUserTime;
    }

    public void setStartUserTime(long startUserTime) {
        this.startUserTime = startUserTime;
    }

    public long getEndCpuTime() {
        return endCpuTime;
    }

    public void setEndCpuTime(long endCpuTime) {
        this.endCpuTime = endCpuTime;
    }

    public long getEndUserTime() {
        return endUserTime;
    }

    public void setEndUserTime(long endUserTime) {
        this.endUserTime = endUserTime;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    @Override
    public String toString() {
        return "Times{" +
                "id=" + id +
                ", startCpuTime=" + startCpuTime +
                ", endCpuTime=" + endCpuTime +
                ", startUserTime=" + startUserTime +
                ", endUserTime=" + endUserTime +
                '}';
    }
}