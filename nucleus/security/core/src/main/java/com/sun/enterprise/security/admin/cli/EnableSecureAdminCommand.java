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

package com.sun.enterprise.security.admin.cli;

import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.SecureAdmin;
import com.sun.enterprise.config.serverbeans.SecureAdminHelper;
import com.sun.enterprise.config.serverbeans.SecureAdminHelper.SecureAdminCommandException;
import com.sun.enterprise.config.serverbeans.SecureAdminPrincipal;
import com.sun.enterprise.security.SecurityLoggerInfo;
import com.sun.enterprise.security.ssl.SSLUtils;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.RestEndpoint;
import org.glassfish.api.admin.RestEndpoints;
import org.glassfish.api.admin.RuntimeType;
import javax.inject.Inject;
import org.glassfish.api.admin.AccessRequired;

import org.jvnet.hk2.annotations.Service;
import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.config.TransactionFailure;

/**
 * Records that secure admin is to be used and adjusts each admin listener
 * configuration in the domain to use secure admin.
 *
 * The command changes the admin-listener set-up within each separate
 * configuration as if by running
 * these commands:
 * <pre>
 * {@code
        ###
	### create new protocol for secure admin
	###
	asadmin create-protocol --securityenabled=true sec-admin-listener
	asadmin create-http --default-virtual-server=__asadmin sec-admin-listener
	#asadmin create-network-listener --listenerport 4849 --protocol sec-admin-listener sec-admin-listener
	asadmin create-ssl --type network-listener --certname s1as --ssl2enabled=false --ssl3enabled=false --clientauthenabled=false sec-admin-listener
        asadmin set configs.config.server-config.network-config.protocols.protocol.sec-admin-listener.ssl.client-auth=want
	asadmin set configs.config.server-config.network-config.protocols.protocol.sec-admin-listener.ssl.classname=com.sun.enterprise.security.ssl.GlassfishSSLImpl


	###
	### create the port redirect config
	###
	asadmin create-protocol --securityenabled=false admin-http-redirect
	asadmin create-http-redirect --secure-redirect true admin-http-redirect
	#asadmin create-http-redirect --secure-redirect true --redirect-port 4849 admin-http-redirect
	asadmin create-protocol --securityenabled=false pu-protocol
	asadmin create-protocol-finder --protocol pu-protocol --targetprotocol sec-admin-listener --classname org.glassfish.grizzly.config.portunif.HttpProtocolFinder http-finder
	asadmin create-protocol-finder --protocol pu-protocol --targetprotocol admin-http-redirect --classname org.glassfish.grizzly.config.portunif.HttpProtocolFinder admin-http-redirect

	###
	### update the admin listener
	###
	asadmin set configs.config.server-config.network-config.network-listeners.network-listener.admin-listener.protocol=pu-protocol
 * }
 *
 *
 * @author Tim Quinn
 */
@Service(name = "enable-secure-admin")
@PerLookup
@I18n("enable.secure.admin.command")
@ExecuteOn({RuntimeType.DAS,RuntimeType.INSTANCE})
@RestEndpoints({
    @RestEndpoint(configBean=Domain.class,
        opType=RestEndpoint.OpType.POST, 
        path="enable-secure-admin", 
        description="enable-secure-admin")
})
@AccessRequired(resource="domain/secure-admin", action="enable")
public class EnableSecureAdminCommand extends SecureAdminCommand {

    @Param(optional = true, defaultValue = SecureAdmin.Duck.DEFAULT_ADMIN_ALIAS)
    public String adminalias;

    @Param(optional = true, defaultValue = SecureAdmin.Duck.DEFAULT_INSTANCE_ALIAS)
    public String instancealias;

    @Inject
    private SSLUtils sslUtils;

    @Inject
    private SecureAdminHelper secureAdminHelper;

    private KeyStore keystore = null;

    @Override
    public void run() throws TransactionFailure, SecureAdminCommandException {
        try {
            ensureNoAdminUsersWithEmptyPassword();
        } catch (SecureAdminCommandException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new TransactionFailure((ex.getMessage() != null ? ex.getMessage() : ""));
        }
            super.run();
    }

    private void ensureNoAdminUsersWithEmptyPassword() throws SecureAdminCommandException {
        boolean isAdminUserWithoutPassword;
        try {
            isAdminUserWithoutPassword = secureAdminHelper.isAnyAdminUserWithoutPassword();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        if (isAdminUserWithoutPassword) {
            throw new SecureAdminCommandException(Strings.get("adminsWithEmptyPW"));
        }
        
    }

    
    @Override
    Iterator<Work<TopLevelContext>> secureAdminSteps() {
        return stepsIterator(secureAdminSteps);
    }

    @Override
    Iterator<Work<ConfigLevelContext>> perConfigSteps() {
        return stepsIterator(perConfigSteps);
    }

    /**
     * Iterator which returns array elements from front to back.
     * @param <T>
     * @param steps
     * @return
     */
    private <T  extends SecureAdminCommand.Context> Iterator<Work<T>> stepsIterator(Step<T>[] steps) {
        return new Iterator<Work<T>> () {
            private Step<T>[] steps;
            private int nextSlot;

            @Override
            public boolean hasNext() {
                return nextSlot < steps.length;
            }

            @Override
            public Work<T> next() {
                return steps[nextSlot++].enableWork();
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }

            Iterator<Work<T>> init(Step<T>[] values) {
                this.steps = values;
                nextSlot  = 0;
                return this;
            }

        }.init(steps);
    }
    
    @Override
    protected boolean updateSecureAdminSettings(
            final SecureAdmin secureAdmin_w) {
        /*
         * Apply the values for the aliases.  We do this whether the user
         * gave explicit values or not, because we need to do some work
         * even for the default aliases.
         */
        try {
            final List<String> badAliases = new ArrayList<String>();
            secureAdmin_w.setDasAlias(processAlias(adminalias, 
                    SecureAdmin.Duck.DEFAULT_ADMIN_ALIAS, 
                    secureAdmin_w, badAliases));
            secureAdmin_w.setInstanceAlias(processAlias(instancealias, 
                    SecureAdmin.Duck.DEFAULT_INSTANCE_ALIAS,
                    secureAdmin_w, badAliases));
            
            ensureSpecialAdminIndicatorIsUnique(secureAdmin_w);
            
            if (badAliases.size() > 0) {
                throw new SecureAdminCommandException(
                        Strings.get("enable.secure.admin.badAlias",
                        badAliases.size(), badAliases.toString()));
            }
            ensureSpecialAdminIndicatorIsUnique(secureAdmin_w);
            return true;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private String processAlias(String alias, final String defaultAlias, final SecureAdmin secureAdmin_w,
            Collection<String> badAliases) throws IOException, KeyStoreException {
        boolean isAliasOK;
        /*
         * Do not validate the default aliases.  The user might be using
         * password-based inter-process authentication in which case the aliases
         * might not even be present in the keystore and/or truststore.  
         */
        if (alias.equals(defaultAlias)) {
            isAliasOK = true;
        } else {
            isAliasOK = validateAlias(alias);
            if ( ! isAliasOK) {
                badAliases.add(alias);
            }
        }
        if (isAliasOK) {
            /*
             * If there is no SecureAdminPrincipal for the DN corresponding
             * to the specified alias then add one now.
             */
            ensureSecureAdminPrincipalForAlias(alias,
                    secureAdmin_w);
        }
        return alias;   
    }

    @Override
    protected String transactionErrorMessageKey() {
        return SecurityLoggerInfo.enablingSecureAdminError;
    }

    private void ensureSpecialAdminIndicatorIsUnique(final SecureAdmin secureAdmin_w) {
        if (secureAdmin_w.getSpecialAdminIndicator().equals(SecureAdmin.Util.ADMIN_INDICATOR_DEFAULT_VALUE)) {
            /*
             * Set the special admin indicator to a unique identifier.
             */
             final UUID uuid = UUID.randomUUID();
             secureAdmin_w.setSpecialAdminIndicator(uuid.toString());
        }
    }
    
    /**
     * Makes sure there is a SecureAdminPrincipal entry for the specified
     * alias.  If not, one is added in the context of the current
     * transaction.
     * 
     * @param alias the alias to check for
     * @param secureAdmin_w SecureAdmin instance (already in a transaction)
     */
    private void ensureSecureAdminPrincipalForAlias(final String alias,
            final SecureAdmin secureAdmin_w) {
        SecureAdminPrincipal p = getSecureAdminPrincipalForAlias(alias, secureAdmin_w);
        if (p != null) {
            return;
        }
        try {
            /*
             * Create a new SecureAdminPrincipal.
             */
            final String dn = secureAdminHelper.getDN(alias, true);
            p = secureAdmin_w.createChild(SecureAdminPrincipal.class);
            p.setDn(dn);
            secureAdmin_w.getSecureAdminPrincipal().add(p);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
    
    private SecureAdminPrincipal getSecureAdminPrincipalForAlias(final String alias,
            final SecureAdmin secureAdmin_w) {
        try {
            final String dnForAlias = secureAdminHelper.getDN(alias, true);
            for (SecureAdminPrincipal p : secureAdmin_w.getSecureAdminPrincipal()) {
                if (p.getDn().equals(dnForAlias)) {
                    return p;
                }
            }
            return null;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
    
    private synchronized KeyStore keyStore() throws IOException {
        if (keystore == null) {
            keystore = sslUtils.getKeyStore();
        }
        return keystore;
    }

    private boolean validateAlias(final String alias) throws IOException, KeyStoreException  {
        return keyStore().containsAlias(alias);
    }
}
