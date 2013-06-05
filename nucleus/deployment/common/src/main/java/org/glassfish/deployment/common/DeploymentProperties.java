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

/*
 * DeploymentProperties.java
 *
 * Created on August 7, 2003, 10:15 PM
 */

package org.glassfish.deployment.common;

import java.util.Properties;
import java.util.Iterator;
import java.util.HashMap;
import java.util.Map;
import java.io.File;

/**
 * This properties are used to transfer information between
 * deployment clients and server
 *
 * @author  Sandhya E
 */
public class DeploymentProperties extends Properties {

    // declare SUID for class versioning compatibility
    // generated using pe build fcs-b50
    // this value should stay the same for all
    // 8.x releases
    static final long serialVersionUID = -6891581813642829148L;

    public DeploymentProperties() {
        super();
    }

    // construct a DeploymentProperties using the props 
    // passe from client
    public DeploymentProperties(Properties props) {
        super();
	putAll(props);
    }
    
    // construct a DeploymentProperties using the map 
    // passed from client  
    // 1. For keys defined before AMX time, since different
    //    keys were defined in the DeploymentMgrMBean,
    //    we need to do conversion between the keys
    //    to keep backward compatibilities
    // 2. For internal keys and the new keys defined after AMX 
    //    time, we don't need to do any conversion
    // 
    public DeploymentProperties(Map map) {
        super();
        if (map == null) {
            return;
        }
        Properties props = new Properties();
        for (Iterator<Map.Entry> itr = map.entrySet().iterator(); itr.hasNext();) {
            Map.Entry entry = itr.next();
            String mapKey = (String) entry.getKey() ;
            String mapValue = (String) entry.getValue();
            String propsKey = (String) keyMap.get(mapKey);
            if (mapValue != null) {
                // for public keys, we need to convert
                if (propsKey != null) {
                    props.put(propsKey, mapValue);
                }
                // for internal keys and new keys, we just add it 
                // without conversion
                else {
                    props.put(mapKey, mapValue);
                }
            }
        }
        putAll(props);
    }

    // Construct a map with the keys defined in DeploymentMgrMBean
    // this is used when the ASAPI client convert the props 
    // from the client to a map to invoke DeploymentMgrMBean API
    // 1. For keys defined before AMX time, since different
    //    keys were defined in the DeploymentMgrMBean,
    //    we need to do conversion between the keys
    //    to keep backward compatibilities
    // 2. For internal keys and the new keys defined after AMX
    //    time, we don't need to do any conversion
    //    
    public static Map propsToMap(Properties dProps) {
        Map map = new HashMap();
        if (dProps == null) {
            return map;
        }
        for (Iterator<Map.Entry<Object, Object>> itr = dProps.entrySet().iterator(); itr.hasNext();) {
            Map.Entry<Object, Object> entry = itr.next();
            String propsKey = (String) entry.getKey();
            String propsValue = (String) entry.getValue();
            String mapKey = (String) keyMap.get(propsKey);
            if (propsValue != null) {
                // for public keys, we need to convert
                if (mapKey != null) {
                    map.put(mapKey, propsValue);
                // for internal keys and new keys, we just add it 
                // without conversion
                } else {
                    map.put(propsKey, propsValue);
                }
            }
        }
        return map;
    }

    /**
     * This set of get and set for WSDL_TARGET_HINT is to enable back generate WSDL with the host and port info of the
     * actual server target in case only one target has been specified by the client; Refer to bug ID 6157923 for more
     * details
     */
    
    public String getWsdlTargetHint() throws IllegalArgumentException {
        return getProperty(WSDL_TARGET_HINT, null);
    }
    
    public void setWsdlTargetHint(String target) {
        if(target != null) {
            setProperty(WSDL_TARGET_HINT, target);
        }
    }
    
    public String getTarget() throws IllegalArgumentException {
        return getProperty(TARGET, null);
    }
    
    public void setTarget(String target) {
        if (target != null)
            setProperty(TARGET, target);
    }
    
    public boolean getRedeploy() {
        return Boolean.valueOf(getProperty(REDEPLOY, DEFAULT_REDEPLOY)).booleanValue();
    }

    public void setRedeploy(boolean redeploy) {
        setProperty(REDEPLOY, Boolean.valueOf(redeploy).toString());
    }

    public String getArchiveName() throws IllegalArgumentException{
        return getProperty(ARCHIVE_NAME, null);
    }
    
    public void setArchiveName(String archiveName) {
        if(archiveName != null)
            setProperty(ARCHIVE_NAME, archiveName);
    }
        
    public boolean getForce() {
        return Boolean.valueOf(getProperty(FORCE,DEFAULT_FORCE)).booleanValue();
    }
    
    public void setForce(boolean force) {
        setProperty(FORCE, Boolean.valueOf(force).toString());
    }

    public boolean getReload() {
        return Boolean.valueOf(getProperty(RELOAD,DEFAULT_RELOAD)).booleanValue();
    }

    public void setReload(boolean reload) {
        setProperty(RELOAD, Boolean.valueOf(reload).toString());
    }

    public boolean getCascade() {
        return Boolean.valueOf(getProperty(CASCADE,DEFAULT_CASCADE)).booleanValue();
    }
    
    public void setCascade(boolean cascade) {
        setProperty(CASCADE, Boolean.valueOf(cascade).toString());
    }
    
    public boolean getPrecompileJSP() {
        return Boolean.valueOf(getProperty(PRECOMPILE_JSP,DEFAULT_PRECOMPILE_JSP)).booleanValue();
    }
    
    public void setPrecompileJSP(boolean precompileJSP) {
        setProperty(PRECOMPILE_JSP, Boolean.valueOf(precompileJSP).toString());
    }
    
    public boolean getVerify() {
        return Boolean.valueOf(getProperty(VERIFY,DEFAULT_VERIFY)).booleanValue();
    }
    
    public void setVerify(boolean verify) {
        setProperty(VERIFY, Boolean.valueOf(verify).toString());
    }
    
    public String getVirtualServers() {
        return getProperty(VIRTUAL_SERVERS , DEFAULT_VIRTUAL_SERVERS);
    }
    
    public void setVirtualServers(String virtualServers) {
        if(virtualServers != null)
	        setProperty(VIRTUAL_SERVERS, virtualServers);
    }
    
    public boolean getEnabled() {
        return Boolean.valueOf(getProperty(ENABLED,DEFAULT_ENABLED)).booleanValue();
    }
    
    public void setEnabled(boolean enabled) {
        setProperty(ENABLED, Boolean.valueOf(enabled).toString());
    }
    
    public String getContextRoot() {
        return getProperty(CONTEXT_ROOT, null);
    }
    
    public void setContextRoot(String contextRoot) {
        if(contextRoot != null)
            setProperty(CONTEXT_ROOT, contextRoot);
    }
    
    public String getName(String filePath) {
        return getProperty(NAME, getDefaultComponentName(filePath));
    }
    
    public void setName(String name) {
        if(name != null)
            setProperty(NAME, name);
    }
    
    public String getDescription() {
        return getProperty(DESCRIPTION, "");
    }

    public void setDescription(String description) {
        if(description != null)
            setProperty(DESCRIPTION, description);
    }

    public boolean getGenerateRMIStubs() {
        return Boolean.valueOf(getProperty(GENERATE_RMI_STUBS,DEFAULT_GENERATE_RMI_STUBS)).booleanValue();
    }

    public void setGenerateRMIStubs(boolean generateRMIStubs ) {
        setProperty(GENERATE_RMI_STUBS,
                    Boolean.valueOf(generateRMIStubs).toString());
    }

    public boolean getAvailabilityEnabled() {
        return Boolean.valueOf(getProperty(AVAILABILITY_ENABLED,DEFAULT_AVAILABILITY_ENABLED)).booleanValue();
    }

    public void setAvailabilityEnabled(boolean availabilityEnabled ) {
        setProperty(AVAILABILITY_ENABLED,
                    Boolean.valueOf(availabilityEnabled).toString());
    }

    public boolean getJavaWebStartEnabled() {
        return Boolean.valueOf(getProperty(DEPLOY_OPTION_JAVA_WEB_START_ENABLED_KEY, DEFAULT_JAVA_WEB_START_ENABLED)).booleanValue();
    }
    
    public void setJavaWebStartEnabled(boolean javaWebStartEnabled) {
        setProperty(DEPLOY_OPTION_JAVA_WEB_START_ENABLED_KEY,
                    Boolean.valueOf(javaWebStartEnabled).toString()); 
    }

    public String getLibraries() {
        return getProperty(DEPLOY_OPTION_LIBRARIES_KEY, null );
    }
        
    public void setLibraries(String libraries) {
        if(libraries != null) {
            setProperty(DEPLOY_OPTION_LIBRARIES_KEY, libraries);
        }
    }

    public String getResourceAction() {
        return getProperty(RESOURCE_ACTION, null );
    }

    public void setResourceAction(String resourceAction) {
        if(resourceAction != null) {
            setProperty(RESOURCE_ACTION, resourceAction);
        }
    }

    public String getResourceTargetList() {
        return getProperty(RESOURCE_TARGET_LIST, null );
    }

    public void setResourceTargetList(String resTargetList) {
        if(resTargetList != null) {
            setProperty(RESOURCE_TARGET_LIST, resTargetList);
        }
    }

    public void setUpload(boolean uploadEnabled) {
        setProperty(UPLOAD, Boolean.toString(uploadEnabled));
    }
    
    public boolean getUpload() {
        return Boolean.valueOf(getProperty(UPLOAD, DEFAULT_UPLOAD)).booleanValue();
    }

    public void setExternallyManaged(boolean isExternallyManaged) {
        setProperty(EXTERNALLY_MANAGED, Boolean.toString(isExternallyManaged));
    }
              
    public void setPath(String path) {
        setProperty(PATH, path);
    }
    
    public String getPath() {
        return getProperty(PATH);
    }
    public boolean getExternallyManaged() {
        return Boolean.valueOf(getProperty(EXTERNALLY_MANAGED, DEFAULT_EXTERNALLY_MANAGED)).booleanValue();
    }

    public Properties getPropertiesForInvoke(){
        return (Properties)this;
    }
    
    public Properties prune() {
        /*Properties propsCopy = props.clone();*/
        remove(FORCE);
        remove(RELOAD);
        remove(CONTEXT_ROOT);
        remove(PRECOMPILE_JSP);
        remove(VERIFY);
        remove(ENABLED);
        remove(VIRTUAL_SERVERS);
        remove(NAME);
        remove(TYPE);
        remove(ARCHIVE_NAME);
        remove(CASCADE);
        remove(REDEPLOY);
        remove(GENERATE_RMI_STUBS);
        remove(AVAILABILITY_ENABLED);
        remove(DEPLOY_OPTION_JAVA_WEB_START_ENABLED_KEY);  
        remove(DEPLOY_OPTION_LIBRARIES_KEY);
        remove(RESOURCE_ACTION);
        remove(RESOURCE_TARGET_LIST);
        remove(UPLOAD);
        remove(EXTERNALLY_MANAGED);
        return this;
    }
    
    /////////////////////////////////////////////////////////////////////////
    public String getDefaultContextRoot(String filePath) {
        return getDefaultComponentName(filePath);
    }
    
    private String getDefaultComponentName(String filePath) {
        final String fileName = new File(filePath).getName();
        int toIndex = fileName.lastIndexOf('.');
        if (toIndex < 0) {
            toIndex = fileName.length();
        }
        final String name = fileName.substring(0, toIndex);
        //FIXME check for blank string
        return name;
    }

    // This map is only used for public keys before AMX time,
    // for keys after AMX time, no need to put in the table and 
    // do conversion.
    // Initialize a key map which contains mapping for the keys 
    // defined in this file and DeploymentMgrMBean
    // the mapping for both directions are contained
    // for example for key A in DeploymentProperties and 
    // corresponding key B in DeploymentMgrMBean, 
    // the map contains both A->B and B->A
    // will only work if A not equals to B
    private static void initializeKeyMap() {
        keyMap = new HashMap();
//        keyMap.put(REDEPLOY, DEPLOY_OPTION_REDEPLOY_KEY);
//        keyMap.put(DEPLOY_OPTION_REDEPLOY_KEY, REDEPLOY);
        keyMap.put(FORCE, DEPLOY_OPTION_FORCE_KEY);
        keyMap.put(DEPLOY_OPTION_FORCE_KEY, FORCE);
        keyMap.put(CASCADE, DEPLOY_OPTION_CASCADE_KEY);
        keyMap.put(DEPLOY_OPTION_CASCADE_KEY, CASCADE);
        keyMap.put(VERIFY, DEPLOY_OPTION_VERIFY_KEY);
        keyMap.put(DEPLOY_OPTION_VERIFY_KEY, VERIFY);
        keyMap.put(VIRTUAL_SERVERS, DEPLOY_OPTION_VIRTUAL_SERVERS_KEY);
        keyMap.put(DEPLOY_OPTION_VIRTUAL_SERVERS_KEY, VIRTUAL_SERVERS);
        keyMap.put(PRECOMPILE_JSP, DEPLOY_OPTION_PRECOMPILE_JSP_KEY);
        keyMap.put(DEPLOY_OPTION_PRECOMPILE_JSP_KEY, PRECOMPILE_JSP);
        keyMap.put(ENABLED, DEPLOY_OPTION_ENABLED_KEY);
        keyMap.put(DEPLOY_OPTION_ENABLED_KEY, ENABLED);
        keyMap.put(CONTEXT_ROOT, DEPLOY_OPTION_CONTEXT_ROOT_KEY);
        keyMap.put(DEPLOY_OPTION_CONTEXT_ROOT_KEY, CONTEXT_ROOT);
        keyMap.put(NAME, DEPLOY_OPTION_NAME_KEY);
        keyMap.put(DEPLOY_OPTION_NAME_KEY, NAME);
        keyMap.put(DESCRIPTION, DEPLOY_OPTION_DESCRIPTION_KEY);
        keyMap.put(DEPLOY_OPTION_DESCRIPTION_KEY, DESCRIPTION);
        keyMap.put(GENERATE_RMI_STUBS, DEPLOY_OPTION_GENERATE_RMI_STUBS_KEY);
        keyMap.put(DEPLOY_OPTION_GENERATE_RMI_STUBS_KEY, GENERATE_RMI_STUBS);
        keyMap.put(AVAILABILITY_ENABLED, DEPLOY_OPTION_AVAILABILITY_ENABLED_KEY);
        keyMap.put(DEPLOY_OPTION_AVAILABILITY_ENABLED_KEY, AVAILABILITY_ENABLED);
    }

    ////////////////////////////////////////////////
    // list of properties from client to server
    ////////////////////////////////////////////////    
    public static final String WSDL_TARGET_HINT = "wsdlTargetHint";
    public static final String TARGET = "target";
    public static final String REDEPLOY = "redeploy";
    public static final String DEFAULT_REDEPLOY = "false";
    public static final String FORCE = "force";
    public static final String DEFAULT_FORCE  = "true";
    public static final String RELOAD = "reload";
    public static final String DEFAULT_RELOAD  = "false";
    public static final String CASCADE = "cascade";
    public static final String DEFAULT_CASCADE  = "false";
    public static final String VERIFY = "verify";
    public static final String DEFAULT_VERIFY  = "false";
    public static final String VIRTUAL_SERVERS = "virtualservers";
    public static final String DEFAULT_VIRTUAL_SERVERS = null;
    public static final String PRECOMPILE_JSP = "precompilejsp";
    public static final String DEFAULT_PRECOMPILE_JSP = "false";
    public static final String GENERATE_RMI_STUBS = "generatermistubs";
    public static final String DEFAULT_GENERATE_RMI_STUBS= "false";
    public static final String AVAILABILITY_ENABLED = "availabilityenabled";
    public static final String DEFAULT_AVAILABILITY_ENABLED = "false";
    public static final String ENABLED = "enabled";
    public static final String DEFAULT_ENABLED = "true";
    public static final String CONTEXT_ROOT = "contextRoot";
    public static final String ARCHIVE_NAME = "archiveName";
    public static final String NAME = "name";
    public static final String TYPE = "type";
    public static final String DESCRIPTION = "description";
    public static final String CLIENTJARREQUESTED = "clientJarRequested";
    public static final String UPLOAD = "upload";
    public static final String EXTERNALLY_MANAGED = "externallyManaged";
    public static final String PATH = "path";
    public static final String COMPATIBILITY = "compatibility";
    public static final String DEFAULT_APP_NAME = "defaultAppName";
    
    ////////////////////////////////////////////////
    // list of properties from server to client
    ////////////////////////////////////////////////
    public static final String MODULE_ID = "moduleid";


    // list of keys defined in DeploymentMgrMBean
    public static final String KEY_PREFIX = "X-DeploymentMgr.";
//    public static final String DEPLOY_OPTION_REDEPLOY_KEY = 
//        KEY_PREFIX + "Redeploy";
    public static final String DEPLOY_OPTION_FORCE_KEY = KEY_PREFIX + "Force";
    public static final String DEPLOY_OPTION_CASCADE_KEY = KEY_PREFIX + "Cascade";
    public static final String DEPLOY_OPTION_VERIFY_KEY = KEY_PREFIX + "Verify"; 
    public static final String DEPLOY_OPTION_VIRTUAL_SERVERS_KEY = 
        KEY_PREFIX + "VirtualServers"; 
    public static final String DEPLOY_OPTION_PRECOMPILE_JSP_KEY = 
        KEY_PREFIX + "PrecompileJSP";
    public static final String DEPLOY_OPTION_ENABLED_KEY = KEY_PREFIX + "Enable";
    public static final String DEPLOY_OPTION_CONTEXT_ROOT_KEY = 
        KEY_PREFIX + "ContextRoot"; 
    public static final String DEPLOY_OPTION_NAME_KEY = KEY_PREFIX + "Name";
    public static final String DEPLOY_OPTION_DESCRIPTION_KEY = 
        KEY_PREFIX + "Description";
    public static final String DEPLOY_OPTION_GENERATE_RMI_STUBS_KEY = 
        KEY_PREFIX + "GenerateRMIStubs";
    public static final String DEPLOY_OPTION_AVAILABILITY_ENABLED_KEY = 
        KEY_PREFIX + "AvailabilityEnabled";
    public static final String DEPLOYMENT_PLAN = "deploymentplan";


    // here are the new keys after AMX time, no conversions needed 
    // for these keys
    public static final String DEPLOY_OPTION_JAVA_WEB_START_ENABLED_KEY =
        KEY_PREFIX + "JavaWebStartEnabled";
    public static final String DEPLOY_OPTION_LIBRARIES_KEY =
        KEY_PREFIX + "Libraries";
    public static final String DEFAULT_JAVA_WEB_START_ENABLED = "true";
    
    public static final String DEFAULT_UPLOAD = "true";
    public static final String DEFAULT_EXTERNALLY_MANAGED = "false";

    // resource constants
    public static final String RESOURCE_ACTION = "resourceAction";
    public static final String RESOURCE_TARGET_LIST = "resourceTargetList";

    // possible values for resource action
    public static final String RES_DEPLOYMENT = "resDeployment";
    public static final String RES_CREATE_REF = "resCreateRef";
    public static final String RES_DELETE_REF = "resDeleteRef";
    public static final String RES_UNDEPLOYMENT = "resUndeployment";
    public static final String RES_REDEPLOYMENT = "resRedeployment";
    public static final String RES_NO_OP = "resNoOp";

    public static final String APP_CONFIG = "appConfig";
    public static final String STATE = "state";
    public static final String MODULE_INFO = "moduleInfo";
    public static final String MODULE_NAME = "module-name";
    public static final String DD_PATH =  "dd-path";
    public static final String DD_CONTENT = "dd-content";

    public static final String SYSTEM_ADMIN = "system-admin";
    public static final String SYSTEM_ALL = "system-all";
    
    public static final String PREVIOUS_TARGETS = "previousTargets";
    public static final String PREVIOUS_VIRTUAL_SERVERS = 
        "previousVirtualServers";
    public static final String PREVIOUS_ENABLED_ATTRIBUTES = 
        "previousEnabledAttributes";
    public static final String PRESERVED_CONTEXT_ROOT = "preservedcontextroot";
    public static final String APP_PROPS = "appprops";
    public static final String IS_REDEPLOY = "isredeploy";
    public static final String IS_UNDEPLOY = "isundeploy";
    public static final String IGNORE_CASCADE = "_ignoreCascade";
    public static final String KEEP_STATE = "keepstate";
    public static final String DROP_TABLES = "droptables";
    public static final String ALT_DD = "altdd";
    public static final String RUNTIME_ALT_DD = "runtimealtdd";
    public static final String COMMAND_PARAMS = "commandparams";


    public static final String PROTOCOL = "protocol";
    public static final String HOST = "host";
    public static final String PORT = "port";
    public static final String CONTEXT_PATH = "contextpath";

    // internal use - from .reload support
    public static final String KEEP_REPOSITORY_DIRECTORY = "keepreposdir";
    // internal user - for redeploy support
    public static final String REDEPLOY_CONTEXT_PROPERTIES = "commandcontextprops";

    // internal use - from autodeployer
    public static final String LOG_REPORTED_ERRORS = "logReportedErrors";

    public static final String KEEP_SESSIONS = "keepSessions";

    public static final String PRESERVE_APP_SCOPED_RESOURCES = "preserveAppScopedResources";

    public static final String IS_SNIFFER_USER_VISIBLE = "isSnifferUserVisible";

    public static final String SKIP_SCAN_EXTERNAL_LIB = "skipScanExternalLib";

    public static final String SNIFFERS = "sniffers"; 

    static Map keyMap;

    static {
        initializeKeyMap();
    }
}
