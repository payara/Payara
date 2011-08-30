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

package com.sun.enterprise.deployment.util;

import com.sun.enterprise.deployment.*;
import com.sun.enterprise.deployment.types.*;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;

import org.glassfish.deployment.common.ModuleDescriptor;
import org.glassfish.deployment.common.Descriptor;
import org.glassfish.deployment.common.DescriptorVisitor;
import org.glassfish.deployment.common.XModuleType;
import org.jvnet.hk2.annotations.Service;

/**
 * This class is responsible for validating the loaded DOL classes and 
 * transform some of the raw XML information into refined values used 
 * by the DOL runtime
 *
 * @author Jerome Dochez
 */
@Service(name="application_deploy")
public class ApplicationValidator extends EjbBundleValidator 
    implements ApplicationVisitor, ManagedBeanVisitor {
    
    public void accept (BundleDescriptor descriptor) {
        if (descriptor instanceof Application) {
            Application application = (Application)descriptor;
            accept(application);

            for (BundleDescriptor ebd : application.getBundleDescriptorsOfType(XModuleType.EJB)) {
                ebd.visit(getSubDescriptorVisitor(ebd));
            }

            for (BundleDescriptor wbd : application.getBundleDescriptorsOfType(XModuleType.WAR)) {
                // This might be null in the case of an appclient
                // processing a client stubs .jar whose original .ear contained
                // a .war.  This will be fixed correctly in the deployment
                // stage but until then adding a non-null check will prevent
                // the validation step from bombing.
                if (wbd != null) {
                    wbd.visit(getSubDescriptorVisitor(wbd));
                }
            }

            for (BundleDescriptor cd :  application.getBundleDescriptorsOfType(XModuleType.RAR)) {
                cd.visit(getSubDescriptorVisitor(cd));
            }

            for (BundleDescriptor acd : application.getBundleDescriptorsOfType(XModuleType.CAR)) {
                acd.visit(getSubDescriptorVisitor(acd));
            }

            // Visit all injectables first.  In some cases, basic type
            // information has to be derived from target inject method or 
            // inject field.
            for(InjectionCapable injectable : application.getInjectableResources(application)) {
                accept(injectable);
            }

            super.accept(descriptor);
        } else {
            super.accept(descriptor);
        }
    }

    /**
     * visit an application object
     * @param application the application descriptor
     */
    public void accept(Application application) {
        this.application = application;
        if (application.getBundleDescriptors().size() == 0) {
            throw new IllegalArgumentException("Application [" + 
                application.getRegistrationName() + 
                "] contains no valid components");
        }

        // now resolve any conflicted module names in the application

        // list to store the current conflicted modules
        List<ModuleDescriptor> conflicted = new ArrayList<ModuleDescriptor>();
        // make sure all the modules have unique names
        Set<ModuleDescriptor<BundleDescriptor>> modules =
            application.getModules();
        for (ModuleDescriptor module : modules) {
            // if this module is already added to the conflicted list
            // no need to process it again
            if (conflicted.contains(module)) {
                continue;
            }
            boolean foundConflictedModule = false;
            for (ModuleDescriptor module2 : modules) {
                // if this module is already added to the conflicted list
                // no need to process it again
                if (conflicted.contains(module2)) {
                    continue;
                }
                if ( !module.equals(module2) && 
                    module.getModuleName().equals(module2.getModuleName())) {
                    conflicted.add(module2);
                    foundConflictedModule = true;
                }
            }
            if (foundConflictedModule) {
                conflicted.add(module);
            }
        }

        // append the conflicted module names with their module type to 
        // make the names unique
        for (ModuleDescriptor cModule : conflicted) {
            cModule.setModuleName(cModule.getModuleName() + 
                cModule.getModuleType().toString());
        }
    }

    /**
     * visits an ejb bundle descriptor
     * @param bundleDescriptor an ejb bundle descriptor
     */
    public void accept(EjbBundleDescriptor bundleDescriptor) {
        
        this.bundleDescriptor = bundleDescriptor;
        application = bundleDescriptor.getApplication();
        super.accept(bundleDescriptor);
        /** set the realm name on each ejb to match the ones on this application
         * this is required right now to pass the stringent CSIv2 criteria 
         * whereby the realm-name for the ejb being authenticated on 
         * has to match the one on the application. We look at the IORConfigurator
         * descriptor
         * @todo: change the csiv2 layer so that it does not look at 
         * IORConfiguratorDescriptor. 
         * @see iiop/security/SecurityMechanismSelector.evaluateClientConformance.
         */
        String rlm = application.getRealm();
        Iterator ejbs = bundleDescriptor.getEjbs().iterator();
        for(; ejbs.hasNext();){
            EjbDescriptor ejb = (EjbDescriptor) ejbs.next();
            Iterator iorconfig = ejb.getIORConfigurationDescriptors().iterator();
            for (;iorconfig.hasNext(); ){
                EjbIORConfigurationDescriptor desc = 
                    (EjbIORConfigurationDescriptor)iorconfig.next();
                if(rlm != null){
                    desc.setRealmName(rlm);
                }
            }
        }
    }

     public void accept(ManagedBeanDescriptor managedBean) {
        this.bundleDescriptor = managedBean.getBundle();
        this.application = bundleDescriptor.getApplication(); 

        for (Iterator itr = managedBean.getEjbReferenceDescriptors().iterator(); itr.hasNext();) {
            EjbReference aRef = (EjbReference) itr.next();
            accept(aRef);
        }

        for (Iterator it = managedBean.getResourceReferenceDescriptors().iterator(); it.hasNext();) {
            ResourceReferenceDescriptor next =
                    (ResourceReferenceDescriptor) it.next();
            accept(next);
        }

        for (Iterator it = managedBean.getJmsDestinationReferenceDescriptors().iterator(); it.hasNext();) {
            JmsDestinationReferenceDescriptor next =
                    (JmsDestinationReferenceDescriptor) it.next();
            accept(next);
        }

        for (Iterator it = managedBean.getMessageDestinationReferenceDescriptors().iterator(); it.hasNext();) {
            MessageDestinationReferencer next =
                    (MessageDestinationReferencer) it.next();
            accept(next);
        }

        Set serviceRefs = managedBean.getServiceReferenceDescriptors();
        for (Iterator itr = serviceRefs.iterator(); itr.hasNext();) {
            accept((ServiceReferenceDescriptor) itr.next());
        }
    }

    
    /**
     * @return a vector of EjbDescriptor for this bundle
     */
    protected Collection getEjbDescriptors() {
        if (application!=null) 
            return application.getEjbDescriptors();
        return new HashSet();
    }     
    
    /**
     * @return the Application object if any
     */
    protected Application getApplication() {
        return application;
    }
    
    /**
     * @return the bundleDescriptor we are validating
     */
    protected BundleDescriptor getBundleDescriptor() {
        return bundleDescriptor;
    }    

    /**
     * get the visitor for its sub descriptor
     * @param sub descriptor to return visitor for
     */
    public DescriptorVisitor getSubDescriptorVisitor(Descriptor subDescriptor) {
        if (subDescriptor instanceof BundleDescriptor) {
            return ((BundleDescriptor)subDescriptor).getBundleVisitor();
        }
        return super.getSubDescriptorVisitor(subDescriptor);
    }
}
