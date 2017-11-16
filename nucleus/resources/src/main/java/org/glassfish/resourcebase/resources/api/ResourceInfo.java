/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2012 Oracle and/or its affiliates. All rights reserved.
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
 *
 * Portions Copyright [2017] Payara Foundation and/or affiliates
 */

package org.glassfish.resourcebase.resources.api;


/**
 * Represents resource information (typically, bindable-resource)
 * @author Jagadish Ramu
 */
public class ResourceInfo implements org.glassfish.resourcebase.resources.api.GenericResourceInfo {

    private String name;
    private String applicationName = null;
    private String moduleName = null;

    public ResourceInfo(String name){
        this.name = name;
    }
    public ResourceInfo(String name, String applicationName){
        this.name = name;
        this.applicationName = applicationName;
    }

    public ResourceInfo(String name, String applicationName, String moduleName){
        this.name = name;
        this.applicationName = applicationName;
        this.moduleName = moduleName;
    }

    /**
     * @inheritDoc
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * @inheritDoc
     */
    @Override
    public String getApplicationName() {
        return applicationName;
    }

    /**
     * @inheritDoc
     */
    @Override
    public String getModuleName() {
        return moduleName;
    }

    @Override
        public String toString(){
            if(applicationName != null && moduleName != null){
                return "{ ResourceInfo : (jndiName="+ name +"), (applicationName="+applicationName+"), (moduleName="+moduleName+")}";
            }else if(applicationName != null){
                return "{ ResourceInfo : (jndiName="+ name +"), (applicationName="+applicationName+") }";
            }else{
                return name;
            }
    }

    @Override
    public boolean equals(Object o){
        boolean result = false;
        if(o == this){
            result = true;
        }else if(o instanceof ResourceInfo){
            ResourceInfo resourceInfo = (ResourceInfo)o;
            boolean poolNameEqual = resourceInfo.getName().equals(name);
            boolean appNameEqual = false;
            if(applicationName == null && resourceInfo.getApplicationName() == null){
                appNameEqual = true;
            }else if(applicationName !=null && resourceInfo.getApplicationName() != null
                    && applicationName.equals(resourceInfo.getApplicationName())){
                appNameEqual = true;
            }
            boolean moduleNameEqual = false;
            if(moduleName == null && resourceInfo.getModuleName() == null){
                moduleNameEqual = true;
            }else if(moduleName !=null && resourceInfo.getModuleName() != null
                    && moduleName.equals(resourceInfo.getModuleName())){
                moduleNameEqual = true;
            }
            result = poolNameEqual && appNameEqual && moduleNameEqual;
        }
        return result;
    }

    @Override
    public int hashCode(){
        int result = 67;
        if (name != null)
            result = 67 * result + name.hashCode();
        if (applicationName != null)
            result = 67 * result + applicationName.hashCode();
        if (moduleName != null)
            result = 67 * result + moduleName.hashCode();

        return result;
    }
}
