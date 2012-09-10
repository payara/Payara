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


package com.sun.enterprise.security.ee;

import com.sun.enterprise.security.ContainerSecurityLifecycle;
import com.sun.enterprise.security.jmac.config.GFAuthConfigFactory;
import com.sun.logging.LogDomains;
import java.security.Security;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.security.auth.message.config.AuthConfigFactory;

import org.jvnet.hk2.annotations.Service;
import org.glassfish.hk2.api.PostConstruct;
import javax.inject.Singleton;

/**
 *
 * @author vbkumarjayanti
 */
@Service
@Singleton
public class JavaEESecurityLifecycle implements ContainerSecurityLifecycle, PostConstruct {

    private static final Logger _logger = LogDomains.getLogger(JavaEESecurityLifecycle.class, LogDomains.SECURITY_LOGGER);

    @Override
    public void onInitialization() {
        java.lang.SecurityManager secMgr = System.getSecurityManager();
        //TODO: need someway to not override the SecMgr if the EmbeddedServer was
        //run with a different non-default SM.
        //right now there seems no way to find out if the SM is the VM's default SM.
        if (secMgr != null
                && !(J2EESecurityManager.class.equals(secMgr.getClass()))) {
            J2EESecurityManager mgr = new J2EESecurityManager();
            try {
                System.setSecurityManager(mgr);
            } catch (SecurityException ex) {
                _logger.log(Level.WARNING, "security.secmgr.could.not.override");
            }
        }
        initializeJMAC();
    }

    private void initializeJMAC()  {

	// define default factory if it is not already defined
	// factory will be constructed on first getFactory call.

	String defaultFactory = Security.getProperty
	    (AuthConfigFactory.DEFAULT_FACTORY_SECURITY_PROPERTY);
	if (defaultFactory == null) {
	    Security.setProperty
		(AuthConfigFactory.DEFAULT_FACTORY_SECURITY_PROPERTY,
		 GFAuthConfigFactory.class.getName());
 	}
    }

    @Override
    public void postConstruct() {
        onInitialization();
    }
}
