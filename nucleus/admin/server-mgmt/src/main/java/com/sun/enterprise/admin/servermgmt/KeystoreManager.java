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

package com.sun.enterprise.admin.servermgmt;

import com.sun.enterprise.admin.servermgmt.domain.DomainConstants;
import com.sun.enterprise.admin.servermgmt.pe.PEFileLayout;
import com.sun.enterprise.universal.glassfish.ASenvPropertyReader;
import com.sun.enterprise.universal.io.SmartFile;
import com.sun.enterprise.universal.process.ProcessUtils;
import com.sun.enterprise.util.*;
import com.sun.enterprise.util.i18n.StringManager;
import com.sun.enterprise.util.io.FileUtils;
import com.sun.enterprise.util.net.NetUtils;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.util.*;
import java.util.logging.Level;
import java.util.regex.Pattern;

import static com.sun.enterprise.admin.servermgmt.SLogger.*;

/**
 * @author kebbs
 */
public class KeystoreManager {

    private static final String KEYTOOL_CMD;
    private static final String KEYTOOL_EXE_NAME = OS.isWindows() ? "keytool.exe" : "keytool";
    private static String CERTIFICATE_DN_PREFIX = "CN=";
    private static String CERTIFICATE_DN_SUFFIX =
            ",OU=GlassFish,O=Oracle Corporation,L=Santa Clara,ST=California,C=US";
    public static final String CERTIFICATE_ALIAS = "s1as";
    public static final String INSTANCE_SECURE_ADMIN_ALIAS = "glassfish-instance";
    public static final String DEFAULT_MASTER_PASSWORD = "changeit";
    private static final String SKID_EXTENSION_SYSTEM_PROPERTY =
            "-J-Dsun.security.internal.keytool.skid";
    private static final String INSTANCE_CN_SUFFIX = "-instance";

    static {
        // Byron Nevins, July 2011
        String nonFinalKeyTool = KEYTOOL_EXE_NAME; // at the end we set the final
        String propName = SystemPropertyConstants.JAVA_ROOT_PROPERTY;
        String javaroot = new ASenvPropertyReader().getProps().get(propName);
        File k = new File(new File(javaroot, "bin"), KEYTOOL_EXE_NAME);

        if (k.canExecute())
            nonFinalKeyTool = SmartFile.sanitize(k.getPath());
        else {
            // can't find it in a JDK.  Maybe it is in the PATH?
            k = ProcessUtils.getExe(KEYTOOL_EXE_NAME);

            if (k != null && k.canExecute())
                nonFinalKeyTool = k.getPath();
        }
        KEYTOOL_CMD = nonFinalKeyTool;
    }

    protected static class KeytoolExecutor extends ProcessExecutor {

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

        //We must override this message so that the stdout appears in the exec exception.
        //Keytool seems to output errors to stdout.
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
                    throw new RepositoryException(_strMgr.getString(keystoreErrorMsg, keystoreName)
                            + getLastExecutionError() + " " + getLastExecutionOutput());
                }
            }
            catch (ExecException ex) {
                throw new RepositoryException(_strMgr.getString(keystoreErrorMsg,
                        keystoreName) + getLastExecutionError() + " " + getLastExecutionOutput(), ex);
            }
        }
    }
    protected PEFileLayout _fileLayout = null;
    private static final StringManager _strMgr =
            StringManager.getManager(KeystoreManager.class);

    /**
     * Creates a new instance of RepositoryManager
     */
    public KeystoreManager() {
    }

    protected static String getCertificateDN(RepositoryConfig cfg, final String CNSuffix) {
        String cn = getCNFromCfg(cfg);
        if (cn == null) {
            try {
                cn = NetUtils.getCanonicalHostName();
            }
            catch (Exception e) {
                cn = "localhost";
            }
        }
        /*
         * Use the suffix, if provided, in creating the DN (by augmenting
         * the CN).
         */
        String x509DistinguishedName = CERTIFICATE_DN_PREFIX + cn
                + (CNSuffix != null ? CNSuffix : "") + CERTIFICATE_DN_SUFFIX;
        return x509DistinguishedName;  //must be of form "CN=..., OU=..."
    }

    protected PEFileLayout getFileLayout(RepositoryConfig config) {
        if (_fileLayout == null) {
            _fileLayout = new PEFileLayout(config);
        }
        return _fileLayout;
    }

    /**
     * Create the default SSL key store using keytool to generate a self signed
     * certificate.
     *
     * @param config
     * @param masterPassword
     * @throws RepositoryException
     */
    protected void createKeyStore(File keystore,
            RepositoryConfig config, String masterPassword) throws RepositoryException {
        //Generate a new self signed certificate with s1as as the alias
        //Create the default self signed cert
        final String dasCertDN = getDASCertDN(config);
        System.out.println(_strMgr.getString("CertificateDN", dasCertDN));
        addSelfSignedCertToKeyStore(keystore, CERTIFICATE_ALIAS, masterPassword, dasCertDN);

        // Create the default self-signed cert for instances to use for SSL auth.
        final String instanceCertDN = getInstanceCertDN(config);
        System.out.println(_strMgr.getString("CertificateDN", instanceCertDN));
        addSelfSignedCertToKeyStore(keystore, INSTANCE_SECURE_ADMIN_ALIAS, masterPassword, instanceCertDN);
    }

    private void addSelfSignedCertToKeyStore(final File keystore,
            final String alias,
            final String masterPassword,
            final String dn) throws RepositoryException {
        final String[] keytoolCmd = {
            "-genkey",
            "-keyalg", "RSA",
            "-keystore", keystore.getAbsolutePath(),
            "-alias", alias,
            "-dname", dn,
            "-validity", "3650",
            "-keypass", masterPassword,
            "-storepass", masterPassword,
            SKID_EXTENSION_SYSTEM_PROPERTY
        };

        KeytoolExecutor p = new KeytoolExecutor(keytoolCmd, 60);
        p.execute("keystoreNotCreated", keystore);
    }
    /*
     protected void addToAsadminTrustStore(
     RepositoryConfig config, File certFile) throws RepositoryException
     {
     boolean newTruststore = false;
     final PEFileLayout layout = getFileLayout(config);
     //import the newly created certificate into the asadmin truststore
     final File asadminTruststore = AsadminTruststore.getAsadminTruststore();

     if (!asadminTruststore.exists()) {
     newTruststore = true;
     }

     //The keystore alias name is the repository name. We want to avoid alias
     //name conflicts since multiple domains are likely to live on the same
     //machine.
     String aliasName = layout.getRepositoryDir().getAbsolutePath();

     //first delete the alias in case it already exists. This can happen for
     //example if a domain is created, deleted, and re-created again.
     String[] keytoolCmd = new String[] {
     "-delete",
     "-keystore", asadminTruststore.getAbsolutePath(),
     "-alias", aliasName,
     };

     final String[] input = {AsadminTruststore.getAsadminTruststorePassword(),
     AsadminTruststore.getAsadminTruststorePassword()}; // twice in case we are creating
     KeytoolExecutor p = new KeytoolExecutor(keytoolCmd, 30, input);
     try {
     p.execute("trustStoreNotCreated", asadminTruststore);
     } catch (RepositoryException ex) {
     //ignore all exceptions. The alias most likely does not exist.
     }

     keytoolCmd = new String[] {
     "-import",
     "-noprompt",
     "-keystore", asadminTruststore.getAbsolutePath(),
     "-alias", aliasName, //alias is the domain name
     "-file", certFile.getAbsolutePath(),
     };

     p = new KeytoolExecutor(keytoolCmd, 30, input);
     p.execute("trustStoreNotCreated", asadminTruststore);

     //If this is a newly created truststore, lock it down.
     if (newTruststore) {
     try {
     chmod("600", asadminTruststore);
     } catch (IOException ex) {
     throw new RepositoryException(_strMgr.getString(
     "trustStoreNotCreated", asadminTruststore), ex);
     }
     }
     }
     */

    protected void copyCertificates(File configRoot, DomainConfig config, String masterPassword)
       throws DomainException 
    {
        try {
            copyCert(configRoot, CERTIFICATE_ALIAS, masterPassword);
            copyCert(configRoot, INSTANCE_SECURE_ADMIN_ALIAS, masterPassword);
        } catch(RepositoryException re) {
            String msg = _strMgr.getString("SomeProblemWithKeytool", re.getMessage());
            throw new DomainException(msg);

        }
    }

    private void copyCert(final File configRoot,
            final String alias,
            final String masterPassword) throws RepositoryException {
    	File keystore = new File(configRoot, DomainConstants.KEYSTORE_FILE);
    	File truststore = new File(configRoot, DomainConstants.TRUSTSTORE_FILE);
        File certFile = null;
        String[] input = {masterPassword};
        String[] keytoolCmd = null;
        KeytoolExecutor p = null;

        try {
            //export the newly created certificate from the keystore
            certFile = new File(configRoot, alias + ".cer");
            keytoolCmd = new String[]{
                "-export",
                "-keystore", keystore.getAbsolutePath(),
                "-alias", alias,
                "-file", certFile.getAbsolutePath(),};

            p = new KeytoolExecutor(keytoolCmd, 30, input);
            p.execute("trustStoreNotCreated", truststore);

            //import the newly created certificate into the truststore
            keytoolCmd = new String[]{
                "-import",
                "-noprompt",
                "-keystore", truststore.getAbsolutePath(),
                "-alias", alias,
                "-file", certFile.getAbsolutePath(),};

            p = new KeytoolExecutor(keytoolCmd, 30, input);
            p.execute("trustStoreNotCreated", truststore);

            //import the newly created certificate into the asadmin truststore
            /* commented out till asadmintruststore can be added back */
            //addToAsadminTrustStore(config, certFile);

        }
        finally {
            if (certFile != null) {
                final boolean isCertFileDeleted = certFile.delete();
                if (!isCertFileDeleted) {
                    getLogger().log(Level.WARNING, BAD_DELETE_TEMP_CERT_FILE,
                            certFile.getAbsolutePath());
                }
            }
        }
    }

    /**
     * Changes the keystore password
     *
     * @param oldPassword the old keystore password
     * @param newPassword the new keystore password
     * @param keystore the keystore whose password is to be changed.
     * @throws RepositoryException
     */
    protected void changeKeystorePassword(String oldPassword, String newPassword,
            File keystore) throws RepositoryException {
        if (!oldPassword.equals(newPassword)) {
            //change truststore password from the default
            String[] keytoolCmd = {
                "-storepasswd",
                "-keystore", keystore.getAbsolutePath(),};

            KeytoolExecutor p = new KeytoolExecutor(keytoolCmd, 30,
                    new String[]{oldPassword, newPassword, newPassword});
            p.execute("keyStorePasswordNotChanged", keystore);
        }
    }

    /**
     * Changes the key password for the default cert whose alias is s1as. The
     * assumption here is that the keystore password is not the same as the key
     * password. This is due to the fact that the keystore password should first
     * be changed followed next by the key password. The end result is that the
     * keystore and s1as key both have the same passwords. This function will
     * tolerate deletion of the s1as alias, but it will not tolerate changing
     * the s1as key from something other than the database password.
     *
     * @param config
     * @param storePassword the keystore password
     * @param oldKeyPassword the old password for the s1as alias
     * @param newKeyPassword the new password for the s1as alias
     * @throws RepositoryException
     */
    protected void changeS1ASAliasPassword(RepositoryConfig config,
            String storePassword, String oldKeyPassword, String newKeyPassword)
            throws RepositoryException {
        if (!storePassword.equals(oldKeyPassword) && !oldKeyPassword.equals(newKeyPassword)) {
            final PEFileLayout layout = getFileLayout(config);
            final File keystore = layout.getKeyStore();
            //First see if the alias exists. The user could have deleted it. Any failure in the
            //command indicates that the alias does not exist, so we return without error.
            String keyStoreType = System.getProperty("javax.net.ssl.keyStoreType");
            if (keyStoreType == null) {
                keyStoreType = KeyStore.getDefaultType();
            }

            //add code to change all the aliases that exist rather then change s1as only
            List<String> aliases = new ArrayList<String>();
            FileInputStream is = null;
            try {
                KeyStore keyStore = KeyStore.getInstance(keyStoreType);
                is = new FileInputStream(keystore);
                keyStore.load(is, storePassword.toCharArray());
                Enumeration<String> all = keyStore.aliases();
                while (all.hasMoreElements()) {
                    aliases.add(all.nextElement());
                }
            }
            catch (Exception e) {
                aliases.add(CERTIFICATE_ALIAS);
            }
            finally {
                if (is != null) {
                    try {
                        is.close();
                    }
                    catch (IOException ex) {
                        getLogger().log(Level.SEVERE, UNHANDLED_EXCEPTION, ex);
                    }
                }
            }

            String[] keytoolCmd = {
                "-list",
                "-keystore", keystore.getAbsolutePath(),
                "-alias", CERTIFICATE_ALIAS,};
            KeytoolExecutor p = new KeytoolExecutor(keytoolCmd, 30,
                    new String[]{storePassword});
            try {
                p.execute("s1asKeyPasswordNotChanged", keystore);
            }
            catch (RepositoryException ex) {
                return;
            }

            //change truststore password from the default
            for (String alias : aliases) {
                keytoolCmd = new String[]{
                    "-keypasswd",
                    "-keystore", keystore.getAbsolutePath(),
                    "-alias", alias,};
                p = new KeytoolExecutor(keytoolCmd, 30,
                        new String[]{storePassword, oldKeyPassword, newKeyPassword, newKeyPassword});
                p.execute("s1asKeyPasswordNotChanged", keystore);
            }

        }
    }

    /**
     * Changes the password of the keystore, truststore and the key password of
     * the s1as alias. It is expected that the key / truststores may not exist.
     * This is due to the fact that the user may have deleted them and wishes to
     * set up their own key/truststore
     *
     * @param config
     * @param storePassword
     * @param oldKeyPassword
     * @param newKeyPassword
     */
    protected void changeSSLCertificateDatabasePassword(RepositoryConfig config,
            String oldPassword, String newPassword) throws RepositoryException {
        final PEFileLayout layout = getFileLayout(config);
        File keystore = layout.getKeyStore();
        File truststore = layout.getTrustStore();

        if (keystore.exists()) {
            //Change the password on the keystore
            changeKeystorePassword(oldPassword, newPassword, keystore);
            //Change the s1as alias password in the keystore...The assumption
            //here is that the keystore password is not the same as the key password. This is
            //due to the fact that the keystore password should first be changed followed next
            //by the key password. The end result is that the keystore and s1as key both have
            //the same passwords. This function will tolerate deletion of the s1as alias, but
            //it will not tolerate changing the s1as key from something other than the
            //database password.
            try {
                changeS1ASAliasPassword(config, newPassword, oldPassword, newPassword);
            }
            catch (Exception ex) {
                //For now we eat all exceptions and dump to the log if the password
                //alias could not be changed.
                getLogger().log(Level.SEVERE, UNHANDLED_EXCEPTION, ex);
            }
        }

        if (truststore.exists()) {
            //Change the password on the truststore
            changeKeystorePassword(oldPassword, newPassword, truststore);
        }
    }

    protected void chmod(String args, File file) throws IOException {
        if (OS.isUNIX()) {
            //args and file should never be null.
            if (args == null || file == null)
                throw new IOException(_strMgr.getString("nullArg"));
            if (!file.exists())
                throw new IOException(_strMgr.getString("fileNotFound"));

            // " +" regular expression for 1 or more spaces
            final String[] argsString = args.split(" +");
            List<String> cmdList = new ArrayList<String>();
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
        if (option == null || option.length() == 0)
            return null;
        String value = getCNFromOption(option);
        if (value == null || value.length() == 0) {
            return null;
        }
        else {
            return value;
        }
    }

    /**
     * Returns CN if valid and non-blank. Returns null otherwise.
     *
     * @param option
     * @param name String representing name of the keytooloption
     * @param ignoreNameCase flag indicating if the comparison should be case
     * insensitive
     * @return
     */
    private static String getValueFromOptionForName(String option, String name, boolean ignoreNameCase) {
        //option is not null at this point
        Pattern p = Pattern.compile(":");
        String[] pairs = p.split(option);
        for (String pair : pairs) {
            p = Pattern.compile("=");
            String[] nv = p.split(pair);
            String n = nv[0].trim();
            String v = nv[1].trim();
            boolean found = (ignoreNameCase == true) ? n.equalsIgnoreCase(name) : n.equals(name);
            if (found)
                return v;
        }
        return null;
    }

    private static String getCNFromOption(String option) {
        return getValueFromOptionForName(option, "CN", true);
    }
}
