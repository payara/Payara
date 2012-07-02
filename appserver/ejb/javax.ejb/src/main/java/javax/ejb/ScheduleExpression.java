/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006-2012 Oracle and/or its affiliates. All rights reserved.
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

package javax.ejb;

import java.util.Date;
import java.io.Serializable;

/**
 * <p> 
 * A calendar-based timeout expression for an enterprise bean
 * timer.</p>
 * 
 * <p>
 * Each attribute used to define a calendar-based timeout schedule 
 * has two overloaded  setter methods, one that takes a String and 
 * one that takes an int.  
 * The int version is merely a convenience method for setting the 
 * attribute in the common case that the value is a simple integer value. </p>
 * 
 * <p>For example, <pre>scheduleExpression.second(10)</pre> is semantically equivalent to 
 *      <pre>scheduleExpression.second("10")</pre></p>
 *
 *
 *
 * There are seven attributes that constitute a schedule specification which are
 * listed below.  In addition, the <code>timezone</code> attribute may be used
 * to specify a non-default time zone in whose context the schedule 
 * specification is to be evaluated.
 * <p>
 * The attributes that specify the calendar-based schedule itself are as
 * follows:
 * <p>
 * <ul>
 * <li> second : one or more seconds within a minute 
 * <p> Allowable values: [0,59]
 * <p>
 * <li> minute : one or more minutes within an hour
 * <p> Allowable values : [0,59]
 * <p>
 * <li> hour : one or more hours within a day
 * <p> Allowable values : [0,23]
 * <p>
 * <li> dayOfMonth : one or more days within a month
 * <p> Allowable values: 
 * <ul>
 * <li> [1,31] 
 * <li> [-7, -1] 
 * <li> "Last" 
 * <li> {"1st", "2nd", "3rd", "4th", "5th", "Last"} {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"}
 * </ul>
 * <p> "Last" means the last day of the month
 * <p> -x (where x is in the range [-7, -1]) means x day(s) before the last day of the month
 * <p> "1st","2nd", etc. applied to a day of the week identifies a single occurrence of that day within the month.
 * <p>
 * <li> month : one or more months within a year
 * <p> Allowable values :
 * <p> 
 * <ul>
 * <li> [1,12]
 * <li> {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", Dec"}
 * </ul>
 * <p>
 * <li> dayOfWeek : one or more days within a week
 * <p> Allowable values :
 * <p>
 * <ul>
 * <li> [0,7] 
 * <li> {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"}
 * </ul>
 * <p> "0" and "7" both refer to Sunday
 * <p>
 * <li> year : a particular calendar year
 * <p> Allowable values : a four-digit calendar year
 * 
 * <p>
 * </ul>
 * <p>
 *
 * Each attribute supports values expressed in one of the following forms
 * <p>
 * <ul>
 * <li> Single Value.  This constrains the attribute to only one of 
 * its possible values.
 * <pre> 
 * Example: second = "10"
 * Example: month = "Sep"</pre>
 * <p>
 * <li> Wild Card.  "*" represents all allowable values for a given attribute.
 * <pre>
 * Example: second = "*"
 * Example: dayOfWeek = "*"</pre>
 * 
 * <li> List.  This constrains the attribute to two or more allowable values
 * or ranges, with a comma used as a separator character within the string.
 * Each item in the list must be a single value or range.  List items cannot
 * be lists, wild cards, or increments.  Duplicate values are ignored.
 * <pre>
 * Example: second = "10,20,30"
 * Example: dayOfWeek = "Mon,Wed,Fri"
 * Example: minute = "0-10,30,40"</pre>
 *
 * <li> Range.  This constrains the attribute to an inclusive range of values,
 * with a dash separating both ends of the range.  Each side of the range 
 * must be a single attribute value.  Members of a range cannot be lists,
 * wild cards, ranges, or increments.  If <code>x</code> is larger than
 * <code>y</code> in a range <code>"x-y"</code>, the range is equivalent
 * to <code>"x-max, min-y"</code>, where <code>max</code> is the largest
 * value of the corresponding attribute and <code>min</code> is the smallest.
 * The range <code>"x-x"</code>, where both range values are the same,
 * evaluates to the single value <code>x</code>.  The day of the week range
 * <code>"0-7"</code> is equivalent to <code>"*"</code>.
 * <p>
 * <pre>
 * Example: second = "1-10"
 * Example: dayOfWeek = "Fri-Mon"
 * Example: dayOfMonth = "27-3" (Equivalent to "27-Last , 1-3")</pre>
 * 
 * <li> Increments.  The forward slash constrains an attribute based on a
 * starting point and an interval, and is used to specify every <code>N</code>
 * seconds, minutes, or hours within the minute, hour, or day, respectively.
 * For the expression <code>x/y</code>, the attribute is constrained to
 * every <code>y</code>th value within the set of allowable values beginning
 * at time <code>x</code>.  The <code>x</code> value is inclusive.  The
 * wild card character (<code>*</code>) can be used in the <code>x</code>
 * position, and is equivalent to <code>0</code>.  The use of increments
 * is only supported within the <code>second</code>, <code>minute</code>,
 * and <code>hour</code> attributes.  For the <code>second</code> and
 * <code>minute</code> attributes, <code>x</code> and <code>y</code> must
 * each be in the range <code>[0,59]</code>.  For the <code>hour</code>
 * attribute, <code>x</code> and <code>y</code> must each be in the range 
 * <code>[0,23]</code>.
 * <p>
 * <pre>
 * Example: minute = "&#8727;/5" (Every five minutes within the hour)</pre>
 * This is equivalent to: 
 * <code>minute = "0,5,10,15,20,25,30,35,40,45,50,55"</code>  
 * <p>
 * <pre>
 * Example: second = "30/10" (Every 10 seconds within the minute, starting at second 30) </pre>
 * This is equivalent to: <code>second = "30,40,50"</code>
 * <p> Note that the set of matching increment values stops once the maximum
 * value for that attribute is exceeded.  It does not "roll over" past the
 * boundary.
 * <p>
 * <pre>
 * Example : ( minute = "&#8727;/14", hour="1,2")</pre>
 * <p>  This is equivalent to: <code>(minute = "0,14,28,42,56", hour = "1,2")</code>  
 *  (Every 14 minutes within the hour, for the hours of 1 and 2 a.m.)
 * </ul>
 *
 * <p>
 * The following additional rules apply to the schedule specification attributes:
 * <ul>
 * <li> If the <code>dayOfMonth</code> attribute has a non-wildcard value and
 * the <code>dayOfWeek</code> attribute has a non-wildcard value, then any
 * day matching either the <code>dayOfMonth</code> value or the 
 * <code>dayOfWeek</code> value will be considered to apply.
 * <li> Whitespace is ignored, except for string constants and numeric values.
 * <li> All string constants (e.g., <code>"Sun"</code>, <code>"Jan"</code>,
 * <code>"1st"</code>, etc.) are case insensitive.
 * </ul>
 * <p>
 * Schedule-based timer times are evaluated in the context of the default 
 * time zone associated with the container in which the application is 
 * executing. A schedule-based timer may optionally override this default
 * and associate itself with a specific time zone. If the schedule-based 
 * timer is associated with a specific time zone, all its times are 
 * evaluated in the context of that time zone, regardless of the default 
 * time zone in which the container is executing.
 * <p>
 * None of the ScheduleExpression methods are required to be called.  
 * The defaults are :
 *
 * <ul>
 * <li>  second: "0"
 * <li>  minute: "0"
 * <li>  hour: "0"
 * <li>  dayOfMonth: "*"
 * <li>  month: "*"
 * <li>  dayOfWeek: "*"
 * <li>  year: "*"
 * <li>  timezone : default JVM time zone
 * <li>  start : upon timer creation
 * <li>  end : no end date</p>
 * </ul>
 *
 * <p>
 * Applications must not rely on the getter methods that return
 * the attributes of a calendar-based timeout schedule to return
 * them in the same syntactic format in which they were passed in to a
 * <code>ScheduleExpression</code> method or supplied to the
 * <code>Schedule</code> annotation, and portable implementations must
 * not assume any particular syntactic format.  Implementations are 
 * required only to preserve semantic equivalence.
 * 
 * @since EJB 3.1
 */

public class ScheduleExpression implements Serializable {

    private static final long serialVersionUID = -3813254457230997879L;

    /**
     * Create a schedule with the default values.
     */
    public ScheduleExpression() {}

    /**
     * Set the second attribute.
     * @param s the attribute value as a <code>String</code>
     */
    public ScheduleExpression second(String s) {
        second_ = s; 
        return this;
    }

    /**
     * Set the second attribute.
     * @param s the attribute value as an <code>int</code>, if the value 
     * is a simple integer value
     */
    public ScheduleExpression second(int s) {
        second_ = s + "";
        return this;
    }

    /**
     * Return the value of the second attribute.
     *
     * @return second 
     */
    public String getSecond() {
        return second_;
    }

    /**
     * Set the minute attribute.
     * @param m the attribute value as a <code>String</code>
     */
    public ScheduleExpression minute(String m) {
        minute_ = m;
        return this;
    }

    /**
     * Set the minute attribute.
     * @param m the attribute value as an <code>int</code>, if the value 
     * is a simple integer value
     */
    public ScheduleExpression minute(int m) {
        minute_ = m + "";
        return this;
    }

    /**
     * Return the value of the minute attribute.
     *
     * @return minute 
     */
    public String getMinute() {
	return minute_;
    }

    /**
     * Set the hour attribute.
     * @param h the attribute value as a <code>String</code>
     */
    public ScheduleExpression hour(String h) {
        hour_ = h;
        return this;
    }

    /**
     * Set the hour attribute.
     * @param h the attribute value as an <code>int</code>, if the value 
     * is a simple integer value
     */
    public ScheduleExpression hour(int h) {
        hour_ = h + "";
        return this;
    }

    /**
     * Return the value of the hour attribute.
     *
     * @return hour
     */
    public String getHour() {
        return hour_;
    }

    /**
     * Set the day of the month attribute.
     * @param d the attribute value as a <code>String</code>
     */
    public ScheduleExpression dayOfMonth(String d) {
        dayOfMonth_ = d;
        return this;
    }

    /**
     * Set the day of the month attribute.
     * @param d the attribute value as an <code>int</code>, if the value 
     * is a simple integer value
     */
    public ScheduleExpression dayOfMonth(int d) {
        dayOfMonth_ = d + "";
        return this;
    }

    /**
     * Return the value of the day of the month attribute.
     *
     * @return day of the month
     */
    public String getDayOfMonth() {
	return dayOfMonth_;
    }

    /**
     * Set the month attribute.
     * @param m the attribute value as a <code>String</code>
     */
    public ScheduleExpression month(String m) {
        month_ = m;
        return this;
    }

    /**
     * Set the month attribute.
     * @param m the attribute value as an <code>int</code>, if the value 
     * is a simple integer value
     */
    public ScheduleExpression month(int m) {
        month_ = m + "";
        return this;
    }

    /**
     * Return the value of the month attribute.
     *
     * @return month
     */
    public String getMonth() {
        return month_;
    }

    /**
     * Set the day of the week attribute.
     * @param d the attribute value as a <code>String</code>
     */
    public ScheduleExpression dayOfWeek(String d) {
        dayOfWeek_ = d;
        return this;
    }

    /**
     * Set the day of the week attribute.
     * @param d the attribute value as an <code>int</code>, if the value 
     * is a simple integer value
     */
    public ScheduleExpression dayOfWeek(int d) {
        dayOfWeek_ = d + "";
        return this;
    }

    /**
     * Return the value of the day of the week attribute.
     *
     * @return day of the week
     */
    public String getDayOfWeek() {
	return dayOfWeek_;
    }

    /**
     * Set the year attribute.
     * @param y the attribute value as a <code>String</code>
     */
    public ScheduleExpression year(String y) {
        year_ = y;
        return this;
    }

    /**
     * Set the year attribute.
     * @param y the attribute value as an <code>int</code>, if the value 
     * is a simple integer value
     */
    public ScheduleExpression year(int y) {
        year_ = y + "";
        return this;
    }

    /**
     * Return the value of the year attribute.
     *
     * @return year
     */
    public String getYear() {
        return year_;
    }

    /**
     * Set the timezone.
     * @param timezoneID the Time zone specified as an ID String
     */
    public ScheduleExpression timezone(String timezoneID) {
        timezoneID_ = timezoneID;
        return this;
    }

    /**
     * Return the timezone, if set; otherwise null.
     *
     * @return timezone
     */
    public String getTimezone() {
        return timezoneID_;
    }

    /**
     * Set the start date.
     * @param s the start date 
     */
    public ScheduleExpression start(Date s) {
        start_ = (s == null) ? null : new Date(s.getTime());

        return this;
    }

    /**
     * Return the start date, if set; otherwise null.
     *
     * @return start date
     */
     public Date getStart() {
        return (start_ == null) ? null : new Date(start_.getTime());
    }

    /**
     * Set the end date.
     * @param e the end date 
     */
    public ScheduleExpression end(Date e) {
        end_ = (e == null) ? null : new Date(e.getTime());

        return this;
    }

   /**
     * Return the end date, if set; otherwise null.
     *
     * @return end date
     */
    public Date getEnd() {
        return (end_ == null) ? null : new Date(end_.getTime());
    }

    public String toString() {
	return "ScheduleExpression [second=" + second_ 
                + ";minute=" + minute_ 
                + ";hour=" + hour_ 
                + ";dayOfMonth=" + dayOfMonth_
                + ";month=" + month_
                + ";dayOfWeek=" + dayOfWeek_
                + ";year=" + year_
                + ";timezoneID=" + timezoneID_
                + ";start=" + start_
                + ";end=" + end_ 
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

}
