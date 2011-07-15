/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2011 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.tools.verifier.tests;
import com.sun.enterprise.deployment.*;
import org.glassfish.deployment.common.ModuleDescriptor;
import org.glassfish.deployment.common.RootDeploymentDescriptor;

/**
 * This class constructs the name of the app client/bean/connector as
 * appName.jarName.componentName
 *
 * @author Sheetal Vartak
 */

public class ComponentNameConstructor {

    private String appName = "";
    private String jarName = "";
    private String componentName = "";
    
    public ComponentNameConstructor(EjbDescriptor ejbDsc) {
	    EjbBundleDescriptor ejbBundle = ejbDsc.getEjbBundleDescriptor();
        ModuleDescriptor moduleDesc = ejbBundle.getModuleDescriptor();
        if(!moduleDesc.isStandalone()){ // print app name only for embedded ones
            this.appName = ejbBundle.getApplication().getRegistrationName();
        }
	    this.jarName = moduleDesc.getArchiveUri();
	    this.componentName = ejbDsc.getName();
    }

    // this takes care of all bundle descriptors.
    public ComponentNameConstructor(BundleDescriptor bundleDesc) {
        ModuleDescriptor moduleDesc = bundleDesc.getModuleDescriptor();
        if(!moduleDesc.isStandalone()){ // print app name only for embedded ones
            this.appName = bundleDesc.getApplication().getRegistrationName();
        }
	    this.jarName = moduleDesc.getArchiveUri();
        // there is no point in printing comp name since it is bundle desc.
    }

    public ComponentNameConstructor(String appName, String jarName, String componentName) {
        this.appName = appName;
        this.jarName = jarName;
        this.componentName = componentName;
    }

    public ComponentNameConstructor(WebServiceEndpoint wse) {
        BundleDescriptor bundleDesc = wse.getBundleDescriptor();
        ModuleDescriptor moduleDesc = bundleDesc.getModuleDescriptor();
        if(!moduleDesc.isStandalone()){ // print app name only for embedded ones
            this.appName = bundleDesc.getApplication().getRegistrationName();
        }
        this.jarName = moduleDesc.getArchiveUri();
        // WebServiceEndpoint path is WebServices->WebService->WebServiceEndpoint
        this.componentName = wse.getWebService().getName()+"#"+wse.getEndpointName(); // NOI18N
    }

    public ComponentNameConstructor(ServiceReferenceDescriptor srd) {
        BundleDescriptor bundleDesc = srd.getBundleDescriptor();
        ModuleDescriptor moduleDesc = bundleDesc.getModuleDescriptor();
        if(!moduleDesc.isStandalone()){ // print app name only for embedded ones
            this.appName = bundleDesc.getApplication().getRegistrationName();
        }
        this.jarName = moduleDesc.getArchiveUri();
        this.componentName = srd.getName();
    }

    public ComponentNameConstructor(WebService wsDsc) {
        BundleDescriptor bundleDesc = wsDsc.getBundleDescriptor();
        ModuleDescriptor moduleDesc = bundleDesc.getModuleDescriptor();
        if(!moduleDesc.isStandalone()){ // print app name only for embedded ones
            this.appName = bundleDesc.getApplication().getRegistrationName();
        }
        this.jarName = moduleDesc.getArchiveUri();
        this.componentName = wsDsc.getName();
    }

    public ComponentNameConstructor(Application application) {
        this.appName = application.getRegistrationName();
    }

    public ComponentNameConstructor(PersistenceUnitDescriptor
            descriptor) {
        PersistenceUnitsDescriptor persistenceUnitsDescriptor =
                descriptor.getParent();
        RootDeploymentDescriptor container = persistenceUnitsDescriptor.getParent();
        if(container.isApplication()) {
            this.appName = Application.class.cast(container).getRegistrationName();
            this.componentName = persistenceUnitsDescriptor.getPuRoot() +
                    "#"+descriptor.getName(); // NOI18N
        } else { // this PU is bundled inside a module
            BundleDescriptor bundleDesc = BundleDescriptor.class.cast(container);
            ModuleDescriptor moduleDesc = bundleDesc.getModuleDescriptor();
            if(!moduleDesc.isStandalone()){ // print app name only for embedded ones
                this.appName = bundleDesc.getApplication().getRegistrationName();
            }
            this.jarName = moduleDesc.getArchiveUri();
            String puRoot = persistenceUnitsDescriptor.getPuRoot();
            // for EJB module, PURoot is empty, so to avoid ## in report, this check is needed.
            this.componentName = ("".equals(puRoot) ? "" : puRoot + "#") + descriptor.getName(); // NOI18N
        }
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        if(!isNullOrEmpty(appName)){
            sb.append(appName);
        }
        if(!isNullOrEmpty(jarName)){
            if(!isNullOrEmpty(appName)) sb.append("#"); // NOI18N
            sb.append(jarName);
        }
        if(!isNullOrEmpty(componentName)){
            if(!isNullOrEmpty(jarName) || !isNullOrEmpty(appName)) sb.append("#"); // NOI18N
            sb.append(componentName);
        }
        return sb.toString();
    }

    private static boolean isNullOrEmpty(String s) {
        return s == null || s.length() == 0;
    }

}
