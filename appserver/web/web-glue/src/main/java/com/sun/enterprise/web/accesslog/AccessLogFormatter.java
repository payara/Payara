/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.web.accesslog;

import org.apache.catalina.Request;
import org.apache.catalina.Response;

import java.nio.CharBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * Abstract class defining an interface for appending access log entries to the
 * access log in a customized access log format.
 */
public abstract class AccessLogFormatter {

    /**
     * The set of month abbreviations for log messages.
     */
    static final String months[] =
    { "Jan", "Feb", "Mar", "Apr", "May", "Jun",
      "Jul", "Aug", "Sep", "Oct", "Nov", "Dec" };

    /**
     * When formatting log lines, we often use strings like this one (" ").
     */
    protected static final String SPACE = " ";

    /**
     * ThreadLocal for a date formatter to format Dates into a day string in the format
     * "dd".
     */
    protected volatile ThreadLocal<SimpleDateFormat> dayFormatter = null;

    /**
     * ThreadLocal for a date formatter to format a Date into a month string in the format
     * "MM".
     */
    protected volatile ThreadLocal<SimpleDateFormat> monthFormatter = null;

    /**
     * ThreadLocal for a date formatter to format a Date into a year string in the format
     * "yyyy".
     */
    protected volatile ThreadLocal<SimpleDateFormat> yearFormatter = null;

    /**
     * ThreadLocal for a date formatter to format a Date into a time in the format
     * "kk:mm:ss" (kk is a 24-hour representation of the hour).
     */
    protected volatile ThreadLocal<SimpleDateFormat> timeFormatter = null;

    /**
     * The time zone relative to GMT.
     */
    protected String timeZone = null;

    protected TimeZone tz = null;

    /*
     * Flag indicating whether we need to measure the time (in milliseconds)
     * that was spent on each request
     */
    protected boolean needTimeTaken;

    /**
     * The system time when we last updated the Date that this valve
     * uses for log lines.
     */
    private Date currentDate = null;


    /**
     * Constructor.
     *
     * Initialize the timeZone and currentDate.
     */
    public AccessLogFormatter() {
        tz = TimeZone.getDefault();
        timeZone = calculateTimeZoneOffset(tz.getRawOffset());
        currentDate = new Date(System.currentTimeMillis());
    }


    /**
     * Appends an access log entry line, with info obtained from the given
     * request and response objects, to the given CharBuffer.
     *
     * @param request The request object from which to obtain access log info
     * @param response The response object from which to obtain access log info
     * @param charBuffer The CharBuffer to which to append access log info
     */
    public abstract void appendLogEntry(Request request,
                                        Response response,
                                        CharBuffer charBuffer);


    /**
     * This method returns a Date object that is accurate to within one
     * second.  If a writerThread calls this method to get a Date and it's been
     * less than 1 second since a new Date was created, this method
     * simply gives out the same Date again so that the system doesn't
     * spend time creating Date objects unnecessarily.
     */
    protected synchronized Date getDate() {

        // Only create a new Date once per second, max.
        long systime = System.currentTimeMillis();
        if ((systime - currentDate.getTime()) > 1000) {
            currentDate = new Date(systime);
        }

        return currentDate;

    }

    protected String calculateTimeZoneOffset(long offset) {
        StringBuilder sb = new StringBuilder();
        if ((offset<0))  {
            sb.append("-");
            offset = -offset;
        } else {
            sb.append("+");
        }

        long hourOffset = offset/(1000*60*60);
        long minuteOffset = (offset/(1000*60)) % 60;

        if (hourOffset<10)
            sb.append("0");
        sb.append(hourOffset);

        if (minuteOffset<10)
            sb.append("0");
        sb.append(minuteOffset);

        return sb.toString();
    }


    /**
     * Return the month abbreviation for the specified month, which must
     * be a two-digit String.
     *
     * @param month Month number ("01" .. "12").
     */
    protected String lookup(String month) {

        int index;
        try {
            index = Integer.parseInt(month) - 1;
        } catch (Throwable t) {
            index = 0;  // Can not happen, in theory
        }
        return (months[index]);

    }


    /**
     * Has the time-taken token been specified in the access log pattern?
     *
     * @return true if the time-taken token has been specified in the access
     * log pattern, false otherwise.
     */
    public boolean needTimeTaken() {
        return needTimeTaken;
    }    
}
