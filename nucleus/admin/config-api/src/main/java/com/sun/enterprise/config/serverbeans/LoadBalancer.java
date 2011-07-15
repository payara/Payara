/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.
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

import java.beans.PropertyVetoException;
import java.util.List;
import java.util.logging.Logger;
import java.util.logging.Level;

import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.logging.LogDomains;
import org.glassfish.api.admin.config.PropertiesDesc;
import org.jvnet.hk2.config.types.Property;
import org.jvnet.hk2.config.types.PropertyBag;
import org.glassfish.quality.ToDo;
import org.jvnet.hk2.component.Injectable;

import org.glassfish.api.Param;
import org.glassfish.config.support.*;
import org.jvnet.hk2.config.*;

import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.PerLookup;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.ActionReport;

import javax.validation.constraints.NotNull;
/**
 *
 */

/* @XmlType(name = "", propOrder = {
    "property"
}) */

@Configured
public interface LoadBalancer extends ConfigBeanProxy, Injectable, PropertyBag {

    /**
     * Gets the value of the name property.
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute(key=true)
    @NotNull
    public String getName();

    /**
     * Sets the value of the name property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    @Param(name="name", primary = true)
    public void setName(String value) throws PropertyVetoException;

    /**
     * Gets the value of the lbConfigName property.
     *
     * Name of the lb-config used by this load balancer
     * 
     * @return possible object is
     *         {@link String }
     */
    @Attribute
    @NotNull
    public String getLbConfigName();

    /**
     * Sets the value of the lbConfigName property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setLbConfigName(String value) throws PropertyVetoException;

    /**
     * Gets the value of the device host property.
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute
    @NotNull
    public String getDeviceHost();

    /**
     * Sets the value of the device host property.
     *
     * @param value allowed object is
     *              {@link String }
     */

    public void setDeviceHost(String value) throws PropertyVetoException;

    /**
     * Gets the value of the device port property.
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute
    @NotNull
    public String getDevicePort();

    /**
     * Sets the value of the device port property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setDevicePort(String value) throws PropertyVetoException;
    
    /**
     * Gets the value of the auto apply enabled property.
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute(dataType=Boolean.class)
    @Deprecated
    public String getAutoApplyEnabled();

    /**
     * Sets the value of the auto apply enabled property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    @Deprecated 
    public void setAutoApplyEnabled(String value) throws PropertyVetoException;

    /**
     *Properties as per {@link org.jvnet.hk2.config.types.PropertyBag}
     *
     * Known properties:
     * ssl-proxy-host - proxy host used for outbound HTTP
     * ssl-proxy-port - proxy port used for outbound HTTP
     */
    @Override
    @ToDo(priority=ToDo.Priority.IMPORTANT, details="Provide PropertyDesc for legal props" )
    @PropertiesDesc(props={})
    @Element
    List<Property> getProperty();

    @Service
    @Scoped(PerLookup.class)
    class DeleteDecorator implements DeletionDecorator<LoadBalancers, LoadBalancer> {
        @Inject
        private Domain domain;

        @Override
        public void decorate(AdminCommandContext context, LoadBalancers parent, LoadBalancer child) throws
                PropertyVetoException, TransactionFailure{
            ActionReport report = context.getActionReport();
            Logger logger = LogDomains.getLogger(LoadBalancer.class, LogDomains.ADMIN_LOGGER);
            LocalStringManagerImpl localStrings = new LocalStringManagerImpl(LoadBalancer.class);
            Transaction t = Transaction.getTransaction(parent);

            String lbName = child.getName();

            String lbConfigName = child.getLbConfigName();
            LbConfig lbConfig = domain.getLbConfigs().getLbConfig(lbConfigName);

            //check if lb-config is used by any other load-balancer
            for (LoadBalancer lb:domain.getLoadBalancers().getLoadBalancer()) {
                if (!lb.getName().equals(lbName) &&
                        lb.getLbConfigName().equals(lbConfigName)) {
                    String msg = localStrings.getLocalString("LbConfigIsInUse", lbConfigName);
                    report.setMessage(msg);
                    report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                    throw new TransactionFailure(msg);
                }
            }

            //remove lb-config corresponding to this load-balancer
            LbConfigs configs = domain.getLbConfigs();
            try {
                if (t != null) {
                    LbConfigs c = t.enroll(configs);
                    List<LbConfig> configList = c.getLbConfig();
                    configList.remove(lbConfig);
                }
            } catch (TransactionFailure ex) {
                logger.log(Level.WARNING,
                        localStrings.getLocalString("DeleteLbConfigFailed",
                        "Unable to remove lb config {0}", lbConfigName), ex);
                String msg = ex.getMessage() != null ? ex.getMessage()
                        : localStrings.getLocalString("DeleteLbConfigFailed",
                        "Unable to remove lb config {0}", lbConfigName);
                report.setMessage(msg);
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                report.setFailureCause(ex);
                throw ex;
            }
        }
    }
}
