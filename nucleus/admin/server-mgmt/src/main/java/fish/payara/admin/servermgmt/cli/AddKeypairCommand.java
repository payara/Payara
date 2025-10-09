/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *    Copyright (c) 2018-2023 Payara Foundation and/or its affiliates. All rights reserved.
 * 
 *     The contents of this file are subject to the terms of either the GNU
 *     General Public License Version 2 only ("GPL") or the Common Development
 *     and Distribution License("CDDL") (collectively, the "License").  You
 *     may not use this file except in compliance with the License.  You can
 *     obtain a copy of the License at
 *     https://github.com/payara/Payara/blob/main/LICENSE.txt
 *     See the License for the specific
 *     language governing permissions and limitations under the License.
 * 
 *     When distributing the software, include this License Header Notice in each
 *     file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
 * 
 *     GPL Classpath Exception:
 *     The Payara Foundation designates this particular file as subject to the "Classpath"
 *     exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *     file that accompanied this code.
 * 
 *     Modifications:
 *     If applicable, add the following below the License Header, with the fields
 *     enclosed by brackets [] replaced by your own identifying information:
 *     "Portions Copyright [year] [name of copyright owner]"
 * 
 *     Contributor(s):
 *     If you wish your version of this file to be governed by only the CDDL or
 *     only the GPL Version 2, indicate your decision by adding "[Contributor]
 *     elects to include this software in this distribution under the [CDDL or GPL
 *     Version 2] license."  If you don't indicate a single choice of license, a
 *     recipient has the option to distribute your version of this file under
 *     either the CDDL, the GPL Version 2 or to extend the choice of license to
 *     its licensees as provided above.  However, if you add GPL Version 2 code
 *     and therefore, elected the GPL Version 2 license, then the option applies
 *     only if the new code is made subject to such option by the copyright
 *     holder.
 */
package fish.payara.admin.servermgmt.cli;

import com.sun.enterprise.admin.servermgmt.KeystoreManager;
import com.sun.enterprise.admin.servermgmt.cli.LocalDomainCommand;
import org.glassfish.api.Param;
import org.glassfish.api.admin.CommandException;
import org.glassfish.api.admin.CommandValidationException;
import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.annotations.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.spec.InvalidKeySpecException;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Adds a new PKCS#8 encoded plain (unencrypted) RSA keypair to the domain's keystore
 *
 * @author ratcash
 */
@Service(name = "add-pkcs8") // the name of the service is the asadmin command name
@PerLookup // this means one instance is created every time the command is run
public class AddKeypairCommand extends LocalDomainCommand {

	@Param(name = "domain_name", optional = true)
	String userArgDomainName;

	@Param(optional = false)
	String destAlias;

	@Param(name = "priv-key-path", optional = false)
	String pkcs8PrivateKeyPath;

	@Param(name = "cert-chain-path", optional = false)
	String certPath;

	//@Inject
	KeystoreManager keyManager = new KeystoreManager();

	@Override
	protected void validate()
			throws CommandException, CommandValidationException {
		setDomainName(userArgDomainName);
		super.validate();
	}

	@Override
	protected int executeCommand() throws CommandException, CommandValidationException {

		File privKeyFile = new File(pkcs8PrivateKeyPath);
		try {
			File destKeyStore = getKeyStoreFile();
			try (InputStream privIn = new FileInputStream(privKeyFile)) {
				PrivateKey privKey = keyManager.readPlainPKCS8PrivateKey(privIn, "RSA");
				Collection<? extends Certificate> certChain = keyManager.readPemCertificateChain(new File(certPath));
				String mp = getMasterPassword();
				keyManager.addKeyPair(destKeyStore, "JKS",
						mp.toCharArray(), privKey, certChain.toArray(new Certificate[1]), destAlias);
				logger.fine(() -> MessageFormat.format("Private key with alias [{0}] added to keystore {1}.",
						new Object[]{destAlias, destKeyStore.getAbsolutePath()}));
			} catch (IOException | NoSuchAlgorithmException | KeyStoreException ex) {
				Logger.getLogger(AddKeypairCommand.class.getName()).log(Level.SEVERE, null, ex);
				throw new CommandException(ex.getLocalizedMessage());
			}
		} catch (InvalidKeySpecException ex) {
			throw new CommandException(ex.getLocalizedMessage());
		}

		return 0;
	}
}
