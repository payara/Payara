/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008-2010 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
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

package com.sun.enterprise.universal;

import com.sun.enterprise.universal.i18n.LocalStringsImpl;

/**
 * Convert a msec duration into weeks, hours, minutes, seconds
 * @author bnevins
 * Thread Safe.  
 * Immutable
 */

public final class Duration {
    public Duration(long msec) {
        long msecLeftover = msec;
        
        numWeeks = msecLeftover / MSEC_PER_WEEK;
        msecLeftover -= numWeeks * MSEC_PER_WEEK;
        
        numDays = msecLeftover / MSEC_PER_DAY;
        msecLeftover -= numDays * MSEC_PER_DAY;
        
        numHours = msecLeftover / MSEC_PER_HOUR;
        msecLeftover -= numHours * MSEC_PER_HOUR;
        
        numMinutes = msecLeftover / MSEC_PER_MINUTE;
        msecLeftover -= numMinutes * MSEC_PER_MINUTE;
        
        numSeconds = msecLeftover / MSEC_PER_SECOND;
        msecLeftover -= numSeconds * MSEC_PER_SECOND;

        numMilliSeconds = msecLeftover;
    }

    /**
     * Use more compact output
     * ref: https://glassfish.dev.java.net/issues/show_bug.cgi?id=12606
     */
    final public void setTerse() {
        terse = true;
    }

    @Override
    public String toString() {
        if(terse)
            return toStringTerse();
        else
            return toStringRegular();
    }

    private String toStringTerse() {
        String s = "";

        if(numWeeks > 0)
            s = strings.get("weeks_t", numWeeks, numDays);
        else if(numDays > 0)
            s = strings.get("days_t", numDays);
        else if(numHours > 0)
            s = strings.get("hours_t", numHours, numMinutes);
        else if(numMinutes > 0)
            s = strings.get("minutes_t", numMinutes, numSeconds);
        else if(numSeconds > 0)
            s = strings.get("seconds_t", numSeconds);
        else
            s = strings.get("milliseconds_t", numMilliSeconds);

        return s;
    }

    private String toStringRegular() {
        String s = "";

        if(numWeeks > 0)
            s = strings.get("weeks", numWeeks, numDays, numHours, numMinutes, numSeconds);
        else if(numDays > 0)
            s = strings.get("days", numDays, numHours, numMinutes, numSeconds);
        else if(numHours > 0)
            s = strings.get("hours", numHours, numMinutes, numSeconds);
        else if(numMinutes > 0)
            s = strings.get("minutes", numMinutes, numSeconds);
        else if(numSeconds > 0)
            s = strings.get("seconds", numSeconds);
        else
            s = strings.get("milliseconds", numMilliSeconds);

        return s;
    }

    public final long numWeeks;
    public final long numDays;
    public final long numHours;
    public final long numMinutes;
    public final long numSeconds;
    public final long numMilliSeconds;

    // possibly useful constants
    public final static long MSEC_PER_SECOND = 1000; 
    public final static long MSEC_PER_MINUTE = 60 * MSEC_PER_SECOND; 
    public final static long MSEC_PER_HOUR = MSEC_PER_MINUTE * 60; 
    public final static long MSEC_PER_DAY = MSEC_PER_HOUR * 24;
    public final static long MSEC_PER_WEEK = MSEC_PER_DAY * 7;
    
    private final LocalStringsImpl strings = new LocalStringsImpl(Duration.class);
    private boolean terse;

}
