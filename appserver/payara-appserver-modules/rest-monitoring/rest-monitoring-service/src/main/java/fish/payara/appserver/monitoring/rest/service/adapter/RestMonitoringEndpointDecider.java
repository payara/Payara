/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2016-2021] Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
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

package fish.payara.appserver.monitoring.rest.service.adapter;

import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.ServerTags;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import jakarta.inject.Inject;
import org.glassfish.grizzly.config.dom.NetworkListener;
import org.glassfish.grizzly.config.dom.ThreadPool;
import org.glassfish.hk2.api.ServiceLocator;
import fish.payara.appserver.monitoring.rest.service.configuration.RestMonitoringConfiguration;

/**
 * This class encapsulates the process of resolving the actual endpoint of the Rest Monitoring application.
 * @author Andrew Pielage
 */
public class RestMonitoringEndpointDecider {
    private String contextRoot;
    
    private int port;
    private InetAddress address;
    private int maxThreadPoolSize = 5;
    private Config config;
    private static final Logger logger = Logger.getLogger(RestMonitoringEndpointDecider.class.getName());
    private List<String> hosts;
    private RestMonitoringConfiguration restMonitoringServiceConfiguration;
    
    private final static String DEFAULT_CONTEXT_ROOT = "/rest-monitoring";
    
    public static final int DEFAULT_ADMIN_PORT = 4848;
    
    @Inject
    ServiceLocator habitat;
    
    public RestMonitoringEndpointDecider(Config config, RestMonitoringConfiguration restMonitoringServiceConfiguration) {
        if (config == null || logger == null)
            throw new IllegalArgumentException("config or logger can't be null");
        this.config = config;
        this.restMonitoringServiceConfiguration = restMonitoringServiceConfiguration;
        setValues();
    }
    
    public int getListenPort() {
        return port;
    }

    public InetAddress getListenAddress() {
        return address;
    }

    public int getMaxThreadPoolSize() {
        return maxThreadPoolSize;
    }
    
    public String getContextRoot() {
        return contextRoot;
    }
    
    private void setValues() {
        NetworkListener networkListener = config.getAdminListener();
        ThreadPool threadPool = networkListener.findThreadPool();
        
        // Set Thread pool size
        if (threadPool != null) {
            try {
                maxThreadPoolSize = Integer.parseInt(threadPool.getMaxThreadPoolSize());
            } catch (NumberFormatException ex) {
                
            }
        }
        
        String defaultVirtualServer = networkListener.findHttpProtocol().getHttp().getDefaultVirtualServer();
        hosts = Collections.unmodifiableList(Arrays.asList(defaultVirtualServer));
        
        // Set network address
        try {
            address = InetAddress.getByName(networkListener.getAddress());
        } catch (UnknownHostException e) {
            throw new IllegalStateException(e);
        }
        
        // Set the context root and port number
        if (ServerTags.ADMIN_LISTENER_ID.equals(networkListener.getName())) {
            // Get the context root from the rest monitoring service
            if (restMonitoringServiceConfiguration == null) {
                contextRoot = DEFAULT_CONTEXT_ROOT;
            } else {
                contextRoot = restMonitoringServiceConfiguration.getContextRoot();
            }
            
            try {
                port = Integer.parseInt(networkListener.getPort());
            } catch(NumberFormatException ne) {
                port = DEFAULT_ADMIN_PORT;
            }
        }
        else {
            try {
                port = Integer.parseInt(networkListener.getPort());
            } catch(NumberFormatException ne) {
                port = DEFAULT_ADMIN_PORT;
            }
            
            // Get the context root from the rest monitoring service
            if (restMonitoringServiceConfiguration == null) {
                contextRoot = DEFAULT_CONTEXT_ROOT;
            } else {
                contextRoot = restMonitoringServiceConfiguration.getContextRoot();
            }
        }
    }
    
    public List<String> getHosts() {
        return hosts;
    }
}
