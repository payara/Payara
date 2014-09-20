/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
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

import com.sun.enterprise.deployment.Application;
import com.sun.enterprise.deployment.BundleDescriptor;
import com.sun.enterprise.deployment.EnvironmentProperty;
import com.sun.enterprise.deployment.ServiceReferenceDescriptor;
import com.sun.enterprise.deployment.WebService;
import com.sun.enterprise.deployment.types.EjbReference;
import com.sun.enterprise.deployment.types.MessageDestinationReferencer;
import org.glassfish.deployment.common.Descriptor;
import org.glassfish.deployment.common.DescriptorVisitor;

/**
 *
 * @author  dochez
 * @version 
 */
public class TracerVisitor extends DefaultDOLVisitor implements ApplicationVisitor {

    @Override
    public void accept (BundleDescriptor descriptor) {
        if (descriptor instanceof Application) {
            Application application = (Application)descriptor;
            accept(application);

            for (BundleDescriptor ebd : application.getBundleDescriptorsOfType(DOLUtils.ejbType())) {
                ebd.visit(getSubDescriptorVisitor(ebd));
            }

            for (BundleDescriptor wbd : application.getBundleDescriptorsOfType(DOLUtils.warType())) {
                // This might be null in the case of an appclient
                // processing a client stubs .jar whose original .ear contained
                // a .war.  This will be fixed correctly in the deployment
                // stage but until then adding a non-null check will prevent
                // the validation step from bombing.
                if (wbd != null) {
                    wbd.visit(getSubDescriptorVisitor(wbd));
                }
            }

            for (BundleDescriptor cd :  application.getBundleDescriptorsOfType(DOLUtils.rarType())) {
                cd.visit(getSubDescriptorVisitor(cd));
            }

            for (BundleDescriptor acd : application.getBundleDescriptorsOfType(DOLUtils.carType())) {
               acd.visit(getSubDescriptorVisitor(acd));
            }
            super.accept(descriptor);
        } else {
            super.accept(descriptor);
        }
    }

    /**
     * visit an application object
     * @param the application descriptor
     */
    @Override
    public void accept(Application application) {
	DOLUtils.getDefaultLogger().info("Application");
	DOLUtils.getDefaultLogger().info("name " + application.getName());
	DOLUtils.getDefaultLogger().info("smallIcon " + application.getSmallIconUri());
    }


    /**
     * visits an ejb reference  for the last J2EE component visited
     * @param the ejb reference
     */
    @Override
    protected void accept(EjbReference ejbRef) {
        DOLUtils.getDefaultLogger().info(ejbRef.toString());
    }

    @Override
    protected void accept(MessageDestinationReferencer referencer) {
        DOLUtils.getDefaultLogger().info(referencer.getMessageDestinationLinkName());
    }
    
    protected void accept(WebService webService) {
        DOLUtils.getDefaultLogger().info(webService.getName());
    }

    @Override
    protected void accept(ServiceReferenceDescriptor serviceRef) {
        DOLUtils.getDefaultLogger().info(serviceRef.getName());
    }

    protected void accept(EnvironmentProperty envEntry) {
        DOLUtils.getDefaultLogger().info( envEntry.toString());
    }

    /**
     * visits a J2EE descriptor
     * @param the descriptor
     */
    @Override
    public void accept(Descriptor descriptor) {
        DOLUtils.getDefaultLogger().info(descriptor.toString());
    } 

    /**
     * get the visitor for its sub descriptor
     * @param sub descriptor to return visitor for
     */
    @Override
    public DescriptorVisitor getSubDescriptorVisitor(Descriptor subDescriptor) {
        if (subDescriptor instanceof BundleDescriptor) {
            DescriptorVisitor tracerVisitor = ((BundleDescriptor)subDescriptor).getTracerVisitor();
            if (tracerVisitor == null) {
                return this;
            }
            return tracerVisitor;
        }
        return super.getSubDescriptorVisitor(subDescriptor);
    }
}
