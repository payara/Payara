/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.admin.amx.intf.config;

import org.glassfish.admin.amx.intf.config.grizzly.NetworkConfig;
import org.glassfish.admin.amx.intf.config.grizzly.NetworkListener;

import java.util.Map;

@Deprecated
public interface Config
        extends NamedConfigElement, PropertiesAccess, SystemPropertiesAccess {


    public String getName();

    public void setName(String param1);

    public NetworkConfig getNetworkConfig();

    public IiopService getIiopService();

    public HttpService getHttpService();

    public SecurityService getSecurityService();

    public MonitoringService getMonitoringService();

    public AdminService getAdminService();

    public ThreadPools getThreadPools();

    public DiagnosticService getDiagnosticService();

    public WebContainer getWebContainer();

    public EjbContainer getEjbContainer();

    public MdbContainer getMdbContainer();

    public AlertService getAlertService();

    public JmsService getJmsService();

    public LogService getLogService();

    public TransactionService getTransactionService();

    public AvailabilityService getAvailabilityService();

    public ConnectorService getConnectorService();

    public GroupManagementService getGroupManagementService();

    public String getDynamicReconfigurationEnabled();

    public void setDynamicReconfigurationEnabled(String param1);

    public ManagementRules getManagementRules();

    public JavaConfig getJavaConfig();

    public void setEjbContainer(EjbContainer param1);

    public void setMdbContainer(MdbContainer param1);

    public void setWebContainer(WebContainer param1);

    public void setGroupManagementService(GroupManagementService param1);

    public void setConnectorService(ConnectorService param1);

    public void setJmsService(JmsService param1);

    public void setHttpService(HttpService param1);

    public void setTransactionService(TransactionService param1);

    public void setNetworkConfig(NetworkConfig param1);

    public void setIiopService(IiopService param1);

    public void setAdminService(AdminService param1);

    public void setLogService(LogService param1);

    public void setSecurityService(SecurityService param1);

    public void setMonitoringService(MonitoringService param1);

    public void setDiagnosticService(DiagnosticService param1);

    public void setJavaConfig(JavaConfig param1);

    public void setAvailabilityService(AvailabilityService param1);

    public void setThreadPools(ThreadPools param1);

    public void setAlertService(AlertService param1);

    public void setManagementRules(ManagementRules param1);

    public Map getLoggingProperties();

    public String setLoggingProperty(String param1, String param2);

    public Map updateLoggingProperties(Map param1);

    public NetworkListener getAdminListener();
}
