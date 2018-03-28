/*
 * Copyright (c) 2018 Payara Foundation. All rights reserved.
 *
 * The contents of this file are subject to the terms of the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 */
package fish.payara.admin.servermgmt.cli;

import com.sun.enterprise.admin.servermgmt.DomainException;
import com.sun.enterprise.admin.servermgmt.KeystoreManager;
import com.sun.enterprise.admin.servermgmt.cli.LocalDomainCommand;
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
import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.annotations.Service;
import org.glassfish.api.Param;
import org.glassfish.api.admin.CommandException;
import org.glassfish.api.admin.CommandValidationException;

/**
 * Adds a new PKCS#8 encoded plain (unencrypted) RSA keypair to the domain's keystore
 *
 * @author ratcash
 */
@Service(name = "add-pkcs8") // the name of the service is the asadmin command name
@PerLookup // this means one instance is created every time the command is run
public class AddKeypairCommand extends LocalDomainCommand {
	//private static final String DEFAULT_SSL_LISTENER = "http-listener-2";
	public static final String DEFAULT_PASSWORD = "changeit";

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

	private File getKeyStoreFile() throws DomainException, IOException {

		if (getServerDirs() == null) {
			return null;
		}

		File mp = new File(getServerDirs().getConfigDir(), "keystore.jks");
		Logger.getLogger(AddKeypairCommand.class.getName()).log(Level.SEVERE, mp.getAbsolutePath());
		if (!mp.canRead()) {
			return null;
		}
		return mp;
	}

	@Override
	protected void validate()
			throws CommandException, CommandValidationException {
		setDomainName(userArgDomainName);
		super.validate();
	}

	@Override
	protected int executeCommand() throws CommandException, CommandValidationException {

		// TODO get the correct pasword from the master password file
		// using the default, for now
		File privKeyFile = new File(pkcs8PrivateKeyPath);
		try {
			File destKeyStore = getKeyStoreFile();
			try (InputStream privIn = new FileInputStream(privKeyFile)) {
				PrivateKey privKey = keyManager.readPlainPKCS8PrivateKey(privIn, "RSA");
				Collection<? extends Certificate> certChain = keyManager.readPemCertificateChain(new File(certPath));
				keyManager.addKeyPair(destKeyStore, "JKS",
						DEFAULT_PASSWORD.toCharArray(), privKey, certChain.toArray(new Certificate[1]), destAlias);
				logger.fine(() -> MessageFormat.format("Private key with alias [{0}] added to keystore {1}.",
						new Object[] {destAlias, destKeyStore.getAbsolutePath()}));
			} catch (IOException | NoSuchAlgorithmException | KeyStoreException ex) {
				Logger.getLogger(AddKeypairCommand.class.getName()).log(Level.SEVERE, null, ex);
				throw new CommandException(ex.getLocalizedMessage());
			}
		} catch (DomainException | IOException | InvalidKeySpecException ex) {
			throw new CommandException(ex.getLocalizedMessage());
		}

		return 0;
	}
}
