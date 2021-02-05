/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *    Copyright (c) 2019 Payara Foundation and/or its affiliates. All rights reserved.
 *
 *     The contents of this file are subject to the terms of either the GNU
 *     General Public License Version 2 only ("GPL") or the Common Development
 *     and Distribution License("CDDL") (collectively, the "License").  You
 *     may not use this file except in compliance with the License.  You can
 *     obtain a copy of the License at
 *     https://github.com/payara/Payara/blob/master/LICENSE.txt
 *     See the License for the specific
 *     language governing permissions and limitations under the License.
 *
 *     When distributing the software, include this License Header Notice in each
 *     file and include the License file at glassfish/legal/LICENSE.txt.
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
package fish.payara.test.containers.tools.security;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStore.Entry;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.KeyStore.ProtectionParameter;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Key Store manager. Wrapper for {@link KeyStore} with nicer API.
 *
 * @author David Matejcek
 */
public class KeyStoreManager implements Serializable {

    private static final long serialVersionUID = -7744494989999007759L;

    private static final Logger LOG = LoggerFactory.getLogger(KeyStoreManager.class);

    private final char[] password;
    private transient KeyStore store;


    /**
     * Loads an existing keystore from file or creates the empty file.
     *
     * @param keyStoreType
     * @param password
     * @throws KeyStoreManagerException
     */
    public KeyStoreManager(final KeyStoreType keyStoreType, final String password) throws KeyStoreManagerException {
        this((File) null, keyStoreType, password);
        LOG.trace("KeyStoreManager(keystore, storeType)");
    }


    /**
     * Loads an existing keystore from file or creates empty keystore.
     *
     * @param keystore
     * @param keyStoreType
     * @param password - storing a password in memory is not secure.
     * @throws KeyStoreManagerException
     */
    public KeyStoreManager(final File keystore, final KeyStoreType keyStoreType, final String password)
        throws KeyStoreManagerException {
        LOG.debug("KeyStoreManager(keystore={}, keyStoreType={}, password={})", keystore, keyStoreType, password);
        Objects.requireNonNull(keyStoreType, "keyStoreType");
        Objects.requireNonNull(password, "password");

        this.password = password.toCharArray();
        try {
            if (keystore == null) {
                this.store = load(null, keyStoreType, password);
                LOG.info("In memory empty keystore created.");
            } else {
                try (final FileInputStream in = new FileInputStream(keystore)) {
                    this.store = load(in, keyStoreType, password);
                    LOG.info("Keystore from file '{}' loaded.", keystore);
                }
            }
        } catch (final GeneralSecurityException | IOException e) {
            throw new KeyStoreManagerException("Cannot load the keystore.", e);
        }
    }


    /**
     * @return type of this keystore.
     */
    public KeyStoreType getKeyStoreType() {
        return KeyStoreType.valueOf(this.store.getType());
    }


    /**
     * Puts the key with the certificate chain into the keystore under the alias.
     *
     * @param alias
     * @param key
     * @param certificateChain - must not be null nor empty if the key is {@link PrivateKeyEntry}
     * @throws KeyStoreManagerException
     */
    public void putKey(final String alias, final Key key, final Certificate... certificateChain)
        throws KeyStoreManagerException {
        LOG.debug("putKey(alias={}, key, certificateChain)", alias);
        if (key instanceof PrivateKey && (certificateChain == null || certificateChain.length == 0)) {
            throw new KeyStoreManagerException(
                "Cannot put the key into the keystore, a certificate chain must be provided.");
        }
        try {
            this.store.setKeyEntry(alias, key, this.password, certificateChain);
        } catch (final KeyStoreException e) {
            throw new KeyStoreManagerException("Cannot put the key into the keystore.", e);
        }
    }


    /**
     * Puts the protected entry into the keystore under the alias.
     *
     * @param alias
     * @param entry
     * @param protection
     * @throws KeyStoreManagerException
     */
    public void putEntry(final String alias, final Entry entry, final ProtectionParameter protection)
        throws KeyStoreManagerException {
        LOG.debug("putEntry(alias={}, entry, protection)", alias);
        try {
            this.store.setEntry(alias, entry, protection);
        } catch (final KeyStoreException e) {
            throw new KeyStoreManagerException("Cannot put the entry into the keystore.", e);
        }
    }


    /**
     * Puts the trusted certificate into the keystore under the alias.
     *
     * @param alias
     * @param certificate
     * @throws KeyStoreManagerException
     */
    public void putTrusted(final String alias, final Certificate certificate) throws KeyStoreManagerException {
        LOG.debug("putTrusted(alias={}, certificate={})", alias, certificate);
        try {
            this.store.setCertificateEntry(alias, certificate);
        } catch (final KeyStoreException e) {
            throw new KeyStoreManagerException("Cannot put the certificate into the keystore.", e);
        }
    }


    /**
     * Look for the alias into the keystore and return the key if exists.
     *
     * @param <T> - expected type of the Key
     * @param alias
     * @return the key found under the alias.
     * @throws KeyStoreManagerException
     */
    public <T extends Key> T getKey(final String alias) throws KeyStoreManagerException {
        LOG.debug("getKey(alias={})", alias);
        try {
            @SuppressWarnings("unchecked")
            final T key = (T) this.store.getKey(alias, this.password);
            return key;
        } catch (final ClassCastException e) {
            throw new KeyStoreManagerException(
                String.format("The key under the alias '%s' is not compatible with the desired type.", alias), e);
        } catch (final GeneralSecurityException e) {
            throw new KeyStoreManagerException(
                String.format("The key under the alias '%s' could not be loaded.", alias), e);
        }
    }


    /**
     * Look for the alias into the keystore and return the entry if exists.
     *
     * @param <T> - expected type of the Key
     * @param alias
     * @param protection
     * @return the key found under the alias.
     * @throws KeyStoreManagerException
     */
    public <T extends Entry> T getEntry(final String alias, final ProtectionParameter protection)
        throws KeyStoreManagerException {
        LOG.debug("getEntry(alias={}, protection={})", alias, protection);
        try {
            @SuppressWarnings("unchecked")
            final T entry = (T) this.store.getEntry(alias, protection);
            return entry;
        } catch (final ClassCastException e) {
            final String message = //
                String.format("The entry under the alias '%s' is not compatible with the desired type.", alias);
            throw new KeyStoreManagerException(message, e);
        } catch (final UnsupportedOperationException e) {
            // invalid protection or method not suitable for the entry.
            final String message = String.format("Cannot retrieve an entry under the alias '%s'.", alias);
            throw new KeyStoreManagerException(message, e);
        } catch (final GeneralSecurityException e) {
            final String message = String.format("The entry under the alias '%s' could not be loaded.", alias);
            throw new KeyStoreManagerException(message, e);
        }
    }


    /**
     * @return a list of aliases in the keystore.
     * @throws KeyStoreManagerException
     */
    public List<String> getAliases() throws KeyStoreManagerException {
        LOG.debug("getAliases()");
        try {
            return Collections.list(this.store.aliases());
        } catch (final KeyStoreException e) {
            throw new KeyStoreManagerException("Cannot get aliases.", e);
        }
    }


  /**
   * Look for the alias into the keystore and return the certificates if exists.
   *
   * @param alias
   * @return an array of certificates
   * @throws KeyStoreManagerException if the chain cannot be retrieved
   */
  public Certificate[] getCertificateChain(final String alias) throws KeyStoreManagerException {
        LOG.debug("getCertificateChain(alias={})", alias);
        try {
            return this.store.getCertificateChain(alias);
        } catch (final KeyStoreException e) {
            final String message = String.format("Cannot retrieve a certificate chain under the alias '%s'.", alias);
            throw new KeyStoreManagerException(message, e);
        }
    }


    /**
     * Look for the alias into the keystore and return the certificates if exists.
     *
     * @param <T> {@link Certificate} type.
     * @param alias
     * @param clazz - a type of the output array element
     * @return an array of certificates or null there is no private key under the alias
     * @throws KeyStoreManagerException if the chain cannot be retrieved or some certificate cannot
     *             be casted to T.
     */
    public <T extends Certificate> T[] getCertificateChain(final String alias, final Class<T> clazz)
        throws KeyStoreManagerException {
        LOG.debug("getCertificateChain(alias={}, clazz={})", alias, clazz);
        final Certificate[] certificates = getCertificateChain(alias);
        if (certificates == null) {
            return null;
        }
        @SuppressWarnings("unchecked")
        final T[] typedCertificates = (T[]) Array.newInstance(clazz, certificates.length);
        for (int i = 0; i < certificates.length; i++) {
            try {
                typedCertificates[i] = clazz.cast(certificates[i]);
            } catch (final ClassCastException e) {
                final String message = String.format("The certificate under the alias '%s' is not an instance of '%s'.",
                    alias, clazz);
                throw new KeyStoreManagerException(message, e);
            }
        }

        return typedCertificates;
    }


    /**
     * @param <T> {@link Certificate} type.
     * @param alias
     * @return the first Certificate found under the alias.
     * @throws KeyStoreManagerException
     */
    public <T extends Certificate> T getCertificate(final String alias) throws KeyStoreManagerException {
        LOG.debug("getCertificate(alias={})", alias);
        try {
            @SuppressWarnings("unchecked")
            final T cert = (T) this.store.getCertificate(alias);
            return cert;
        } catch (final ClassCastException e) {
            final String message = String
                .format("The entry under the alias '%s' is not compatible with the desired type.", alias);
            throw new KeyStoreManagerException(message, e);
        } catch (final KeyStoreException e) {
            final String message = String.format("Cannot retrieve a certificate under the alias '%s'.", alias);
            throw new KeyStoreManagerException(message, e);
        }
    }


    /**
     * Saves the keystore to the file - if it does not exist, it will be created.
     *
     * @param file target file
     * @throws KeyStoreManagerException
     */
    public void save(final File file) throws KeyStoreManagerException {
        LOG.debug("save(file={})", file);
        FileOutputStream fos = null;
        try {
            if (!file.exists()) {
                LOG.info("Creating new keystore: {}", file);
            }
            fos = new FileOutputStream(file);
            this.store.store(fos, this.password);
            LOG.debug("Keystore saved.");
        } catch (final GeneralSecurityException | IOException e) {
            throw new KeyStoreManagerException("Cannot save the key store.", e);
        } finally {
            close(fos);
        }
    }

    /**
     * Saves the keystore to the byte array.
     * @return byte[]
     */
    public byte[] toBytes() {
        try (ByteArrayOutputStream stream = new ByteArrayOutputStream(8192)) {
            this.store.store(stream, password);
            return stream.toByteArray();
        } catch (final GeneralSecurityException | IOException e) {
            throw new KeyStoreManagerException("Cannot convert the key store to byte array.", e);
        }
    }


    private void close(final FileOutputStream object) {
        LOG.trace("close(object={})", object);
        if (object == null) {
            return;
        }
        try {
            object.close();
        } catch (final IOException e) {
            LOG.warn("Cannot close the object " + object, e);
        }
    }


    private void writeObject(final ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        out.writeObject(this.store.getType());
        try {
            this.store.store(out, this.password);
        } catch (KeyStoreException | NoSuchAlgorithmException | CertificateException e) {
            throw new IOException("Cannot serialize the keystore!", e);
        }
    }


    private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        final KeyStoreType type = KeyStoreType.valueOf((String) in.readObject());
        try {
            this.store = load(in, type, String.valueOf(this.password));
        } catch (final GeneralSecurityException | IllegalArgumentException e) {
            throw new IOException("Cannot deserialize the keystore!", e);
        }
    }


    private KeyStore load(final InputStream keystore, final KeyStoreType keyStoreType, final String passwd)
        throws GeneralSecurityException, IOException {
        final KeyStore ks = KeyStore.getInstance(keyStoreType.name());
        ks.load(keystore, passwd.toCharArray());
        return ks;
    }
}
