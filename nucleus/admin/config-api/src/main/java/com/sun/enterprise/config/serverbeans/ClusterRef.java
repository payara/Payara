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

import com.sun.enterprise.config.serverbeans.customvalidators.ReferenceConstraint;
import javax.validation.Payload;
import org.jvnet.hk2.config.*;
import static org.glassfish.config.support.Constants.NAME_SERVER_REGEX;

import java.beans.PropertyVetoException;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

/**
 * Element relating a reference to a cluster to be load balanced to an
 * (optional) health-checker
 *
 */

/* @XmlType(name = "", propOrder = {
    "healthChecker"
}) */

@Configured
@ReferenceConstraint(skipDuringCreation=true, payload=ClusterRef.class)
public interface ClusterRef extends ConfigBeanProxy, Ref, Payload  {
    
    /**
     * Gets the value of the ref property.
     *
     * @return possible object is
     *         {@link String }
     */
    @Override
    @Attribute(key=true)
    @NotNull
    @Pattern(regexp=NAME_SERVER_REGEX)
    @ReferenceConstraint.RemoteKey(message="{resourceref.invalid.cluster-ref}", type=Cluster.class)
    public String getRef();

    /**
     * Sets the value of the ref property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    @Override
    public void setRef(String value) throws PropertyVetoException;

    /**
     * Gets the value of the lbPolicy property.
     *
     * load balancing policy to be used for this cluster. Possible
     * values are round-robin , weighted-round-robin or
     * user-defined. round-robin is the default. For
     * weighted-round-robin, the weights of the instance are
     * considered while load balancing. For user-defined, the policy
     * is implemented by a shared library which is loaded by the
     * load balancer and the instance selected is delegated to the
     * loaded module.
     * 
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="round-robin")
    public String getLbPolicy();

    /**
     * Sets the value of the lbPolicy property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setLbPolicy(String value) throws PropertyVetoException;

    /**
     * Gets the value of the lbPolicyModule property.
     *
     * Specifies the absolute path to the shared library implementing the
     * user-defined policy. This should be specified only when the lb-policy
     * is user-defined. The shared library should exist and be readable in
     * the machine where load balancer is running.
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute
    public String getLbPolicyModule();

    /**
     * Sets the value of the lbPolicyModule property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setLbPolicyModule(String value) throws PropertyVetoException;

    /**
     * Gets the value of the healthChecker property.
     *
     * Each cluster would be configured for a ping based health check mechanism.
     * 
     * @return possible object is
     *         {@link HealthChecker }
     */
    @Element
    public HealthChecker getHealthChecker();

    /**
     * Sets the value of the healthChecker property.
     *
     * @param value allowed object is
     *              {@link HealthChecker }
     */
    public void setHealthChecker(HealthChecker value) throws PropertyVetoException;
}
