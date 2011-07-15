/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.jbi.serviceengine.core;

import javax.xml.namespace.QName;

/**
 * Class which holds the endpoint information specified in jbi.xml of the 
 * service unit.
 *
 * @author Vikas Awasthi
 */
public class DescriptorEndpointInfo {
    /**
     * Interface name.
     */
    QName interfacename;

    /**
     * Service name.
     */
    QName servicename;

    /**
     * Endpoint name.
     */
    String endpointname;

    /**
     * Provider endpoint.
     */
    boolean provider = false;
    boolean jbiPrivate = false;
    boolean started = false;
    private String su_Name;

    public DescriptorEndpointInfo(String su_Name) {
        this.su_Name = su_Name;
    }

    /**
     * Sets the endpoint name.
     */
    public void setEndpointName(String epname) {
        endpointname = epname;
    }

    /**
     * Returns the endpoint name.
     */
    public String getEndpointName() {
        return endpointname;
    }

    /**
     * Sets the interface name.
     */
    public void setInterfaceName(QName intername) {
        interfacename = intername;
    }

    /**
     * Returns the interface name.
     */
    public QName getInterfaceName() {
        return interfacename;
    }

    /**
     * Sets the endpoint as provider.
     */
    public void setProvider() {
        provider = true;
    }

    /**
     * Returns true if the endpoint is provider.
     */
    public boolean isProvider() {
        return provider;
    }

    public boolean isStarted() {
        return started;
    }

    public void setStarted(boolean started) {
        this.started = started;
    }

    /**
     * Sets the service name.
     */
    public void setServiceName(QName sername) {
        servicename = sername;
    }

    /**
     * Returns the service name.
     */
    public QName getServiceName() {
        return servicename;
    }

    public String getKey() {
        return servicename.getLocalPart() + endpointname;
    }

    public String getSu_Name() {
        return su_Name;
    }

    public static String getDEIKey(QName serviceName, String endpointName) {
        return serviceName.getLocalPart() + endpointName;
    }
    
    public boolean isPrivate() {
        return jbiPrivate;
    }
    
    public void setPrivate(boolean b) {
        jbiPrivate = b;
    }
    
    public boolean equals(DescriptorEndpointInfo other) {
        if(getKey().equals(other.getKey())) {
            return true;
        }
        return false;
    }
    
}
