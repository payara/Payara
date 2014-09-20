/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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
package com.sun.enterprise.security.cli;

import com.sun.enterprise.config.serverbeans.AuthRealm;
import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.JaccProvider;
import com.sun.enterprise.config.serverbeans.MessageSecurityConfig;
import com.sun.enterprise.config.serverbeans.SecurityService;
import com.sun.enterprise.config.serverbeans.Server;
import com.sun.enterprise.util.LocalStringManagerImpl;
import java.util.List;
import org.glassfish.api.ActionReport;

/**
 *
 * 
 */
public class CLIUtil {
    
    final private static LocalStringManagerImpl localStrings = 
        new LocalStringManagerImpl(CLIUtil.class);    

    /**
     * Selects a config of interest from the domain, based on the target.
     * (Eliminates duplicated code formerly in Create, Delete, and ListAuthRealm).
     * 
     * @param domain
     * @param target
     * @return 
     */
    static Config chooseConfig(final Domain domain, 
            final String target) {
        Config config = null;
        Config tmp = null;
        try {
            tmp = domain.getConfigs().getConfigByName(target);
        } catch (Exception ex) {
        }

        if (tmp != null) {
            return tmp;
        }
        Server targetServer = domain.getServerNamed(target);
        if (targetServer != null) {
            config = domain.getConfigNamed(targetServer.getConfigRef());
        }
        com.sun.enterprise.config.serverbeans.Cluster cluster = domain.getClusterNamed(target);
        if (cluster != null) {
            config = domain.getConfigNamed(cluster.getConfigRef());
        }
        return config;
    }
    
    static Config chooseConfig(final Domain domain,
            final String target,
            final ActionReport report) {
        final Config config = chooseConfig(domain, target);
        if (config == null) {
            report.setMessage(localStrings.getLocalString(
                "util.noconfigfortarget",
                "Configuration for target {0} not found.", target));
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
        }
        return config;
    }
    
    static boolean isRealmNew(final SecurityService securityService,
            final String authRealmName) {
        
        // check if there exists an auth realm byt he specified name
        // if so return failure.
        List<AuthRealm> authrealms = securityService.getAuthRealm();
        for (AuthRealm authrealm : authrealms) {
            if (authrealm.getName().equals(authRealmName)) {
                return false;
            }
        }
        return true;
    }
    
    static AuthRealm findRealm(final SecurityService securityService,
            String authRealmName) {
        // ensure we have the file authrealm
        
        if (authRealmName == null) {
            authRealmName = securityService.getDefaultRealm();
        }
        
        for (AuthRealm authRealm : securityService.getAuthRealm()) {            
            if (authRealm.getName().equals(authRealmName)) {
                return authRealm;
            }
        }     
        return null;
    }
    
    static JaccProvider findJaccProvider(final SecurityService securityService,
            final String jaccProviderName) {
        final List<JaccProvider> jaccProviders = securityService.getJaccProvider();
        for (JaccProvider jaccProv : jaccProviders) {
            if (jaccProv.getName().equals(jaccProviderName)) {
                return jaccProv;
            }
        }
        return null;
    }
    
    static MessageSecurityConfig findMessageSecurityConfig(final SecurityService securityService,
            final String authLayer) {
        List<MessageSecurityConfig> mscs = securityService.getMessageSecurityConfig();        
        
        for (MessageSecurityConfig  msc : mscs) {
            if (msc.getAuthLayer().equals(authLayer)) {
                return msc;
            }
        }
        return null;
    }
}
