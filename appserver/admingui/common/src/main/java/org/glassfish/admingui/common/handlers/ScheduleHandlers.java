
/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2016 Oracle and/or its affiliates. All rights reserved.
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

//import java.util.*;


import com.sun.jsftemplating.annotation.HandlerInput;
import com.sun.jsftemplating.annotation.HandlerOutput;
import com.sun.jsftemplating.annotation.Handler;
import com.sun.jsftemplating.layout.descriptors.handler.HandlerContext;

import org.glassfish.admingui.common.util.GuiUtil;
import java.text.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import org.glassfish.admingui.common.util.RestUtil;
/**
 *
 * @author Siraj
 */
public class ScheduleHandlers {

    private static final String[] DAYS_OF_WEEK ={ "Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat",};
    private static final String[] MONTHS ={"", "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
    private static final String[] DAYS_OF_MONTH = new String[33];
    private static final Map<String, String[]> values = new HashMap();

    private static final String DAY_OF_WEEK="dayOfWeek", DAY_OF_MONTH="dayOfMonth", MONTH="month";

    static {
        int i = 0;
        for (; i <= 31; i++) {
            DAYS_OF_MONTH[i] = String.valueOf(i);
        }
        DAYS_OF_MONTH[i] = "last";

        values.put(DAY_OF_WEEK, DAYS_OF_WEEK);
        values.put(MONTH, MONTHS);
        values.put(DAY_OF_MONTH, DAYS_OF_MONTH);
    }

    @Handler(id = "gf.getScheduleData",
        input = {
            @HandlerInput(name = "scheduleName", type = String.class)},
        output = {
            @HandlerOutput(name = DAY_OF_WEEK, type = java.util.Map.class),
            @HandlerOutput(name = DAY_OF_MONTH, type = java.util.Map.class),
            @HandlerOutput(name = MONTH, type = java.util.Map.class)})


    public static void getScheduleData(HandlerContext handlerCtx) {
        String scheduleName = (String) handlerCtx.getInputValue("scheduleName");
        String dayOfWeek = "*", dayOfMonth = "*", month = "*";

        if (scheduleName != null) {
            String endPoint = GuiUtil.getSessionValue("REST_URL") + "/configs/config/server-config/schedules/schedule/" +
                    scheduleName;

            Map attribs = RestUtil.getAttributesMap(endPoint);

            dayOfWeek = (String)attribs.get(DAY_OF_WEEK);
            dayOfMonth = (String)attribs.get(DAY_OF_MONTH);
            month = (String)attribs.get(MONTH);
        }
        Map dayOfWeekMap = getDataMap(dayOfWeek, DAY_OF_WEEK);
        Map dayOfMonthMap = getDataMap(dayOfMonth, DAY_OF_MONTH);
        Map monthMap = getDataMap(month, MONTH);

        handlerCtx.setOutputValue(DAY_OF_WEEK, dayOfWeekMap);
        handlerCtx.setOutputValue(DAY_OF_MONTH, dayOfMonthMap);
        handlerCtx.setOutputValue(MONTH, monthMap);

    }

    private static Map getDataMap(String data, String type) {
        List<String> dataList = GuiUtil.parseStringList(data, ",");
        String[] dataValues = values.get(type);
        Map dataMap = new HashMap();
        for (String dataItem : dataList) {
            if (dataItem.equals("*")) {
                dataMap.put(dataItem, dataItem);
                continue;
            }
            try {
                int i = Integer.parseInt(dataItem);
                if (i >=0 && i < dataValues.length)
                    dataMap.put(dataValues[i], String.valueOf(i));
            } catch (NumberFormatException e) {
                for (int i = 0; i < dataValues.length; i++) {
                    if (dataValues[i].equalsIgnoreCase(dataItem))
                        dataMap.put(dataItem, String.valueOf(i));
                }
            }
        }
        return dataMap;
    }

    @Handler(id = "gf.convertScheduleToString",
        input = {
            @HandlerInput(name = "map", type = java.util.Map.class, required=true),
            @HandlerInput(name = "type", type = String.class, required=true),
            @HandlerInput(name = "delimiter", type = String.class)
        },
        output = {
            @HandlerOutput(name = "str", type = String.class)
        })

    public static void convertScheduleToString(HandlerContext handlerCtx) {
        Map<String, String> map = (Map) handlerCtx.getInputValue("map");
        String delimiter = (String)handlerCtx.getInputValue("delimiter");
        String type = (String)handlerCtx.getInputValue("type");
        
        if (delimiter == null)
            delimiter =",";
        String str = "";
        String[] data = values.get(type);
        for(Map.Entry<String,String> e : map.entrySet()){
            Object o = e.getValue();
            if (o == null)
                continue;
            if (e.getKey().equals("*")) {
                str = "*";
                break;
            }
            try {
                int val = Integer.parseInt(o.toString());

                if (val >= 0 && val < data.length) {
                    if (str.length() > 0)
                        str = str + ",";
                    str = str + data[val];
                }
            } catch(Exception ex) {
                GuiUtil.getLogger().info(GuiUtil.getCommonMessage("log.error.convertScheduleToString") + ex.getLocalizedMessage());
                if (GuiUtil.getLogger().isLoggable(Level.FINE)){
                    ex.printStackTrace();
                }
            }
        }
        if (str.length() == 0)
            str = "*";
        handlerCtx.setOutputValue("str", str);
    }

    @Handler(id = "gf.sort",
        input = {
            @HandlerInput(name = "months", type = String.class, required=true),
            @HandlerInput(name = "delimiter", type = String.class)
        },
        output = {
            @HandlerOutput(name = "sorted", type = String.class)
        })
    public static void sortMonths(HandlerContext handlerContext) {

        DateFormat formatter = new SimpleDateFormat("dd-MMM-yy");

        List<Date> dateList = new ArrayList();
        String months = (String) handlerContext.getInputValue("months");
        List<String> monthsList = GuiUtil.parseStringList(months, ",");
        for (String month:monthsList) {
            if (month.equals("*")) {
//                sortedList.add("*");
            } else {
                try {
                    Date date1 = formatter.parse("01-" + month + "-00");
                    dateList.add(date1);
                } catch (Exception ex) {
                    GuiUtil.getLogger().info(GuiUtil.getCommonMessage("log.error.sortMonths") + ex.getLocalizedMessage());
                    if (GuiUtil.getLogger().isLoggable(Level.FINE)){
                        ex.printStackTrace();
                    }
                }
            }
        }
        Collections.sort(dateList);
        handlerContext.setOutputValue("sortedList", dateList);
/*
        Date d  = Calendar.getInstance().getTime();
        String[] shortMonths = new DateFormatSymbols().getShortMonths();

        for (int i = 0; i < shortMonths.length; i++) {
            String shortMonth = shortMonths[i];
            if (monthsList.contains(shortMonths[i])) {

            }
        }
 *
 */
    }

}
