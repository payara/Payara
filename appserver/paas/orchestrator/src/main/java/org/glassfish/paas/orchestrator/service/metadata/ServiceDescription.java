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

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlType;
import java.util.List;

/**
 * @author bhavanishankar@java.net
 */
@XmlType(propOrder = {"templateOrCharacteristics", "configurations"})
public class ServiceDescription {

    private String name;
    private String initType;
    private String appName;

    // User can either specify which template to use or the characterstics of the template.
    private Object templateOrCharacteristics;

    private List<Property> configurations;

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

    //TODO  hack ?
    @XmlAttribute(name = "app-name", required = false)
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
        this.templateOrCharacteristics = templateOrCharacteristics;
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
        }
        // TODO :: retrieve the template using the template ID and identity the serviceType
        return null;
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
        return " \n[name=" + getName() + ", initType=" + getInitType()
                + (templateOrCharacteristics instanceof TemplateIdentifier ? ", template=" : ", characteristics = \n")
                + templateOrCharacteristics + ", configurations=" + configurations + "]";
    }
}
