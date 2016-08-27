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
package fish.payara.nucleus.notification;

import java.util.concurrent.TimeUnit;

/**
 * @author mertcaliskan
 *
 */
public final class TimeHelper {

    private static long ONE_SEC = 1000;
    private static long ONE_MIN = 60 * ONE_SEC;

    public static String prettyPrintDuration(long value) {
        long minutes = 0;
        long seconds = 0;
        StringBuilder sb = new StringBuilder();

        if (value > ONE_MIN) {
            minutes = TimeUnit.MILLISECONDS.toMinutes(value);
            value -= TimeUnit.MINUTES.toMillis(minutes);
        }
        if (value > ONE_SEC) {
            seconds = TimeUnit.MILLISECONDS.toSeconds(value);
            value -= TimeUnit.SECONDS.toMillis(seconds);
        }
        if (value >= 0) {
            if (minutes > 0) {
                sb.append(minutes).append(" minutes ");
            }
            if (seconds > 0) {
                sb.append(seconds).append(" seconds ");
            }
            if (value > 0) {
                sb.append(value);
                sb.append(" milliseconds");
            }
            return sb.toString();
        }
        else {
            return null;
        }
    }
}
