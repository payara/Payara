/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2017-2026 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.nucleus.notification.admin;

import java.util.List;

import fish.payara.internal.notification.EventLevel;
import jakarta.inject.Inject;

import org.glassfish.api.ActionReport;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.CommandLock;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.RestEndpoint;
import org.glassfish.api.admin.RestEndpoints;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.hk2.api.messaging.Topic;
import org.jvnet.hk2.annotations.Service;

import fish.payara.internal.notification.PayaraNotification;
import fish.payara.internal.notification.PayaraNotificationBuilder;
import fish.payara.internal.notification.PayaraNotificationFactory;
import fish.payara.internal.notification.admin.NotificationServiceConfiguration;

/**
 * Tests that the log notifier works
 * @author jonathan coustick
 * @since 4.1.2.173
 */
@Service(name = "test-notifier-configuration")
@PerLookup
@CommandLock(CommandLock.LockType.NONE)
@ExecuteOn({RuntimeType.DAS, RuntimeType.INSTANCE})
@TargetType(value = {CommandTarget.DAS, CommandTarget.STANDALONE_INSTANCE, CommandTarget.CONFIG})
@RestEndpoints({
        @RestEndpoint(configBean = NotificationServiceConfiguration.class,
                opType = RestEndpoint.OpType.GET,
                path = "test-notifier-configuration",
                description = "Tests Notifier Configuration")
})
public class TestNotifier implements AdminCommand {
    
    private static final String EVENT_TYPE = "TEST";
    private static final String SUBJECT = "Test Notification";
    private static final String MESSAGE = "This is a test notification";
    
    @Param(name = "all", shortName = "a", optional = true)
    private Boolean all;

    @Param(name = "notifiers", shortName = "n", optional = true)
    private List<String> notifiers;

    @Inject
    private Topic<PayaraNotification> topic;

    @Inject
    private PayaraNotificationFactory factory;
    
    @Override
    public void execute(AdminCommandContext context) {
        
        final ActionReport report = context.getActionReport();

        PayaraNotificationBuilder builder = factory.newBuilder()
                .eventType(EVENT_TYPE)
                .subject(SUBJECT)
                .message(MESSAGE)
                .level(EventLevel.SEVERE);

        if (all == null || !all) {
            if (notifiers == null || notifiers.isEmpty()) {
                report.setMessage("Must either specify all notifiers or a select list");
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                return;
            }
            builder = builder.whitelist(notifiers.toArray(new String[0]));
        }

        final PayaraNotification event = builder.build();
        
        try {
            topic.publish(event);
            report.setMessage("SUCCESS");
            report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
        } catch (Exception ex) {
            report.setMessage(ex.getMessage());
            report.setFailureCause(ex);
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
        }
    }
}
