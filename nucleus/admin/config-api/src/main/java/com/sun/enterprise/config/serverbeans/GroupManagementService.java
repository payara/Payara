/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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

import java.beans.PropertyVetoException;
import java.util.List;

import org.glassfish.api.admin.config.PropertiesDesc;
import org.jvnet.hk2.config.types.Property;
import org.jvnet.hk2.config.types.PropertyBag;
import org.glassfish.quality.ToDo;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import org.glassfish.api.admin.config.ConfigExtension;

/**
 * group-management-service(GMS) is an in-process service that provides cluster
 * monitoring and group communication services. GMS notifies registered modules
 * in an application server instance when one or more members in the cluster
 * fail (become unreachable). GMS also provides the ability to send and receive
 * messages between a group of processes. GMS is a abstraction layer that
 * plugs-in group communication technologies which rely on a configurable stack
 * of protocols. Each of these protocols has properties that can be changed for
 * a given network and deployment topology. These relevant configurable
 * protocols are: failure-detection enables its members to periodically monitor other
 * group members to determine their availability in the group.
 * group-discovery is used for discovery of group &
 * its members. failure-detection.verify-failure-timeout-in-millis
 * verifies suspect instances by adding a verification
 * layer to mark a failure suspicion as a confirmed failure.
 *
 */

/* @XmlType(name = "", propOrder = {
    "property"
}) */

@Configured
@SuppressWarnings({"deprecation"})
public interface GroupManagementService extends PropertyBag, ConfigExtension {

    /**
     * Gets the value of the groupManagementService property.
     *
     * @return possible object is
     *         {@link GroupManagementService }
     * @since glassfish v3.1
     */
    @Element //(required=true)
    @NotNull
    FailureDetection getFailureDetection();

    /**
     * Sets the value of the failureDetection property
     *
     * @param value allowed object is
     *              {@link FailureDetection }
     * @since glassfish v3.1
     */
    void setFailureDetection(FailureDetection value) throws PropertyVetoException;


    /**
     * Gets the value of the groupDiscoveryTimeoutInMillis property.
     *
     * Amount of time in milliseconds that GMS waits for discovery of other
     * members in this group. Must be a positive integer.
     *
     * @return possible object is
     *         {@link String }
     * @since glassfish v3.1
     */
    @Attribute (defaultValue="5000")
    @Min(value=1000)
    @Max(value=120000)
    String getGroupDiscoveryTimeoutInMillis();

    /**
     * Sets the value of the groupDiscoveryTimeoutInMillis property.
     *
     * @param value allowed object is
     *              {@link String }
     * @since glassfish v3.1
     */
    void setGroupDiscoveryTimeoutInMillis(String value) throws PropertyVetoException;



    /**
     * Gets the value of the fdProtocolMaxTries property.
     *
     * Maximum number of attempts to try before GMS confirms that a failure is
     * suspected in the group. Must be a positive integer.
     *
     * @return possible object is
     *         {@link String }
     * @deprecate
     * Replaced by {@link FailureDetection.getMaxMissedHeartbeats()}.
     */
    /*
     * Moved to FailureDetection in v3.1.
     * V2
     */
     @Deprecated
     @Attribute
     //@Attribute (defaultValue="3")
     //@Min(value=1)
     String getFdProtocolMaxTries();

    /**
     * Sets the value of the fdProtocolMaxTries property.
     *
     * @param value allowed object is
     *              {@link String }
     * @deprecate
     * Replaced by {@link FailureDetection.setMaxMissedHeartbeats(String)}
     */
    /*
     * Moved to FailureDetection in v3.1.
     */
     @Deprecated
     void setFdProtocolMaxTries(String value) throws PropertyVetoException;


    /**
     * Gets the value of the fdProtocolTimeoutInMillis property.
     *
     * Period of time between monitoring attempts to detect failure.
     * Must be a positive integer.
     *
     * @return possible object is
     *         {@link String }
     * @deprecate
     * Replaced by {@link FailureDetection.getHeartbeatFrequency()}.
     */
    /*
     * Moved to FailureDetection in v3.1.
     */
     @Attribute
     @Deprecated
     //@Attribute (defaultValue="2000")
     //@Min(value=1000)
     //@Max(value=120000)
     String getFdProtocolTimeoutInMillis();

    /**
     * Sets the value of the fdProtocolTimeoutInMillis property.
     *
     * @param value allowed object is
     *              {@link String }
     * @deprecate
     * Replaced by {@link FailureDetection.setHeartbeatFrequency(String)}.
     */
    /*
     * Moved to FailureDetection in v3.1.
     */
    @Deprecated
    void setFdProtocolTimeoutInMillis(String value) throws PropertyVetoException;


    /**
     * Gets the value of the mergeProtocolMaxIntervalInMillis property.
     *
     * Specifies the maximum amount of time to wait to collect sub-group
     * information before performing a merge. Must be a positive integer.
     *
     * @return possible object is
     *         {@link String }
     * @deprecate
     */
    //@Attribute (defaultValue="10000")
    @Deprecated
    @Attribute
    //@Min(value=10000)
    //@Max(value=15000)
    String getMergeProtocolMaxIntervalInMillis();

    /**
     * Sets the value of the mergeProtocolMaxIntervalInMillis property.
     *
     * @param value allowed object is
     *              {@link String }
     * @deprecate
     */
    /* Not needed by gms in v3.1, was not used in v2. */
    @Deprecated
    void setMergeProtocolMaxIntervalInMillis(String value) throws PropertyVetoException;

    /**
     * Gets the value of the mergeProtocolMinIntervalInMillis property.
     *
     * Specifies the minimum amount of time to wait to collect sub-group
     * information before performing a merge. Must be a positive integer
     *
     * @return possible object is
     *         {@link String }
     * @deprecate
     */
    /* Not needed by gms in v3.1, was not used in v2. Remove default value.*/
    @Deprecated
    @Attribute
    //@Attribute (defaultValue="5000")
    //@Min(value=1000)
    //@Max(value=10000)
    String getMergeProtocolMinIntervalInMillis();

    /**
     * Sets the value of the mergeProtocolMinIntervalInMillis property.
     *
     * @param value allowed object is
     *              {@link String }
     * @deprecate
     */
    @Deprecated
    void setMergeProtocolMinIntervalInMillis(String value) throws PropertyVetoException;

    /**
     * Gets the value of the pingProtocolTimeoutInMillis property.
     *
     * Amount of time in milliseconds that GMS waits for discovery of other
     * members in this group. Must be a positive integer.
     *
     * @return possible object is
     *         {@link String }
     * @deprecate
     * @see #getGroupDiscoveryTimeoutInMillis()
     */
    /* renamed in v3.1 */
    @Deprecated
    @Attribute
    //@Attribute (defaultValue="5000")
    //@Min(value=1000)
    //@Max(value=120000)
    String getPingProtocolTimeoutInMillis();

    /**
     * Sets the value of the pingProtocolTimeoutInMillis property.
     *
     * @param value allowed object is
     *              {@link String }
     * @deprecate
     * @see #setGroupDiscoveryTimeoutInMillis(String)
     */
    /* renamed in v3.1 to GroupDiscoveryTimeoutInMillis */
    @Deprecated
    void setPingProtocolTimeoutInMillis(String value) throws PropertyVetoException;

    /**
     * Gets the value of the vsProtocolTimeoutInMillis property.
     *
     * After this timeout a suspected failure is marked as verified.
     * Must be a positive integer.
     *
     * @return possible object is
     *         {@link String }
     * @deprecate
     * Replaced by {@link FailureDetection.getVerifyFailureWaittimeInMillis()}.
     */
    /*
     * Moved to FailureDetection in v3.1.
     * V2
     */
    @Deprecated
    @Attribute
    //@Attribute (defaultValue="1500")
    //@Min(value=1500)
    //@Max(value=120000)
    String getVsProtocolTimeoutInMillis();

    /**
     * Sets the value of the vsProtocolTimeoutInMillis property.
     *
     * @param value allowed object is
     *              {@link String }
     * @deprecate
     * Replaced by {@link FailureDetection.setVerifyFailureWaittimeInMillis(String)}.
     */
    /* Moved to FailureDetection in v3.1
     */
    @Deprecated
    void setVsProtocolTimeoutInMillis(String value) throws PropertyVetoException;


    /**
    	Properties as per {@link org.jvnet.hk2.config.types.PropertyBag}
     */
    @ToDo(priority=ToDo.Priority.IMPORTANT, details="Provide PropertyDesc for legal props" )
    @PropertiesDesc(props={})
    @Element
    List<Property> getProperty();
}
