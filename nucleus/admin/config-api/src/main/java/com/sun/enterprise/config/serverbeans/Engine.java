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

package com.sun.enterprise.config.serverbeans;

import org.jvnet.hk2.config.Attribute;
import org.jvnet.hk2.config.Configured;
import org.jvnet.hk2.config.Element;
import org.jvnet.hk2.config.ConfigBeanProxy;

import java.beans.PropertyVetoException;
import java.util.List;

import org.glassfish.api.admin.config.PropertiesDesc;
import org.jvnet.hk2.config.types.Property;
import org.jvnet.hk2.config.types.PropertyBag;

import org.glassfish.quality.ToDo;

import javax.validation.constraints.NotNull;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.DuckTyped;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;

/**
 *
 */

/* @XmlType(name = "", propOrder = {
    "description",
    "property"
}) */

@Configured
public interface Engine extends ConfigBeanProxy, PropertyBag {

    /**
     * Gets the value of the sniffer property.
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute(key=true)
    @NotNull
    String getSniffer();

    /**
     * Sets the value of the sniffer property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setSniffer(String value) throws PropertyVetoException;

    /**
     * Gets the value of the description property.
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute
    String getDescription();

    /**
     * Sets the value of the description property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setDescription(String value) throws PropertyVetoException;


    // TODO: Make this not a list once the hk2/config bug with a single (not list) ("*") is working.
    @Element("*")
    List<ApplicationConfig> getApplicationConfigs();

//    void setConfig(ApplicationConfig config) throws PropertyVetoException;

    // TODO: remove this once hk2/config supports non-list @Element("*").
    @DuckTyped
    ApplicationConfig getApplicationConfig();

    // TODO: remove this once hk2/config supports non-list @Element("*").
    @DuckTyped
    void setApplicationConfig(ApplicationConfig config);

    /**
     * Creates a new instance of the specified type of app config.
     * @param <T> stands for the specific type required
     * @param configType the Class for the type required
     * @return new instance of the specified type of ApplicationConfig
     * @throws TransactionFailure
     */
    @DuckTyped
    <T extends ApplicationConfig> T newApplicationConfig(Class<T> configType)
            throws TransactionFailure;

    /**
    	Properties as per {@link PropertyBag}
     */
    @ToDo(priority=ToDo.Priority.IMPORTANT, details="Provide PropertyDesc for legal props" )
    @PropertiesDesc(props={})
    @Element
    List<Property> getProperty();

    // TODO: remove this once hk2/config supports non-list @Element("*").
    class Duck {
        public static ApplicationConfig getApplicationConfig(Engine instance) {
            return (instance.getApplicationConfigs().size() == 0) ? null :
                    instance.getApplicationConfigs().get(0);
        }

        public static void setApplicationConfig(Engine instance, ApplicationConfig config) {
            instance.getApplicationConfigs().clear();
            instance.getApplicationConfigs().add(config);
        }

        public static <T extends ApplicationConfig> T newApplicationConfig(
                final Engine instance, final Class<T> configType) throws TransactionFailure {
            return (T) ConfigSupport.apply(new SingleConfigCode<Engine>() {

                public Object run(Engine e) throws PropertyVetoException, TransactionFailure {
                    T newChild = e.createChild(configType);
                    e.getApplicationConfigs().add(newChild);
                    return newChild;
                }
            }, instance);
        }
    }
}
