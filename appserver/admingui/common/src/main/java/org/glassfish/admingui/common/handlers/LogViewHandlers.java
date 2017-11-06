/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2016 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.admingui.common.handlers;

import com.sun.jsftemplating.annotation.Handler;
import com.sun.jsftemplating.annotation.HandlerInput;
import com.sun.jsftemplating.annotation.HandlerOutput;
import com.sun.jsftemplating.layout.descriptors.handler.HandlerContext;
import com.sun.jsftemplating.util.Util;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import javax.ws.rs.core.MultivaluedHashMap;

import org.glassfish.admingui.common.util.GuiUtil;

public class LogViewHandlers {

    /**
     * Creates a new instance of LogViewHandlers
     */
    public LogViewHandlers() {
    }

    /**
     * <p> This handler creates a Map&lt;String, String&gt; which contains the
     * QUERY_STRING parameters that should be passed to the REST logging
     * endpoint to make a query with the given constraints.</p>
     *
     * @param	context	The HandlerContext.
     */
    @Handler(id = "gf.getLogQueryAttributes",
            input = {
        @HandlerInput(name = "InstanceName", type = String.class, required = true),
        @HandlerInput(name = "LogFileName", type = String.class, required = true),
        @HandlerInput(name = "LogLevel", type = String.class, required = true),
        @HandlerInput(name = "FromRecord", type = Integer.class),
        @HandlerInput(name = "AfterRecord", type = Boolean.class),
        @HandlerInput(name = "DateEnabled", type = String.class),
        @HandlerInput(name = "FromDate", type = Object.class),
        @HandlerInput(name = "FromTime", type = Object.class),
        @HandlerInput(name = "ToDate", type = Object.class),
        @HandlerInput(name = "ToTime", type = Object.class),
        @HandlerInput(name = "Loggers", type = Object.class),
        @HandlerInput(name = "CustomLoggers", type = Object.class),
        @HandlerInput(name = "anySearch", type = String.class),
        @HandlerInput(name = "NumToDisplay", type = Integer.class),
        @HandlerInput(name = "OnlyLevel", type = Boolean.class, defaultValue = "false"),
        @HandlerInput(name = "LogDateSortDirection", type = Boolean.class)},
            output = {
        @HandlerOutput(name = "attributes", type = Map.class)})
    public static void getLogQueryAttributes(HandlerContext handlerCtx) {
        // Create a Map to hold the attributes
        Map<String, Object> attMap = new HashMap<String, Object>();

        // Attempt to read values passed in
        String logFileName = (String) handlerCtx.getInputValue("LogFileName");
        Integer fromRecord = (Integer) handlerCtx.getInputValue("FromRecord");
        Boolean after = (Boolean) handlerCtx.getInputValue("AfterRecord");
        String dateEnabledString = (String) handlerCtx.getInputValue("DateEnabled");
        Object fromDate = handlerCtx.getInputValue("FromDate");
        Object fromTime = handlerCtx.getInputValue("FromTime");
        Object toDate = handlerCtx.getInputValue("ToDate");
        Object toTime = handlerCtx.getInputValue("ToTime");
        Object loggers = handlerCtx.getInputValue("Loggers");
        String logLevel = (String) handlerCtx.getInputValue("LogLevel");
        Object customLoggers = handlerCtx.getInputValue("CustomLoggers");
        String anySearch = (String) handlerCtx.getInputValue("anySearch");
        Integer numberToDisplay = (Integer) handlerCtx.getInputValue("NumToDisplay");
        Boolean onlyLevel = (Boolean) handlerCtx.getInputValue("OnlyLevel");
        Boolean direction = (Boolean) handlerCtx.getInputValue("LogDateSortDirection");
        String instanceName = (String) handlerCtx.getInputValue("InstanceName");

        notNullStringPut(attMap, "instanceName", instanceName);
        
        if ((instanceName != null)) {
            if (logFileName != null) {
            Date from;
            Date to;

                // Convert Date/Time fields
                if ((dateEnabledString != null)
                        && ("enabled".equalsIgnoreCase(dateEnabledString)
                        || "true".equalsIgnoreCase(dateEnabledString))) {
                    // Date is enabled, figure out what the values are
                    from = convertDateTime(handlerCtx, fromDate, fromTime);
                    to = convertDateTime(handlerCtx, toDate, toTime);
                    if ((from == null)) {
                        GuiUtil.handleError(handlerCtx, "Specific Date Range was chosen, however, date fields are incomplete.");
                    }
                    if (to != null && from != null) {
                        if (from.after(to)) {
                            GuiUtil.handleError(handlerCtx, "Timestamp value of 'From: ' field " + fromDate
                                    + " must not be greater than 'To: ' field value " + toDate);
                        }
                    }
                } else {
                    // Date not enabled, ignore from/to dates
                    from = null;
                    to = null;
                }

                if ((logLevel != null) && (logLevel.trim().length() == 0)) {
                    logLevel = null;
                }

                // Convert module array to List
                //List moduleList = null;
                //Set moduleList = new HashSet();
                String listOfModules = "";
                String sep = "";

                if (loggers != null) {
                    int len = ((Object[]) loggers).length;
                    if (len > 0) {
                        Object val;
                        StringBuilder sb = new StringBuilder();
                        for (int count = 0; count < len; count++) {
                            val = (((Object[]) loggers)[count]);
                            if ((val == null) || (val.toString().trim().length() == 0)) {
                                continue;
                            }
                            sb.append(sep).append(val);
                            sep = ",";
                        }
                        listOfModules = sb.toString();
                    }
                }

                // Add custom loggers
                if ((customLoggers != null) && (customLoggers.toString().trim().length() != 0)) {
                    String customLoggerList = customLoggers.toString().trim();
                    
                    for (String delim : CUSTOM_LOGGER_DELIMITERS) {
                        customLoggerList = customLoggerList.replace(delim, ",");
                    }
                    listOfModules += sep + customLoggerList;
                }
                
                if (!listOfModules.isEmpty()) {
                    attMap.put("listOfModules", listOfModules);
                }

                // Get the number to Display
                if (numberToDisplay == null) {
                    numberToDisplay = DEFAULT_NUMBER_TO_DISPLAY;
                }

                // Get the direction
                if (direction == null) {
                    direction = Boolean.FALSE;
                }

                // Get AfterRecord flag
                if (after == null) {
                    // Not supplied, use direction
                    after = direction;
                }


                notNullStringPut(attMap, "logFileName", logFileName);
                notNullStringPut(attMap, "startIndex", fromRecord);
                notNullStringPut(attMap, "searchForward", after);//direction
                notNullStringPut(attMap, "maximumNumberOfResults", numberToDisplay);
                notNullStringPut(attMap, "onlyLevel", onlyLevel);

                if (from != null) {
                    notNullStringPut(attMap, "fromTime", Long.valueOf(from.getTime()));
                }
                if (to != null) {
                    notNullStringPut(attMap, "toTime", Long.valueOf(to.getTime()));
                }
                notNullStringPut(attMap, "anySearch", anySearch);
                notNullStringPut(attMap, "logLevel", logLevel);
                notNullStringPut(attMap, "logFileRefresh", "true");
//                if (moduleList != null) {
//                    attMap.addAll("listOfModules", moduleList);
//                }
                //notNullStringPut(attMap, "logFileRefresh", logFileName);
            }
        }
        handlerCtx.setOutputValue("attributes", attMap);
    }

    /**
     * <p> This handler creates a Map&lt;String, String&gt; which contains the
     * QUERY_STRING parameters that should be passed to the REST logging
     * endpoint to make a query with the given constraints.</p>
     *
     * @param	context	The HandlerContext.
     */
    @Handler(id = "gf.processLogRecords",
            input = {
        @HandlerInput(name = "logRecords", type = List.class, required = true),
        @HandlerInput(name = "truncate", type = Boolean.class, defaultValue = "true"),
        @HandlerInput(name = "truncateLength", type = Integer.class, defaultValue = "100")},
            output = {
        @HandlerOutput(name = "result", type = List.class),
        @HandlerOutput(name = "firstRecord", type = Integer.class),
        @HandlerOutput(name = "lastRecord", type = Integer.class)})
    public static void processLogRecords(HandlerContext handlerCtx) {
        // Get the input...
        List<Map<String, Object>> records = (List<Map<String, Object>>) handlerCtx.getInputValue("logRecords");
        if (records != null) {
            // Make sure there's something to do...
            boolean truncate = (Boolean) handlerCtx.getInputValue("truncate");
            int truncLen = (Integer) handlerCtx.getInputValue("truncateLength");
            Locale locale = GuiUtil.getLocale();

            // Loop through the records...
            for (Map<String, Object> record : records) {
                record.put("dateTime",
                        formatDateForDisplay(locale, new Date(Long.parseLong(record.get("loggedDateTimeInMS").toString()))));
                /*
                 // FIXME: Should we add this code back in?  It was not being
                 // FIXME: used in the current version.
                 String msgId = (String) row.getMessageID();
                 String level = (String) row.getLevel();
                 String moduleName = (String)row.getModule();
                 //only SEVERE msg provoides diagnostic info.
                 if (level.equalsIgnoreCase("severe")) {
                 // NOTE: Image name/location is hard-coded
                 record.put("levelImage", GuiUtil.getMessage("common.errorGif"));
                 record.put(SHOW_LEVEL_IMAGE, new Boolean(true));
                 record.put("diagnosticCauses", getDiagnosticCauses(handlerCtx, msgId, moduleName));
                 record.put("diagnosticChecks", getDiagnosticChecks(handlerCtx, msgId, moduleName));
                 //                        record.put("diagnosticURI", getDiagnosticURI(handlerCtx, msgId));
                 } else {
                 record.put(SHOW_LEVEL_IMAGE, new Boolean(false));
                 record.put("diagnostic", "");
                 }
                 record.put("level", level);
                 record.put("productName", row.getProductName());
                 record.put("logger", moduleName);
                 */
                String message = ((String) record.get("Message")).trim();
                if (truncate && (message.length() > truncLen)) {
                    message = message.substring(0, truncLen).concat("...\n");
                }
                record.put("Message", Util.htmlEscape(message));
            }
        }

        // Set the first / last record numbers as attributes
        if ((records != null) && (records.size() > 0)) {
            handlerCtx.setOutputValue("firstRecord", records.get(0).get("recordNumber"));
            handlerCtx.setOutputValue("lastRecord", records.get(records.size() - 1).get("recordNumber"));
            //hasResults = true;
        } else {
            handlerCtx.setOutputValue("firstRecord", "-1");
            handlerCtx.setOutputValue("lastRecord", "-1");
        }
        handlerCtx.setOutputValue("result", records);
    }

    /**
     * Utility for adding non-null values to the map as a String.
     */
    private static void notNullStringPut(Map<String, Object> map, String key, Object val) {
        if (val != null) {
            map.put(key, val.toString());
        }
    }

    /**
     * This method converts a date/time string to a Date.
     *
     * @param	request	The ServletRequest
     * @param	date	The date as a String (or the date/time as a Date)
     * @param	time	The time as a String (or null)
     * @param	vd	The ViewDescriptor (for exception handling)
     * @param	view	The View (for exception handling)
     */
    protected static Date convertDateTime(HandlerContext handlerCtx, Object date, Object time) {
        // If Date is already a Date, then do nothing
        if (date instanceof Date) {
            return (Date) date;
        }
        // If Date is null or empty, return null
        if ((date == null) || (date.toString().trim().length() == 0)) {
            return null;
        }

        // Get the date / time string
        if ((time != null) && (time.toString().trim().length() == 0)) {
            time = null;
        }
        String dateTime = date.toString()
                + ((time == null) ? "" : (" " + time.toString()));
        DateFormat df = DateFormat.getDateInstance(
                DateFormat.SHORT, GuiUtil.getLocale());
        if ((time != null) && (df instanceof SimpleDateFormat)) {
            SimpleDateFormat fmt = (SimpleDateFormat) df;
            String formatPrefix = fmt.toLocalizedPattern();
            try {
                // Try w/ HH:mm:ss.SSS
                date = parseDateString(
                        fmt, formatPrefix + TIME_FORMAT, dateTime);
            } catch (ParseException ex) {
                try {
                    // Try w/ HH:mm:ss
                    date = parseDateString(
                            fmt, formatPrefix + TIME_FORMAT_2, dateTime);
                } catch (ParseException ex2) {
                    try {
                        // Try w/ HH:mm
                        date = parseDateString(
                                fmt, formatPrefix + TIME_FORMAT_3, dateTime);
                    } catch (ParseException ex3) {
                        GuiUtil.handleError(handlerCtx, "Unable to parse Date/Time: '" + dateTime + "'.");
                    }
                }
            }
        } else if (time != null) {
            // I don't think this ever happens
            df = DateFormat.getDateTimeInstance(
                    DateFormat.SHORT, DateFormat.LONG, GuiUtil.getLocale());
            try {
                date = df.parse(dateTime);
            } catch (ParseException ex) {
                GuiUtil.handleError(handlerCtx, "Unable to parse Date/Time: '" + dateTime + "'.");
            }
        } else {
            try {
                date = df.parse(dateTime);
            } catch (ParseException ex) {
                GuiUtil.handleError(handlerCtx, "Unable to parse Date/Time: '" + dateTime + "'.");
            }
        }

        // Return the result
        Date convertDate = null;
        try {
            convertDate = (Date) date;
        } catch (Exception ex) {
            convertDate = null;
        }
        return convertDate;
    }

    /**
     * This method simply takes the given SimpleDateFormat and parses the given
     * String after applying the given format String.
     */
    private static Date parseDateString(SimpleDateFormat fmt, String format, String dateTime) throws ParseException {
        fmt.applyLocalizedPattern(format);
        return fmt.parse(dateTime);
    }

    /**
     * This method formats a log file date to a more readable date (based on
     * locale).
     */
    public static String formatDateForDisplay(Locale locale, Date date) {
        DateFormat dateFormat = DateFormat.getDateInstance(
                DateFormat.MEDIUM, locale);
        if (dateFormat instanceof SimpleDateFormat) {
            SimpleDateFormat fmt = (SimpleDateFormat) dateFormat;
            fmt.applyLocalizedPattern(fmt.toLocalizedPattern() + TIME_FORMAT);
            return fmt.format(date);
        } else {
            dateFormat = DateFormat.getDateTimeInstance(
                    DateFormat.MEDIUM, DateFormat.LONG, locale);
            return dateFormat.format(date);
        }
    }

    /**
     * <p> This handler returns the first and last log record.</p>
     *
     * <p> Output value: "LogFileNames" -- Type:
     * <code>java.util.SelectItem</code>
     *
     * @param context The HandlerContext.
     */
    @Handler(id = "getFirstLastRecord",
            input = {
        @HandlerInput(name = "FirstRecord", type = String.class, required = true),
        @HandlerInput(name = "LastRecord", type = String.class, required = true)},
            output = {
        @HandlerOutput(name = "First", type = String.class),
        @HandlerOutput(name = "Last", type = String.class)})
    public static void getFirstLastRecord(HandlerContext handlerCtx) {
        // Get the first/last row numbers
        String firstLogRow = (String) handlerCtx.getInputValue("FirstRecord");
        String lastLogRow = (String) handlerCtx.getInputValue("LastRecord");
        if (firstLogRow == null) {
            firstLogRow = "0";
        }
        if (lastLogRow == null) {
            lastLogRow = "0";
        }
        int firstRow = 0;
        try {
            firstRow = Integer.parseInt(firstLogRow);
            int lastRow = Integer.parseInt(lastLogRow);
            if (firstRow > lastRow) {
                String temp = firstLogRow;
                firstLogRow = lastLogRow;
                lastLogRow = temp;
            }
            handlerCtx.setOutputValue("First", firstLogRow);
            handlerCtx.setOutputValue("Last", lastLogRow);
        } catch (NumberFormatException ex) {
            // ignore
        }
    }

    /**
     * This method formats the diagnostic to be displayed for HTML Add '<br>' to
     * each elements of the ArrayList and returns the String.
     */
    protected static String formatArrayForDisplay(String[] diag) {
        if ((diag == null) || (diag.length == 0)) {
            return "";
        }
        StringBuilder buf = new StringBuilder("<br>");
        for (int i = 0; i < diag.length; i++) {
            buf.append((String) diag[i]);
            buf.append("<br>");
        }
        return buf.toString();
    }

    /**
     * <P>This method puts the current time (as a String) in the desired
     * attribute. The result attribute must be specified via an attribute named
     * "getTimeResultAttribute"</P>
     */
    @Handler(id = "getTime",
            output = {
        @HandlerOutput(name = "Time", type = String.class)})
    public void getTime(HandlerContext handlerCtx) {
        try {
            DateFormat df = DateFormat.getTimeInstance(
                    DateFormat.SHORT, GuiUtil.getLocale());
            ((SimpleDateFormat) df).applyLocalizedPattern(TIME_FORMAT);

            // Set the return value
            handlerCtx.setOutputValue("Time", df.format(new Date()));
        } catch (Exception ex) {
            GuiUtil.handleException(handlerCtx, ex);
        }
    }

    /**
     * <P>This method returns the current date (as a String). The DATE_FORMAT
     * must be specified, if it is not this method will fail. You may set it to
     * "short", "medium", "long", or "FULL".</P>
     *
     * <P>If you do not set it to one of these values, you may set it to a valid
     * format string.</P>
     */
    @Handler(id = "getDate",
            input = {
        @HandlerInput(name = "DateFormat", type = String.class, required = true)},
            output = {
        @HandlerOutput(name = "Date", type = String.class)})
    public void getDate(HandlerContext handlerCtx) {
        // Get the required attribute
        String formatString = (String) handlerCtx.getInputValue("DateFormat");

        // Get the type
        int formatType = -1;
        if (formatString.equals(GET_DATE_SHORT)) {
            formatType = DateFormat.SHORT;
        } else if (formatString.equals(GET_DATE_MEDIUM)) {
            formatType = DateFormat.MEDIUM;
        } else if (formatString.equals(GET_DATE_LONG)) {
            formatType = DateFormat.LONG;
        } else if (formatString.equals(GET_DATE_FULL)) {
            formatType = DateFormat.FULL;
        }
        DateFormat df = null;
        if (formatType == -1) {
            df = DateFormat.getDateInstance(
                    DateFormat.SHORT, GuiUtil.getLocale());
            ((SimpleDateFormat) df).applyLocalizedPattern(formatString);
        } else {
            df = DateFormat.getDateInstance(
                    formatType, GuiUtil.getLocale());
        }

        // Set the return value
        handlerCtx.setOutputValue("Date", df.format(new Date()));
    }

    /**
     * <P>This method returns the formatted date (as a String). </P>
     *
     */
    @Handler(id = "getFormattedDateTime",
            input = {
        @HandlerInput(name = "Timestamp", type = String.class, required = true),
        @HandlerInput(name = "AddHour", type = Boolean.class)},
            output = {
        @HandlerOutput(name = "Time", type = String.class),
        @HandlerOutput(name = "Date", type = String.class)})
    public void getFormattedDateTime(HandlerContext handlerCtx) {
        String timeStamp = (String) handlerCtx.getInputValue("Timestamp");
        Boolean addHour = (Boolean) handlerCtx.getInputValue("AddHour");
        Date date = null;
        if ((timeStamp == null) || "".equals(timeStamp)) {
            date = new Date(System.currentTimeMillis());
        } else {
            try {
                if (addHour != null) {
                    date = new Date(Long.parseLong(timeStamp) + ONE_HOUR);
                } else {
                    date = new Date(Long.parseLong(timeStamp));
                }
            } catch (Exception ex) {
                GuiUtil.getLogger().info(GuiUtil.getCommonMessage("log.error.dateFormat") + ex.getLocalizedMessage());
                if (GuiUtil.getLogger().isLoggable(Level.FINE)) {
                    ex.printStackTrace();
                }
                date = new Date(System.currentTimeMillis());
            }
        }
        DateFormat df = DateFormat.getDateInstance(DateFormat.SHORT, GuiUtil.getLocale());
        DateFormat tf = DateFormat.getTimeInstance(DateFormat.MEDIUM, GuiUtil.getLocale());
        ((SimpleDateFormat) tf).applyLocalizedPattern(" HH:mm:ss.SSS");

        String ftime = tf.format(date);
        String fdate = df.format(date);
        handlerCtx.setOutputValue("Time", ftime);
        handlerCtx.setOutputValue("Date", fdate);
    }
    /**
     * This defines the short date format, used by DATE_FORMAT. ("short")
     */
    public static final String GET_DATE_SHORT = "short";
    /**
     * This defines the medium date format, used by DATE_FORMAT. ("medium")
     */
    public static final String GET_DATE_MEDIUM = "medium";
    /**
     * This defines the long date format, used by DATE_FORMAT. ("long")
     */
    public static final String GET_DATE_LONG = "long";
    /**
     * This defines the full date format, used by DATE_FORMAT. ("full")
     */
    public static final String GET_DATE_FULL = "full";
    /**
     * This specifies how TIME fields are input and displayed. We need to do
     * this in order to get a display/input that works with milliseconds.
     * Perhaps in the future we may want to just append the milliseconds?
     */
    public static final String TIME_FORMAT = " HH:mm:ss.SSS";
    public static final String TIME_FORMAT_2 = " HH:mm:ss";
    public static final String TIME_FORMAT_3 = " HH:mm";
    /**
     * If the number to display is not specified, this value will be used (40).
     */
    public static final Integer DEFAULT_NUMBER_TO_DISPLAY = Integer.valueOf(40);
    /**
     *
     */
    public static final String FIRST_LOG_ROW = "firstLogRow";
    public static final int FROM_RECORD = 0;
    /**
     *
     */
    public static final String LAST_LOG_ROW = "lastLogRow";
    /**
     * The following constant defines the valid delimiters that can be used to
     * seperate custom loggers on input. (" \t\n\r\f,;:")
     */
    private static final String[] CUSTOM_LOGGER_DELIMITERS = {" \t", "\r\n", "\f", ",", ";", ":"};
    /**
     * The following constant defines the valid delimiters that can be used to
     * seperate nvp entries on input. (" \t\n\r\f,;:")
     */
    public static final String NVP_DELIMITERS = " \t\n\r\f,;:";
    /**
     * This is the delimiter between the property name and property value.
     */
    public static final char EQUALS = '=';
    /**
     * This model key is set by the filter method, it is true if a level image
     * should be displayed.
     */
    public static final String SHOW_LEVEL_IMAGE = "showLevelImage";
    /**
     * This is the root directory of the alert images
     */
    public static final String LEVEL_IMAGE_ROOT =
            "/com_sun_web_ui/images/alerts/";
    public static final long ONE_HOUR = (1000 /* ms */) * (60 /* sec */) * (60 /* min */);
}
