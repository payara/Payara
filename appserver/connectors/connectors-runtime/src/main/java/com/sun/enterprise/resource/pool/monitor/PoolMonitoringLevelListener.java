/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2012 Oracle and/or its affiliates. All rights reserved.
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
package com.sun.enterprise.resource.pool.monitor;

import com.sun.enterprise.config.serverbeans.*;
import com.sun.logging.LogDomains;
import org.glassfish.server.ServerEnvironmentImpl;

import org.jvnet.hk2.annotations.Service;
import org.glassfish.hk2.api.PostConstruct;
import org.glassfish.hk2.api.PreDestroy;
import javax.inject.Singleton;
import org.jvnet.hk2.config.*;

import java.beans.PropertyChangeEvent;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;

@Service
@Singleton
public class PoolMonitoringLevelListener implements PostConstruct, PreDestroy, ConfigListener {

    @Inject
    private ServerEnvironmentImpl serverEnvironment;

    @Inject
    private Domain domain;

    private ModuleMonitoringLevels monitoringLevel;

    private boolean jdbcPoolMonitoringEnabled ;
    private boolean connectorPoolMonitoringEnabled;

    private static final Logger _logger = LogDomains.getLogger(PoolMonitoringLevelListener.class, LogDomains.RSR_LOGGER);

    public void postConstruct() {
        String instanceName = serverEnvironment.getInstanceName();
        Server server = domain.getServerNamed(instanceName);
        Config config = server.getConfig();
        if (config != null) {
            MonitoringService monitoringService = config.getMonitoringService();
            if (monitoringService != null) {
                ModuleMonitoringLevels monitoringLevel = monitoringService.getModuleMonitoringLevels();
                if (monitoringLevel != null) {
                    this.monitoringLevel = monitoringLevel;
                    ObservableBean bean = (ObservableBean) ConfigSupport.getImpl((ConfigBeanProxy) monitoringLevel);
                    bean.addListener(this);
                    jdbcPoolMonitoringEnabled = !monitoringLevel.getJdbcConnectionPool().equalsIgnoreCase("OFF");
                    connectorPoolMonitoringEnabled = !monitoringLevel.getConnectorConnectionPool().equalsIgnoreCase("OFF");
                }
            }
        }
    }

    public void preDestroy() {
        if(monitoringLevel != null){
            ObservableBean bean = (ObservableBean) ConfigSupport.getImpl((ConfigBeanProxy)monitoringLevel);
            bean.removeListener(this);
        }
    }

    public UnprocessedChangeEvents changed(PropertyChangeEvent[] events) {
        return ConfigSupport.sortAndDispatch(events, new PropertyChangeHandler(events), _logger);
    }

    class PropertyChangeHandler implements Changed {

            private PropertyChangeHandler(PropertyChangeEvent[] events) {
            }

            /**
             * Notification of a change on a configuration object
             *
             * @param type            type of change : ADD mean the changedInstance was added to the parent
             *                        REMOVE means the changedInstance was removed from the parent, CHANGE means the
             *                        changedInstance has mutated.
             * @param changedType     type of the configuration object
             * @param changedInstance changed instance.
             */
            public <T extends ConfigBeanProxy> NotProcessed changed(TYPE type, Class<T> changedType, T changedInstance) {
                NotProcessed np = null;

                    switch (type) {
                        case CHANGE:
                            if(_logger.isLoggable(Level.FINE)) {
                                _logger.fine("A " + changedType.getName() + " was changed : " + changedInstance);
                            }
                            np = handleChangeEvent(changedInstance);
                            break;
                        default:
                    }
                    return np;
            }
            private <T extends ConfigBeanProxy> NotProcessed handleChangeEvent(T instance) {
                NotProcessed np = null;
                if(instance instanceof ModuleMonitoringLevels){
                    ModuleMonitoringLevels mml = (ModuleMonitoringLevels)instance;
                    connectorPoolMonitoringEnabled = !mml.getConnectorConnectionPool().equalsIgnoreCase("OFF");
                    jdbcPoolMonitoringEnabled = !mml.getJdbcConnectionPool().equalsIgnoreCase("OFF");
                }
                return np;
            }
        }

    public boolean getJdbcPoolMonitoringEnabled(){
        return jdbcPoolMonitoringEnabled;
    }

    public boolean getConnectorPoolMonitoringEnabled(){
        return connectorPoolMonitoringEnabled;
    }
}
