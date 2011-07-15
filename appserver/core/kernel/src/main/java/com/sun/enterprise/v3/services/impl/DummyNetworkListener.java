/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007-2011 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.v3.services.impl;

import org.glassfish.grizzly.config.dom.NetworkListener;
import org.glassfish.grizzly.config.dom.NetworkListeners;
import org.glassfish.grizzly.config.dom.Protocol;
import org.glassfish.grizzly.config.dom.ThreadPool;
import org.glassfish.grizzly.config.dom.Transport;
import java.util.ArrayList;
import java.util.List;
import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.config.TransactionFailure;
import org.jvnet.hk2.config.types.Property;

/**
 * This is a dummy implementation of the NetworkListener interface. This is used to create a fake
 * network-listener elements. This is used only to support lazyInit attribute of iiop and jms services through the
 * light weight listener. Ultimately, these services will move to their own network-listener
 * element in domain.xml (at which point we have to get rid of this fake object). But till the time IIOP and JMS
 * service elements in domain.xml can move to use network-listener element, we will create this "fake network-listener"
 * which in turn will help start light weight listener for these services.
 */
public class DummyNetworkListener implements NetworkListener {
    private String address = "0.0.0.0";
    private String enabled = "true";
    private String name;
    private String port;
    private String protocol;
    private String pool;
    private String transport;
    private String jkEnabled;
    private String jkConfigurationFile;
    private final List<Property> properties = new ArrayList<Property>();

    public DummyNetworkListener() {
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String value) {
        address = value;
    }

    public String getEnabled() {
        return enabled;
    }

    public void setEnabled(String enabled) {
        this.enabled = enabled;
    }

    public String getName() {
        return name;
    }

    public void setName(String value) {
        name = value;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String value) {
        port = value;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String value) {
        protocol = value;
    }

    public String getThreadPool() {
        return pool;
    }

    public void setThreadPool(String value) {
        pool = value;
    }

    public String getTransport() {
        return transport;
    }

    public void setTransport(String value) {
        transport = value;
    }

    public String getJkEnabled() {
        return jkEnabled;
    }

    public void setJkEnabled(String value) {
        jkEnabled = value;
    }

    public String getJkConfigurationFile() {
        return jkConfigurationFile;
    }

    public void setJkConfigurationFile(String jkConfigurationFile) {
        this.jkConfigurationFile = jkConfigurationFile;
    }

    public void injectedInto(Object target){}

    @Override
    public <T extends ConfigBeanProxy> T createChild(Class<T> type) throws TransactionFailure {
        throw new UnsupportedOperationException();
    }

    @Override
    public Protocol findProtocol() {
        return null;
    }

    public String findHttpProtocolName() {
        return null;
    }

    @Override
    public Protocol findHttpProtocol() {
        return null;
    }

    @Override
    public ThreadPool findThreadPool() {
        return null;
    }

    @Override
    public Transport findTransport() {
        return null;
    }

    @Override
    public NetworkListeners getParent() {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T extends ConfigBeanProxy> T getParent(Class<T> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ConfigBeanProxy deepCopy(ConfigBeanProxy parent) {
        throw new UnsupportedOperationException();
    }

    public List<Property> getProperty() {
        return properties;
    }

    public Property getProperty(String name) {
        if (name == null) return null;
        
        for(Property property : properties) {
            if (name.equals(property.getName())) {
                return property;
            }
        }

        return null;
    }

    public String getPropertyValue(String name) {
        return getPropertyValue(name, null);
    }

    public String getPropertyValue(String name, String defaultValue) {
        final Property property = getProperty(name);
        if (property != null) {
            return property.getValue();
        }

        return defaultValue;
    }
}

