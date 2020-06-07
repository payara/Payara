/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2016-2019] Payara Foundation and/or its affiliates. All rights reserved.
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

import fish.payara.notification.snmp.exception.InvalidSnmpVersion;
import fish.payara.nucleus.notification.configuration.NotifierType;
import fish.payara.nucleus.notification.configuration.SnmpNotifier;
import fish.payara.nucleus.notification.domain.NotificationEvent;
import fish.payara.nucleus.notification.service.QueueBasedNotifierService;
import org.glassfish.api.StartupRunLevel;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;
import org.snmp4j.CommunityTarget;
import org.snmp4j.Snmp;
import org.snmp4j.TransportMapping;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.*;
import org.snmp4j.transport.DefaultUdpTransportMapping;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.glassfish.hk2.api.messaging.MessageReceiver;
import org.glassfish.hk2.api.messaging.SubscribeTo;

/**
 * @author mertcaliskan
 */
@Service(name = "service-snmp")
@RunLevel(StartupRunLevel.VAL)
@MessageReceiver
public class SnmpNotifierService extends QueueBasedNotifierService<SnmpNotificationEvent,
        SnmpNotifier,
        SnmpNotifierConfiguration,
        SnmpMessageQueue> {

    private static Logger logger = Logger.getLogger(SnmpNotifierService.class.getCanonicalName());

    static final String ADDRESS_SEPARATOR = "/";
    static final String version1  = "v1";
    static final String version2c = "v2c";
    private Snmp snmp;
    private SnmpNotifierConfigurationExecutionOptions execOptions;

    SnmpNotifierService() {
        super("snmp-message-consumer-");
    }

    @Override
    public void handleNotification(@SubscribeTo NotificationEvent event) {
        if (event instanceof SnmpNotificationEvent && execOptions != null && execOptions.isEnabled()) {
            SnmpMessage message = new SnmpMessage((SnmpNotificationEvent) event, event.getSubject(), event.getMessage());
            queue.addMessage(message);
        }
    }

    @Override
    public void bootstrap() {
        register(NotifierType.SNMP, SnmpNotifier.class, SnmpNotifierConfiguration.class);
        execOptions = (SnmpNotifierConfigurationExecutionOptions) getNotifierConfigurationExecutionOptions();

        if (execOptions != null && execOptions.isEnabled()) {
            try {
                TransportMapping transport = new DefaultUdpTransportMapping();
                snmp = new Snmp(transport);
                CommunityTarget cTarget = new CommunityTarget();

                cTarget.setCommunity(new OctetString(execOptions.getCommunity()));
                int snmpVersion = decideOnSnmpVersion(execOptions.getVersion());
                cTarget.setVersion(snmpVersion);
                cTarget.setAddress(new UdpAddress(execOptions.getHost() + ADDRESS_SEPARATOR + execOptions.getPort()));

                initializeExecutor();
                scheduledFuture = scheduleExecutor(new SnmpNotificationRunnable(queue, execOptions, snmp, cTarget, snmpVersion));
            }
            catch (IOException e) {
                logger.log(Level.SEVERE, "Error occurred while creating UDP transport", e);
            }
            catch (InvalidSnmpVersion invalidSnmpVersion) {
                logger.log(Level.SEVERE, "Error occurred while configuring SNMP version: " + invalidSnmpVersion.getMessage());
            }
        }
    }

    @Override
    public void shutdown() {
        super.shutdown();
        if (snmp != null) {
            try {
                snmp.close();
            }
            catch (IOException e) {
                logger.log(Level.SEVERE, "Error occurred while closing SNMP connection", e);
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