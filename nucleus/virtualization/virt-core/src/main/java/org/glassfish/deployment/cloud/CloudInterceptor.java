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

package org.glassfish.deployment.cloud;

import org.glassfish.api.ActionReport;
import org.glassfish.api.deployment.DeploymentContext;
import org.glassfish.api.deployment.archive.ReadableArchive;
import org.glassfish.deployment.admin.DeployCommand;
import org.glassfish.deployment.common.DeploymentException;
import org.glassfish.hk2.Services;
import org.glassfish.virtualization.runtime.VirtualCluster;
import org.glassfish.virtualization.runtime.VirtualClusters;
import org.glassfish.virtualization.spi.*;
import org.glassfish.virtualization.util.RuntimeContext;
import org.glassfish.virtualization.util.ServiceType;
import org.glassfish.virtualization.util.VirtualizationType;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.Habitat;
import org.jvnet.hk2.component.PerLookup;
import org.jvnet.hk2.config.*;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by IntelliJ IDEA.
 * User: dochez
 * Date: 3/2/11
 * Time: 3:18 PM
 * To change this template use File | Settings | File Templates.
 */
@Service
@Scoped(PerLookup.class)
public class CloudInterceptor implements DeployCommand.Interceptor {

    @Inject
    Habitat habitat;

    @Inject
    RuntimeContext rtContext;

    @Inject(name="plain")
    ActionReport actionReport;

    @Inject
    Services services;

    @Inject
    IAAS iaas;

    @Inject
    VirtualClusters virtualClusters;

    @Override
    public void intercept(DeployCommand command, DeploymentContext context) {
        System.out.println("Interceptor called for ..." + command.name());
        boolean clusterCreated = false;
        try {
            if (context.getSource().exists("META-INF/cloud.xml")) {
                System.out.println("This is a virtual application !");
                CloudApplication cloudApplication = readConfig(context.getSource());
                for (CloudService cloudService : cloudApplication.getServices().getServices()) {
                    if (cloudService instanceof JavaEEService) {
                        JavaEEService javaEE = (JavaEEService) cloudService;
                        System.out.println("Applications wants " + javaEE.getMinInstances() + " Java EE instances");
                        rtContext.executeAdminCommand(actionReport, "create-virtual-cluster", command.name(), "min", javaEE.getMinInstances());
                        if (actionReport.hasFailures()) {
                            throw new DeploymentException(actionReport.getMessage());
                        }
                        clusterCreated = true;
                        command.target = command.name();
                    } else if (cloudService instanceof DatabaseService) {
                        try {
                            VirtualCluster virtualCluster = virtualClusters.byName(command.name());
                            for (ServerPool serverPool : iaas) {
                                String virtTypeName = serverPool.getConfig().getVirtualization().getName();
                                VirtualizationType virtType = new VirtualizationType(virtTypeName);
                                ServiceType serviceType = new ServiceType("Database");
                                TemplateRepository templateRepository = services.forContract(TemplateRepository.class).get();
                                for (TemplateInstance ti : templateRepository.all()) {
                                    if (ti.satisfies(virtType) && (ti.satisfies(serviceType))) {
                                        VMOrder vmOrder = new VMOrder(ti, virtualCluster );
                                        iaas.allocate(vmOrder, null);
                                    }
                                }

                            }
                        } catch (VirtException e) {
                            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                        }
                    }

                }
            }
        } catch(IOException e) {
            e.printStackTrace();
            if (clusterCreated) {
                rtContext.executeAdminCommand(actionReport, "delete-virtual-cluster", command.name());
            }
        }
    }

    private CloudApplication readConfig(ReadableArchive archive) throws IOException {
        InputStream is=null;

        try {
            is = archive.getEntry("META-INF/cloud.xml");
            if (is==null) return null;

            final XMLStreamReader reader;
            try {
                reader = XMLInputFactory.newInstance().createXMLStreamReader(is);
            } catch (XMLStreamException e) {
                e.printStackTrace();
                return null;
            }

            final ConfigParser configParser = new ConfigParser(habitat);
            DomDocument document =  configParser.parse(reader);
            return document.getRoot().createProxy(CloudApplication.class);

       } catch(Exception e) {
            if (is!=null) {
                try { is.close(); } catch(Exception ex) { // ignore
                }
                throw new IOException(e);
            }
       }
       return null;
    }
}
