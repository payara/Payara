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
package com.sun.enterprise.security.admin.cli;

import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.SecureAdmin;
import com.sun.enterprise.config.serverbeans.SecureAdminHelper;
import com.sun.enterprise.config.serverbeans.SecureAdminPrincipal;
import com.sun.enterprise.module.bootstrap.StartupContext;
import com.sun.enterprise.security.admin.cli.SecureAdminCommand.ConfigLevelContext;
import com.sun.enterprise.security.admin.cli.SecureAdminCommand.TopLevelContext;
import com.sun.enterprise.security.admin.cli.SecureAdminCommand.Work;
import com.sun.enterprise.security.ssl.SSLUtils;
import java.io.IOException;
import java.security.KeyStoreException;
import java.util.Iterator;
import java.util.Properties;
import java.util.UUID;
import org.glassfish.grizzly.config.dom.NetworkConfig;
import org.glassfish.grizzly.config.dom.Protocol;
import javax.inject.Inject;

import org.jvnet.hk2.annotations.Service;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.hk2.api.ServiceLocator;
import org.jvnet.hk2.config.RetryableException;
import org.jvnet.hk2.config.Transaction;
import org.jvnet.hk2.config.TransactionFailure;

/**
 * Common logic for formal upgrade (i.e., start-domain --upgrade) and
 * silent upgrade (starting a newer version of GlassFish using an older version's
 * domain.xml).
 * 
 * @author Tim Quinn
 */
@Service
@PerLookup
public class SecureAdminUpgradeHelper {
    
    protected final static String DAS_CONFIG_NAME = "server-config";
    
    @Inject
    protected Domain domain;
    
    @Inject
    protected ServiceLocator habitat;
    
    @Inject
    protected StartupContext startupContext;
    
    private Transaction t = null;
    
    private SecureAdmin secureAdmin = null;
    
    private TopLevelContext topLevelContext = null;
    private SecureAdminHelper secureAdminHelper = null;
    private SSLUtils sslUtils = null;
    
    private Properties startupArgs = null;
    
    
    final protected Transaction transaction() {
        if (t == null) {
            t = new Transaction();
        }
        return t;
    }
    
    private TopLevelContext topLevelContext() {
        if (topLevelContext == null) {
            topLevelContext = new TopLevelContext(transaction(), domain);
        }
        return topLevelContext;
    }
    
    final protected void commit() throws RetryableException, TransactionFailure {
        if (t != null) { 
            t.commit();
        }
    }
    
    final protected void rollback() {
        if (t != null) {
            t.rollback();
        }
    }
    
    final protected String specialAdminIndicator() {
        final UUID uuid = UUID.randomUUID();
        return uuid.toString();
    }
    
    final protected SecureAdmin secureAdmin() throws TransactionFailure {
        if (secureAdmin == null) {
            secureAdmin = domain.getSecureAdmin();
            if (secureAdmin == null) {
                secureAdmin = /* topLevelContext(). */writableSecureAdmin(); 
                secureAdmin.setSpecialAdminIndicator(specialAdminIndicator());
            }
        }
        return secureAdmin;
    }
    
    final protected Domain writableDomain() throws TransactionFailure {
        return topLevelContext().writableDomain();
    }
    
    final protected SecureAdmin writableSecureAdmin() throws TransactionFailure {
        return topLevelContext().writableSecureAdmin();
    }
    
    final protected SecureAdminHelper secureAdminHelper() {
        if (secureAdminHelper == null) {
            secureAdminHelper = habitat.getService(SecureAdminHelper.class);
        }
        return secureAdminHelper;
    }
    
    final protected SSLUtils sslUtils() {
        if (sslUtils == null) {
            sslUtils = habitat.getService(SSLUtils.class);
        }
        return sslUtils;
    }
    
    final protected void ensureSecureAdminReady() throws TransactionFailure, IOException, KeyStoreException {
        if (secureAdmin().getSpecialAdminIndicator().isEmpty()) {
            /*
             * Set the indicator to a unique value so we can distinguish
             * one domain from another.
             */
            writableSecureAdmin().setSpecialAdminIndicator(specialAdminIndicator());
        }
        if (secureAdmin().getSecureAdminPrincipal().isEmpty() &&
            secureAdmin().getSecureAdminInternalUser().isEmpty()) {
            /*
             * Add principal(s) for the aliases.
             */
            addPrincipalForAlias(secureAdmin().dasAlias());
            addPrincipalForAlias(secureAdmin().instanceAlias());
        }
    }

    final protected String startupArg(final String argName) {
        if (startupArgs == null) {
            if (startupContext != null) {
                startupArgs = startupContext.getArguments();
            } else {
                startupArgs = new Properties(); // shouldn't happen
            }
        }
        return startupArgs.getProperty(argName);
    }
    
    private void addPrincipalForAlias(final String alias) throws IOException, KeyStoreException, TransactionFailure {
        final SecureAdminPrincipal p = writableSecureAdmin().createChild(SecureAdminPrincipal.class);
        p.setDn(secureAdminHelper().getDN(alias, true));
        writableSecureAdmin().getSecureAdminPrincipal().add(p);
    }
    
    final protected void ensureNonDASConfigsReady() throws TransactionFailure {
        for (Config c : domain.getConfigs().getConfig()) {
            if ( ! c.getName().equals(SecureAdminCommand.DAS_CONFIG_NAME)) {
                if (!ensureConfigReady(c)) {
                    break;
                }
            }
        }
    }
    
    final protected void ensureDASConfigReady() {
        
    }
    
    private boolean ensureConfigReady(final Config c) throws TransactionFailure {
        /*
         * See if this config is already set up for secure admin.
         */
        final NetworkConfig nc = c.getNetworkConfig();
        if (nc == null) {
            /*
             * If there is no network config for this configuration then it is
             * probably a test configuration of some sort.  In any case, there
             * is no lower-level network protocols to verify so declare this
             * config to be OK.
             */
            return true;
        }
        Protocol secAdminProtocol = nc.getProtocols().findProtocol(SecureAdminCommand.SEC_ADMIN_LISTENER_PROTOCOL_NAME);
        if (secAdminProtocol != null) {
            return true;
        }
        final EnableSecureAdminCommand enableCmd = new EnableSecureAdminCommand();
        final Config c_w = transaction().enroll(c);
        ConfigLevelContext configLevelContext = 
                new ConfigLevelContext(topLevelContext(), c_w);
        for (Iterator<Work<ConfigLevelContext>> it = enableCmd.perConfigSteps(); it.hasNext();) {
            final Work<ConfigLevelContext> step = it.next();
            if ( ! step.run(configLevelContext)) {
                rollback();
                return false;
            }
        }
        return true;
    }
}
