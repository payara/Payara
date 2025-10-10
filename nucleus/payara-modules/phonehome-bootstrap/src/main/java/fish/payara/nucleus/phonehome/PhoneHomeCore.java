/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *    Copyright (c) 2016-2025 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.nucleus.phonehome;

import com.sun.enterprise.config.serverbeans.Domain;
import fish.payara.nucleus.executorservice.PayaraExecutorService;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.glassfish.api.StartupRunLevel;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.api.event.EventListener;
import org.glassfish.api.event.EventTypes;
import org.glassfish.api.event.Events;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;

import java.util.UUID;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author David Weaver
 */
@Service(name = "phonehome-core")
@RunLevel(StartupRunLevel.VAL)
public class PhoneHomeCore implements EventListener {

    private boolean enabled;

    private UUID phoneHomeId;

    @Inject
    private PayaraExecutorService executor;

    @Inject
    @Named(ServerEnvironment.DEFAULT_INSTANCE_NAME)
    PhoneHomeRuntimeConfiguration configuration;

    @Inject
    private ServerEnvironment env;

    @Inject
    private Events events;

    @Inject
    private Domain domain;

    private ScheduledFuture<?> scheduledFuture;

    @PostConstruct
    public void postConstruct() {
        events.register(this);

        if (env.isDas()) {

            if (configuration == null) {
                enabled = true;
                phoneHomeId = UUID.randomUUID();
            } else {
                enabled = Boolean.valueOf(configuration.getEnabled());

                // Get the UUID from the config if one is present, otherwise use a randomly generated one
                try {
                    phoneHomeId = UUID.fromString(configuration.getPhoneHomeId());
                } catch (NullPointerException ex) {
                    phoneHomeId = UUID.randomUUID();
                    //PAYARA-2249 don't bother updating domain.xml in micro as likely a waste of time
                    if (!env.isMicro()) {
                        try {
                            ConfigSupport.apply((SingleConfigCode<PhoneHomeRuntimeConfiguration>) configurationProxy -> {
                                configurationProxy.setPhoneHomeId(phoneHomeId.toString());
                                return configurationProxy;
                            }, configuration);
                        } catch (TransactionFailure e) {
                            // Ignore and just don't write the ID to the config file
                        }
                    }
                }
            }
        } else {
            enabled = false;
        }
    }

    /**
     *
     * @param event
     */
    @Override
    public void event(Event event) {
        if (event.is(EventTypes.SERVER_READY)) {
            bootstrapPhoneHome();
        }
    }

    private void bootstrapPhoneHome() {
        if (enabled) {
            scheduledFuture = executor.scheduleAtFixedRate(new PhoneHomeTask(phoneHomeId.toString(), domain, env), 0, 1, TimeUnit.DAYS);
        }
    }

    private void shutdownPhoneHome() {
        if (scheduledFuture != null) {
            scheduledFuture.cancel(true);
        }
    }

    public void start() {
        if (this.enabled) {
            shutdownPhoneHome();
            bootstrapPhoneHome();
        } else {
            this.enabled = true;
            bootstrapPhoneHome();
        }
    }

    public void stop() {
        if (this.enabled) {
            this.enabled = false;
            shutdownPhoneHome();
        }
    }

}
