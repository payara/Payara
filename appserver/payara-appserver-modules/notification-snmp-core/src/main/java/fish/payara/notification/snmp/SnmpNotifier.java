/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2016-2020] Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.notification.snmp;

import java.io.IOException;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.glassfish.api.StartupRunLevel;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;
import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.TransportMapping;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.IpAddress;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.transport.DefaultUdpTransportMapping;
import org.snmp4j.util.DefaultPDUFactory;

import fish.payara.internal.notification.PayaraConfiguredNotifier;
import fish.payara.internal.notification.PayaraNotification;
import fish.payara.notification.snmp.exception.InvalidSnmpVersion;

/**
 * @author mertcaliskan
 */
@Service(name = "snmp-notifier")
@RunLevel(StartupRunLevel.VAL)
public class SnmpNotifier extends PayaraConfiguredNotifier<SnmpNotifierConfiguration> {

    private static Logger LOGGER = Logger.getLogger(SnmpNotifier.class.getCanonicalName());

    private static final String ADDRESS_SEPARATOR = "/";
    private static final String version1  = "v1";
    private static final String version2c = "v2c";

    private Snmp snmp;
    private CommunityTarget target;

    @Override
    public void handleNotification(PayaraNotification event) {
        if (snmp == null || target == null) {
            LOGGER.fine("SNMP notifier received notification, but no target was available.");
            return;
        }

        final String message = event.getMessage();
        final String subject = String.format("%s. (host: %s, server: %s, domain: %s, instance: %s)", 
                event.getSubject(),
                event.getHostName(),
                event.getServerName(),
                event.getDomainName(),
                event.getInstanceName());
        final OctetString data = new OctetString(message + "\n" + subject);

        try {
            PDU pdu = DefaultPDUFactory.createPDU(target.getVersion());
            if (SnmpConstants.version1 == target.getVersion()) {
                pdu.setType(PDU.V1TRAP);
            } else {
                pdu.setType(PDU.TRAP);
            }
            pdu.add(new VariableBinding(SnmpConstants.sysUpTime));
            OID oidInstance = new OID(configuration.getOid());

            pdu.add(new VariableBinding(SnmpConstants.snmpTrapOID, oidInstance));
            pdu.add(new VariableBinding(SnmpConstants.snmpTrapAddress, new IpAddress(configuration.getHost())));
            pdu.add(new VariableBinding(SnmpConstants.sysUpTime, new OctetString(new Date().toString())));
            pdu.add(new VariableBinding(oidInstance, data));

            snmp.send(pdu, target);
            LOGGER.log(Level.FINE, "Message sent successfully");
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "IO Error while sending SNMP message", e);
        }
    }

    @Override
    public void bootstrap() {
        try {
            TransportMapping<?> transport = new DefaultUdpTransportMapping();
            this.snmp = new Snmp(transport);
            this.target = new CommunityTarget();

            target.setCommunity(new OctetString(configuration.getCommunity()));
            target.setVersion(decideOnSnmpVersion(configuration.getVersion()));
            target.setAddress(new UdpAddress(configuration.getHost() + ADDRESS_SEPARATOR + configuration.getPort()));
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error occurred while creating UDP transport", e);
        } catch (InvalidSnmpVersion invalidSnmpVersion) {
            LOGGER.log(Level.SEVERE,
                    "Error occurred while configuring SNMP version: " + invalidSnmpVersion.getMessage());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error occurred while configuring SNMP notifier", e);
        }
    }

    @Override
    public void destroy() {
        if (snmp != null) {
            try {
                snmp.close();
                snmp = null;
                target = null;
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Error occurred while closing SNMP connection", e);
            }
        }
    }

    static int decideOnSnmpVersion(String version) throws InvalidSnmpVersion {
        switch (version) {
            case version1:
                return SnmpConstants.version1;
            case version2c:
                return SnmpConstants.version2c;
            default:
                throw new InvalidSnmpVersion(version);
        }
    }
}