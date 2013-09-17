/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2013 Oracle and/or its affiliates. All rights reserved.
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

import java.io.UnsupportedEncodingException;
import org.glassfish.admingui.common.util.GuiUtil;

import com.sun.jsftemplating.annotation.Handler;
import com.sun.jsftemplating.annotation.HandlerInput;
import com.sun.jsftemplating.annotation.HandlerOutput;
import com.sun.jsftemplating.layout.descriptors.handler.HandlerContext;
import java.net.URLEncoder;

import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Locale;
import java.util.Date;
import java.util.ListIterator;
import java.util.logging.Level;

import org.glassfish.admingui.common.util.RestResponse;
import org.glassfish.admingui.common.util.RestUtil;

/**
 *
 * @author Ana
 */
public class MonitoringHandlers {
    @Handler(id = "gf.getMonitorLevels",
    input = {
        @HandlerInput(name = "endpoint", type = String.class, required = true)},
    output = {
        @HandlerOutput(name = "monitorCompList", type = List.class)
    })
    public static void getMonitorLevels(HandlerContext handlerCtx) {
        String endpoint = (String) handlerCtx.getInputValue("endpoint");
        List result = new ArrayList();
        try {
            String monitoringLevelsEndPoint = endpoint + "/monitoring-service/module-monitoring-levels";
            Map<String, Object> attrs = RestUtil.getEntityAttrs(monitoringLevelsEndPoint, "entity");
            for(Map.Entry<String,Object> e : attrs.entrySet()){
                Map oneRow = new HashMap();
                String name = null;
                String moduleName=e.getKey();
                //workaround for GLASSFISH-19722.  Skip any cloud module.
                if(moduleName.startsWith("cloud")){
                    continue;
                }
                ListIterator ni = monDisplayList.listIterator();
                ListIterator vi = monNamesList.listIterator();
                while (ni.hasNext() && vi.hasNext()) {
                    String dispName = (String) ni.next();
                    String value = (String) vi.next();
                    if ((moduleName.equals(value))) {
                        name = dispName;
                        break;
                    }
                }
                if (name == null) {
                    name = moduleName;
                }
                oneRow.put("monCompName", name);
                oneRow.put("attrName", moduleName);
                oneRow.put("level", e.getValue());
                oneRow.put("selected", false);
                result.add(oneRow);
            }
        } catch (Exception ex) {
            GuiUtil.handleException(handlerCtx, ex);
        }
        handlerCtx.setOutputValue("monitorCompList", result);
    }

    /*
     * This handler returns a list of statistical data for an endpoint.
     * Useful for populating table
     */
    @Handler(id = "getStats",
    input = {
        @HandlerInput(name = "endpoint", type = String.class, required = true),
        @HandlerInput(name = "statType", type = String.class),
        @HandlerInput(name = "type", type = String.class)},
    output = {
        @HandlerOutput(name = "result", type = List.class),
        @HandlerOutput(name = "hasStats", type = Boolean.class)})
    public static void getStats(HandlerContext handlerCtx) {
        String endpoint = (String) handlerCtx.getInputValue("endpoint");
        String statType = (String) handlerCtx.getInputValue("statType");
        String type = (String) handlerCtx.getInputValue("type");
        Locale locale = GuiUtil.getLocale();
        DateFormat df = DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.DEFAULT, locale);
        //NumberFormat nf = NumberFormat.getNumberInstance(locale);
        List result = new ArrayList();

        try {
            //This check is to get the correct type of statistics.
            if ((type == null || statType == null) || type.equals(statType)) {
                if (RestUtil.doesProxyExist(endpoint)) {
                    Map<String, Object> stats = getMonitoringStatInfo(endpoint);
                    //Jersey monitoring data format
                    if (statType != null && statType.equals("jersey")) {
                        Map<String, Object> jerseyStats = new HashMap<String, Object>();
                        for(Map.Entry<String,Object> e : stats.entrySet()){
                            Map<String, Object> jerseyStat = (Map<String, Object>) e.getValue();
                            if (jerseyStat != null) {
                                jerseyStats.putAll(jerseyStat);
                            }
                        }
                        stats = jerseyStats;
                    }
                    for(Map.Entry<String,Object> e : stats.entrySet()){
                        if (!(e.getValue().getClass().equals(HashMap.class))) {
                            continue;
                        }
                        Map<String, Object> monAttrs = (Map<String, Object>) e.getValue();
                        Map<String, String> statMap = new HashMap();
                        String val = "";
                        String details = "--";
                        String desc = "--";
                        String start = "--";
                        String last = "--";
                        String unit = "";
                        String mname = null;
                        String runtimes = null;
                        String queuesize = null;
                        String thresholds = "--";

                        if (!monAttrs.isEmpty()) {

                            if (monAttrs.containsKey("name")) {
                                mname = (String) monAttrs.get("name");
                            } else if (monAttrs.containsKey("appname")) {
                                mname = (String) monAttrs.get("appname");
                            }
                            unit = (String) monAttrs.get("unit");
                            desc = (String) monAttrs.get("description");

                            Long lastTime = (Long) monAttrs.get("lastsampletime");
                            if (lastTime != -1) {
                                last = df.format(new Date(lastTime));
                            }
                            Long startTime = (Long) monAttrs.get("starttime");
                            if (startTime != -1) {
                                start = df.format(new Date(startTime));
                            }
                            if (monAttrs.containsKey("count")) {
                                val = monAttrs.get("count") + " " + unit;
                            } else if (monAttrs.containsKey("current")) {
                                if (unit != null) {
                                    if (unit.equals("String")) {
                                        if (mname.equals("LiveThreads")) {
                                            String str = (String) monAttrs.get("current");
                                            val = formatStringForDisplay(str);
                                        } else {
                                            val = (String) monAttrs.get("current");
                                        }
                                    } else if (unit.equals("List")) {
                                        String str = (String) monAttrs.get("current");
                                        String formatStr = formatActiveIdsForDisplay(str);
                                        if (!formatStr.isEmpty() && !formatStr.equals("")) {
                                            val = formatStr;
                                        }
                                    } else {
                                        Long currentVal = (Long) monAttrs.get("current");
                                        val = currentVal + unit;
                                    }
                                }
                            } else if (monAttrs.containsKey("applicationtype")) {
                                val = (String) monAttrs.get("applicationtype");
                            }

                            //Update the details
                            if (monAttrs.containsKey("appName")) {
                                details = (GuiUtil.getMessage("msg.AppName") + ": " + monAttrs.get("appName") + "<br/>");
                            }
                            if (monAttrs.containsKey("appname")) {
                                details = (GuiUtil.getMessage("msg.AppName") + ": " + monAttrs.get("appname") + "<br/>");
                            }
                            if (monAttrs.containsKey("environment")) {
                                details = details + (GuiUtil.getMessage("msg.Environment") + ": " + monAttrs.get("environment") + "<br/>");
                            }
                            if (monAttrs.containsKey("address")) {
                                details = details + (GuiUtil.getMessage("msg.Address") + ": " + monAttrs.get("address") + "<br/>");
                            }
                            if (monAttrs.containsKey("deploymenttype")) {
                                details = details + (GuiUtil.getMessage("msg.DepType") + ": " + monAttrs.get("deploymenttype") + "<br/>");
                            }
                            if (monAttrs.containsKey("endpointname")) {
                                details = details + (GuiUtil.getMessage("msg.EndPointName") + ": " + monAttrs.get("endpointname") + "<br/>");
                            }
                            if (monAttrs.containsKey("classname")) {
                                details = (GuiUtil.getMessage("msg.ClassName") + ": " + monAttrs.get("classname") + "<br/>");
                            }
                            if (monAttrs.containsKey("impltype")) {
                                details = details + (GuiUtil.getMessage("msg.ImplClass") + ": " + monAttrs.get("implclass") + "<br/>");
                            }
                            if (monAttrs.containsKey("implclass") && monAttrs.containsKey("impltype")) {
                                details = details + (GuiUtil.getMessage("msg.ImplType") + ": " + monAttrs.get("impltype") + "<br/>");
                            }

                            if (monAttrs.containsKey("namespace")) {
                                details = details + (GuiUtil.getMessage("msg.NameSpace") + ": " + monAttrs.get("namespace") + "<br/>");
                            }
                            if (monAttrs.containsKey("portname")) {
                                details = details + (GuiUtil.getMessage("msg.PortName") + ": " + monAttrs.get("portname") + "<br/>");
                            }
                            if (monAttrs.containsKey("servicename")) {
                                details = details + (GuiUtil.getMessage("msg.ServiceName") + ": " + monAttrs.get("servicename") + "<br/>");
                            }
                            if (monAttrs.containsKey("tester")) {
                                details = details + (GuiUtil.getMessage("msg.Tester") + ": " + monAttrs.get("tester") + "<br/>");
                            }
                            if (monAttrs.containsKey("wsdl")) {
                                details = details + (GuiUtil.getMessage("msg.WSDL") + ": " + monAttrs.get("wsdl") + "<br/>");
                            }

                            if (monAttrs.containsKey("maxtime")) {
                                details = (GuiUtil.getMessage("msg.MaxTime") + ": " + monAttrs.get("maxtime") + " " + unit + "<br/>");
                            }
                            if (monAttrs.containsKey("mintime")) {
                                details = details + (GuiUtil.getMessage("msg.MinTime") + ": " + monAttrs.get("mintime") + " " + unit + "<br/>");
                            }
                            if (monAttrs.containsKey("totaltime")) {
                                details = details + (GuiUtil.getMessage("msg.TotalTime") + ": " + monAttrs.get("totaltime") + " " + unit + "<br/>");
                            }
                            if (monAttrs.containsKey("highwatermark")) {
                                details = (GuiUtil.getMessage("msg.HWaterMark") + ": " + monAttrs.get("highwatermark") + " " + unit + "<br/>");
                            }
                            if (monAttrs.containsKey("lowwatermark")) {
                                details = details + (GuiUtil.getMessage("msg.LWaterMark") + ": " + monAttrs.get("lowwatermark") + " " + unit + "<br/>");
                            }
                            if (monAttrs.containsKey("activeruntimes")) {
                                runtimes = (String) monAttrs.get("activeruntimes");
                            }
                            if (monAttrs.containsKey("queuesize")) {
                                queuesize = (String) monAttrs.get("queuesize");
                            }
                            if (monAttrs.containsKey("hardmaximum") && monAttrs.get("hardmaximum") != null) {
                                val = monAttrs.get("hardmaximum") + " " + "hard max " + "<br/>" + monAttrs.get("hardminimum") + " " + "hard min";
                            }
                            if (monAttrs.containsKey("newthreshold") && monAttrs.get("newThreshold") != null) {
                                thresholds = monAttrs.get("newthreshold") + " " + "new " + "<br/>" + monAttrs.get("queuedownthreshold") + " " + "queue down";
                            }
                            if (monAttrs.containsKey("queuesize") && monAttrs.containsKey("environment")) {
                                details = details + monAttrs.get("environment");
                            }

                            statMap.put("name", mname);
                            statMap.put("startTime", start);
                            statMap.put("lastTime", last);
                            statMap.put("description", desc);
                            statMap.put("value", (val == null) ? "" : val);
                            statMap.put("details",  details);
                            statMap.put("thresholds", thresholds);
                            statMap.put("queueSize", (queuesize == null) ? "--" : queuesize);
                            statMap.put("runtimes", (runtimes == null) ? "--" : runtimes);
                            result.add(statMap);
                        }
                    }
                }
            }
            handlerCtx.setOutputValue("result", result);
            handlerCtx.setOutputValue("hasStats", (result.size() == 0) ? false : true);
        } catch (Exception ex) {
            GuiUtil.handleException(handlerCtx, ex);
        }
    }

    @Handler(id = "updateMonitorLevels",
    input = {
        @HandlerInput(name = "allRows", type = List.class, required = true),
        @HandlerInput(name = "endpoint", type = String.class)})
    public static void updateMonitorLevels(HandlerContext handlerCtx) {
        String endpoint = (String) handlerCtx.getInputValue("endpoint");
        List<Map> allRows = (List<Map>) handlerCtx.getInputValue("allRows");
        Map payload = new HashMap();
        for (Map<String, String> oneRow : allRows) {
            payload.put(oneRow.get("attrName"), oneRow.get("level"));
        }
        try{
            RestUtil.restRequest( endpoint , payload, "post" , null, false);
        }catch (Exception ex){
            GuiUtil.getLogger().severe(GuiUtil.getCommonMessage("msg.error.save.monitor.modules" ,  new Object[]{endpoint, payload}));
            GuiUtil.handleError(handlerCtx, GuiUtil.getMessage("msg.error.checkLog"));
            return;
        }
    }

    /**
     *	<p> Add list to new list
     */
    @Handler(id = "addToMonitorList",
    input = {
        @HandlerInput(name = "oldList", type = List.class),
        @HandlerInput(name = "newList", type = List.class)},
    output = {
        @HandlerOutput(name = "result", type = List.class)
    })
    public static void addToMonitorList(HandlerContext handlerCtx) {
        List<String> oldList = (List) handlerCtx.getInputValue("oldList");
        List<String> newList = (List) handlerCtx.getInputValue("newList");
        if (newList == null) {
            newList = new ArrayList();
        }
        if (oldList != null) {
            for (String sk : oldList) {
                newList.add(sk);
            }
        }
        handlerCtx.setOutputValue("result", newList);
    }

    @Handler(id = "getValidMonitorLevels",
    output = {
        @HandlerOutput(name = "monitorLevelList", type = List.class)
    })
    public static void getValidMonitorLevels(HandlerContext handlerCtx) {
        handlerCtx.setOutputValue("monitorLevelList", levels);
    }

    @Handler(id = "getFirstValueFromList",
    input = {
        @HandlerInput(name = "values", type = List.class, required = true)},
    output = {
        @HandlerOutput(name = "firstValue", type = String.class)
    })
    public static void getFirstValueFromList(HandlerContext handlerCtx) {
        List values = (List) handlerCtx.getInputValue("values");
        String firstval = "";
        if ((values != null) && (values.size() != 0)) {
            firstval = (String) values.get(0);

        }
        handlerCtx.setOutputValue("firstValue", firstval);
    }

    @Handler(id = "getAppName",
    input = {
        @HandlerInput(name = "endpoint", type = String.class, required = true),
        @HandlerInput(name = "name", type = String.class, required = true)},
    output = {
        @HandlerOutput(name = "appName", type = String.class),
        @HandlerOutput(name = "appFullName", type = String.class)
    })
    public static void getAppName(HandlerContext handlerCtx) {
        String endpoint = (String) handlerCtx.getInputValue("endpoint");
        String name = (String) handlerCtx.getInputValue("name");
        String appName = name;
        String fullName = name;
        try {
            List<String> applications = new ArrayList<String>(RestUtil.getChildMap(endpoint).keySet());
            for (String oneApp : applications) {
                List<String> modules = new ArrayList<String>(RestUtil.getChildMap(endpoint + "/" + oneApp + "/module").keySet());
                if (modules.contains(name)) {
                    appName = oneApp;
                    break;
                }
            }
            if (fullName != null && !(name.equals(appName))) {
                fullName = URLEncoder.encode(appName, "UTF-8") + "/" + URLEncoder.encode(name, "UTF-8");
            }
            if (appName != null) {
                appName = URLEncoder.encode(appName, "UTF-8");
            }
        } catch (Exception ex) {
            GuiUtil.handleException(handlerCtx, ex);
        }

        handlerCtx.setOutputValue("appName", appName);
        handlerCtx.setOutputValue("appFullName", fullName);
    }

    @Handler(id = "getWebStatsUrl",
    input = {
        @HandlerInput(name = "app", type = String.class, required = true),
        @HandlerInput(name = "compVal", type = String.class, required = true),
        @HandlerInput(name = "vsList", type = List.class, required = true),
        @HandlerInput(name = "monitorURL", type = String.class, required = true),
        @HandlerInput(name = "moduleProps", type = Map.class, required = true)},
    output = {
        @HandlerOutput(name = "webServletUrl", type = String.class),
        @HandlerOutput(name = "webServletType", type = String.class),
        @HandlerOutput(name = "webUrl", type = String.class),
        @HandlerOutput(name = "webType", type = String.class)
    })
    public static void getWebStatsUrl(HandlerContext handlerCtx) {
        String app = (String) handlerCtx.getInputValue("app");
        String monitorURL = (String) handlerCtx.getInputValue("monitorURL");
        List<String> vsList = (List<String>) handlerCtx.getInputValue("vsList");
        String compVal = (String) handlerCtx.getInputValue("compVal");
        Map<String, String> moduleProps = (Map<String, String>) handlerCtx.getInputValue("moduleProps");
        String webUrl = "EMPTY";
        String webServletUrl = "EMPTY";
        String statType = "EMPTY";
        String webType = "EMPTY";
        String monitorEndpoint = monitorURL + "/applications/" + app;

        try {
            for (String vs : vsList) {
                if (doesMonitoringDataExist(monitorEndpoint + "/" + URLEncoder.encode(vs, "UTF-8"))) {
                    webUrl = monitorEndpoint + "/" + URLEncoder.encode(vs, "UTF-8");
                    webType = "Web";
                    break;
                }
            }
            if (compVal != null && !(compVal.equals(""))) {
                String[] compStrs = compVal.split("/");
                if (vsList.contains(compStrs[0])) {
                    if (moduleProps != null && moduleProps.containsKey(compStrs[1]) && moduleProps.get(compStrs[1]).equals("Servlet")) {
                        monitorEndpoint = monitorEndpoint + "/" + URLEncoder.encode(compStrs[0], "UTF-8") + "/" + URLEncoder.encode(compStrs[1], "UTF-8");

                        if (RestUtil.doesProxyExist(monitorEndpoint)) {
                            webServletUrl = monitorEndpoint;
                            statType = "ServletInstance";
                        }
                    }
                }
            }
        } catch (UnsupportedEncodingException ex) {
            GuiUtil.getLogger().info(GuiUtil.getCommonMessage("log.error.getWebStatsUrl") + ex.getLocalizedMessage());
            if (GuiUtil.getLogger().isLoggable(Level.FINE)) {
                ex.printStackTrace();
            }
        }
        handlerCtx.setOutputValue("webServletUrl", webServletUrl);
        handlerCtx.setOutputValue("webServletType", statType);
        handlerCtx.setOutputValue("webUrl", webUrl);
        handlerCtx.setOutputValue("webType", webType);
    }

    @Handler(id = "getStatsUrl",
    input = {
        @HandlerInput(name = "app", type = String.class, required = true),
        @HandlerInput(name = "moduleProps", type = Map.class, required = true),
        @HandlerInput(name = "monitorURL", type = String.class, required = true),
        @HandlerInput(name = "compVal", type = String.class, required = true)},
    output = {
        @HandlerOutput(name = "statUrl", type = String.class),
        @HandlerOutput(name = "statType", type = String.class)
    })
    public static void getStatsUrl(HandlerContext handlerCtx) {
        String app = (String) handlerCtx.getInputValue("app");
        String comp = (String) handlerCtx.getInputValue("compVal");
        String monitorURL = (String) handlerCtx.getInputValue("monitorURL");
        Map<String, String> moduleProps = (Map<String, String>) handlerCtx.getInputValue("moduleProps");
        StringBuilder statUrl = new StringBuilder();
        String statType = "";

        if (comp != null && !(comp.trim().equals(""))) {
            List<String> compStrs = new ArrayList<String>();
            if (comp.startsWith("resources/")) {
                compStrs.add("resources");
                compStrs.add(comp.substring(10));
            } else {
                compStrs = Arrays.asList(comp.split("/"));
            }
            try {
                statUrl = statUrl.append(monitorURL).append("/applications/").append(app);
                for (String str : compStrs) {
                    statUrl = statUrl.append("/").append(URLEncoder.encode(str, "UTF-8"));
                }
            } catch (UnsupportedEncodingException ex) {
                GuiUtil.getLogger().log(Level.INFO, "{0}{1}", new Object[]{GuiUtil.getCommonMessage("log.error.getStatsUrl"), ex.getLocalizedMessage()});
                if (GuiUtil.getLogger().isLoggable(Level.FINE)) {
                    ex.printStackTrace();
                }
            }

            if (RestUtil.doesProxyExist(statUrl.toString())) {
                if (compStrs.size() == 1) {
                    statType = (String) moduleProps.get(compStrs.get(0));
                } else if (compStrs.get(0).equals("resources")) {
                    statType="AppScopedResource";
                } else {
                    statType = modifyStatType(compStrs.get(1));
                }
            }
        }else{
            statUrl.append("EMPTY");
        }

        handlerCtx.setOutputValue("statUrl", statUrl.toString());
        handlerCtx.setOutputValue("statType", statType);
    }
    /*
     * Filter the request,session,jsp and servlets. 
     * Filed an issue :12687.
     * Once this issue is resolved, we can remove this handler.
     */

    @Handler(id = "filterWebStats",
    input = {
        @HandlerInput(name = "webStats", type = List.class, required = true),
        @HandlerInput(name = "statType", type = String.class, required = true)},
    output = {
        @HandlerOutput(name = "stats", type = List.class)
    })
    public static void filterWebStats(HandlerContext handlerCtx) {
        List<Map> webStats = (List<Map>) handlerCtx.getInputValue("webStats");
        String statType = (String) handlerCtx.getInputValue("statType");
        List<String> requestStatNames = java.util.Arrays.asList("MaxTime", "ProcessingTime", "RequestCount", "ErrorCount");
        List stats = new ArrayList();
        if (webStats != null) {
            for (Map webStat : webStats) {
                String statName = (String) webStat.get("name");
                if (requestStatNames.contains(statName) && statType.equals("Request")) {
                    stats.add(webStat);
                } else if (statName.contains(statType) && !(statType.equals("Request"))) {
                    stats.add(webStat);
                }
            }
        }

        handlerCtx.setOutputValue("stats", stats);
    }

    /*
     * Returns true if the given pool name is the child of an entity
     * (jdbc connection pools or connector connection pools).
     * This is used in monitoringResourceStats.jsf.
     */
    @Handler(id = "isPool",
    input = {
        @HandlerInput(name = "poolName", type = String.class, required = true),
        @HandlerInput(name = "endpoint", type = String.class, required = true)},
    output = {
        @HandlerOutput(name = "result", type = Boolean.class)
    })
    public static void isPool(HandlerContext handlerCtx) {
        String poolName = (String) handlerCtx.getInputValue("poolName");
        String endpoint = (String) handlerCtx.getInputValue("endpoint");
        Boolean result = false;
        try {
            List<String> poolNames = new ArrayList<String>(RestUtil.getChildMap(endpoint).keySet());
            if (poolNames.contains(poolName)) {
                result = true;
            }
        } catch (Exception ex) {
            GuiUtil.handleException(handlerCtx, ex);
        }

        handlerCtx.setOutputValue("result", result);

    }

    /*
     * Returns the jdbc connection pools, connector connection pools,
     * first jdbc element and first connector element for the given set of
     * pool names  and resources endpoint. 
     */
    @Handler(id = "gf.getMonitoringPools",
    input = {
        @HandlerInput(name = "poolNames", type = List.class, required = true),
        @HandlerInput(name = "endpoint", type = String.class, required = true)},
    output = {
        @HandlerOutput(name = "jdbcList", type = List.class),
        @HandlerOutput(name = "firstJdbc", type = String.class),
        @HandlerOutput(name = "connectorList", type = List.class),
        @HandlerOutput(name = "firstConnector", type = String.class)
    })
    public static void getMonitoringPools(HandlerContext handlerCtx) {
        List<String> poolNames = (List<String>) handlerCtx.getInputValue("poolNames");
        String endpoint = (String) handlerCtx.getInputValue("endpoint");
        List<String> jdbcMonitorList = new ArrayList<String>();
        List<String> connectorMonitorList = new ArrayList<String>();
        String fisrtJdbc = "";
        String firstConnector = "";

        try {
            List<String> jdbcPools = new ArrayList<String>(RestUtil.getChildMap(endpoint + "/jdbc-connection-pool").keySet());
            List<String> connectorPools = new ArrayList<String>(RestUtil.getChildMap(endpoint + "/connector-connection-pool").keySet());
            for (String poolName : poolNames) {
                if (jdbcPools.contains(poolName)) {
                    jdbcMonitorList.add(poolName);
                } else if (connectorPools.contains(poolName)) {
                    connectorMonitorList.add(poolName);
                }
            }
        } catch (Exception ex) {
            GuiUtil.handleException(handlerCtx, ex);
        }

        handlerCtx.setOutputValue("jdbcList", jdbcMonitorList);
        handlerCtx.setOutputValue("firstJdbc", fisrtJdbc);
        handlerCtx.setOutputValue("connectorList", connectorMonitorList);
        handlerCtx.setOutputValue("firstConnector", firstConnector);
    }

    @Handler(id = "gf.getInstanceMonitorURL",
    input = {
        @HandlerInput(name = "instanceName", type = String.class, defaultValue = "server")},
    output = {
        @HandlerOutput(name = "monitorURL", type = String.class)
    })
    public static void getInstanceMonitorURL(HandlerContext handlerCtx) {
        String instanceName = (String) handlerCtx.getInputValue("instanceName");
        String monitorURL = (String) GuiUtil.getSessionValue("MONITOR_URL") + "/" + instanceName;
        handlerCtx.setOutputValue("monitorURL", monitorURL);
    }

    public static Boolean doesAppProxyExist(String appName, String moduleName) {
        boolean proxyexist = false;
        Map<String, Object> subComps = getSubComponents(appName, moduleName);
        if (subComps != null && subComps.size() > 0) {
            proxyexist = true;
        }
        return proxyexist;
    }

    public static Map<String, Object> getSubComponents(String appName, String moduleName) {
        String endpoint = GuiUtil.getSessionValue("REST_URL") + "/applications/application/" + appName + "/list-sub-components";
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put("appname", appName);
        attrs.put("id", moduleName);

        try {
            Map<String, Object> responseMap = RestUtil.restRequest(endpoint, attrs, "GET", null, false);
            Map<String, Object> propsMap = (Map<String, Object>) ((Map<String, Object>) responseMap.get("data")).get("properties");
            if (propsMap != null && propsMap.size() > 0) {
                return propsMap;
            }
        } catch (Exception ex) {
            GuiUtil.getLogger().severe("Error in getSubComponents ; \nendpoint = " + endpoint + "attrs=" + attrs + "method=GET");
            //we don't need to call GuiUtil.handleError() because thats taken care of in restRequest() when we pass in the handler.
        }
        return null;
    }

    public static Boolean doesMonitoringDataExist(String endpoint) {
        if (RestUtil.doesProxyExist(endpoint)) {
            if (getMonitoringStatInfo(endpoint).size() > 0) {
                return true;
            }
        }
        return false;
    }

    public static String modifyStatType(String name) {
        String[] nameStrs = name.split("-");
        StringBuilder modifiedName = new StringBuilder();
        for (int i = 0; i < nameStrs.length; i++) {
            String tmp = nameStrs[i].substring(0, 1).toUpperCase(GuiUtil.guiLocale) + nameStrs[i].substring(1);
            modifiedName.append(tmp);
        }
        return modifiedName.toString();
    }

    private static String formatActiveIdsForDisplay(String str) {
        if (str==null){
            return "";
        }
        StringBuilder values = new StringBuilder(" ");
        String[] strArray = str.split("%%%EOL%%%");
        if (strArray.length > 0) {
            values.append("<table>");
            for (String s : (String[]) strArray) {
                if (s.startsWith("Transaction")) {
                    String sh = s.replaceFirst(" ", "_");
                    String[] strHeaders = sh.split(" ");
                    if (strHeaders.length > 0) {
                        values.append("<tr>");
                        for (String h : (String[]) strHeaders) {
                            if (!h.isEmpty()) {
                                values.append("<td>").append("</td>");
                            }

                        }
                        values.append("</tr>");
                    }
                } else {
                    String[] strData = s.split(" ");
                    if (strData.length > 0) {
                        values.append("<tr>");
                        for (String d : (String[]) strData) {
                            if (!d.isEmpty()) {
                                values.append("<td>").append(d).append("</td>");
                            }

                        }
                        values.append("</tr>");
                    }

                }
            }
            values.append("</table>");
        }
        return values.toString();
    }

    private static String formatStringForDisplay(String strToFormat) {
        String[] strs = strToFormat.split(",");
        StringBuilder formattedStr = new StringBuilder();
        if (strs.length > 10) {
            for (int i = 0; i < strs.length; i++) {
                String str = strs[i];
                if (! (formattedStr.length() == 0)) {
                    formattedStr.append(",");
                }
                if (i % 10 == 0 && i != 0) {
                    formattedStr.append("\n");
                }
                formattedStr.append(str);
            }
            return formattedStr.toString();
        }
        return strToFormat;
    }

    private static Map<String, Object> getMonitoringStatInfo(String endpoint) {
        Map<String, Object> monitorInfoMap = new HashMap<String, Object>();
        try {
            Map<String, Object> responseMap = RestUtil.restRequest(endpoint, null, "GET", null, false);
            Map<String, Object> dataMap = (Map<String, Object>) responseMap.get("data");
            if (dataMap != null) {
                Map<String, Object> extraPropsMap = (Map<String, Object>) dataMap.get("extraProperties");
                if (extraPropsMap != null) {
                    Map<String, Object> entityMap = (Map<String, Object>) extraPropsMap.get("entity");
                    if (entityMap != null) {
                        monitorInfoMap = entityMap;
                    }
                }
            }
        } catch (Exception ex) {
            GuiUtil.getLogger().log(Level.SEVERE,"Error in getMonitoringStatInfo ; \nendpoint = {0}" + "attrs=" + "method=GET", endpoint);
            //we don't need to call GuiUtil.handleError() because thats taken care of in restRequest() when we pass in the handler.
        }
        return monitorInfoMap;
    }
    final private static List<String> levels = new ArrayList();

    static {
        levels.add("OFF");
        levels.add("LOW");
        levels.add("HIGH");
    }
    //monitoring modulemonitoring.module names
    public static final String JVM = GuiUtil.getCommonMessage("monitoring.module.Jvm");

    public static final String WEB_CONTAINER = GuiUtil.getCommonMessage("monitoring.module.Web");

    public static final String HTTP_SERVICE = GuiUtil.getCommonMessage("monitoring.module.Http");

    public static final String THREAD_POOL = GuiUtil.getCommonMessage("monitoring.module.ThreadPool");

    public static final String JDBC_CONNECTION_POOL = GuiUtil.getCommonMessage("monitoring.module.Jdbc");

    public static final String CONNECTOR_CONNECTION_POOL = GuiUtil.getCommonMessage("monitoring.module.Connector");

    public static final String EJB_CONTAINER = GuiUtil.getCommonMessage("monitoring.module.Ejb");

    public static final String TRANSACTION_SERVICE = GuiUtil.getCommonMessage("monitoring.module.TransactionService");

    public static final String ORB = GuiUtil.getCommonMessage("monitoring.module.Orb");

    public static final String CONNECTOR_SERVICE = GuiUtil.getCommonMessage("monitoring.module.ConnectorService");

    public static final String JMS_SERVICE = GuiUtil.getCommonMessage("monitoring.module.JmsService");

    public static final String WEB_SERVICES_CONTAINER = GuiUtil.getCommonMessage("monitoring.module.WebServices");

    public static final String JPA = GuiUtil.getCommonMessage("monitoring.module.Jpa");

    public static final String SECURITY = GuiUtil.getCommonMessage("monitoring.module.Security");

    public static final String JERSEY = GuiUtil.getCommonMessage("monitoring.module.Jersey");

    public static final String DEPLOYMENT = GuiUtil.getCommonMessage("monitoring.module.Deployment");

    final private static List monDisplayList = new ArrayList();

    static {
        monDisplayList.add(JVM);
        monDisplayList.add(WEB_CONTAINER);
        monDisplayList.add(HTTP_SERVICE);
        monDisplayList.add(THREAD_POOL);
        monDisplayList.add(JDBC_CONNECTION_POOL);
        monDisplayList.add(CONNECTOR_CONNECTION_POOL);
        monDisplayList.add(EJB_CONTAINER);
        monDisplayList.add(TRANSACTION_SERVICE);
        monDisplayList.add(ORB);
        monDisplayList.add(CONNECTOR_SERVICE);
        monDisplayList.add(JMS_SERVICE);
        monDisplayList.add(WEB_SERVICES_CONTAINER);
        monDisplayList.add(JPA);
        monDisplayList.add(SECURITY);
        monDisplayList.add(JERSEY);
        monDisplayList.add(DEPLOYMENT);
    }
    final private static List monNamesList = new ArrayList();

    static {
        monNamesList.add("jvm");
        monNamesList.add("webContainer");
        monNamesList.add("httpService");
        monNamesList.add("threadPool");
        monNamesList.add("jdbcConnectionPool");
        monNamesList.add("connectorConnectionPool");
        monNamesList.add("ejbContainer");
        monNamesList.add("transactionService");
        monNamesList.add("orb");
        monNamesList.add("connectorService");
        monNamesList.add("jmsService");
        monNamesList.add("webServicesContainer");
        monNamesList.add("jpa");
        monNamesList.add("security");
        monNamesList.add("jersey");
        monNamesList.add("deployment");
    }
    final private static List containerDispList = new ArrayList();

    final private static List containerNameList = new ArrayList();

}
