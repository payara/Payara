/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2013 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.connectors.config;

import com.sun.enterprise.config.serverbeans.Resource;
import org.glassfish.resourcebase.resources.ResourceDeploymentOrder;
import org.glassfish.resourcebase.resources.ResourceTypeOrder;
import org.jvnet.hk2.config.*;

import java.beans.PropertyVetoException;
import java.util.List;

import org.glassfish.api.admin.config.PropertiesDesc;
import org.jvnet.hk2.config.types.Property;
import org.jvnet.hk2.config.types.PropertyBag;
import org.glassfish.api.admin.RestRedirects;
import org.glassfish.api.admin.RestRedirect;
import static org.glassfish.config.support.Constants.NAME_REGEX;

import org.glassfish.quality.ToDo;

import javax.validation.constraints.Pattern;
/**
 *
 */

/* @XmlType(name = "", propOrder = {
    "property"
}) */

/**
 *  This element is for configuring the resource adapter. These values
 * (properties) over-rides the default values present in ra.xml. The name
 * attribute has to be unique . It is optional for PE. It is used mainly for EE.
 */

@Configured
@RestRedirects({
 @RestRedirect(opType = RestRedirect.OpType.POST, commandName = "create-resource-adapter-config"),
 @RestRedirect(opType = RestRedirect.OpType.DELETE, commandName = "delete-resource-adapter-config")
})
@ResourceTypeOrder(deploymentOrder= ResourceDeploymentOrder.RESOURCEADAPTERCONFIG_RESOURCE)
public interface ResourceAdapterConfig extends ConfigBeanProxy, Resource, PropertyBag {

    /**
     * Gets the value of the name property.
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute
    @Pattern(regexp=NAME_REGEX)
    public String getName();

    /**
     * Sets the value of the name property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setName(String value) throws PropertyVetoException;

    /**
     * Gets the value of the threadPoolIds property.
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute
    public String getThreadPoolIds();

    /**
     * Sets the value of the threadPoolIds property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setThreadPoolIds(String value) throws PropertyVetoException;

    /**
     * Gets the value of the resourceAdapterName property.
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute(key=true)
    @Pattern(regexp="[^',][^',\\\\]*")
    public String getResourceAdapterName();

    /**
     * Sets the value of the resourceAdapterName property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setResourceAdapterName(String value) throws PropertyVetoException;
    
    /**
    	Properties as per {@link org.jvnet.hk2.config.types.PropertyBag}
     */
    @ToDo(priority=ToDo.Priority.IMPORTANT, details="Provide PropertyDesc for legal props" )
    @PropertiesDesc(props={})
    @Element
    @Override
    List<Property> getProperty();

    @DuckTyped
    @Override
    String getIdentity();

    class Duck {
        public static String getIdentity(ResourceAdapterConfig resource){
            return resource.getResourceAdapterName();
        }
    }
}
