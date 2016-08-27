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
public class HealthCheckConnectionPoolExecutionOptions extends HealthCheckWithThresholdExecutionOptions {

    private String poolName;

    public HealthCheckConnectionPoolExecutionOptions(boolean enabled, long time, TimeUnit unit, String
            thresholdCritical, String thresholdWarning, String thresholdGood, String poolName) {
        super(enabled, time, unit, thresholdCritical, thresholdWarning, thresholdGood);
        this.poolName = poolName;
    }

    public String getPoolName() {
        return poolName;
    }

    public void setPoolName(String poolName) {
        this.poolName = poolName;
    }
}