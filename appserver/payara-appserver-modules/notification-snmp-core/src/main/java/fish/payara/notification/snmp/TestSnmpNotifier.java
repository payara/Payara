/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *    Copyright (c) [2017] Payara Foundation and/or its affiliates. All rights reserved.
 * 
 *     The contents of this file are subject to the terms of either the GNU
 *     General Public License Version 2 only ("GPL") or the Common Development
 *     and Distribution License("CDDL") (collectively, the "License").  You
 *     may not use this file except in compliance with the License.  You can
 *     obtain a copy of the License at
 *     https://github.com/payara/Payara/blob/master/LICENSE.txt
 *     See the License for the specific
 *     language governing permissions and limitations under the License.
 * 
 *     When distributing the software, include this License Header Notice in each
 *     file and include the License file at glassfish/legal/LICENSE.txt.
 * 
 *     GPL Classpath Exception:
 *     The Payara Foundation designates this particular file as subject to the "Classpath"
 *     exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *     file that accompanied this code.
 * 
 *     Modifications:
 *     If applicable, add the following below the License Header, with the fields
 *     enclosed by brackets [] replaced by your own identifying information:
 *     "Portions Copyright [year] [name of copyright owner]"
 * 
 *     Contributor(s):
 *     If you wish your version of this file to be governed by only the CDDL or
 *     only the GPL Version 2, indicate your decision by adding "[Contributor]
 *     elects to include this software in this distribution under the [CDDL or GPL
 *     Version 2] license."  If you don't indicate a single choice of license, a
 *     recipient has the option to distribute your version of this file under
 *     either the CDDL, the GPL Version 2 or to extend the choice of license to
 *     its licensees as provided above.  However, if you add GPL Version 2 code
 *     and therefore, elected the GPL Version 2 license, then the option applies
 *     only if the new code is made subject to such option by the copyright
 *     holder.
 */
package fish.payara.notification.snmp;

import com.sun.enterprise.config.serverbeans.Config;
import fish.payara.notification.snmp.exception.InvalidSnmpVersion;
import fish.payara.nucleus.notification.BlockingQueueHandler;
import fish.payara.nucleus.notification.TestNotifier;
import fish.payara.nucleus.notification.configuration.NotificationServiceConfiguration;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import javax.inject.Inject;
import org.glassfish.api.ActionReport;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.CommandLock;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.RestEndpoint;
import org.glassfish.api.admin.RestEndpoints;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;
import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.annotations.Service;
import org.snmp4j.CommunityTarget;
import org.snmp4j.Snmp;
import org.snmp4j.TransportMapping;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.transport.DefaultUdpTransportMapping;

/**
 *
 * @author jonathan coustick
 */
@Service(name = "test-snmp-notifier-configuration")
@PerLookup
@CommandLock(CommandLock.LockType.NONE)
@ExecuteOn({RuntimeType.DAS, RuntimeType.INSTANCE})
@TargetType(value = {CommandTarget.DAS, CommandTarget.STANDALONE_INSTANCE, CommandTarget.CLUSTER, CommandTarget.CLUSTERED_INSTANCE, CommandTarget.CONFIG})
@RestEndpoints({
        @RestEndpoint(configBean = NotificationServiceConfiguration.class,
                opType = RestEndpoint.OpType.GET,
                path = "test-snmp-notifier-configuration",
                description = "Tests SNMP Notifier Configuration")
})
public class TestSnmpNotifier extends TestNotifier {
    private static final String MESSAGE = "SNMP notifier test";
    
    @Param(name = "community", defaultValue = "public", optional = true)
    private String community;

    @Param(name = "oid", defaultValue = ".1.3.6.1.2.1.1.8", optional = true)
    private String oid;

    @Param(name = "version", defaultValue = "v2c", optional = true, acceptableValues = "v1,v2c")
    private String version;

    @Param(name = "hostName", optional = true)
    private String hostName;

    @Param(name = "port", defaultValue = "162", optional = true)
    private Integer port;

    @Inject
    SnmpNotificationEventFactory factory;
    
    @Override
    public void execute(AdminCommandContext context) {
        
        ActionReport actionReport = context.getActionReport();
        
        Config config = targetUtil.getConfig(target);
        if (config == null) {
            context.getActionReport().setMessage("No such config named: " + target);
            context.getActionReport().setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;
        }
        
        SnmpNotifierConfiguration snmpConfig = config.getExtensionByType(SnmpNotifierConfiguration.class);
        
        if (community == null){
                community = snmpConfig.getCommunity();
        }
        if (oid == null){
            oid = snmpConfig.getOid();
        }
        if (version == null){
            version = snmpConfig.getVersion();
        }
        if (hostName == null){
            hostName = snmpConfig.getHost();
        }
        if (port == null){
            port = snmpConfig.hashCode();
        }
        //prepare SNMP message
        SnmpNotificationEvent event = factory.buildNotificationEvent(SUBJECT, MESSAGE);
        
        SnmpMessageQueue queue = new SnmpMessageQueue();
        queue.addMessage(new SnmpMessage(event, event.getSubject(), event.getMessage()));
        SnmpNotifierConfigurationExecutionOptions options = new SnmpNotifierConfigurationExecutionOptions();
        options.setCommunity(community);
        options.setOid(oid);
        options.setVersion(version);
        options.setHost(hostName);
        options.setPort(port);
        SnmpNotificationRunnable notifierRun = null;
        try {
                TransportMapping transport = new DefaultUdpTransportMapping();
                Snmp snmp = new Snmp(transport);
                CommunityTarget cTarget = new CommunityTarget();

                cTarget.setCommunity(new OctetString(options.getCommunity()));
                int snmpVersion = SnmpNotifierService.decideOnSnmpVersion(options.getVersion());
                cTarget.setVersion(snmpVersion);
                cTarget.setAddress(new UdpAddress(options.getHost() + SnmpNotifierService.ADDRESS_SEPARATOR + options.getPort()));
                
                notifierRun = new SnmpNotificationRunnable(queue, options, snmp, cTarget, snmpVersion);
        }
        catch (IOException e) {
                Logger.getLogger(TestSnmpNotifier.class.getCanonicalName()).log(Level.SEVERE, "Error occurred while creating UDP transport", e);
                actionReport.setMessage("Error occurred while creating UDP transport");
                actionReport.setActionExitCode(ActionReport.ExitCode.FAILURE);
            }
        catch (InvalidSnmpVersion invalidSnmpVersion) {
                Logger.getLogger(TestSnmpNotifier.class.getCanonicalName()).log(Level.SEVERE, "Error occurred while configuring SNMP version: " + invalidSnmpVersion.getMessage());
                actionReport.setMessage("Error occurred while configuring SNMP version: " + invalidSnmpVersion.getMessage());
                actionReport.setActionExitCode(ActionReport.ExitCode.FAILURE);
        }
                
                
        
        //set up logger to store result
        Logger logger = Logger.getLogger(SnmpNotificationRunnable.class.getCanonicalName());
        BlockingQueueHandler bqh = new BlockingQueueHandler(10);
        bqh.setLevel(Level.FINE);
        Level oldLevel = logger.getLevel();
        logger.setLevel(Level.FINE);
        logger.addHandler(bqh);
        //send message, this occurs in its own thread
        Thread notifierThread = new Thread(notifierRun, "test-snmp-notifier-thread");
        notifierThread.start();
        try {
            notifierThread.join();
        } catch (InterruptedException ex) {
            Logger.getLogger(TestSnmpNotifier.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            logger.setLevel(oldLevel);
        }
        LogRecord message = bqh.poll();
        bqh.clear();
        if (message == null){
            //something's gone wrong
            Logger.getLogger(TestSnmpNotifier.class.getName()).log(Level.SEVERE, "Failed to send SNMP message");
            actionReport.setMessage("Failed to send SNMP message");
            actionReport.setActionExitCode(ActionReport.ExitCode.FAILURE);
        } else {
            actionReport.setMessage(message.getMessage());
            if (message.getLevel()==Level.FINE){
                actionReport.setActionExitCode(ActionReport.ExitCode.SUCCESS);               
            } else {
                actionReport.setActionExitCode(ActionReport.ExitCode.FAILURE);
            }
            
            
        }
        
    }
}
