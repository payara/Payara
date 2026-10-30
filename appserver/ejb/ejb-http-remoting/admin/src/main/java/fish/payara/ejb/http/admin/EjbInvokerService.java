/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2019-2021] Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.ejb.http.admin;

import com.sun.enterprise.deployment.Application;
import com.sun.enterprise.deployment.WebBundleDescriptor;
import static com.sun.enterprise.deployment.runtime.web.SunWebApp.HTTPSERVLET_SECURITY_PROVIDER;
import com.sun.enterprise.deployment.web.LoginConfiguration;
import com.sun.enterprise.util.StringUtils;
import static fish.payara.ejb.http.admin.Constants.EJB_INVOKER_APP;
import java.beans.PropertyChangeEvent;
import java.util.ArrayList;
import java.util.List;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import static jakarta.servlet.http.HttpServletRequest.FORM_AUTH;
import org.glassfish.api.StartupRunLevel;
import org.glassfish.api.deployment.DeploymentContext;
import org.glassfish.api.event.EventListener;
import org.glassfish.api.event.Events;
import org.glassfish.hk2.runlevel.RunLevel;
import org.glassfish.internal.api.Globals;
import org.glassfish.internal.deployment.Deployment;
import org.glassfish.web.deployment.runtime.SunWebAppImpl;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.ConfigListener;
import org.jvnet.hk2.config.UnprocessedChangeEvent;
import org.jvnet.hk2.config.UnprocessedChangeEvents;
/**
 *
 * @author Gaurav Gupta
 */
@Service(name = "ejb-invoker-service")
@RunLevel(StartupRunLevel.VAL)
public class EjbInvokerService implements EventListener, ConfigListener {
    
    @Inject
    private Events events;
    
    @Inject
    private EjbInvokerConfiguration config;

    @PostConstruct
    public void init() {
        if (events == null) {
            events = Globals.getDefaultBaseServiceLocator().getService(Events.class);
        }
        events.register(this);
    }

    @Override
    public void event(EventListener.Event event) {
        if (event.is(Deployment.APPLICATION_PREPARED)) {
            DeploymentContext context = (DeploymentContext) event.hook();
            Application app = context.getModuleMetaData(Application.class);
            if (app != null
                    && EJB_INVOKER_APP.equals(app.getAppName())
                    && Boolean.parseBoolean(config.getSecurityEnabled())) {
                for (WebBundleDescriptor descriptor : app.getBundleDescriptors(WebBundleDescriptor.class)) {
                    SunWebAppImpl webApp = (SunWebAppImpl) descriptor.getSunDescriptor();
                    String moduleName;
                    if (StringUtils.ok(config.getAuthModuleClass())
                            && config.getAuthModuleClass().indexOf('.') != -1) {
                        moduleName = config.getAuthModuleClass().substring(config.getAuthModuleClass().lastIndexOf('.') + 1);
                        webApp.setAttributeValue(HTTPSERVLET_SECURITY_PROVIDER, moduleName);
                    } else if (StringUtils.ok(config.getAuthModule())) {
                        moduleName = config.getAuthModule();
                        webApp.setAttributeValue(HTTPSERVLET_SECURITY_PROVIDER, moduleName);
                    }
                    LoginConfiguration loginConf = descriptor.getLoginConfiguration();
                    String authType = config.getAuthType();
                    String realmName = config.getRealmName();
                    if (StringUtils.ok(authType)) {
                        loginConf.setAuthenticationMethod(authType);
                    }
                    if (StringUtils.ok(realmName)) {
                        loginConf.setRealmName(realmName);
                    }
                    if (FORM_AUTH.equals(config.getAuthType())) {
                        loginConf.setFormErrorPage("/error.xhtml");
                        loginConf.setFormLoginPage("/login.xhtml");
                    }
                }
            }
        }
    }

    @Override
    public UnprocessedChangeEvents changed(PropertyChangeEvent[] events) {
        List<UnprocessedChangeEvent> unchangedList = new ArrayList<>();
        if(!Boolean.parseBoolean(config.getEnabled())) {
            return null;
        }
        for (PropertyChangeEvent event : events) {
            if ("enabled".equals(event.getPropertyName())){
                unchangedList.clear();
                break;
            }
            unchangedList.add(new UnprocessedChangeEvent(event, 
                    "EJB Invoker configuration changed: " + event.getPropertyName()
                    + " was changed from " + event.getOldValue() + " to " + event.getNewValue()));
        }
        return (unchangedList.size() > 0)
                ? new UnprocessedChangeEvents(unchangedList) : null;
    }
}
