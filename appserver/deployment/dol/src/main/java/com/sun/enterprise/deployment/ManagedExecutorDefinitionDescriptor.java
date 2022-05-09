/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2022] Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
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

import java.util.Properties;

import static org.glassfish.deployment.common.JavaEEResourceType.*;

public class ManagedExecutorDefinitionDescriptor extends ResourceDescriptor {

    private static final String JAVA_URL = "java:";
    private static final String JAVA_COMP_URL = "java:comp/";

    private String name;
    private int maximumPoolSize = Integer.MAX_VALUE;
    private long hungAfterSeconds = 0;
    private String context;
    private Properties properties = new Properties();

    public ManagedExecutorDefinitionDescriptor() {
        super.setResourceType(MEDD);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    public long getMaximumPoolSize() {
        return maximumPoolSize;
    }

    public void setMaximumPoolSize(int maximumPoolSize) {
        this.maximumPoolSize = maximumPoolSize;
    }

    public long getHungAfterSeconds() {
        return hungAfterSeconds;
    }

    public void setHungAfterSeconds(long hungAfterSeconds) {
        this.hungAfterSeconds = hungAfterSeconds;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
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

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ManagedExecutorDefinitionDescriptor) {
            ManagedExecutorDefinitionDescriptor ref = (ManagedExecutorDefinitionDescriptor) obj;
            return this.getName().equals(getURLName(ref.getName()));
        }
        return false;
    }

    public static String getURLName(String thisName) {
        if (!thisName.contains(JAVA_URL)) {
            thisName = JAVA_COMP_URL + thisName;
        }
        return thisName;
    }

    public void addManagedExecutorPropertyDescriptor(ResourcePropertyDescriptor propertyDescriptor) {
        properties.put(propertyDescriptor.getName(), propertyDescriptor.getValue());
    }
}
