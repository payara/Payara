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

/**
 * @author steve
 */
public enum HealthCheckResultStatus {

    CHECK_ERROR(1),
    CRITICAL(2),
    WARNING(3),
    GOOD(4),
    FINE(5);

    private final int level;

    HealthCheckResultStatus(int level) {
        this.level = level;
    }

    public int getLevel() {
        return level;
    }
}