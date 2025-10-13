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
// Portions Copyright 2018-2023 Payara Foundation and/or affiliates

package com.sun.enterprise.security.auth.realm;

import com.sun.enterprise.loader.CurrentBeforeParentClassLoader;
import static com.sun.enterprise.security.SecurityLoggerInfo.realmCreated;
import static com.sun.enterprise.security.SecurityLoggerInfo.realmDeleted;
import static com.sun.enterprise.security.SecurityLoggerInfo.realmUpdated;
import static com.sun.enterprise.security.auth.realm.RealmsManagerStore.getRealmsManager;
import static com.sun.enterprise.security.auth.realm.RealmsManagerStore.tryGetRealmsManager;
import static java.util.logging.Level.FINER;
import static java.util.logging.Level.INFO;
import static org.glassfish.external.probe.provider.PluginPoint.SERVER;

import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.Properties;
import java.util.logging.Logger;

import org.glassfish.external.probe.provider.StatsProviderManager;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.internal.api.ClassLoaderHierarchy;
import org.glassfish.internal.api.Globals;
import org.jvnet.hk2.annotations.Contract;

import com.sun.enterprise.security.SecurityLoggerInfo;
import com.sun.enterprise.util.LocalStringManagerImpl;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * @author Harish Prabandham
 * @author Harpreet Singh
 * @author Jyri Virkki
 * @author Shing Wai Chan
 *
 * @see java.security.Principal
 */
@Contract
public abstract class Realm extends AbstractStatefulRealm implements Comparable<Realm> {
    /**
     * Recommended property for keeping JAAS Context of a realm.
     *
     * @see #getJAASContext()
     */
    public static final String JAAS_CONTEXT_PARAM = "jaas-context";

    protected static final Logger _logger = SecurityLoggerInfo.getLogger();
    private static LocalStringManagerImpl localStrings = new LocalStringManagerImpl(Realm.class);

    private static RealmStatsProvider realmStatsProvier;

    // Keep a mapping from "default" to default realm (if no such named
    // realm is present) for the sake of all the hardcoded accesses to it.
    // This needs to be removed as part of RI security service cleanup.
    public static final String RI_DEFAULT = "default";


    // ---[ Public Static methods ]------------------------------------------------


    /**
     * Instantiate a Realm with the given name and properties using the Class name given.
     *
     * @param name Name of the new realm.
     * @param className Java Class name of the realm to create.
     * @param props Properties containing values of the Property element from server.xml
     * @return Reference to the new Realm. The Realm class keeps an internal list of all
     *         instantiated realms.
     * @throws BadRealmException If the requested realm cannot be instantiated.
     */
    public static synchronized Realm instantiate(String name, String className, Properties props) throws BadRealmException {
        // Register the realm provider
        registerRealmStatsProvier();

        Realm realmClass = _getInstance(name);
        if (realmClass == null) {
            realmClass = doInstantiate(name, className, props);
            getRealmsManager().putIntoLoadedRealms(name, realmClass);
        }

        return realmClass;
    }

    /**
     * Instantiate a Realm with the given name and properties using the Class name given.
     *
     * @param name Name of the new realm.
     * @param className Java Class name of the realm to create.
     * @param props Properties containing values of the Property element from server.xml
     * @param configName the config to which this realm belongs
     *
     * @returns Reference to the new Realm. The Realm class keeps an internal list of all instantiated
     * realms.
     * @throws BadRealmException If the requested realm cannot be instantiated.
     *
     */
    public static synchronized Realm instantiate(String name, String className, Properties props, String configName) throws BadRealmException {
        // Register the realm provider
        registerRealmStatsProvier();

        Realm realmClass = _getInstance(configName, name);
        if (realmClass == null) {
            realmClass = doInstantiate(name, className, props);
            getRealmsManager().putIntoLoadedRealms(configName, name, realmClass);
        }

        return realmClass;
    }

    /**
     * Convenience method which returns the Realm object representing the current default realm.
     * Equivalent to getInstance(getDefaultRealm()).
     *
     * @return Realm representing default realm.
     * @throws NoSuchRealmException if default realm does not exist
     */
    public static synchronized Realm getDefaultInstance() throws NoSuchRealmException {
        return getInstance(getDefaultRealm());
    }

    /**
     * Returns the name of the default realm.
     *
     * @return Default realm name.
     *
     */
    public static synchronized String getDefaultRealm() {
        return tryGetRealmsManager().getDefaultRealmName();
    }

    /**
     * Sets the name of the default realm.
     *
     * @param realmName Name of realm to set as default.
     *
     */
    public static synchronized void setDefaultRealm(String realmName) {
        tryGetRealmsManager().setDefaultRealmName(realmName);
    }

    /**
     * Returns the realm identified by the name which is passed as a parameter. This function knows
     * about all the realms which exist; it is not possible to store (or create) one which is not
     * accessible through this routine.
     *
     * @param name identifies the realm
     *
     * @return the requested realm
     * @throws NoSuchRealmException if the realm is invalid
     */
    public static synchronized Realm getInstance(String name) throws NoSuchRealmException {
        Realm realmInstance = _getInstance(name);

        if (realmInstance == null) {
            throw new NoSuchRealmException(
                localStrings.getLocalString("realm.no_such_realm", name + " realm does not exist.", new Object[] { name }));
        }

        return realmInstance;
    }

    /**
     * Returns the realm identified by the name which is passed as a parameter. This function knows
     * about all the realms which exist; it is not possible to store (or create) one which is not
     * accessible through this routine.
     *
     * @param configName
     * @param name identifies the realm
     *
     * @return the requested realm
     * @throws NoSuchRealmException if the realm is invalid
     */
    public static synchronized Realm getInstance(String configName, String name) throws NoSuchRealmException {
        Realm realmInstance = _getInstance(configName, name);

        if (realmInstance == null) {
            throw new NoSuchRealmException(
                localStrings.getLocalString("realm.no_such_realm", name + " realm does not exist.", new Object[] { name }));
        }

        return realmInstance;
    }

    /**
     * Returns the names of accessible realms.
     *
     * @return set of realm names
     */
    public static synchronized Enumeration<String> getRealmNames() {
        return tryGetRealmsManager().getRealmNames();
    }

    /**
     * Checks if the given realm name is loaded/valid.
     *
     * @param name name of the realm to check.
     * @return true if realm present, false otherwise.
     */
    public static boolean isValidRealm(String name) {
        return tryGetRealmsManager().isValidRealm(name);
    }

    /**
     * Checks if the given realm name is loaded/valid.
     *
     * @param configName
     * @param name name of the realm to check.
     * @return true if realm present, false otherwise.
     */
    public static boolean isValidRealm(String configName, String name) {
        return tryGetRealmsManager().isValidRealm(configName, name);
    }

    public static synchronized void getRealmStatsProvier() {
        if (realmStatsProvier == null) {
            realmStatsProvier = new RealmStatsProvider();
        }
    }

    /**
     * Remove realm with given name from cache.
     *
     * @param realmName
     * @throws NoSuchRealmException
     */
    public static synchronized void unloadInstance(String realmName) throws NoSuchRealmException {
        // Make sure instance exist
        getInstance(realmName);

        tryGetRealmsManager().removeFromLoadedRealms(realmName);

        _logger.log(INFO, realmDeleted, realmName);
    }

    /**
     * Remove realm with given name from cache.
     *
     * @param configName
     * @param realmName
     * @throws NoSuchRealmException
     */
    public static synchronized void unloadInstance(String configName, String realmName) throws NoSuchRealmException {
        tryGetRealmsManager().removeFromLoadedRealms(configName, realmName);

        _logger.log(INFO, realmDeleted, realmName);
    }


    // ---[ Protected Static methods ]------------------------------------------------


    /**
     * Replace a Realm instance. Can be used by a Realm subclass to replace a previously initialized
     * instance of itself. Future getInstance requests will then obtain the new instance.
     *
     * <P>
     * Minimal error checking is done. The realm being replaced must already exist (instantiate() was
     * previously called), the new instance must be fully initialized properly and it must of course be
     * of the same class as the previous instance.
     *
     * @param realm The new realm instance.
     * @param name The (previously instantiated) name for this realm.
     *
     */
    protected static synchronized void updateInstance(Realm realm, String name) {
        RealmsManager realmsManager = tryGetRealmsManager();

        Realm oldRealm = realmsManager.getFromLoadedRealms(name);
        if (!oldRealm.getClass().equals(realm.getClass())) {
            // Would never happen unless bug in realm subclass
            throw new Error("Incompatible class " + realm.getClass() + " in replacement realm " + name);
        }
        realm.setName(oldRealm.getName());
        realmsManager.putIntoLoadedRealms(name, realm);

        _logger.log(FINER, realmUpdated, new Object[] { realm.getName() });
    }

    /**
     * Replace a Realm instance. Can be used by a Realm subclass to replace a previously initialized
     * instance of itself. Future getInstance requests will then obtain the new instance.
     *
     * <P>
     * Minimal error checking is done. The realm being replaced must already exist (instantiate() was
     * previously called), the new instance must be fully initialized properly and it must of course be
     * of the same class as the previous instance.
     *
     * @param configName
     * @param realm The new realm instance.
     * @param name The (previously instantiated) name for this realm.
     *
     */
    protected static synchronized void updateInstance(String configName, Realm realm, String name) {
        RealmsManager realmsManager = tryGetRealmsManager();

        Realm oldRealm = realmsManager.getFromLoadedRealms(configName, name);
        if (!oldRealm.getClass().equals(realm.getClass())) {
            // Would never happen unless bug in realm subclass
            throw new Error("Incompatible class " + realm.getClass() + " in replacement realm " + name);
        }

        realm.setName(oldRealm.getName());
        realmsManager.putIntoLoadedRealms(configName, name, realm);

        _logger.log(FINER, realmUpdated, new Object[] { realm.getName() });
    }




    // ---[ Private methods ]------------------------------------------------


    /**
     * Instantiates a Realm class of the given type and invokes its init()
     *
     */
    private static synchronized Realm doInstantiate(String name, String className, Properties props) throws BadRealmException {

        ServiceLocator serviceLocator = Globals.getDefaultHabitat();
        try {
            RealmsManager realmsManager = getRealmsManager();

            // Try a HK2 route first
            Realm realm = serviceLocator.getService(Realm.class, name);
            if (realm == null) {
                try {
                    CurrentBeforeParentClassLoader commonClassLoader =
                            serviceLocator.getService(ClassLoaderHierarchy.class).getCommonClassLoader();
                    String realmJarPath = props.getProperty("realmJarPath");
                    if (realmJarPath != null) {
                        commonClassLoader.addURL(Paths.get(realmJarPath).toUri().toURL());
                    }
                    // TODO: workaround here. Once fixed in V3 we should be able to use
                    // Context ClassLoader instead.
                    realm = (Realm) commonClassLoader.loadClass(className).newInstance();
                } catch (ClassNotFoundException | MalformedURLException ex) {
                    realm = (Realm) Class.forName(className).newInstance();
                }
            }

            realm.setName(name);
            realm.init(props);

            if (realmsManager == null) {
                throw new BadRealmException("Unable to locate RealmsManager Service");
            }

            _logger.log(FINER, realmCreated, new Object[] { name, className });

            return realm;

        } catch (NoSuchRealmException | InstantiationException | IllegalAccessException | ClassNotFoundException ex) {
            throw new BadRealmException(ex);
        }
    }

    /**
     * This is a private method for getting realm instance. If realm does not exist, then it will not
     * return null rather than throw exception.
     *
     * @param name identifies the realm
     * @return the requested realm
     */
    private static synchronized Realm _getInstance(String name) {
        return tryGetRealmsManager().getInstance(name);
    }

    /**
     * This is a private method for getting realm instance. If realm does not exist, then it will not
     * return null rather than throw exception.
     *
     * @param name identifies the realm
     * @return the requested realm
     */
    private static synchronized Realm _getInstance(String configName, String name) {
        return tryGetRealmsManager().getInstance(configName, name);
    }

    private static void registerRealmStatsProvier() {
        if (realmStatsProvier == null) {
            getRealmStatsProvier();
            StatsProviderManager.register("security", SERVER, "security/realm", realmStatsProvier);
        }
    }
}
