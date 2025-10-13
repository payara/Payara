/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */
// Portions Copyright [2018-2019] [Payara Foundation and/or its affiliates]

package com.sun.enterprise.universal;

/**
 * Convert a nanosec duration into something readable
 * @author bnevins
 * Thread Safe.
 * Immutable
 */
public final class NanoDuration {

    // possibly useful constants
    private static final double NSEC_PER_MICROSECOND = 1000;
    private static final double NSEC_PER_MILLISECOND = 1000d * 1000;
    private static final double NSEC_PER_SECOND = 1000d * 1000 * 1000;
    private static final double NSEC_PER_MINUTE = 60d * 1000 * 1000 * 1000;

    private double numSeconds = -1.0;
    private double numMilliSeconds = -1.0;
    private double numMicroSeconds = -1.0;
    private double numNanoSeconds = -1.0;
    private Duration duration;

    public NanoDuration(long nsec) {
        // if > 1 minute -- use Duration
        if(nsec >= NSEC_PER_MINUTE) {
            duration = new Duration(nsec / ((long)NSEC_PER_MILLISECOND));
            return;
        }

        if((double) nsec >= NSEC_PER_SECOND) {
            numSeconds = (double) nsec / NSEC_PER_SECOND;
        } else if((double) nsec >= NSEC_PER_MILLISECOND) {
            numMilliSeconds = (double) nsec / NSEC_PER_MILLISECOND;

        } else if((double) nsec >= NSEC_PER_MICROSECOND) {
            numMicroSeconds = (double) nsec / NSEC_PER_MICROSECOND;

        } else {
            numNanoSeconds = nsec; // not a double!
        }
    }

    @Override
    public String toString() {
        if(duration != null)
            return duration.toString();

        final String fmt = "%.2f %s";
        String s;

        if(numSeconds > 0.0)
            s = String.format(fmt, numSeconds, "seconds");
        else if(numMilliSeconds > 0.0)
            s = String.format(fmt, numMilliSeconds, "msec");
        else if(numMicroSeconds > 0.0)
            s = String.format(fmt, numMicroSeconds, "usec");
        else if(numNanoSeconds > 0.0)
            s = String.format(fmt, numNanoSeconds, "nsec");
        else
            s = String.format(fmt, 0.0, "nsec"); // unlikely!

        return s;
    }

}
