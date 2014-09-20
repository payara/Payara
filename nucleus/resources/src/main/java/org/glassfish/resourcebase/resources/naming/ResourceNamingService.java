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

package org.glassfish.resourcebase.resources.naming;

import com.sun.logging.LogDomains;
import org.glassfish.api.admin.ProcessEnvironment;
import org.glassfish.api.naming.ComponentNamingUtil;
import org.glassfish.api.naming.GlassfishNamingManager;
import org.glassfish.api.naming.JNDIBinding;
import org.glassfish.resourcebase.resources.api.GenericResourceInfo;
import org.glassfish.resourcebase.resources.api.ResourceInfo;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import javax.naming.InitialContext;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.glassfish.resourcebase.resources.ResourceLoggingConstansts;
import org.glassfish.logging.annotation.LoggerInfo;
import org.glassfish.logging.annotation.LogMessagesResourceBundle;


/**
 * Resource naming service which helps to bind resources and internal resource objects
 * to appropriate namespace in JNDI. Supports "java:app", "java:module" and normal(physical)
 * names.
 * @author Jagadish Ramu
 *
 */
@Service
public class ResourceNamingService {

    //TODO ASR introduce contract for this service and refactor this service to connector-runtime ?
    @Inject
    private GlassfishNamingManager namingManager;

    @Inject
    private ComponentNamingUtil cnu;

    @Inject
    private ProcessEnvironment pe;

    @LogMessagesResourceBundle
    public static final String LOGMESSAGE_RESOURCE = "org.glassfish.resourcebase.resources.LogMessages";

    @LoggerInfo(subsystem="RESOURCE", description="Nucleus Resource", publish=true)

    public static final String LOGGER = "javax.enterprise.resources.naming";
    private static final Logger _logger = Logger.getLogger(LOGGER, LOGMESSAGE_RESOURCE);

    public static final String JAVA_APP_SCOPE_PREFIX = "java:app/";
    public static final String JAVA_COMP_SCOPE_PREFIX = "java:comp/";
    public static final String JAVA_MODULE_SCOPE_PREFIX = "java:module/";
    public static final String JAVA_GLOBAL_SCOPE_PREFIX = "java:global/";


    public void publishObject(GenericResourceInfo resourceInfo, String jndiName, Object object,boolean rebind)
            throws NamingException{
        String applicationName = resourceInfo.getApplicationName();
        String moduleName = resourceInfo.getModuleName();
        moduleName = org.glassfish.resourcebase.resources.util.ResourceUtil.getActualModuleName(moduleName);

        if(!isGlobalName(resourceInfo.getName()) && applicationName != null && moduleName != null ){

            Object alreadyBoundObject = null;
            if(rebind){
                try{
                    namingManager.unbindModuleObject(applicationName, moduleName, getModuleScopedName(jndiName));
                }catch(NameNotFoundException e){
                    //ignore
                }
            }else{
                try {
                    alreadyBoundObject = namingManager.lookupFromModuleNamespace(applicationName,
                            moduleName, getModuleScopedName(jndiName), null);
                } catch (NameNotFoundException e) {
                    //ignore
                }
                if (alreadyBoundObject != null) {
                    throw new NamingException("Object already bound for jndiName " +
                            "[ " + jndiName + " ] of  module namespace [" + moduleName+ "] " +
                            "of application ["+applicationName+"] ");
                }
            }

            JNDIBinding bindings = new ModuleScopedResourceBinding(getModuleScopedName(jndiName), object);
            List<JNDIBinding> list = new ArrayList<JNDIBinding>();
            list.add(bindings);
            if(_logger.isLoggable(Level.FINEST)){
                debug("application=" + applicationName + ", module name=" +moduleName+", binding name=" + jndiName);
            }
            namingManager.bindToModuleNamespace(applicationName, moduleName, list);
        }else if(!isGlobalName(resourceInfo.getName()) && applicationName != null ) {

            Object alreadyBoundObject = null;
            if(rebind){
                try{
                    namingManager.unbindAppObject(applicationName, getAppScopedName(jndiName));
                }catch(NameNotFoundException e){
                    //ignore
                }
            }else{
                try {
                    alreadyBoundObject = namingManager.lookupFromAppNamespace(applicationName,
                            getAppScopedName(jndiName), null);
                } catch (NameNotFoundException e) {
                    //ignore
                }
                if (alreadyBoundObject != null) {
                    throw new NamingException("Object already bound for jndiName " +
                            "[ " + jndiName + " ] of application's namespace [" + applicationName + "]");
                }
            }

            JNDIBinding bindings = new ApplicationScopedResourceBinding(getAppScopedName(jndiName), object);
            List<JNDIBinding> list = new ArrayList<JNDIBinding>();
            list.add(bindings);
            if(_logger.isLoggable(Level.FINEST)){
                debug("application=" + applicationName + ", binding name=" + jndiName);
            }
            namingManager.bindToAppNamespace(applicationName, list);
            bindAppScopedNameForAppclient(object, jndiName, applicationName);
        }else{
            namingManager.publishObject(jndiName, object, true);
        }
    }

    public void publishObject(ResourceInfo resourceInfo, Object object,boolean rebind) throws NamingException{
        String jndiName = resourceInfo.getName();
        publishObject(resourceInfo, jndiName, object, rebind);
    }

    private void bindAppScopedNameForAppclient(Object object, String jndiName, String applicationName)
            throws NamingException {
        String internalGlobalJavaAppName =
                cnu.composeInternalGlobalJavaAppName(applicationName, getAppScopedName(jndiName));
        if(_logger.isLoggable(Level.FINEST)){
            debug("binding app-scoped-resource for appclient : " + internalGlobalJavaAppName);
        }
        namingManager.publishObject(internalGlobalJavaAppName, object, true);
    }


    public void unpublishObject(GenericResourceInfo resourceInfo, String jndiName) throws NamingException {
        String applicationName = resourceInfo.getApplicationName();
        String moduleName = resourceInfo.getModuleName();
        moduleName = org.glassfish.resourcebase.resources.util.ResourceUtil.getActualModuleName(moduleName);

        if(!isGlobalName(resourceInfo.getName()) && applicationName != null && moduleName != null){
            namingManager.unbindModuleObject(applicationName, moduleName, getModuleScopedName(jndiName));
        }else if(!isGlobalName(resourceInfo.getName()) && applicationName != null) {
            namingManager.unbindAppObject(applicationName, getAppScopedName(jndiName));
            unbindAppScopedNameForAppclient(jndiName, applicationName);
        }else{
            namingManager.unpublishObject(jndiName);
        }
    }

    private void unbindAppScopedNameForAppclient(String jndiName, String applicationName) throws NamingException {
        String internalGlobalJavaAppName =
                cnu.composeInternalGlobalJavaAppName(applicationName, getAppScopedName(jndiName));
        namingManager.unpublishObject(internalGlobalJavaAppName);
    }

    private boolean isGlobalName(String jndiName) {
        return jndiName.startsWith(JAVA_GLOBAL_SCOPE_PREFIX) ||
                (!jndiName.startsWith(JAVA_APP_SCOPE_PREFIX) &&
                !jndiName.startsWith(JAVA_MODULE_SCOPE_PREFIX));
    }

    public Object lookup(GenericResourceInfo resourceInfo, String name, Hashtable env) throws NamingException{
        String applicationName = resourceInfo.getApplicationName();
        String moduleName = resourceInfo.getModuleName();
        moduleName = org.glassfish.resourcebase.resources.util.ResourceUtil.getActualModuleName(moduleName);

        if(!isGlobalName(resourceInfo.getName()) && applicationName != null && moduleName != null){
            return namingManager.lookupFromModuleNamespace(applicationName, moduleName, getModuleScopedName(name), env);
        }else if(!isGlobalName(resourceInfo.getName()) && applicationName != null) {
            if(pe.getProcessType().isServer() || pe.getProcessType().isEmbedded()){
                return namingManager.lookupFromAppNamespace(applicationName, getAppScopedName(name), env);
            }else{
                String internalGlobalJavaAppName =
                        cnu.composeInternalGlobalJavaAppName(applicationName, getAppScopedName(name));
                if(_logger.isLoggable(Level.FINEST)){
                    debug("appclient lookup : " + internalGlobalJavaAppName);
                }
                return namingManager.getInitialContext().lookup(internalGlobalJavaAppName);
            }
        }else{
            if(env != null){
                InitialContext ic = new InitialContext(env);
                return ic.lookup(name);
            }else{
                return namingManager.getInitialContext().lookup(name);
            }
        }
    }

    public Object lookup(GenericResourceInfo resourceInfo, String name) throws NamingException{
        return lookup(resourceInfo, name, null);
    }

    private String getModuleScopedName(String name){

/*
        if(!name.startsWith(JAVA_MODULE_SCOPE_PREFIX) && !name.startsWith(JAVA_GLOBAL_SCOPE_PREFIX)){
            return JAVA_MODULE_SCOPE_PREFIX+name;
        }
*/

        return name;
    }

    private String getAppScopedName(String name){

/*
        if(!name.startsWith(JAVA_APP_SCOPE_PREFIX) && !name.startsWith(JAVA_GLOBAL_SCOPE_PREFIX)){
            return JAVA_APP_SCOPE_PREFIX+name;
        }
*/

        return name;
    }

    private void debug(String message) {
        if(_logger.isLoggable(Level.FINEST)) {
            _logger.finest("[ASR] [ResourceNamingService] : " + message);
        }
    }
}
