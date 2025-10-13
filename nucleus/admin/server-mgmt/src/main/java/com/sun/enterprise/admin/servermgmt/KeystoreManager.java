/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2013 Oracle and/or its affiliates. All rights reserved.
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
 *
 * Portions Copyright [2018-2023] [Payara Foundation and/or its affiliates]
 */
package com.sun.enterprise.admin.servermgmt;

import com.sun.enterprise.admin.servermgmt.pe.PEFileLayout;
import com.sun.enterprise.security.auth.realm.certificate.OID;
import com.sun.enterprise.universal.glassfish.ASenvPropertyReader;
import com.sun.enterprise.universal.io.SmartFile;
import com.sun.enterprise.universal.process.ProcessUtils;
import com.sun.enterprise.util.ExecException;
import com.sun.enterprise.util.OS;
import com.sun.enterprise.util.ProcessExecutor;
import com.sun.enterprise.util.SystemPropertyConstants;
import com.sun.enterprise.util.i18n.StringManager;
import com.sun.enterprise.util.net.NetUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.sun.enterprise.admin.servermgmt.SLogger.BAD_DELETE_TEMP_CERT_FILE;
import static com.sun.enterprise.admin.servermgmt.SLogger.UNHANDLED_EXCEPTION;
import static com.sun.enterprise.admin.servermgmt.SLogger.getLogger;

/**
 * @author kebbs
 */
public class KeystoreManager {

    private static final String KEYTOOL_CMD;
    private static final String KEYTOOL_EXE_NAME = OS.isWindows() ? "keytool.exe" : "keytool";
    private static final String CERTIFICATE_DN_PREFIX = "CN=";
    private static final String CERTIFICATE_DN_SUFFIX = ",OU=Payara,O=Payara Foundation,L=Great Malvern,ST=Worcestershire,C=UK";
    public static final String CERTIFICATE_ALIAS = "s1as";
    public static final String INSTANCE_SECURE_ADMIN_ALIAS = "glassfish-instance";
    public static final String DEFAULT_MASTER_PASSWORD = "changeit";
    private static final String SKID_EXTENSION_SYSTEM_PROPERTY = "-J-Dsun.security.internal.keytool.skid";
    private static final String INSTANCE_CN_SUFFIX = "-instance";

    private static final StringManager STRING_MANAGER = StringManager.getManager(KeystoreManager.class);

    protected PEFileLayout _fileLayout = null;


    static {
        // Byron Nevins, July 2011
        String nonFinalKeyTool = KEYTOOL_EXE_NAME; // at the end we set the final
        String propName = SystemPropertyConstants.JAVA_ROOT_PROPERTY;
        String javaroot = new ASenvPropertyReader().getProps().get(propName);
        File k = new File(new File(javaroot, "bin"), KEYTOOL_EXE_NAME);

        if (k.canExecute()) {
            nonFinalKeyTool = SmartFile.sanitize(k.getPath());
        } else {
            // can't find it in a JDK. Maybe it is in the PATH?
            k = ProcessUtils.getExe(KEYTOOL_EXE_NAME);

            if (k != null && k.canExecute()) {
                nonFinalKeyTool = k.getPath();
            }
        }
        KEYTOOL_CMD = nonFinalKeyTool;
    }

    public static class KeytoolExecutor extends ProcessExecutor {

        public KeytoolExecutor(String[] args, long timeoutInSeconds) {
            super(args, timeoutInSeconds);
            setExecutionRetentionFlag(true);
            addKeytoolCommand();
        }

        public KeytoolExecutor(String[] args, long timeoutInSeconds, String[] inputLines) {
            super(args, timeoutInSeconds, inputLines);
            setExecutionRetentionFlag(true);
            addKeytoolCommand();
        }

        // We must override this message so that the stdout appears in the exec exception.
        // Keytool seems to output errors to stdout.
        @Override
        protected String getExceptionMessage() {
            return getLatestOutput(mOutFile) + " " + getFileBuffer(mErrFile);
        }

        private void addKeytoolCommand() {
            if (!mCmdStrings[0].equals(KEYTOOL_CMD)) {
                String[] newArgs = new String[mCmdStrings.length + 1];
                newArgs[0] = KEYTOOL_CMD;
                System.arraycopy(mCmdStrings, 0, newArgs, 1, mCmdStrings.length);
                mCmdStrings = newArgs;
            }
        }

        public void execute(String keystoreErrorMsg, File keystoreName) throws RepositoryException {
            try {
                super.execute();
                if (getProcessExitValue() != 0) {
                    throw new RepositoryException(
                            STRING_MANAGER.getString(keystoreErrorMsg, keystoreName) + getLastExecutionError() + " " + getLastExecutionOutput());
                }
            } catch (ExecException ex) {
                throw new RepositoryException(
                        STRING_MANAGER.getString(keystoreErrorMsg, keystoreName) + getLastExecutionError() + " " + getLastExecutionOutput(), ex);
            }
        }
    }

    protected static String getCertificateDN(RepositoryConfig cfg, final String CNSuffix) {
        String cn = getCNFromCfg(cfg);
        if (cn == null) {
            try {
                cn = NetUtils.getCanonicalHostName();
            } catch (Exception e) {
                cn = "localhost";
            }
        }

        /*
         * Use the suffix, if provided, in creating the DN (by augmenting the CN).
         */
        return CERTIFICATE_DN_PREFIX + cn + (CNSuffix != null ? CNSuffix : "") + CERTIFICATE_DN_SUFFIX;// must be of form "CN=..., OU=..."
    }

    protected PEFileLayout getFileLayout(RepositoryConfig config) {
        if (_fileLayout == null) {
            _fileLayout = new PEFileLayout(config);
        }

        return _fileLayout;
    }

    /**
     * Create the default SSL key store using keytool to generate a self signed certificate.
     *
     * @param keystore
     * @param config
     * @param masterPassword
     * @throws RepositoryException
     */
    protected void createKeyStore(File keystore, RepositoryConfig config, String masterPassword) throws RepositoryException {
        // Generate a new self signed certificate with s1as as the alias
        // Create the default self signed cert
        final String dasCertDN = getDASCertDN(config);
        getLogger().log(Level.INFO, STRING_MANAGER.getString("CertificateDN", dasCertDN));
        addSelfSignedCertToKeyStore(keystore, CERTIFICATE_ALIAS, masterPassword, dasCertDN);

        // Create the default self-signed cert for instances to use for SSL auth.
        final String instanceCertDN = getInstanceCertDN(config);
        getLogger().log(Level.INFO,STRING_MANAGER.getString("CertificateDN", instanceCertDN));
        addSelfSignedCertToKeyStore(keystore, INSTANCE_SECURE_ADMIN_ALIAS, masterPassword, instanceCertDN);
    }

    private void addSelfSignedCertToKeyStore(final File keystore, final String alias, final String masterPassword, final String dn)
            throws RepositoryException {
        final String[] keytoolCmd = { "-genkey", "-keyalg", "RSA", "-keystore", keystore.getAbsolutePath(), "-alias", alias, "-dname", dn,
                "-validity", "3650", "-keypass", masterPassword, "-storepass", masterPassword, SKID_EXTENSION_SYSTEM_PROPERTY };

        KeytoolExecutor p = new KeytoolExecutor(keytoolCmd, 60);
        p.execute("keystoreNotCreated", keystore);
    }
    /*
     * protected void addToAsadminTrustStore( RepositoryConfig config, File certFile) throws RepositoryException { boolean
     * newTruststore = false; final PEFileLayout layout = getFileLayout(config); //import the newly created certificate into
     * the asadmin truststore final File asadminTruststore = AsadminTruststore.getAsadminTruststore();
     *
     * if (!asadminTruststore.exists()) { newTruststore = true; }
     *
     * //The keystore alias name is the repository name. We want to avoid alias //name conflicts since multiple domains are
     * likely to live on the same //machine. String aliasName = layout.getRepositoryDir().getAbsolutePath();
     *
     * //first delete the alias in case it already exists. This can happen for //example if a domain is created, deleted,
     * and re-created again. String[] keytoolCmd = new String[] { "-delete", "-keystore",
     * asadminTruststore.getAbsolutePath(), "-alias", aliasName, };
     *
     * final String[] input = {AsadminTruststore.getAsadminTruststorePassword(),
     * AsadminTruststore.getAsadminTruststorePassword()}; // twice in case we are creating KeytoolExecutor p = new
     * KeytoolExecutor(keytoolCmd, 30, input); try { p.execute("trustStoreNotCreated", asadminTruststore); } catch
     * (RepositoryException ex) { //ignore all exceptions. The alias most likely does not exist. }
     *
     * keytoolCmd = new String[] { "-import", "-noprompt", "-keystore", asadminTruststore.getAbsolutePath(), "-alias",
     * aliasName, //alias is the domain name "-file", certFile.getAbsolutePath(), };
     *
     * p = new KeytoolExecutor(keytoolCmd, 30, input); p.execute("trustStoreNotCreated", asadminTruststore);
     *
     * //If this is a newly created truststore, lock it down. if (newTruststore) { try { chmod("600", asadminTruststore); }
     * catch (IOException ex) { throw new RepositoryException(STRING_MANAGER.getString( "trustStoreNotCreated", asadminTruststore),
     * ex); } } }
     */

    /**
     * Copy certain certificates from the keystore into the truststore.
     * @param keyStore keystore to copy from
     * @param trustStore the truststore to copy to
     * @param config the domain's configuration
     * @param masterPassword the master password for the truststore
     * @throws DomainException if an error occured
     */
    protected void copyCertificates(File keyStore, File trustStore, DomainConfig config, String masterPassword) throws DomainException {
        try {
            copyCert(keyStore, trustStore, CERTIFICATE_ALIAS, masterPassword);
            copyCert(keyStore, trustStore, INSTANCE_SECURE_ADMIN_ALIAS, masterPassword);
        } catch (RepositoryException re) {
            String msg = STRING_MANAGER.getString("SomeProblemWithKeytool", re.getMessage());
            throw new DomainException(msg);
        }
    }

    /**
     * Cleans the given truststore of invalid certs and copies all valid certificates 
     * from the currently used JDK to the given trust store.
     *
     * @param trustStore the trust store to copy the certificates to.
     * @param masterPassword the password to the trust store.
     * @throws RepositoryException if an error occured a {@link RepositoryException} will wrap the original exception
     */
    protected void updateCertificates(File trustStore, String masterPassword) throws RepositoryException {
        // Gets the location of the JDK trust store.
        String javaHome = System.getProperty("java.home").concat("/").replaceAll("//", "/");
        String jreHome;
        if (Files.exists(Paths.get(javaHome , "jre/"))) {
            jreHome = javaHome + "jre/";
        } else {
            jreHome = javaHome;
        }
        String javaTrustStoreLocation = jreHome + "lib/security/";
        File javaTrustStoreFile = new File(javaTrustStoreLocation, "cacerts");

        // Load the java trust store
        KeyStore javaTrustStore;
        KeyStore destTrustStore;
        try (FileInputStream javaIn = new FileInputStream(javaTrustStoreFile); FileInputStream destIn = new FileInputStream(trustStore)) {

            javaTrustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            javaTrustStore.load(javaIn, DEFAULT_MASTER_PASSWORD.toCharArray());

            destTrustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            destTrustStore.load(destIn, masterPassword.toCharArray());
        } catch (KeyStoreException ex) {
            throw new RepositoryException("Unable to create Keystore object.", ex);
        } catch (NoSuchAlgorithmException ex) {
            throw new RepositoryException("Unable to read Keystore file.", ex);
        } catch (CertificateException ex) {
            throw new RepositoryException("Unable to load certificate from Keystore instance.", ex);
        } catch (FileNotFoundException ex) {
            throw new RepositoryException("Unable to find Keystore file.", ex);
        } catch (IOException ex) {
            throw new RepositoryException("Unexpected exception reading Keystore file.", ex);
        }

        removeExpiredCerts(destTrustStore);
        
        // Load the valid certificates to the store
        Map<String, Certificate> validCerts = getValidCertificates(javaTrustStore);
        try {
            for (Entry<String,Certificate> alias : validCerts.entrySet()) {
                Certificate cert = alias.getValue();
                if (!destTrustStore.containsAlias(alias.getKey())) {
                    destTrustStore.setCertificateEntry(alias.getKey(), cert);
                }
            }
        } catch (KeyStoreException ex) {
            throw new RepositoryException("Keystore hasn't been initialized.", ex);
        }

        // Load the store back to the initial file
        try (FileOutputStream out = new FileOutputStream(trustStore)) {
            destTrustStore.store(out, masterPassword.toCharArray());
            out.flush();
        } catch (KeyStoreException | NoSuchAlgorithmException | CertificateException | IOException ex) {
            throw new RepositoryException("Unexpected exception writing certificates to the Keystore file.", ex);
        }
    }
    
    private void removeExpiredCerts(KeyStore store) throws RepositoryException {
        
        Map<String, Certificate> invalidCerts = getInvalidCertificates(store);
        for(Entry<String,Certificate> alias : invalidCerts.entrySet()){
            try {
                store.deleteEntry(alias.getKey());
            } catch (KeyStoreException ex) {
                throw new RepositoryException("Could not delete invalid cert", ex);
            }
        }
        
    }
    
    /**
     * Gets the valid certs from the given KeyStore as a map by their alias
     * @param keyStore the KeyStore to get the certs from
     * @return map of valid certs. Key is alias of the cert
     * @throws RepositoryException 
     */
    protected Map<String, Certificate> getValidCertificates(KeyStore keyStore) throws RepositoryException {
        return getCertificates(keyStore, true);
    }
    
    /**
     * Gets the invalid certs from the given KeyStore as a map by their alias
     * @param keyStore the KeyStore to get the certs from
     * @return map of invalid certs. Kkey is alias of the cert
     * @throws RepositoryException 
     */
    protected Map<String, Certificate> getInvalidCertificates(KeyStore keyStore) throws RepositoryException {
        return getCertificates(keyStore, false);
    }
    
    private  Map<String, Certificate> getCertificates(KeyStore keyStore, boolean returnValidCerts) throws RepositoryException {

        Map<String, Certificate> certs = new HashMap<>();

        try {
            for (String alias : Collections.list(keyStore.aliases())) {
                Certificate cert = keyStore.getCertificate(alias);
                if (cert.getType().equals("X.509")) {
                    X509Certificate xCert = (X509Certificate) cert;
                    try {
                        xCert.checkValidity();
                        if(returnValidCerts) {
                            certs.put(alias, cert);
                        }
                    } catch (CertificateExpiredException | CertificateNotYetValidException e) {
                        if(!returnValidCerts) { // return invalid
                            certs.put(alias, cert);
                        }
                    }
                }
            }
        } catch (KeyStoreException ex) {
            throw new RepositoryException("Keystore hasn't been initialized.", ex);
        }

        return certs;
    }
    
    

    /**
     * Copies a certificate from a keyStore to a trustStore.
     *
     * @param keyStore the key store to copy the certificate from.
     * @param trustStore the trust store to copy the certificate to.
     * @param alias the name of the certificate to copy.
     * @param masterPassword the password for the trust stores.
     */
    public void copyCert(final File keyStore, final File trustStore, final String alias, final String masterPassword)
            throws RepositoryException {

        File certFile = null;
        String[] input = { masterPassword };
        String[] keytoolCmd = null;
        KeytoolExecutor p = null;

        try {
            // export the newly created certificate from the first keystore
            certFile = new File(keyStore.getParentFile(), alias + ".cer");
            keytoolCmd = new String[] { "-export", "-keystore", keyStore.getAbsolutePath(), "-alias", alias, "-file",
                    certFile.getAbsolutePath(), };

            p = new KeytoolExecutor(keytoolCmd, 30, input);
            p.execute("trustStoreNotCreated", trustStore);

            // import the newly created certificate into the truststore
            keytoolCmd = new String[] { "-import", "-noprompt", "-keystore", trustStore.getAbsolutePath(), "-alias", alias, "-file",
                    certFile.getAbsolutePath(), };

            p = new KeytoolExecutor(keytoolCmd, 30, input);
            p.execute("trustStoreNotCreated", trustStore);

            // import the newly created certificate into the asadmin truststore
            /* commented out till asadmintruststore can be added back */
            // addToAsadminTrustStore(config, certFile);

        } finally {
            if (certFile != null) {
                final boolean isCertFileDeleted = certFile.delete();
                if (!isCertFileDeleted) {
                    getLogger().log(Level.WARNING, BAD_DELETE_TEMP_CERT_FILE, certFile.getAbsolutePath());
                }
            }
        }
    }

    /**
     * Throws an IllegalArgumentException if the password's complexity does not meet requirements
     *
     * @param pw
     * @param msgId
     */
    protected void enforcePasswordComplexity(char[] pw, String msgId) {
        if (pw == null || pw.length < 6) {
            throw new IllegalArgumentException(STRING_MANAGER.getString(msgId));
        }
    }

    /**
     * Loads a (JKS or PKCS#12) keystore. This method does not use the keytool, but instead the JAVA API
     *
     * @param source the path of the file to be opened and loaded into the keystore
     * @param storeType the type of the keystore to be read
     * @param pw the keystore password
     * @return the keystore, if load was successful
     * @throws KeyStoreException
     */
    public KeyStore openKeyStore(File source, String storeType, char[] pw) throws KeyStoreException {
        KeyStore keyStore = KeyStore.getInstance(storeType);
        try (InputStream keyStoreStream = new FileInputStream(source)) {
            keyStore.load(keyStoreStream, pw);
        } catch (Exception ex) {
            throw new KeyStoreException(ex);
        }
        return keyStore;
    }

    /**
     * Saves the (modified) keystore. This method does not use the keytool, but instead the JAVA API
     *
     * @param keyStore the keystore to be written
     * @param dest path of the file the keystore is to be written to
     * @param pw keystore password
     * @throws KeyStoreException
     */
    public void saveKeyStore(KeyStore keyStore, File dest, char[] pw) throws KeyStoreException {
        enforcePasswordComplexity(pw, "invalidPassword");
        try (OutputStream outStream = new FileOutputStream(dest)) {
            keyStore.store(outStream, pw);
            outStream.flush();
        } catch (Exception ex) {
            throw new KeyStoreException(ex);
        }
    }

    /**
     * Adds/updates a keypair to a keystore. This method does not use the keytool, but instead the JAVA API
     *
     * @param keyStore the keystore. Must not be null.
     * @param storeType the type of the keystore (JKS or PKCS#12)
     * @param storePw the keystore password. Since glassfish requires that keystore and key passwords are identical, this is
     * also used as password for the private key
     * @param privKey the private key to be added to the store
     * @param certChain chain of certificates
     * @param alias the alis of the key to be used inside the keystore
     * @throws java.security.KeyStoreException in case of problems
     */
    public void addKeyPair(File keyStore, String storeType, char[] storePw, PrivateKey privKey, Certificate[] certChain, String alias)
            throws KeyStoreException {
        enforcePasswordComplexity(storePw, "invalidPassword");
        KeyStore ks = openKeyStore(keyStore, storeType, storePw);

        // glassfish requires that keystore and key passwords are identical
        ks.setKeyEntry(alias, privKey, storePw, certChain);
        saveKeyStore(ks, keyStore, storePw);
    }

    /**
     * Adds/updates a keypair to a keystore. This method does not use the keytool, but instead the JAVA API
     * <p>
     * <b>NOTE:</b> Glassfish expects the keystore and key passwords to be identical. For this reason prefer using
     * {@link #addKeyPair(java.io.File, java.lang.String, char[], java.security.PrivateKey, java.security.cert.Certificate[], java.lang.String) }
     * instead
     * </p>
     *
     * @param keyStore the keystore. Must not be null.
     * @param storeType the type of the keystore (JKS or PKCS#12).
     * @param storePw the keystore password
     * @param privKey the private key to be added to the store
     * @param keyPw the private key's password.
     * @param certChain chain of certificates
     * @param alias the alis of the key to be used inside the keystore
     * @throws java.security.KeyStoreException in case of problems
     */
    public void addKeyPair(File keyStore, String storeType, char[] storePw, PrivateKey privKey, char[] keyPw, Certificate[] certChain,
            String alias) throws KeyStoreException {
        enforcePasswordComplexity(keyPw, "invalidPassword");
        KeyStore ks = openKeyStore(keyStore, storeType, storePw);
        ks.setKeyEntry(alias, privKey, keyPw, certChain);
        saveKeyStore(ks, keyStore, storePw);
    }

    /**
     * Changes the keystore password
     *
     * @param oldPassword the old keystore password
     * @param newPassword the new keystore password
     * @param keystore the keystore whose password is to be changed.
     * @throws RepositoryException
     */
    protected void changeKeyStorePassword(String oldPassword, String newPassword, File keystore) throws RepositoryException {
        if (!oldPassword.equals(newPassword)) {
            // change truststore password from the default
            String[] keytoolCmd = { "-storepasswd", "-keystore", keystore.getAbsolutePath(), };

            KeytoolExecutor p = new KeytoolExecutor(keytoolCmd, 30, new String[] { oldPassword, newPassword, newPassword });
            p.execute("keyStorePasswordNotChanged", keystore);
        }
    }

    /**
     * Changes the keystore's password and all contained keys'. This is done to ensure the convention used by glassfish:
     * same password for the keystore and all keys inside.
     * <p>
     * This method DOES NOT use the keytool, but manipulates the given file directly from JAVA.
     * </p>
     *
     * @param keyStore the destination keystore - may be null for an in-memory keystore
     * @param storeType the type of the keystore (JKS or PKCS#12)
     * @param oldPw the old password
     * @param newPw the new password
     * @throws java.security.KeyStoreException in case of problems
     */
    public void changeKeyStorePassword(File keyStore, String storeType, char[] oldPw, char[] newPw) throws KeyStoreException {
        changeKeyStorePassword(keyStore, storeType, oldPw, newPw, true);
    }

    /**
     * Changes the keystore's password and all contained keys'.
     * <p>
     * <b>NOTE:</b> Glassfish expects the keystore and key passwords to be identical. For this reason prefer using
     * {@link #changeKeyStorePassword(java.io.File, java.lang.String, char[], char[]) } instead
     * </p>
     * <p>
     * This method DOES NOT use the keytool, but manipulates the given file directly from JAVA.
     * </p>
     *
     * @param keyStore the destination keystore - may be null for an in-memory keystore
     * @param storeType the type of the keystore (JKS or PKCS#12)
     * @param oldPw the old password
     * @param newPw the new password
     * @param changeKeyPasswords if true, all the keys contained in the keystore will have their passwords set to newStorePw
     * as well
     * @throws java.security.KeyStoreException in case of problems
     */
    public void changeKeyStorePassword(File keyStore, String storeType, char[] oldPw, char[] newPw, boolean changeKeyPasswords)
            throws KeyStoreException {
        enforcePasswordComplexity(newPw, "invalidPassword");
        KeyStore ks = openKeyStore(keyStore, storeType, oldPw);

        if (changeKeyPasswords) {
            Enumeration<String> aliases = ks.aliases();
            // change all private key's passwords
            try {
                while (aliases.hasMoreElements()) {
                    String alias = aliases.nextElement();
                    Key k = ks.getKey(alias, oldPw);
                    if (k != null) {
                        Certificate[] certChain = ks.getCertificateChain(alias);
                        ks.setKeyEntry(alias, k, newPw, certChain);
                    }
                }
            } catch (KeyStoreException | NoSuchAlgorithmException | UnrecoverableKeyException ex) {
                throw new KeyStoreException(ex);
            }
        }
        saveKeyStore(ks, keyStore, newPw);
    }

    /**
     * Changes a private key's password. This method DOES NOT use the keytool, but manipulates the given file directly from
     * JAVA. In addition, this method changes just the password of the key, not the keystore.
     * <p>
     * <b>NOTE:</b> Glassfish expects the keystore and key passwords to be identical. For this reason prefer using
     * {@link #changeKeyStorePassword(java.io.File, java.lang.String, char[], char[]) } instead
     * </p>
     *
     * @param keyStore the path of the keystore where the key with alias is to be modified
     * @param storeType - either "JKS" or "PKCS12"
     * @param storePw - must not be null
     * @param alias the alias of the key to be changed
     * @param oldKeyPw the old password
     * @param newKeyPw the new password
     * @throws KeyStoreException in case of problems
     */
    public void changeKeyPassword(File keyStore, String storeType, char[] storePw, String alias, char[] oldKeyPw, char[] newKeyPw)
            throws KeyStoreException {
        enforcePasswordComplexity(newKeyPw, "invalidPassword");
        try {
            KeyStore ks = openKeyStore(keyStore, storeType, storePw);
            Key privKey = ks.getKey(alias, storePw);
            Certificate[] certs = ks.getCertificateChain(alias);
            ks.setKeyEntry(alias, privKey, newKeyPw, certs);
            saveKeyStore(ks, keyStore, storePw);
        } catch (KeyStoreException | NoSuchAlgorithmException | UnrecoverableKeyException ex) {
            throw new KeyStoreException(ex);
        }
    }

    /**
     * Reads an unencrypted, PKCS#8 formattted and base64 encoded RSA private key from the given File
     *
     * @param keyFile the file containing the private key
     * @return the RSA private key
     * @throws IOException
     * @throws InvalidKeySpecException
     * @throws NoSuchAlgorithmException
     */
    public PrivateKey readPlainPKCS8PrivateKey(File keyFile) throws IOException, InvalidKeySpecException, NoSuchAlgorithmException {
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePrivate(new PKCS8EncodedKeySpec(extractPrivateKeyBytes(Files.lines(keyFile.toPath()))));
    }

    /**
     * Reads an unencrypted, PKCS#8 formattted and base64 encoded private key from the given InputStream using the specified
     * algo
     *
     * @param is the input stream containing the private key
     * @param algo the algorithm used for the private key
     * @return the RSA private key
     * @exception java.io.UncheckedIOException if there is an error with the {@link InputStream}
     * @throws InvalidKeySpecException if the key used was not a valid with the algorithm
     * @throws NoSuchAlgorithmException if no provider exists for the specified algorithm
     */
    public PrivateKey readPlainPKCS8PrivateKey(InputStream is, String algo)
            throws InvalidKeySpecException, NoSuchAlgorithmException {
        KeyFactory keyFactory = KeyFactory.getInstance(algo);
        return keyFactory
                .generatePrivate(new PKCS8EncodedKeySpec(extractPrivateKeyBytes(new BufferedReader(new InputStreamReader(is)).lines())));
    }

    /**
     * Ignores the header and footer and extracts the private key bytes from a PKCS#8 format BASe64 encoded file
     *
     * @param privateKeyLines
     * @return the decoded binary content
     */
    byte[] extractPrivateKeyBytes(Stream<String> privateKeyLines) {
        String base64KeyData = privateKeyLines.filter((line) -> line.charAt(0) != '-').collect(Collectors.joining());
        return Base64.getDecoder().decode(base64KeyData);
    }

    /**
     * Reads X509 certificate(s) from the provided files
     *
     * @param pemFile path to the PEM (or .cer) file containing the X.509 certificate
     * @return certificate chain loaded from the file, if successful
     * @throws KeyStoreException in case of problems
     */
    public Collection<? extends Certificate> readPemCertificateChain(File pemFile) throws KeyStoreException {
        try (InputStream is = new FileInputStream(pemFile)) {
            return CertificateFactory.getInstance("X.509").generateCertificates(is);
        } catch (Exception ex) {
            throw new KeyStoreException(ex);
        }
    }

    /**
     * Changes the key password for the default cert whose alias is s1as. The assumption here is that the keystore password
     * is not the same as the key password. This is due to the fact that the keystore password should first be changed
     * followed next by the key password. The end result is that the keystore and s1as key both have the same passwords.
     * This function will tolerate deletion of the s1as alias, but it will not tolerate changing the s1as key from something
     * other than the database password.
     *
     * @param config
     * @param storePassword the keystore password
     * @param oldKeyPassword the old password for the s1as alias
     * @param newKeyPassword the new password for the s1as alias
     * @throws RepositoryException
     */
    protected void changeS1ASAliasPassword(RepositoryConfig config, String storePassword, String oldKeyPassword, String newKeyPassword)
            throws RepositoryException {
        if (!storePassword.equals(oldKeyPassword) && !oldKeyPassword.equals(newKeyPassword)) {
            final PEFileLayout layout = getFileLayout(config);
            final File keystore = layout.getKeyStore();
            // First see if the alias exists. The user could have deleted it. Any failure in the
            // command indicates that the alias does not exist, so we return without error.
            String realKeyStoreType = null;
            // change all the aliases that exist rather than change s1as only
            List<String> aliases = new ArrayList<>();
            try {
                KeyStore keyStore = KeyStore.getInstance(keystore, storePassword.toCharArray());
                realKeyStoreType = keyStore.getType();
                // debugging hint: try to load the key with password
                //keyStore.getKey("s1as", "changeit".toCharArray())
                Enumeration<String> all = keyStore.aliases();
                while (all.hasMoreElements()) {
                    aliases.add(all.nextElement());
                }
            } catch (Exception e) {
                aliases.add(CERTIFICATE_ALIAS);
            }

            // change truststore password from the default, only for jks, not for pkcs12
            if ("jks".equalsIgnoreCase(realKeyStoreType)) {
                String[] keytoolListCmd = {"-list", "-keystore", keystore.getAbsolutePath(), "-alias", CERTIFICATE_ALIAS,};
                KeytoolExecutor p = new KeytoolExecutor(keytoolListCmd, 30, new String[]{storePassword});
                try {
                    // verify, that the keystore is readable by the new password, using keytool
                    p.execute("s1asKeyPasswordNotChanged", keystore);

                    // keystore is readable, update the key passwords
                    for (String alias : aliases) {
                        String[] keytoolKeyPasswdCmd = new String[]{"-keypasswd", "-keystore", keystore.getAbsolutePath(), "-alias", alias,};
                        p = new KeytoolExecutor(keytoolKeyPasswdCmd, 30, new String[]{storePassword, oldKeyPassword, newKeyPassword, newKeyPassword});
                        p.execute("s1asKeyPasswordNotChanged", keystore);
                    }
                } catch (RepositoryException ex) {
                    getLogger().log(Level.SEVERE, UNHANDLED_EXCEPTION, ex);
                }
            }
        }
    }

    /**
     * Changes the password of the keystore, truststore and the key password of the s1as alias.
     * It is expected that the key / truststores may not exist. This is
     * due to the fact that the user may have deleted them and wishes to set up their own key/truststore
     *
     * @param config the configuration with details of the truststore location and master password
     * @param oldPassword the previous password
     * @param newPassword the new password
     * @throws RepositoryException
     */
    protected void changeSSLCertificateDatabasePassword(RepositoryConfig config, String oldPassword, String newPassword)
            throws RepositoryException {
        final PEFileLayout layout = getFileLayout(config);
        File keystore = layout.getKeyStore();
        File truststore = layout.getTrustStore();

        if (keystore.exists()) {
            // Change the password on the keystore
            changeKeyStorePassword(oldPassword, newPassword, keystore);
            // Change the s1as alias password in the keystore...The assumption
            // here is that the keystore password is not the same as the key password. This is
            // due to the fact that the keystore password should first be changed followed next
            // by the key password. The end result is that the keystore and s1as key both have
            // the same passwords. This function will tolerate deletion of the s1as alias, but
            // it will not tolerate changing the s1as key from something other than the
            // database password.
            try {
                changeS1ASAliasPassword(config, newPassword, oldPassword, newPassword);
            } catch (Exception ex) {
                // For now we eat all exceptions and dump to the log if the password
                // alias could not be changed.
                getLogger().log(Level.SEVERE, UNHANDLED_EXCEPTION, ex);
            }
        } else {
            getLogger().log(Level.SEVERE, SLogger.INVALID_FILE_LOCATION, keystore.getAbsolutePath());
        }

        if (truststore.exists()) {
            // Change the password on the truststore
            changeKeyStorePassword(oldPassword, newPassword, truststore);
        } else {
            getLogger().log(Level.SEVERE, SLogger.INVALID_FILE_LOCATION, truststore.getAbsolutePath());
        }
    }

    protected void chmod(String args, File file) throws IOException {
        if (OS.isUNIX()) {
            // args and file should never be null.
            if (args == null || file == null) {
                throw new IOException(STRING_MANAGER.getString("nullArg"));
            }
            if (!file.exists()) {
                throw new IOException(STRING_MANAGER.getString("fileNotFound"));
            }

            // " +" regular expression for 1 or more spaces
            final String[] argsString = args.split(" +");
            List<String> cmdList = new ArrayList<>();
            cmdList.add("/bin/chmod");
            cmdList.addAll(Arrays.asList(argsString));
            cmdList.add(file.getAbsolutePath());
            new ProcessBuilder(cmdList).start();
        }
    }

    public static String getDASCertDN(final RepositoryConfig cfg) {
        return getCertificateDN(cfg, null);
    }

    public static String getInstanceCertDN(final RepositoryConfig cfg) {
        return getCertificateDN(cfg, INSTANCE_CN_SUFFIX);
    }

    private static String getCNFromCfg(RepositoryConfig cfg) {
        String option = (String) cfg.get(DomainConfig.KEYTOOLOPTIONS);
        if (option == null || option.length() == 0) {
            return null;
        }
        String value = getCNFromOption(option);
        if (value == null || value.length() == 0) {
            return null;
        } else {
            return value;
        }
    }

    /**
     * Returns CN if valid and non-blank. Returns null otherwise.
     *
     * @param option
     * @param name String representing name of the keytooloption
     * @param ignoreNameCase flag indicating if the comparison should be case insensitive
     * @return
     */
    private static String getValueFromOptionForName(String option, String name, boolean ignoreNameCase) {
        // option is not null at this point
        Pattern p = Pattern.compile(":");
        String[] pairs = p.split(option);
        for (String pair : pairs) {
            p = Pattern.compile("=");
            String[] nv = p.split(pair);
            String n = nv[0].trim();
            String v = nv[1].trim();
            boolean found = (ignoreNameCase) ? n.equalsIgnoreCase(name) : n.equals(name);
            if (found) {
                return v;
            }
        }
        return null;
    }

    private static String getCNFromOption(String option) {
        return getValueFromOptionForName(option, OID.CN.getName(), true);
    }
}
