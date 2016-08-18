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

import fish.payara.nucleus.healthcheck.preliminary.BaseHealthCheck;

import java.util.logging.Level;

/**
 * @author steve
 */
class HealthCheckTask implements Runnable {

    private final String name;
    private final BaseHealthCheck check;

    HealthCheckTask(String name, BaseHealthCheck check) {
        this.name = name;
        this.check = check;
    }

    String getName() {
        return name;
    }

    public BaseHealthCheck getCheck() {
        return check;
    }

    @Override
    public void run() {
        if (check.getOptions().isEnabled()) {
            HealthCheckResult checkResult = check.doCheck();

            if (checkResult != null && checkResult.getCumulativeStatus() != null) {
                Level level;
                switch (checkResult.getCumulativeStatus()) {
                    case CHECK_ERROR:
                        level = Level.SEVERE;
                        break;
                    case CRITICAL:
                        level = Level.SEVERE;
                        break;
                    case WARNING:
                        level = Level.WARNING;
                        break;
                    case GOOD:
                        level = Level.INFO;
                        break;
                    default:
                        level = Level.OFF;
                        break;
                    case FINE:
                        level = Level.FINE;
                        break;
                }
                check.sendNotification(level, "{0}:{1}", new Object[]{name, checkResult.getCumulativeMessages()});
            }
        }
    }
}