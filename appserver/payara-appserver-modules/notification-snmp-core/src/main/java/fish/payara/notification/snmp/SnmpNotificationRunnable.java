/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2016 Payara Foundation and/or its affiliates. All rights reserved.
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

import fish.payara.nucleus.notification.service.NotificationRunnable;
import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.IpAddress;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.util.DefaultPDUFactory;

import java.io.IOException;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author mertcaliskan
 */
public class SnmpNotificationRunnable extends NotificationRunnable<SnmpMessageQueue, SnmpNotifierConfigurationExecutionOptions> {

    private static Logger logger = Logger.getLogger(SnmpNotificationRunnable.class.getCanonicalName());

    private final Snmp snmp;
    private final CommunityTarget communityTarget;
    private final int snmpVersion;

    public SnmpNotificationRunnable(SnmpMessageQueue queue, SnmpNotifierConfigurationExecutionOptions executionOptions,
                                    Snmp snmp, CommunityTarget communityTarget, int snmpVersion) {
        this.queue = queue;
        this.executionOptions = executionOptions;
        this.snmp = snmp;
        this.communityTarget = communityTarget;
        this.snmpVersion = snmpVersion;
    }

    @Override
    public void run() {
        while (queue.size() > 0) {
            try {
                PDU pdu = DefaultPDUFactory.createPDU(snmpVersion);
                if (SnmpConstants.version1 == snmpVersion) {
                    pdu.setType(PDU.V1TRAP);
                } else {
                    pdu.setType(PDU.TRAP);
                }
                pdu.add(new VariableBinding(SnmpConstants.sysUpTime));
                OID oidInstance = new OID(executionOptions.getOid());

                pdu.add(new VariableBinding(SnmpConstants.snmpTrapOID, oidInstance));
                pdu.add(new VariableBinding(SnmpConstants.snmpTrapAddress, new IpAddress(executionOptions.getHost())));
                pdu.add(new VariableBinding(SnmpConstants.sysUpTime, new OctetString(new Date().toString())));
                SnmpMessage message = queue.getMessage();
                pdu.add(new VariableBinding(oidInstance, new OctetString(message.getSubject() + "\n" + message.getMessage())));

                snmp.send(pdu, communityTarget);
            }
            catch (IOException e) {
                logger.log(Level.SEVERE, "IO Error while sending SNMP message", e);
            }
        }
    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        logger.log(Level.SEVERE, "Error occurred consuming SNMP messages from queue", e);
    }
}