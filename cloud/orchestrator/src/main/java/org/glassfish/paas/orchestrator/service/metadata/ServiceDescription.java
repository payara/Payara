/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2012 Oracle and/or its affiliates. All rights reserved.
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

import org.glassfish.internal.api.Globals;
import org.glassfish.paas.orchestrator.provisioning.ServiceScope;
import org.glassfish.paas.orchestrator.service.spi.ServicePlugin;
import org.glassfish.virtualization.spi.TemplateInstance;
import org.glassfish.virtualization.spi.TemplateRepository;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;
import java.util.ArrayList;
import java.util.List;

/**
 * @author bhavanishankar@java.net
 */
@XmlType(propOrder = {"templateOrCharacteristics", "configurations"})
public class ServiceDescription {

    private String name;
    private String initType;
    private String appName;
    private String virtualClusterName;
    private ServiceScope serviceScope;
    private String serviceType;

    // User can either specify which template to use or the characteristics of the template.
    private Object templateOrCharacteristics;

    private List<Property> configurations = new ArrayList<Property>();
    private ServicePlugin plugin;

    public ServiceDescription() {
        // default constructor required for JAXB.
    }

    public ServiceDescription(String name, String appName, String initType,
                              Object templateOrCharacteristics, List<Property> configurations) {
        setName(name);
        setAppName(appName);
        setInitType(initType);
        setTemplateOrCharacteristics(templateOrCharacteristics);
        setConfigurations(configurations);
    }

    public ServiceDescription(ServiceDescription other) { // clone the service description
        setName(other.getName());
        setAppName(other.getAppName());
        setVirtualClusterName(other.getVirtualClusterName());
        setInitType(other.getInitType());
        cloneTemplateOrCharacteristics(other.getTemplateOrCharacteristics());
        cloneConfigurations(other.getConfigurations());
    }

    @XmlAttribute(name = "name", required = true)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @XmlAttribute(name = "init-type")
    public String getInitType() {
        return initType;
    }

    @XmlTransient
    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public void setInitType(String initType) {
        this.initType = initType;
    }

    @XmlElementWrapper(name = "configurations")
    @XmlElement(name = "configuration")
    public List<Property> getConfigurations() {
        return configurations;
    }

    public void setConfigurations(List<Property> configurations) {
        this.configurations = configurations;
    }

    @XmlElements(value = {
            @XmlElement(name = "template", type = TemplateIdentifier.class),
            @XmlElement(name = "characteristics", type = ServiceCharacteristics.class)
    })
    public Object getTemplateOrCharacteristics() {
        return templateOrCharacteristics;
    }

    public void setTemplateOrCharacteristics(Object templateOrCharacteristics) {
        //for an external-service, neither template nor characteristics will be applicable.
        if (templateOrCharacteristics != null) {
            if (templateOrCharacteristics instanceof TemplateIdentifier ||
                    templateOrCharacteristics instanceof ServiceCharacteristics) {
                this.templateOrCharacteristics = templateOrCharacteristics;
            } else {
                throw new RuntimeException("Invalid type [" + templateOrCharacteristics.getClass() + "], " +
                        "neither TemplateIdentifier nor ServiceCharacteristics");
            }
        }
    }

    public void cloneTemplateOrCharacteristics(Object templateOrCharacteristics) {
        //for an external-service, neither template nor characteristics will be applicable.
        if (templateOrCharacteristics != null) {
            if(templateOrCharacteristics instanceof TemplateIdentifier){
                this.templateOrCharacteristics=new TemplateIdentifier();
                ((TemplateIdentifier)this.templateOrCharacteristics).setId(((TemplateIdentifier) templateOrCharacteristics).getId());

            }else if(templateOrCharacteristics instanceof ServiceCharacteristics){
                this.templateOrCharacteristics=new ServiceCharacteristics((ServiceCharacteristics)templateOrCharacteristics);
            }else{
               throw new RuntimeException("Invalid type [" + templateOrCharacteristics.getClass() + "], " +
                        "neither TemplateIdentifier nor ServiceCharacteristics");
            }
        }
    }

    private void cloneConfigurations(List<Property> configurations) {
        for(Property property:configurations){
            this.configurations.add(new Property(property.getName(),property.getValue()));
        }
    }

    public TemplateIdentifier getTemplateIdentifier() {
        if (getTemplateOrCharacteristics() instanceof TemplateIdentifier) {
            return (TemplateIdentifier) getTemplateOrCharacteristics();
        }
        return null;
    }

    public ServiceCharacteristics getServiceCharacteristics() {
        if (getTemplateOrCharacteristics() instanceof ServiceCharacteristics) {
            return (ServiceCharacteristics) getTemplateOrCharacteristics();
        }
        return null;
    }

    /**
     * The service type is either a property in <characteristics> or
     * should be figured out from the service template.
     *
     * @return Type of the service. Eg., JavaEE, RDBMS, JMS, etc.
     */
    public String getServiceType() {
        if (templateOrCharacteristics instanceof ServiceCharacteristics) {
            List<Property> characteristics =
                    ((ServiceCharacteristics) templateOrCharacteristics).getServiceCharacteristics();
            for (Property characteristic : characteristics) {
                if (characteristic.getName().equals("service-type")) {
                    return characteristic.getValue();
                }
            }
        } else if (templateOrCharacteristics instanceof TemplateIdentifier) {
            // TODO :: add service-type as an attribute of service-description.
            // TODO :: for now, compute the service-type using template repository, search, etc...
            TemplateIdentifier templateId = (TemplateIdentifier) templateOrCharacteristics;
            TemplateRepository templateRepository =
                    Globals.getDefaultHabitat().getByContract(TemplateRepository.class);
            TemplateInstance templateInstance =
                    templateRepository.byName(templateId.getId());
            if(templateInstance != null){
                return templateInstance.getConfig().byName("ServiceType").getValue();
            }
        }
        return serviceType;
    }

    @XmlTransient
    public void setServiceType(String serviceType){ //used for external-service
        this.serviceType = serviceType;
    }


    public String getConfiguration(String configKey) {
        for (Property p : getConfigurations()) {
            if (p.getName().equals(configKey)) {
                return p.getValue();
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return "{name = " + getName() + ", init-type = " + getInitType()
                + (templateOrCharacteristics instanceof TemplateIdentifier ? ", template-id = " : ", service-characteristics = ")
                + templateOrCharacteristics + ", service-configurations = " + configurations + "}";
    }

    @XmlTransient
    public String getVirtualClusterName() {
        return virtualClusterName;
    }

    public void setVirtualClusterName(String virtualClusterName) {
        this.virtualClusterName = virtualClusterName;
    }

    @XmlTransient
    public ServicePlugin getPlugin(){
        return plugin;
    }

    public void setPlugin(ServicePlugin plugin){
        this.plugin = plugin;
    }

    @XmlTransient
    public ServiceScope getServiceScope(){
        return serviceScope;
    }

    public void setServiceScope(ServiceScope serviceScope){
        this.serviceScope = serviceScope;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ServiceDescription)) return false;

        ServiceDescription sd = (ServiceDescription) o;

        if (appName != null ? !appName.equals(sd.appName) : sd.appName != null) return false;
        if (configurations != null ? !configurations.equals(sd.configurations) : sd.configurations != null)
            return false;
        if (initType != null ? !initType.equals(sd.initType) : sd.initType != null) return false;
        if (name != null ? !name.equals(sd.name) : sd.name != null) return false;
        if (plugin != null ? !plugin.equals(sd.plugin) : sd.plugin != null) return false;
        if (serviceScope != sd.serviceScope) return false;
        if (serviceType != null ? !serviceType.equals(sd.serviceType) : sd.serviceType != null) return false;
        if (templateOrCharacteristics != null ? !templateOrCharacteristics.equals(sd.templateOrCharacteristics) :
                sd.templateOrCharacteristics != null)
            return false;
        if (virtualClusterName != null ? !virtualClusterName.equals(sd.virtualClusterName) : sd.virtualClusterName != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (initType != null ? initType.hashCode() : 0);
        result = 31 * result + (appName != null ? appName.hashCode() : 0);
        result = 31 * result + (virtualClusterName != null ? virtualClusterName.hashCode() : 0);
        result = 31 * result + (serviceScope != null ? serviceScope.hashCode() : 0);
        result = 31 * result + (serviceType != null ? serviceType.hashCode() : 0);
        result = 31 * result + (templateOrCharacteristics != null ? templateOrCharacteristics.hashCode() : 0);
        result = 31 * result + (configurations != null ? configurations.hashCode() : 0);
        result = 31 * result + (plugin != null ? plugin.hashCode() : 0);
        return result;
    }
}
