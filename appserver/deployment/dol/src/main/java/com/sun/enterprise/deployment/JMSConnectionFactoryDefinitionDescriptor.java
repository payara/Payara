/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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
import org.glassfish.deployment.common.Descriptor;

import java.util.Properties;

import static org.glassfish.deployment.common.JavaEEResourceType.*;

public class JMSConnectionFactoryDefinitionDescriptor extends Descriptor {

    private static final long serialVersionUID = 794492878801534084L;

    // the <description> element will be processed by base class
    private String name ;
    private String className;
    private String resourceAdapterName;
    private String user;
    private String password;
    private String clientId;
    private Properties properties = new Properties();
    private int connectionTimeout = 0;
    private boolean transactional = true;
    private int initialPoolSize = -1;
    private int maxPoolSize = -1;
    private int minPoolSize = -1;
    private int maxIdleTime = -1;

    private boolean transactionSet = false;
    private boolean connectionTimeoutSet = false;

    private String resourceId;
    private MetadataSource metadataSource = MetadataSource.XML;

    private static final String JAVA_URL = "java:";
    private static final String JAVA_COMP_URL = "java:comp/";

    public JMSConnectionFactoryDefinitionDescriptor() {
        super();
        super.setResourceType(JMSCFDD);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getResourceAdapterName() {
        return resourceAdapterName;
    }

    public void setResourceAdapterName(String resourceAdapterName) {
        this.resourceAdapterName = resourceAdapterName;
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

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public void addProperty(String key, String value) {
        properties.put(key, value);
    }

    public String getProperty(String key) {
        return (String)properties.get(key);
    }

    public Properties getProperties(){
        return properties;
    }

    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
        setConnectionTimeoutSet(true);
    }

    public boolean isConnectionTimeoutSet() {
        return connectionTimeoutSet;
    }

    private void setConnectionTimeoutSet(boolean value) {
        connectionTimeoutSet = value;
    }

    public boolean isTransactional() {
        return transactional;
    }

    public void setTransactional(boolean transactional) {
        this.transactional = transactional;
        setTransactionSet(true);
    }

    public boolean isTransactionSet() {
        return transactionSet;
    }

    public void setTransactionSet(boolean value) {
        this.transactionSet = value;
    }

    public int getInitialPoolSize() {
        return initialPoolSize;
    }

    public void setInitialPoolSize(int initialPoolSize) {
        this.initialPoolSize = initialPoolSize;
    }

    public int getMaxPoolSize() {
        return maxPoolSize;
    }

    public void setMaxPoolSize(int maxPoolSize) {
        this.maxPoolSize = maxPoolSize;
    }

    public int getMinPoolSize() {
        return minPoolSize;
    }

    public void setMinPoolSize(int minPoolSize) {
        this.minPoolSize = minPoolSize;
    }

    public int getMaxIdleTime() {
        return maxIdleTime;
    }

    public void setMaxIdleTime(int maxIdleTime) {
        this.maxIdleTime = maxIdleTime;
    }

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public MetadataSource getMetadataSource() {
        return metadataSource;
    }

    public void setMetadataSource(MetadataSource metadataSource) {
        this.metadataSource = metadataSource;
    }

    public boolean equals(Object object) {
        if (object instanceof JMSConnectionFactoryDefinitionDescriptor) {
            JMSConnectionFactoryDefinitionDescriptor reference = (JMSConnectionFactoryDefinitionDescriptor)object;
            return getJavaName(this.getName()).equals(getJavaName(reference.getName()));
        }
        return false;
    }

    public int hashCode() {
        int result = 17;
        result = 37 * result + getName().hashCode();
        return result;
    }

    public static String getJavaName(String theName) {
        if (!theName.contains(JAVA_URL)) {
            theName = JAVA_COMP_URL + theName;
        }
        return theName;
    }

    public void addJMSConnectionFactoryPropertyDescriptor(ResourcePropertyDescriptor propertyDescriptor){
        properties.put(propertyDescriptor.getName(), propertyDescriptor.getValue());
    }

    public boolean isConflict(JMSConnectionFactoryDefinitionDescriptor other) {
        return (getName().equals(other.getName())) &&
            !(
                DOLUtils.equals(getClassName(), other.getClassName()) &&
                DOLUtils.equals(getResourceAdapterName(), other.getResourceAdapterName()) &&
                DOLUtils.equals(getUser(), other.getUser()) &&
                DOLUtils.equals(getPassword(), other.getPassword()) &&
                DOLUtils.equals(getClientId(), other.getClientId()) &&
                properties.equals(other.properties) &&
                getConnectionTimeout() == other.getConnectionTimeout() &&
                isTransactional() == other.isTransactional() &&
                getInitialPoolSize() == other.getInitialPoolSize() &&
                getMinPoolSize() == other.getMinPoolSize() &&
                getMaxPoolSize() == other.getMaxPoolSize() &&
                getMaxIdleTime() == other.getMaxIdleTime()
            );
    }
}

