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

package org.glassfish.elasticity.config.serverbeans;

import com.sun.enterprise.config.serverbeans.*;

import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.config.support.*;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.*;
import java.beans.PropertyVetoException;
import java.util.List;

/**
 * A cluster defines a homogeneous set of service instances that share the same
 * applications, resources, and configuration.
 */
@Configured
public interface ElasticService extends ConfigBeanProxy  {

    /**
     * Sets the elastic service name
     * @param value elastic name
     * @throws PropertyVetoException if a listener vetoes the change
     */
    @Param(name="name", primary = true)
    public void setName(String value) throws PropertyVetoException;

    @Attribute
    public String getName();

    /*
     * Sets the service elastic enabled value
     * @param value service elastic enabled
     * @throws PropertyVetoException if a listener vetoes the change
     */

    @Param (name="enabled", optional=true, defaultValue = "true")
    public void setEnabled(boolean value) throws PropertyVetoException;
    /*
     * Get the service elastic enabled value
     * @return  service elastic enabled  value
     */
    @Attribute(defaultValue = "true")
    public boolean getEnabled();

    /*
     * Sets the service elastic min value
     * @param value service elastic min
     * @throws PropertyVetoException if a listener vetoes the change
     */
    @Param (name="min", optional=true, defaultValue = "1")
    public void setMin(int value) throws PropertyVetoException;
    /*
     * Get the service elastic min value
     * @return  service elastic min  value
     */

    @Attribute(defaultValue = "1")
    public int getMin();

    /*
     * Sets the service elastic max value
     * @param value service elastic max
     * @throws PropertyVetoException if a listener vetoes the change
     */
    @Param (name="max", optional=true, defaultValue = "2")
    public void setMax(int value) throws PropertyVetoException;
    /*
     * Get the service elastic max value
     * @return  service elastic max  value
     */

    @Attribute(defaultValue = "2")
    public int getMax();

    /**
     * Sets the reconfig scale-up wait period in seconds
     * @param value reconfig scale-up
     * @throws PropertyVetoException if a listener vetoes the change
     */
    @Param(name="reconfig-scale-up", optional=true,  defaultValue="300")
    public void setReconfigScaleUp(int value) throws PropertyVetoException;
    /*
     * Get the reconfig scale up value
     * @return  reconfig scale up value
     */
    @Attribute (defaultValue = "300")
    public int getReconfigScaleUp();

    /**
     * Sets the reconfig scale-dowm wait period in seconds
     * @param value reconfig scale-down
     * @throws PropertyVetoException if a listener vetoes the change
     */
    @Param(name="reconfig-scale-down", optional=true, defaultValue="300")
    public void setReconfigScaleDown(int value) throws PropertyVetoException;
    /*
     * Get the reconfig scale down value
     * @return  reconfig scale down value
     */
    @Attribute (defaultValue = "300")
    public int getReconfigScaleDown();

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
    @Element
    Actions getActions();

     /**
     * Sets the Actions element
     * @param actions
     */

    void setActions(Actions actions);

    @Service
    public class ElasticServiceResolver implements CrudResolver {

        @Param(name="name")
        String name;

        @Inject
        ElasticServices elasticServices;

        @Override
        @SuppressWarnings("unchecked")
        public <T extends ConfigBeanProxy> T resolve(AdminCommandContext context, Class<T> type)  {
            for (ElasticService elasticService : elasticServices.getElasticService()) {
                if (elasticService.getName().equals(name)) {
                    return (T) elasticService;
                }
            }
            return null;
        }
    }

    @Service
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
             //for now, create memory gatherer and take defaults

             MetricGatherers mgs = instance.createChild(MetricGatherers.class);
             MetricGatherer mg  =  mgs.createChild(MetricGatherer.class);
             mg.setName( "memory");
             mgs.getMetricGatherer().add(mg);
             instance.setMetricGatherers(mgs);

             // create the scale up action element

             Actions actionsS = instance.createChild(Actions.class);
             ScaleUpAction scaleUpAction = actionsS.createChild(ScaleUpAction.class);
             scaleUpAction.setName("scale-up-action");
             actionsS.setScaleUpAction(scaleUpAction);
             instance.setActions(actionsS);

             Alerts alerts = instance.createChild(Alerts.class);
             instance.setAlerts(alerts);
         }
    }

}
