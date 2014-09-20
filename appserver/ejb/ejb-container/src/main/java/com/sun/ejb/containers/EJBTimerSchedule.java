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

package com.sun.ejb.containers;

import javax.ejb.ScheduleExpression;

import org.glassfish.common.util.timer.TimerSchedule;
import org.glassfish.ejb.deployment.descriptor.ScheduledTimerDescriptor;

/**
 * A runtime representation of the user-defined calendar-based 
 * timeout expression for an enterprise bean timer.
 *
 * @author mvatkina
 */

public class EJBTimerSchedule extends TimerSchedule {

    private boolean automatic_ = false;
    private String methodName_ = null;
    private int paramCount_ = 0;

    /** Construct EJBTimerSchedule instance with all defaults.
     */
    public EJBTimerSchedule() {
        super();
    }

    /** Construct EJBTimerSchedule instance from a given ScheduleExpression.
      * Need to copy all values because ScheduleExpression is mutable
      * and can be modified by the user.
      */
    public EJBTimerSchedule(ScheduleExpression se) {
        second(se.getSecond());
        minute(se.getMinute());
        hour(se.getHour());
        dayOfMonth(se.getDayOfMonth());
        month(se.getMonth());
        dayOfWeek(se.getDayOfWeek());
        year(se.getYear());
        timezone(se.getTimezone());

        // Create local copies
        start(se.getStart());
        end(se.getEnd());

        configure();
    }

    /** Construct EJBTimerSchedule instance from a given Schedule annotation.
      */
    public EJBTimerSchedule(ScheduledTimerDescriptor sd, String methodName, int paramCount) {
        second(sd.getSecond());
        minute(sd.getMinute());
        hour(sd.getHour());
        dayOfMonth(sd.getDayOfMonth());
        month(sd.getMonth());
        dayOfWeek(sd.getDayOfWeek());
        year(sd.getYear());
        timezone(sd.getTimezone());
        start(sd.getStart());
        end(sd.getEnd());

        methodName_ = methodName;
        paramCount_ = paramCount;

        automatic_ = true;

        configure();
    }

    /** Construct EJBTimerSchedule instance with all defaults.
     * The subclass will call back for additional parsing.
     */
    public EJBTimerSchedule(String s) {
        super(s);

        // Parse the rest of elements
        String[] sp = s.split(" # ");
        automatic_ = Boolean.parseBoolean(sp[10]);

        if (sp.length == 13) {
            methodName_ = sp[11];
            paramCount_ = Integer.parseInt(sp[12]);
        }
    }

    public EJBTimerSchedule setAutomatic(boolean b) {
        automatic_ = b;
        return this;
    }

    public boolean isAutomatic() {
        return automatic_;
    }

    public String getTimerMethodName() {
        return methodName_;
    }

    public int getMethodParamCount() {
        return paramCount_;
    }

    public String getScheduleAsString() {
        StringBuffer s = new StringBuffer(super.getScheduleAsString())
               .append(" # ").append(automatic_);

        if (automatic_) {
            s.append(" # ").append(methodName_).append(" # ").append(paramCount_);
        }

        return s.toString();
    }

    public ScheduleExpression getScheduleExpression() {
        return new ScheduleExpression().
                second(getSecond()).
                minute(getMinute()).
                hour(getHour()).
                dayOfMonth(getDayOfMonth()).
                month(getMonth()).
                dayOfWeek(getDayOfWeek()).
                year(getYear()).
                timezone(getTimeZoneID()).
                start(getStart()).
                end(getEnd());

    }

    public int hashCode() {
        return getScheduleAsString().hashCode();
    }

    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (o == null || !(o instanceof EJBTimerSchedule))
            return false;

        EJBTimerSchedule t = (EJBTimerSchedule)o;
        return getScheduleAsString().equals(t.getScheduleAsString());

    }

    /**
     * Returns true if this Schedule can calculate its next timeout
     * without errors.
     */
    public static boolean isValid(ScheduledTimerDescriptor s) {
        EJBTimerSchedule ts = new EJBTimerSchedule(s, null, 0);
        ts.getNextTimeout();

        return true;
    }

    @Override
    protected boolean isExpectedElementCount(String[] el) {
        return (el.length == 11 || el.length == 13);
    }

}
