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
import org.jvnet.hk2.config.Attribute;
import org.jvnet.hk2.config.Configured;
import org.jvnet.hk2.config.Element;
import org.jvnet.hk2.config.ConfigBeanProxy;
import static org.glassfish.config.support.Constants.NAME_SERVER_REGEX;

import java.beans.PropertyVetoException;
import javax.validation.Payload;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
/**
 *
 */

/* @XmlType(name = "", propOrder = {
    "healthChecker"
}) */

@Configured
@ReferenceConstraint(skipDuringCreation=true, payload=ServerRef.class)
public interface ServerRef extends ConfigBeanProxy, Ref, Payload  {

    /**
     * Gets the value of the ref property.
     *
     * A reference to the name of a server defined elsewhere
     *
     * @return possible object is
     *         {@link String }
     */
    @Override
    @Attribute(key=true)
    @NotNull
    @Pattern(regexp=NAME_SERVER_REGEX, message="{server.invalid.name}", payload=ServerRef.class)
    @ReferenceConstraint.RemoteKey(message="{resourceref.invalid.server-ref}", type=Server.class)
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
     * Gets the value of the disableTimeoutInMinutes property.
     *
     * The time, in minutes, that it takes this server to reach a quiescent
     * state after having been disabled
     * 
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="30")
    public String getDisableTimeoutInMinutes();

    /**
     * Sets the value of the disableTimeoutInMinutes property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setDisableTimeoutInMinutes(String value) throws PropertyVetoException;

    /**
     * Gets the value of the lbEnabled property.
     *
     * Causes any and all load-balancers using this server to consider this
     * server available to them. Defaults to available(true)
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue=LBENABLED_DEFAULT_VALUE, dataType=Boolean.class)
    public String getLbEnabled();

    /**
     * Sets the value of the lbEnabled property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setLbEnabled(String value) throws PropertyVetoException;

    /**
     * Gets the value of the enabled property.
     *
     * A boolean flag that causes the server to be enabled to serve end-users,
     * or not. Default is to be enabled (true)
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
     * Gets the value of the healthChecker property.
     *
     * @return possible object is
     *         {@link HealthChecker }
     */
    @Element("health-checker")
    public HealthChecker getHealthChecker();

    /**
     * Sets the value of the healthChecker property.
     *
     * @param value allowed object is
     *              {@link HealthChecker }
     */
    public void setHealthChecker(HealthChecker value) throws PropertyVetoException;


    //defines the default value for lb-enabled attribute
    public String LBENABLED_DEFAULT_VALUE = "true";

}
