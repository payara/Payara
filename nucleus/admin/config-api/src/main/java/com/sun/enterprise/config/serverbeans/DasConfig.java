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
import org.jvnet.hk2.config.Element;
import org.jvnet.hk2.config.Configured;
import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.config.types.Property;
import org.jvnet.hk2.config.types.PropertyBag;

import java.beans.PropertyVetoException;
import java.util.List;

import org.glassfish.api.admin.config.PropertiesDesc;

import org.glassfish.quality.ToDo;

import javax.validation.constraints.Min;

/**
 *
 */

/* @XmlType(name = "", propOrder = {
    "property"
}) */

@Configured
public interface DasConfig extends ConfigBeanProxy, PropertyBag {

    /**
     * Gets the value of the dynamicReloadEnabled property.
     * When true, server checks timestamp on a .reload file at every module and
     * application directory level to trigger reload. Polling frequency is
     * controlled by reload-poll-interval-in-seconds
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="true",dataType=Boolean.class)
    String getDynamicReloadEnabled();

    /**
     * Sets the value of the dynamicReloadEnabled property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setDynamicReloadEnabled(String value) throws PropertyVetoException;

    /**
     * Gets the value of the dynamicReloadPollIntervalInSeconds property.
     *
     * Maximum period, in seconds, that a change to the load balancer
     * configuration file takes before it is detected by the load balancer
     * and the file reloaded. A value of 0 indicates that reloading is disabled.
     * Default period is 1 minute (60)

     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="2",dataType=Integer.class)
    @Min(value=1)
    String getDynamicReloadPollIntervalInSeconds();

    /**
     * Sets the value of the dynamicReloadPollIntervalInSeconds property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setDynamicReloadPollIntervalInSeconds(String value) throws PropertyVetoException;

    /**
     * Gets the value of the autodeployEnabled property.
     *
     * This will enable the autodeployment service. If true, the service will
     * automatically starts with the admin-server. Auto Deployment is a feature
     * that enables developers to quickly deploy applications and modules to a
     * running application server withoutrequiring the developer to perform an
     * explicit application server restart or separate deployment operation.  
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="true",dataType=Boolean.class)
    String getAutodeployEnabled();

    /**
     * Sets the value of the autodeployEnabled property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setAutodeployEnabled(String value) throws PropertyVetoException;

    /**
     * Gets the value of the autodeployPollingIntervalInSeconds property.
     *
     * The polling interval (in sec), at the end of which autodeployment service
     * will scan the source directory (specified by "autodeploy-dir" tag) for
     * any new deployable component.
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="2",dataType=Integer.class)
    @Min(value=1)
    String getAutodeployPollingIntervalInSeconds();

    /**
     * Sets the value of the autodeployPollingIntervalInSeconds property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setAutodeployPollingIntervalInSeconds(String value) throws PropertyVetoException;

    /**
     * Gets the value of the autodeployDir property.
     *
     * The source directory (relative to instance root) from which autodeploy
     * service will pick deployable components.
     * You can also specify an absolute directory.
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="${com.sun.aas.instanceRoot}/autodeploy")
    String getAutodeployDir();

    /**
     * Sets the value of the autodeployDir property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setAutodeployDir(String value) throws PropertyVetoException;

    /**
     * Gets the value of the autodeployVerifierEnabled property.
     * To enable/disable verifier, during auto-deployment. If true, verification
     * will be done before any deployment activity. In the event of any verifier
     * test failure, deployment is not performed.
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="false",dataType=Boolean.class)
    String getAutodeployVerifierEnabled();

    /**
     * Sets the value of the autodeployVerifierEnabled property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setAutodeployVerifierEnabled(String value) throws PropertyVetoException;

    /**
     * Gets the value of the autodeployJspPrecompilationEnabled property.
     *
     * If true, JSPs will be pre compiled during deployment of war module(s).
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="false",dataType=Boolean.class)
    String getAutodeployJspPrecompilationEnabled();

    /**
     * Sets the value of the autodeployJspPrecompilationEnabled property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setAutodeployJspPrecompilationEnabled(String value) throws PropertyVetoException;

    /**
     * Gets the value of the autodeployRetryTimeout property.
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="4",dataType=Integer.class)
    String getAutodeployRetryTimeout();

    /**
     * Sets the value of the autodeployRetryTimeout property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setAutodeployRetryTimeout(String value) throws PropertyVetoException;

    /**
     * Gets the value of the deployXmlValidation property.
     *
     * specifies if descriptor validation is required or not.
     * full
     *     xml will be validated and in case of xml validation errors,
     *     deployment will fail
     * parsing
     *    xml errors will be reported but deployment process will continue.
     * none
     *    no xml validation will be perfomed on the standard or runtime
     *    deployment descriptors
     * 
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="full")
    String getDeployXmlValidation();

    /**
     * Sets the value of the deployXmlValidation property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setDeployXmlValidation(String value) throws PropertyVetoException;

    /**
     * Gets the value of the adminSessionTimeoutInMinutes property.
     *
     * Timeout in minutes indicating the administration gui session timeout
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="60")
    String getAdminSessionTimeoutInMinutes();

    /**
     * Sets the value of the adminSessionTimeoutInMinutes property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setAdminSessionTimeoutInMinutes(String value) throws PropertyVetoException;
    
    /**
    	Properties as per {@link PropertyBag}
     */
    @ToDo(priority=ToDo.Priority.IMPORTANT, details="Provide PropertyDesc for legal props" )
    @PropertiesDesc(props={})
    @Element
    List<Property> getProperty();
}
