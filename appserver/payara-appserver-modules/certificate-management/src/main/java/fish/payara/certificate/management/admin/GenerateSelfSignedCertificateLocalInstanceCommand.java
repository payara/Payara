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
import com.sun.enterprise.admin.cli.cluster.LocalInstanceCommand;
import com.sun.enterprise.admin.cli.remote.RemoteCLICommand;
import com.sun.enterprise.admin.servermgmt.KeystoreManager;
import com.sun.enterprise.admin.servermgmt.RepositoryException;
import com.sun.enterprise.universal.xml.MiniXmlParser;
import com.sun.enterprise.universal.xml.MiniXmlParserException;
import com.sun.enterprise.util.SystemPropertyConstants;
import com.sun.enterprise.util.cluster.SyncRequest;
import com.sun.enterprise.util.io.FileUtils;
import fish.payara.certificate.management.CertificateManagementUtils;
import org.glassfish.api.Param;
import org.glassfish.api.admin.CommandException;
import org.glassfish.common.util.admin.AuthTokenManager;
import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.annotations.Service;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.PropertyException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

@Service(name = "_generate-self-signed-certificate-local-instance")
@PerLookup
public class GenerateSelfSignedCertificateLocalInstanceCommand extends LocalInstanceCommand {

    private static final Logger logger = Logger.getLogger(CLICommand.class.getPackage().getName());

    @Param(name = "distinguishedname", alias = "dn")
    private String dn;

    @Param(name = "alternativenames", optional = true, alias = "altnames", separator = ';')
    private String[] altnames;

    @Param(name = "listener", optional = true)
    private String listener;

    @Param(name = "reload", optional = true)
    private boolean reload;

    @Param(name = "alias")
    private String alias;

    @Param(name = "instance_name", primary = true, optional = true)
    private String instanceName0;

    private File keystore;
    private File truststore;

    @Override
    protected void validate() throws CommandException {
        if (ok(instanceName0))
            instanceName = instanceName0;
        super.validate();
    }

    @Override
    protected int executeCommand() throws CommandException {
        String password = "";

        try {
            MiniXmlParser parser = new MiniXmlParser(getDomainXml(), instanceName0);
            keystore = CertificateManagementUtils.resolveKeyStore(parser, listener, instanceDir);
            truststore = CertificateManagementUtils.resolveTrustStore(parser, listener, instanceDir);
            password = getPassword(parser, listener);
        } catch (MiniXmlParserException miniXmlParserException) {
            throw new CommandException("Error parsing domain.xml", miniXmlParserException);
        }

        // If the target is not the DAS and is configured to use the default key or trust store, sync with the
        // DAS instead
        boolean defaultKeystore = keystore.getAbsolutePath()
                .equals(CertificateManagementUtils.DEFAULT_KEYSTORE
                        .replace("${com.sun.aas.instanceRoot}", instanceDir.getAbsolutePath()));
        boolean defaultTruststore = truststore.getAbsolutePath()
                .equals(CertificateManagementUtils.DEFAULT_TRUSTSTORE
                        .replace("${com.sun.aas.instanceRoot}", instanceDir.getAbsolutePath()));

        if (defaultKeystore || defaultTruststore) {
            logger.warning("The target instance is using the default key or trust store, any new certificates"
                    + " added directly to instance stores would be lost upon next sync.");
            if (reload) {
                logger.warning("Syncing with the DAS instead of generating a new certificate");
                synchroniseInstance(defaultKeystore, defaultTruststore);

                // Reload Keystore and Truststores
                // TO-DO

                return CLIConstants.WARNING;
            } else {
                logger.info("Skipping sync with the DAS since --reload wasn't enabled");
                return CLIConstants.WARNING;
            }
        }


        // Run keytool command to generate self-signed cert
        KeystoreManager.KeytoolExecutor keytoolExecutor = new KeystoreManager.KeytoolExecutor(
                CertificateManagementUtils.constructGenerateCertKeytoolCommand(keystore, password, alias, dn, altnames),
                60);
        try {
            keytoolExecutor.execute("keystoreNotCreated", keystore);
        } catch (RepositoryException re) {
            logger.severe(re.getCause().getMessage()
                    .replace("keytool error: java.lang.Exception: ", "")
                    .replace("keytool error: java.io.IOException: ", ""));
            return CLIConstants.ERROR;
        }

        // Run keytool command to place self-signed cert in truststore
        keytoolExecutor = new KeystoreManager.KeytoolExecutor(CertificateManagementUtils.constructImportCertKeytoolCommand(
                keystore, truststore, password, alias),
                60);

        try {
            keytoolExecutor.execute("certNotTrusted", keystore);
        } catch (RepositoryException re) {
            logger.severe(re.getCause().getMessage()
                    .replace("keytool error: java.lang.Exception: ", "")
                    .replace("keytool error: java.io.IOException: ", ""));
            return CLIConstants.ERROR;
        }


        return 0;
    }

    private void synchroniseInstance(boolean defaultKeystore, boolean defaultTruststore) throws CommandException {
        // Because we reuse the command, we also need to reuse the auth token (if one is present).
        final String origAuthToken = programOpts.getAuthToken();
        if (origAuthToken != null) {
            programOpts.setAuthToken(AuthTokenManager.markTokenForReuse(origAuthToken));
        }

        try {
            RemoteCLICommand syncCmd = new RemoteCLICommand("_synchronize-files", programOpts, env);

            File tempFile = File.createTempFile("mt.", ".xml");
            FileUtils.deleteOnExit(tempFile);
            JAXBContext context = JAXBContext.newInstance(SyncRequest.class);

            Marshaller marshaller = context.createMarshaller();
            marshaller.setProperty("jaxb.formatted.output", Boolean.TRUE);

            if (defaultKeystore && defaultTruststore) {
                List<File> filesToSync = new ArrayList<>();
                filesToSync.add(keystore);
                filesToSync.add(truststore);
                marshaller.marshal(createSyncRequest(filesToSync), tempFile);
            } else if (defaultKeystore) {
                marshaller.marshal(createSyncRequest(keystore), tempFile);
            } else if (defaultTruststore) {
                marshaller.marshal(createSyncRequest(truststore), tempFile);
            }

            syncCmd.executeAndReturnOutput("_synchronize-files", "--upload=true", tempFile.getPath());
        } catch (IOException e) {
            e.printStackTrace();
        } catch (PropertyException e) {
            e.printStackTrace();
        } catch (JAXBException e) {
            e.printStackTrace();
        }

    }

    private SyncRequest createSyncRequest(File fileToSync) {
        List<File> filesToSync = new ArrayList<>();
        filesToSync.add(fileToSync);
        return createSyncRequest(filesToSync);
    }

    private SyncRequest createSyncRequest(List<File> filesToSync) {
        SyncRequest syncRequest = new SyncRequest();
        syncRequest.instance = instanceName0;
        syncRequest.dir = "config";

        for (File fileToSync : filesToSync) {
            syncRequest.files.add(new SyncRequest.ModTime(new File(instanceDir + File.separator + "config").toURI()
                    .relativize(fileToSync.toURI()).getPath(), System.currentTimeMillis()));
        }

        return syncRequest;
    }

    private String getPassword(MiniXmlParser parser, String listener) throws MiniXmlParserException, CommandException {
        String password = "";
        if (listener != null) {
            // Check if listener has a password set
            password = CertificateManagementUtils.getPasswordFromListener(parser, listener);
        }

        // Default to using the master password
        if (ok(password)) {
            password = getMasterPassword();
        }

        return password;
    }
}
