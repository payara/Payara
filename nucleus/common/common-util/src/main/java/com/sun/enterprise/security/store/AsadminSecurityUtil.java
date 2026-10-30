/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2012 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
// Portions Copyright [2018-2022] [Payara Foundation and/or its affiliates]

package com.sun.enterprise.security.store;

import com.sun.enterprise.universal.i18n.LocalStringsImpl;
import com.sun.enterprise.util.CULoggerInfo;
import com.sun.enterprise.util.SystemPropertyConstants;
import java.io.*;
import java.nio.file.Files;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

/**
 * Various utility methods related to certificate-based security.
 * <p>
 * In particular, this class opens both the client-side keystore and the
 * client-side truststore when either one is requested.  This allows us to
 * prompt only once for the master password (if necessary) without storing the
 * password the user responds with which would be a security risk.
 *
 * @author Tim Quinn (with portions refactored from elsewhere)
 */
public class AsadminSecurityUtil {

    private static final File DEFAULT_CLIENT_DIR;

    private static AsadminSecurityUtil instance = null;

    private static final Logger logger = CULoggerInfo.getLogger();


    static {

		/** Patch to make dir $HOME/.gfclient configurable.
		 *
		 * To set a different gfclient directory, add in /payara7/glassfish/config/[asenv.bat/asenv.conf]
		 * the  AS_GFCLIENT variable.
		 * Example:
		 * export AS_GFCLIENT="/var/www/html/payara7/.gfclient"
		 */
		String AS_GFCLIENT = System.getenv("AS_GFCLIENT");
		logger.log(Level.FINER, "AS_GFCLIENT: {0}", AS_GFCLIENT);
		if (AS_GFCLIENT != null) {
    			DEFAULT_CLIENT_DIR = new File(AS_GFCLIENT);
		} else {
    			DEFAULT_CLIENT_DIR = new File(System.getProperty("user.home"), ".gfclient");
		}
		logger.log(Level.FINER, "Set .gfclient directory to: {0}", DEFAULT_CLIENT_DIR);
    }


    /**
     * Returns the usable instance, creating it if needed.
     * @param commandLineMasterPassword password provided via the command line
     * @param isPromptable if the command requiring the object was run by a human who is present to respond to a prompt for the master password
     * @return the usable instance
     */
    public synchronized static AsadminSecurityUtil getInstance(
            final char[] commandLineMasterPassword,
            final boolean isPromptable) {
        if (instance == null) {
            instance = new AsadminSecurityUtil(commandLineMasterPassword, isPromptable);
        }
        return instance;
    }

    /**
     * Returns the usable instance, creating it if needed.
     * @param isPromptable if the command requiring the object was run by a human who is present to respond to a prompt for the master password
     * @return
     */
    public synchronized static AsadminSecurityUtil getInstance(
            final boolean isPromptable) {
        return getInstance(null, isPromptable);
    }


    private AsadminTruststore asadminTruststore = null;

    private KeyStore asadminKeystore = null;

    private static final LocalStringsImpl strmgr = new LocalStringsImpl(AsadminSecurityUtil.class);


    /**
     * Returns the master password for the keystore and truststore, as set by
     * the system property (defaulted if the property is not set).
     * @return
     */
    public static char[] getAsadminTruststorePassword()
    {
        return System.getProperty(SystemPropertyConstants.CLIENT_TRUSTSTORE_PASSWORD_PROPERTY,
            "changeit").toCharArray();
    }

    /**
     * Get the default location for client related files
     */
    public static File getDefaultClientDir() {
        if (!DEFAULT_CLIENT_DIR.isDirectory()) {
            if (DEFAULT_CLIENT_DIR.mkdirs() == false) {
                logger.log(Level.SEVERE, CULoggerInfo.errorCreatingDirectory,
                        DEFAULT_CLIENT_DIR);
                // return the File anyway, the user of the file will report the failure
            }
        }
        return DEFAULT_CLIENT_DIR;
    }

    private AsadminSecurityUtil(final char[] commandLineMasterPassword, final boolean isPromptable) {
        try {
            init(commandLineMasterPassword, isPromptable);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * If we fail to open the client database using the default password
     * (changeit) or the password found in  "javax.net.ssl.trustStorePassword"
     * system property, then the fallback behavior is to prompt the user for
     * the password by calling this method.
     * @return the password to the client side truststore
     */
    private char[] promptForPassword() {
        LineReader lineReader = null;
        try {
            char mask = 0;
            Terminal terminal = TerminalBuilder.builder()
                    .system(true)
                    .build();
            lineReader = LineReaderBuilder.builder()
                    .terminal(terminal).build();

            String line = lineReader.readLine(strmgr.get("certificateDbPrompt"), mask);
            return line.toCharArray();
        } catch (IOException ioe) {
            logger.log(Level.WARNING, "Error reading input", ioe);
        } catch (UserInterruptException | EndOfFileException e) {
                // Ignore
        }finally {
            if (lineReader != null && lineReader.getTerminal() != null) {
                try {
                    lineReader.getTerminal().close();
                } catch (IOException ioe) {
                    logger.log(Level.WARNING, "Error closing terminal", ioe);
                }
            }
        }

        return null;
    }

    /**
     * Returns the opened AsadminTruststore object.
     * @return the AsadminTruststore object
     */
    public AsadminTruststore getAsadminTruststore() {
        return asadminTruststore;
    }

    public KeyStore getAsadminKeystore() {
        return asadminKeystore;
    }

    private void init(final char[] commandLineMasterPassword, final boolean isPromptable)
            throws IOException, KeyStoreException, NoSuchAlgorithmException, CertificateException {
        char[] passwordToUse = chooseMasterPassword(commandLineMasterPassword);
        try {
            /*
             * Open the keystore if the user has specified one using
             * the standard system property.  That would allow users to add a
             * key to a client-side keystore and use SSL client auth from
             * asadmin to the DAS (if they have added the corresponding cert to
             * the DAS truststore).
             */
            asadminKeystore = openKeystore(passwordToUse);
            if (asadminKeystore == null) {
                logger.finer("Skipped loading keystore - location null");
            } else {
                logger.finer("Loaded keystore using command or default master password");
            }
        } catch (IOException ex) {
            if (ex.getCause() instanceof UnrecoverableKeyException) {
                /*
                 * The password did not allow access to the keystore.  Prompt
                 * the user if possible.
                 */
                if ( ! isPromptable) {
                    throw ex;
                }
                passwordToUse = promptForPassword();
                if (passwordToUse == null) {
                    throw new IllegalArgumentException();
                }
                asadminKeystore = openKeystore(passwordToUse);
                logger.finer("Loaded keystore using prompted master password");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        /*
         * The keystore has been opened successfully, using passwordToUse.
         * Open the truststore with that password.
         */
        try {
            asadminTruststore = openTruststore(passwordToUse);
        } catch (IOException e) {
            //If we're using the default trust store try to recreate it, otherwise just throw the exception
            if (System.getProperty(SystemPropertyConstants.CLIENT_TRUSTSTORE_PROPERTY) == null) {
                logger.log(Level.WARNING, String.format("Error when reading truststore, exception:%s. Now recreating file", e));
                recreateDefaultTrustStore(passwordToUse);
            } else {
                throw e;
            }
        }
    }

    /**
     * Method to recreate the default truststore file from the .gfclient folder
     *
     * @param passwordToUse password to use
     * @throws IOException
     */
    protected void recreateDefaultTrustStore(char[] passwordToUse) throws IOException {
        File trustStoreFile = new File(AsadminSecurityUtil.getDefaultClientDir(), "truststore");
        logger.log(Level.INFO, String.format("Recreating default truststore file: %s", trustStoreFile.getPath()));
        try {
            Files.deleteIfExists(trustStoreFile.toPath());
            asadminTruststore = openTruststore(passwordToUse);
        } catch (IOException | KeyStoreException | NoSuchAlgorithmException | CertificateException e) {
            logger.log(Level.WARNING,
                    String.format("Error when processing truststore with path:%s and exception:%s",
                            trustStoreFile.getPath(), e));
            throw new RuntimeException(e);
        }
    }

    private AsadminTruststore openTruststore(final char[] password)
            throws CertificateException, KeyStoreException, NoSuchAlgorithmException, IOException {
        return new AsadminTruststore(password);
    }

    /**
     * Open the keystore, using the password provided.
     * @param candidateMasterPassword password to use in opening the keystore
     * @return opened keystore
     * @throws KeyStoreException
     * @throws IOException
     * @throws NoSuchAlgorithmException
     * @throws CertificateException
     */
    private KeyStore openKeystore(final char[] candidateMasterPassword)
            throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException {
        final KeyStore permanentKS = KeyStore.getInstance("JKS");
        try (InputStream keyStoreStream = asadminKeyStoreStream()) {
            if (keyStoreStream == null) {
                return null;
            }
            permanentKS.load(keyStoreStream, candidateMasterPassword);
            return permanentKS;
        }
    }

    /**
     * Returns the master password passed on the command line or, if none,
     * the default master password.
     * @param commandMasterPassword master password passed on the command line; null if none
     * @return master password to use
     */
    private char[] chooseMasterPassword(final char[] commandMasterPassword) {
        return (commandMasterPassword == null ? defaultMasterPassword() : commandMasterPassword);
    }

    /**
     * Returns an open stream to the keystore.
     * @return stream to the keystore
     * @throws FileNotFoundException
     */
    private InputStream asadminKeyStoreStream() throws FileNotFoundException {
        String location = System.getProperty(SystemPropertyConstants.KEYSTORE_PROPERTY);
        if (location == null) {
            return null;
        }
        return new BufferedInputStream(new FileInputStream(location));
    }

    private char[] defaultMasterPassword() {
        return System.getProperty(SystemPropertyConstants.CLIENT_TRUSTSTORE_PASSWORD_PROPERTY,
            "changeit").toCharArray();
    }

}
