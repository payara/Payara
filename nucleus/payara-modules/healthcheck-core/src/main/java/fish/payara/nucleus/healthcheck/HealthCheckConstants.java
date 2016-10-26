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
public interface HealthCheckConstants {

    long ONE_KB = 1024;
    long ONE_MB = ONE_KB * ONE_KB;
    long ONE_GB = ONE_KB * ONE_MB;

    long ONE_SEC = 1000;
    long ONE_MIN = 60 * ONE_SEC;
    long FIVE_MIN = 5 * ONE_MIN;

    String THRESHOLD_CRITICAL = "threshold-critical";
    String THRESHOLD_WARNING = "threshold-warning";
    String THRESHOLD_GOOD = "threshold-good";
    String THRESHOLD_DEFAULTVAL_CRITICAL = "80";
    String THRESHOLD_DEFAULTVAL_WARNING = "50";
    String THRESHOLD_DEFAULTVAL_GOOD = "0";

    String YOUNG_COPY = "Copy";
    String YOUNG_PS_SCAVENGE = "PS Scavenge";
    String YOUNG_PARNEW = "ParNew";
    String YOUNG_G1GC = "G1 Young Generation";

    String OLD_MARK_SWEEP_COMPACT = "MarkSweepCompact";
    String OLD_PS_MARKSWEEP = "PS MarkSweep";
    String OLD_CONCURRENTMARKSWEEP = "ConcurrentMarkSweep";
    String OLD_G1GC = "G1 Old Generation";
}
