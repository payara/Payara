/*
 DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 Copyright (c) 2015 C2B2 Consulting Limited. All rights reserved.
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
public class HealthCheckWithThresholdExecutionOptions extends HealthCheckExecutionOptions {

    private int thresholdCritical;
    private int thresholdWarning;
    private int thresholdGood;

    public HealthCheckWithThresholdExecutionOptions(boolean enabled, long time, TimeUnit unit, String
            thresholdCritical, String thresholdWarning, String thresholdGood) {
        super(enabled, time, unit);
        this.thresholdCritical = Integer.parseInt(thresholdCritical);
        this.thresholdWarning = Integer.parseInt(thresholdWarning);
        this.thresholdGood = Integer.parseInt(thresholdGood);
    }

    public int getThresholdCritical() {
        return thresholdCritical;
    }

    public int getThresholdWarning() {
        return thresholdWarning;
    }

    public int getThresholdGood() {
        return thresholdGood;
    }

    public void setThresholdCritical(int thresholdCritical) {
        this.thresholdCritical = thresholdCritical;
    }

    public void setThresholdGood(int thresholdGood) {
        this.thresholdGood = thresholdGood;
    }

    public void setThresholdWarning(int thresholdWarning) {
        this.thresholdWarning = thresholdWarning;
    }
}
