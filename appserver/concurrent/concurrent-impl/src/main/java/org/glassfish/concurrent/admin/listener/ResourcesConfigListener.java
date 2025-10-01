/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *    Copyright (c) 2025 Payara Foundation and/or its affiliates. All rights reserved.
 *
 *     The contents of this file are subject to the terms of either the GNU
 *     General Public License Version 2 only ("GPL") or the Common Development
 *     and Distribution License("CDDL") (collectively, the "License").  You
 *     may not use this file except in compliance with the License.  You can
 *     obtain a copy of the License at
 *     https://github.com/payara/Payara/blob/main/LICENSE.txt
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
package org.glassfish.concurrent.admin.listener;

import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.util.i18n.StringManager;
import fish.payara.internal.api.PostBootRunLevel;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import org.glassfish.concurrent.admin.ManagedExecutorServiceBaseManager;
import org.glassfish.concurrent.config.ManagedExecutorService;
import org.glassfish.concurrent.config.ManagedScheduledExecutorService;
import org.glassfish.concurrent.config.ManagedThreadFactory;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.ConfigListener;
import org.jvnet.hk2.config.Transactions;
import org.jvnet.hk2.config.UnprocessedChangeEvent;
import org.jvnet.hk2.config.UnprocessedChangeEvents;

import java.beans.PropertyChangeEvent;
import java.util.ArrayList;
import java.util.List;

@Service
@RunLevel(PostBootRunLevel.VAL)
public class ResourcesConfigListener implements ConfigListener {

    private static StringManager sm
            = StringManager.getManager(ManagedExecutorServiceBaseManager.class);

    @Inject
    private Transactions transactions;

    @Override
    public UnprocessedChangeEvents changed(PropertyChangeEvent[] events) {
        List<UnprocessedChangeEvent> unprocessedChangeEvents = new ArrayList<>(1);

        for (PropertyChangeEvent propertyChangeEvent : events) {
            Object source = propertyChangeEvent.getSource();

            if (!(source instanceof ManagedExecutorService
                    || source instanceof ManagedScheduledExecutorService)
                    || source instanceof ManagedThreadFactory) {
                continue;
            }

            if ("use-virtual-threads".equals(propertyChangeEvent.getPropertyName())
                    && (!propertyChangeEvent.getOldValue().equals(propertyChangeEvent.getNewValue()))) {
                unprocessedChangeEvents.add(new UnprocessedChangeEvent(propertyChangeEvent, sm.getString("virtual.threads.change.requires.restart")));
            }
        }
        return new UnprocessedChangeEvents(unprocessedChangeEvents);
    }

    @PostConstruct
    public void postConstruct() {
        transactions.addListenerForType(ResourcesConfigListener.class, this);
    }
}
