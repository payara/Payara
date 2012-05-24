/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2012 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.webservices.deployment;

import org.glassfish.gmbal.ManagedData;
import org.glassfish.gmbal.Description;
import org.glassfish.gmbal.ManagedAttribute;
import org.glassfish.external.statistics.impl.StatisticImpl;
import com.sun.enterprise.deployment.WebServiceEndpoint;
import com.sun.enterprise.deployment.Application;
import com.sun.xml.ws.api.server.WSEndpoint;
import com.sun.xml.ws.transport.http.servlet.ServletAdapter;

import javax.xml.namespace.QName;
import java.util.Collection;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;

/**
 * 109 and sun-jaxws.xml style deployed endpoint's info.
 *
 * @author Jitendra Kotamraju
 */
@ManagedData
@Description("109 deployed endpoint info")
public class DeployedEndpointData extends StatisticImpl implements Map {

    @ManagedAttribute
    @Description("Application Name")
    public final String appName;

    @ManagedAttribute
    @Description("Endpoint Name")
    public final String endpointName;

    @ManagedAttribute
    @Description("Target Namespace of the Web Service")
    public final String namespace;

    @ManagedAttribute
    @Description("Web Service name")
    public final String serviceName;

    @ManagedAttribute
    @Description("Web Service port name")
    public final String portName;

    @ManagedAttribute
    @Description("Service Implementation Class")
    public final String implClass;

    @ManagedAttribute
    @Description("Address for Web Service")
    public final String address;

    @ManagedAttribute
    @Description("WSDL for Web Service")
    public final String wsdl;

    @ManagedAttribute
    @Description("Tester for Web Service")
    public final String tester;

    @ManagedAttribute
    @Description("Implementation Type: EJB or SERVLET")
    public final String implType;

    @ManagedAttribute
    @Description("Deployment Type: 109 or RI")
    public final String deploymentType;

    private Map<String, String> infoMap = new HashMap<String, String>();

    // 109 deployed endpoint
    public DeployedEndpointData(String path, Application app, WebServiceEndpoint endpoint) {
        super(path, "", "");
        this.appName = app.getAppName();
        this.endpointName = endpoint.getEndpointName();
        this.namespace = endpoint.getServiceName().getNamespaceURI();
        this.serviceName = endpoint.getServiceName().getLocalPart();
        QName pName = endpoint.getWsdlPort();
        this.portName = (pName != null) ? pName.getLocalPart() : "";
        this.implClass = endpoint.implementedByEjbComponent()
                ? endpoint.getEjbComponentImpl().getEjbImplClassName()
                : endpoint.getServletImplClass();
        this.address = path;
        this.wsdl = address+"?wsdl";
        this.tester = address+"?Tester";
        this.implType = endpoint.implementedByEjbComponent() ? "EJB" : "SERVLET";
        this.deploymentType = "109";
        fillStatMap();
    }

    // sun-jaxws.xml deployed endpoint
    public DeployedEndpointData(String path, ServletAdapter adapter) {
        super(path, "", "");
        WSEndpoint endpoint = adapter.getEndpoint();

        this.appName = "";
        this.endpointName = adapter.getName();
        this.namespace = endpoint.getServiceName().getNamespaceURI();
        this.serviceName = endpoint.getServiceName().getLocalPart();
        this.portName = endpoint.getPortName().getLocalPart();
        this.implClass = endpoint.getImplementationClass().getName();
        this.address = path;
        this.wsdl = address+"?wsdl";
        this.tester = "";
        this.implType = "SERVLET";
        this.deploymentType = "RI";
        fillStatMap();
    }

    @Override
    public Map<String, String> getStaticAsMap() {
        return infoMap;
    }

    private void fillStatMap() {
        infoMap.put("appName", appName);
        infoMap.put("endpointName", endpointName);
        infoMap.put("namespace", namespace);
        infoMap.put("serviceName", serviceName);
        infoMap.put("portName", portName);
        infoMap.put("implClass", implClass);
        infoMap.put("address", address);
        infoMap.put("wsdl", wsdl);
        infoMap.put("tester", tester);
        infoMap.put("implType", implType);
        infoMap.put("deploymentType", deploymentType);
    }

    @Override
    public int size() {
        return infoMap.size();
    }

    @Override
    public boolean isEmpty() {
        return infoMap.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return infoMap.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return infoMap.containsValue(value);
    }

    @Override
    public Object get(Object key) {
        return infoMap.get(key);
    }

    @Override
    public Object put(Object key, Object value) {
        throw new UnsupportedOperationException("DeployedEndpointData is a read-only map");
    }

    @Override
    public Object remove(Object key) {
        throw new UnsupportedOperationException("DeployedEndpointData is a read-only map");
    }

    @Override
    public void putAll(Map m) {
        throw new UnsupportedOperationException("DeployedEndpointData is a read-only map");
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("DeployedEndpointData is a read-only map");
    }

    @Override
    public Set keySet() {
        return infoMap.keySet();
    }

    @Override
    public Collection values() {
        return infoMap.values();
    }

    @Override
    public Set entrySet() {
        return infoMap.entrySet();
    }
}
