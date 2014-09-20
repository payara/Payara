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

import org.glassfish.api.Param;
import javax.validation.Payload;
import org.jvnet.hk2.config.Attribute;
import org.jvnet.hk2.config.Configured;

import org.jvnet.hk2.config.types.PropertyBag;

import javax.validation.constraints.Pattern;
import java.beans.PropertyVetoException;

/**
 * This config bean will define parameters for Managed jobs
 * A Managed job is a commands which is annotated with either @ManagedJob,@Progress
 * or running with --detach
 * @author Bhakti Mehta
 */
@Configured
public interface ManagedJobConfig extends DomainExtension, PropertyBag, Payload {

    /**
     * Gets the value of inMemoryRetentionPeriod property
     * @return possible object is
     *         {@link String }
     */
    @Attribute(defaultValue="1h")
    @Pattern(regexp = "[1-9]\\d*([hms]|[HMS])" , message = "{invalid.time.period.specified}"
            , payload = ManagedJobConfig.class)
    String getInMemoryRetentionPeriod();

    /**
     * Sets the value of the inMemoryRetentionPeriod property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    @Param(name = "inmemoryretentionperiod", optional=true)
    void setInMemoryRetentionPeriod(String value) throws PropertyVetoException;

    /**
     * Gets the value of jobRetentionPeriod
     * @return
     */
    @Attribute(defaultValue="24h")
    @Pattern(regexp = "[1-9]\\d*([hms]|[HMS])" , message = "{invalid.time.period.specified}" ,
            payload = ManagedJobConfig.class)
    String getJobRetentionPeriod();

    /**
     * Sets the value of the jobRetentionPeriod property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    @Param(name = "jobretentionperiod", optional=true)
    void setJobRetentionPeriod(String value) throws PropertyVetoException;


    /**
     * Gets the value of persistingEnabled property
     * @return
     */
    @Attribute (defaultValue="true",dataType=Boolean.class)
    boolean getPersistingEnabled();


    /**
     * Sets the value of the persistingenabled property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setPersistingEnabled(boolean value) throws PropertyVetoException;

    /**
     * Gets the value of pollInterval property
     * @return possible object is
     *         {@link String }
     */
    @Attribute(defaultValue="20m")
    @Pattern(regexp = "[1-9]\\d*([hms]|[HMS])" , message = "{invalid.time.period.specified}"
            , payload = ManagedJobConfig.class)
    String getPollInterval();

    /**
     * Sets the value of the pollInterval property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    @Param(name = "pollinterval", optional=true)
    void setPollInterval(String value) throws PropertyVetoException;

    /**
     * Gets the value of initialDelay
     * @return
     */
    @Attribute(defaultValue="20m")
    @Pattern(regexp = "[1-9]\\d*([hms]|[HMS])" , message = "{invalid.time.period.specified}" ,
            payload = ManagedJobConfig.class)
    String getInitialDelay();

    /**
     * Sets the value of the initialDelay property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    @Param(name = "initialdelay", optional=true)
    void setInitialDelay(String value) throws PropertyVetoException;





}
