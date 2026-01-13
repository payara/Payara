/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *    Copyright (c) [2026] Payara Foundation and/or its affiliates. All rights reserved.
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
 *     file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
package fish.payara.tools.dev.core;

import fish.payara.tools.dev.admin.DevConsoleApplication;
import fish.payara.tools.dev.admin.DevConsoleServiceConfiguration;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import java.beans.PropertyChangeEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.glassfish.api.StartupRunLevel;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.ConfigListener;
import org.jvnet.hk2.config.UnprocessedChangeEvent;
import org.jvnet.hk2.config.UnprocessedChangeEvents;

/**
 *
 * @author Gaurav Gupta
 */
@Service(name = "dev-console-service")
@RunLevel(StartupRunLevel.VAL)
public class DevConsoleService implements ConfigListener {

    @Inject
    private ServerEnvironment serverEnv;

    @Inject
    private ServiceLocator serviceLocator;

    private Boolean devConsoleEnabled;

    private DevConsoleServiceConfiguration devConsoleServiceConfiguration;

    private final ConcurrentHashMap<String, DevConsoleApplication> apps 
            = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<String, DevConsoleRegistry> registries
            = new ConcurrentHashMap<>();

    public void register(DevConsoleApplication app, DevConsoleRegistry registry) {
        apps.put(app.id(), app);
        registries.put(app.id(), registry);
    }

    public void unregister(String appId) {
        apps.remove(appId);
        registries.remove(appId);
    }

    public Collection<DevConsoleApplication> getApplications() {
        return apps.values();
    }

    public DevConsoleRegistry getRegistry(String appId) {
        return registries.get(appId);
    }

    public Map<String, DevConsoleRegistry> getAllRegistries() {
        return Map.copyOf(registries);
    }

    @PostConstruct
    public void init() {
        devConsoleServiceConfiguration = serviceLocator.getService(DevConsoleServiceConfiguration.class);
    }

    public boolean isEnabled() {
        if (devConsoleEnabled == null) {
            devConsoleEnabled = Boolean.valueOf(devConsoleServiceConfiguration.getEnabled());
        }
        return devConsoleEnabled;
    }

    public void resetDevConsoleEnabledProperty() {
        devConsoleEnabled = null;
    }

    @Override
    public UnprocessedChangeEvents changed(PropertyChangeEvent[] events) {
        List<UnprocessedChangeEvent> unchangedList = new ArrayList<>();
        for (PropertyChangeEvent event : events) {
            unchangedList.add(new UnprocessedChangeEvent(event, "Dev Console configuration changed:" + event.getPropertyName()
                    + " was changed from " + event.getOldValue().toString() + " to " + event.getNewValue().toString()));
        }
        return new UnprocessedChangeEvents(unchangedList);
    }
}
