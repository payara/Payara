/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
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


package com.sun.jaspic.config.jaas;

import com.sun.jaspic.config.helper.AuthContextHelper;
import com.sun.jaspic.config.helper.AuthConfigProviderHelper;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Map;
import javax.security.auth.message.AuthException;
import javax.security.auth.message.config.AuthConfigFactory;
import javax.security.auth.message.config.AuthConfigFactory.RegistrationContext;

/**
 *
 * @author Ron Monzillo
 */
public abstract class JAASAuthConfigProvider extends AuthConfigProviderHelper {

    private static final String CONFIG_FILE_NAME_KEY = "config.file.name";
    private static final String DEFAULT_JAAS_APP_NAME = "other";
    private static final String ALL_APPS = "*";

    private String configFileName;
    private ExtendedConfigFile jaasConfig;

    private Map<String, ?> properties;
    private AuthConfigFactory factory;

    public JAASAuthConfigProvider(Map properties, AuthConfigFactory factory) {
        this.properties = properties;
        this.factory = factory;

        configFileName = getProperty(CONFIG_FILE_NAME_KEY,null);

        if (configFileName == null) {
            jaasConfig = new ExtendedConfigFile();
        } else {
            try {
                URI uri = new URI(configFileName);
                jaasConfig = new ExtendedConfigFile(uri);
            } catch (URISyntaxException use) {
                IllegalArgumentException iae = new IllegalArgumentException(use);
                throw iae;
            }
        }
       selfRegister();
    }

    public Map<String, ?> getProperties() {
        return properties;
    }


    public AuthConfigFactory getFactory() {
        return factory;
    }

    private RegistrationContext getRegistrationContext(String id) {

        final String layer = getLayer();
        final String appContext;
        if (id.toLowerCase(Locale.getDefault()).equals(DEFAULT_JAAS_APP_NAME)) {
            appContext = ALL_APPS;
        } else {
            appContext = id;
        }

        return new RegistrationContext() {

            final String description = "JAAS AuthConfig: " + appContext;

            public String getMessageLayer() {
                return layer;
            }

            public String getAppContext() {
                return appContext;
            }

            public String getDescription() {
                return description;
            }

            public boolean isPersistent() {
                return false;
            }
        };
    }

    public AuthConfigFactory.RegistrationContext[] getSelfRegistrationContexts() {
        final String[] appContexts = jaasConfig.getAppNames(getModuleTypes());
        RegistrationContext[] rvalue = new RegistrationContext[appContexts.length];
        for (int i = 0; i < appContexts.length; i++) {
            rvalue[i] = getRegistrationContext(appContexts[i]);
        }
        return rvalue;
    }

    public AuthContextHelper getAuthContextHelper(String appContext, boolean returnNullContexts)
            throws AuthException {
        return new JAASAuthContextHelper(getLoggerName(), returnNullContexts,
                jaasConfig, properties, appContext);
    }

    @Override
    public void refresh() {
        jaasConfig.refresh();
        super.refresh();
    }

}
