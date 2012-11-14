/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.deployment;


import org.glassfish.deployment.common.Descriptor;
import org.glassfish.deployment.common.JavaEEResourceType;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: naman
 * Date: 24/5/12
 * Time: 11:24 AM
 * To change this template use File | Settings | File Templates.
 */
public abstract class CommonResourceBundleDescriptor  extends BundleDescriptor {

    CommonResourceFunctionality commonResourceFunctionality = new CommonResourceFunctionality();
    Set<Descriptor> dsdDescriptors = new HashSet<Descriptor>();
    Set<Descriptor> msdDescriptors = new HashSet<Descriptor>();
    Set<Descriptor> crdDescriptors = new HashSet<Descriptor>();
    Set<Descriptor> aodDescriptors = new HashSet<Descriptor>();
    Set<Descriptor> jmscfdDescriptors = new HashSet<Descriptor>();
    Set<Descriptor> jmsddDescriptors = new HashSet<Descriptor>();
    Set<Descriptor> allDescriptors = new HashSet<Descriptor>();

    public CommonResourceBundleDescriptor() {
        super();
    }

    public CommonResourceBundleDescriptor(String name, String description) {
        super(name, description);
    }

    public void addResourceDescriptor(Descriptor descriptor) {
        switch (descriptor.getResourceType()){
            case DSD:
                commonResourceFunctionality.addDataSourceDefinitionDescriptor((DataSourceDefinitionDescriptor)descriptor);
                break;
            case MSD:
                commonResourceFunctionality.addMailSessionDescriptor((MailSessionDescriptor)descriptor);
                break;
            case CRD:
                commonResourceFunctionality.addConnectorResourceDefinitionDescriptor((ConnectorResourceDefinitionDescriptor)descriptor);
                break;
            case AODD:
                commonResourceFunctionality.addAdministeredObjectDefinitionDescriptor((AdministeredObjectDefinitionDescriptor)descriptor);
                break;
            case JMSCFDD:
                commonResourceFunctionality.addJMSConnectionFactoryDefinitionDescriptor((JMSConnectionFactoryDefinitionDescriptor)descriptor);
                break;
            case JMSDD:
                commonResourceFunctionality.addJMSDestinationDefinitionDescriptor((JMSDestinationDefinitionDescriptor)descriptor);
                break;
        }
    }

    public void removeResourceDescriptor(Descriptor descriptor) {
        switch (descriptor.getResourceType()) {
            case DSD:
                commonResourceFunctionality.removeDataSourceDefinitionDescriptor((DataSourceDefinitionDescriptor)descriptor);
                break;
            case MSD:
                commonResourceFunctionality.removeMailSessionDescriptor((MailSessionDescriptor)descriptor);
                break;
            case CRD:
                commonResourceFunctionality.removeConnectorResourceDefinitionDescriptor((ConnectorResourceDefinitionDescriptor)descriptor);
                break;
            case AODD:
                commonResourceFunctionality.removeAdministeredObjectDefinitionDescriptor((AdministeredObjectDefinitionDescriptor)descriptor);
                break;
            case JMSCFDD:
                commonResourceFunctionality.removeJMSConnectionFactoryDefinitionDescriptor((JMSConnectionFactoryDefinitionDescriptor)descriptor);
                break;
            case JMSDD:
                commonResourceFunctionality.removeJMSDestinationDefinitionDescriptor((JMSDestinationDefinitionDescriptor)descriptor);
                break;
        }
    }

    public Set<Descriptor> getResourceDescriptors(JavaEEResourceType type) {
        switch (type) {
            case DSD:
                Set<DataSourceDefinitionDescriptor> dataSourceDefinitionDescriptors = commonResourceFunctionality.getDataSourceDefinitionDescriptors();
                dsdDescriptors.addAll(dataSourceDefinitionDescriptors);
                return dsdDescriptors;
            case MSD:
                Set<MailSessionDescriptor> mailSessionDescriptors = commonResourceFunctionality.getMailSessionDescriptors();
                msdDescriptors.addAll(mailSessionDescriptors);
                return msdDescriptors;
            case CRD:
                Set<ConnectorResourceDefinitionDescriptor> connectorResourceDefinitionDescriptors = commonResourceFunctionality.getConnectorResourceDefinitionDescriptors();
                crdDescriptors.addAll(connectorResourceDefinitionDescriptors);
                return crdDescriptors;
            case AODD:
                Set<AdministeredObjectDefinitionDescriptor> administeredObjectDefinitionDescriptors = commonResourceFunctionality.getAdministeredObjectDefinitionDescriptors();
                aodDescriptors.addAll(administeredObjectDefinitionDescriptors);
                return aodDescriptors;
            case JMSCFDD:
                Set<JMSConnectionFactoryDefinitionDescriptor> jmsConnectionFactoryDefinitionDescriptors = commonResourceFunctionality.getJMSConnectionFactoryDefinitionDescriptors();
                jmscfdDescriptors.addAll(jmsConnectionFactoryDefinitionDescriptors);
                return jmscfdDescriptors;
            case JMSDD:
                Set <JMSDestinationDefinitionDescriptor> jmsDestinationDefinitionDescriptors = commonResourceFunctionality.getJMSDestinationDefinitionDescriptors();
                jmsddDescriptors.addAll(jmsDestinationDefinitionDescriptors);
                return jmsddDescriptors;
            case ALL:
                allDescriptors.addAll(commonResourceFunctionality.getJMSDestinationDefinitionDescriptors());
                allDescriptors.addAll(commonResourceFunctionality.getJMSConnectionFactoryDefinitionDescriptors());
                allDescriptors.addAll(commonResourceFunctionality.getAdministeredObjectDefinitionDescriptors());
                allDescriptors.addAll(commonResourceFunctionality.getConnectorResourceDefinitionDescriptors());
                allDescriptors.addAll(commonResourceFunctionality.getMailSessionDescriptors());
                allDescriptors.addAll(commonResourceFunctionality.getDataSourceDefinitionDescriptors());
                return allDescriptors;
        }
        return null;
    }

    protected Descriptor getResourceDescriptor(JavaEEResourceType type, String name) {
        Descriptor descriptor = null;
        switch (type) {
            case DSD:
                descriptor = commonResourceFunctionality.getDataSourceDefinitionDescriptor(name);
                break;
            case MSD:
                descriptor = commonResourceFunctionality.getMailSessionDescriptor(name);
                break;
            case CRD:
                descriptor = commonResourceFunctionality.getConnectorResourceDefinitionDescriptor(name);
                break;
            case AODD:
                descriptor = commonResourceFunctionality.getAdministeredObjectDefinitionDescriptor(name);
                break;
            case JMSCFDD:
                descriptor = commonResourceFunctionality.getJMSConnectionFactoryDefinitionDescriptor(name);
                break;
            case JMSDD:
                descriptor = commonResourceFunctionality.getJMSDestinationDefinitionDescriptor(name);
                break;
        }
        return descriptor;
    }
}

