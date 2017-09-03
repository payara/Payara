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

import com.hazelcast.logging.ILogger;
import com.hazelcast.nio.Address;
import com.hazelcast.spi.discovery.AbstractDiscoveryStrategy;
import com.hazelcast.spi.discovery.DiscoveryNode;
import com.hazelcast.spi.discovery.SimpleDiscoveryNode;
import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.Node;
import com.sun.enterprise.config.serverbeans.Server;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.glassfish.internal.api.Globals;

/**
 * A Hazelcast Discovery Strategy that uses the domain.xml to discovery new 
 * nodes in the Hazelcast cluster
 * First it tries to connect to the DAS and then it tries other instances in turn
 * @since 5.0.0
 * @author steve
 */
public class DomainDiscoveryMode extends AbstractDiscoveryStrategy {
    
    public DomainDiscoveryMode(ILogger logger, Map<String,Comparable> properties) {
        super(logger,properties);
    }

    @Override
    public Iterable<DiscoveryNode> discoverNodes() {
        List<DiscoveryNode> nodes = new LinkedList<>();
        Domain domain = Globals.getDefaultHabitat().getService(Domain.class);
        // add all servers
        domain.getNodes().getNode().forEach((node) -> {
            domain.getServers().getServersOnNode(node).forEach((server) -> {
                HazelcastRuntimeConfiguration hzConfig = server.getConfig().getExtensionByType(HazelcastRuntimeConfiguration.class);
                try {
                    nodes.add(new SimpleDiscoveryNode(new Address(node.getNodeHost(), Integer.parseInt(hzConfig.getStartPort()))));
                } catch (UnknownHostException ex) {
                    Logger.getLogger(DomainDiscoveryMode.class.getName()).log(Level.SEVERE, null, ex);
                }
            });
        });
        return nodes;
    }

}
