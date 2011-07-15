/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.deployment.client;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.glassfish.deployment.common.DeploymentUtils;

/**
 * Convenience class for managing deployment properties - settings or options
 * to be conveyed to the back-end during deployment-related operations.
 * <p>
 * Heavily inspired by the original from common-utils but copied here to
 * minimize dependencies.
 * 
 * @author tjquinn
 */
public class DFDeploymentProperties extends Properties {

//    private Properties deplProps = new Properties();
    
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
    
    public String getName() {
        return getProperty(NAME);
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
        return Boolean.valueOf(getProperty(DEPLOY_OPTION_JAVA_WEB_START_ENABLED, DEFAULT_JAVA_WEB_START_ENABLED)).booleanValue();
    }
    
    public void setJavaWebStartEnabled(boolean javaWebStartEnabled) {
        setProperty(DEPLOY_OPTION_JAVA_WEB_START_ENABLED,
                    Boolean.valueOf(javaWebStartEnabled).toString()); 
    }

    public String getLibraries() {
        return getProperty(DEPLOY_OPTION_LIBRARIES, null  );
    }
        
    public void setLibraries(String libraries) {
        if(libraries != null) {
            setProperty(DEPLOY_OPTION_LIBRARIES, libraries);
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

    public void setProperties(Properties props) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<Object,Object> prop : props.entrySet()) {
            if (sb.length() > 0) {
                sb.append(PROPERTY_SEPARATOR);
            }
            sb.append(prop.getKey()).append("=").append(prop.getValue());
        }
        setProperty(PROPERTY, sb.toString());
    }

    public Properties getProperties() {
        Properties result = new Properties();
        String[] settings = getProperty(PROPERTY).split(PROPERTY_SEPARATOR);
        for (String setting : settings) {
            int equals = setting.indexOf('=');
            if (equals != -1) {
                result.setProperty(setting.substring(0, equals), setting.substring(equals + 1));
            }
        }
        return result;
    }
    
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
    public static final String CONTEXT_ROOT = "contextroot";
    public static final String ARCHIVE_NAME = "archiveName";
    public static final String NAME = "name";
    public static final String TYPE = "type";
    public static final String DESCRIPTION = "description";
    public static final String CLIENTJARREQUESTED = "clientJarRequested";
    public static final String UPLOAD = "upload";
    public static final String EXTERNALLY_MANAGED = "externallyManaged";
    public static final String PATH = "path";
    public static final String DEFAULT_JAVA_WEB_START_ENABLED = "true";
    public static final String DEPLOYMENT_PLAN = "deploymentplan";

    public static final String PROPERTY = "property";
    private static final String PROPERTY_SEPARATOR = ":";
    
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
    public static final String DEPLOY_OPTION_JAVA_WEB_START_ENABLED = DeploymentUtils.DEPLOYMENT_PROPERTY_JAVA_WEB_START_ENABLED;
    public static final String DEPLOY_OPTION_LIBRARIES = "libraries";

    // possible values for module state
    public static final String ALL = "all";
    public static final String RUNNING = "running";
    public static final String NON_RUNNING = "non-running";

    // lifecycle module constants
    public static final String LIFECYCLE_MODULE = "lifecycle-module";
    public static final String CLASS_NAME = "class-name";
    public static final String CLASSPATH = "classpath";
    public static final String LOAD_ORDER = "load-order";
    public static final String IS_FAILURE_FATAL = "is-failure-fatal";
    public static final String IS_LIFECYCLE = "isLifecycle"; 
    public static final String IS_COMPOSITE = "isComposite"; 

    public Map<String,String> asMap() {
        return new HashMap<String,String>();
    }
    
//    private String getProperty(String propertyName, String defaultValue) {
//        return deplProps.getProperty(propertyName, defaultValue);
//    }
//    
//    private String getProperty(String propertyName) {
//        return deplProps.getProperty(propertyName);
//    }
//    
//    private void setProperty(String propertyName, String value) {
//        deplProps.setProperty(propertyName, value);
//    }
}
