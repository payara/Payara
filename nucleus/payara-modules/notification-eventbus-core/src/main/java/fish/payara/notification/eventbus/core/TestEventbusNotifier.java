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
package fish.payara.notification.eventbus.core;

import com.sun.enterprise.config.serverbeans.Config;
import fish.payara.notification.eventbus.EventbusMessage;
import fish.payara.nucleus.notification.BlockingQueueHandler;
import fish.payara.nucleus.notification.TestNotifier;
import fish.payara.nucleus.notification.log.LogNotificationEvent;
import fish.payara.nucleus.notification.log.LogNotificationEventFactory;
import fish.payara.nucleus.notification.log.LogNotifierConfiguration;
import fish.payara.nucleus.notification.log.LogNotifierConfigurationExecutionOptions;
import fish.payara.nucleus.notification.log.LogNotifierService;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import javax.inject.Inject;
import org.glassfish.api.ActionReport;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.internal.api.Target;

/**
 *
 * @author jonathan coustick
 */
public class TestEventbusNotifier extends TestNotifier {
    
    private static final String MESSAGE = "Eventbus notifier test";
    
    @Param(name = "topicName", optional = false)
    private String topicName;
    
    @Param(name = "target", optional = true, defaultValue = "server")
    private String target;
    
    @Inject
    private Target targetUtil;

    @Inject
    EventbusNotificationEventFactory factory;
    
    @Override
    public void execute(AdminCommandContext context) {
        
        ActionReport actionReport = context.getActionReport();
        
        Config config = targetUtil.getConfig(target);
        if (config == null) {
            context.getActionReport().setMessage("No such config named: " + target);
            context.getActionReport().setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;
        }
        
        EventbusNotifierConfiguration eventbusConfig = config.getExtensionByType(EventbusNotifierConfiguration.class);
        
        if (topicName == null){
                topicName = eventbusConfig.getTopicName();
        }
        //prepare hipchat message
        EventbusNotificationEvent event = factory.buildNotificationEvent(SUBJECT, MESSAGE);
        
        EventbusMessageQueue queue = new EventbusMessageQueue();
        queue.addMessage(new EventbusMessageImpl(event, event.getSubject(), event.getMessage()));
        EventbusNotifierConfigurationExecutionOptions options = new EventbusNotifierConfigurationExecutionOptions();
        options.setTopicName(topicName);
        EventbusNotifierService service = new EventbusNotifierService();
        //set up logger to store result
        
        Logger logger = Logger.getLogger(EventbusNotifierService.class.getCanonicalName());
        BlockingQueueHandler bqh = new BlockingQueueHandler(10);
        bqh.setLevel(Level.FINE);
        service.handleNotification(event);
        Level oldLevel = logger.getLevel();
        logger.setLevel(Level.FINE);
        logger.addHandler(bqh);
        logger.setLevel(oldLevel);
        LogRecord message = bqh.poll();
        bqh.clear();
        if (message == null){
            //something's gone wrong
            Logger.getGlobal().log(Level.SEVERE, "Failed to send Eventbus message");
            actionReport.setMessage("Failed to send Eventbus message");
            actionReport.setActionExitCode(ActionReport.ExitCode.FAILURE);
        } else {;
            actionReport.setMessage(message.getMessage());
            if (message.getLevel()==Level.FINE){
                actionReport.setActionExitCode(ActionReport.ExitCode.SUCCESS);               
            } else {
                actionReport.setActionExitCode(ActionReport.ExitCode.FAILURE);
            }
            
            
        }
        
    }
    
}
