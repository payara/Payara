/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *  Copyright (c) [2018-2020] Payara Foundation and/or its affiliates. All rights reserved.
 * 
 *  The contents of this file are subject to the terms of either the GNU
 *  General Public License Version 2 only ("GPL") or the Common Development
 *  and Distribution License("CDDL") (collectively, the "License").  You
 *  may not use this file except in compliance with the License.  You can
 *  obtain a copy of the License at
 *  https://github.com/payara/Payara/blob/main/LICENSE.txt
 *  See the License for the specific
 *  language governing permissions and limitations under the License.
 * 
 *  When distributing the software, include this License Header Notice in each
 *  file and include the License.
 * 
 *  When distributing the software, include this License Header Notice in each
 *  file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
 * 
 *  GPL Classpath Exception:
 *  The Payara Foundation designates this particular file as subject to the "Classpath"
 *  exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *  file that accompanied this code.
 * 
 *  Modifications:
 *  If applicable, add the following below the License Header, with the fields
 *  enclosed by brackets [] replaced by your own identifying information:
 *  "Portions Copyright [year] [name of copyright owner]"
 * 
 *  Contributor(s):
 *  If you wish your version of this file to be governed by only the CDDL or
 *  only the GPL Version 2, indicate your decision by adding "[Contributor]
 *  elects to include this software in this distribution under the [CDDL or GPL
 *  Version 2] license."  If you don't indicate a single choice of license, a
 *  recipient has the option to distribute your version of this file under
 *  either the CDDL, the GPL Version 2 or to extend the choice of license to
 *  its licensees as provided above.  However, if you add GPL Version 2 code
 *  and therefore, elected the GPL Version 2 license, then the option applies
 *  only if the new code is made subject to such option by the copyright
 *  holder.
 */
package fish.payara.nucleus.hazelcast;

import com.hazelcast.cluster.Address;
import com.hazelcast.spi.discovery.DiscoveryNode;
import com.hazelcast.spi.discovery.SimpleDiscoveryNode;
import com.hazelcast.spi.discovery.integration.DiscoveryService;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;

/**
 *
 * @author jonathan coustick
 * @since 4.1.2.184
 */
public class DnsDiscoveryService implements DiscoveryService {

    private static final String DEFAULT_PORT = "4900";
    private static final String A_RECORD = "A"; //The A DNS Record

    private static final Logger LOGGER = Logger.getLogger(DnsDiscoveryService.class.getName());

    private final String[] settings;

    public DnsDiscoveryService(String[] settings) {
        this.settings = settings;
    }

    @Override
    public void start() {
        //no-op
    }

    @Override
    public Iterable<DiscoveryNode> discoverNodes() {

        LOGGER.log(Level.FINER, "Starting Domain Node Discovery");
        List<DiscoveryNode> nodes = new LinkedList<>();

        for (String host : settings) {
            String hostname;
            String port;
            int colon = host.indexOf(':');
            if (colon == -1) {
                hostname = host;
                port = DEFAULT_PORT;
            } else {
                hostname = host.substring(0, colon);
                port = host.substring(colon + 1);
            }
            try {
                InetAddress[] addresses = InetAddress.getAllByName(hostname);
                for (InetAddress address : addresses) {
                    if (!address.isLoopbackAddress()) {
                        LOGGER.log(Level.FINE, "Adding Node {0}", address);
                        nodes.add(new SimpleDiscoveryNode(new Address(address.getHostAddress(), Integer.valueOf(port))));
                    }
                }
            } catch (UnknownHostException ex) {
                LOGGER.log(Level.FINEST, ex.getMessage());
                // not a known host, do a DNS lookup
                try {
                    
                    DirContext urlContext = new InitialDirContext();
                    Attributes attributes = urlContext.getAttributes("dns:/" + hostname, new String[]{"A"});
                    NamingEnumeration record = attributes.get("A").getAll();
                    while (record.hasMore()) {
                        String address = record.next().toString();
                        LOGGER.log(Level.FINE, "Adding Node {0}", address);
                        nodes.add(new SimpleDiscoveryNode(new Address(address, Integer.valueOf(port))));
                        
                    }
                } catch (NamingException | UnknownHostException ex1) {
                    LOGGER.log(Level.WARNING, "Unable to find DNS record for {0}", hostname);
                }
            }

        }

        return nodes;
    }

    @Override
    public void destroy() {
        // do nothing
    }

    @Override
    public Map<String, String> discoverLocalMetadata() {
        return Collections.emptyMap();
    }

}
