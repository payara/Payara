/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2011 Oracle and/or its affiliates. All rights reserved.
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
// Portions Copyright [2018-2021] [Payara Foundation and/or its affiliates]
package com.sun.enterprise.server.pluggable;

import com.sun.enterprise.security.ssl.impl.SecuritySupportImpl;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import javax.net.ssl.KeyManager;
import javax.net.ssl.TrustManager;
import org.jvnet.hk2.annotations.Contract;

/**
 * SecuritySupport is part of PluggableFeature that provides access to internal services managed by application server.
 * 
 * <p>
 * SecuritySupport deals with loading, caching and providing access to key stores and trust stores, including their managers.
 * 
 * <p>
 * This is mainly used via the <code>com.sun.enterprise.security.ssl.SSLUtils</code> facade, though various other classes
 * such as the <code>BaseContainerCallbackHandler</code> use this directly.
 * 
 * @author Shing Wai Chan
 */
@Contract
public abstract class SecuritySupport {

    public static final String KEYSTORE_PASS_PROP = "javax.net.ssl.keyStorePassword";
    public static final String TRUSTSTORE_PASS_PROP = "javax.net.ssl.trustStorePassword";
    public static final String KEYSTORE_TYPE_PROP = "javax.net.ssl.keyStoreType";
    public static final String TRUSTSTORE_TYPE_PROP = "javax.net.ssl.trustStoreType";
    public static final String keyStoreProp = "javax.net.ssl.keyStore";
    public static final String additionalKeyStoreProp = "fish.payara.ssl.additionalKeyStores";
    public static final String trustStoreProp = "javax.net.ssl.trustStore";
    public static final String additionalTrustStoreProp = "fish.payara.ssl.additionalTrustStores";

    private static volatile SecuritySupport defaultInstance = null;

    public static SecuritySupport getDefaultInstance() {
        if (defaultInstance == null) {
            defaultInstance = new SecuritySupportImpl();
        }
        return defaultInstance;
    }

    /**
     * This method returns an array of keystores containing keys and certificates.
     */
    abstract public KeyStore[] getKeyStores();

    /**
     * This method returns an array of truststores containing certificates.
     */
    abstract public KeyStore[] getTrustStores();

    /**
     * @param token
     * @return a keystore. If token is null, return the the first keystore.
     */
    abstract public KeyStore getKeyStore(String token);

    /**
     * @param token
     * @return a truststore. If token is null, return the first truststore.
     */
    abstract public KeyStore getTrustStore(String token);

    /**
     * @param algorithm
     * @return KeyManagers for the specified algorithm.
     * @throws IOException
     * @throws KeyStoreException
     * @throws NoSuchAlgorithmException
     * @throws UnrecoverableKeyException
     */
    abstract public KeyManager[] getKeyManagers(String algorithm) throws IOException, KeyStoreException, NoSuchAlgorithmException, UnrecoverableKeyException;

    /**
     * @param algorithm
     * @return TrustManagers for the specified algorithm.
     * @throws IOException
     * @throws KeyStoreException
     * @throws NoSuchAlgorithmException
     */
    abstract public TrustManager[] getTrustManagers(String algorithm) throws IOException, KeyStoreException, NoSuchAlgorithmException;
    
    /**
     * Resets the security instance by effectively re-initializing it.
     * 
     * <p>
     * This means the default keystores and truststores will be reloaded from their default locations (which may be configured
     * by system properties, such as with the default SecuritySupport instance).
     */
    public void reset() {
        // Do nothing by default
    }
    
    /**
     * @param type
     * @param index
     * @return load a null keystore of given type.
     */
    abstract public KeyStore loadNullStore(String type, int index) throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException;

    /**
     * @param masterPass
     * @return result whether the given master password is correct.
     */
    abstract public boolean verifyMasterPassword(final char[] masterPass);

    /**
     * Gets the PrivateKey for specified alias from the corresponding keystore indicated by the index.
     *
     * @param alias Alias for which the PrivateKey is desired.
     * @param keystoreIndex Index of the keystore.
     * @return
     * @throws KeyStoreException
     * @throws NoSuchAlgorithmException
     * @throws UnrecoverableKeyException
     */
    abstract public PrivateKey getPrivateKeyForAlias(String alias, int keystoreIndex) throws KeyStoreException, NoSuchAlgorithmException, UnrecoverableKeyException;

    /**
     * This method returns an array of token names in order corresponding to array of keystores.
     */
    abstract public String[] getTokenNames();

    /**
     * This method synchronize key file for given realm.
     * 
     * @param config the ConfigContextx
     * @param fileRealmName
     * @exception if fail to synchronize, a known exception is
     * com.sun.enterprise.ee.synchronization.SynchronizationException
     */
    /** TODO:V3:Cluster ConfigContext is no longer present so find out what this needs to be */
    abstract public void synchronizeKeyFile(Object configContext, String fileRealmName) throws Exception;

    /**
     * Check permission for the given key.
     * 
     * @param key
     */
    abstract public void checkPermission(String key);

}
