/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.paas.gfplugin;

import com.sun.enterprise.deployment.*;
import com.sun.enterprise.deployment.archivist.ApplicationFactory;
import org.glassfish.api.deployment.archive.ReadableArchive;
import org.glassfish.internal.data.ApplicationInfo;
import org.glassfish.internal.data.ApplicationRegistry;
import org.glassfish.paas.orchestrator.service.metadata.ServiceReference;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Service;

import java.util.*;

/**
 * This processes the orchestration archive and figures out all the GlassFish
 * service references.
 *
 * @author bhavanishankar@java.net
 */
@Service
public class GlassFishCloudArchiveProcessor {


    @Inject
    private ApplicationFactory applicationFactory;


    @Inject
    private ApplicationRegistry appRegistry;

    public Set<ServiceReference> getServiceReferences(
            ReadableArchive cloudArchive, String appName) {
        Set<ServiceReference> serviceReferences = new HashSet<ServiceReference>();
        Set<ResourceReferenceDescriptor> resRefs = new HashSet<ResourceReferenceDescriptor>();
        try {
            //Application application = applicationFactory.openArchive(cloudArchive.getURI());
            ApplicationInfo appInfo = appRegistry.get(appName);
            Application application = appInfo.getMetaData(Application.class);
            if(application == null){
                return serviceReferences;
            }

            //determine persistence.xml's data-source dependencies
            scanAndPopulateServiceRefsForDSRefsInPUs(serviceReferences, application);

            //TODO ideally, we should be scanning any descriptor that will have resource-ref in a generic manner.
//            boolean isDistributable = false;
            for (WebBundleDescriptor descriptor : application.getBundleDescriptors(WebBundleDescriptor.class)) {
//                if (descriptor.isDistributable()) {
//                    isDistributable = true;
//                }
                resRefs.addAll(descriptor.getResourceReferenceDescriptors());
            }
//            if (isDistributable) {
//                serviceReferences.add(new ServiceReference(cloudArchive.getName() + "-lb", "LB", null));
//            }
            for (EjbBundleDescriptor descriptor : application.getBundleDescriptors(EjbBundleDescriptor.class)) {
                resRefs.addAll(descriptor.getResourceReferenceDescriptors());
            }
            for (ApplicationClientDescriptor descriptor : application.getBundleDescriptors(ApplicationClientDescriptor.class)) {
                resRefs.addAll(descriptor.getResourceReferenceDescriptors());
            }

            // make sure duplicate resource-refs (eg: @Resource(name="xyz", mappedName="jdbc/foo")
            // and <resource-ref>jdbc/foo</resource-ref>
            // are filtered out.
            Set<String> resRefNames = new HashSet<String>();
            Set<ResourceReferenceDescriptor> mappedNameSet = new HashSet<ResourceReferenceDescriptor>();
            Set<ResourceReferenceDescriptor> filteredResRefSet = new HashSet<ResourceReferenceDescriptor>();

            for(ResourceReferenceDescriptor resRef : resRefs){
                if(resRef.getMappedName() != null && !resRef.getMappedName().isEmpty()){
                    mappedNameSet.add(resRef);
                    continue;
                }
                if(resRef.getJndiName() != null){
                    resRefNames.add(resRef.getJndiName());
                    filteredResRefSet.add(resRef);
                }
            }

            for(ResourceReferenceDescriptor resRef : mappedNameSet){
                if(!resRefNames.contains(resRef.getMappedName())){
                    filteredResRefSet.add(resRef);
                }
            }

            // TODO :: Get the explicit service references from META-INF/glassfish-resources.xml
            for (ResourceReferenceDescriptor resRef : filteredResRefSet) {
                serviceReferences.add(new ServiceReference(resRef.getName(),
                        resRef.getType(), null, resRef.getSchemaGeneratorProperties()));
            }

            //add an implicit service-reference of type "glassfish"/"javaee" so that
            //glassfish plugin will always get called during association/dissociation phase
            //This will be used to set "target" during "deploy" and "undeploy"
            //serviceReferences.add(new ServiceReference(cloudArchive.getName(),
            //        GlassFishPlugin.JAVAEE_SERVICE_TYPE, null, null));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return serviceReferences;
    }

    private void scanAndPopulateServiceRefsForDSRefsInPUs(Set<ServiceReference> serviceReferences, Application application) {
        Set<BundleDescriptor> bundleDescriptors = application.getBundleDescriptors();
        Set<String> jndiNames = new HashSet<String>();
        if(bundleDescriptors != null){
            for(BundleDescriptor bundleDescriptor : bundleDescriptors){
                Collection<? extends PersistenceUnitDescriptor> puDescriptors = bundleDescriptor.findReferencedPUs();
                for(PersistenceUnitDescriptor pud : puDescriptors){
                    if(pud.getJtaDataSource() != null){
                        jndiNames.add(pud.getJtaDataSource());
                    }
                    if(pud.getNonJtaDataSource() != null){
                        jndiNames.add(pud.getNonJtaDataSource());
                    }
                }
            }
        }

        for(String jndiName : jndiNames){
            serviceReferences.add(new ServiceReference(jndiName, "javax.sql.DataSource", null));
        }
    }
}
