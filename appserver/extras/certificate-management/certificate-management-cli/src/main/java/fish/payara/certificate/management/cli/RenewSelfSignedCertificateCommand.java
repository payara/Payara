/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2020 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.certificate.management.cli;

import com.sun.enterprise.admin.cli.CLICommand;
import com.sun.enterprise.admin.cli.Environment;
import com.sun.enterprise.admin.cli.ProgramOptions;
import com.sun.enterprise.admin.servermgmt.KeystoreManager;
import com.sun.enterprise.admin.servermgmt.RepositoryException;
import com.sun.enterprise.util.SystemPropertyConstants;
import fish.payara.certificate.management.CertificateManagementKeytoolCommands;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.glassfish.api.Param;
import org.glassfish.api.admin.CommandException;
import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.annotations.Service;

/**
 * Replaces all self-signed certificates with new ones that have the same alias and dname.
 *
 * These new certificates WILL have different fingerprints to the old ones.
 *
 * @author jonathan coustick
 * @since 5.21.0
 */
@Service(name = "renew-self-signed-certificates")
@PerLookup
public class RenewSelfSignedCertificateCommand extends AbstractCertManagementCommand {

    private static final Logger LOGGER = Logger.getLogger(RenewSelfSignedCertificateCommand.class.getPackage().getName());

    @Param(name = "reload", optional = true)
    private boolean reload;

    private KeyStore store;

    @Override
    protected int executeCommand() throws CommandException {
        // If we're targetting an instance that isn't the DAS, use a different command
        if (target != null && !target.equals(SystemPropertyConstants.DAS_SERVER_NAME)) {
            RenewSelfSignedCertificateLocalInstanceCommand localInstanceCommand =
                    new RenewSelfSignedCertificateLocalInstanceCommand(programOpts, env);
            localInstanceCommand.validate();
            return localInstanceCommand.executeCommand();
        }
        
        
        parseKeyAndTrustStores();

        try {
            store = KeyStore.getInstance(KeyStore.getDefaultType());
            store.load(new FileInputStream(keystore), keystorePassword);
        } catch (KeyStoreException | IOException | CertificateException | NoSuchAlgorithmException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
            return ERROR;
        }

        List<X509Certificate> certificates = findSelfSignedCerts();
        for (X509Certificate cert : certificates) {
            if (renewCertificate(cert) == ERROR) {
                    return ERROR;
            }
        }

        if (reload) {
            restartHttpListeners();
        }

        return SUCCESS;
    }

    private List<X509Certificate> findSelfSignedCerts() {
        ArrayList<X509Certificate> selfSignedCerts = new ArrayList<>();
        try {
            Enumeration<String> aliases = store.aliases();
            while (aliases.hasMoreElements()) {
                X509Certificate cert = (X509Certificate) store.getCertificate(aliases.nextElement());
                if (cert.getIssuerX500Principal().equals(cert.getSubjectX500Principal())) {
                    selfSignedCerts.add(cert);
                }

            }
        } catch (KeyStoreException ex) {
            Logger.getLogger(RenewSelfSignedCertificateCommand.class.getName()).log(Level.SEVERE, null, ex);
        }
        return selfSignedCerts;
    }

    private void addToKeystore(String dname, String alias, File keyStore) throws CommandException {
        // Run keytool command to generate self-signed cert
        KeystoreManager.KeytoolExecutor keytoolExecutor = new KeystoreManager.KeytoolExecutor(
                CertificateManagementKeytoolCommands.constructGenerateCertKeytoolCommand(keyStore, keystorePassword, alias, dname, new String[0]), 60);

        try {
            keytoolExecutor.execute("certNotCreated", keystore);
        } catch (RepositoryException re) {
            logger.severe(re.getCause().getMessage()
                    .replace("keytool error: java.lang.Exception: ", "")
                    .replace("keytool error: java.io.IOException: ", ""));
            throw new CommandException(re);
        }
    }

    private void addToTruststore(String alias) throws CommandException {
        KeystoreManager manager = new KeystoreManager();
        try {
            manager.copyCert(keystore, truststore, alias, new String(masterPassword()));
        } catch (RepositoryException re) {
            logger.severe(re.getCause().getMessage()
                    .replace("keytool error: java.lang.Exception: ", "")
                    .replace("keytool error: java.io.IOException: ", ""));
            throw new CommandException(re);
        }
    }

    private class RenewSelfSignedCertificateLocalInstanceCommand extends AbstractLocalInstanceCertManagementCommand {

        public RenewSelfSignedCertificateLocalInstanceCommand(ProgramOptions programOpts, Environment env) {
            super(programOpts, env);
        }

        @Override
        protected int executeCommand() throws CommandException {
            parseKeyAndTrustStores();

            try {
                store = KeyStore.getInstance(KeyStore.getDefaultType());
                store.load(new FileInputStream(keystore), keystorePassword);
            } catch (KeyStoreException | IOException | CertificateException | NoSuchAlgorithmException ex) {
                LOGGER.log(Level.SEVERE, null, ex);
                return ERROR;
            }

            List<X509Certificate> certificates = findSelfSignedCerts();
            for (X509Certificate cert : certificates) {
                if (renewCertificate(cert) == ERROR) {
                    return ERROR;
                }
            }

            if (reload) {
                restartHttpListeners();
            }

            return SUCCESS;
        }
    }
    
    private int renewCertificate(X509Certificate cert) {
        try {
            String alias = store.getCertificateAlias(cert);
            userArgAlias = alias;
            String dname = cert.getSubjectX500Principal().getName();

            removeFromKeyStore(); //Remove old entry from keystore
            removeFromTrustStore(); //Remove old entry from truststore
            addToKeystore(dname, alias, keystore); //create new entry
            addToTruststore(alias); //Add new entry to truststore
            return SUCCESS;
        } catch (KeyStoreException | CommandException ex) {
            LOGGER.log(Level.SEVERE, "Error renewing certificate", ex);
            return ERROR;
        }
    }

}
