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

import org.glassfish.paas.orchestrator.service.spi.ServicePlugin;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlTransient;
import java.util.Properties;

/**
 * Indicates the binding between a service reference in an application component
 * (such as a resource-ref in sun-web.xml) and a service definition.
 *
 * @author Sivakumar Thyagarajan
 */
public class ServiceReference {

    //a unique identifier for the service reference.
    //for instance, this could be a resource manager connection factory 
    //jndi name used by a component in the application
    //must not be null
    //Example: "jdbc/FooDataSource"
    private String name;

    //this must point to a service-definition's id or the name of shared or external-service
    //This may or may not be specified.
    //If unspecified, the orchestrator would create a default service-definition
    //for this service-reference and set that as the target (serviceName)
    //Example: ""(empty) or "Department1-MySQLDatabase"
    private String serviceName;

    //reference type for this service reference. 
    //For instance, this could be the resource manager connection factory type
    //Example: "javax.sql.DataSource"
    private String referenceType;

    // Since the service can be referenced by a resource, we need to hold the resource properties.
    // Eg., If service is referenced from jdbc-connection-pool, this will hold all the pool properties.
    // TODO :: instead of storing resource properties, we can directly have a reference to the resource itself.
    private Properties properties = new Properties();

    //Plugin that requested this service-reference.
    private ServicePlugin requestingPlugin = null;

    public ServiceReference() {}
    
    public ServiceReference(String refId, String refType, String serviceName) {
        this.name = refId;
        this.serviceName = serviceName;
        this.referenceType = refType;
    }

    public ServiceReference(String refId, String refType,
                            String serviceName, Properties properties) {
        this.name = refId;
        this.serviceName = serviceName;
        this.referenceType = refType;
        this.setProperties(properties);
        if(properties != null && properties.getProperty("serviceName") != null) {
            this.serviceName = properties.getProperty("serviceName");
        }
    }

    @XmlAttribute(name = "name", required=true)
    public String getName() {
        return name;
    }

    public void setName(String name){
        this.name = name;
    }

    @XmlAttribute(name = "service-name", required = true)
    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName){
        this.serviceName = serviceName;
    }

    @XmlAttribute(name = "type")
    public String getType() {
        return referenceType;
    }

    public void setType(String type){
        this.referenceType = type;
    }

    public void setRequestingPlugin(ServicePlugin plugin){
        this.requestingPlugin = plugin;
    }

    @XmlTransient
    public ServicePlugin getRequestingPlugin(){
        return requestingPlugin;
    }

    @Override
    public String toString() {
        return "ServiceReference [ name = " + name + ", referenceType = " + referenceType +
                ", serviceName = " + serviceName + " ]";
    }

    public Properties getProperties() {
        return properties;
    }

    public void setProperties(Properties properties) {
        this.properties = properties;
    }
}
