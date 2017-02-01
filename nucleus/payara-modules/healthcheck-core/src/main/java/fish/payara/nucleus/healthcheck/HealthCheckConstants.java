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

    final long ONE_KB = 1024;
    final long ONE_MB = ONE_KB * ONE_KB;
    final long ONE_GB = ONE_KB * ONE_MB;

    final long ONE_SEC = 1000;
    final long ONE_MIN = 60 * ONE_SEC;
    final long FIVE_MIN = 5 * ONE_MIN;

    final String THRESHOLD_CRITICAL = "threshold-critical";
    final String THRESHOLD_WARNING = "threshold-warning";
    final String THRESHOLD_GOOD = "threshold-good";
    final String THRESHOLD_DEFAULTVAL_CRITICAL = "80";
    final String THRESHOLD_DEFAULTVAL_WARNING = "50";
    final String THRESHOLD_DEFAULTVAL_GOOD = "0";

    final String YOUNG_COPY = "Copy";
    final String YOUNG_PS_SCAVENGE = "PS Scavenge";
    final String YOUNG_PARNEW = "ParNew";
    final String YOUNG_G1GC = "G1 Young Generation";

    final String OLD_MARK_SWEEP_COMPACT = "MarkSweepCompact";
    final String OLD_PS_MARKSWEEP = "PS MarkSweep";
    final String OLD_CONCURRENTMARKSWEEP = "ConcurrentMarkSweep";
    final String OLD_G1GC = "G1 Old Generation";
    
    final String DEFAULT_ENABLED = "false";
    final String DEFAULT_TIME = "5";
    final String DEFAULT_UNIT = "MINUTES";
    final String DEFAULT_RETRY_COUNT = "3";
    final String DEFAULT_THRESHOLD_PERCENTAGE = "95";
    
    final String DEFAULT_GARBAGE_COLLECTOR_NAME = "GBGC";
    final String DEFAULT_CONNECTION_POOL_NAME = "CONP";
    final String DEFAULT_CPU_USAGE_NAME = "CPUC";
    final String DEFAULT_HEAP_MEMORY_USAGE_NAME = "HEAP";
    final String DEFAULT_MACHINE_MEMORY_USAGE_NAME = "MEMM";
    final String DEFAULT_HOGGING_THREADS_NAME = "HOGT";
}
