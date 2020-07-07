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
package fish.payara.certificate.management.admin;

import com.sun.enterprise.admin.cli.CLICommand;
import com.sun.enterprise.admin.cli.CLIConstants;
import com.sun.enterprise.admin.cli.Environment;
import com.sun.enterprise.admin.cli.ProgramOptions;
import com.sun.enterprise.util.SystemPropertyConstants;
import org.glassfish.api.admin.CommandException;
import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.annotations.Service;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Command to remove all expired certificates from the target instance or listener's key and trust stores.
 *
 * @author Jonathan Coustick
 * @author Andrew Pielage
 */
@Service(name = "remove-expired-certificates")
@PerLookup
public class RemoveExpiredCertsCommand extends AbstractCertManagementCommand {

    private static final Logger logger = Logger.getLogger(CLICommand.class.getPackage().getName());

    @Override
    protected int executeCommand() throws CommandException {
        // If we're targetting an instance that isn't the DAS, use a different command
        if (target != null && !target.equals(SystemPropertyConstants.DAS_SERVER_NAME)) {
            RemoveExpiredCertsLocalInstanceCommand localInstanceCommand =
                    new RemoveExpiredCertsLocalInstanceCommand(programOpts, env);
            localInstanceCommand.validate();
            return localInstanceCommand.executeCommand();
        }

        // Parse the location of the key and trust stores, and the passwords required to access them
        parseKeyAndTrustStores();

        try {
            // Remove from key store
            KeyStore store = KeyStore.getInstance(KeyStore.getDefaultType());
            store.load(new FileInputStream(keystore), keystorePassword);
            filterExpiredKeys(store);
            save(store);

            // Remove from trust store
            store = KeyStore.getInstance(KeyStore.getDefaultType());
            store.load(new FileInputStream(truststore), truststorePassword);
            filterExpiredKeys(store);
            save(store);
        } catch (KeyStoreException | NoSuchAlgorithmException | CertificateException | IOException ex) {
            throw new CommandException(ex);
        }

        return CLIConstants.SUCCESS;
    }

    private void filterExpiredKeys(KeyStore store) throws KeyStoreException {
        logger.log(Level.INFO, "Removing expired keys.");
        int removedKeys = 0;

        // Count through all aliases
        Enumeration<String> aliases = store.aliases();
        while (aliases.hasMoreElements()) {
            // Get the certificate and alias
            String alias = aliases.nextElement();
            Certificate cert = store.getCertificate(alias);

            // If the certificate is an X509 certificate
            if (cert.getType().equals("X.509")) {
                X509Certificate xCert = (X509Certificate) cert;
                String expiryDate = new SimpleDateFormat("dd/MM/yyyy").format(xCert.getNotAfter());

                logger.log(Level.FINE, "Checking certificate {0} (expires {1}).", new Object[]{alias, expiryDate});
                // Check the certificate validity, and remove it if it's expired.
                try {
                    xCert.checkValidity();
                } catch (CertificateExpiredException | CertificateNotYetValidException e) {
                    store.deleteEntry(alias);
                    logger.log(Level.INFO, "Removed certificate {0} (expired {1}).", new Object[]{alias, expiryDate});
                    removedKeys++;
                }
            }
        }
        logger.log(Level.INFO, "Successfully removed {0} expired keys out of a total {1}.",
                new Object[] { removedKeys, store.size() });
    }

    /**
     * Writes the keystore back to the given file.
     *
     * @throws KeyStoreException if the keystore has not been initialized (loaded).
     * @throws IOException if there was an I/O problem with data.
     * @throws NoSuchAlgorithmException if the appropriate data integrity algorithm could not be found.
     * @throws CertificateException if any of the certificates included in the keystore data could not be stored.
     */
    private void save(KeyStore store) throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
        logger.log(Level.INFO, "Writing keystore to file: {0}.", keystore.getAbsolutePath());
        try (FileOutputStream out = new FileOutputStream(keystore)) {
            store.store(out, keystorePassword);
            out.flush();
        }
        logger.log(Level.INFO, "Keystore written successfully.", keystore.getAbsolutePath());
    }



    private class RemoveExpiredCertsLocalInstanceCommand extends AbstractLocalInstanceCertManagementCommand {

        public RemoveExpiredCertsLocalInstanceCommand(ProgramOptions programOpts, Environment env) {
            super(programOpts, env);
        }

        @Override
        protected int executeCommand() throws CommandException {
            parseKeyAndTrustStores();

            // If the target is not the DAS and is configured to use the default key or trust store, sync with the
            // DAS instead
            if (checkDefaultKeyOrTrustStore()) {
                logger.warning("The target instance is using the default key or trust store, any changes"
                        + " made directly to instance stores would be lost upon next sync.");

                if (!alreadySynced) {
                    logger.warning("Syncing with the DAS...");
                    synchronizeInstance();
                }

                if (defaultKeystore && defaultTruststore) {
                    // Do nothing
                } else if (defaultKeystore) {
                    logger.info("Please remove expired certificates from truststore manually using the remove-from-truststore command");
                } else {
                    logger.info("Please remove expired certificates from keystore manually using the remove-from-keystore command");
                }

                return CLIConstants.WARNING;
            }

            try {
                // Remove from key store
                KeyStore store = KeyStore.getInstance(KeyStore.getDefaultType());
                store.load(new FileInputStream(keystore), keystorePassword);
                filterExpiredKeys(store);
                save(store);

                // Remove from trust store
                store = KeyStore.getInstance(KeyStore.getDefaultType());
                store.load(new FileInputStream(truststore), truststorePassword);
                filterExpiredKeys(store);
                save(store);
            } catch (KeyStoreException | NoSuchAlgorithmException | CertificateException | IOException ex) {
                throw new CommandException(ex);
            }

            return CLIConstants.SUCCESS;
        }

    }
}
