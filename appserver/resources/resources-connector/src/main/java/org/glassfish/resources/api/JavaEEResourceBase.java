/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
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
 *
 * Portions Copyright [2017-2019] Payara Foundation and/or affiliates
 */

package org.glassfish.resources.api;

import com.sun.enterprise.repository.ResourceProperty;
import org.glassfish.resourcebase.resources.api.ResourceInfo;

import java.io.Serializable;
import java.util.*;

/**
 * Base class for common JavaEE Resource implementation.
 */
public abstract class JavaEEResourceBase implements JavaEEResource, Serializable {

    ResourceInfo resourceInfo;
    Map properties_;
    // START OF IASRI #4626188
    boolean enabled_;
    String description_;
    // END OF IASRI #4626188

    public JavaEEResourceBase(ResourceInfo resourceInfo) {
        this.resourceInfo = resourceInfo;
        properties_ = new HashMap();
    }

    @Override
    public ResourceInfo getResourceInfo() {
        return resourceInfo;
    }

    // START OF IASRI #4626188
    @Override
    public void setEnabled(boolean value) {
        enabled_ = value;
    }

    @Override
    public boolean isEnabled() {
        return enabled_;
    }

    @Override
    public void setDescription(String value) {
        description_ = value;
    }

    @Override
    public String getDescription() {
        return description_;
    }
    // END OF IASRI #4626188

    @Override
    public abstract int getType();

    @Override
    public Set getProperties() {
        Set shallowCopy = new HashSet();
        Collection collection = properties_.values();
        for (Iterator iter = collection.iterator(); iter.hasNext();) {
            ResourceProperty next = (ResourceProperty) iter.next();
            shallowCopy.add(next);
        }
        return shallowCopy;
    }

    @Override
    public void addProperty(ResourceProperty property) {
        properties_.put(property.getName(), property);
    }

    @Override
    public boolean removeProperty(ResourceProperty property) {
        Object removedObj = properties_.remove(property.getName());
        return (removedObj != null);
    }

    @Override
    public ResourceProperty getProperty(String propertyName) {
        return (ResourceProperty) properties_.get(propertyName);
    }

    @Override
    public JavaEEResource makeClone(ResourceInfo resourceInfo) {
        JavaEEResource clone = doClone(resourceInfo);
        Set entrySet = properties_.entrySet();
        for (Iterator iter = entrySet.iterator(); iter.hasNext();) {
            Map.Entry next = (Map.Entry) iter.next();
            ResourceProperty propClone =
                    new ResourcePropertyImpl((String) next.getKey());
            propClone.setValue(next.getValue());

            clone.addProperty(propClone);
        }
        // START OF IASRI #4626188
        clone.setEnabled(isEnabled());
        clone.setDescription(getDescription());
        // END OF IASRI #4626188
        return clone;
    }

    /**
     * Gets a the of properties as a JSON array
     * i.e. [ propname1=value , propname2=othervalue ]
     * <p>
     * If there are no properties an empty string is returned.
     * @return
     */
    protected String getPropsString() {
        StringBuilder propsBuffer = new StringBuilder();
        Set props = getProperties();
        if (!props.isEmpty()) {
            for (Iterator iter = props.iterator(); iter.hasNext();) {
                if (propsBuffer.length() == 0) {
                    propsBuffer.append("[ ");
                } else {
                    propsBuffer.append(" , ");
                }
                ResourceProperty next = (ResourceProperty) iter.next();
                propsBuffer.append(next.getName()).append("=").append(next.getValue());
            }
            propsBuffer.append(" ]");
        }
        return propsBuffer.toString();
    }

    /**
     * Creates a JavaEEResource from a specified {@link ResourceInfo}
     * @param resourceInfo
     * @return
     *///
    protected abstract JavaEEResource doClone(ResourceInfo resourceInfo);
}
