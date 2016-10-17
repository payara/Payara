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
 * @author mertcaliskan
 */
public class HealthCheckResultEntry {

    private HealthCheckResultStatus status;
    private String message;
    private Exception exception;

    public HealthCheckResultEntry(HealthCheckResultStatus status, String msg) {
        this.status = status;
        this.message = msg;
    }

    public HealthCheckResultEntry(HealthCheckResultStatus status, String msg, Exception ex) {
        this.status = status;
        this.message = msg;
        this.exception = ex;
    }

    public HealthCheckResultStatus getStatus() {
        return status;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[status=" + status);
        sb.append(", message='" + message + '\'');
        if (exception != null) {
            sb.append(", exception=" + exception);
        }
        sb.append("']'");

        return sb.toString();
    }
}