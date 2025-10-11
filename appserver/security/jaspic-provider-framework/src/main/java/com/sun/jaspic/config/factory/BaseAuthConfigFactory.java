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
// Portions Copyright [2018-2022] [Payara Foundation and/or its affiliates]
package com.sun.jaspic.config.factory;

import static com.sun.jaspic.config.helper.JASPICLogManager.JASPIC_LOGGER;
import static com.sun.jaspic.config.helper.JASPICLogManager.RES_BUNDLE;
import static java.util.logging.Level.WARNING;

import java.security.Permission;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;
import java.util.logging.Logger;

import jakarta.security.auth.message.AuthException;
import jakarta.security.auth.message.config.AuthConfigFactory;
import jakarta.security.auth.message.config.AuthConfigProvider;
import jakarta.security.auth.message.config.RegistrationListener;
import jakarta.security.auth.message.module.ServerAuthModule;
import jakarta.servlet.ServletContext;
import java.security.AccessController;
import java.security.PrivilegedAction;
import static org.glassfish.soteria.Utils.isEmpty;


/**
 * This class implements methods in the abstract class AuthConfigFactory.
 * 
 * @author Shing Wai Chan
 */
public abstract class BaseAuthConfigFactory extends AuthConfigFactory {

    private static final Logger logger = Logger.getLogger(JASPIC_LOGGER, RES_BUNDLE);

    private static final ReadWriteLock readWriteLock = new ReentrantReadWriteLock(true);

    public static final Lock readLock = readWriteLock.readLock();
    public static final Lock writeLock = readWriteLock.writeLock();

    private static Map<String, AuthConfigProvider> idToProviderMap;
    private static Map<String, RegistrationContext> idToRegistrationContextMap;
    private static Map<String, List<RegistrationListener>> idToRegistrationListenersMap;
    private static Map<AuthConfigProvider, List<String>> providerToIdsMap;

    protected static final String CONF_FILE_NAME = "auth.conf";
    private static final String CONTEXT_REGISTRATION_ID = "org.glassfish.security.message.registrationId";

    /**
     * Get a registered AuthConfigProvider from the factory.
     *
     * Get the provider of ServerAuthConfig and/or ClientAuthConfig objects registered for the identified message layer and
     * application context.
     * 
     * <p>
     * All factories shall employ the following precedence rules to select the registered AuthConfigProvider that matches
     * (via matchConstructors) the layer and appContext arguments:
     * <ul>
     * <li>The provider that is specifically registered for both the corresponding message layer and appContext shall be
     * selected.
     * <li>if no provider is selected according to the preceding rule, the provider specifically registered for the
     * corresponding appContext and for all message layers shall be selected.
     * <li>if no provider is selected according to the preceding rules, the provider specifically registered for the
     * corresponding message layer and for all appContexts shall be selected.
     * <li>if no provider is selected according to the preceding rules, the provider registered for all message layers and
     * for all appContexts shall be selected.
     * <li>if no provider is selected according to the preceding rules, the factory shall terminate its search for a
     * registered provider.
     * </ul>
     *
     * @param layer a String identifying the message layer for which the registered AuthConfigProvider is to be returned.
     * This argument may be null.
     *
     * @param appContext a String that identifies the application messaging context for which the registered
     * AuthConfigProvider is to be returned. This argument may be null.
     *
     * @param listener the RegistrationListener whose <code>notify</code> method is to be invoked if the corresponding
     * registration is unregistered or replaced. The value of this argument may be null.
     *
     * @return the implementation of the AuthConfigProvider interface registered at the factory for the layer and appContext
     * or null if no AuthConfigProvider is selected.
     * 
     */
    @Override
    public AuthConfigProvider getConfigProvider(String layer, String appContext, RegistrationListener listener) {
        if (listener == null) {
            return doReadLocked(() -> getConfigProviderUnderLock(layer, appContext, null));
        }

        return doWriteLocked(() -> getConfigProviderUnderLock(layer, appContext, listener));
    }

    /**
     * Registers within the factory, a provider of ServerAuthConfig and/or ClientAuthConfig objects for a message layer and
     * application context identifier.
     *
     * <P>
     * At most one registration may exist within the factory for a given combination of message layer and appContext. Any
     * pre-existing registration with identical values for layer and appContext is replaced by a subsequent registration.
     * When replacement occurs, the registration identifier, layer, and appContext identifier remain unchanged, and the
     * AuthConfigProvider (with initialization properties) and description are replaced.
     *
     * <p>
     * Within the lifetime of its Java process, a factory must assign unique registration identifiers to registrations, and
     * must never assign a previously used registration identifier to a registration whose message layer and or appContext
     * identifier differ from the previous use.
     *
     * <p>
     * Programmatic registrations performed via this method must update (according to the replacement rules described
     * above), the persistent declarative representation of provider registrations employed by the factory constructor.
     *
     * @param className the fully qualified name of an AuthConfigProvider implementation class. This argument must not be
     * null.
     *
     * @param properties a Map object containing the initialization properties to be passed to the provider constructor.
     * This argument may be null. When this argument is not null, all the values and keys occuring in the Map must be of
     * type String.
     *
     * @param layer a String identifying the message layer for which the provider will be registered at the factory. A null
     * value may be passed as an argument for this parameter, in which case, the provider is registered at all layers.
     *
     * @param appContext a String value that may be used by a runtime to request a configuration object from this provider.
     * A null value may be passed as an argument for this parameter, in which case, the provider is registered for all
     * configuration ids (at the indicated layers).
     *
     * @param description a text String describing the provider. this value may be null.
     *
     * @return a String identifier assigned by the factory to the provider registration, and that may be used to remove the
     * registration from the provider.
     *
     * @exception SecurityException if the caller does not have permission to register a provider at the factory.
     *
     * @exception AuthException if the provider construction or registration fails.
     */
    @Override
    @SuppressWarnings("unchecked")
    public String registerConfigProvider(String className, @SuppressWarnings("rawtypes") Map properties, String layer, String appContext,
            String description) {
        tryCheckPermission(providerRegistrationSecurityPermission);

        return _register(_constructProvider(className, properties, null), properties, layer, appContext, description, true);
    }

    @Override
    public String registerConfigProvider(AuthConfigProvider provider, String layer, String appContext, String description) {
        tryCheckPermission(providerRegistrationSecurityPermission);

        return _register(provider, null, layer, appContext, description, false);
    }

    /**
     * Remove the identified provider registration from the factory and invoke any listeners associated with the removed
     * registration.
     *
     * @param registrationID a String that identifies a provider registration at the factory
     *
     * @return true if there was a registration with the specified identifier and it was removed. Return false if the
     * registraionID was invalid.
     *
     * @exception SecurityException if the caller does not have permission to unregister the provider at the factory.
     *
     */
    @Override
    public boolean removeRegistration(String registrationID) {
        tryCheckPermission(AuthConfigFactory.providerRegistrationSecurityPermission);

        return _unRegister(registrationID);
    }

    /**
     * Disassociate the listener from all the provider registrations whose layer and appContext values are matched by the
     * corresponding arguments to this method.
     *
     * @param listener the RegistrationListener to be detached.
     *
     * @param layer a String identifying the message layer or null.
     *
     * @param appContext a String value identifying the application context or null.
     *
     * @return an array of String values where each value identifies a provider registration from which the listener was
     * removed. This method never returns null; it returns an empty array if the listener was not removed from any
     * registrations.
     *
     * @exception SecurityException if the caller does not have permission to detach the listener from the factory.
     *
     */
    @Override
    public String[] detachListener(RegistrationListener listener, String layer, String appContext) {
        tryCheckPermission(providerRegistrationSecurityPermission);

        List<String> removedListenerIds = new ArrayList<>();
        String registrationId = getRegistrationID(layer, appContext);

        doWriteLocked(() -> {
            for (Entry<String, List<RegistrationListener>> entry : idToRegistrationListenersMap.entrySet()) {
                String targetID = entry.getKey();
                if (regIdImplies(registrationId, targetID)) {
                    List<RegistrationListener> listeners = entry.getValue();
                    if (listeners != null && listeners.remove(listener)) {
                        removedListenerIds.add(targetID);
                    }
                }
            }
        });

        return removedListenerIds.toArray(new String[removedListenerIds.size()]);
    }

    /**
     * Get the registration identifiers for all registrations of the provider instance at the factory.
     *
     * @param provider the AuthConfigurationProvider whose registration identifiers are to be returned. This argument may be
     * null, in which case, it indicates that the the id's of all active registration within the factory are returned.
     *
     * @return an array of String values where each value identifies a provider registration at the factory. This method
     * never returns null; it returns an empty array when their are no registrations at the factory for the identified
     * provider.
     */
    @Override
    public String[] getRegistrationIDs(AuthConfigProvider provider) {
        return doReadLocked(() -> {
            Collection<String> registrationIDs = null;

            if (provider != null) {
                registrationIDs = providerToIdsMap.get(provider);
            } else {
                Collection<List<String>> collList = providerToIdsMap.values();
                if (collList != null) {
                    registrationIDs = new HashSet<>();
                    for (List<String> listIds : collList) {
                        if (listIds != null) {
                            registrationIDs.addAll(listIds);
                        }
                    }
                }
            }

            return registrationIDs != null ? registrationIDs.toArray(new String[registrationIDs.size()]) : new String[0];
        });
    }

    /**
     * Get the the registration context for the identified registration.
     *
     * @param registrationID a String that identifies a provider registration at the factory
     *
     * @return a RegistrationContext or null. When a Non-null value is returned, it is a copy of the registration context
     * corresponding to the registration. Null is returned when the registration identifier does not correspond to an active
     * registration
     */
    @Override
    public RegistrationContext getRegistrationContext(String registrationID) {
        return doReadLocked(() -> idToRegistrationContextMap.get(registrationID));
    }

    /**
     * Cause the factory to reprocess its persistent declarative representation of provider registrations.
     *
     * <p>
     * A factory should only replace an existing registration when a change of provider implementation class or
     * initialization properties has occurred.
     *
     * @exception AuthException if an error occurred during the reinitialization.
     *
     * @exception SecurityException if the caller does not have permission to refresh the factory.
     */
    @Override
    public void refresh() {
        tryCheckPermission(AuthConfigFactory.providerRegistrationSecurityPermission);

        Map<String, List<RegistrationListener>> preExistingListenersMap = doWriteLocked(() -> loadFactory());

        // Notify pre-existing listeners after (re)loading factory
        if (preExistingListenersMap != null) {
            notifyListeners(preExistingListenersMap);
        }
    }

    abstract protected RegStoreFileParser getRegStore();

    private AuthConfigProvider getConfigProviderUnderLock(String layer, String appContext, RegistrationListener listener) {
        AuthConfigProvider provider = null;
        String registrationID = getRegistrationID(layer, appContext);

        boolean providerFound = false;
        if (idToProviderMap.containsKey(registrationID)) {
            provider = idToProviderMap.get(registrationID);
            providerFound = true;
        }

        if (!providerFound) {
            String matchedID = getRegistrationID(null, appContext);
            if (idToProviderMap.containsKey(matchedID)) {
                provider = idToProviderMap.get(matchedID);
                providerFound = true;
            }
        }

        if (!providerFound) {
            String matchedID = getRegistrationID(layer, null);
            if (idToProviderMap.containsKey(matchedID)) {
                provider = idToProviderMap.get(matchedID);
                providerFound = true;
            }
        }

        if (!providerFound) {
            String matchedID = getRegistrationID(null, null);
            if (idToProviderMap.containsKey(matchedID)) {
                provider = idToProviderMap.get(matchedID);
            }
        }

        if (listener != null) {
            List<RegistrationListener> listeners = idToRegistrationListenersMap.computeIfAbsent(
                    registrationID, e -> new ArrayList<RegistrationListener>());

            if (!listeners.contains(listener)) {
                listeners.add(listener);
            }
        }

        return provider;
    }

    private static String getRegistrationID(String layer, String appContext) {

        // (layer, appContext) -> __3<nn>_<layer><appContext>
        // (layer, null) -> __2<layer>
        // (null, appContext) -> __1<appContext>
        // (null, null) -> __0

        if (layer != null) {
            return appContext != null ? "__3" + layer.length() + "_" + layer + appContext : "__2" + layer;
        }

        return appContext != null ? "__1" + appContext : "__0";
    }

    /**
     * This API decomposes the given registration ID into layer and appContext.
     * 
     * @param registrationId
     * @return a String array with layer and appContext
     */
    private static String[] decomposeRegistrationId(String registrationId) {
        String layer = null;
        String appContext = null;

        if (registrationId.equals("__0")) {
            // null, null
        } else if (registrationId.startsWith("__1")) {
            appContext = (registrationId.length() == 3) ? "" : registrationId.substring(3);
        } else if (registrationId.startsWith("__2")) {
            layer = (registrationId.length() == 3) ? "" : registrationId.substring(3);
        } else if (registrationId.startsWith("__3")) {
            int ind = registrationId.indexOf('_', 3);
            if (registrationId.length() > 3 && ind > 0) {
                String numberString = registrationId.substring(3, ind);
                int n;
                try {
                    n = Integer.parseInt(numberString);
                } catch (Exception ex) {
                    throw new IllegalArgumentException();
                }
                layer = registrationId.substring(ind + 1, ind + 1 + n);
                appContext = registrationId.substring(ind + 1 + n);
            } else {
                throw new IllegalArgumentException();
            }
        } else {
            throw new IllegalArgumentException();
        }

        return new String[] { layer, appContext };
    }

    private static AuthConfigProvider _constructProvider(String className, Map<String, String> properties, AuthConfigFactory factory) {
        AuthConfigProvider provider = null;

        if (className != null) {
            try {
                provider = (AuthConfigProvider) Class.forName(className, true, Thread.currentThread().getContextClassLoader())
                        .getConstructor(Map.class, AuthConfigFactory.class)
                        .newInstance(new Object[] { properties, factory });
            } catch (Throwable t) {
                Throwable cause = t.getCause();
                logger.log(WARNING, "jaspic.factory_unable_to_load_provider",
                        new Object[] { className, t.toString(), cause == null ? "cannot determine" : cause.toString() });
            }
        }

        return provider;
    }

    // XXX need to update persistent state and notify effected listeners
    private String _register(AuthConfigProvider provider, Map<String, String> properties, String layer, String appContext,
            String description, boolean persistent) {
        String registrationId = getRegistrationID(layer, appContext);
        RegistrationContext registrationContext = new RegistrationContextImpl(layer, appContext, description, persistent);

        Map<String, List<RegistrationListener>> listenerMap = doWriteLocked(
                () -> register(provider, properties, persistent, registrationId, registrationContext));

        // Outside write lock to prevent dead lock
        notifyListeners(listenerMap);

        return registrationId;
    }

    private Map<String, List<RegistrationListener>> register(AuthConfigProvider provider, Map<String, String> properties,
            boolean persistent, String registrationId, RegistrationContext registrationContext) {
        RegistrationContext previousRegistrationContext = idToRegistrationContextMap.get(registrationId);
        AuthConfigProvider previousProvider = idToProviderMap.get(registrationId);

        // Handle the persistence first - so that any exceptions occur before
        // the actual registration happens
        if (persistent) {
            _storeRegistration(registrationContext, provider, properties);
        } else if (previousRegistrationContext != null && previousRegistrationContext.isPersistent()) {
            _deleteStoredRegistration(previousRegistrationContext);
        }

        if (idToProviderMap.containsKey(registrationId)) {
            List<String> previousRegistrationsIds = providerToIdsMap.get(previousProvider);
            previousRegistrationsIds.remove(registrationId);
            if (previousRegistrationsIds.isEmpty()) {
                providerToIdsMap.remove(previousProvider);
            }
        }

        idToProviderMap.put(registrationId, provider);
        idToRegistrationContextMap.put(registrationId, registrationContext);

        List<String> registrationIds = providerToIdsMap.computeIfAbsent(provider, e -> new ArrayList<String>());

        if (!registrationIds.contains(registrationId)) {
            registrationIds.add(registrationId);
        }

        return getEffectedListeners(registrationId);
    }

    // XXX need to update persistent state and notify effected listeners
    private boolean _unRegister(String registrationId) {
        boolean hasProvider = false;
        Map<String, List<RegistrationListener>> listenerMap;

        writeLock.lock();
        try {
            RegistrationContext registrationContext = idToRegistrationContextMap.remove(registrationId);
            hasProvider = idToProviderMap.containsKey(registrationId);
            AuthConfigProvider provider = idToProviderMap.remove(registrationId);

            List<String> registrationIds = providerToIdsMap.get(provider);
            if (registrationIds != null) {
                registrationIds.remove(registrationId);
            }

            if (registrationIds == null || registrationIds.isEmpty()) {
                providerToIdsMap.remove(provider);
            }

            if (!hasProvider) {
                return false;
            }

            listenerMap = getEffectedListeners(registrationId);
            if (registrationContext != null && registrationContext.isPersistent()) {
                _deleteStoredRegistration(registrationContext);
            }
        } finally {
            writeLock.unlock();
        }

        // Outside write lock to prevent dead lock
        notifyListeners(listenerMap);

        return hasProvider;
    }

    private Map<String, List<RegistrationListener>> loadFactory() {
        Map<String, List<RegistrationListener>> oldId2RegisListenersMap = idToRegistrationListenersMap;

        _loadFactory();

        return oldId2RegisListenersMap;
    }

    // ### The following methods implement the factory's persistence layer

    protected void _loadFactory() {
        try {
            initializeMaps();

            List<EntryInfo> persistedEntries = getRegStore().getPersistedEntries();

            for (EntryInfo info : persistedEntries) {
                if (info.isConstructorEntry()) {
                    _constructProvider(info.getClassName(), info.getProperties(), this);
                } else {
                    boolean first = true;
                    AuthConfigProvider configProvider = null;
                    for (RegistrationContext context : info.getRegistrationContexts()) {
                        if (first) {
                            configProvider = _constructProvider(info.getClassName(), info.getProperties(), null);
                        }

                        _loadRegistration(configProvider, context.getMessageLayer(), context.getAppContext(), context.getDescription());
                    }
                }
            }
        } catch (Exception e) {
            if (logger.isLoggable(WARNING)) {
                logger.log(WARNING, "jaspic.factory_auth_config_loader_failure", e);
            }
        }
    }

    /**
     * Initialize the static maps in a static method
     */
    private static void initializeMaps() {
        idToProviderMap = new HashMap<>();
        idToRegistrationContextMap = new HashMap<>();
        idToRegistrationListenersMap = new HashMap<>();
        providerToIdsMap = new HashMap<>();
    }

    private static String _loadRegistration(AuthConfigProvider provider, String layer, String appContext, String description) {

        RegistrationContext registrationContext = new RegistrationContextImpl(layer, appContext, description, true);
        String registrationId = getRegistrationID(layer, appContext);

        AuthConfigProvider previousProvider = idToProviderMap.get(registrationId);

        boolean wasRegistered = idToProviderMap.containsKey(registrationId);
        if (wasRegistered) {
            List<String> previousRegistrationIds = providerToIdsMap.get(previousProvider);
            previousRegistrationIds.remove(registrationId);
            if (previousRegistrationIds.isEmpty()) {
                providerToIdsMap.remove(previousProvider);
            }
        }

        idToProviderMap.put(registrationId, provider);
        idToRegistrationContextMap.put(registrationId, registrationContext);

        List<String> registrationIds = providerToIdsMap.get(provider);
        if (registrationIds == null) {
            registrationIds = new ArrayList<>();
            providerToIdsMap.put(provider, registrationIds);
        }

        if (!registrationIds.contains(registrationId)) {
            registrationIds.add(registrationId);
        }

        return registrationId;
    }

    private void _storeRegistration(RegistrationContext registrationContext, AuthConfigProvider configProvider,
            Map<String, String> properties) {
        String className = null;
        if (configProvider != null) {
            className = configProvider.getClass().getName();
        }

        if (propertiesContainAnyNonStringValues(properties)) {
            throw new IllegalArgumentException("AuthConfigProvider cannot be registered - properties must all be of type String.");
        }

        if (registrationContext.isPersistent()) {
            getRegStore().store(className, registrationContext, properties);
        }
    }

    private boolean propertiesContainAnyNonStringValues(Map<String, String> properties) {
        if (properties != null) {
            for (Map.Entry<String, String> entry : properties.entrySet()) {
                if (!(entry.getValue() instanceof String)) {
                    return true;
                }
            }
        }

        return false;
    }

    private void _deleteStoredRegistration(RegistrationContext registrationContext) {
        if (registrationContext.isPersistent()) {
            getRegStore().delete(registrationContext);
        }
    }

    private static boolean regIdImplies(String reference, String target) {

        boolean rvalue = true;

        String[] refID = decomposeRegistrationId(reference);
        String[] targetID = decomposeRegistrationId(target);

        if (refID[0] != null && !refID[0].equals(targetID[0])) {
            rvalue = false;
        } else if (refID[1] != null && !refID[1].equals(targetID[1])) {
            rvalue = false;
        }

        return rvalue;
    }

    /**
     * Will return some extra listeners. In other words, effected listeners could be reduced by removing any associated with
     * a provider registration id that is more specific than the one being added or removed.
     */
    private static Map<String, List<RegistrationListener>> getEffectedListeners(String regisID) {
        Map<String, List<RegistrationListener>> effectedListeners = new HashMap<>();
        Set<String> listenerRegistrations = new HashSet<>(idToRegistrationListenersMap.keySet());

        for (String listenerID : listenerRegistrations) {
            if (regIdImplies(regisID, listenerID)) {
                if (!effectedListeners.containsKey(listenerID)) {
                    effectedListeners.put(listenerID, new ArrayList<>());
                }
                effectedListeners.get(listenerID).addAll(idToRegistrationListenersMap.remove(listenerID));
            }
        }
        return effectedListeners;
    }

    private static void tryCheckPermission(Permission permission) {
        SecurityManager securityManager = System.getSecurityManager();
        if (securityManager != null) {
            securityManager.checkPermission(permission);
        }
    }

    protected <T> T doReadLocked(Supplier<T> supplier) {
        readLock.lock();
        try {
            return supplier.get();
        } finally {
            readLock.unlock();
        }
    }

    protected <T> T doWriteLocked(Supplier<T> supplier) {
        writeLock.lock();
        try {
            return supplier.get();
        } finally {
            writeLock.unlock();
        }
    }

    protected void doWriteLocked(Runnable runnable) {
        writeLock.lock();
        try {
            runnable.run();
        } finally {
            writeLock.unlock();
        }
    }

    private static void notifyListeners(Map<String, List<RegistrationListener>> map) {
        Set<Map.Entry<String, List<RegistrationListener>>> entrySet = map.entrySet();
        for (Map.Entry<String, List<RegistrationListener>> entry : entrySet) {
            List<RegistrationListener> listeners = map.get(entry.getKey());

            if (listeners != null && listeners.size() > 0) {
                String[] dIds = decomposeRegistrationId(entry.getKey());

                for (RegistrationListener listener : listeners) {
                    listener.notify(dIds[0], dIds[1]);
                }
            }
        }
    }

    @Override
    public String registerServerAuthModule(ServerAuthModule sam, Object context) {
        String registrationId = null;
        if (context instanceof ServletContext) {
            ServletContext servletContext = (ServletContext) context;

            String appContext = servletContext.getVirtualServerName() + " " + servletContext.getContextPath();
            registrationId = AccessController.doPrivileged((PrivilegedAction<String>) () -> registerConfigProvider(
                    new DefaultAuthConfigProvider(sam),
                    "HttpServlet",
                    appContext,
                    "Default authentication config provider"));

            servletContext.setAttribute(CONTEXT_REGISTRATION_ID, registrationId);
        }
        return registrationId;
    }

    @Override
    public void removeServerAuthModule(Object context) {
        if (context instanceof ServletContext) {
            ServletContext servletContext = (ServletContext) context;
            String registrationId = (String) servletContext.getAttribute(CONTEXT_REGISTRATION_ID);
            if (!isEmpty(registrationId)) {
                AccessController.doPrivileged((PrivilegedAction<Boolean>) () -> removeRegistration(registrationId));
            }
        }
    }
}
