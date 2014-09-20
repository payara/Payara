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

package org.glassfish.resourcebase.resources.util;

import com.sun.enterprise.config.serverbeans.*;
import com.sun.logging.LogDomains;
import org.glassfish.resourcebase.resources.api.GenericResourceInfo;
import org.glassfish.resourcebase.resources.api.PoolInfo;
import org.glassfish.resourcebase.resources.api.ResourceConstants;
import org.glassfish.resourcebase.resources.api.ResourceInfo;

import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.glassfish.resourcebase.resources.ResourceLoggingConstansts;
import org.glassfish.logging.annotation.LoggerInfo;
import org.glassfish.logging.annotation.LogMessagesResourceBundle;


/**
 * @author Jagadish Ramu
 */
public class ResourceUtil {

    @LogMessagesResourceBundle
    public static final String LOGMESSAGE_RESOURCE = "org.glassfish.resourcebase.resources.LogMessages";

    @LoggerInfo(subsystem="RESOURCE", description="Nucleus Resource", publish=true)

    public static final String LOGGER = "javax.enterprise.resources.util";
    private static final Logger _logger = Logger.getLogger(LOGGER, LOGMESSAGE_RESOURCE);

    private static final String RESOURCES_XML_META_INF = "META-INF/glassfish-resources.xml";
    private static final String RESOURCES_XML_WEB_INF = "WEB-INF/glassfish-resources.xml";

    public static BindableResource getBindableResourceByName(Resources resources, String name) {
        Collection<BindableResource> typedResources = resources.getResources(BindableResource.class);
        if(typedResources != null)
        for(BindableResource resource : typedResources){
            if(resource.getJndiName().equals(name)){
                return resource;
            }
        }
        return null;
    }


    public static Resource getResourceByName(Class<? extends Resource> clazz, Resources resources,
        String name) {
      Collection<? extends Resource> typedResources = resources.getResources(clazz);
      if (typedResources != null)
        for (Resource resource : typedResources) {
          if (resource.getIdentity().equals(name)) {
            return resource;
          }
        }
      return null;
    }

    public static Resource getResourceByIdentity(Resources resources, String name) {
        for (Resource resource : resources.getResources()) {
          if (resource.getIdentity().equals(name)) {
            return resource;
          }
        }
        return null;
    }

    public static ResourceInfo getResourceInfo(BindableResource resource){

        if(resource.getParent() != null && resource.getParent().getParent() instanceof Application){
            Application application = (Application)resource.getParent().getParent();
            return new ResourceInfo(resource.getJndiName(), application.getName());
        }else if(resource.getParent() != null && resource.getParent().getParent() instanceof Module){
            Module module = (Module)resource.getParent().getParent();
            Application application = (Application)module.getParent();
            return new ResourceInfo(resource.getJndiName(), application.getName(), module.getName());
        }else{
            return new ResourceInfo(resource.getJndiName());
        }
    }

    public static boolean isValidEventType(Object instance) {
        return instance instanceof Resource;
    }

    /**
     * given a resource config bean, returns the resource name / jndi-name
     * @param resource
     * @return resource name / jndi-name
     */
    public static ResourceInfo getGenericResourceInfo(Resource resource){
        ResourceInfo resourceInfo = null;
        String resourceName = resource.getIdentity();
        resourceInfo = getGenericResourceInfo(resource, resourceName);
        return resourceInfo;
    }

    public static ResourceInfo getGenericResourceInfo(Resource resource, String resourceName){
        if(resource.getParent() != null && resource.getParent().getParent() instanceof Application){
            Application application = (Application)resource.getParent().getParent();
            return new ResourceInfo(resourceName, application.getName());
        }else if(resource.getParent() != null && resource.getParent().getParent() instanceof Module){
            Module module = (Module)resource.getParent().getParent();
            Application application = (Application)module.getParent();
            return new ResourceInfo(resourceName, application.getName(), module.getName());
        }else{
            return new ResourceInfo(resourceName);
        }
    }

    public static PoolInfo getPoolInfo(ResourcePool resource){

        if(resource.getParent() != null && resource.getParent().getParent() instanceof Application){
            Application application = (Application)resource.getParent().getParent();
            return new PoolInfo(resource.getName(), application.getName());
        }else if(resource.getParent() != null && resource.getParent().getParent() instanceof Module){
            Module module = (Module)resource.getParent().getParent();
            Application application = (Application)module.getParent();
            return new PoolInfo(resource.getName(), application.getName(), module.getName());
        }else{
            return new PoolInfo(resource.getName());
        }
    }

    public static String getActualModuleNameWithExtension(String moduleName) {
        String actualModuleName = moduleName;
        if(moduleName.endsWith("_war")){
            int index = moduleName.lastIndexOf("_war");
            actualModuleName = moduleName.substring(0, index) + ".war";
        }else if(moduleName.endsWith("_rar")){
            int index = moduleName.lastIndexOf("_rar");
            actualModuleName = moduleName.substring(0, index) + ".rar";
        }else if(moduleName.endsWith("_jar")){
            int index = moduleName.lastIndexOf("_jar");
            actualModuleName = moduleName.substring(0, index) + ".jar";
        }
        return actualModuleName;
    }

    //TODO ASR : checking for .jar / .rar / .war / .ear ?
    public static String getActualModuleName(String moduleName){
        if(moduleName != null){
            if(moduleName.endsWith(".jar") /*|| moduleName.endsWith(".war") */|| moduleName.endsWith(".rar")){
                moduleName = moduleName.substring(0,moduleName.length()-4);
            }
        }
        return moduleName;
    }

    /**
     * load and create an object instance
     * @param className class to load
     * @return instance of the class
     */
    public static Object loadObject(String className) {
        Object obj = null;
        Class c;

        try {
            c = Thread.currentThread().getContextClassLoader().loadClass(className);
            obj = c.newInstance();
        } catch (Exception ex) {
            _logger.log(Level.SEVERE, ResourceLoggingConstansts.LOAD_CLASS_FAIL, className);
            _logger.log(Level.SEVERE, ResourceLoggingConstansts.LOAD_CLASS_FAIL_EXCEP, ex.getMessage());
        }
        return obj;
    }


    //TODO ASR : instead of explicit APIs, getScope() can return "none" or "app" or "module" enum value ?
    public static boolean isApplicationScopedResource(GenericResourceInfo resourceInfo){
        return resourceInfo != null && resourceInfo.getApplicationName() != null &&
                resourceInfo.getName() != null && resourceInfo.getName().startsWith(ResourceConstants.JAVA_APP_SCOPE_PREFIX);
    }

    public static boolean isModuleScopedResource(GenericResourceInfo resourceInfo){
        return resourceInfo != null && resourceInfo.getApplicationName() != null && resourceInfo.getModuleName() != null &&
                resourceInfo.getName() != null && resourceInfo.getName().startsWith(ResourceConstants.JAVA_MODULE_SCOPE_PREFIX);
    }
}
