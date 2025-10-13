/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyright [2018-2020] [Payara Foundation and/or its affiliates]
 */
package com.sun.enterprise.admin.servermgmt.domain;

import static com.sun.enterprise.admin.servermgmt.domain.DomainConstants.KEYSTORE_FILE;
import static com.sun.enterprise.admin.servermgmt.domain.DomainConstants.TRUSTSTORE_FILE;

import java.io.File;
import java.io.IOException;

import org.glassfish.security.common.FileRealmStorageManager;

import com.sun.enterprise.admin.servermgmt.DomainConfig;
import com.sun.enterprise.admin.servermgmt.MasterPasswordFileManager;
import com.sun.enterprise.admin.servermgmt.RepositoryException;
import com.sun.enterprise.security.store.PasswordAdapter;
import com.sun.enterprise.util.i18n.StringManager;

public class DomainSecurity extends MasterPasswordFileManager {

    private static final StringManager STRING_MANAGER = StringManager.getManager(DomainSecurity.class);

    /**
     * Modifies the contents of given keyfile with administrator's user-name and password. Uses the FileRealm classes that
     * application server's Runtime uses.
     *
     * @param keyFile File to store encrypted admin credentials.
     * @param user Username.
     * @param password Password.
     */
    void processAdminKeyFile(File keyFile, String user, String password, String[] adminUserGroups) throws IOException {
        FileRealmStorageManager fileStorageManager = new FileRealmStorageManager(keyFile.getAbsolutePath());
        fileStorageManager.addUser(user, password.toCharArray(), adminUserGroups);
        fileStorageManager.persist();
    }

    /**
     * Create the password alias keystore (initially empty)
     *
     * @param passwordFile File to store encrypted password.
     * @param password password protecting the keystore
     * @throws RepositoryException if any error occurs in creation.
     */
    void createPasswordAliasKeystore(File passwordFile, String password) throws RepositoryException {
        try {
            new PasswordAdapter(passwordFile.getAbsolutePath(), password.toCharArray()).writeStore();
        } catch (Exception ex) {
            throw new RepositoryException(STRING_MANAGER.getString("passwordAliasKeystoreNotCreated", passwordFile), ex);
        }
    }

    /**
     * Create the default SSL key store using keytool to generate a self signed certificate.
     *
     * @param configRoot Config directory.
     * @param config A {@link DomainConfig} object
     * @param masterPassword Master password.
     * @throws RepositoryException if any error occurs during keystore creation.
     */
    void createSSLCertificateDatabase(File configDir, DomainConfig config, String masterPassword) throws RepositoryException {
        File trustStore = new File(configDir, TRUSTSTORE_FILE);
        File keyStore = new File(configDir, KEYSTORE_FILE);
        
        createKeyStore(keyStore, config, masterPassword);
        changeKeyStorePassword(DEFAULT_MASTER_PASSWORD, masterPassword, trustStore);
        copyCertificates(keyStore, trustStore, config, masterPassword);
        updateCertificates(trustStore, masterPassword);
    }

    /**
     * Change the permission for a given file/directory.
     * <p>
     * <b>NOTE:</b> Applicable only for Unix env.
     * </p>
     *
     * @param args New sets of permission arguments.
     * @param file File on which permission has to be applied.
     * @throws IOException If any IO error occurs during operation.
     */
    void changeMode(String args, File file) throws IOException {
        super.chmod(args, file);
    }
}
