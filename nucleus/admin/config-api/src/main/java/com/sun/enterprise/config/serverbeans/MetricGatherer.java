/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2011 Oracle and/or its affiliates. All rights reserved.
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

import com.sun.enterprise.config.serverbeans.customvalidators.NotTargetKeyword;
import org.glassfish.api.Param;
import org.glassfish.api.admin.config.Named;
import org.glassfish.api.admin.config.ReferenceContainer;
import org.jvnet.hk2.component.Injectable;
import org.jvnet.hk2.config.Attribute;
import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.config.Configured;
import java.beans.PropertyVetoException;

import javax.validation.Payload;
import javax.validation.constraints.Pattern;

import static org.glassfish.config.support.Constants.NAME_SERVER_REGEX;

/**
 * Created by IntelliJ IDEA.
 * User: cmott
 * Date: 8/19/11
 * Time: 11:07 AM
 * To change this template use File | Settings | File Templates.
 */
@Configured
public interface MetricGatherer extends Injectable, ConfigBeanProxy, Named, ReferenceContainer, RefContainer, Payload {

    /**
     * Sets the metric type
     * @param value type
     * @throws PropertyVetoException if a listener vetoes the change
     */
    @Param(name="name", primary = true)
    @Override
    public void setName(String value) throws PropertyVetoException;

    @Override
    public String getName();

    /*
     *  sets the collection rate for data collection  in milliseconds
     * @param value collection rate
     * @throws PropertyVetoException if a listener vetoes the change
     */
    @Param(name="collection-rate",defaultValue = "15")
    public void setCollectionRate(int value) throws PropertyVetoException;

    @Attribute(defaultValue = "15")
    public int getCollectionRate();

    /*
     *  sets how long the data will be retain in the system. in hours
     * @param value retain data
     * @throws PropertyVetoException if a listener vetoes the change
     */
    @Param(name="retain-data", defaultValue = "2")
    public void setRetainData(int value) throws PropertyVetoException;

    @Attribute(defaultValue = "2")
    public int getRetainData();



}
