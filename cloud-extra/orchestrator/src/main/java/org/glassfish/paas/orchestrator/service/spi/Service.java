/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2012 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.paas.orchestrator.service.spi;

import org.glassfish.paas.orchestrator.service.ServiceType;
import org.glassfish.paas.orchestrator.service.metadata.ServiceDescription;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;

/**
 * Generic representation of a Service which can be
 * of types, managed or unmanaged.
 *
 * @author Sivakumar Thyagarajan, Jagadish Ramu
 * @author Bhavanishankar S
 */
public interface Service {

    /**
     * type of service
     * @return ServiceType
     */
    public ServiceType getServiceType();

    /**
     * ServiceDescription pertaining to the service.
     * @return ServiceDescription
     */
    public ServiceDescription getServiceDescription();

    /**
     * service specific properties that could be used to get
     * service related information.
     * @return Properties
     */
    public Properties getServiceProperties();

    /**
     * name of the service
     * @return String name
     */
    public String getName();

    /**
     * returns the list of Child Services for this Service<br>
     * eg: GlassFish service is represented by multiple instances of a cluster.
     * @return Set<Service>
     */
    public Set<Service> getChildServices();

    /**
     * placeholder for extra-properties.
     * @return Properties
     */
    public Properties getProperties();

    /**
     * Collect the log records since the given time for a given level and type.
     * <p/>
     * Some implementation considerations:
     * <p/>
     * (a) Convert plain text file lines into ServiceLogRecord.
     * (a) Map java.util.logging.Level to service specific log level.
     * (b) Prefer service exposed transport over vm.executeOn(...) to collect logs.
     * (c) If possible, decorate the log for isolating application usage in case of
     * service being a shared service:
     * <p/>
     * For example:
     * <p/>
     * If app1 and app2 are both using the service, an external entity should be
     * able to filter logs pertaining only to app1 (or app2). One way to achieve is
     * by decorating the message being logged with application specific information.
     * We need the underlying service to provide such capability and
     * Plugin implementation must know how to configure it at the time of
     * provisioning the service or at the time of associating the
     * application with an existing provisioned (shared) service.
     *
     * @param type collect logs only for this type
     * @param level collect logs only for this level
     * @param since collect logs since this time till the latest
     * @return service log records matching the input criteria. When the service has
     * multiple nodes each node will have set of service records, otherwise the key
     * for the map is this service itself.
     */

    public Map<Service, List<ServiceLogRecord>> collectLogs(ServiceLogType type,
                                             Level level, Date since);

    /**
     * Collect the most recent requested number of log records for a
     * given level and type.
     *
     * @param type collect logs only for this type
     * @param level collect logs only for this level
     * @param count collect most recent given count of log records.
     * @return service log records matching the input criteria. When the service has
     * multiple nodes each node will have set of service records, otherwise the key
     * for the map is this service itself.
     */
    public Map<Service, List<ServiceLogRecord>> collectLogs(ServiceLogType type,
                                             Level level, long count);


    /**
     * Get the available log types for the service.
     * <p/>
     * For example :
     * <p/>
     * Java EE service might have logs for HTTP access info, debug logs, jvm logs, etc
     * <p/>
     * Messaging service might contain MTA (message transfer agent) logs,
     * Error logs, Message Store and Service logs.
     *
     * @return set of log types for the service.
     */
    public Set<ServiceLogType> getLogTypes();

    /**
     * Get the default log type for the service.
     *
     * @return default log type
     */
    public ServiceLogType getDefaultLogType();
}
