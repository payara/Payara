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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * @author mertcaliskan
 */
public class HealthCheckResult implements Iterable<HealthCheckResultEntry> {

    private List<HealthCheckResultEntry> entries = new LinkedList<HealthCheckResultEntry>();
    private HealthCheckResultStatus cumulativeStatus;

    @Override
    public Iterator<HealthCheckResultEntry> iterator() {
        return this.entries.iterator();
    }

    public void add(HealthCheckResultEntry e) {
        if (entries.isEmpty()) {
            cumulativeStatus = HealthCheckResultStatus.GOOD;
        }
        entries.add(e);
        if (e.getStatus().getLevel() < cumulativeStatus.getLevel()) {
            cumulativeStatus = e.getStatus();
        }
    }

    public HealthCheckResultStatus getCumulativeStatus() {
        return cumulativeStatus;
    }

    public String getCumulativeMessages() {
        return "Health Check Result:" + this.entries.toString();
    }
}