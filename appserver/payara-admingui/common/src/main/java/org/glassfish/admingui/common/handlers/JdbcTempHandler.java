/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2011 Oracle and/or its affiliates. All rights reserved.
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

/*
 * JdbcHandler.java
 *
 * Created on August 10, 2006, 2:32 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
/**
 *
 * @author anilam
 */
package org.glassfish.admingui.common.handlers;

import com.sun.jsftemplating.annotation.Handler;
import com.sun.jsftemplating.annotation.HandlerInput;
import com.sun.jsftemplating.annotation.HandlerOutput;
import com.sun.jsftemplating.layout.descriptors.handler.HandlerContext;

import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;


import org.glassfish.admingui.common.util.GuiUtil;
import org.glassfish.admingui.common.util.RestUtil;

public class JdbcTempHandler {

    /** Creates a new instance of JdbcHandler */
    public JdbcTempHandler() {
    }

    /**
     *	<p> This handler gets the default values and resource type and puts them in session
     */
    @Handler(id = "setJDBCPoolWizard",
    input = {
        @HandlerInput(name = "fromStep2", type = Boolean.class),
        @HandlerInput(name = "attrMap", type = Map.class)},
    output = {
        @HandlerOutput(name = "ResTypeList", type = java.util.List.class),
        @HandlerOutput(name = "DBVendorList", type = java.util.List.class)
    })
    public static void setJDBCPoolWizard(HandlerContext handlerCtx) {
        //We need to use 2 maps for JDBC Connection Pool creation because there are extra info we need to keep track in
        //the wizard, but cannot be passed to the creation API.

        Boolean fromStep2 = (Boolean) handlerCtx.getInputValue("fromStep2");
        if ((fromStep2 != null) && fromStep2) {
            //wizardPool is already in session map
        } else {
            Map attrMap = (Map) handlerCtx.getInputValue("attrMap");
            Map sessionMap = handlerCtx.getFacesContext().getExternalContext().getSessionMap();
            sessionMap.put("wizardMap", attrMap);
            sessionMap.put("wizardPoolExtra", new HashMap());
            //sessionMap.put("wizardPoolProperties", new HashMap());
        }
        handlerCtx.setOutputValue("ResTypeList", resTypeList);
        handlerCtx.setOutputValue("DBVendorList", dbVendorList);
    }

    /**
     *	<p> This handler gets the datasource classname and properties and sets them in session
     */
    @Handler(id = "gf.updateJDBCPoolWizardStep1")
    public static void updateJDBCPoolWizardStep1(HandlerContext handlerCtx) {
        //Map pool = (Map) handlerCtx.getFacesContext().getExternalContext().getSessionMap().get("wizardPool");
        Map extra = (Map) handlerCtx.getFacesContext().getExternalContext().getSessionMap().get("wizardPoolExtra");

        String resType = (String) extra.get("resType");
        String dbVendorBox = (String) extra.get("DBVendorBox");
        String dbVendorField = (String) extra.get("DBVendorField");
        String introspect = (String) extra.get("introspect");
        if (introspect == null || introspect.isEmpty()) {
            introspect = Boolean.toString(false);
        }

        String dbVendor = (GuiUtil.isEmpty(dbVendorField)) ? dbVendorBox : dbVendorField;

        extra.put("DBVendor", dbVendor);
        String previousResType = (String) extra.get("PreviousResType");
        String previousDB = (String) extra.get("PreviousDB");
        String previousInstrospect = (String) extra.get("PreviousIntrospect");

        if (resType.equals(previousResType) && dbVendor.equals(previousDB) && introspect.equals(previousInstrospect)) {
            //&& !GuiUtil.isEmpty((String) extra.get("DatasourceClassname"))) {
            //User didn't change type and DB, keep the datasource classname as the same.
        } else {

            if (!GuiUtil.isEmpty(resType) && !GuiUtil.isEmpty(dbVendor)) {
                try {
                    List dsl = getJdbcDriverClassNames(dbVendor, resType, Boolean.valueOf(introspect));
                    if (guiLogger.isLoggable(Level.FINE)) {
                        guiLogger.fine("======= getJdbcDriverClassNames(" + dbVendor + ", " + resType + ")");
                        guiLogger.fine("=======  # of items for JDBC_DRIVER_CLASS_NAMES_KEY  " + dsl.size());
                        for (int i = 0; i < dsl.size(); i++) {
                            guiLogger.fine("classname[" + i + "] : " + dsl.get(i));
                        }
                    }

                    List<Map<String, String>> noprops = new ArrayList<Map<String, String>>();
                    String dslName = (dsl != null && (dsl.size() > 0)) ? (String) dsl.get(0) : "";
                    if (resType.equals(DRIVER)) {
                        extra.put("DList", dsl);
                        extra.put("DSList", "");
                        extra.put("DatasourceClassnameField", "");
                        extra.put("dsClassname", Boolean.FALSE);
                        extra.put("driverClassname", dslName);
                    } else {
                        extra.put("DSList", dsl);
                        extra.put("DList", "");
                        extra.put("DriverClassnameField", "");
                        extra.put("dsClassname", Boolean.TRUE);
                        extra.put("datasourceClassname", dslName);
                    }
                    if (guiLogger.isLoggable(Level.FINE)) {
                        guiLogger.fine("===== getConnectionDefinitionPropertiesAndDefaults(\"" + dslName + "\"," + resType + ")");
                    }
                    Map<String, String> props = getConnectionDefinitionPropertiesAndDefaults(dslName, resType);
                    if (props.size() > 0) {
                        if (guiLogger.isLoggable(Level.FINE)) {
                            guiLogger.fine("=======  getConnectionDefinitionPropertiesAndDefaults returns # of properties: " + props.size());
                        }
                        handlerCtx.getFacesContext().getExternalContext().getSessionMap().put("wizardPoolProperties", GuiUtil.convertMapToListOfMap(props));
                    } else {
                        if (guiLogger.isLoggable(Level.FINE)) {
                            guiLogger.fine("======= getConnectionDefinitionPropertiesAndDefaults returns NULL");
                        }
                        handlerCtx.getFacesContext().getExternalContext().getSessionMap().put("wizardPoolProperties", noprops);
                    }
                } catch (Exception ex) {
                    GuiUtil.getLogger().info(GuiUtil.getCommonMessage("log.error.updateJDBCPoolWizardStep1" + ex.getLocalizedMessage()));
                    if (GuiUtil.getLogger().isLoggable(Level.FINE)){
                        ex.printStackTrace();
                    }
                }
            } else {
                // Allow user to provide DataSource ClassName when resourceType is not of type Driver
                // or is not selected.
                if (DRIVER.equals(resType)) {
                    extra.put("DatasourceClassnameField", "");
                    extra.put("dsClassname", Boolean.FALSE);
                } else {
                    extra.put("DatasourceClassnameField", "");
                    extra.put("dsClassname", Boolean.TRUE);
                }
            }

            extra.put("PreviousResType", resType);
            extra.put("PreviousDB", dbVendor);
            extra.put("PreviousIntrospect", introspect);

        }
    }

    /**
     *	<p> updates the wizard map properties on step 2
     */
    @Handler(id = "gf.updateJdbcConnectionPoolPropertiesTable")
    public static void updateJdbcConnectionPoolPropertiesTable(HandlerContext handlerCtx) {
        Map extra = (Map) handlerCtx.getFacesContext().getExternalContext().getSessionMap().get("wizardPoolExtra");
        String resType = (String) extra.get("resType");
        String classname = (String) extra.get("datasourceClassname");
        List<Map<String, String>> noprops = new ArrayList<Map<String, String>>();
        if (guiLogger.isLoggable(Level.FINE)) {
            guiLogger.fine("===== getConnectionDefinitionPropertiesAndDefaults(\"" + classname + "\"," + resType + ")");
        }
        Map<String, String> props = getConnectionDefinitionPropertiesAndDefaults(classname, resType);
        if (props.size() != 0) {
            if (guiLogger.isLoggable(Level.FINE)) {
                guiLogger.fine("=======  getConnectionDefinitionPropertiesAndDefaults returns # of properties: " + props.size());
            }
            handlerCtx.getFacesContext().getExternalContext().getSessionMap().put("wizardPoolProperties", GuiUtil.convertMapToListOfMap(props));
        } else {
            if (guiLogger.isLoggable(Level.FINE)) {
                guiLogger.fine("======= getConnectionDefinitionPropertiesAndDefaults returns NULL");
            }
            handlerCtx.getFacesContext().getExternalContext().getSessionMap().put("wizardPoolProperties", noprops);
        }
    }

    /**
     *	<p> updates the wizard map properties on step 2
     */
    @Handler(id = "updateJdbcConnectionPoolWizardStep2")
    public static void updateJdbcConnectionPoolWizardStep2(HandlerContext handlerCtx) {
        Map extra = (Map) handlerCtx.getFacesContext().getExternalContext().getSessionMap().get("wizardPoolExtra");
        Map attrs = (Map) handlerCtx.getFacesContext().getExternalContext().getSessionMap().get("wizardMap");

        String resType = (String) extra.get("resType");
        String classname = (String) extra.get("datasourceClassname");
        String driver = (String) extra.get("driverClassname");
        String name = (String) extra.get("name");
        String classnamefield = (String) extra.get("DatasourceClassnameField");
        String driverfield = (String) extra.get("DriverClassnameField");
        attrs.put("name", name);
        attrs.put("resType", resType);
        if ("".equals(attrs.get("transactionIsolationLevel"))) {
            attrs.remove("transactionIsolationLevel");
        }
        if (!GuiUtil.isEmpty(classnamefield) || !GuiUtil.isEmpty(driverfield)) {
            attrs.put("datasourceClassname", classnamefield);
            attrs.put("driverClassname", driverfield);
        } else if (!GuiUtil.isEmpty(classname) || !GuiUtil.isEmpty(driver)) {
            attrs.put("datasourceClassname", classname);
            attrs.put("driverClassname", driver);
        } else {
            GuiUtil.handleError(handlerCtx, GuiUtil.getMessage("org.glassfish.jdbc.admingui.Strings", "msg.Error.classNameCannotBeEmpty"));
            return;
        }

    }

    /**
     *	<p> This handler adds the class name table column depends on the resource type.
     */
    @Handler(id = "gf.addClassNameColumn",
    input = {
        @HandlerInput(name = "poolsData", type = List.class)},
    output = {
        @HandlerOutput(name = "result", type = java.util.List.class)
    })
    public static void addClassNameColumn(HandlerContext handlerCtx) {
        List<Map<String, String>> poolsData = (List<Map<String, String>>) handlerCtx.getInputValue("poolsData");
        if (poolsData != null) {
            for (Map<String, String> poolData : poolsData) {
                String resType = poolData.get("resType");
                String driverClassName = poolData.get("driverClassname");
                String datasourceClassName = poolData.get("datasourceClassname");
                if (!resType.isEmpty()) {
                    if (resType.equals("java.sql.Driver")) {
                        poolData.put("className", driverClassName);
                    } else {
                        poolData.put("className", datasourceClassName);
                    }
                } else {
                    if (!datasourceClassName.isEmpty()) {
                        poolData.put("className", datasourceClassName);
                    }
                    if (!driverClassName.isEmpty()) {
                        poolData.put("className", driverClassName);
                    }
                }
            }
        }
        handlerCtx.setOutputValue("result", poolsData);
    }

    private static List getJdbcDriverClassNames(String dbVendor, String resType, boolean introspect) {
        String endpoint = (String) GuiUtil.getSessionValue("REST_URL");
        endpoint = endpoint + "/resources/get-jdbc-driver-class-names";
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put("dbVendor", dbVendor);
        attrs.put("restype", resType);
        attrs.put("introspect", ((Boolean) introspect).toString());
        List<String> jdbcClassNames = new ArrayList<String>();
        try {
            Map<String, Object> responseMap = RestUtil.restRequest(endpoint, attrs, "GET", null, false);
            Map<String, Object> extraPropsMap = (Map<String, Object>) ((Map<String, Object>) responseMap.get("data")).get("extraProperties");
            if ( extraPropsMap != null) {
                jdbcClassNames = (List<String>) extraPropsMap.get("driverClassNames");
            }            
        } catch (Exception ex) {
            GuiUtil.getLogger().severe("Error in getJdbcDriverClassNames ; \nendpoint = " + endpoint + "attrs=" + attrs + "method=GET");
            //we don't need to call GuiUtil.handleError() because thats taken care of in restRequest() when we pass in the handler.
        }
        return jdbcClassNames;
    }

    private static List getDatabaseVendorNames() {
        String endpoint = (String) GuiUtil.getSessionValue("REST_URL");
        endpoint = endpoint + "/resources/get-database-vendor-names";
        List<String> vendorList = new ArrayList<String>();
        try {
            Map<String, Object> responseMap = RestUtil.restRequest(endpoint, null, "GET", null, false);
            Map<String, Object> extraPropsMap = (Map<String, Object>) ((Map<String, Object>) responseMap.get("data")).get("extraProperties");
            if ( extraPropsMap != null) {
                vendorList = (List<String>) extraPropsMap.get("vendorNames");
            }            
        } catch (Exception ex) {
            GuiUtil.getLogger().severe("Error in getDatabaseVendorNames ; \nendpoint = " + endpoint + "attrs=null method=GET");
            //we don't need to call GuiUtil.handleError() because thats taken care of in restRequest() when we pass in the handler.
        }
        return vendorList;
    }

    private static Map<String, String> getConnectionDefinitionPropertiesAndDefaults(String datasourceClassName, String resType) {
        String endpoint = (String) GuiUtil.getSessionValue("REST_URL");
        endpoint = endpoint + "/resources/get-connection-definition-properties-and-defaults";
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put("connectionDefinitionClass", datasourceClassName);
        attrs.put("restype", resType);
        Map<String, String> connDefProps = new HashMap<String, String>();
        try {
            Map<String, Object> responseMap = RestUtil.restRequest(endpoint, attrs, "GET", null, false);
            Map<String, Object> extraPropsMap = (Map<String, Object>) ((Map<String, Object>) responseMap.get("data")).get("extraProperties");
            if ( extraPropsMap != null) {
                connDefProps = (Map<String, String>) extraPropsMap.get("connectionDefinitionPropertiesAndDefaults");
            }
        } catch (Exception ex) {
            GuiUtil.getLogger().severe("Error in getConnectionDefinitionPropertiesAndDefaults ; \nendpoint = " + endpoint + "attrs=" + attrs + "method=GET");
            //we don't need to call GuiUtil.handleError() because thats taken care of in restRequest() when we pass in the handler.
        }
        return connDefProps;
    }
    public static final Logger guiLogger = GuiUtil.getLogger();
    public static final String REASON_FAILED_KEY = "ReasonFailedKey";
    //public static final  String SET_KEY = "SetKey";
    //public static final  String BOOLEAN_KEY = "BooleanKey";
    static private final String DATA_SOURCE = "javax.sql.DataSource";
    static private final String XADATA_SOURCE = "javax.sql.XADataSource";
    static private final String CCDATA_SOURCE = "javax.sql.ConnectionPoolDataSource";
    static private final String DRIVER = "java.sql.Driver";
    static private final String JAVADB = "JavaDB";
    static private final String ORACLE = "Oracle";
    static private final String DERBY = "Derby";
    static private final String SYBASE = "Sybase";
    static private final String DB2 = "DB2";
    static private final String POINTBASE = "PointBase";
    static private final String POSTGRESQL = "PostgreSQL";
    static private final String INFORMIX = "Informix";
    static private final String CLOUDSCAPE = "Cloudscape";
    static private final String MSSQL = "Microsoft SQL Server";
    static private final String MYSQL = "MySQL";
    static private List resTypeList = new ArrayList();
    static private List dbVendorList = new ArrayList();

    static {
        dbVendorList = getDatabaseVendorNames();
        dbVendorList.add(0, "");
        resTypeList.add("");
        resTypeList.add(DATA_SOURCE);
        resTypeList.add(XADATA_SOURCE);
        resTypeList.add(CCDATA_SOURCE);
        resTypeList.add(DRIVER);

//        dbVendorList.add("");
//        dbVendorList.add(JAVADB);
//        dbVendorList.add(ORACLE);
//        dbVendorList.add(DERBY);
//        dbVendorList.add(SYBASE);
//        dbVendorList.add(DB2);
//        dbVendorList.add(POINTBASE);
//        dbVendorList.add(POSTGRESQL);
//        dbVendorList.add(INFORMIX);
//        dbVendorList.add(CLOUDSCAPE);
//        dbVendorList.add(MSSQL);
//        dbVendorList.add(MYSQL);
//        Collections.sort(dbVendorList);
    }
}
