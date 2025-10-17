/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *  Copyright (c) [2019-2024] Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.audit;

import java.security.Principal;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import fish.payara.internal.notification.EventLevel;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import javax.security.auth.Subject;

import org.glassfish.api.StartupRunLevel;
import org.glassfish.api.admin.ParameterMap;
import org.glassfish.hk2.api.messaging.Topic;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;

import fish.payara.internal.notification.PayaraNotification;
import fish.payara.internal.notification.PayaraNotificationFactory;

/**
 * Audit Admin commands and sends them to the notification services.
 * 
 * Currently only for commands sent via the admin console.
 * @author jonathan coustick
 * @since 5.192
 */
@Service(name = "asadmin-audit")
@RunLevel(StartupRunLevel.VAL)
public class AdminAuditService {
    
    private static final String AUDIT_MESSAGE = "Admin Command Audit";
    private static final List<String> ACCESSOR_COMMAND_START = Arrays.asList("_", "get", "list", "help", "version");
    
    private boolean enabled;
    private AuditLevel auditLevel = AuditLevel.MODIFIERS;

    @Inject
    private Topic<PayaraNotification> notificationEventBus;

    @Inject
    private PayaraNotificationFactory notificationFactory;
    
    @Inject
    AdminAuditConfiguration configuration;

    private final Set<String> enabledNotifiers = new LinkedHashSet<>();
    
    @PostConstruct
    public void postConstruct() {
        enabled = Boolean.valueOf(configuration.getEnabled());
        auditLevel = AuditLevel.valueOf(configuration.getAuditLevel());
        if (enabled) {
            bootstrapNotifierList();
        }
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(Boolean enabled) {
        if (!this.enabled && enabled) {
            Logger.getLogger("fish.payara.audit").log(Level.INFO, "Admin Audit Service Started");
            bootstrapNotifierList();
        }
        this.enabled = enabled;
    }
    
    public AuditLevel getAuditLevel() {
        return auditLevel;
    }
    
    public void setAuditLevel(AuditLevel level){
        auditLevel = level;
    }

    public Set<String> getEnabledNotifiers() {
        return enabledNotifiers;
    }
    
    /**
     * Starts all notifiers that have been enable with the admin audit service.
     */
    public synchronized void bootstrapNotifierList() {
        enabledNotifiers.clear();
        if (configuration.getNotifierList() != null) {
            configuration.getNotifierList().forEach(enabledNotifiers::add);
        }
    }
    
    public void recordAsadminCommand(String command, ParameterMap parameters, Subject subject) {
        if (enabled && !enabledNotifiers.isEmpty() && checkAuditLevel(command)) {

            Set<Principal> principals = subject.getPrincipals();
            String name = principals.iterator().next().getName();

            PayaraNotification notification = notificationFactory.newBuilder()
                .whitelist(enabledNotifiers.toArray(new String[0]))
                .subject(AUDIT_MESSAGE)
                .message(name + " issued command " + command + " with parameters " + parameters.toString())
                .level(this.getEventLevel())
                .build();

            notificationEventBus.publish(notification);
        }
    }

    private EventLevel getEventLevel () {
        switch (this.auditLevel) {
            case INTERNAL:
                return EventLevel.INFO;
            case ACCESSORS:
                return EventLevel.WARNING;
            case MODIFIERS:
            default:
                return EventLevel.SEVERE;
        }
    }
    
    private boolean checkAuditLevel(String command) {        
        switch (auditLevel) {
            case INTERNAL:
                return true;
            case ACCESSORS:
                if (!command.startsWith("_")) {
                    return true;
                }
                break;
            default:
                for (String start: ACCESSOR_COMMAND_START) {
                    if (command.startsWith(start)) {
                        return false;
                    }
                }
                return true;
        }
        return false;
    }
    
}
