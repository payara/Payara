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
 */
// Portions Copyright [2019-2025] Payara Foundation and/or affiliates

package com.sun.enterprise.security.store;

import com.sun.enterprise.util.SystemPropertyConstants;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import java.util.Enumeration;
import java.util.Arrays;

/**
 * This class implements an adapter for password manipulation a JCEKS.
 ** Note that although it uses locks ('synchronized'), it tends to be created
 ** anew with each use, an inefficient and potentially problematic use that
 ** could create more than one instance accessing the same keystore at a time.
 */
public final class PasswordAdapter {
    public static final String PASSWORD_ALIAS_KEYSTORE = "domain-passwords";

    private KeyStore    pwdStore;
    private final File  keyFile;
    private char[]      masterPassword;

    private char[] getMasterPassword() {
        return masterPassword;
    }

    private static String getDefaultKeyFileName() {
        return System.getProperty(SystemPropertyConstants.INSTANCE_ROOT_PROPERTY) + File.separator + "config" + File.separator + PASSWORD_ALIAS_KEYSTORE;
    }

    /**
     * Construct a PasswordAdapter with given Shared Master Password,
     * SMP using the default keyfile (domain-passwords.jceks)
     * @param smp master password
     * @throws CertificateException
     * @throws IOException
     * @throws KeyStoreException
     * @throws NoSuchAlgorithmException
     */
    public PasswordAdapter(char[] masterPassword) throws CertificateException, IOException, KeyStoreException, NoSuchAlgorithmException {
       this( getDefaultKeyFileName(), masterPassword );
    }

    /**
     * Construct a PasswordAdapter with given Shared Master Password,
     * SMP.
     * @param keyfileName the jceks key file name
     * @param smp master password
     * @throws CertificateException
     * @throws IOException
     * @throws KeyStoreException
     * @throws NoSuchAlgorithmException
     */
    public PasswordAdapter(final String keyStoreFileName, final char[] masterPassword) 
            throws CertificateException, IOException, KeyStoreException, NoSuchAlgorithmException {
        
        final File  keyStoreFile    = new File( keyStoreFileName );

        this.pwdStore   = loadKeyStore( keyStoreFile, masterPassword);

        // assign these only once the store is good; no need to keep copies otherwise!
        this.keyFile            = keyStoreFile;
        this.masterPassword     = masterPassword;

    }

    /**
     * Construct a PasswordAdapter with given Shared Master Password,
     * SMP.
     * @param keyfileName the jceks key file name
     * @param smp the master password
     * @exception CertificateException
     * @exception IOException
     * @exception KeyStoreException
     * @exception NoSuchAlgorithmException
     */
    private static KeyStore loadKeyStore( final File keyStoreFile, final char[] masterPassword )
            throws CertificateException, IOException, KeyStoreException, NoSuchAlgorithmException {
        
        final KeyStore keyStore = KeyStore.getInstance("JCEKS");

        if (keyStoreFile.exists()) {
            try ( // don't buffer keystore; it's tiny anyway
                    FileInputStream input = new FileInputStream(keyStoreFile)) {
                keyStore.load(input, masterPassword);
            }
        } else {
            keyStore.load(null, masterPassword);
        }

        return keyStore;
    }

    /**
     * This methods returns password String for a given alias and SMP.
     * @param alias
     * @return corresponding password or null if the alias does not exist.
     * @exception KeyStoreException
     * @exception NoSuchAlgorithmException
     * @exception UnrecoverableKeyException
     */
    public synchronized String getPasswordForAlias(final String alias)
            throws KeyStoreException, NoSuchAlgorithmException,
            UnrecoverableKeyException
    {
        String passwordString = null;

        final Key key = pwdStore.getKey( alias, getMasterPassword() );
        if ( key != null )
        {
            passwordString  = new String(key.getEncoded(), StandardCharsets.UTF_8);
        }

        return passwordString;
    }

    /**
     * This methods returns password SecretKey for a given alias and SMP.
     * @param alias
     * @return corresponding password SecretKey or
     *         null if the alias does not exist.
     * @exception KeyStoreException
     * @exception NoSuchAlgorithmException
     * @exception UnrecoverableKeyException
     */
    public synchronized SecretKey getPasswordSecretKeyForAlias(String alias)
            throws KeyStoreException, NoSuchAlgorithmException, UnrecoverableKeyException {

        return (SecretKey)pwdStore.getKey(alias, getMasterPassword());
    }

    /**
     * See if the given alias exists
     * @param alias the alias name
     * @return true if the alias exists in the keystore
     */
    public synchronized boolean aliasExists( final String alias) throws KeyStoreException {
        return pwdStore.containsAlias(alias);
    }

    /**
     * Remove an alias from the keystore
     * @param alias The name of the alias to remove
     * @throws KeyStoreException
     * @throws IOException
     * @throws NoSuchAlgorithmException
     * @throws CertificateException
     */
    public synchronized void removeAlias( final String alias)
        throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException, UnrecoverableKeyException {
        pwdStore.deleteEntry(alias);
        writeStore();
    }

    /**
     * Return the aliases from the keystore.
     * @return An enumeration containing all the aliases in the keystore.
     */
    public synchronized Enumeration<String> getAliases()throws KeyStoreException {
        return pwdStore.aliases();
    }

    /**
     * Writes the keystore to disk
     * @throws KeyStoreException
     * @throws IOException
     * @throws NoSuchAlgorithmException
     * @throws CertificateException
     */
    public void writeStore() throws KeyStoreException, IOException,
        NoSuchAlgorithmException, CertificateException, UnrecoverableKeyException
    {
        writeStore(null);
    }

    public void writeStore(final String newFilePath) throws KeyStoreException, IOException,
        NoSuchAlgorithmException, CertificateException, UnrecoverableKeyException
    {
        writeKeyStoreSafe(getMasterPassword(), newFilePath);
    }

    /**
     * This methods set alias, secretKey into JCEKS keystore.
     * @param alias
     * @param keyBytes
     * @exception CertificateException
     * @exception IOException
     * @exception KeyStoreException
     * @exception NoSuchAlgorithmException
     */
    public synchronized void setPasswordForAlias(final String alias, final byte[] keyBytes)
        throws CertificateException, IOException, KeyStoreException, NoSuchAlgorithmException, UnrecoverableKeyException {
        setPasswordForAlias(alias, keyBytes, null);
    }

    public synchronized void setPasswordForAlias(final String alias, final byte[] keyBytes, final String newFilePath)
        throws CertificateException, IOException, KeyStoreException, NoSuchAlgorithmException, UnrecoverableKeyException {
        
        final Key key = new SecretKeySpec(keyBytes, "AES");
        pwdStore.setKeyEntry( alias, key, getMasterPassword(), null);
        writeStore(newFilePath);
    }


    /**
     * Make a new in-memory KeyStore with all the keys secured with the new master password.
     */
    private KeyStore duplicateKeyStore(final char[] newMasterPassword)
            throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException, UnrecoverableKeyException {
        
        final char[] oldMasterPassword = getMasterPassword();

        final KeyStore oldStore = pwdStore;
        final KeyStore newKeyStore = KeyStore.getInstance("JCEKS", pwdStore.getProvider());
        newKeyStore.load(null, newMasterPassword);

        final Enumeration<String> aliasesEnum = oldStore.aliases();
        while (aliasesEnum.hasMoreElements()) {
            final String alias = aliasesEnum.nextElement();

            if (!oldStore.isKeyEntry(alias)) {
                throw new IllegalArgumentException("Expecting keys only");
            }

            final Key key = oldStore.getKey(alias, oldMasterPassword);
            newKeyStore.setKeyEntry(alias, key, newMasterPassword, null);
        }

        return newKeyStore;
    }

    /**
     * Write the KeyStore to disk. Calling code should protect against overwriting any original file.
     *
     * @param keyStore
     * @param file
     * @param masterPassword
     */
    private static void writeKeyStoreToFile(final KeyStore keyStore, final File file, final char[] masterPassword)
            throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException {
        
        try (FileOutputStream out = new FileOutputStream(file)) {
            keyStore.store(out, masterPassword);
        }
    }


    /**
        Writes the current KeyStore to disk in a manner that preserves its
        on-disk representation from being destroyed if something goes wrong;
        a temporary file is used.
     */
    private synchronized void writeKeyStoreSafe(final char[] masterPassword)
        throws KeyStoreException, IOException, NoSuchAlgorithmException,
        CertificateException, UnrecoverableKeyException {
        writeKeyStoreSafe(masterPassword, null);
    }

    private synchronized void writeKeyStoreSafe(final char[] masterPassword, final String newFilePath)
            throws KeyStoreException, IOException, NoSuchAlgorithmException,
            CertificateException, UnrecoverableKeyException {

        File fileToSave = newFilePath == null ? keyFile : new File(newFilePath);

        // if the KeyStore exists, update it in a manner that doesn't destroy
        // the existing store if a failure occurs.
        if (keyFile.exists()) {
            final KeyStore newKeyStore;
            if (Arrays.equals(masterPassword, this.masterPassword)) {
                newKeyStore = this.pwdStore;
            } else {
                newKeyStore = duplicateKeyStore(masterPassword);
            }

            // 'newKeyStore' is now complete; rename the old KeyStore, the write the new one in its place
            final File saveOld = new File(keyFile.toString() + ".save");

            if (!keyFile.renameTo(saveOld)) {
                final String msg = "Can't rename " + keyFile + " to " + saveOld;
                throw new IOException(msg);
            }

            try {
                //debug( "Writing KeyStore to " + _keyFile + " using master password = " + new String(masterPassword) );
                writeKeyStoreToFile(newKeyStore, fileToSave, masterPassword);
                pwdStore = newKeyStore;
                this.masterPassword = masterPassword;
                //debug( "KeyStore written successfully" );
            } catch (final Throwable t) {
                try {
                    if (!saveOld.renameTo(keyFile)) {
                        throw new RuntimeException("Could not write new KeyStore, and cannot restore KeyStore to original state", t);
                    }
                } catch (final Throwable tt) {
                    /* best effort failed */
                    throw new RuntimeException("Could not write new KeyStore, and cannot restore KeyStore to original state", tt);
                }

                throw new RuntimeException("Can't write new KeyStore", t);
            }

            try {
                if (!saveOld.delete()) {
                    throw new RuntimeException("Can't remove old KeyStore \"" + keyFile + "\"");
                }

            } catch (Throwable t) {
                throw new RuntimeException("Can't remove old KeyStore \"" + keyFile + "\"", t);
            }
        } else {
            writeKeyStoreToFile(pwdStore, fileToSave, masterPassword);
        }

        loadKeyStore(fileToSave, getMasterPassword());
    }


    /**
        Changes the keystore password, including the encoding of the keys within it.
        <p>
        There are several error conditions that could occur:
        <ul>
            <li>Problem extracting existing alias keys with new ones.</li>
            <li>Problem writing the keystore, including destroying it if an I/O problem occurs.</li>
            <li></li>
        </ul>
        For these reasons,  make a new KeyStore and write it, then swap it with the old
        one.

      * @param newMasterPassword the new keystore password
      * @throws KeyStoreException
      * @throws IOException
      * @throws NoSuchAlgorithmException
      * @throws CertificateException
      */
    public synchronized void changePassword(char[] newMasterPassword)
        throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException, UnrecoverableKeyException {

        writeKeyStoreSafe(newMasterPassword);

     }
 }


