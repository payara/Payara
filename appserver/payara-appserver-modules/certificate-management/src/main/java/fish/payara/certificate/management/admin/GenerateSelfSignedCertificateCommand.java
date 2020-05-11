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
import com.sun.enterprise.admin.cli.remote.RemoteCLICommand;
import com.sun.enterprise.admin.servermgmt.KeystoreManager;
import com.sun.enterprise.admin.servermgmt.RepositoryException;
import com.sun.enterprise.admin.servermgmt.cli.LocalDomainCommand;
import com.sun.enterprise.universal.xml.MiniXmlParser;
import com.sun.enterprise.universal.xml.MiniXmlParserException;
import com.sun.enterprise.util.SystemPropertyConstants;
import fish.payara.certificate.management.CertificateManagementUtils;
import org.glassfish.api.Param;
import org.glassfish.api.admin.CommandException;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.internal.api.Globals;
import org.jvnet.hk2.annotations.Service;

import java.io.File;
import java.util.logging.Logger;

@Service(name = "generate-self-signed-certificate")
@PerLookup
public class GenerateSelfSignedCertificateCommand extends LocalDomainCommand {

    private static final Logger logger = Logger.getLogger(CLICommand.class.getPackage().getName());
    private static final String helperCommandName = "_generate-self-signed-certificate-local-instance";

    @Param(name = "domain_name", optional = true)
    private String domainName0;

    @Param(name = "distinguishedname", alias = "dn")
    private String dn;

    @Param(name = "alternativenames", optional = true, alias = "altnames", separator = ';')
    private String[] altnames;

    @Param(name = "listener", optional = true)
    private String listener;

    @Param(name = "target", optional = true, defaultValue = SystemPropertyConstants.DAS_SERVER_NAME)
    private String target;

    @Param(name = "reload", optional = true)
    private boolean reload;

    @Param(name = "alias", primary = true)
    private String alias;

    private File keystore;
    private File truststore;

    @Override
    protected void validate() throws CommandException {
        setDomainName(domainName0);
        super.validate();
    }

    @Override
    protected int executeCommand() throws CommandException {
        // If we're targetting an instance that isn't the DAS, use a different command
        if (target != null && !target.equals(SystemPropertyConstants.DAS_SERVER_NAME)) {
//            RemoteCLICommand helperCommand = new RemoteCLICommand(helperCommandName, programOpts, env);

            String altnamesString = "";
            if (altnames != null) {
                for (String altname : altnames) {
                    altnamesString += altname + ";";
                }
                altnamesString = altnamesString.substring(0, altnamesString.length() - 1);
            }

//            return helperCommand.execute(helperCommandName,
//                    "--distinguishedname", dn,
//                    "--alternativenames", altnamesString,
//                    "--listener", listener,
//                    "--instance_name", target,
//                    "--reload", String.valueOf(reload),
//                    "--alias", alias);

            GenerateSelfSignedCertificateLocalInstanceCommand helperCommand = Globals.getDefaultBaseServiceLocator().getService(GenerateSelfSignedCertificateLocalInstanceCommand.class);



            helperCommand.executeCommand();




        }

        String password = "";
        try {
            MiniXmlParser parser = new MiniXmlParser(getDomainXml(), target);
            keystore = CertificateManagementUtils.resolveKeyStore(parser, listener, getDomainRootDir());
            truststore = CertificateManagementUtils.resolveTrustStore(parser, listener, getDomainRootDir());
            password = getPassword(parser, listener);
        } catch (MiniXmlParserException miniXmlParserException) {
            throw new CommandException("Error parsing domain.xml", miniXmlParserException);
        }

        // Run keytool command to generate self-signed cert and place in keystore
        KeystoreManager.KeytoolExecutor keytoolExecutor = new KeystoreManager.KeytoolExecutor(
                CertificateManagementUtils.constructGenerateCertKeytoolCommand(
                        keystore, password, alias, dn, altnames),
                60);
        try {
            keytoolExecutor.execute("certNotCreated", keystore);
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

    private String getPassword(MiniXmlParser parser, String listener) throws MiniXmlParserException, CommandException {
        String password = "";
        if (listener != null) {
            // Check if listener has a password set
            password = CertificateManagementUtils.getPasswordFromListener(parser, listener);
        }

        // Default to using the master password
        if (!ok(password)) {
            password = getMasterPassword();
        }

        return password;
    }
}