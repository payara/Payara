/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright (c) 2010-2011 Oracle and/or its affiliates. All rights reserved.
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
import com.sun.enterprise.config.serverbeans.Configs;
import com.sun.enterprise.config.serverbeans.HttpService;
import com.sun.enterprise.config.serverbeans.VirtualServer;
import com.sun.enterprise.security.admin.cli.SecureAdminCommand.SecureAdminCommandException;
import com.sun.enterprise.security.SecurityUpgradeService;
import org.glassfish.grizzly.config.dom.NetworkConfig;
import org.glassfish.grizzly.config.dom.NetworkListener;
import org.glassfish.grizzly.config.dom.NetworkListeners;
import org.glassfish.grizzly.config.dom.Protocol;
import org.glassfish.grizzly.config.dom.Ssl;
import java.beans.PropertyVetoException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.glassfish.api.admin.config.ConfigurationUpgrade;
import org.glassfish.config.support.GrizzlyConfigSchemaMigrator;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.Habitat;
import org.jvnet.hk2.component.PostConstruct;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.RetryableException;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.Transaction;
import org.jvnet.hk2.config.TransactionFailure;

/**
 * Upgrades older config to current.
 *
 * @author Tim Quinn
 */
@Service
public class SecureAdminConfigUpgrade implements ConfigurationUpgrade, PostConstruct {

    private final static String DAS_CONFIG_NAME = "server-config";
    private final static String ADMIN_LISTENER_NAME = "admin-listener";
    
    /*
     * Constants used for creating a missing network-listener during upgrade.
     * Ideally this will be handled in the grizzly upgrade code.
     */
    private final static String ASADMIN_LISTENER_PORT = "${ASADMIN_LISTENER_PORT}";
    private final static String ASADMIN_LISTENER_TRANSPORT = "tcp";
    private final static String ASADMIN_LISTENER_THREADPOOL = "http-thread-pool";

    private final static String ASADMIN_VS_NAME = "__asadmin";
        
    private static final Logger logger = Logger.getAnonymousLogger();

    // Thanks to Jerome for suggesting this injection to make sure the
    // Grizzly migration runs before this migration
    @Inject
    private GrizzlyConfigSchemaMigrator grizzlyMigrator;

    @Inject
    private SecurityUpgradeService securityUpgradeService;

    @Inject
    private Habitat habitat;

    @Inject
    private Configs configs;

    @Override
    public void postConstruct() {
        try {
            ensureNonDASConfigsHaveAdminNetworkListener();
            logger.log(Level.INFO, "Added admin-listener network listeners to non-DAS configurations");
        } catch (TransactionFailure tf) {
            logger.log(Level.SEVERE, "Error adding admin-listener to non-DAS configuration", tf);
            return;
        }
        /*
         * See if we need to set up secure admin during the upgrade.
         */
        if (requiresSecureAdmin()) {
            final EnableSecureAdminCommand enableSecureAdminCommand =
                    habitat.getComponent(EnableSecureAdminCommand.class);
            try {
                enableSecureAdminCommand.run();
                logger.log(Level.INFO, "Upgraded secure admin set-up");
            } catch (TransactionFailure tf){
                Logger.getAnonymousLogger().log(Level.SEVERE,
                        "Error upgrading secure admin set-up", tf);
            } catch (SecureAdminCommandException ex) {
                logger.log(Level.INFO,
                            "Attempt to upgrade secure admin set-up failed",
                            ex);
            }
        } else {
            logger.log(Level.INFO, "No secure admin set-up was detected in the original configuration so no upgrade of it was needed");
        }
    }

    private boolean requiresSecureAdmin() {
        return isOriginalAdminSecured() || securityUpgradeService.requiresSecureAdmin();
    }

    private void ensureNonDASConfigsHaveAdminNetworkListener() throws TransactionFailure {

        final Transaction t = new Transaction();
        
        for (Config c : configs.getConfig()) {
            final NetworkConfig nc = c.getNetworkConfig();
            final NetworkListener nl = nc.getNetworkListener(SecureAdminCommand.ADMIN_LISTENER_NAME);
            if (nl != null) {
                continue;
            }
            
            /*
             * Create an admin-listener for this configuration.
             */
            ConfigSupport.apply(new SingleConfigCode<Config>() {

                @Override
                public Object run(Config config_w) throws PropertyVetoException, TransactionFailure {
                    
                    final NetworkListener nl_w = createAdminNetworkListener(t, nc);
                    final VirtualServer vs_w = createAdminVirtualServer(t, config_w);
                    return config_w;
                }
                
            }, c);
                    
        }
        try {
            t.commit();
        } catch (RetryableException ex) {
            throw new TransactionFailure("Error adding admin-listener for a non-DAS config", ex);
        }
        
    }

    private NetworkListener createAdminNetworkListener(
            final Transaction t,
            final NetworkConfig nc) throws TransactionFailure {
        final NetworkListeners nls_w = t.enroll(nc.getNetworkListeners());
        final NetworkListener nl_w = nls_w.createChild(NetworkListener.class);
        nls_w.getNetworkListener().add(nl_w);
        nl_w.setName(ADMIN_LISTENER_NAME);
        nl_w.setProtocol(ADMIN_LISTENER_NAME);
        nl_w.setPort(ASADMIN_LISTENER_PORT);
        nl_w.setTransport(ASADMIN_LISTENER_TRANSPORT);
        nl_w.setThreadPool(ASADMIN_LISTENER_THREADPOOL);
        return nl_w;
    }
    
    private VirtualServer createAdminVirtualServer(
            final Transaction t,
            final Config config_w) throws TransactionFailure, PropertyVetoException {
        final HttpService hs_w = t.enroll(config_w.getHttpService());
        final VirtualServer vs_w = hs_w.createChild(VirtualServer.class);
        hs_w.getVirtualServer().add(vs_w);
        vs_w.setId(ASADMIN_VS_NAME);
        vs_w.setNetworkListeners(ADMIN_LISTENER_NAME);
        return vs_w;
    }
    
    
    
    private boolean isOriginalAdminSecured() {
        /*
         * The Grizzly conversion has already occurred.  So look for
         * 
         * <server-config>
         *   <network-config>
         *     <protocols>
         *       <protocol name="admin-listener">
         *         <ssl ...>
         *
         */
         final Config serverConfig;
         final NetworkConfig nc;
         final Protocol p;
         final Ssl ssl ;
         if ((serverConfig = configs.getConfigByName(DAS_CONFIG_NAME)) == null) {
             return false;
         }

         if ((nc = serverConfig.getNetworkConfig()) == null) {
             return false;
         }

         if ((p = nc.findProtocol(ADMIN_LISTENER_NAME)) == null) {
             return false;
         }

         
         if ((ssl = p.getSsl()) == null) {
             return false;
         }
         return true;
    }

}
