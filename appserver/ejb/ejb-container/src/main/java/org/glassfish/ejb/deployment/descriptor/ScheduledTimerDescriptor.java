/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2012 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.ejb.deployment.descriptor;

import java.util.Date;

import com.sun.enterprise.deployment.DescribableDescriptor;
import com.sun.enterprise.deployment.MethodDescriptor;


/**
 * This class holds the metadata for a calendar-based timer.
 */
public class ScheduledTimerDescriptor extends DescribableDescriptor {

    public void setSecond(String s) {
        second_ = s;
    }

    public String getSecond() {
        return second_;
    }

    public void setMinute(String m) {
        minute_ = m;
    }

    public String getMinute() {
	    return minute_;
    }

    public void setHour(String h) {
        hour_ = h;
    }

    public String getHour() {
        return hour_;
    }

    public void setDayOfMonth(String d) {
        dayOfMonth_ = d;
    }

    public String getDayOfMonth() {
	    return dayOfMonth_;
    }

    public void setMonth(String m) {
        month_ = m;
    }

    public String getMonth() {
        return month_;
    }

    public void setDayOfWeek(String d) {
        dayOfWeek_ = d;
    }

    public String getDayOfWeek() {
	    return dayOfWeek_;
    }

    public void setYear(String y) {
        year_ = y;
    }

    public String getYear() {
        return year_;
    }

    public void setTimezone(String timezoneID) {
        timezoneID_ = timezoneID;
    }

    public String getTimezone() {
        return timezoneID_;
    }

    public void setStart(Date s) {
        start_ = (s == null) ? null : new Date(s.getTime());
    }

    public Date getStart() {
        return (start_ == null) ? null : new Date(start_.getTime());
    }

    public void setEnd(Date e) {
        end_ = (e == null) ? null : new Date(e.getTime());
    }

    public Date getEnd() {
        return (end_ == null) ? null : new Date(end_.getTime());
    }

    public void setPersistent(boolean flag) {
        persistent_ = flag;
    }

    public boolean getPersistent() {
        return persistent_;
    }

    public void setInfo(String i) {
        info_ = i;
    }

    public String getInfo() {
        return info_;
    }


    public void setTimeoutMethod(MethodDescriptor m) {
        timeoutMethod_ = m;
    }


    public MethodDescriptor getTimeoutMethod() {
        return timeoutMethod_;
    }

    public String toString() {
        return "ScheduledTimerDescriptor [second=" + second_
                + ";minute=" + minute_
                + ";hour=" + hour_
                + ";dayOfMonth=" + dayOfMonth_
                + ";month=" + month_
                + ";dayOfWeek=" + dayOfWeek_
                + ";year=" + year_
                + ";timezoneID=" + timezoneID_
                + ";start=" + start_
                + ";end=" + end_
                + ";" + timeoutMethod_ //MethodDescriptor prints it's name
                + ";persistent=" + persistent_ 
                + ";info=" + info_ 
                + "]";
    }


    private String second_ = "0";
    private String minute_ = "0";
    private String hour_ = "0";

    private String dayOfMonth_ = "*";
    private String month_ = "*";
    private String dayOfWeek_ = "*";
    private String year_ = "*";

    private String timezoneID_ = null;

    private Date start_ = null;

    private Date end_ = null;

    private MethodDescriptor timeoutMethod_;

    private boolean persistent_ = true;

    private String info_ = null;



}
