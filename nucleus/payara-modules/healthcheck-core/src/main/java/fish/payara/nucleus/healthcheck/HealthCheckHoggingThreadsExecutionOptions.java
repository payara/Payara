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
package fish.payara.nucleus.healthcheck;

import java.util.concurrent.TimeUnit;

/**
 * @author mertcaliskan
 */
public class HealthCheckHoggingThreadsExecutionOptions extends HealthCheckExecutionOptions {

    private Long thresholdPercentage;
    private int retryCount;

    public HealthCheckHoggingThreadsExecutionOptions(boolean enabled, long time, TimeUnit unit, Long thresholdPercentage, int retryCount) {
        super(enabled, time, unit);
        this.thresholdPercentage = thresholdPercentage;
        this.retryCount = retryCount;
    }

    public Long getThresholdPercentage() {
        return thresholdPercentage;
    }

    public int getRetryCount() {
        return retryCount;
    }
}