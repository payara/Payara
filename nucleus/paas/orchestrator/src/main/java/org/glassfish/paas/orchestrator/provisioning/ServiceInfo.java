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

package org.glassfish.paas.orchestrator.provisioning;

import org.jvnet.hk2.config.types.Property;

import java.util.HashMap;
import java.util.Map;

/**
 * A subset of a <code>ProvisionedService</code> that is persisted in CPAS'
 * configuration store.
 *  
 * @author bhavanishankar@java.net
 */
public class ServiceInfo {

    private String serviceName;

    private String ipAddress;

    private String instanceId;

    private String serverType;

    private String state;

    //not-null when it is an application scoped service.
    private String appName;

    // general name-values.
    Map<String,String> properties = new HashMap<String, String>();

    public static enum State {
        Initializing, NotRunning, Running, Stop_in_progress, Start_in_progress, Delete_in_progress
    }

    //TODO should not be in Orchestrator ?
    public static enum Type {
        Domain, Cluster, StandAloneInstance, ClusterInstance
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String cloudName) {
        this.serviceName = cloudName;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public String getServerType() {
        return serverType;
    }

    public void setServerType(String serverType) {
        this.serverType = serverType;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getProperty(String name) {
        return properties.get(name);
    }

    public void setProperty(String name, String value) {
        properties.put(name, value);
    }

    public Map<String,String> getProperties() {
        return properties;
    }
    
    @Override
    public String toString() {
        StringBuilder toString = new StringBuilder();
        toString.append("ServiceInfo :: \n" +
                "serviceName [ " + serviceName + "] \n" +
                "serverType [ " + serverType + "] \n" +
                "state [ " + state + "] \n");
        StringBuffer property = new StringBuffer("properties [\n");
        for(Map.Entry<String, String> entry : properties.entrySet()){
            property.append(entry.getKey() + " = " + entry.getValue() + "\n");
        }
        property.append("]\n");
        toString.append(property.toString());
        return toString.toString();
    }
}
