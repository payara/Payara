/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2016-2017] Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
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
package fish.payara.nucleus.hazelcast;

import com.hazelcast.nio.Address;
import com.hazelcast.spi.discovery.DiscoveryNode;
import com.hazelcast.spi.discovery.SimpleDiscoveryNode;
import com.hazelcast.spi.discovery.integration.DiscoveryService;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.Node;
import com.sun.enterprise.util.io.InstanceDirs;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.internal.api.Globals;
import org.glassfish.internal.api.ServerContext;

/**
 * Provides a Discovery SPI implementation for Hazelcast that uses knowledge 
 * of the domain topology to build out the cluster and discover members
 * @since 5.0
 * @author steve
 */
public class DomainDiscoveryService implements DiscoveryService {

    @Override
    public void start() {
        //
    }

    @Override
    public Iterable<DiscoveryNode> discoverNodes() {
        List<DiscoveryNode> nodes = new LinkedList<>();
        Domain domain = Globals.getDefaultHabitat().getService(Domain.class);
        ServerContext ctxt = Globals.getDefaultHabitat().getService(ServerContext.class);
        ServerEnvironment env = Globals.getDefaultHabitat().getService(ServerEnvironment.class);
        // add the DAS
        HazelcastRuntimeConfiguration hzConfig = domain.getExtensionByType(HazelcastRuntimeConfiguration.class);

        if (!env.isDas()) {
            try {
                // first get hold of the DAS host
                String dasHost = hzConfig.getDASPublicAddress();
                if (dasHost == null || dasHost.isEmpty()) {
                    dasHost = hzConfig.getDASBindAddress();
                }
                
                if (dasHost.isEmpty()) {
                    // ok drag it off the properties file
                    InstanceDirs instance = new InstanceDirs(env.getInstanceRoot());
                    Properties dasProps = new Properties();
                    dasProps.load(new FileInputStream(instance.getDasPropertiesFile()));
                    dasHost = dasProps.getProperty("agent.das.host");
                    // then do an IP lookup
                    dasHost = InetAddress.getByName(dasHost).getHostAddress();
                }
                    
                if (dasHost.isEmpty() || dasHost.equals("localhost")) {
                        Enumeration e = NetworkInterface.getNetworkInterfaces();
                        while (e.hasMoreElements()) {
                            NetworkInterface ni = (NetworkInterface) e.nextElement();
                            if (!ni.isLoopback()) {
                                for (InterfaceAddress ia : ni.getInterfaceAddresses()) {
                                    nodes.add(new SimpleDiscoveryNode(new Address(ia.getAddress(), Integer.valueOf(hzConfig.getDasPort()))));
                                }
                            }
                        }
                } else {
                    nodes.add(new SimpleDiscoveryNode(new Address(dasHost, Integer.valueOf(hzConfig.getDasPort()))));
                }

                // also add all nodes we are aware of
                for (Node node : domain.getNodes().getNode()) {
                    nodes.add(new SimpleDiscoveryNode(new Address(node.getNodeHost(), Integer.valueOf(hzConfig.getStartPort()))));
                }
            } catch (IOException ex) {
                Logger.getLogger(DomainDiscoveryService.class.getName()).log(Level.SEVERE, null, ex);
            }
            
        } else if (env.isMicro()) {
            try {
                nodes.add(new SimpleDiscoveryNode(new Address(hzConfig.getDASPublicAddress(), Integer.valueOf(hzConfig.getDasPort()))));
            } catch (UnknownHostException ex) {
                Logger.getLogger(DomainDiscoveryService.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            // ok this is the DAS
            for (Node node : domain.getNodes().getNode()) {
                try {
                    nodes.add(new SimpleDiscoveryNode(new Address(node.getNodeHost(),Integer.valueOf(hzConfig.getStartPort()))));
                } catch (UnknownHostException ex) {
                    Logger.getLogger(DomainDiscoveryService.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        return nodes;
    }

    @Override
    public void destroy() {
        //
    }

    @Override
    public Map<String, Object> discoverLocalMetadata() {
        return Collections.EMPTY_MAP;
    }
    
}
