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
import com.sun.enterprise.config.serverbeans.customvalidators.NotDuplicateTargetName;
import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.enterprise.util.net.NetUtils;
import com.sun.enterprise.util.StringUtils;
import com.sun.logging.LogDomains;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.config.support.*;
import static org.glassfish.config.support.Constants.*;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.Habitat;
import org.jvnet.hk2.component.PerLookup;
import org.jvnet.hk2.config.*;
import org.jvnet.hk2.component.Injectable;
import org.glassfish.api.admin.config.Named;
import org.glassfish.api.admin.config.ReferenceContainer;

import javax.validation.Payload;
import java.beans.PropertyVetoException;
import java.io.File;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.validation.constraints.Pattern;

/**
 * A cluster defines a homogeneous set of service instances that share the same
 * applications, resources, and configuration.
 */
@Configured
@SuppressWarnings("unused")
@NotDuplicateTargetName(message="{elastic.service.duplicate.name}", payload=ElasticService.class)
public interface ElasticService extends ConfigBeanProxy, Injectable, Named, ReferenceContainer, RefContainer, Payload {

    /**
     * Sets the elastic service name
     * @param value elastic name
     * @throws PropertyVetoException if a listener vetoes the change
     */
    @Param(name="name", primary = true)
    @Override
    public void setName(String value) throws PropertyVetoException;

    @NotTargetKeyword(message="{elastic.service.reserved.name}", payload=ElasticService.class)
    @Pattern(regexp=NAME_SERVER_REGEX, message="{elastic.service.invalid.name}", payload=ElasticService.class)
    @Override
    public String getName();

    /**
         * Gets the value of the metric-gatherer property.
         *
         * @return possible object is
         *         {@link MetricGatherers }
         */
    @Element
    MetricGatherers getMetricGatherers();

    void setMetricGatherers(MetricGatherers metricGatherers);

     /**
     * Gets the value of the alerts property.
     *
     * @return possible object is
     *         {@link Alerts }
     */
    @Element
    Alerts getAlerts();

    /**
     * Sets the Alerts element
     * @param alerts
     */

    void setAlerts(Alerts alerts);
    /**
         * Gets the value of the actions property.
         *
         * @return possible object is
         *         {@link Actions }
         */
//    @Element
//    Actions getActions();


    @Service
    @Scoped(PerLookup.class)
    class Decorator implements CreationDecorator<ElasticService> {
        /**
          * Decorates the newly CRUD created elastic configuration instance.
          * tasks :
          *      - create the Alerts subelement
          *
          * @param context administration command context
          * @param instance newly created configuration element
          * @throws TransactionFailure
          * @throws PropertyVetoException
          */
         @Override
         public void decorate(AdminCommandContext context, final ElasticService instance) throws TransactionFailure, PropertyVetoException {

             //create the metric gatherers for CPU and URL.  Need a command to update the URL gatherer

             MetricGatherers mgs = instance.createChild(MetricGatherers.class);
             MetricGatherer mg  =  mgs.createChild(MetricGatherer.class);
             //create memory gatherer and take defaults
             mg.setName( "memory");
             mgs.getMetricGatherer().add(mg);
             instance.setMetricGatherers(mgs);

         }
    }
}
