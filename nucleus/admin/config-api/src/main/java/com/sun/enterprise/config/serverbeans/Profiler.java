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

package com.sun.enterprise.config.serverbeans;

import org.jvnet.hk2.config.Attribute;
import org.jvnet.hk2.config.Configured;
import org.jvnet.hk2.config.Element;
import org.jvnet.hk2.config.ConfigBeanProxy;

import java.beans.PropertyVetoException;
import java.util.List;

import org.glassfish.api.admin.config.PropertiesDesc;
import static org.glassfish.config.support.Constants.NAME_REGEX;
import org.jvnet.hk2.config.types.Property;
import org.jvnet.hk2.config.types.PropertyBag;

import org.glassfish.quality.ToDo;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

/**
 *
 * Profilers could be one of jprobe, optimizeit, hprof, wily and so on
 * jvm-options and property elements are used to record the settings needed to
 * get a particular profiler going. A server instance is tied to a particular
 * profiler, by the profiler element in java-config. Changing the profiler will
 * require a server restart
 *
 * The adminstrative graphical interfaces, could list multiple supported
 * profilers (incomplete at this point) and will populate server.xml
 * appropriately.
 * 
 */

/* @XmlType(name = "", propOrder = {
    "jvmOptionsOrProperty"
}) */

@Configured
public interface Profiler extends ConfigBeanProxy, PropertyBag, JvmOptionBag {

    /**
     * Gets the value of the name property.
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute(key=false)   // bizarre case of having a name, but it's not a key; it's a singleton
    @NotNull
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
     * Gets the value of the classpath property.
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute
    public String getClasspath();

    /**
     * Sets the value of the classpath property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setClasspath(String value) throws PropertyVetoException;

    /**
     * Gets the value of the nativeLibraryPath property.
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute
    public String getNativeLibraryPath();

    /**
     * Sets the value of the nativeLibraryPath property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setNativeLibraryPath(String value) throws PropertyVetoException;

    /**
     * Gets the value of the enabled property.
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="true",dataType=Boolean.class)
    public String getEnabled();

    /**
     * Sets the value of the enabled property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setEnabled(String value) throws PropertyVetoException;

    /**
    	Properties as per {@link org.jvnet.hk2.config.types.PropertyBag}
     */
    @ToDo(priority=ToDo.Priority.IMPORTANT, details="Provide PropertyDesc for legal props" )
    @PropertiesDesc(props={})
    @Element
    List<Property> getProperty();

}
