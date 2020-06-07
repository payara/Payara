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
// Portions Copyright [2018-2019] [Payara Foundation and/or its affiliates]

package org.glassfish.admingui.common.handlers;

import com.sun.enterprise.universal.xml.MiniXmlParser.JvmOption;
import com.sun.jsftemplating.annotation.Handler;  
import com.sun.jsftemplating.annotation.HandlerInput; 
import com.sun.jsftemplating.annotation.HandlerOutput;
import com.sun.jsftemplating.layout.descriptors.handler.HandlerContext;  
import java.net.URLEncoder;
import java.util.*;
import java.util.logging.Level;
import static org.glassfish.admingui.common.handlers.InstanceHandler.JVM_OPTION;
import static org.glassfish.admingui.common.handlers.InstanceHandler.MAX_VERSION;
import static org.glassfish.admingui.common.handlers.InstanceHandler.MIN_VERSION;
import static org.glassfish.admingui.common.handlers.InstanceHandler.TARGET;

import org.glassfish.admingui.common.util.GuiUtil;
import org.glassfish.admingui.common.util.RestResponse;
import org.glassfish.admingui.common.util.RestUtil;


/**
 *
 * @author anilam
 */
public class SecurityHandler {
    

    /**
     *	<p> This handler returns the a Map for storing the attributes for realm creation.
     *	@param	handlerCtx	The HandlerContext.
     */
    @Handler(id="getRealmAttrForCreate",
    output={
        @HandlerOutput(name="attrMap",      type=Map.class),
        @HandlerOutput(name="classnameOption",      type=String.class),
        @HandlerOutput(name="realmClasses",      type=List.class),
        @HandlerOutput(name="properties", type=List.class)})
    public static void getRealmAttrForCreate(HandlerContext handlerCtx) {
        
        handlerCtx.setOutputValue("realmClasses", realmClassList);
        handlerCtx.setOutputValue("classnameOption", "predefine");
        Map attrMap = new HashMap();
        attrMap.put("fileJaax", "fileRealm");
        attrMap.put("ldapJaax", "ldapRealm" );
        attrMap.put("solarisJaax", "solarisRealm");
        attrMap.put("jdbcJaax", "jdbcRealm");
        attrMap.put("predefinedClassname", Boolean.TRUE);
        attrMap.put("createNew", true);
        handlerCtx.setOutputValue("attrMap", attrMap);
        handlerCtx.setOutputValue("properties", new ArrayList());
    }
    
    /**
     *	<p> This handler returns the a Map for storing the attributes for editing a realm.
     *  This can be used by either the node agent realm or the realm in configuration-Security-realm
     *	@param	handlerCtx	The HandlerContext.
     */
    @Handler(id="getRealmAttrForEdit",
    input={
        @HandlerInput(name="endpoint", type=String.class)},
    output={
        @HandlerOutput(name="attrMap",      type=Map.class),
        @HandlerOutput(name="classnameOption",      type=String.class),
        @HandlerOutput(name="realmClasses",      type=List.class),
        @HandlerOutput(name="properties", type=List.class)})

    public static void getRealmAttrForEdit(HandlerContext handlerCtx) {

        String endpoint = (String) handlerCtx.getInputValue("endpoint");

        HashMap<String, Object> realmMap = (HashMap<String, Object>) RestUtil.getEntityAttrs(endpoint, "entity");
        
        HashMap<String, Object> responseMap = (HashMap<String, Object>) RestUtil.restRequest(endpoint + "/property.json", null, "GET", null, false);
        HashMap propsMap = (HashMap) ((Map<String, Object>) responseMap.get("data")).get("extraProperties");
        ArrayList<HashMap> propList = (ArrayList<HashMap>) propsMap.get("properties");
        HashMap origProps = new HashMap();
        for (HashMap prop : propList) {
            origProps.put(prop.get("name"), prop.get("value"));
        }

        Map attrMap = new HashMap();
        attrMap.put("Name", (String) realmMap.get("name"));
        attrMap.put("fileJaax", "fileRealm");
        attrMap.put("ldapJaax", "ldapRealm" );
        attrMap.put("solarisJaax", "solarisRealm");
        attrMap.put("jdbcJaax", "jdbcRealm");

        String classname = (String) realmMap.get("classname");

        if (realmClassList.contains(classname)){
            handlerCtx.setOutputValue("classnameOption", "predefine");
            attrMap.put("predefinedClassname", Boolean.TRUE);
            attrMap.put("classname", classname);
            List props = getChildrenMapForTableList(propList, "property", skipRealmPropsList);
            handlerCtx.setOutputValue("properties", props);

            if(classname.indexOf("FileRealm")!= -1){
                attrMap.put("file", origProps.get("file"));
                attrMap.put("fileJaax", origProps.get("jaas-context"));
                attrMap.put("fileAsGroups", origProps.get("assign-groups"));
            }else
            if(classname.indexOf("LDAPRealm")!= -1){
                attrMap.put("ldapJaax", origProps.get("jaas-context"));
                attrMap.put("ldapAsGroups", origProps.get("assign-groups"));
                attrMap.put("directory", origProps.get("directory"));
                attrMap.put("baseDn", origProps.get("base-dn"));
            }else
            if(classname.indexOf("SolarisRealm")!= -1){
                attrMap.put("solarisJaax", origProps.get("jaas-context"));
                attrMap.put("solarisAsGroups", origProps.get("assign-groups"));
            }else
            if(classname.indexOf("PamRealm")!= -1){
                attrMap.put("pamJaax", origProps.get("jaas-context"));
            }else
            if(classname.indexOf("JDBCRealm")!= -1){
                attrMap.put("jdbcJaax", origProps.get("jaas-context"));
                attrMap.put("jdbcAsGroups", origProps.get("assign-groups"));
                attrMap.put("datasourceJndi", origProps.get("datasource-jndi"));
                attrMap.put("userTable", origProps.get("user-table"));
                attrMap.put("userNameColumn", origProps.get("user-name-column"));
                attrMap.put("passwordColumn", origProps.get("password-column"));
                attrMap.put("groupTable", origProps.get("group-table"));
                attrMap.put("groupTableUserName", origProps.get("group-table-user-name-column"));
                attrMap.put("groupNameColumn", origProps.get("group-name-column"));
                attrMap.put("dbUser", origProps.get("db-user"));
                attrMap.put("dbPassword", origProps.get("db-password"));
                attrMap.put("digestAlgorithm", origProps.get("digest-algorithm"));
                attrMap.put("pswdEncAlgorithm", origProps.get("digestrealm-password-enc-algorithm"));
                attrMap.put("encoding", origProps.get("encoding"));
                attrMap.put("charset", origProps.get("charset"));

           }else
            if(classname.indexOf("CertificateRealm")!= -1){
                attrMap.put("certAsGroups", origProps.get("assign-groups"));
            }
        }else{
            //Custom realm class
            handlerCtx.setOutputValue("classnameOption", "input");
            attrMap.put("predefinedClassname", Boolean.FALSE);
	    attrMap.put("classnameInput", classname);
            attrMap.put("classname", classname);
            List props = getChildrenMapForTableList(propList, "property", null);
            handlerCtx.setOutputValue("properties", props);
        }

        handlerCtx.setOutputValue("attrMap", attrMap);
        handlerCtx.setOutputValue("realmClasses", realmClassList);
    }

    public static List getChildrenMapForTableList(List<HashMap> propList, String childType, List skipList){
        boolean hasSkip = true;
        if (skipList == null ){
            hasSkip = false;
        }
        List result = new ArrayList();
        if (propList != null) {
            for(HashMap oneMap: propList){
                HashMap oneRow = new HashMap();
                String name = (String) oneMap.get("name");
                if (hasSkip && skipList.contains(name)){
                    continue;
                }
                oneRow.put("selected", false);
                oneRow.put("name", name);
                oneRow.put("value", oneMap.get("value"));
                oneRow.put("description", oneMap.get("description"));
                result.add(oneRow);
            }
        }
        return result;
    }

    public static List<HashMap> getListfromMap(HashMap<String, Object> props) {
        List<HashMap> result = new ArrayList();
        Iterator it = props.entrySet().iterator();
        while(it.hasNext()) {
            Map.Entry m =(Map.Entry)it.next();
            HashMap oneRow = new HashMap();
            oneRow.put("selected", false);
            oneRow.put("Name", m.getKey());
            oneRow.put("Value", m.getValue());
            oneRow.put("Description", "");
            result.add(oneRow);
        }
        return result;
    }
    
    @Handler(id="saveRealm",
    input={
        @HandlerInput(name="endpoint",   type=String.class),
        @HandlerInput(name="classnameOption",   type=String.class),
        @HandlerInput(name="attrMap",      type=Map.class),
        @HandlerInput(name="edit",      type=Boolean.class, required=true),
        @HandlerInput(name="contentType", type=String.class, required=false),
        @HandlerInput(name="propList", type=List.class)
    },
    output={
        @HandlerOutput(name="newPropList", type=List.class)
    })
    public static void saveRealm(HandlerContext handlerCtx) {
        String option = (String) handlerCtx.getInputValue("classnameOption");
        List<Map<String,String>> propListOrig = (List)handlerCtx.getInputValue("propList");
        PropertyList propList = PropertyList.fromList(propListOrig);
        Map<String,String> attrMap = (Map)handlerCtx.getInputValue("attrMap");
        Boolean edit = (Boolean) handlerCtx.getInputValue("edit");

        if (attrMap == null) {
            attrMap = new HashMap();
        }
        String classname;
        try{
          if(option.equals("predefine")){
            classname = attrMap.get("classname");

            if(classname.indexOf("FileRealm")!= -1){
                propList.put( "file", attrMap, "file");
                propList.put( "jaas-context", attrMap, "fileJaax");
                propList.put( "assign-groups", attrMap, "fileAsGroups");
            }else
            if(classname.indexOf("LDAPRealm")!= -1){
                propList.put( "jaas-context", attrMap, "ldapJaax");
                propList.put( "base-dn", attrMap, "baseDn");
                propList.put( "directory", attrMap, "directory");
                propList.put( "assign-groups", attrMap, "ldapAsGroups");
            }else
            if(classname.indexOf("SolarisRealm")!= -1){
                propList.put( "jaas-context", attrMap, "solarisJaax");
                propList.put( "assign-groups", attrMap, "solarisAsGroups");
            }else
            if(classname.indexOf("PamRealm")!= -1){
                propList.put( "jaas-context", attrMap, "pamJaax");
            }else
            if(classname.indexOf("JDBCRealm")!= -1){
                propList.put( "jaas-context", attrMap, "jdbcJaax");
                propList.put( "datasource-jndi", attrMap, "datasourceJndi");
                propList.put( "user-table", attrMap, "userTable");
                propList.put( "user-name-column", attrMap, "userNameColumn");
                propList.put( "password-column", attrMap, "passwordColumn");
                propList.put( "group-table", attrMap, "groupTable");
                propList.put( "group-table-user-name-column", attrMap, "groupTableUserName");
                propList.put( "group-name-column", attrMap, "groupNameColumn");
                propList.put( "db-user", attrMap, "dbUser");
                propList.put( "db-password", attrMap, "dbPassword");
                propList.put( "digest-algorithm", attrMap, "digestAlgorithm");
                propList.put( "digestrealm-password-enc-algorithm", attrMap, "pswdEncAlgorithm");
                propList.put( "encoding", attrMap, "encoding");
                propList.put( "charset", attrMap, "charset");
                propList.put( "assign-groups", attrMap, "jdbcAsGroups");
           }else {
               if(classname.indexOf("CertificateRealm")!= -1){
                   propList.put( "assign-groups", attrMap, "certAsGroups");
               }
           }
        } else {
           classname = attrMap.get("classnameInput");
        }

        //for edit case, only properties will be changed since we don't allow classname change.
        //return the prop list so it can continue processing in the .jsf
        if (edit){
            handlerCtx.setOutputValue("newPropList", propList.toList());
            return;
        }

        Map<String, Object> cMap = new HashMap();
        cMap.put("name", attrMap.get("Name"));
        cMap.put("classname", classname);
        cMap.put(TARGET, attrMap.get(TARGET));

        if (Boolean.parseBoolean(attrMap.get("registerLoginModule"))) {
            propList.put("jaas-context", attrMap, "loginModuleJaax");
            cMap.put("login-module", attrMap.get("loginModuleClass"));
        }

        cMap.put("property", propList.toParamValue());

        String endpoint = (String) handlerCtx.getInputValue("endpoint");
        endpoint = endpoint + "/auth-realm";
        RestUtil.restRequest(endpoint, cMap, "post", handlerCtx, false);
      }catch(Exception ex){
          GuiUtil.handleException(handlerCtx, ex);
      }
    }


    static public void putOptional(Map<String,String> attrMap, List propList, String propName, String key)
    {
        Map oneProp = new HashMap();
        oneProp.put("name", propName);
        String value = attrMap.get(key);
        if (GuiUtil.isEmpty(value))
            return;
        oneProp.put("value", attrMap.get(key));
        propList.add(oneProp);
    }


    /* Handler for Group/User managemenet */

    /**
     *	<p> This handler update's user info.</p>
     *  <p> Input value: "Realm" -- Type: <code>java.lang.String</code></p>
     *  <p> Output value: "UserId" -- Type: <code>java.lang.String</code></p>
     *  <p> Output value: "GroupList" -- Type: <code>java.lang.String</code></p>
     *  <p> Output value: "Password" -- Type: <code>java.lang.String</code></p>
     *	@param	handlerCtx	The HandlerContext.
     */
    @Handler(id="saveUser",
	input={
	    @HandlerInput(name="Realm", type=String.class, required=true),
            @HandlerInput(name="configName", type=String.class, required=true),
	    @HandlerInput(name="UserId", type=String.class, required=true),
	    @HandlerInput(name="GroupList", type=String.class, required=true),
	    @HandlerInput(name="Password", type=String.class, required=true),
	    @HandlerInput(name="CreateNew", type=String.class, required=true)})
    public static void saveUser(HandlerContext handlerCtx) {
        char[] password = null;
        try {
            String realmName = (String) handlerCtx.getInputValue("Realm");
            String configName = (String) handlerCtx.getInputValue("configName");
            String grouplist = (String)handlerCtx.getInputValue("GroupList");
            password = ((String)handlerCtx.getInputValue("Password")).toCharArray();
            String userid = (String)handlerCtx.getInputValue("UserId");
            String createNew = (String)handlerCtx.getInputValue("CreateNew");

            if (password == null) {
                password = "".toCharArray();
            }
            // before save user synchronize realm, for the case if keyfile is changed
            String tmpEP = GuiUtil.getSessionValue("REST_URL") + "/configs/config/"
                                                    + configName + "/synchronize-realm-from-config";
            HashMap attrs = new HashMap<>();
            attrs.put("id", configName);
            attrs.put("realmName", realmName);
            RestUtil.restRequest(tmpEP, attrs, "POST", handlerCtx, false);
            String endpoint = GuiUtil.getSessionValue("REST_URL") + "/configs/config/" + configName + "/security-service/auth-realm/" + realmName ;
            if (Boolean.valueOf(createNew)) {
                endpoint = endpoint +  "/create-user?target=" + configName;
            }else{
                endpoint = endpoint + "/update-user?target=" + configName;
            }

            attrs = new HashMap<>();
            attrs.put("id", userid);
            // Converting the password back to string as this is passed directly as payload in REST request
            attrs.put("userpassword", new String(password));
            attrs.put("target", configName);
            if (grouplist != null && grouplist.contains(",")) {
                grouplist = grouplist.replace(',', ':');
            }
            List<String> grpList = new ArrayList();
            if (grouplist != null) {
                grpList.add(grouplist);
            }
            attrs.put("groups", grpList);
            RestUtil.restRequest(endpoint, attrs, "POST", null, true, true );
        } catch(Exception ex) {
            GuiUtil.handleException(handlerCtx, ex);
        }
        finally {
            if (password != null) {
                Arrays.fill(password, ' ');
            }
        }
    }

   /**
     *	<p> This handler returns the attribute values in the
     *      Edit Manage User Password Page.</p>
     *  <p> Input value: "Realm" -- Type: <code>java.lang.String</code></p>
     *  <p> Output value: "UserId" -- Type: <code>java.lang.String</code></p>
     *  <p> Output value: "GroupList" -- Type: <code>java.lang.String</code></p>
     *	@param	handlerCtx	The HandlerContext.
     */
    @Handler(id="getUserInfo",
    input={
        @HandlerInput(name="Realm", type=String.class, required=true),
        @HandlerInput(name="configName", type=String.class, required=true),
        @HandlerInput(name="User", type=String.class, required=true)},
    output={
        @HandlerOutput(name="GroupList",     type=String.class)})

        public static void getUserInfo(HandlerContext handlerCtx) {

        String realmName = (String) handlerCtx.getInputValue("Realm");
        String userName = (String) handlerCtx.getInputValue("User");
        String configName = (String) handlerCtx.getInputValue("configName");
        handlerCtx.setOutputValue("GroupList", getGroupNames(realmName, userName, configName, handlerCtx)  );
    }

   /**
     *	<p> This handler returns the list of file users for specified realm.
     *	@param	handlerCtx	The HandlerContext.
     */
    @Handler(id="getFileUsers",
        input={
            @HandlerInput(name="Realm", type=String.class, required=true),
            @HandlerInput(name="configName", type=String.class, required=true)},
        output={
            @HandlerOutput(name="result", type=java.util.List.class)}
     )
     public static void getFileUsers(HandlerContext handlerCtx){
        String realmName = (String) handlerCtx.getInputValue("Realm");
        String configName = (String) handlerCtx.getInputValue("configName");
        List result = new ArrayList();
        try{
            String endpoint = GuiUtil.getSessionValue("REST_URL") + "/configs/config/" + configName +
                                                                "/security-service/auth-realm/" + realmName + "/list-users.json?target=" + configName;
            Map<String, Object> responseMap = RestUtil.restRequest(endpoint, null, "get", handlerCtx, false);
            responseMap = (Map<String, Object>) responseMap.get("data");
            List<HashMap> children = (List<HashMap>) responseMap.get("children");
            if(children != null) {
                Map<String, Object> map = null;
                for (HashMap child : children) {
                    map = new HashMap<String, Object>();
                    String name = (String) child.get("message");
                    map.put("users", name);
                    map.put("groups", getGroupNames( realmName, name, configName, handlerCtx));
                    map.put("selected", false);
                    result.add(map);
                }
            }
        }catch(Exception ex){
            GuiUtil.handleException(handlerCtx, ex);
        }
        handlerCtx.setOutputValue("result", result);
    }


  /**
     *	<p> This handler removes users for specified realm.
     *	@param	handlerCtx	The HandlerContext.
     */
    @Handler(id="removeUser",
        input={
            @HandlerInput(name="Realm", type=String.class, required=true),
            @HandlerInput(name="configName", type=String.class, required=true),
            @HandlerInput(name="selectedRows", type=List.class, required=true)},
        output={
            @HandlerOutput(name="result", type=java.util.List.class)}
     )
     public static void removeUser(HandlerContext handlerCtx){

        String error = null;
        String realmName = (String) handlerCtx.getInputValue("Realm");
        String configName = (String) handlerCtx.getInputValue("configName");
        try{
            List obj = (List) handlerCtx.getInputValue("selectedRows");
            List<Map> selectedRows = (List) obj;
            for(Map oneRow : selectedRows){
                String user = (String)oneRow.get("name");
                String endpoint = GuiUtil.getSessionValue("REST_URL") + "/configs/config/" + configName + "/admin-service/jmx-connector/system.json";
                Map<String, Object> responseMap = RestUtil.restRequest(endpoint, null, "get", handlerCtx, false);
                Map<String, Object> valueMap = (Map<String, Object>) responseMap.get("data");
                valueMap = (Map<String, Object>) ((Map<String, Object>) valueMap.get("extraProperties")).get("entity");
                String authRealm = (String) valueMap.get("authRealmName");
                if (realmName.equals(authRealm) && user.equals(GuiUtil.getSessionValue("userName"))){
                    error = GuiUtil.getMessage(COMMON_BUNDLE, "msg.error.cannotDeleteCurrent");
                    continue;
                }else{
                    HashMap attrs = new HashMap<String, Object>();
                    endpoint = GuiUtil.getSessionValue("REST_URL") + "/configs/config/" + configName +
                                                                "/security-service/auth-realm/" + realmName + "/delete-user?target=" + configName;
                    attrs.put("name", user);
                    RestResponse response = RestUtil.delete(endpoint, attrs);
                    if (!response.isSuccess()) {
                        GuiUtil.getLogger().severe("Remove user failed.  parent=" + endpoint + "; attrs =" + attrs);
                        error = GuiUtil.getMessage("msg.error.checkLog");
                    }
                }
            }
            if (error != null){
                GuiUtil.prepareAlert("error", error, null);
            }
        }catch(Exception ex){
            GuiUtil.handleException(handlerCtx, ex);
        }
    }


    private static String getGroupNames(String realmName, String userName, String configName, HandlerContext handlerCtx){
        try{
            String endpoint = GuiUtil.getSessionValue("REST_URL") + "/configs/config/" + configName +
                                                                "/security-service/auth-realm/" + realmName + "/list-group-names?username=" + userName + "&target=" + configName;
            Map<String, Object> responseMap = RestUtil.restRequest(endpoint, null, "get", handlerCtx, false);
            HashMap children = (HashMap)((Map<String, Object>) responseMap.get("data")).get("extraProperties");
            List<String> groupList= (List<String>)children.get("groups");
            StringBuilder groups = new StringBuilder();
            String sepHolder = "";
            if (groupList != null){
                for(String oneGroup : groupList){
                    groups.append(sepHolder).append(oneGroup);
                    sepHolder=":";
                }
            }
            return groups.toString();
        }catch(Exception ex){
            GuiUtil.getLogger().info(GuiUtil.getCommonMessage("log.error.getGroupNames") + ex.getLocalizedMessage());
            if (GuiUtil.getLogger().isLoggable(Level.FINE)){
                ex.printStackTrace();
            }
            return "";
        }
    }


    private static List skipRealmPropsList = new ArrayList();
    private static List realmClassList = new ArrayList();
    static {
        String endpoint = GuiUtil.getSessionValue("REST_URL") 
            + "/list-predefined-authrealm-classnames";
            //+ "/configs/config/server-config/security-service/auth-realm/list-predefined-authrealm-classnames";
        Map<String, Object> responseMap = RestUtil.restRequest(endpoint, null, "GET", null, false);
        Map<String, Object> valueMap = (Map<String, Object>) responseMap.get("data");
        ArrayList<HashMap> classNames = (ArrayList<HashMap>) ((ArrayList<HashMap>) valueMap.get("children"));
        for (HashMap className : classNames) {
            realmClassList.add(className.get("message"));
        }
        skipRealmPropsList.add("jaas-context");
        skipRealmPropsList.add("file");
        skipRealmPropsList.add("assign-groups");
        skipRealmPropsList.add("base-dn");
        skipRealmPropsList.add("directory");
        skipRealmPropsList.add("datasource-jndi");
        skipRealmPropsList.add("user-table");
        skipRealmPropsList.add("user-name-column");
        skipRealmPropsList.add("password-column");
        skipRealmPropsList.add("group-table");
        skipRealmPropsList.add("group-name-column");
        skipRealmPropsList.add("group-table-user-name-column");
        skipRealmPropsList.add("db-user");
        skipRealmPropsList.add("db-password");
        skipRealmPropsList.add("digest-algorithm");
        skipRealmPropsList.add("digestrealm-password-enc-algorithm");
        skipRealmPropsList.add("encoding");
        skipRealmPropsList.add("charset");
    }

    private static final String PROPERTY_NAME = "Name";
    private static final String PROPERTY_VALUE = "Value";
    private static final String COMMON_BUNDLE = "org.glassfish.common.admingui.Strings";



    @Handler(id="addDefaultProviderInfo",
        input={
            @HandlerInput(name="providerList", type=List.class, required=true),
            @HandlerInput(name="configName", type=String.class, required=true),
            @HandlerInput(name="msgSecurityName", type=String.class, required=true)
    })
    public static void addDefaultProviderInfo(HandlerContext handlerCtx){
        List<HashMap> providerList = (ArrayList<HashMap>) handlerCtx.getInputValue("providerList");
        String configName = (String) handlerCtx.getInputValue("configName");
        String msgSecurityName = (String) handlerCtx.getInputValue("msgSecurityName");

        String endpoint = GuiUtil.getSessionValue("REST_URL") + "/configs/config/" + configName
                                        + "/security-service/message-security-config/" + msgSecurityName;
        Map<String, Object> valueMap = (Map<String, Object>) RestUtil.getEntityAttrs(endpoint, "entity");
        String defaultProvider = (String) valueMap.get("defaultProvider");
        String defaultClientProvider = (String) valueMap.get("defaultClientProvider");
        String trueStr = GuiUtil.getMessage("common.true");
        String falseStr = GuiUtil.getMessage("common.false");
        for(Map oneRow : providerList){
            if ( oneRow.get("name").equals(defaultProvider) || oneRow.get("name").equals(defaultClientProvider)){
                oneRow.put("default", trueStr);
            }else{
                oneRow.put("default", falseStr);
            }
        }
    }


    @Handler(id="getMessageSecurityAuthLayersForCreate",
        input={
            @HandlerInput(name="attrMap", type=Map.class, required=true),
            @HandlerInput(name="configName", type=String.class, required=true),
            @HandlerInput(name="propList", type=List.class, required=true)},
        output={
            @HandlerOutput(name="layers", type=List.class)}
        )
    public static void getMessageSecurityAuthLayersForCreate(HandlerContext handlerCtx) throws Exception {
        List layers = new ArrayList();
        String configName = (String) handlerCtx.getInputValue("configName");
        layers.add("SOAP");
        layers.add("HttpServlet");
        String endpoint = GuiUtil.getSessionValue("REST_URL") + "/configs/config/" + configName + "/security-service/message-security-config";
        Set<String> msgSecurityCfgs = (Set<String>) (RestUtil.getChildMap(endpoint)).keySet();
        for(String name : msgSecurityCfgs){
            if (layers.contains(name)) {
                layers.remove(name);
            }
        }
        handlerCtx.setOutputValue("layers", layers);
    }


    @Handler(id="getProvidersByType",
        input={
            @HandlerInput(name="msgSecurityName", type=String.class, required=true),
            @HandlerInput(name="configName", type=String.class, required=true),
            @HandlerInput(name="type", type=List.class, required=true)},
        output={
            @HandlerOutput(name="result", type=List.class)})
     public static void getProvidersByType(HandlerContext handlerCtx) throws Exception {
        List type = (List) handlerCtx.getInputValue("type");
        List result = new ArrayList();
        String configName = (String) handlerCtx.getInputValue("configName");
        String msgSecurityName = (String) handlerCtx.getInputValue("msgSecurityName");
        String endpoint = GuiUtil.getSessionValue("REST_URL") + "/configs/config/" + configName +
                                "/security-service/message-security-config/" + msgSecurityName + "/provider-config";
        List<String> providers = (List<String>) RestUtil.getChildList(endpoint);
        for(String providerEndpoint : providers){
            Map providerAttrs = (HashMap) RestUtil.getAttributesMap(providerEndpoint);
            String providerType = (String) providerAttrs.get("providerType");
            if (type.contains(providerType)) {
                result.add(com.sun.jsftemplating.util.Util.htmlEscape((String)providerAttrs.get("providerId")));
            }
        }
        result.add(0, "");
        handlerCtx.setOutputValue("result", result);
    }

    @Handler(id="saveMsgProviderInfo",
         input={
            @HandlerInput(name="attrMap", type=Map.class, required=true),
            @HandlerInput(name="configName", type=String.class, required=true),
            @HandlerInput(name="edit", type=String.class, required=true)
     } )
     public static void saveMsgProviderInfo(HandlerContext handlerCtx){
        Map<String,String> attrMap = (Map<String,String>) handlerCtx.getInputValue("attrMap");
        String edit = (String)handlerCtx.getInputValue("edit");
        String msgSecurityName = attrMap.get("msgSecurityName");
        String configName = (String)handlerCtx.getInputValue("configName");
        
        try{
            String providerName = URLEncoder.encode((String)attrMap.get("Name"), "UTF-8");
            String providerEndpoint = GuiUtil.getSessionValue("REST_URL") + "/configs/config/" + configName +
                    "/security-service/message-security-config/" + msgSecurityName + "/provider-config/" + providerName;
        
            if (edit.equals("true")){
                boolean providerExist = RestUtil.get(providerEndpoint).isSuccess();
                if (!providerExist){
                    GuiUtil.handleError(handlerCtx, GuiUtil.getMessage(COMMON_BUNDLE, "msg.error.noSuchProvider")); //normally won't happen.
                    return;
                }else{
                    Map<String, Object> providerMap = (Map<String, Object>)RestUtil.getEntityAttrs(providerEndpoint, "entity");
                    providerMap.put("className", attrMap.get("ClassName"));
                    providerMap.put("providerType", attrMap.get("ProviderType"));
                    RestUtil.restRequest(providerEndpoint, providerMap, "POST", null, false);
                    Map attrs = new HashMap();
                    String endpoint = GuiUtil.getSessionValue("REST_URL") + "/configs/config/" + configName +
                                    "/security-service/message-security-config/" + attrMap.get("msgSecurityName");
                    attrs.put("authLayer", attrMap.get("msgSecurityName"));
                    if (attrMap.get("defaultProvider") != null && attrMap.get("defaultProvider").equals("true")){
                        if (providerMap.get("providerType").equals("client")) {
                            attrs.put("defaultClientProvider", providerName);
                        }
                        else if (providerMap.get("providerType").equals("server")) {
                            attrs.put("defaultProvider", providerName);
                        }
                        else if (providerMap.get("providerType").equals("client-server")) {
                            attrs.put("defaultProvider", providerName);
                            attrs.put("defaultClientProvider", providerName);
                        }
                    }
                    if (attrMap.get("defaultProvider") == null){
                        if ( providerMap.get("providerType").equals("client") && providerName.equals(attrMap.get("defaultClientProvider"))) {
                            attrs.put("defaultClientProvider", "");
                        }
                        else if (providerMap.get("providerType").equals("server") && providerName.equals(attrMap.get("defaultProvider"))) {
                            attrs.put("defaultProvider", "");
                        }
                        else if (providerMap.get("providerType").equals("client-server")) {
                            if ( providerName.equals(attrMap.get("defaultServerProvider")) && providerName.equals(attrMap.get("defaultClientProvider"))) {
                                attrs.put("defaultProvider", "");
                                attrs.put("defaultClientProvider", "");
                            }
                        }
                    }
                    RestUtil.sendUpdateRequest(endpoint, attrs, null, null, null);
                }
            }else{
                String endpoint = GuiUtil.getSessionValue("REST_URL") + "/configs/config/" + configName +
                                    "/security-service/message-security-config";
                Map attrs = new HashMap();
                if (attrMap.get("defaultProvider") == null){
                    attrMap.put("defaultProvider", "false");
                }
                attrs.put("isdefaultprovider", attrMap.get("defaultProvider"));
                attrs.put("id", attrMap.get("Name"));
                attrs.put("classname", attrMap.get("ClassName"));
                attrs.put("providertype", attrMap.get("ProviderType"));
                attrs.put("layer", attrMap.get("msgSecurityName"));
                attrs.put("target", configName);
                RestUtil.restRequest(endpoint, attrs, "POST", null, false);
            }

            //if we pass in "", backend will throw bean violation, since it only accepts certain values.
            String[] attrList= new String[] {"Request-AuthSource","Request-AuthRecipient", "Response-AuthSource", "Response-AuthRecipient"};
            for(int i=0; i< attrList.length; i++){
                if ("".equals(attrMap.get(attrList[i]))){
                    attrMap.put( attrList[i], null);
                }
            }

            Map reqPolicyMap = new HashMap();
            reqPolicyMap.put("authSource", attrMap.get("Request-AuthSource"));
            reqPolicyMap.put("authRecipient", attrMap.get("Request-AuthRecipient"));
            String reqPolicyEP = providerEndpoint + "/request-policy";
            RestUtil.restRequest(reqPolicyEP, reqPolicyMap, "POST", null, false);

            Map respPolicyMap = new HashMap();
            respPolicyMap.put("authSource", attrMap.get("Response-AuthSource"));
            respPolicyMap.put("authRecipient", attrMap.get("Response-AuthRecipient"));
            String respPolicyEP = providerEndpoint + "/response-policy";
            RestUtil.restRequest(respPolicyEP, respPolicyMap, "POST", null, false);
        }catch(Exception ex){
            GuiUtil.handleException(handlerCtx, ex);
        }
    }


    @Handler(id="saveSecurityManagerValue",
         input={
            @HandlerInput(name="configName", type=String.class),
            @HandlerInput(name="value", type=String.class, required=true)
     })
     public static void saveSecurityManagerValue(HandlerContext handlerCtx){
        try {
            String configName = (String) handlerCtx.getInputValue("configName");
            if (GuiUtil.isEmpty(configName))
                configName = "server-config";
            String endpoint = GuiUtil.getSessionValue("REST_URL") +
                    "/configs/config/" + configName + "/java-config/jvm-options.json";
            ArrayList<Map<String, String>> list;
            Map result = (HashMap) RestUtil.restRequest(endpoint, null, "GET", null, false).get("data");
            list = (ArrayList<Map<String,String>>) ((Map<String, Object>) result.get("extraProperties")).get("leafList");
            if (list == null)
                list = new ArrayList<>();
            Boolean status = isSecurityManagerEnabled(list);
            String value = (String) handlerCtx.getInputValue("value");
            Boolean userValue = Boolean.valueOf(value);
            if (status.equals(userValue)){
                //no need to change
                return;
            }

            ArrayList<Map<String, String>> newOptions = new ArrayList<>();
            if (userValue){
                for (Map<String, String> origOption : list){
                    newOptions.add(origOption);
                }
                newOptions.add(Collections.singletonMap(JVM_OPTION, JVM_OPTION_SECURITY_MANAGER));
            } else {
                for (Map<String, String> origOption : list){
                    String str = origOption.get(JVM_OPTION);
                    if (! (str.trim().equals(JVM_OPTION_SECURITY_MANAGER) ||
                            str.trim().startsWith(JVM_OPTION_SECURITY_MANAGER_WITH_EQUAL))){
                        Map<String, String> jvmOptions = new HashMap<>(3);
                        jvmOptions.put(JVM_OPTION, str);
                        jvmOptions.put(MIN_VERSION, origOption.get(MIN_VERSION));
                        jvmOptions.put(MAX_VERSION, origOption.get(MAX_VERSION));
                        newOptions.add(Collections.unmodifiableMap(jvmOptions));
                    }
                }
            }
            Map<String, Object> payload = new HashMap<String, Object>();
            payload.put("target", configName);
            for (Map<String, String> option : newOptions) {
                ArrayList kv = InstanceHandler.getKeyValuePair(new JvmOption(UtilHandlers.escapePropertyValue(option.get(JVM_OPTION)),
                        option.get(MIN_VERSION), option.get(MAX_VERSION)).toString());
                payload.put((String)kv.get(0), kv.get(1));
            }
            RestUtil.restRequest(endpoint, payload, "POST", handlerCtx, false);
        }catch(Exception ex){
            GuiUtil.handleException(handlerCtx, ex);
        }
    }

    @Handler(id="getSecurityManagerValue",
        input={
            @HandlerInput(name="endpoint", type=String.class),
            @HandlerInput(name="attrs", type=Map.class, required=false)},
       output={
           @HandlerOutput(name="value", type=String.class)}
    )
    public static void getSecurityManagerValue(HandlerContext handlerCtx){
        List<Map<String, String>> list = InstanceHandler.getJvmOptions(handlerCtx);
        handlerCtx.setOutputValue("value",  isSecurityManagerEnabled(list).toString());
    }

    private static Boolean isSecurityManagerEnabled(List<Map<String, String>> jvmOptions){
        for (Map<String, String> jvmOptionMap : jvmOptions){
            String jvmOption = jvmOptionMap.get(JVM_OPTION);
            if (jvmOption.trim().equals(JVM_OPTION_SECURITY_MANAGER) ||
                    jvmOption.trim().startsWith(JVM_OPTION_SECURITY_MANAGER_WITH_EQUAL)){
                return Boolean.TRUE;
            }
        }
        return Boolean.FALSE;
    }

    private static final String JVM_OPTION_SECURITY_MANAGER = "-Djava.security.manager";
    private static final String JVM_OPTION_SECURITY_MANAGER_WITH_EQUAL = "-Djava.security.manager=";
    
}
