/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2017-2023 Payara Foundation and/or its affiliates. All rights reserved.
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
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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

import com.hazelcast.instance.EndpointQualifier;
import com.hazelcast.spi.MemberAddressProvider;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.logging.Level;
import org.glassfish.api.admin.ServerEnvironment;
import java.util.logging.Logger;
import org.glassfish.grizzly.utils.Holder.LazyHolder;

/**
 * This class tries to work out which interface to choose as the address of the member
 * broadcast within Hazelcast
 *
 * This method picks an interface using the following rules:
 * If there is only one interface that is not loopback, choose that
 * If there is an interfaces element use that
 * For the DAS, if there is a bind address specified use that
 * If none of those choose the one that isn't the default docker one
 * For a standalone if the DAS specifies a Bind or Public address,
 * choose the interface on the same net or subnet
 * If none of those choose the one that is not the default docker interface
 * For micro if domain discovery mode, choose the network on the same subnet
 * If tcpip mode choose the interface which matches a subnet in the tcpip list
 * If none of those choose the first interface that is not the default docker one
 *
 * @author Steve Millidge (Payara Foundation)
 * @since 5.0.181
 */
public class MemberAddressPicker implements MemberAddressProvider {
    private static final Logger logger = Logger.getLogger(MemberAddressPicker.class.getName());
    private final ServerEnvironment env;
    private final HazelcastRuntimeConfiguration config;
    private final HazelcastConfigSpecificConfiguration localConfig;
    private final InetSocketAddress bindAddress;
    private final InetSocketAddress publicAddress;
    private final LazyHolder<InetAddress> chosenAddress = LazyHolder.lazyHolder(MemberAddressPicker::findMyAddress);

    MemberAddressPicker(ServerEnvironment env, HazelcastRuntimeConfiguration config, HazelcastConfigSpecificConfiguration localConfig) {
        this.env = env;
        this.config = config;
        this.localConfig = localConfig;

        // determine public address and bind address
        bindAddress = initBindAddress();
        publicAddress = initPublicAddress(bindAddress);
    }

    @Override
    public InetSocketAddress getBindAddress(EndpointQualifier endpointQualifier) {
        return getBindAddress();
    }

    @Override
    public InetSocketAddress getPublicAddress(EndpointQualifier endpointQualifier) {
        return getPublicAddress();
    }

    @Override
    public InetSocketAddress getBindAddress() {
        return bindAddress;
    }

    @Override
    public InetSocketAddress getPublicAddress() {
        return publicAddress;
    }

    private InetSocketAddress initBindAddress() {
        InetSocketAddress address = new InetSocketAddress(0);
        if (env.isDas() && !env.isMicro() && !config.getDASBindAddress().isEmpty()) {
            int port = Integer.valueOf(config.getDasPort());
            address = initAddress(config.getDASBindAddress(), port);
            logger.log(Level.FINE, "Bind address is specified in the configuration so we will use that {0}", address);
        } else if (config.getDiscoveryMode().startsWith("multicast")) {
            // in multicast mode, Hazelcast needs actual interface to bind, not wildcard
            address = ensureAddress(null, null, chosenAddress, 0);
        } else {
            logger.log(Level.FINE, "Using Wildcard bind address");
        }
        return address;
    }

    private InetSocketAddress initPublicAddress(InetSocketAddress bindAddress) {
        logger.fine("Finding an appropriate address for Hazelcast to use");
        InetSocketAddress address;
        int port = 0;
        if (env.isDas() && !env.isMicro()) {
            port = Integer.valueOf(config.getDasPort());
            // try setting public address from global Payara config
            address = initAddress(config.getDASPublicAddress(), port);
        } else {
            // try setting public address from configuration, Payara node config
            address = initAddress(localConfig.getPublicAddress(), Integer.parseInt(config.getStartPort()));
        }
        return ensureAddress(address, bindAddress, chosenAddress, port);
    }

    static InetSocketAddress initAddress(String address, int port) {
        if (address != null && !address.isEmpty()) {
            String addressParts[] = address.split(":");
            if (addressParts.length > 1) {
                return new InetSocketAddress(addressParts[0], Integer.parseInt(addressParts[1]));
            } else {
                return new InetSocketAddress(addressParts[0], port);
            }
        } else {
            return null;
        }
    }

    static InetAddress findMyAddressOrLocalHost() {
        InetAddress myAddress = findMyAddress();
        if (myAddress == null) {
            return tryLocalHostOrLoopback(0, true).getAddress();
        }
        return myAddress;
    }

    private static InetSocketAddress ensureAddress(InetSocketAddress targetAddress, InetSocketAddress sourceAddress,
            LazyHolder<InetAddress> backupAddress, int port) {
        if (targetAddress == null) {
            if (sourceAddress != null && !sourceAddress.getAddress().isAnyLocalAddress()) {
                targetAddress = sourceAddress;
            } else if (backupAddress.get() != null) {
                targetAddress = new InetSocketAddress(backupAddress.get(), port);
            } else {
                targetAddress = tryLocalHostOrLoopback(port, false);
            }
        }
        return targetAddress;
    }

    private static InetSocketAddress tryLocalHostOrLoopback(int port, boolean warn) {
        try {
            // ok do the easy thing
            logger.log(warn ? Level.WARNING : Level.FINE,
                    "Could not find an appropriate address, falling back to local host");
            return new InetSocketAddress(InetAddress.getLocalHost(), port);
        } catch (UnknownHostException ex) {
            logger.log(warn ? Level.WARNING : Level.FINE,
                    "Could not find local host, falling back to loop back address");
            return new InetSocketAddress(InetAddress.getLoopbackAddress(), port);
        }
    }

    private static InetAddress findMyAddress() {
        //add to list filtering out docker0
        HashSet<NetworkInterface> possibleInterfaces = new HashSet<>();
        try {
            logger.fine("No address in configuration so let's find one");
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface intf = interfaces.nextElement();
                logger.log(Level.FINE, "Found Network Interface {0}", new Object[]{intf.getName()});

                if (intf.isUp() && !intf.isLoopback() && !intf.isVirtual() && !intf.isPointToPoint() && !intf.getName().contains("docker0") &&
                        !intf.getDisplayName().contains("Teredo") && intf.getInterfaceAddresses().size() > 0) {
                    logger.log(Level.FINE, "Adding interface {0} as a possible interface", intf.getName());
                    possibleInterfaces.add(intf);
                } else {
                    logger.log(Level.FINE, "Ignoring down, docker, p2p or loopback interface {0}", intf.getName());
                }
            }
        } catch (SocketException socketException) {
            logger.log(Level.WARNING, "There was a problem determining the network interfaces on this machine", socketException);
        }

        InetAddress chosenAddress = null;
        if (!possibleInterfaces.isEmpty()) {
            // we haven't found an address
            // this is our interface
            // get first address on the interface
            NetworkInterface intf = possibleInterfaces.iterator().next();
            Enumeration<InetAddress> addresses = intf.getInetAddresses();
            while (addresses.hasMoreElements()) {
                chosenAddress = addresses.nextElement();
                if (chosenAddress instanceof Inet4Address) {
                    // prefer Inet4Address
                    break;
                }
            }
            logger.log(Level.FINE, "Picked address {0}", chosenAddress);
        }
        return chosenAddress;
    }
}
