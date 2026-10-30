/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2020-2024] Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.internal.notification;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * A notification received by notifiers and the notification service
 */
public class PayaraNotification implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String eventType;
    private final String serverName;
    private final String hostName;
    private final String domainName;
    private final String instanceName;
    private final String subject;
    private final String message;
    private final Serializable data;
    private final EventLevel level;

    private final List<String> notifierWhitelist;
    private final List<String> notifierBlacklist;

    public PayaraNotification(String eventType, String serverName, String hostName, String domainName,
                              String instanceName, String subject, String message, Serializable data, List<String> notifierWhitelist,
                              List<String> notifierBlacklist) {
        this(eventType, serverName, hostName, domainName, instanceName, subject, message, data, EventLevel.INFO, notifierWhitelist, notifierBlacklist);
    }

    public PayaraNotification (String eventType, String serverName, String hostName, String domainName,
                               String instanceName, String subject, String message, Serializable data, EventLevel level, List<String> notifierWhitelist,
            List<String> notifierBlacklist) {
        this.eventType = eventType;
        this.serverName = serverName;
        this.hostName = hostName;
        this.domainName = domainName;
        this.instanceName = instanceName;
        this.subject = subject;
        this.message = message;
        this.data = data;
        this.level = level;
        this.notifierWhitelist = (notifierWhitelist == null) ? null : Collections.unmodifiableList(notifierWhitelist);
        this.notifierBlacklist = (notifierBlacklist == null) ? null : Collections.unmodifiableList(notifierBlacklist);
    }

    public String getEventType() {
        return eventType;
    }

    public String getServerName() {
        return serverName;
    }

    public String getHostName() {
        return hostName;
    }

    public String getDomainName() {
        return domainName;
    }

    public String getInstanceName() {
        return instanceName;
    }

    public String getSubject() {
        return subject;
    }

    public String getMessage() {
        return message;
    }

    public EventLevel getLevel () {
        return level;
    }

    public List<String> getNotifierWhitelist() {
        return notifierWhitelist;
    }

    public List<String> getNotifierBlacklist() {
        return notifierBlacklist;
    }

    public Serializable getData() {
        return data;
    }

}