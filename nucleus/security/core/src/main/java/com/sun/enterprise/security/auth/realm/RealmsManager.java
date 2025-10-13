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
package com.sun.enterprise.security.auth.realm;

import static com.sun.enterprise.security.SecurityLoggerInfo.noRealmsError;
import static com.sun.enterprise.security.auth.realm.Realm.RI_DEFAULT;
import static java.util.Collections.enumeration;
import static java.util.Collections.synchronizedMap;
import static java.util.logging.Level.SEVERE;
import static java.util.stream.Collectors.toList;
import static org.glassfish.hk2.utilities.BuilderHelper.createContractFilter;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.internal.api.Globals;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.types.Property;

import com.sun.enterprise.config.serverbeans.AuthRealm;
import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.SecurityService;
import com.sun.enterprise.security.SecurityLoggerInfo;

/**
 *
 * @author kumar.jayanti
 */
@Service
@Singleton
public class RealmsManager {
    
    private static final Logger LOGGER = SecurityLoggerInfo.getLogger();
    
    private static final String DEFAULT_DIGEST_ALGORITHM = "default-digest-algorithm";
    
    // Keep track of name of default realm for this domain. This is updated during startup
    // using value from server.xml
    private volatile String defaultRealmName = "default";
    
    // Per domain list of loaded Realms
    private final Map<String, Map<String, Realm>> loadedRealms = synchronizedMap(new HashMap<>());
    
    private String defaultDigestAlgorithm;

    @Inject
    @Named(ServerEnvironment.DEFAULT_INSTANCE_NAME)
    private Config config;

    private final RealmsProbeProvider probeProvider = new RealmsProbeProvider();

    

    // ### Public methods
    
    
    public synchronized String getDefaultRealmName() {
        return defaultRealmName;
    }

    public synchronized void setDefaultRealmName(String defaultRealmName) {
        this.defaultRealmName = defaultRealmName;
    }
    
    public void createRealms() {
        createRealms(config.getSecurityService(), config);
    }

    public void createRealms(Config config) {
        if (config == null) {
            return;
        }
        
        createRealms(config.getSecurityService(), config);
    }
    
    public String getDefaultDigestAlgorithm() {
        return defaultDigestAlgorithm;
    }
    
    /**
     * Checks if the given realm name is loaded/valid.
     * 
     * @param name of the realm to check.
     * @return true if realm present, false otherwise.
     */
    public boolean isValidRealm(String name) {
        if (name == null) {
            return false;
        } 
        
        return configContainsRealm(name, config.getName());
    }

    /**
     * Checks if the given realm name is loaded/valid.
     * 
     * @param name of the realm to check.
     * @return true if realm present, false otherwise.
     */
    public boolean isValidRealm(String configName, String name) {
        if (name == null) {
            return false;
        }
        
        return configContainsRealm(name, configName);
    }

    /**
     * Returns the names of accessible realms.
     * 
     * @return set of realm names
     */
    public Enumeration<String> getRealmNames() {
        return enumeration(getRealmNames(config.getName()));
    }

    public Realm getFromLoadedRealms(String realmName) {
        return configGetRealmInstance(config.getName(), realmName);
    }

    public Realm getFromLoadedRealms(String configName, String realmName) {
        return configGetRealmInstance(configName, realmName);
    }

    /**
     * Returns names of predefined AuthRealms' classes supported by security service.
     * 
     * @returns list of predefined AuthRealms' classes
     */
    public List<String> getPredefinedAuthRealmClassNames() {
        return 
            Globals.getDefaultHabitat()
                   .getDescriptors(createContractFilter(Realm.class.getName()))
                   .stream()
                   .map(e -> e.getImplementation())
                   .collect(toList());
    }

    public void putIntoLoadedRealms(String configName, String realmName, Realm realm) {
        Map<String, Realm> containedRealms = loadedRealms.get(configName);
        if (containedRealms == null) {
            containedRealms = new ConcurrentHashMap<>();
            if (configName == null) { // Is "null" really a valid key above?
                configName = config.getName();
            }
            loadedRealms.put(configName, containedRealms);
        }
        
        containedRealms.put(realmName, realm);
    }
    
    public void refreshRealm(String configName, String realmName) {
        if (realmName != null && !realmName.isEmpty()) {
            try {
                Realm realm = Realm.getInstance(configName, realmName);

                if (realm != null) {
                    realm.refresh(configName);
                }
            } catch (NoSuchRealmException | BadRealmException nre) {
                // Do nothing
            }
        }
    }
    
    public void removeFromLoadedRealms(String realmName) {
        Realm realm = removeFromLoadedRealms(config.getName(), realmName);
        if (realm != null) {
            probeProvider.realmRemovedEvent(realmName);
        }
    }
    
    public Realm removeFromLoadedRealms(String configName, String realmName) {
        Map<String, Realm> containedRealms = loadedRealms.get(configName);
        if (containedRealms == null) {
            return null;
        }
        
        return containedRealms.remove(realmName);
    }
    
    
    
    // ### Default access methods
    
    
    Realm getInstance(String name) {
        return getInstance(config.getName(), name);
    }
    
    Realm getInstance(String configName, String name) {
        Realm retval = configGetRealmInstance(configName, name);

        // Some tools as well as numerous other locations assume that
        // getInstance("default") always works; keep them from breaking
        // until code can be properly cleaned up. 4628429

        // Also note that for example the appcontainer will actually create
        // a Subject always containing realm='default' so this notion
        // needs to be fixed/handled.
        if (retval == null && RI_DEFAULT.equals(name)) {
            retval = configGetRealmInstance(configName, getDefaultRealmName());
        }

        return retval;
    }
    
    void putIntoLoadedRealms(String realmName, Realm realm) {
        putIntoLoadedRealms(config.getName(), realmName, realm);
        probeProvider.realmAddedEvent(realmName);
    }

    
    
    // ### Private methods
    
    
    /**
     * Load all configured realms from server.xml and initialize each one. Initialization is done by calling
     * Realm.initialize() with its name, class and properties. The name of the default realm is also saved in the Realm
     * class for reference during server operation.
     *
     * <P>
     * This method supersedes the RI RealmManager.createRealms() method.
     *
     */
    private void createRealms(SecurityService securityBean, Config cfg) {
        // Check if realms are already loaded by admin GUI ?
        if (realmsAlreadyLoaded(cfg.getName())) {
            return;
        }

        setDefaultDigestAlgorithm();
        
        try {
            LOGGER.fine("Initializing configured realms from SecurityService in Domain.xml....");

            if (securityBean == null) {
                securityBean = cfg.getSecurityService();
                assert (securityBean != null);
            }

            // grab default realm name
            String defaultRealm = securityBean.getDefaultRealm();

            // get set of auth-realms and process each
            List<AuthRealm> realms = securityBean.getAuthRealm();
            assert (realms != null);

            RealmConfig.createRealms(defaultRealm, realms, cfg.getName());

        } catch (Exception e) {
            LOGGER.log(SEVERE, noRealmsError, e);
        }
    }
    
    private void setDefaultDigestAlgorithm() {
        SecurityService service = config.getSecurityService();
        if (service == null) {
            return;
        }
        
        List<Property> props = service.getProperty();
        if (props == null) {
            return;
        }
        
        Iterator<Property> propsIterator = props.iterator();
        while (propsIterator != null && propsIterator.hasNext()) {
            Property prop = propsIterator.next();
            if (prop != null && DEFAULT_DIGEST_ALGORITHM.equals(prop.getName())) {
                defaultDigestAlgorithm = prop.getValue();
                break;
            }
        }
    }
    
    private boolean realmsAlreadyLoaded(String cfgName) {
        Set<String> realmNames = getRealmNames(cfgName);
        
        return realmNames != null && !realmNames.isEmpty();
    }

    private boolean configContainsRealm(String name, String configName) {
        Map<String, Realm> containedRealms = loadedRealms.get(configName);
        
        return containedRealms != null && containedRealms.containsKey(name);
    }

    private Set<String> getRealmNames(String configName) {
        Map<String, Realm> containedRealms = loadedRealms.get(configName);
        if (containedRealms == null) {
            return null;
        }
        
        return containedRealms.keySet();
    }

    private Realm configGetRealmInstance(String configName, String realm) {
        Map<String, Realm> containedRealms = loadedRealms.get(configName);
        if (containedRealms == null) {
            return null;
        }
        
        return containedRealms.get(realm);
    }
}
