/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2013 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.deployment;

import com.sun.enterprise.deployment.util.DOLUtils;
import java.util.Properties;

import static org.glassfish.deployment.common.JavaEEResourceType.*;

/**
 * Represents the data from a @MailSessionDefinition annotation.
 */
public class MailSessionDescriptor extends ResourceDescriptor {

    private String name;
    private String storeProtocol;
    private String transportProtocol;
    private String user;
    private String password;
    private String host;
    private String from;
    private Properties properties = new Properties();

    private static final String JAVA_URL = "java:";
    private static final String JAVA_COMP_URL = "java:comp/";

    private boolean deployed = false;

    public MailSessionDescriptor(){
        super.setResourceType(MSD);
    }


    public String getName() {
        return name;
    }

    public static String getName(String thisName) {
        if (!thisName.contains(JAVA_URL)) {
            thisName = JAVA_COMP_URL + thisName;
        }
        return thisName;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void addProperty(String key, String value) {
        properties.put(key, value);
    }

    public String getProperty(String key) {
        return (String) properties.get(key);
    }

    public Properties getProperties() {
        return properties;
    }

    public String getStoreProtocol() {
        return storeProtocol;
    }

    public void setStoreProtocol(String storeProtocol) {
        this.storeProtocol = storeProtocol;
    }

    public String getTransportProtocol() {
        return transportProtocol;
    }

    public void setTransportProtocol(String transportProtocol) {
        this.transportProtocol = transportProtocol;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public void addMailSessionPropertyDescriptor(ResourcePropertyDescriptor propertyDescriptor) {
        properties.put(propertyDescriptor.getName(), propertyDescriptor.getValue());
    }

    public void setDeployed(boolean deployed) {
        this.deployed = deployed;
    }

    public boolean isDeployed() {
        return deployed;
    }

    public boolean equals(Object object) {
        if (object instanceof MailSessionDescriptor) {
            MailSessionDescriptor reference = (MailSessionDescriptor) object;
            return getJavaName(this.getName()).equals(getJavaName(reference.getName()));
        }
        return false;
    }

    public int hashCode() {
        int result = 17;
        result = 37 * result + getName().hashCode();
        return result;
    }

    public static String getJavaName(String thisName) {
        if (!thisName.startsWith(JAVA_URL)) {
            thisName = JAVA_COMP_URL + thisName;
        }
        return thisName;
    }

    public boolean isConflict(MailSessionDescriptor other) {
            return (getName().equals(other.getName())) &&
                !(
                    DOLUtils.equals(getUser(), other.getUser()) &&
                    DOLUtils.equals(getPassword(), other.getPassword()) &&
                    DOLUtils.equals(getFrom(), other.getFrom()) &&
                    DOLUtils.equals(getHost(), other.getHost()) &&
                    DOLUtils.equals(getPassword(), other.getPassword()) &&
                    DOLUtils.equals(getStoreProtocol(),
                                                other.getStoreProtocol()) &&
                    DOLUtils.equals(getTransportProtocol(),
                                                other.getTransportProtocol()) &&
                    DOLUtils.equals(getDescription(), other.getDescription()) &&
                    properties.equals(other.properties)
                );
        }
}
