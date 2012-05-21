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

import javax.xml.bind.annotation.XmlElement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * @author bhavanishankar@java.net
 */

public class ServiceCharacteristics {

    private List<Property> serviceCharacteristics;

    public ServiceCharacteristics() {
        // default constructor required for JAXB
    }

    public ServiceCharacteristics(List<Property> characteristics) {
        setServiceCharacteristics(characteristics);
    }

    public ServiceCharacteristics(ServiceCharacteristics other) {
           cloneServiceCharacteristics(other);
    }

    @XmlElement(name="characteristic")
    public List<Property> getServiceCharacteristics() {
        return serviceCharacteristics;
    }

    public Properties all() {
        Properties props = new Properties();
        for (Property characteristic : getServiceCharacteristics()) {
            props.setProperty(characteristic.getName(), characteristic.getValue());
        }
        return props;
    }

    public void setServiceCharacteristics(List<Property> serviceCharacteristics) {
        this.serviceCharacteristics = serviceCharacteristics;
    }

    private void cloneServiceCharacteristics(ServiceCharacteristics other) {
        this.serviceCharacteristics=new ArrayList<Property>();
        for(Property property:other.getServiceCharacteristics()){
            this.serviceCharacteristics.add(new Property(property.getName(),property.getValue()));
        }
    }

    public String getCharacteristic(String name) {
        for (Property p : serviceCharacteristics) {
            if (p.getName().equals(name)) {
                return p.getValue();
            }
        }
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ServiceCharacteristics)) return false;

        ServiceCharacteristics sc = (ServiceCharacteristics) o;

        if (serviceCharacteristics != null ? !serviceCharacteristics.equals(sc.serviceCharacteristics) :
                sc.serviceCharacteristics != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        return serviceCharacteristics != null ? serviceCharacteristics.hashCode() : 0;
    }

    @Override
    public String toString() {
        return serviceCharacteristics != null ? serviceCharacteristics.toString() : super.toString();
    }
}
