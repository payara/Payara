/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2017 Payara Foundation and/or its affiliates. All rights reserved.
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

import com.hazelcast.spi.MemberAddressProvider;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import org.glassfish.api.admin.ServerEnvironment;
import java.util.logging.Logger;

/**
 * This class tries to work out which interface to choose as the address of the member
 * broadcast within Hazelcast
 * @author Steve Millidge (Payara Foundation)
 */
public class MemberAddressPicker implements MemberAddressProvider {

    private final ServerEnvironment env;
    private final HazelcastRuntimeConfiguration config;
    private final HazelcastConfigSpecificConfiguration localConfig;
    private InetSocketAddress bindAddress;
    private InetSocketAddress publicAddress;
    private static final Logger logger = Logger.getLogger(MemberAddressPicker.class.getName());
    
    MemberAddressPicker(ServerEnvironment env, HazelcastRuntimeConfiguration config, HazelcastConfigSpecificConfiguration localConfig) {
        this.env = env;
        this.config = config;
        this.localConfig = localConfig;
        
        // determine public address and bind address
        findAppropriateInterfaces();
    }

    @Override
    public InetSocketAddress getBindAddress() {
        return bindAddress;
    }

    @Override
    public InetSocketAddress getPublicAddress() {
        if (publicAddress != null) {
            return publicAddress;
        } else {
            return bindAddress;
        }
    }
    
    /**
     * This method picks an interface using the following rules
     * If there is only one interface that is not loopback choose that
     * If there is an interfaces element use that
     * For the DAS if there is a bind address specified use that
     * If none of those choose the one that isn't the default docker one
     * For a standalone if the DAS specifies a Bind or Public address choose the interface on the same net or subnet
     * If none of those choose the one that is not the default docker interface
     * For micro if domain discovery mode choose the network on the same subnet
     * If tcpip mode choose the interface which matches a subnet in the tcpip list
     * If none of those choose the first interface that is not the default docker one
     */
    private void findAppropriateInterfaces() {
        
        //
        logger.fine("Finding an appropriate address for Hazelcast to use");
        int port = 0;
        if (env.isDas() && !env.isMicro()) {
            port = new Integer(config.getDasPort());
            if (config.getDASPublicAddress() != null && !config.getDASPublicAddress().isEmpty()) {
                publicAddress = new InetSocketAddress(config.getDASPublicAddress(), port);
            }
            
            if (config.getDASBindAddress() != null && !config.getDASBindAddress().isEmpty()) {
                bindAddress = new InetSocketAddress(config.getDASBindAddress(), port);
                logger.log(Level.FINE, "Bind address is specified in the configuration so we will use that {0}", bindAddress);
                return;
            }
        }
   
        //add to list filtering out docker0
        HashSet<NetworkInterface> possibleInterfaces = new HashSet<>();
        try {
            logger.fine("No address in configuration so let's find one");
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface intf = interfaces.nextElement();
                logger.log(Level.FINE, "Found Network Interface {0}", new Object[]{intf.getName()});
                
                if (intf.isUp() && !intf.isLoopback() && !intf.isVirtual() && !intf.getName().contains("docker0") &&!intf.getDisplayName().contains("Teredo") && intf.getInterfaceAddresses().size()>0) {
                    logger.log(Level.FINE, "Adding interface {0} as a possible interface", intf.getName());
                    possibleInterfaces.add(intf);
                } else {
                    logger.fine("Ignoring down, docker or loopback interface " + intf.getName());
                }
            }
        } catch (SocketException socketException) {
            logger.log(Level.WARNING,"There was a problem determining the network interfaces on this machine", socketException);
        }
        
        if (possibleInterfaces.size() >= 1) {
            // this is our interface
            // get first address on the interface
            NetworkInterface intf = possibleInterfaces.iterator().next();
            Enumeration<InetAddress> addresses = intf.getInetAddresses();
            InetAddress chosenAddress = null;
            while (addresses.hasMoreElements()) {
                chosenAddress = addresses.nextElement();
                if (chosenAddress instanceof Inet4Address) {
                    // prefer Inet4Address
                    break;
                }   
            }
            logger.log(Level.FINE, "Picked address {0}", chosenAddress);
            bindAddress = new InetSocketAddress(chosenAddress,port);
        }
        
        if (bindAddress == null) {
            try {
                // ok do the easy thing
                logger.log(Level.FINE,"Could not find an appropriate address by searching falling back to local host");
                bindAddress = new InetSocketAddress(InetAddress.getLocalHost(),port);
            } catch (UnknownHostException ex) {
                logger.log(Level.FINE,"Could not find local host, falling back to loop back address");
                bindAddress = new InetSocketAddress(InetAddress.getLoopbackAddress(),port);
            }
        }
    }
    
}
