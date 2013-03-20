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

import static org.glassfish.deployment.common.JavaEEResourceType.JMSCFDD;

public class JMSConnectionFactoryDefinitionDescriptor extends AbstractConnectorResourceDescriptor {

    private static final long serialVersionUID = 794492878801534084L;

    // the <description> element will be processed by base class
    private String interfaceName;
    private String className;
    private String user;
    private String password;
    private String clientId;
    private boolean transactional = true;
    private int maxPoolSize = -1;
    private int minPoolSize = -1;

    private boolean transactionSet = false;

    public JMSConnectionFactoryDefinitionDescriptor() {
        super();
        super.setResourceType(JMSCFDD);
    }

    public String getInterfaceName() {
        return interfaceName;
    }

    public void setInterfaceName(String interfaceName) {
        this.interfaceName = interfaceName;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
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

    public void addJMSConnectionFactoryPropertyDescriptor(ResourcePropertyDescriptor propertyDescriptor){
        getProperties().put(propertyDescriptor.getName(), propertyDescriptor.getValue());
    }

    public boolean isConflict(JMSConnectionFactoryDefinitionDescriptor other) {
        return (getName().equals(other.getName())) &&
            !(
                DOLUtils.equals(getInterfaceName(), other.getInterfaceName()) &&
                DOLUtils.equals(getClassName(), other.getClassName()) &&
                DOLUtils.equals(getResourceAdapter(), other.getResourceAdapter()) &&
                DOLUtils.equals(getUser(), other.getUser()) &&
                DOLUtils.equals(getPassword(), other.getPassword()) &&
                DOLUtils.equals(getClientId(), other.getClientId()) &&
                getProperties().equals(other.getProperties()) &&
                isTransactional() == other.isTransactional() &&
                getMinPoolSize() == other.getMinPoolSize() &&
                getMaxPoolSize() == other.getMaxPoolSize()
            );
    }
}
