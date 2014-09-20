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

package org.glassfish.admin.rest.cli;

import com.sun.enterprise.config.serverbeans.AuthRealm;
import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.SecurityService;
import com.sun.enterprise.security.auth.login.LoginContextDriver;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Properties;



import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.internal.api.Globals;
import com.sun.enterprise.security.auth.realm.RealmsManager;
import com.sun.enterprise.security.auth.realm.Realm;
import com.sun.enterprise.security.auth.realm.User;

import java.util.Enumeration;

import org.jvnet.hk2.config.types.Property;

/**
 * AMX Realms implementation.
 * Note that realms don't load until {@link #loadRealms} is called.
 * @author ludovic champenosi
 */
public class SecurityUtil {
    private static final String DAS_CONFIG = "server-config";
    private static String ADMIN_REALM = "admin-realm";
    private static String FILE_REALM_CLASSNAME = "com.sun.enterprise.security.auth.realm.file.FileRealm";

    private Domain domain;

    public SecurityUtil(Domain domain) {
        this.domain = domain;
        _loadRealms();
    }

    public  RealmsManager getRealmsManager() {
        RealmsManager mgr = Globals.getDefaultHabitat().getService(RealmsManager.class);
        return mgr;
    }

    private SecurityService getSecurityService() {

        Config config = domain.getConfigs().getConfig().get(0);

        return config.getSecurityService();
    }

    private void _loadRealms() {


        List<AuthRealm> authRealmConfigs = getSecurityService().getAuthRealm();

        List<String> goodRealms = new ArrayList<String>();
        for (AuthRealm authRealm : authRealmConfigs) {
            List<Property> propConfigs = authRealm.getProperty();
            Properties props = new Properties();
            for (Property p : propConfigs) {
                String value = p.getValue();
                props.setProperty(p.getName(), value);
            }
            try {
                Realm.instantiate(authRealm.getName(), authRealm.getClassname(), props);
                goodRealms.add(authRealm.getName());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (!goodRealms.isEmpty()) {
            //not used String goodRealm = goodRealms.iterator().next();
            try {
                String defaultRealm = getSecurityService().getDefaultRealm();
                /*Realm r = */Realm.getInstance(defaultRealm);
                Realm.setDefaultRealm(defaultRealm);
            } catch (Exception e) {
                Realm.setDefaultRealm(goodRealms.iterator().next());
                e.printStackTrace();

            }
        }

    }

    private String[] _getRealmNames() {

        Enumeration<String> es = getRealmsManager().getRealmNames();
        List<String> l = new ArrayList<String>();
        while (es.hasMoreElements()) {
            l.add(es.nextElement());
        }
                return (String[])l.toArray(new String[l.size()]);

    }

    public String[] getRealmNames() {
        try {
            return _getRealmNames();
        } catch (Exception e) {
            e.printStackTrace();
            return null;

        }
    }

    public String[] getPredefinedAuthRealmClassNames() {
        List<String> items = getRealmsManager().getPredefinedAuthRealmClassNames();
        return (String[])items.toArray(new String[items.size()]);
    }

    public String getDefaultRealmName() {
        return getRealmsManager().getDefaultRealmName();
    }

    public void setDefaultRealmName(String realmName) {
        getRealmsManager().setDefaultRealmName(realmName);
    }

    private Realm getRealm(String realmName) {
        Realm realm = getRealmsManager().getFromLoadedRealms(realmName);
        if (realm == null) {
            throw new IllegalArgumentException("No such realm: " + realmName);
        }
        return realm;
    }

    public void addUser(
            String realmName,
            String user,
            String password,
            String[] groupList) {
        checkSupportsUserManagement(realmName);

        try {
            Realm realm = getRealm(realmName);
            realm.addUser(user, password.toCharArray(), groupList);
            realm.persist();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void updateUser(
            String realmName,
            String existingUser,
            String newUser,
            String password,
            String[] groupList) {
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

    public boolean supportsUserManagement(String realmName) {
        return getRealm(realmName).supportsUserManagement();
    }

    private void checkSupportsUserManagement(String realmName) {
        if (!supportsUserManagement(realmName)) {
            throw new IllegalStateException("Realm " + realmName + " does not support user management");
        }
    }

    public String[] getUserNames(String realmName) {
        try {

            Enumeration<String> es = getRealm(realmName).getUserNames();
            List<String> l = new ArrayList<String>();
            while (es.hasMoreElements()) {
                l.add(es.nextElement());
            }
                    return (String[])l.toArray(new String[l.size()]);


        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String[] getGroupNames(String realmName) {
        try {

            Enumeration<String> es = getRealm(realmName).getGroupNames();
            List<String> l = new ArrayList<String>();
            while (es.hasMoreElements()) {
                l.add(es.nextElement());
            }
        return (String[])l.toArray(new String[l.size()]);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    public String[] getGroupNames(String realmName, String user) {

        try {

            Enumeration<String> es = getRealm(realmName).getGroupNames(user);
            List<String> l = new ArrayList<String>();
            while (es.hasMoreElements()) {
                l.add(es.nextElement());
            }
        return (String[])l.toArray(new String[l.size()]);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }


    }

    public Map<String, Object> getUserAttributes(String realmName, String username) {
        try {
            User user = getRealm(realmName).getUser(username);
            Map<String, Object> m = new HashMap<String, Object>();
            Enumeration e = user.getAttributeNames();
            List<String> attrNames = new ArrayList<String>();
            while (e.hasMoreElements()) {
                attrNames.add((String)e.nextElement());
            }
            for (String attrName : attrNames) {
                m.put(attrName, user.getAttribute(attrName));
            }
            return m;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String getAnonymousUser(ServiceLocator habitat) {
        String user = null;
        // find the ADMIN_REALM
        AuthRealm adminFileAuthRealm = null;

        for (AuthRealm auth : domain.getConfigNamed(DAS_CONFIG).getSecurityService().getAuthRealm()) {
            if (auth.getName().equals(ADMIN_REALM)) {
                adminFileAuthRealm = auth;
                break;
            }
        }

        if (adminFileAuthRealm == null) {
            // There must always be an admin realm
            throw new IllegalStateException("Cannot find admin realm");
        }

        // Get FileRealm class name
        String fileRealmClassName = adminFileAuthRealm.getClassname();
        if (fileRealmClassName != null && !fileRealmClassName.equals(FILE_REALM_CLASSNAME)) {
            // This condition can arise if admin-realm is not a File realm. Then the API to extract
            // the anonymous user should be integrated for the logic below this line of code. for now,
            // we treat this as an error and instead of throwing exception return false;
            return null;
        }

        List<Property> props = adminFileAuthRealm.getProperty();
        

        Property keyfileProp = null;
        
        for (Property prop : props) {
            if ("file".equals(prop.getName())) {
                keyfileProp = prop;
            }
        }
        if (keyfileProp == null) {
            throw new IllegalStateException("Cannot find property 'file'");
        }
        String keyFile = keyfileProp.getValue();
        if (keyFile == null) {
            throw new IllegalStateException("Cannot find key file");
        }

        String[] usernames = getUserNames(adminFileAuthRealm.getName());
        if (usernames.length == 1) {
            try {
                habitat.getService(com.sun.enterprise.security.SecurityLifecycle.class);
                LoginContextDriver.login(usernames[0], new char[0], ADMIN_REALM);
                user = usernames[0];
            } catch (Exception e) {
            }
        }

        return user;
    }
}
