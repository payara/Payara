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

import com.sun.enterprise.config.serverbeans.AdminService;
import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.Configs;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.JmxConnector;
import com.sun.enterprise.config.serverbeans.SecureAdmin;
import com.sun.enterprise.config.serverbeans.SecureAdminHelper.SecureAdminCommandException;
import com.sun.enterprise.security.SecurityLoggerInfo;

import org.glassfish.grizzly.config.dom.FileCache;
import org.glassfish.grizzly.config.dom.Http;
import org.glassfish.grizzly.config.dom.HttpRedirect;
import org.glassfish.grizzly.config.dom.NetworkConfig;
import org.glassfish.grizzly.config.dom.NetworkListener;
import org.glassfish.grizzly.config.dom.PortUnification;
import org.glassfish.grizzly.config.dom.Protocol;
import org.glassfish.grizzly.config.dom.ProtocolFinder;
import org.glassfish.grizzly.config.dom.Protocols;
import org.glassfish.grizzly.config.dom.Ssl;
import java.beans.PropertyVetoException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.glassfish.api.ActionReport;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import javax.inject.Inject;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.Transaction;
import org.jvnet.hk2.config.TransactionFailure;

/**
 * Provides common behavior for the enable and disable secure admin commands.
 * <p>
 * This class, and the concrete subclasses EnableSecureAdminCommand and
 * DisableSecureAdminComment, define what must be done in terms of steps.
 * Each step has enable work and disable work.  This class builds 
 * arrays of steps, one array which operates at the domain level and one which
 * operates at the config level.  The enable work of these steps is run in order through
 * the array to fully
 * enable secure admin, and the disable work of these steps are run in REVERSE
 * order to fully disable secure admin.
 * <p>
 * I have defined the steps and their enable and disable work here, together
 * in some inner classes, rather than separately in the EnableSecureAdminCommand
 * and DisableSecureAdminCommand classes to try to keep common details together.
 * The concrete subclasses can override a few methods, in particular overriding
 * methods which return an Iterator over the steps to be performed.  This way we
 * have a single array of all the steps, with each class returning a forward-running
 * or reverse-running iterator over those steps.
 *
 * @author Tim Quinn
 */
public abstract class SecureAdminCommand implements AdminCommand {

    final static String SEC_ADMIN_LISTENER_PROTOCOL_NAME = "sec-admin-listener";
    private final static String REDIRECT_PROTOCOL_NAME = "admin-http-redirect";
    public final static String ADMIN_LISTENER_NAME = "admin-listener";
    static final String DAS_CONFIG_NAME = "server-config";
    final static String PORT_UNIF_PROTOCOL_NAME = "pu-protocol";
    
    static final Logger logger = SecurityLoggerInfo.getLogger();


    @Inject
    protected Domain domain;

    /**
     * Applies changes other than whether secure admin is enabled or disabled
     * to the secure-admin element.
     * <p>
     * This method is primarily for the enable processing to apply the
     * admin and/or instance alias values, if specified on the enable-secure-admin
     * command, to the secure-admin element.
     *
     * @param secureAdmin_w
     * @return
     */
    boolean updateSecureAdminSettings(
            final SecureAdmin secureAdmin_w) throws TransactionFailure {
        /*
         * Default implementation - currently used by DisableSecureAdminCommand
         * and overridden by EnableSecureAdminCommand.
         */
        return true;
    }

    interface Context {
    }

    /**
     * Work to be performed - either for enable or disable - in a step.
     * @param <T>
     */
    interface Work<T extends Context> {
        boolean run(final T context) throws TransactionFailure;
    }

    /**
     * A step to be performed during enabling or disabling secure admin.
     *
     * @param <T> either TopLevelContext or ConfigLevelContext, depending on
     * whether the step applies per-domain or per-config.
     */
    interface Step<T extends Context> {
        Work<T> enableWork();
        Work<T> disableWork();
    }

//    interface TopLevelStep {
//        TopLevelWork enableWork();
//        TopLevelWork disableWork();
//    }

//    /**
//     * The contract for performing the work associate with a single step of
//     * enabling or disabling secure admin.
//     */
//    interface SecureAdminWork<T> {
//        boolean run(
//                Transaction t,
//                AdminCommandContext context,
//                T writeableConfigBean) throws TransactionFailure;
//    }
//
//    /**
//     * Enabling or disabling secure admin each involves multiple steps.
//     * Each step has some logic run during enable and some logic run during disable
//     * @param <T>
//     */
//    interface SecureAdminStep<T> {
//        SecureAdminWork<T> enableWork();
//        SecureAdminWork<T> disableWork();
//    }

    static class AbstractContext implements Context {
    }

    /**
     * Keeps track of whether we've found writable versions of various 
     * ConfigBeans.
     */
    static class TopLevelContext extends AbstractContext {

        private final Transaction t;
        private final Domain d;
        private Domain d_w = null;

        private SecureAdmin secureAdmin_w = null;

        TopLevelContext(
                final Transaction t,
                final Domain d) {
            this.t = t;
            this.d = d;
        }


        Domain writableDomain() throws TransactionFailure {
            if (d_w == null) {
                d_w = t.enroll(d);
            }
            return d_w;
        }

        /*
         * Gets the SecureAdmin object in writeable form, from the specified
         * domain if the SecureAdmin object already exists or by creating a new
         * one attached to the domain if one does not already exist.
         */
        SecureAdmin writableSecureAdmin() throws TransactionFailure {
            if (secureAdmin_w == null) {

                /*
                 * Create the secure admin node if it is not already there.
                 */
                SecureAdmin secureAdmin = d.getSecureAdmin();
                if (secureAdmin == null) {
                    secureAdmin_w = writableDomain().createChild(SecureAdmin.class);
                    writableDomain().setSecureAdmin(secureAdmin_w);
                } else {
                    /*
                     * It already existed, so join it to the transaction.
                     */
                    secureAdmin_w = t.enroll(secureAdmin);
                }
            }
            return secureAdmin_w;
        }
    }

    /**
     * Tracks writable config beans for each configuration.
     */
    static class ConfigLevelContext extends AbstractContext {

        private static final String CLIENT_AUTH_VALUE = "want";
        private static final String SSL3_ENABLED_VALUE = "true";
        private static final String CLASSNAME_VALUE = "com.sun.enterprise.security.ssl.GlassfishSSLImpl";
        
        private final Transaction t;
        private final Config config_w;
        private final TopLevelContext topLevelContext;

        private Protocols protocols_w = null;
        private Map<String,Protocol> namedProtocols_w = new
                HashMap<String,Protocol>();

        private JmxConnector jmxConnector_w = null;
        private Ssl jmxConnectorSsl_w = null;

        ConfigLevelContext(
                final TopLevelContext topLevelContext,
                final Config config_w) {
            this.topLevelContext = topLevelContext;
            this.t = topLevelContext.t;
            this.config_w = config_w;
        }

        /**
         * Prepares a given Ssl configuration instance so the connection 
         * represented by the Ssl's parent configuration object operates
         * securely, using SSL.
         * 
         * @param ssl_w writeable Ssl instance to be modified
         * @param certNickname the cert nickname to be used by the connection to identify itself
         * @return 
         */
        private static Ssl initSsl(final Ssl ssl_w, final String certNickname) {
            ssl_w.setClientAuth(CLIENT_AUTH_VALUE);
            ssl_w.setSsl3Enabled(SSL3_ENABLED_VALUE);
            ssl_w.setClassname(CLASSNAME_VALUE);
            ssl_w.setCertNickname(certNickname);
            ssl_w.setRenegotiateOnClientAuthWant(true);
            return ssl_w;
        }
        
        private static String chooseCertNickname(
                final String configName,
                final String dasAlias,
                final String instanceAlias) throws TransactionFailure {
            return (configName.equals(DAS_CONFIG_NAME) ? dasAlias : instanceAlias);
        }
        
        private JmxConnector writeableJmxConnector() throws TransactionFailure {
            if (jmxConnector_w == null) {
                final AdminService adminService = config_w.getAdminService();
                if (adminService == null) {
                    return null;
                }
                final JmxConnector jmxC = adminService.getSystemJmxConnector();
                if (jmxC == null) {
                    return null;
                }
                jmxConnector_w = t.enroll(jmxC);
            }
            return jmxConnector_w;
        }
        
        private Ssl writeableJmxSSL() throws TransactionFailure, PropertyVetoException {
            if (jmxConnectorSsl_w == null) {
                final JmxConnector jmxC_w = writeableJmxConnector();
                if (jmxC_w == null) {
                    return null;
                }
                Ssl jmxConnectorSsl = jmxC_w.getSsl();
                if (jmxConnectorSsl == null) {
                    jmxConnectorSsl = jmxC_w.createChild(Ssl.class);
                    jmxC_w.setSsl(jmxConnectorSsl);
                    jmxConnectorSsl_w = jmxConnectorSsl;
                } else {
                    jmxConnectorSsl_w = t.enroll(jmxConnectorSsl);
                }
            }
            return jmxConnectorSsl_w;
        }
        private Protocols writableProtocols() throws TransactionFailure {
            if (protocols_w == null) {
                final NetworkConfig nc = config_w.getNetworkConfig();
                if (nc == null) {
                    return null;
                }
                final Protocols p = nc.getProtocols();
                protocols_w = t.enroll(p);
            }
            return protocols_w;
        }

        private Protocol writableProtocol(final String protocolName,
                final boolean isSecure) throws TransactionFailure {
            Protocol p_w = namedProtocols_w.get(protocolName);
            if (p_w == null) {
                /*
                 * Try to find an existing entry for this protocol.
                 */
                final Protocol p_r = findProtocol(protocolName);
                if (p_r == null) {
                    final Protocols ps_w = writableProtocols();
                    if (ps_w == null) {
                        return null;
                    }
                    p_w = ps_w.createChild(Protocol.class);
                    p_w.setName(protocolName);
                    ps_w.getProtocol().add(p_w);
                } else {
                    p_w = t.enroll(p_r);
                }
                namedProtocols_w.put(protocolName, p_w);
            }
            p_w.setSecurityEnabled(Boolean.toString(isSecure));
            return p_w;
        }

        private Protocol findProtocol(final String protocolName) {
            final NetworkConfig nc = config_w.getNetworkConfig();
            if (nc == null) {
                return null;
            }
            final Protocols ps = nc.getProtocols();
            if (ps == null) {
                return null;
            }
            return ps.findProtocol(protocolName);
        }

        private void deleteProtocol(
                    final String protocolName) throws TransactionFailure {
                final Protocols ps_w = writableProtocols();
                if (ps_w == null) {
                    return;
                }
                final Protocol doomedProtocol = ps_w.findProtocol(protocolName);
                if (doomedProtocol != null) {
                    ps_w.getProtocol().remove(doomedProtocol);
                }
            }
    }

    /**
     * Updates the secure-admin element itself.
     * <p>
     * The concrete command implementation classes implement updateSecureAdminSettings
     * differently but they expose the same method signature, so onEnable and
     * onDisable just invoke the same method.
     */
    private Step<TopLevelContext> perDomainStep = new Step<TopLevelContext>() {

        /**
         * Sets enabled=true/false on the secure-admin element.
         */
        private void updateSecureAdminEnabledSetting(
                final SecureAdmin secureAdmin_w,
                final boolean newEnabledValue) {
            secureAdmin_w.setEnabled(Boolean.toString(newEnabledValue));
        }

        /*
         * Both the enable and disable steps for the "secure admin element only"
         * task set the enabled value and then let the concrete command
         * subclass do any additional work.
         */
        class TopLevelWork implements Work<TopLevelContext> {

            private final boolean newEnabledState;

            TopLevelWork(final boolean newEnabledState) {
                this.newEnabledState = newEnabledState;
            }

            @Override
            public boolean run(final TopLevelContext context) throws TransactionFailure {
                final SecureAdmin secureAdmin_w = context.writableSecureAdmin();
                updateSecureAdminEnabledSetting(
                        secureAdmin_w, newEnabledState);
                /*
                 * The subclass might have overridden updateSecureAdminSettings.
                 * Give it a change to run logic specific to enable or disable.
                 */
                return SecureAdminCommand.this.updateSecureAdminSettings(secureAdmin_w);
            }
        }
        
        @Override
        public Work<TopLevelContext> enableWork() {
            return new TopLevelWork(true);
            };

        @Override
        public Work<TopLevelContext> disableWork() {
            return new TopLevelWork(false);
        }
    };

    /**
     * Manages the jmx-connector settings.
     */
    private Step<ConfigLevelContext> jmxConnectorStep = new Step<ConfigLevelContext>() {

        /**
         * Sets the jmx-connector security-enabled to true and creates and
         * initializes the child ssl element.
         */
        @Override
        public Work<ConfigLevelContext> enableWork() {
            return new Work<ConfigLevelContext>() {

                @Override
                public boolean run(ConfigLevelContext context) throws TransactionFailure {
                    /*
                     * Make sure the JMX connector is enabled for secure operation.
                     * Then make sure the JMX Connector's SSL child is present
                     * and correctly set up.
                     */
                    final JmxConnector jmxConnector_w = context.writeableJmxConnector();
                    if (jmxConnector_w == null) {
                        return false;
                    }
                    try {
                        jmxConnector_w.setSecurityEnabled("true");
                        final Ssl ssl_w = context.writeableJmxSSL();
                        ConfigLevelContext.initSsl(ssl_w, ConfigLevelContext.chooseCertNickname(
                                context.config_w.getName(),
                                context.topLevelContext.writableSecureAdmin().dasAlias(),
                                context.topLevelContext.writableSecureAdmin().instanceAlias()));
                        return true;
                    } catch (Exception ex) {
                        throw new RuntimeException(ex);
                    }
                }
                
            };
        }

        @Override
        public Work<ConfigLevelContext> disableWork() {
            return new Work<ConfigLevelContext>() {

                @Override
                public boolean run(ConfigLevelContext context) throws TransactionFailure {
                    /*
                     * Remove the SSL child of the JMX configuration.  Then
                     * turn off the security-enabled attribute of the JMX config.
                     */
                    final JmxConnector jmxC_w = context.writeableJmxConnector();
                    try {
                        jmxC_w.setSsl(null);
                        jmxC_w.setSecurityEnabled("false");
                        return true;
                    } catch (PropertyVetoException ex) {
                        throw new RuntimeException(ex);
                    }
                }
                
            };
        }
    };
    
    /**
     * Manages the sec-admin-listener protocol.
     */
    private Step<ConfigLevelContext> secAdminListenerProtocolStep = new Step<ConfigLevelContext>() {

        private static final String ASADMIN_VIRTUAL_SERVER_NAME = "__asadmin";

        private static final String AUTH_LAYER_NAME = "HttpServlet";
        private static final String PROVIDER_ID_VALUE = "GFConsoleAuthModule";

        private Http writeableHttpWithFileCacheChild(final Transaction t,
                final Protocol secAdminListenerProtocol_w) throws TransactionFailure {
            /*
             * Because of the calling context, the secAdminListenerProtocol is already
             * writeable -- it was just created moments ago and has not yet
             * been committed.
             */
            Http http_w;
            Http http = secAdminListenerProtocol_w.getHttp();
            if (http == null) {
                http_w = secAdminListenerProtocol_w.createChild(Http.class);
                secAdminListenerProtocol_w.setHttp(http_w);
            } else {
                http_w = t.enroll(http);
            }
            http_w.setDefaultVirtualServer(ASADMIN_VIRTUAL_SERVER_NAME);
            http_w.setEncodedSlashEnabled("true");
            
            final FileCache cache = http_w.createChild(FileCache.class);
//            cache.setEnabled("false");
            http_w.setFileCache(cache);
                    
            return http_w;
        }

        private Ssl writeableSsl(final Transaction t,
                final Protocol secAdminListenerProtocol_w,
                final String certNickname) throws TransactionFailure {
            Ssl ssl_w = null;
            Ssl ssl = secAdminListenerProtocol_w.getSsl();
            if (ssl == null) {
                ssl_w = secAdminListenerProtocol_w.createChild(Ssl.class);
                secAdminListenerProtocol_w.setSsl(ssl_w);
            } else {
                ssl_w = t.enroll(ssl);
            }
            return ConfigLevelContext.initSsl(ssl_w, certNickname);
        }
        
        @Override
        public Work<ConfigLevelContext> enableWork() {
            return new Work<ConfigLevelContext>() {

                @Override
                public boolean run(final ConfigLevelContext context) throws TransactionFailure {
                    /*
                     * Get an existing or new writeable sec-admin-listener protocol.
                     */
                    final Protocol secAdminListenerProtocol_w =
                            context.writableProtocol(SEC_ADMIN_LISTENER_PROTOCOL_NAME,
                            true);

                    /*
                     * It seems possible to create configs without network listeners
                     * and children.  In that case it's not a real config so we
                     * will skip it.
                     */
                    if (secAdminListenerProtocol_w == null) {
                        return false;
                    }
                    
                    /*
                     * Get an existing or create a new writeable http child under the new protocol.
                     */
                    writeableHttpWithFileCacheChild(context.t, secAdminListenerProtocol_w);

                    /*
                     * Get an existing or create a new writeable ssl child under the new protocol.
                     * Which cert nickname we set depends on whether this is the DAS's config
                     * we're working on or an instance's.  
                     */
                    writeableSsl(context.t, secAdminListenerProtocol_w,
                            ConfigLevelContext.chooseCertNickname(
                                context.config_w.getName(),
                                context.topLevelContext.writableSecureAdmin().dasAlias(),
                                context.topLevelContext.writableSecureAdmin().instanceAlias()));

                    return true;
                }
            };
        }

        @Override
        public Work<ConfigLevelContext> disableWork() {
            return new Work<ConfigLevelContext>() {

                @Override
                public boolean run(ConfigLevelContext context) throws TransactionFailure {


                    context.deleteProtocol(SEC_ADMIN_LISTENER_PROTOCOL_NAME);

                    return true;
                }
            };
        }
    };

    private enum ProtocolFinderInfo {
        HTTP_FINDER("http-finder", SEC_ADMIN_LISTENER_PROTOCOL_NAME),
        ADMIN_HTTP_REDIRECT_FINDER("admin-http-redirect", REDIRECT_PROTOCOL_NAME);

        private final String name;
        private final String protocolName;
        private final String classname = "org.glassfish.grizzly.config.portunif.HttpProtocolFinder";

        private ProtocolFinderInfo(final String name, final String protocolName) {
            this.name = name;
            this.protocolName = protocolName;
        }

    }

    private Step<ConfigLevelContext> secAdminPortUnifAndRedirectStep = new Step<ConfigLevelContext>() {

        private final static String PORT_UNIF_PROTOCOL_NAME = "pu-protocol";
        private final static String UNSECURE_ADMIN_LISTENER_PROTOCOL_NAME = "admin-listener";

        private final static String HTTP_FINDER_PROTOCOL_FINDER_NAME = "http-finder";
        private final static String ADMIN_HTTP_REDIRECT_FINDER_NAME = "admin-http-redirect";

        private final static String PROTOCOL_FINDER_CLASSNAME = "org.glassfish.grizzly.config.portunif.HttpProtocolFinder";


        private HttpRedirect writeableHttpRedirect(
                final Transaction t,
                final Protocol adminHttpRedirectProtocol_w) throws TransactionFailure {
            HttpRedirect redirect = adminHttpRedirectProtocol_w.getHttpRedirect();
            HttpRedirect redirect_w;
            if (redirect == null) {
                redirect_w = adminHttpRedirectProtocol_w.createChild(HttpRedirect.class);
                adminHttpRedirectProtocol_w.setHttpRedirect(redirect_w);
            } else {
                redirect_w = t.enroll(redirect);
            }
            redirect_w.setSecure(Boolean.TRUE.toString());
            return redirect_w;
        }

        private PortUnification writeablePortUnification(
                final Transaction t,
                final Protocol protocol_w) throws TransactionFailure {
            PortUnification pu_w;
            PortUnification pu = protocol_w.getPortUnification();
            if (pu == null) {
                pu_w = protocol_w.createChild(PortUnification.class);
                protocol_w.setPortUnification(pu_w);
            } else {
                pu_w = t.enroll(pu);
            }
            return pu_w;
        }

        private ProtocolFinder writeableProtocolFinder(
                final Transaction t,
                final PortUnification pu_w,
                final ProtocolFinderInfo finderInfo) throws TransactionFailure {
            ProtocolFinder pf_w = null;
            for (ProtocolFinder pf : pu_w.getProtocolFinder()) {
                if (pf.getName().equals(finderInfo.name)) {
                    pf_w = t.enroll(pf);
                    break;
                }
            }
            if (pf_w == null) {
                pf_w = pu_w.createChild(ProtocolFinder.class);
                pu_w.getProtocolFinder().add(pf_w);
            }
            pf_w.setName(finderInfo.name);
            pf_w.setClassname(finderInfo.classname);
            pf_w.setProtocol(finderInfo.protocolName);
            return pf_w;
        }

        private NetworkListener writableNetworkListener(
                final Transaction t,
                final Config config_w,
                final String listenerName) throws TransactionFailure {
            NetworkListener nl_w = null;
            final NetworkConfig nc = config_w.getNetworkConfig();
            if (nc == null) {
                return null;
            }
            NetworkListener nl = nc.getNetworkListener(listenerName);
            if (nl == null) {
                return null;
            } else {
                nl_w = t.enroll(nl);
            }
            return nl_w;
        }

        

        private void assignAdminListenerProtocol(
                final Transaction t,
                final Config config_w,
                final String protocolName) throws TransactionFailure {
            final NetworkListener nl_w = writableNetworkListener(t, config_w, ADMIN_LISTENER_NAME);
            if (nl_w != null) {
                nl_w.setProtocol(protocolName);
            }
        }

        @Override
        public Work<ConfigLevelContext> enableWork() {
            return new Work<ConfigLevelContext>() {

                @Override
                public boolean run(ConfigLevelContext context) throws TransactionFailure {
                    /*
                     * Get an existing or new writeable admin-http-redirect protocol.
                     */

                    final Protocol adminHttpRedirectProtocol_w =
                            context.writableProtocol(
                            REDIRECT_PROTOCOL_NAME,
                            false);

                    if (adminHttpRedirectProtocol_w == null) {
                        return true;
                    }

                    writeableHttpRedirect(
                            context.t, adminHttpRedirectProtocol_w);

                    final Protocol puProtocol_w = context.writableProtocol(
                            PORT_UNIF_PROTOCOL_NAME,
                            false);

                    final PortUnification portUnif_w = writeablePortUnification(
                            context.t, puProtocol_w);

                    writeableProtocolFinder(
                            context.t, portUnif_w, ProtocolFinderInfo.HTTP_FINDER);

                    writeableProtocolFinder(
                            context.t, portUnif_w, ProtocolFinderInfo.ADMIN_HTTP_REDIRECT_FINDER);

                    assignAdminListenerProtocol(context.t, context.config_w, PORT_UNIF_PROTOCOL_NAME);
                    
                    return true;
                }
            };
        }

        @Override
        public Work<ConfigLevelContext> disableWork() {
            return new Work<ConfigLevelContext>() {

            @Override
                public boolean run(final ConfigLevelContext context) throws TransactionFailure {
                    final Config config_w = context.config_w;
                    final Transaction t = context.t;
                    assignAdminListenerProtocol(t, config_w, UNSECURE_ADMIN_LISTENER_PROTOCOL_NAME);

                    context.deleteProtocol(PORT_UNIF_PROTOCOL_NAME);
                    context.deleteProtocol(REDIRECT_PROTOCOL_NAME);
                    return true;
                }

            };
        }

    };

//    /**
//     * Details for managing the protocol associated directly with the
//     * admin listener.  This logic takes care of creating
//     * the sec-admin-listener protocol and assoociating it with
//     * the admin listener, or deleting the sec-admin-listener protocol and
//     * associating the admin listener back with the original protocol.
//     */
//    private SecureAdminWork adminListenerProtocolSetting = new SecureAdminWork<Config>() {
//
//        private final static String ADMIN_LISTENER_NETWORK_LISTENER_NAME = "admin-listener";
//        private final static String ORIGINAL_ADMIN_LISTENER_PROTOCOL_NAME = "admin-listener";
//
//
//        @Override
//        public boolean onEnable(
//                final Transaction t,
//                final AdminCommandContext context,
//                final Config tConfig) throws TransactionFailure {
//            writeableAdminNetworkListener(tConfig).
//                    setProtocol(SEC_ADMIN_LISTENER_PROTOCOL_NAME);
//            return true;
//        }
//
//        @Override
//        public boolean onDisable(
//                final Transaction t,
//                final AdminCommandContext context,
//                final Config tConfig
//                ) throws TransactionFailure {
//            writeableAdminNetworkListener(tConfig).
//                    setProtocol(ORIGINAL_ADMIN_LISTENER_PROTOCOL_NAME);
//            return true;
//        }
//
//        private NetworkListener writeableAdminNetworkListener(
//                final Config tConfig) throws TransactionFailure {
//            return tConfig.getNetworkConfig().
//                    getNetworkListener(ADMIN_LISTENER_NETWORK_LISTENER_NAME);
//        }

//        private NetworkListener writeableAdminNetworkListener(
//                final Transaction t,
//                final Config tConfig) throws TransactionFailure {
//            ConfigSupport.apply(new SingleConfigCode<NetworkListener>() {
//                @Override
//                public Object run(NetworkListener nl) throws PropertyVetoException, TransactionFailure {
//
//                    try {
//
//                        for (Config c : configs.getConfig()) {
//                            final MessagePart partForThisConfig = report.getTopMessagePart().addChild();
//
//                            /*
//                             * Again, delegate to update the admin
//                             * listener configuration.
//                             */
//                            updateAdminListenerConfig(context, t, c, partForThisConfig);
//                        }
//
//                        t.commit();
//                    } catch (RetryableException ex) {
//                        throw new RuntimeException(ex);
//                    }
//
//                    return Boolean.TRUE;
//                }
//            }, tConfig.getNetworkConfig().getNetworkListener(ADMIN_LISTENER_NETWORK_LISTENER_NAME));
//        }
//
//    };

    /**
     * Tasks executed once per enable or disable.
     */
    final Step<TopLevelContext>[] secureAdminSteps =
            new Step[] {
        perDomainStep
    };

    
    /**
     * Tasks executed once per config during an enable or disable.
     */
    final Step<ConfigLevelContext>[] perConfigSteps =
            new Step[] {
        secAdminListenerProtocolStep,
        secAdminPortUnifAndRedirectStep,
        jmxConnectorStep
    };

    /**
     * Returns the error key for finding a message describing an error
     * during the operation - either enable or disable.
     * <p>
     * Each concrete subclass overrides this to supply the relevant message key.
     *
     * @return the message key corresponding to the error message to display
     */
    protected abstract String transactionErrorMessageKey();

    /**
     * Returns an Iterator over the work (enable work for enable-secure-admin,
     * disable for disable-secure-admin) for the steps related to
     * just the top-level secure-admin element.
     *
     * @return
     */
    abstract Iterator<Work<TopLevelContext>> secureAdminSteps();

    /**
     * Returna an Iterator over the work related to each configuration that
     * has to be modified.
     *
     * @return
     */
    abstract Iterator<Work<ConfigLevelContext>> perConfigSteps();

    /**
     * Performs the enable/disable logic for secure admin.
     * <p>
     * This is separate from the execute method so it can be invoked during
     * upgrade.
     * 
     * @throws TransactionFailure
     */
    public void run() throws TransactionFailure, SecureAdminCommandException {
        ConfigSupport.apply(new SingleConfigCode<Domain>() {
            @Override
            public Object run(Domain domain_w) throws PropertyVetoException, TransactionFailure {

                // get the transaction
                final Transaction t = Transaction.getTransaction(domain_w);
                final TopLevelContext topLevelContext = 
                        new TopLevelContext(t, domain_w);
                if (t!=null) {

                    /*
                     * Do the work on just the secure-admin element.
                     */
                    for (Iterator<Work<TopLevelContext>> it = secureAdminSteps(); it.hasNext();) {
                        final Work<TopLevelContext> step = it.next();
                        if ( ! step.run(topLevelContext) ) {
                            t.rollback();
                            return Boolean.FALSE;
                        }
                    }

                    /*
                     * Now apply the required changes to the admin listener
                     * in the configurations.  Include all configs, because even
                     * though the non-DAS configs default to use SSL the user
                     * might have specified a different alias to use and that 
                     * value must be set in the SSL element for the 
                     * secure admin listener.
                     */
                    final Configs configs = domain_w.getConfigs();
                    for (Config c : configs.getConfig()) {
                        final Config c_w = t.enroll(c);
                        ConfigLevelContext configLevelContext = 
                                new ConfigLevelContext(topLevelContext, c_w);
                        for (Iterator<Work<ConfigLevelContext>> it = perConfigSteps(); it.hasNext();) {
                            final Work<ConfigLevelContext> step = it.next();
                            if ( ! step.run(configLevelContext)) {
                                t.rollback();
                                return Boolean.FALSE;
                            }
                        }
                    }
                }

                return Boolean.TRUE;
            }
        }, domain);
    }
    /**
     * Executes the particular xxx-secure-admin command (enable or disable).
     * @param context
     */
    @Override
    public void execute(final AdminCommandContext context) {
        final ActionReport report = context.getActionReport();

        try {
            report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
            run();
            report.setMessage(Strings.get("restartReq"));
        } catch (TransactionFailure ex) {
            report.failure(context.getLogger(), Strings.get(transactionErrorMessageKey()), ex);
        } catch (SecureAdminCommandException ex) {
            report.failure(context.getLogger(), ex.getLocalizedMessage());
        }
    }
    
    /*
     * Executes the command with no action report.  Primarily useful from the
     * upgrade class (which does not have a convenient action report).
     */
    void execute() throws TransactionFailure, SecureAdminCommandException {
        try {
            run();
        } catch (TransactionFailure ex) {
            logger.log(Level.SEVERE, Strings.get(transactionErrorMessageKey()), ex);
            throw ex;
        } catch (SecureAdminCommandException ex) {
            logger.log(Level.SEVERE, ex.getLocalizedMessage());
            throw ex;
        }
    }
}
