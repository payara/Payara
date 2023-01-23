/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2020-2021 Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/legal/LICENSE.txt.
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
package fish.payara.certificate.management.admin;

import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.Node;
import com.sun.enterprise.config.serverbeans.Servers;
import com.sun.enterprise.security.ssl.impl.MasterPasswordImpl;
import com.sun.enterprise.util.StringUtils;
import com.sun.enterprise.util.SystemPropertyConstants;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.config.support.TranslatedConfigView;
import org.glassfish.grizzly.config.dom.Protocol;
import org.glassfish.grizzly.config.dom.Ssl;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.orb.admin.config.IiopListener;
import org.glassfish.orb.admin.config.IiopService;

import jakarta.inject.Inject;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Parent class that contains various shared variables and methods for the remote certificate management commands.
 * @author Andrew Pielage <andrew.pielage@payara.fish>
 */
public abstract class AbstractRemoteCertificateManagementCommand implements AdminCommand {

    @Param(name = "target", optional = true, defaultValue = SystemPropertyConstants.DAS_SERVER_NAME)
    protected String target;

    @Param(name = "listener", optional = true)
    protected String listener;

    @Inject
    protected ServerEnvironment serverEnvironment;

    @Inject
    protected Servers servers;

    @Inject
    protected Domain domain;

    @Inject
    protected IiopService iiopService;

    @Inject
    protected ServiceLocator serviceLocator;

    protected File keystore;
    protected File truststore;
    protected char[] keystorePassword;
    protected char[] truststorePassword;

    /**
     * Resolves the keystore location and the password required to access it.
     */
    protected void resolveKeyStore() {
        Config config = servers.getServer(target).getConfig();

        if (listener != null) {
            // Check if listener is an HTTP listener
            List<Protocol> protocols = config.getNetworkConfig().getProtocols().getProtocol();
            for (Protocol protocol : protocols) {
                if (protocol.getName().equals(listener)) {
                    Ssl sslConfig = protocol.getSsl();
                    if (sslConfig != null) {
                        if (StringUtils.ok(sslConfig.getKeyStore())) {
                            keystore = new File(TranslatedConfigView.expandConfigValue(sslConfig.getKeyStore()));
                            keystorePassword = TranslatedConfigView.expandConfigValue(sslConfig.getKeyStorePassword())
                                    .toCharArray();
                        }
                    }
                }
            }

            if (keystore == null) {
                // Check if listener is an IIOP listener
                List<IiopListener> listeners = iiopService.getIiopListener();
                for (IiopListener listener : listeners) {
                    if (listener.getId().equals(listener)) {
                        Ssl sslConfig = listener.getSsl();
                        if (StringUtils.ok(sslConfig.getKeyStore())) {
                            keystore = new File(TranslatedConfigView.expandConfigValue(sslConfig.getKeyStore()));
                            keystorePassword = TranslatedConfigView.expandConfigValue(sslConfig.getKeyStorePassword())
                                    .toCharArray();
                        }
                    }
                }
            }
        }

        // Default to getting it from the JVM options if no non-default value found
        if (keystore == null) {
            List<String> jvmOptions = config.getJavaConfig().getJvmOptions();

            for (String jvmOption : jvmOptions) {
                if (jvmOption.startsWith("-Djavax.net.ssl.keyStore")) {
                    keystore = new File(TranslatedConfigView.expandConfigValue(
                            jvmOption.substring(jvmOption.indexOf("=") + 1)));
                }
            }
        }

        // If it's STILL null, just go with default
        if (keystore == null) {
            keystore = serverEnvironment.getJKS();
        }

        // If the password hasn't been set, go with master
        if (keystorePassword == null) {
            MasterPasswordImpl masterPasswordService = serviceLocator.getService(MasterPasswordImpl.class);
            keystorePassword = masterPasswordService.getMasterPassword();
        }
    }

    /**
     * Resolves the truststore location and the password required to access it.
     */
    protected void resolveTrustStore() {
        Config config = servers.getServer(target).getConfig();

        if (listener != null) {
            // Check if listener is an HTTP listener
            List<Protocol> protocols = config.getNetworkConfig().getProtocols().getProtocol();
            for (Protocol protocol : protocols) {
                if (protocol.getName().equals(listener)) {
                    Ssl sslConfig = protocol.getSsl();
                    if (sslConfig != null) {
                        if (StringUtils.ok(sslConfig.getTrustStore())) {
                            truststore = new File(TranslatedConfigView.expandConfigValue(sslConfig.getTrustStore()));
                            truststorePassword = TranslatedConfigView.expandConfigValue(sslConfig.getTrustStorePassword())
                                    .toCharArray();
                        }
                    }
                }
            }

            if (truststore == null) {
                // Check if listener is an IIOP listener
                List<IiopListener> listeners = iiopService.getIiopListener();
                for (IiopListener listener : listeners) {
                    if (listener.getId().equals(listener)) {
                        Ssl sslConfig = listener.getSsl();
                        if (StringUtils.ok(sslConfig.getTrustStore())) {
                            truststore = new File(TranslatedConfigView.expandConfigValue(sslConfig.getTrustStore()));
                            truststorePassword = TranslatedConfigView.expandConfigValue(sslConfig.getTrustStorePassword())
                                    .toCharArray();
                        }
                    }
                }
            }
        }

        // Default to getting it from the JVM options if no non-default value found
        if (truststore == null) {
            List<String> jvmOptions = config.getJavaConfig().getJvmOptions();

            for (String jvmOption : jvmOptions) {
                if (jvmOption.startsWith("-Djavax.net.ssl.trustStore")) {
                    truststore = new File(TranslatedConfigView.expandConfigValue(
                            jvmOption.substring(jvmOption.indexOf("=") + 1)));
                }
            }
        }

        // If it's STILL null, just go with default
        if (truststore == null) {
            truststore = serverEnvironment.getTrustStore();
        }

        // If the password hasn't been set, go with master
        if (truststorePassword == null) {
            MasterPasswordImpl masterPassword = serviceLocator.getService(MasterPasswordImpl.class);
            truststorePassword = masterPassword.getMasterPassword();
        }
    }

    /**
     * Creates the `add-to-keystore` or `add-to-truststore` CLI command to run on the local instance.
     * @param commandName The command name to run. Should be `add-to-keystore` or `add-to-truststore`
     * @param node The node of the target local instance
     * @param file The file to add to the target store
     * @param alias The alias to store the entry under
     * @return A list representing the CLI command to run on the local instance
     */
    protected List<String> createAddToStoreCommand(String commandName, Node node, File file, String alias) {
        List<String> command = new ArrayList<>();

        command.add(commandName);

        if (StringUtils.ok(listener)) {
            command.add("--listener");
            command.add(listener);
        }

        // serverEnvironment.getDomainName() is not always 100% accurate, and neither is getting the domaindir from
        // the config beans
        command.add("--domainname");
        command.add(domain.getName());
        command.add("--domaindir");
        command.add(System.getProperty("com.sun.aas.domainsRoot"));

        // Not relevant to the DAS
        if (!serverEnvironment.isDas()) {
            command.add("--node");
            command.add(node.getName());

            // Nodes in the default directory don't necessarily have a nodedir set
            String nodedir = node.getNodeDirAbsolute();
            if (nodedir == null) {
                nodedir = System.getProperty("com.sun.aas.agentRoot");
            }

            command.add("--nodedir");
            command.add(nodedir);
        }

        command.add("--target");
        command.add(target);

        command.add("--file");
        command.add(file.getAbsolutePath());

        if (alias != null) {
            command.add(alias);
        }

        return command;
    }
}
