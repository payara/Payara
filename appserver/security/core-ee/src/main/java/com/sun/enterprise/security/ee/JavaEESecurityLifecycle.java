/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2012 Oracle and/or its affiliates. All rights reserved.
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
// Portions Copyright [2018-2021] [Payara Foundation and/or its affiliates]
package com.sun.enterprise.security.ee;

import com.sun.enterprise.security.ContainerSecurityLifecycle;
import com.sun.enterprise.security.jaspic.config.GFAuthConfigFactory;
import com.sun.logging.LogDomains;

import java.security.Security;
import java.util.logging.Logger;

import jakarta.inject.Singleton;

import org.glassfish.common.util.Constants;
import org.glassfish.hk2.api.PostConstruct;
import org.glassfish.hk2.api.Rank;
import org.glassfish.internal.api.InitRunLevel;
import org.jvnet.hk2.annotations.Service;

import static java.util.logging.Level.WARNING;
import static jakarta.security.auth.message.config.AuthConfigFactory.DEFAULT_FACTORY_SECURITY_PROPERTY;


/**
 * @author vbkumarjayanti
 * @author David Matejcek
 */
@InitRunLevel
@Rank(Constants.IMPORTANT_RUN_LEVEL_SERVICE)
@Service
@Singleton
public class JavaEESecurityLifecycle implements ContainerSecurityLifecycle, PostConstruct {

    private static final Logger LOG = LogDomains.getLogger(JavaEESecurityLifecycle.class, LogDomains.SECURITY_LOGGER);

    @Override
    public void postConstruct() {
        onInitialization();
    }


    @Override
    public void onInitialization() {
        LOG.finest(() -> "Initializing " + getClass());

        // TODO: Need some way to not override the security manager if the EmbeddedServer was
        // run with a different non-default security manager.
        //
        // Right now there seems no way to find out if the security manager is the VM's default security manager.
        final SecurityManager systemSecurityManager = System.getSecurityManager();
        if (systemSecurityManager != null && !(J2EESecurityManager.class.equals(systemSecurityManager.getClass()))) {
            J2EESecurityManager eeSecurityManager = new J2EESecurityManager();
            try {
                System.setSecurityManager(eeSecurityManager);
                LOG.config(() -> "System security manager has been set to " + eeSecurityManager);
            } catch (SecurityException ex) {
                LOG.log(WARNING, "security.secmgr.could.not.override", ex);
            }
        }
        initializeJASPIC();
    }

    private void initializeJASPIC() {
        // Define default factory if it is not already defined.
        // The factory will be constructed on the first getFactory call.
        final String defaultFactory = Security.getProperty(DEFAULT_FACTORY_SECURITY_PROPERTY);
        if (defaultFactory == null) {
            final String defaultAuthConfigProvideFactoryClassName = GFAuthConfigFactory.class.getName();
            Security.setProperty(DEFAULT_FACTORY_SECURITY_PROPERTY, defaultAuthConfigProvideFactoryClassName);
            LOG.config(() -> String.format("System JVM option '%s' has been set to '%s'",
                DEFAULT_FACTORY_SECURITY_PROPERTY, defaultAuthConfigProvideFactoryClassName));
        }
    }
}
