/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2010 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.webservices.monitoring;

import com.sun.xml.ws.transport.http.servlet.ServletAdapter;
import org.glassfish.external.probe.provider.annotations.ProbeListener;
import org.glassfish.external.probe.provider.annotations.ProbeParam;
import org.glassfish.external.statistics.Statistic;
import org.glassfish.external.statistics.Stats;
import org.glassfish.gmbal.*;
import org.glassfish.webservices.deployment.DeployedEndpointData;

import javax.servlet.ServletContext;
import java.util.*;


/**
 * Provides statistics for Web Service endpoints.
 * 
 * For deployment - keeps track of 109 and sun-jaxws.xml style deployed
 * applications.
 *
 * @author Jitendra Kotamraju
 */
@AMXMetadata(type="web-service-mon", group="monitoring")
@ManagedObject
@Description("Stats for Web Services deployed")
public class WebServiceStatsProvider {

    // path (context path+url-pattern) --> deployed data
    private final Map<String, DeployedEndpointData> endpoints =
            new HashMap<String, DeployedEndpointData>();

    // Only RI endpoints
    private final Map<String, List<DeployedEndpointData>> riEndpoints =
            new HashMap<String, List<DeployedEndpointData>>();

    // sun-jaxws.xml deployment
    @ProbeListener("glassfish:webservices:deployment-ri:deploy")
    public synchronized void riDeploy(@ProbeParam("adapter")ServletAdapter adapter) {
        String contextPath = adapter.getServletContext().getContextPath();
        String path = contextPath+adapter.getValidPath();
        DeployedEndpointData data = endpoints.get(path);
        if (data == null) {
            data = new DeployedEndpointData(path, adapter);
            endpoints.put(path, data);
        }

        List<DeployedEndpointData> ri = riEndpoints.get(contextPath);
        if (ri == null) {
            ri = new ArrayList<DeployedEndpointData>();
            riEndpoints.put(contextPath, ri);
        }
        ri.add(data);
    }

    // sun-jaxws.xml undeployment
    @ProbeListener("glassfish:webservices:deployment-ri:undeploy")
    public synchronized void riUndeploy(@ProbeParam("adapter")ServletAdapter adapter) {
        ServletContext ctxt = adapter.getServletContext();
        String name = ctxt.getContextPath()+adapter.getValidPath();
        DeployedEndpointData data = endpoints.remove(name);

        String contextPath = adapter.getServletContext().getContextPath();
        List<DeployedEndpointData> ri = riEndpoints.get(contextPath);
        if (ri != null) {
            ri.remove(data);
            if (ri.isEmpty()) {
                riEndpoints.remove(contextPath);
            }
        }
    }

    // admin CLI doesn't pick-up Collection<DeployedEndpointData>. Hence
    // implementing "Stats"
    @ManagedAttribute
    @Description("Deployed Web Service Endpoints")
    public synchronized MyStats getEndpoints() {
        return new MyStats(endpoints);
    }

    // Returns all the RI endpoints for context root
    @ManagedOperation
    public synchronized List<Map<String, String>> getRiEndpoint(String contextPath) {
        List<Map<String, String>> list = new ArrayList<Map<String, String>>();
        List<DeployedEndpointData> ri = riEndpoints.get(contextPath);
        if (ri != null) {
            for(DeployedEndpointData de : ri) {
                list.add(de.getStaticAsMap());
            }
        }
        return list;
    }

    @ManagedData
    private static class MyStats implements Stats {

        final Map<String, DeployedEndpointData> endpoints = new HashMap<String, DeployedEndpointData>();
        final DeployedEndpointData[] data;

        MyStats(Map<String, DeployedEndpointData> curEndpoints) {
            endpoints.putAll(curEndpoints);     // Take a snapshot of current endpoints
            data = this.endpoints.values().toArray(new DeployedEndpointData[endpoints.size()]);
        }

        public Statistic getStatistic(String s) {
            return endpoints.get(s);
        }

        public String[] getStatisticNames() {
            Set<String> names = endpoints.keySet();
            return names.toArray(new String[names.size()]);
        }

        @ManagedAttribute
        public DeployedEndpointData[] getStatistics() {
            return data;
        }
    }

}
