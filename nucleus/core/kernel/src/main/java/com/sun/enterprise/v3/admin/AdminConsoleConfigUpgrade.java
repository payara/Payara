/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2013 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
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

package com.sun.enterprise.v3.admin;

import com.sun.enterprise.config.serverbeans.*;
import com.sun.enterprise.module.bootstrap.EarlyLogHandler;
import org.glassfish.grizzly.config.dom.NetworkListener;
import org.glassfish.api.admin.config.ConfigurationUpgrade;
import javax.inject.Inject;
import javax.inject.Named;

import org.jvnet.hk2.annotations.Optional;
import org.jvnet.hk2.annotations.Service;
import org.glassfish.hk2.api.PostConstruct;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;
import org.jvnet.hk2.config.types.Property;

import java.beans.PropertyVetoException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 * Adds the needed message-security-config information to domain.xml
 * during an upgrade from a v2.X server. For more information see:
 * https://glassfish.dev.java.net/issues/show_bug.cgi?id=13443
 */
@Service
public class AdminConsoleConfigUpgrade implements ConfigurationUpgrade, PostConstruct {

    private static final String AUTH_LAYER = "HttpServlet";
    private static final String PROVIDER_TYPE = "server";
    private static final String PROVIDER_ID = "GFConsoleAuthModule";
    private static final String CLASS_NAME =
        "org.glassfish.admingui.common.security.AdminConsoleAuthModule";
    private static final String AUTH_SOURCE = "sender";
    private static final String AUTH_URL_VAL_TEMPLATE =
        "http://localhost:%s/management/sessions";
    public static final String DEFAULT_ADMIN_PORT = "4848";
    private static final String LOGIN_PAGE_PROP = "loginPage";
    private static final String LOGIN_PAGE_VAL = "/login.jsf";
    private static final String LOGIN_ERR_PAGE_PROP = "loginErrorPage";
    private static final String LOGIN_ERR_PAGE_VAL = "/loginError.jsf";

    @Inject
    Configs configs;

    // This will force the Grizzly upgrade code to run before
    // AdminConsoleConfigUpgrade runs. Issue GLASSFISH-15599
    @Inject
    @Named("grizzlyconfigupgrade")
    @Optional
    ConfigurationUpgrade precondition = null;

    @Override
    public void postConstruct() {
        Config config = configs.getConfigByName("server-config");
        if (config != null) {
            SecurityService s = config.getSecurityService();
            if (s != null) {
                try {
                    ConfigSupport.apply(new AdminConsoleConfigCode(), s);
                } catch (TransactionFailure tf) {
                    LogRecord lr = new LogRecord(Level.SEVERE,
                        "Could not upgrade security service for admin console: " +
                        tf);
                    lr.setLoggerName(getClass().getName());
                    EarlyLogHandler.earlyMessages.add(lr);
                }
            }
        }
    }

    static private class AdminConsoleConfigCode
        implements SingleConfigCode<SecurityService> {

        @Override
        public Object run(SecurityService service)
            throws PropertyVetoException, TransactionFailure {

            /*
             * TODO: if the element is already present, should we check it
             * instead of just returning? If so, don't forget to enroll
             * it in the transaction.
             */
            for (MessageSecurityConfig msc :
                service.getMessageSecurityConfig()) {
                if (AUTH_LAYER.equals(msc.getAuthLayer())) {
                    return null;
                }
            }

            // create/add message-security-config
            MessageSecurityConfig msConfig =
                service.createChild(MessageSecurityConfig.class);
            msConfig.setAuthLayer(AUTH_LAYER);
            service.getMessageSecurityConfig().add(msConfig);

            // create/add provider-config
            ProviderConfig pConfig = msConfig.createChild(ProviderConfig.class);
            pConfig.setProviderType(PROVIDER_TYPE);
            pConfig.setProviderId(PROVIDER_ID);
            pConfig.setClassName(CLASS_NAME);
            msConfig.getProviderConfig().add(pConfig);

            // create/add request-policy
            RequestPolicy reqPol = pConfig.createChild(RequestPolicy.class);
            reqPol.setAuthSource(AUTH_SOURCE);
            pConfig.setRequestPolicy(reqPol);

            // create/add response-policy
            ResponsePolicy resPol = pConfig.createChild(ResponsePolicy.class);
            pConfig.setResponsePolicy(resPol);

            // get admin port property from config
            Config parent = service.getParent(Config.class);
            if (parent.getAdminListener() == null) {
                LogRecord lr = new LogRecord(Level.WARNING, 
                        String.format(
                            "Couldn't get admin port from config '%s'. Using default %s",
                            parent.getName(),
                            DEFAULT_ADMIN_PORT)
                );
                lr.setLoggerName(getClass().getName());
                EarlyLogHandler.earlyMessages.add(lr);                
            }

            // add properties
            Property logPageProp = pConfig.createChild(Property.class);
            logPageProp.setName(LOGIN_PAGE_PROP);
            logPageProp.setValue(LOGIN_PAGE_VAL);

            Property logErrPage = pConfig.createChild(Property.class);
            logErrPage.setName(LOGIN_ERR_PAGE_PROP);
            logErrPage.setValue(LOGIN_ERR_PAGE_VAL);

            List<Property> props = pConfig.getProperty();
            props.add(logPageProp);
            props.add(logErrPage);

            return null;
        }
    }

}
