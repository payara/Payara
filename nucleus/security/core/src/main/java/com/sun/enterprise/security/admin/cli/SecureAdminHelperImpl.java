/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright (c) 2011-2013 Oracle and/or its affiliates. All rights reserved.
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
package com.sun.enterprise.security.admin.cli;

import com.sun.enterprise.config.serverbeans.AdminService;
import com.sun.enterprise.config.serverbeans.AuthRealm;
import com.sun.enterprise.config.serverbeans.SecureAdminHelper;
import com.sun.enterprise.security.auth.realm.BadRealmException;
import com.sun.enterprise.security.auth.realm.NoSuchRealmException;
import com.sun.enterprise.security.auth.realm.NoSuchUserException;
import com.sun.enterprise.security.auth.realm.file.FileRealm;
import com.sun.enterprise.security.auth.realm.file.FileRealmUser;
import com.sun.enterprise.security.ssl.SSLUtils;
import com.sun.enterprise.security.store.DomainScopedPasswordAliasStore;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import org.glassfish.api.admin.ServerEnvironment;
import javax.inject.Inject;
import javax.inject.Named;


import org.jvnet.hk2.annotations.Service;
import org.glassfish.hk2.api.PerLookup;

/**
 * Various utility methods which support secure admin operations.
 * 
 * @author Tim Quinn
 */
@Service
@PerLookup
public class SecureAdminHelperImpl implements SecureAdminHelper {

    private static final char[] emptyPassword = new char[0];
    private final static String DOMAIN_ADMIN_GROUP_NAME = "asadmin";
    

    @Inject
    private SSLUtils sslUtils;
    
    @Inject
    private DomainScopedPasswordAliasStore domainPasswordAliasStore;
    
    @Inject @Named(ServerEnvironment.DEFAULT_INSTANCE_NAME)
    private volatile AdminService as;

    /**
     * Returns the correct DN to use for a given secure admin principal, mapping
     * the alias (if it's an alias specified) to the DN for the corresponding 
     * cert in the key store.
     * 
     * @param value user-provided value (alias name or the actual DN)
     * @param isAlias whether the value is an alias
     * @return DN to use 
     * @throws IOException if there is an error accessing the key store
     * @throws KeyStoreException if the keystore has not been initialized
     * @throws IllegalArgumentException if the cert for the specified alias as fetched from the key store is not an X509 certificate
     */
    @Override
    public String getDN(final String value, final boolean isAlias) throws IOException, KeyStoreException {
        if (isAlias) {
            final KeyStore keyStore = sslUtils.getKeyStore();
            if (keyStore == null) {
                throw new RuntimeException(Strings.get("noKeyStore"));
            }
            final Certificate cert = keyStore.getCertificate(value);
            if (cert == null) {
                throw new IllegalArgumentException(Strings.get("noAlias", value));
            }
            if ( ! (cert instanceof X509Certificate)) {
                throw new IllegalArgumentException(Strings.get("certNotX509Certificate", value));
            }
            return (((X509Certificate) cert).getSubjectX500Principal().getName());
        } else {
            return value;
        }
    }

    /**
     * Makes sure the username is a valid admin username and that the password
     * alias is defined.  This method does NOT make sure that the password
     * associated with the username and the password associated with the 
     * password alias are the same.
     * 
     * @param username user-provided username
     * @param passwordAlias name of the password alias 
     */
    @Override
    public void validateInternalUsernameAndPasswordAlias(String username, String passwordAlias) {
        try {
            validateUser(username);
            validatePasswordAlias(passwordAlias);
        } catch (Exception ex) {
            throw new RuntimeException(Strings.get("errVal"), ex);
        }
    }
    
    private void validateUser(final String username) throws BadRealmException, NoSuchRealmException {
        final FileRealm fr = adminRealm();
        try {
            FileRealmUser fru = (FileRealmUser)fr.getUser(username);
            if (isInAdminGroup(fru)) {
                return;
            }
            /*
             * The user is valid but is not in the admin group.
             */
            throw new RuntimeException(Strings.get("notAdminUser", username));
        } catch (NoSuchUserException ex) {
            /*
             * The user is not valid, but use the same error as if the user
             * IS present but is not an admin user.  This provides a would-be
             * intruder a little less information by not distinguishing 
             * between a valid user that's not an admin user and an
             * invalid user.
             */
            throw new RuntimeException(Strings.get("notAdminUser", username));
        }
    }
    
    private boolean isInAdminGroup(final FileRealmUser user) {
        for (String group : user.getGroups()) {
            if (group.equals(DOMAIN_ADMIN_GROUP_NAME)) {
                return true;
            }
        }
        return false;
    }
    
    private void validatePasswordAlias(final String passwordAlias) 
            throws CertificateException, NoSuchAlgorithmException, 
            KeyStoreException, NoSuchAlgorithmException, IOException {
            
        if ( ! domainPasswordAliasStore.containsKey(passwordAlias)) {
            throw new RuntimeException(Strings.get("noAlias", passwordAlias));
        }
    }
    private FileRealm adminRealm() throws BadRealmException, NoSuchRealmException {
        final AuthRealm ar = as.getAssociatedAuthRealm();
        if (FileRealm.class.getName().equals(ar.getClassname())) {
            String adminKeyFilePath = ar.getPropertyValue("file");
            FileRealm fr = new FileRealm(adminKeyFilePath);
            return fr;
        }
        return null;
    }
    
    /**
     * Returns whether at least one admin user has an empty password.
     * 
     * @return true if at least one admin user has an empty password; false otherwise 
     * @throws BadRealmException
     * @throws NoSuchRealmException
     * @throws NoSuchUserException 
     */
    @Override
    public boolean isAnyAdminUserWithoutPassword() throws Exception {
        final FileRealm adminRealm = adminRealm();
        /*
         * If the user has configured the admin realm to use a realm other than 
         * the default file realm bypass the check that makes sure no admin users have
         * an empty password.
         */
        if (adminRealm == null) {
            return false;
        }
        for (final Enumeration<String> e = adminRealm.getUserNames(); e.hasMoreElements(); ) {
            final String username = e.nextElement();
            /*
                * Try to authenticate this user with an empty password.  If it 
                * works we can stop.
                */
            final String[] groupNames = adminRealm.authenticate(username, emptyPassword);
            if (groupNames != null) {
                for (String groupName : groupNames) {
                    if (DOMAIN_ADMIN_GROUP_NAME.equals(groupName)) {
                        return true;
                    }
                }
            }
                    
        }
        return false;
    }
}
