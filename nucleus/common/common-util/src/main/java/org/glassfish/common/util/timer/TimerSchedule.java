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

package org.glassfish.common.util.timer;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.BitSet;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.regex.Pattern;
import java.io.Serializable;

/**
 * This class converts a cron-like string to its internal representation
 * and calculates the next timeout after the current timestamp.
 *
 * @author mvatkina
 */

public class TimerSchedule implements Serializable {

    private String second_ = "0";
    private String minute_ = "0";
    private String hour_ = "0";

    private String dayOfMonth_ = "*";
    private String month_ = "*";
    private String dayOfWeek_ = "*";
    private String year_ = "*";
    private String timezone_ = null;
    private TimeZone tz_ = null;

    private Date start_ = null;
    private Date end_ = null;

    private boolean configured = false;
    private boolean isValid = true;

    private boolean lastDayOfMonth = false;
    private int dayBeforeEndOfMonth = 0;

    private BitSet seconds = new BitSet(60);
    private BitSet minutes = new BitSet(60);
    private BitSet hours = new BitSet(24);
    private BitSet days = new BitSet(31);
    private BitSet daysOfWeek = new BitSet(7);
    private BitSet daysOfMonth = new BitSet(31);
    private BitSet months = new BitSet(12);

    private static Map<Object, Integer> conversionTable = new HashMap<Object, Integer>();

    private List<String> daysOfWeekOrRangesOfDaysInMonth = new ArrayList<String>();
    private List<Integer> years = new ArrayList<Integer>();

    private static final Pattern simpleRangePattern = Pattern.compile("[0-9]+\\s*-\\s*([0-9]+|last)");
    private static final Pattern positivePattern = Pattern.compile("[0-9]+");
    private static final Pattern negativePattern = Pattern.compile("-[1-7]");
    private static final Pattern orderedDayPattern = Pattern.compile("(1st|2nd|3rd|[45]th|last)\\s+[a-z][a-z][a-z]");
    private static final Pattern yearPattern = Pattern.compile("[1-9][0-9][0-9][0-9]");

    private static final char rangeChar     = '-';
    private static final char incrementChar = '/';
    private static final String TIMEZONE = "timezone";
    private static final String YEAR = "year";
    private static final String MONTH = "month";
    private static final String DAY_OF_MONTH = "dayOfMonth";
    private static final String DAY_OF_WEEK = "dayOfWeek";
    private static final String HOUR = "hour";
    private static final String MINUTE = "minute";
    private static final String SECOND = "second";

    private static final int MAX_YEAR_TRY = 100;

    static {
        conversionTable.put("jan", 1);
        conversionTable.put("feb", 2);
        conversionTable.put("mar", 3);
        conversionTable.put("apr", 4);
        conversionTable.put("may", 5);
        conversionTable.put("jun", 6);
        conversionTable.put("jul", 7);
        conversionTable.put("aug", 8);
        conversionTable.put("sep", 9);
        conversionTable.put("oct", 10);
        conversionTable.put("nov", 11);
        conversionTable.put("dec", 12);

        conversionTable.put("sun", 0);
        conversionTable.put("mon", 1);
        conversionTable.put("tue", 2);
        conversionTable.put("wed", 3);
        conversionTable.put("thu", 4);
        conversionTable.put("fri", 5);
        conversionTable.put("sat", 6);

        conversionTable.put(0, Calendar.SUNDAY);
        conversionTable.put(1, Calendar.MONDAY);
        conversionTable.put(2, Calendar.TUESDAY);
        conversionTable.put(3, Calendar.WEDNESDAY);
        conversionTable.put(4, Calendar.THURSDAY);
        conversionTable.put(5, Calendar.FRIDAY);
        conversionTable.put(6, Calendar.SATURDAY);
        conversionTable.put(7, Calendar.SUNDAY);
    }

    /** Construct TimerSchedule instance with all defaults.
      */
    public TimerSchedule() {}

    /** Construct TimerSchedule instance from a given String.
     */
    public TimerSchedule(String s) {
        String[] sp = s.split(" # ");

        if (!isExpectedElementCount(sp)) {
            throw new IllegalStateException("Cannot construct " + getClass().getName() + " from " + s);
        }

        second_ = sp[0];
        minute_ = sp[1];
        hour_ = sp[2];
        dayOfMonth_ = sp[3];
        month_ = sp[4];
        dayOfWeek_ = sp[5];
        year_ = sp[6];
        timezone_ = (sp[7].equals("null")? null : sp[7]);
        start_ = (sp[8].equals("null")? null : new Date(Long.parseLong(sp[8])));
        end_ = (sp[9].equals("null")? null : new Date(Long.parseLong(sp[9])));

        configure();
    }

    public TimerSchedule second(String s) {
        assertNotEmpty(s, SECOND);
        second_ = s.trim(); 
        return this;
    }

    public String getSecond() {
        return second_;
    }

    public TimerSchedule minute(String m) {
        assertNotEmpty(m, MINUTE);
        minute_ = m.trim();
        return this;
    }

    public String getMinute() {
	return minute_;
    }

    public TimerSchedule hour(String h) {
        assertNotEmpty(h, HOUR);
        hour_ = h.trim();
        return this;
    }

    public String getHour() {
        return hour_;
    }

    public TimerSchedule dayOfMonth(String d) {
        assertNotEmpty(d, DAY_OF_MONTH);
        dayOfMonth_ = d.trim();
        return this;
    }

    public String getDayOfMonth() {
	return dayOfMonth_;
    }

    public TimerSchedule month(String m) {
        assertNotEmpty(m, MONTH);
        month_ = m.trim();
        return this;
    }

    public String getMonth() {
        return month_;
    }

    public TimerSchedule dayOfWeek(String d) {
        assertNotEmpty(d, DAY_OF_WEEK);
        dayOfWeek_ = d.trim();
        return this;
    }

    public String getDayOfWeek() {
	return dayOfWeek_;
    }

    public TimerSchedule year(String y) {
        assertNotEmpty(y, YEAR);
        year_ = y.trim();
        return this;
    }

    public String getYear() {
        return year_;
    }

    public TimerSchedule timezone(String tz) {
        timezone_ = (tz != null && tz.length() > 0)? tz.trim() : null;
        return this;
    }

    public String getTimeZoneID() {
        return timezone_;
    }

    public TimeZone getTimeZone() {
        return tz_;
    }

    public TimerSchedule start(Date s) {
        // Create a copy of the user's value
        start_ = (s == null) ? null : new Date(s.getTime());

        return this;
    }

    public Date getStart() {
        // Return a copy of the internal value
        return (start_ == null) ? null : new Date(start_.getTime());
    }

    public TimerSchedule end(Date e) {
        // Create a copy of the user's value
        end_ = (e == null) ? null : new Date(e.getTime());
        return this;
    }

    public Date getEnd() {
        // Return a copy of the internal value
        return (end_ == null) ? null : new Date(end_.getTime());
    }

    public String getScheduleAsString() {
        StringBuffer s = new StringBuffer()
               .append(second_).append(" # ")
               .append(minute_).append(" # ") 
               .append( hour_).append(" # ") 
               .append( dayOfMonth_).append(" # ") 
               .append( month_).append(" # ") 
               .append( dayOfWeek_).append(" # ") 
               .append( year_).append(" # ") 
               .append( timezone_).append(" # ") 
               .append(((start_ == null) ? null : start_.getTime()))
               .append(" # ") 
               .append(((end_ == null) ? null : end_.getTime()));

        return s.toString();
    }

    public int hashCode() {
        return getScheduleAsString().hashCode();
    }

    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (o == null || !(o instanceof TimerSchedule))
            return false;

        TimerSchedule t = (TimerSchedule)o;
        return getScheduleAsString().equals(t.getScheduleAsString());

    }

    /**
     * Returns true if this date can be used for the next timeout of
     * the schedule represented by this instance.
     */
    public boolean isValid(Calendar date) {
        if ((end_ != null && date.getTimeInMillis() > end_.getTime())) {
            return false;
        }

        if (years.size() == 0) {
            return isValid;
        }

        Calendar now = new GregorianCalendar(Locale.ENGLISH);
        if (tz_ != null) {
            now.setTimeZone(tz_);
        }

        int currYear = now.get(Calendar.YEAR);

        for(int year : years) {
            if (year < currYear) {
                continue;
            }

            if (date.get(Calendar.YEAR) == year) {
                return true;
            }
        }

        return false;
    }

    /**
     * Returns the Date of the next possible timeout.
     */
    public Calendar getNextTimeout() {

        return getNextTimeout(new GregorianCalendar(Locale.ENGLISH));
    }

   /**
     * Returns the Date of the next possible timeout after the specific date.
     */
    public Calendar getNextTimeout(Date date) {
        Calendar next = new GregorianCalendar(Locale.ENGLISH);
        next.setTime(date);

        return getNextTimeout(next);
    }

   /**
     * Returns the Date of the next possible timeout after the specific Calendar date
     */
    private Calendar getNextTimeout(Calendar next) {
        if (!configured) {
            configure();
        }

        if (tz_ != null) {
            next.setTimeZone(tz_);
        }

        if (start_ != null && next.getTimeInMillis() < start_.getTime()) {
            next.setTime(start_);
        } else {
            next.add(Calendar.SECOND, 1);
            next.set(Calendar.MILLISECOND, 0);
        }

        //System.out.println("... starting with ... " + next.getTime() );

        if (years.size() == 0) {
            return getNextTimeout(next, 0);
        }

        int currYear = next.get(Calendar.YEAR);
        for(int year : years) {
            //System.out.println("... testing year ... " + year);
            if (year < currYear) {
                continue;
            }

            if (next.get(Calendar.YEAR) == year) {
                next = getNextTimeout(next, year);

            } else if (next.get(Calendar.YEAR) < year) {
                // set to the beginning of the year
                next.set(year, 0, 1, 0, 0, 0);
                System.out.println("==> Year reset " + next.getTime()); 
                next = getNextTimeout(next, year);
            }

            if (next.get(Calendar.YEAR) == year) {
                break;
            }
        }

        //System.out.println("... returning ... " + next.getTime() + " <> " + next.getTimeInMillis());
        return next;
    }


    /**
     * Returns the Date of the next possible timeout for a specific
     * year value and starting date. If year is 0, any year will be correct.
     */
    private Calendar getNextTimeout(Calendar next, int year) {
        int i = 0;
        while (end_ == null || !next.getTime().after(end_)) {

            if (year != 0 && next.get(Calendar.YEAR) > year) {
                break;
            }

            if(skipToNextValue(next, months, Calendar.MONTH, Calendar.YEAR)) {
                if (++i > MAX_YEAR_TRY) {
                    // Can't find the date - it's most probably invalid
                    isValid = false;
                    break;
                }

                next.set(Calendar.DAY_OF_MONTH, 1);
                next.set(Calendar.HOUR_OF_DAY, 0);
                next.set(Calendar.MINUTE, 0);
                next.set(Calendar.SECOND, 0);

                continue;
            }

            if (dayOfWeek_.equals("*")) {
                // Either dayOfMonth_ is specified, and we'll use it or 
                // neither is specified and any one can be used.
                //System.out.println("==> Processing DAY_OF_MONTH ...");
                if(skipToNextValue(next, daysOfMonth, Calendar.DAY_OF_MONTH, Calendar.MONTH)) {
                    next.set(Calendar.HOUR_OF_DAY, 0);
                    next.set(Calendar.MINUTE, 0);
                    next.set(Calendar.SECOND, 0);
                    continue;
                }
            } else if (dayOfMonth_.equals("*")) {
                // dayOfWeek_ is specified and dayOfMonth_ is not
                //System.out.println("==> Processing DAY_OF_WEEK ...");
                if(skipToNextValue(next, daysOfWeek, Calendar.DAY_OF_WEEK, Calendar.WEEK_OF_MONTH)) {
                    next.set(Calendar.HOUR_OF_DAY, 0);
                    next.set(Calendar.MINUTE, 0);
                    next.set(Calendar.SECOND, 0);
                    continue;
                }

            } else {
                // Both are specified - pick the closest date:
                Calendar date1 = (Calendar)next.clone();
                Calendar date2 = (Calendar)next.clone();
                boolean changed = false;

                //System.out.println("==> Processing 1 DAY_OF_MONTH ...");
                if(skipToNextValue(date1, daysOfMonth, Calendar.DAY_OF_MONTH, Calendar.MONTH)) {
                    date1.set(Calendar.HOUR_OF_DAY, 0);
                    date1.set(Calendar.MINUTE, 0);
                    date1.set(Calendar.SECOND, 0);
                }

                //System.out.println("==> Processing 2 DAY_OF_WEEK ...");
                if(skipToNextValue(date2, daysOfWeek, Calendar.DAY_OF_WEEK, Calendar.WEEK_OF_MONTH)) {
                    date2.set(Calendar.HOUR_OF_DAY, 0);
                    date2.set(Calendar.MINUTE, 0);
                    date2.set(Calendar.SECOND, 0);

                }

                Calendar date0 = (date1.before(date2))? date1 : date2;
                if (!next.equals(date0)) {
                    next = date0;
                    continue;
                }
            }

            if(skipToNextValue(next, hours, Calendar.HOUR_OF_DAY, Calendar.DAY_OF_MONTH)) {
                next.set(Calendar.MINUTE, 0);
                next.set(Calendar.SECOND, 0);
                continue;
            }

            if(skipToNextValue(next, minutes, Calendar.MINUTE, Calendar.HOUR_OF_DAY)) {
                next.set(Calendar.SECOND, 0);
                continue;
            }

            if(skipToNextValue(next, seconds, Calendar.SECOND, Calendar.MINUTE)) {
                continue;
            }

            break;
        }

        return next;
    }

    /**
     * Validate number of elements in the input string
     */
    protected boolean isExpectedElementCount(String[] el) {
        return el.length == 10;
    }

    /**
     * Populate all internale structures to be used for the next timeout
     * calculations
     */
    protected void configure() {
        // XXX Can it ever be called by different threads?

        parseNumbersOrNames(second_, seconds, 0, 60, true, SECOND);
        parseNumbersOrNames(minute_, minutes, 0, 60, true, MINUTE);
        parseNumbersOrNames(hour_, hours, 0, 24, true, HOUR);
        parseNumbersOrNames(dayOfWeek_, daysOfWeek, 0, 7, false, DAY_OF_WEEK);
        parseNumbersOrNames(month_, months, 1, 12, false, MONTH);
        parseDaysOfMonth();
        parseYears();
        if (timezone_ != null) {
            tz_ = TimeZone.getTimeZone(timezone_);
        }

        configured = true;
    }

    /**
     * Populate the BitSet where true bits represent set values.
     * Input data can be either a number or a case insensitive abbreviated name.
     */
    private void parseNumbersOrNames(String s, BitSet bits, 
            int start, int size, boolean incrementAllowed, String field) {
        // All
        if (s.equals("*")) {
            if (!isDayOfWeek(field)) {
                bits.set(0, size);
            } else {
                for (int i = start; i <= size; i++) {
                    bits.set(conversionTable.get(i));
                }
            }
            return;
        }

        // List
        if (s.indexOf(',') > 0) {
            String[] arr = splitList(s);
            for (String s0 : arr) {
                if (s0.indexOf(rangeChar, 1) > 0) {
                    processRange(s0, bits, start, size, field);
                } else {
                    bits.set(getNumericValue(s0, start, size, field));
                }
            }
            return;
        }

        // Range
        if (s.indexOf(rangeChar) > 0) {
            processRange(s, bits, start, size, field);
            return;
        }

        // Increments
        if (incrementAllowed && s.indexOf(incrementChar) > 0) {
            String[] arr = splitBy(s, incrementChar);
            int begin = 0;
            if (!arr[0].equals("*")) {
                begin = getNumericValue(arr[0], start, size, field);
            }

            int incr = getNumericValue(arr[1], start, size, field);
            for (int i = begin; i < size; ) {
                bits.set(i);
                i = i + incr;
            }
            return;
        }

        // Single value
        bits.set(getNumericValue(s, start, size, field));
        
    }

    /**
     * Process a range of values for that represents values other than days of the month.
     */
    private void processRange(String s, BitSet bits, int start, int size, String field) {

        //System.out.println("==> IN RANGE: " + s);
        String[] arr = splitBy(s, rangeChar);
        int begin = getNumericValue(arr[0], start, size, field);
        int end = getNumericValue(arr[1], start, size, field);
        if (begin < 0) {
            throw new IllegalArgumentException("Negative range start for " + field + " : " + s);
        }

        boolean b = isDayOfWeek(field);
        // Special case for dayOfWeek="0-7" translates to "*"
        if (b && (arr[0].equals(Integer.toString(start)) && arr[1].equals(Integer.toString(size)))) {
            //System.out.println("== 0-7 RANGE from: " + arr[0] + " to " + arr[1]);
            for (int i = start; i <= size; i++) {
                bits.set(conversionTable.get(i));
            }
        } else {
            //System.out.println("== RANGE from: " + begin + " to " + end);
            if (b) {
                // DaysOfWeek start with 1 in java.util.Calendar, and there are 7 of them
                setBitsRange(bits, begin, end, 1, 8);
            } else {
                setBitsRange(bits, begin, end, 0, size);
            }
        }
    }

    /**
     * Preprocess data that represents days of the month.
     * Input data can be one or more of a positive or a negative number, an order,
     * or a case insensitive abbreviated name.
     */
    private void parseDaysOfMonth() {
        // All
        if (dayOfMonth_.equals("*")) {
            daysOfMonth.set(1, 32);
            return;
        }

        // List
        if (dayOfMonth_.indexOf(',') > 0) {
            String[] arr = splitList(dayOfMonth_);
            for (String s0 : arr) {
                if (s0.indexOf(rangeChar, 1) > 0) {
                    processRangeDaysOfMonth(s0);
                } else {
                    processDayOfMonth(s0);
                }
            }
            return;
        }

        // Range
        if (dayOfMonth_.indexOf(rangeChar, 1) > 0) {
            processRangeDaysOfMonth(dayOfMonth_);

            return;
        }

        // Single value
        processDayOfMonth(dayOfMonth_);
    }

    /**
     * Process a range of values for that represents days of the month.
     */
    private void processRangeDaysOfMonth(String s) {
        if (simpleRangePattern.matcher(s).matches()) {
            // If these are positive numbers or a range from a positive
            // number to the last day of the month - process them now
            String[] arr = splitBy(s, rangeChar);
            int begin = parseInt(arr[0], DAY_OF_MONTH);
            int end = 31;
            if (positivePattern.matcher(arr[1]).matches()) {
                end = parseInt(arr[1], DAY_OF_MONTH);
            }

            if (begin < 1 || end > 31) {
                throw new IllegalArgumentException("Invalid dayOfMonth range: " + s);
            }

            setBitsRange(daysOfMonth, begin, end, 1, 32);

         } else {
             // Otherwise just remember - we'll process it later
             daysOfWeekOrRangesOfDaysInMonth.add(s.toLowerCase(Locale.ENGLISH));
         } 
     }

    /**
     * Preprocess data that represents years.
     * Input data can be one or more positive number, or a range of them.
     */
    private void parseYears() {
        // All
        if (year_.equals("*")) {
            return;
        }

        // List
        if (year_.indexOf(',') > 0) {
            String[] arr = splitList(year_);
            for (int i = 0; i < arr.length; i++) {
                if (arr[i].indexOf(rangeChar, 1) > 0) {
                    processRangeAsList(years, arr[i], YEAR, yearPattern);
                } else {
                    years.add(assertValidYear(parseInt(arr[i], YEAR)));
                }
            }

            Collections.sort(years);
            return;
        }

        // Range
        if (year_.indexOf(rangeChar, 1) > 0) {
            processRangeAsList(years, year_, YEAR, yearPattern);
            return;
        }

        years.add(assertValidYear(parseInt(year_, YEAR)));
    }

    /**
     * Adds a List of values that correspond to the specified range 
     */
    private void processRangeAsList(List list, String s, String field, Pattern pattern) {
        String[] arr = splitBy(s, rangeChar);
        int begin = parseInt(arr[0], field);
        int end = parseInt(arr[1], field);
        if (begin > end || !pattern.matcher(arr[0]).matches() || 
                !pattern.matcher(arr[1]).matches()) {
            throw new IllegalArgumentException("Invalid " + field + " range: " + s);
        }

        for (int i = begin; i <= end; i++) {
            list.add(i);
        }

    }

    private boolean skipToNextValue(Calendar date, BitSet bits, int field, int highfiled) {
        boolean changed = false;

        int currvalue = date.get(field);
        if (field == Calendar.DAY_OF_MONTH) {
             bits = populateCurrentMonthBits(date);
        }
        if (!bits.get(currvalue)) {
            int nextvalue = bits.nextSetBit(currvalue);
            if (nextvalue == -1 || nextvalue > date.getActualMaximum(field)) {
                //System.out.println("==> Incrementing ...");
                date.add(highfiled, 1);
                if (field == Calendar.DAY_OF_MONTH) {
                    // Recalculate bits for the next month
                    bits = populateCurrentMonthBits(date);
                }
                nextvalue = bits.nextSetBit(0);
            }

            if (nextvalue == -1) 
                throw new IllegalArgumentException("Should not happen - no value found");

            //System.out.println(".... seting " + field + " ... to ... " + nextvalue);
            date.set(field, nextvalue);
            changed = true;
        }

        return changed;
    }

    /**
     * Split a String that represents a list of values.
     */
    private String[] splitList(String s) {
        return s.split("\\s*,\\s*");
    }

    /**
     * Split a String that represents a range of values.
     */
    private String[] splitBy(String s, char ch) {
        int i = s.indexOf(ch, 1);
        return new String[] {s.substring(0,i).trim(), s.substring(i+1).trim()};
    }

    /**
     * Convert a String to a number. If the String represents a 
     * number, return its int value. If the String represents a 
     * (case insensitive) name of the day of the week or a month, 
     * return the corresponding numeric value from the conversionTable. 
     * If field represents DAY_OF_WEEK, return the value from the conversionTable
     * that represents Calendar's value of the result.
     */
    private int getNumericValue(String s, int start, int size, String field) {

        int i;
        if (positivePattern.matcher(s).matches()) {
            i = parseInt(s, field);
            if (!isDayOfWeek(field) && (i < start || i > (start + size -1))) {
                throw new IllegalArgumentException("Invalid " + field + " value: " + s);
            }
        } else {
            Integer val = conversionTable.get(s.toLowerCase(Locale.ENGLISH));
            assertValid(val, s, field);
            i = val.intValue();
        }

        int result = i - start;
        if (isDayOfWeek(field)) {
            Integer val = conversionTable.get(i);
            assertValid(val, s, field);
            result = val.intValue();
        }
        return result;
    }

    /**
     * Process a single value that represents a day of the month.
     * Input data can be a positive or a negative number, an order,
     * or a case insensitive abbreviated name.
     */
    private void processDayOfMonth(String s) {
        String s0 = s.toLowerCase(Locale.ENGLISH);

        if (positivePattern.matcher(s0).matches()) {
            int i = parseInt(s0, DAY_OF_MONTH);
            if (i < 1 || i > 31) {
                throw new IllegalArgumentException("Invalid dayOfMonth value: " + s0);
            }

            daysOfMonth.set(i);
        } else if (negativePattern.matcher(s0).matches()) {
            dayBeforeEndOfMonth = parseInt(s0.substring(1), DAY_OF_MONTH);
        } else if (s0.equals("last")) {
            lastDayOfMonth = true;
        } else {
            // Just remember - we'll process it later
            daysOfWeekOrRangesOfDaysInMonth.add(s0);
        }
    }

    /**
     * Use preprocessed values to create a BitSet that represents set 
     * days of this month.
     */
    private BitSet populateCurrentMonthBits(Calendar date) {
        if(dayOfMonth_.equals("*")) {
            return daysOfMonth;
        }

        BitSet bits = (BitSet)daysOfMonth.clone();
        if (lastDayOfMonth) {
            bits.set(date.getActualMaximum(Calendar.DAY_OF_MONTH));
        }

        if (dayBeforeEndOfMonth > 0) {
            bits.set(date.getActualMaximum(Calendar.DAY_OF_MONTH) - dayBeforeEndOfMonth);
        }

        int size = daysOfWeekOrRangesOfDaysInMonth.size();
        for (int i = 0; i < size; i++) {
            setDaysOfWeek(bits, date, daysOfWeekOrRangesOfDaysInMonth.get(i));
        }

        return bits;
    }

    /**
     * Return day of the month that represents the specific occurance of 
     * this day of the week, like "2nd Mon" or "Last Wed" or part of a range
     * which in turn can be any valid option for dayOfMonth.
     */
    private int getDayForDayOfMonth(Calendar date, String s) {
        if (positivePattern.matcher(s).matches()) {
             return parseInt(s, DAY_OF_MONTH);
        }

        Calendar testdate = (Calendar)date.clone();
        int lastday = testdate.getActualMaximum(Calendar.DAY_OF_MONTH);

        if (s.equals("last")) {
            return lastday;

        } else if (negativePattern.matcher(s).matches()) {
            return lastday - parseInt(s.substring(1), DAY_OF_MONTH);

        } else if (orderedDayPattern.matcher(s).matches()) {
            String arr[] = splitBy(s, ' ');
            int num = -1;
            if (!arr[0].equals("last")) {
                num = parseInt(arr[0].substring(0, 1), DAY_OF_MONTH);
            }

            // Convert name of the day to a number, then number to the
            // Calendar's value for that day.
            Integer weekday = conversionTable.get(arr[1]);
            assertValid(weekday, arr[1], DAY_OF_MONTH);
             
            int day = conversionTable.get(weekday);
            return getDayForDayOfWeek(testdate, lastday, day, num);
        }

        throw new IllegalArgumentException("Invalid dayOfMonth value: " + s);
    }

    /**
     * Return day of the month that represents the specific occurance of
     * this day of the week, like "2nd Mon" or "Last Wed".
     */
    private int getDayForDayOfWeek(Calendar testdate, int lastday, int day, int num) {

        if (num == -1) {
            return getLastDayForDayOfWeek(testdate, day, lastday);
        }

        int result = 1;
        for (int i = (num - 1) * 7 + 1; i <= lastday; i++ ) {
            testdate.set(Calendar.DAY_OF_MONTH, i);
            int testday = testdate.get(Calendar.DAY_OF_WEEK);
            if (testday == day) {
                result = i;
                break;
            }
        }

        return result;
    }

    /**
     * Return day of the month that represents the last occurance of this day of the week
     */
    private int getLastDayForDayOfWeek(Calendar testdate, int day, int lastday) {

        int result = lastday;
        for (int i = lastday; i >= 1; i--) {
            testdate.set(Calendar.DAY_OF_MONTH, i);
            int testday = testdate.get(Calendar.DAY_OF_WEEK);
            if (testday == day) {
                result = i;
                break;
            }
        }

        return result;
    }

    private void setDaysOfWeek(BitSet bits, Calendar date, String s) {
        // Check if it's a range
        if (s.indexOf(rangeChar, 1) > 0) {
            String[] arr = splitBy(s, rangeChar);

            int begin = getDayForDayOfMonth(date, arr[0]);
            int end = getDayForDayOfMonth(date, arr[1]);
            setBitsRange(bits, begin, end, 1, date.getActualMaximum(Calendar.DAY_OF_MONTH) + 1);

        } else {
            //System.out.println("++++++++ getDayForDayOfMonth(" + date.getTime() + " - " + s + " ) "  + getDayForDayOfMonth(date, s));
            bits.set(getDayForDayOfMonth(date, s));
        } 
    }

    /**
     * Set bits on for all values between begin and end (inclusive).
     * This method doesn't use conversion table because all values had been
     * already processed before reaching this point.
     */
    private void setBitsRange(BitSet bits, int begin, int end, int start, int size) {
        if (begin <= end) {
            //System.out.println("== SETTING from: " + begin + " up to " + (end + 1));
            // "Dec-Dec" is "Dec", not "*"
            bits.set(begin, end + 1);
        } else {
            //System.out.println("== SETTING from: " + begin + " up to " + size);
            //System.out.println("== AND from: " + start + " up to " + (end + 1));
            bits.set(begin, size);
            bits.set(start, end + 1);
        }
    }

    /** 
     * Convert a String to an int. Throws IllegalArgumentException instead of
     * the NumberFormatException.
     */
    private int parseInt(String s, String field) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid " + field + " value: " + s);
        }
    }

    /**
     * A valid year value is 4 digits after 1969
     */
    private int assertValidYear(int y) {
        assertValid(y, 1970, 9999, YEAR);
        return y;
    }

    /** Checks that a value is between the valid range
     */
    private void assertValid(int v, int min, int max, String field) {
        if (v < min || v > max) {
            throw new IllegalArgumentException("Invalid " + field + " value: " + v);
        }
    }

    /** Checks that a value is not null 
     */
    private void assertNotNull(Object s, String field) {
        if (s == null) {
            throw new IllegalArgumentException("Field " + field + " cannot be null");
        }
    }

    /** Checks that a value is not null and not an empty String.
     */
    private void assertNotEmpty(String s, String field) {
        assertNotNull(s, field);
        if (s.length() == 0) {
            throw new IllegalArgumentException("Field " + field + " cannot be an empty String");
        }
    }

    /** Checks that a conversion of a String to the internal representation was successful.
     */
    private void assertValid(Integer v, String s, String field) {
        if (v == null) {
            throw new IllegalArgumentException("Invalid " + field + " value: " + s);
        }
    }

   /** Returns true if field represents DAY_OF_WEEK.
    * Will be used to get the value from the conversionTable
    * that represents Calendar's value of the result instead of the face value.
    */
    private boolean isDayOfWeek(String field) {
        return field.equals(DAY_OF_WEEK);
    }
}
