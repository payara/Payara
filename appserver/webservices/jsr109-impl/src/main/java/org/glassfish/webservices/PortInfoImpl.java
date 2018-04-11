/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2011 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.webservices;

import com.sun.xml.ws.api.BindingID;

import javax.xml.namespace.QName;
import javax.xml.ws.handler.PortInfo;

/**
 * Implementation of the PortInfo interface. This is just a simple
 * class used to hold the info necessary to uniquely identify a port,
 * including the port name, service name, and binding ID. This class
 * is only used on the client side.
 */
public class PortInfoImpl implements PortInfo {
    
    private BindingID bindingId;
    private QName portName;
    private QName serviceName;
        
    public PortInfoImpl(BindingID bindingId, QName portName, QName serviceName) {
        this.bindingId = bindingId;
        this.portName = portName;
        this.serviceName = serviceName;
    }

    @Override
    public String getBindingID() {
        return bindingId.toString();
    }

    @Override
    public QName getPortName() {
        return portName;
    }

    @Override
    public QName getServiceName() {
        return serviceName;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof PortInfo) {
            PortInfo info = (PortInfo) obj;
            if (bindingId.toString().equals(info.getBindingID()) &&
                portName.equals(info.getPortName()) &&
                serviceName.equals(info.getServiceName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Needed by JAXWS so PortInfoImpl can be used as a key in a map..
     */
    @Override
    public int hashCode() {
        return bindingId.toString().hashCode();
    }    
}
