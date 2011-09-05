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

package org.glassfish.paas.orchestrator.service.metadata;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 *
 * Holds both service description and service references.
 *
 * During deployment this object is initially used to read service descriptions
 * specified in the descriptor file. Later the implicitly discovered
 * service descriptions and references are added to this. At the end of scanning
 * of the archive, this object will hold ALL the required service description and
 * references.
 *
 * Currently the initial service descriptions are read from META-INF/glassfish-services.xml.
 * But this is a generic holder object that can hold service descriptions and references
 * from different descriptor file(s).
 *
 * @author bhavanishankar@java.net
 */
@XmlRootElement(name = "glassfish-services")
public class ServiceMetadata {

    private Set<ServiceDescription> serviceDescriptions;
    private Set<ServiceReference> serviceReferences = new HashSet<ServiceReference>();
    private String appName;

    
    @XmlElement(name = "service-description")
    public Set<ServiceDescription> getServiceDescriptions() {
        if(serviceDescriptions == null ){
            serviceDescriptions = new HashSet<ServiceDescription>();
        }
        return serviceDescriptions;
    }

    public void setServiceDescriptions(Set<ServiceDescription> serviceDescriptions) {
        this.serviceDescriptions = serviceDescriptions;
    }

    public void addServiceDescription(ServiceDescription sd) {
        getServiceDescriptions().add(sd);
    }

    @XmlTransient
    public Set<ServiceReference> getServiceReferences() {
        return serviceReferences;
    }

    public void setServiceReferences(Set<ServiceReference> serviceReferences) {
        this.serviceReferences = serviceReferences;
    }

    public void addServiceReference(ServiceReference sr) {
        getServiceReferences().add(sr);
    }

    @XmlTransient
    public String getAppName(){
        return appName;
    }

    public void setAppName(String appName){
        this.appName = appName;
    }

    @Override
    public String toString() {
        return super.toString()  + "\n [ServiceDescriptions = \n"
                + serviceDescriptions + " ]" + ". ServiceReferences = ["
                + serviceReferences + "]";
    }

}
