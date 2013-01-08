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

package com.sun.enterprise.security.store;

import com.sun.enterprise.util.SystemPropertyConstants;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import java.util.Enumeration;

/**
 * This class implements an adapter for password manipulation a JCEKS.
 ** Note that although it uses locks ('synchronized'), it tends to be created
 ** anew with each use, an inefficient and potentially problematic use that
 ** could create more than one instance accessing the same keystore at a time.
 */
public final class PasswordAdapter {
    public static final String PASSWORD_ALIAS_KEYSTORE = "domain-passwords";

    private KeyStore    _pwdStore;
    private final File  _keyFile;
    private char[]      _masterPassword;

    private char[] getMasterPassword() {
        return _masterPassword;
    }

/*
    private static final boolean DEBUG = true;
    private void //debug( final Object o )
    {
        if ( DEBUG )
        {
            System.out.println( "PasswordAdapter.debug: " + o );
        }
    }
        private void
    debugState( final String when)
    {
        if ( DEBUG )
        {
            final String INDENT = "    ";
            try
            {
                String s = "PasswordAdapter Status: " + when + "\n";
                s = s +INDENT + "Master password: " + new String( getMasterPassword() ) +"\n";
                s = s + INDENT + "Key file: " + _keyFile + "\n";
                s = s +INDENT + "Aliases:\n";
                final Enumeration<String>   aliases = getAliases();
                while ( aliases.hasMoreElements() )
                {
                    final String alias  = aliases.nextElement();
                    s = s +INDENT + INDENT + alias + "=" +
                            new String( getPasswordForAlias(alias) ) + "\n";
                }
                //debug(s);
            }
            catch( Throwable t )
            {
                t.printStackTrace();
            }
        }
    }
*/

        private static String
    getDefaultKeyFileName()
    {
        return System.getProperty(SystemPropertyConstants.INSTANCE_ROOT_PROPERTY) +
            File.separator + "config" + File.separator + PASSWORD_ALIAS_KEYSTORE;
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
    public PasswordAdapter(char[] masterPassword)
            throws CertificateException, IOException,
            KeyStoreException, NoSuchAlgorithmException
    {
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
    public PasswordAdapter(
        final String keyStoreFileName,
        final char[] masterPassword)
            throws CertificateException, IOException,
            KeyStoreException, NoSuchAlgorithmException
    {
        final File  keyStoreFile    = new File( keyStoreFileName );

        _pwdStore   = loadKeyStore( keyStoreFile, masterPassword);

        // assign these only once the store is good; no need to keep copies otherwise!
        _keyFile            = keyStoreFile;
        _masterPassword     = masterPassword;

        //debugState( "PasswordAdapter constructor" );
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
       private static KeyStore
    loadKeyStore( final File keyStoreFile, final char[] masterPassword )
            throws CertificateException, IOException,
            KeyStoreException, NoSuchAlgorithmException
    {
        final KeyStore  keyStore = KeyStore.getInstance("JCEKS");

        if ( keyStoreFile.exists() )
        {
            // don't buffer keystore; it's tiny anyway
            final FileInputStream   input   = new FileInputStream( keyStoreFile );
            try {
                keyStore.load( input, masterPassword );
            }
            finally {
                input.close();
            }
        }
        else
        {
            keyStore.load( null, masterPassword );
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

        final Key key = _pwdStore.getKey( alias, getMasterPassword() );
        if ( key != null )
        {
            passwordString  = new String( key.getEncoded() );
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
            throws KeyStoreException, NoSuchAlgorithmException,
            UnrecoverableKeyException {

        return (SecretKey)_pwdStore.getKey(alias, getMasterPassword());
    }

    /**
     * See if the given alias exists
     * @param alias the alias name
     * @return true if the alias exists in the keystore
     */
    public synchronized boolean aliasExists( final String alias) throws KeyStoreException
    {
        return _pwdStore.containsAlias(alias);
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
        throws KeyStoreException, IOException,
            NoSuchAlgorithmException, CertificateException, UnrecoverableKeyException
    {
        _pwdStore.deleteEntry(alias);
        writeStore();
    }

    /**
     * Return the aliases from the keystore.
     * @return An enumeration containing all the aliases in the keystore.
     */
    public synchronized Enumeration<String> getAliases()
        throws KeyStoreException
    {
        return _pwdStore.aliases();
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
        writeKeyStoreSafe( getMasterPassword() );
    }

    /**
     * This methods set alias, secretKey into JCEKS keystore.
     * @param alias
     * @param secretKey
     * @exception CertificateException
     * @exception IOException
     * @exception KeyStoreException
     * @exception NoSuchAlgorithmException
     */
    public synchronized void setPasswordForAlias(
        final String alias,
        final byte[] keyBytes)
        throws CertificateException, IOException, KeyStoreException,
            NoSuchAlgorithmException, UnrecoverableKeyException
    {
        //debugState( "BEFORE setPasswordForAlias" );
        final Key key = new SecretKeySpec(keyBytes, "AES");
        _pwdStore.setKeyEntry( alias, key, getMasterPassword(), null);
        writeStore();
        //debugState( "AFTER setPasswordForAlias" );
    }


    /**
        Make a new in-memory KeyStore with all the keys secured with
        the new master password.
      */
        private KeyStore
    duplicateKeyStore( final char[] newMasterPassword)
        throws KeyStoreException, IOException, NoSuchAlgorithmException,
            CertificateException, UnrecoverableKeyException
    {
        final char[]    oldMasterPassword       = getMasterPassword();

        final KeyStore oldStore = _pwdStore;
        final KeyStore newKeyStore = KeyStore.getInstance("JCEKS", _pwdStore.getProvider() );
        newKeyStore.load( null, newMasterPassword );

        final Enumeration<String>   aliasesEnum = oldStore.aliases();
        while ( aliasesEnum.hasMoreElements() )
        {
            final String alias  = aliasesEnum.nextElement();

            if ( ! oldStore.isKeyEntry( alias ) )
            {
                throw new IllegalArgumentException( "Expecting keys only" );
            }

            final Key key      = oldStore.getKey( alias, oldMasterPassword );
            newKeyStore.setKeyEntry( alias, key, newMasterPassword, null);
        }

        return newKeyStore;
    }

    /**
        Write the KeyStore to disk.  Calling code should protect against
        overwriting any original file.
        @param keyStore
        @param file
        @param masterPassword
     */
        private static void
    writeKeyStoreToFile(
        final KeyStore  keyStore,
        final File      file,
        final char[]    masterPassword )
           throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException
    {
        final FileOutputStream  out = new FileOutputStream(file);
        try
        {
            keyStore.store( out, masterPassword);
        }
        finally
        {
            out.close();
        }
    }


    /**
        Writes the current KeyStore to disk in a manner that preserves its
        on-disk representation from being destroyed if something goes wrong;
        a temporary file is used.
     */
        private synchronized void
    writeKeyStoreSafe( final char[] masterPassword)
        throws KeyStoreException, IOException, NoSuchAlgorithmException,
            CertificateException, UnrecoverableKeyException
     {
        final boolean keystoreExists = _keyFile.exists();

        // if the KeyStore exists, update it in a manner that doesn't destroy
        // the existing store if a failure occurs.
        if ( keystoreExists )
        {
            final KeyStore newKeyStore = duplicateKeyStore( masterPassword );

            // 'newKeyStore' is now complete; rename the old KeyStore, the write the new one in its place
            final File  saveOld = new File( _keyFile.toString() + ".save" );

            if ( ! _keyFile.renameTo( saveOld ) )
            {
                final String msg    = "Can't rename " + _keyFile + " to " + saveOld;
                throw new IOException( msg );
            }

            try
            {
                //debug( "Writing KeyStore to " + _keyFile + " using master password = " + new String(masterPassword) );
                writeKeyStoreToFile( newKeyStore, _keyFile, masterPassword );
                _pwdStore   = newKeyStore;
                _masterPassword = masterPassword;
                //debug( "KeyStore written successfully" );
            }
            catch( final Throwable t )
            {
                try
                {
                    if(!saveOld.renameTo( _keyFile ))
                        throw new RuntimeException( "Could not write new KeyStore, and " +
                        "cannot restore KeyStore to original state", t );
                }
                catch( final Throwable tt )
                {
                    /* best effort failed */
                    throw new RuntimeException( "Could not write new KeyStore, and " +
                        "cannot restore KeyStore to original state", tt );
                }

                throw new RuntimeException( "Can't write new KeyStore", t );
            }

            try
            {
                //debug( "deleting old keystore " + saveOld );
                if(!saveOld.delete())
                    throw new RuntimeException( "Can't remove old KeyStore \"" +  _keyFile  + "\"");

               // //debug( "done deleting old keystore "  saveOld );
            }
            catch( Throwable t )
            {
                throw new RuntimeException( "Can't remove old KeyStore \"" +  _keyFile  + "\"", t );
            }
        }
        else
        {
            //debug( "Writing new KeyStore to " + _keyFile + " using master password = " + new String(masterPassword) );
            writeKeyStoreToFile( _pwdStore, _keyFile, masterPassword );
        }

        //debugState( "AFTER changing master password" );

        loadKeyStore( _keyFile, getMasterPassword() );

        //debugState( "AFTER forcing reload from file" );
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

      * @param newpassword the new keystore password
      * @throws KeyStoreException
      * @throws IOException
      * @throws NoSuchAlgorithmException
      * @throws CertificateException
      */
    public synchronized void changePassword(char[] newMasterPassword)
        throws KeyStoreException, IOException, NoSuchAlgorithmException,
            CertificateException, UnrecoverableKeyException
     {
        //debug( "Changing master password from " + new String(oldMasterPassword) + " to "  + new String(newMasterPassword) );
        //debugState( "BEFORE changing master password" );

        writeKeyStoreSafe(newMasterPassword);

        //debugState( "AFTER changing master password" );
     }
 }


