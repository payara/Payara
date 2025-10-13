/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
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
// Portions Copyright [2017-2019] [Payara Foundation and/or its affiliates]
package org.glassfish.admin.amx.impl.mbean;

import static java.util.logging.Level.WARNING;
import static org.glassfish.admin.amx.util.CollectionUtil.toArray;
import static org.glassfish.admin.amx.util.ListUtil.newList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;

import javax.management.ObjectName;

import org.glassfish.admin.amx.base.Realms;
import org.glassfish.admin.amx.impl.util.InjectedValues;
import org.glassfish.admin.amx.util.AMXLoggerInfo;
import org.glassfish.admin.amx.util.SetUtil;
import org.glassfish.admin.amx.util.StringUtil;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.internal.api.Globals;
import org.jvnet.hk2.config.types.Property;

import com.sun.enterprise.config.serverbeans.AuthRealm;
import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.SecurityService;
import com.sun.enterprise.security.SecurityLifecycle;
import com.sun.enterprise.security.auth.WebAndEjbToJaasBridge;
import com.sun.enterprise.security.auth.realm.Realm;
import com.sun.enterprise.security.auth.realm.RealmsManager;
import com.sun.enterprise.security.auth.realm.User;

/**
 * AMX Realms implementation. Note that realms don't load until {@link #loadRealms} is called.
 */
public final class RealmsImpl extends AMXImplBase {
    
    private static final String ADMIN_REALM = "admin-realm";
    private static final String FILE_REALM_CLASSNAME = "com.sun.enterprise.security.auth.realm.file.FileRealm";
    
    private volatile boolean realmsLoaded;
    
    public RealmsImpl(final ObjectName containerObjectName) {
        super(containerObjectName, Realms.class);
    }

    public static RealmsManager getRealmsManager() {
        return Globals.getDefaultHabitat().getService(RealmsManager.class);
    }

    private SecurityService getSecurityService() {
        return InjectedValues.getInstance().getHabitat().getService(SecurityService.class, ServerEnvironment.DEFAULT_INSTANCE_NAME);
    }

    private List<AuthRealm> getAuthRealms() {
        return getSecurityService().getAuthRealm();
    }

    /** realm names as found in configuration; some might be defective and unable to be loaded */
    private Set<String> getConfiguredRealmNames() {
        Set<String> names = new HashSet<String>();
        for (AuthRealm realm : getAuthRealms()) {
            names.add(realm.getName());
        }
        
        return names;
    }

    private synchronized void loadRealms() {
        if (realmsLoaded) {
            final Set<String> loaded = SetUtil.newStringSet(_getRealmNames());
            if (getConfiguredRealmNames().equals(loaded)) {
                return;
            }
            // reload: there are new or different realms
            realmsLoaded = false;
        }

        _loadRealms();
    }

    private void _loadRealms() {
        if (realmsLoaded)
            throw new IllegalStateException();

        final List<AuthRealm> authRealms = getAuthRealms();

        final List<String> goodRealms = new ArrayList<String>();
        for (final AuthRealm authRealm : authRealms) {
            final List<Property> propList = authRealm.getProperty();
            final Properties props = new Properties();
            for (final Property p : propList) {
                props.setProperty(p.getName(), p.getValue());
            }
            try {
                Realm.instantiate(authRealm.getName(), authRealm.getClassname(), props);
                goodRealms.add(authRealm.getName());
            } catch (final Exception e) {
                AMXLoggerInfo.getLogger().log(WARNING, AMXLoggerInfo.cantInstantiateRealm,
                        new Object[] { StringUtil.quote(authRealm), e.getLocalizedMessage() });
            }
        }

        if (!goodRealms.isEmpty()) {
            String goodRealm = goodRealms.iterator().next();
            try {
                String defaultRealm = getSecurityService().getDefaultRealm();
                Realm.getInstance(defaultRealm);
                Realm.setDefaultRealm(defaultRealm);
            } catch (final Exception e) {
                AMXLoggerInfo.getLogger().log(WARNING, AMXLoggerInfo.cantInstantiateRealm,
                        new Object[] { StringUtil.quote(goodRealm), e.getLocalizedMessage() });
                Realm.setDefaultRealm(goodRealms.iterator().next());
            }
        }

        realmsLoaded = true;
    }

    private String[] _getRealmNames() {
        return toArray(newList(getRealmsManager().getRealmNames()), String.class);
    }

    public String[] getRealmNames() {
        try {
            loadRealms();
            return _getRealmNames();
        } catch (final Exception e) {
            AMXLoggerInfo.getLogger().log(Level.WARNING, AMXLoggerInfo.cantGetRealmNames, e.getLocalizedMessage());
            return new String[] {};
        }
    }

    public String[] getPredefinedAuthRealmClassNames() {
        return toArray(getRealmsManager().getPredefinedAuthRealmClassNames(), String.class);
    }

    public String getDefaultRealmName() {
        return getRealmsManager().getDefaultRealmName();
    }

    public void setDefaultRealmName(final String realmName) {
        getRealmsManager().setDefaultRealmName(realmName);
    }

    private Realm getRealm(String realmName) {
        loadRealms();
        Realm realm = getRealmsManager().getFromLoadedRealms(realmName);
        if (realm == null) {
            throw new IllegalArgumentException("No such realm: " + realmName);
        }
        
        return realm;
    }

    public void addUser(String realmName, String user, String password, String[] groupList) {
        checkSupportsUserManagement(realmName);

        try {
            Realm realm = getRealm(realmName);
            realm.addUser(user, password.toCharArray(), groupList);
            realm.persist();
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void updateUser(String realmName, String existingUser, String newUser, String password, String[] groupList) {
        checkSupportsUserManagement(realmName);

        try {
            Realm realm = getRealm(realmName);
            realm.updateUser(existingUser, newUser, password.toCharArray(), groupList);
            realm.persist();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void removeUser(String realmName, String user) {
        checkSupportsUserManagement(realmName);

        try {
            Realm realm = getRealm(realmName);
            realm.removeUser(user);
            realm.persist();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public boolean supportsUserManagement(final String realmName) {
        return getRealm(realmName).supportsUserManagement();
    }

    private void checkSupportsUserManagement(final String realmName) {
        if (!supportsUserManagement(realmName)) {
            throw new IllegalStateException("Realm " + realmName + " does not support user management");
        }
    }

    public String[] getUserNames(final String realmName) {
        try {
            return toArray(newList(getRealm(realmName).getUserNames()), String.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String[] getGroupNames(final String realmName) {
        try {
            return toArray(newList(getRealm(realmName).getGroupNames()), String.class);
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String[] getGroupNames(String realmName, String user) {
        try {
            return toArray(newList(getRealm(realmName).getGroupNames(user)), String.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Map<String, Object> getUserAttributes(final String realmName, final String username) {
        try {
            User user = getRealm(realmName).getUser(username);
            Map<String, Object> userAttributes = new HashMap<String, Object>();
            for (String attrName : newList(user.getAttributeNames())) {
                userAttributes.put(attrName, user.getAttribute(attrName));
            }
            
            return userAttributes;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String getAnonymousUser() {
        Domain domain = InjectedValues.getInstance().getHabitat().getService(Domain.class);
        List<Config> configs = domain.getConfigs().getConfig();

        // find the ADMIN_REALM
        AuthRealm adminFileAuthRealm = null;
        for (Config config : configs) {
            if (config.getSecurityService() == null)
                continue;

            for (AuthRealm auth : config.getSecurityService().getAuthRealm()) {
                if (auth.getName().equals(ADMIN_REALM)) {
                    adminFileAuthRealm = auth;
                    break;
                }
            }
        }
        if (adminFileAuthRealm == null) {
            // There must always be an admin realm
            throw new IllegalStateException("Cannot find admin realm");
        }

        // Get FileRealm class name
        String fileRealmClassName = adminFileAuthRealm.getClassname();
        if (!fileRealmClassName.equals(FILE_REALM_CLASSNAME)) {
            // This condition can arise if admin-realm is not a File realm. Then the API to extract
            // the anonymous user should be integrated for the logic below this line of code. for now,
            // we treat this as an error and instead of throwing exception return false;
            return null;
        }

        Property keyfileProp = adminFileAuthRealm.getProperty("file");
        if (keyfileProp == null) {
            throw new IllegalStateException("Cannot find property 'file'");
        }
        
        String keyFile = keyfileProp.getValue();
        if (keyFile == null) {
            throw new IllegalStateException("Cannot find key file");
        }

        String user = null;
        String[] usernames = getUserNames(adminFileAuthRealm.getName());
        if (usernames.length == 1) {
            try {
                InjectedValues.getInstance().getHabitat().getService(SecurityLifecycle.class);
                WebAndEjbToJaasBridge.login(usernames[0], new char[0], ADMIN_REALM);
                user = usernames[0];
            } catch (final Exception e) {
                // 
            }
        }

        return user;
    }

}
