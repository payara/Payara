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
 */

package org.glassfish.resources.api;

import com.sun.enterprise.repository.ResourceProperty;
import org.glassfish.resourcebase.resources.api.ResourceInfo;

import java.util.Set;

/**
 * Interface representing J2EE Resource info.
 *
 * @author Kenneth Saks
 */
public interface JavaEEResource {

    /**
     * Resource Types
     */
    final int JMS_DESTINATION = 1;
    final int JMS_CNX_FACTORY = 2;
    final int JDBC_RESOURCE = 3;
    final int JDBC_XA_RESOURCE = 4;
    final int JDBC_DRIVER = 5;
    final int CONNECTOR_RESOURCE = 6;
    final int RESOURCE_ADAPTER = 7;

    // START OF IASRI #4626188
    final int JDBC_CONNECTION_POOL = 8;
    final int PMF_RESOURCE = 9;
    final int EXTERNAL_JNDI_RESOURCE = 10;
    final int CUSTOM_RESOURCE = 11;
    // START OF IASRI #4650786
    final int MAIL_RESOURCE = 12;
    // END OF IASRI #4650786
    // END OF IASRI #4626188

    /**
     * Resource Info. Immutable.
     */
    ResourceInfo getResourceInfo();

    /**
     * Resource type.  Defined above. Immutable.
     */
    int getType();

    /**
     * Set containing elements of type ResourceProperty.
     * Actual property names are resource type specific.
     *
     * @return Shallow copy of resource property set. If
     *         resource has 0 properties, empty set is
     *         returned.
     */
    Set getProperties();

    /**
     * Add a property. Underlying set is keyed by
     * property name.  The new property overrides any
     * existing property with same name.
     */
    void addProperty(ResourceProperty property);

    /**
     * Remove a property. Underlying set is keyed by
     * property name.
     *
     * @return true if property was removed, false if
     *         property was not found
     */
    boolean removeProperty(ResourceProperty property);

    /**
     * Get a property with the given name.
     *
     * @return ResourceProperty or null if not found.
     */
    ResourceProperty getProperty(String propertyName);

    /**
     * Create a new resource with the given name
     * that has the same attribute and property
     * settings as the invoked object.
     */
    JavaEEResource makeClone(ResourceInfo resourceInfo);

    // START OF IASRI #4626188
    void setEnabled(boolean value);

    boolean isEnabled();

    void setDescription(String value);

    String getDescription();
    // END OF IASRI #4626188
}
