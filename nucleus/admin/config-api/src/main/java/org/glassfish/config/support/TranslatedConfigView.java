/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2017 Oracle and/or its affiliates. All rights reserved.
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
 */
// Portions Copyright [2016-2022] [Payara Foundation and/or its affiliates]

package org.glassfish.config.support;

import com.sun.enterprise.security.store.DomainScopedPasswordAliasStore;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigValue;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.glassfish.hk2.api.ServiceLocator;
import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.config.ConfigView;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * View that translate configured attributes containing properties like ${foo.bar}
 * into system properties values.
 * <p>
 * Also support translation of Password Aliases
 * <p>
 * Also support translation of Environment Variables
 *
 * @author Jerome Dochez
 */
public class TranslatedConfigView implements ConfigView {

    public static final ThreadLocal<Boolean> doSubstitution = new ThreadLocal<Boolean>() {
        @Override
        protected Boolean initialValue() {
            return Boolean.TRUE;
        }
    };

    static final Pattern p = Pattern.compile("([^\\$]*)\\$\\{([^\\}]*)\\}([^\\$]*)");
    static final Pattern envP = Pattern.compile("([^\\$]*)\\$\\{ENV=([^\\}]*)\\}([^\\$]*)");
    static final Pattern mpConfigP = Pattern.compile("([^\\$]*)\\$\\{MPCONFIG=([^\\}]*)\\}([^\\$]*)");
    private static final String ALIAS_TOKEN = "ALIAS";
    private static final String DEFAULT_SEPARATOR = ":";
    private static final int MAX_SUBSTITUTION_DEPTH = 100;
    private static final boolean SUBSTITUTION_DISABLED = Boolean.valueOf(System.getProperty("fish.payara.substitution.disable", "false"));
    static ServiceLocator habitat;
    private static DomainScopedPasswordAliasStore domainPasswordAliasStore = null;
    final ConfigView masterView;

    TranslatedConfigView(ConfigView master) {
        this.masterView = master;
    }

    /**
     * Expand variables in string value (non-config usage).
     * This expansion can be disabled by system property {@code fish.payara.substitution.disable}, and should be used by any code that handles
     * deployment descriptors, where this constitutes a non-standard behavior.
     *
     * @param value value to be expanded
     * @return expanded value or {@code null} when value is null. Original value when substitution is disabled.
     */
    public static String expandValue(String value) {
        if (value == null || SUBSTITUTION_DISABLED) {
            return value;
        }
        return expandConfigValue(value);
    }

    /**
     * Expand variables in string value (config usage).
     * This method should be called when expanding values from config, where substitution is necessary at all times.
     *
     * @param value value to be expanded
     * @return expanded value or {@code null} when value is null.
     */
    public static String expandConfigValue(String value) {
        return (String) getTranslatedValue(value);
    }

    private static Object getTranslatedValue(Object value) {
        if (value != null && value instanceof String) {
            String stringValue = value.toString();
            if (stringValue.indexOf('$') == -1) {
                return value;
            }
            if (!doSubstitution.get()) {
                return value;
            }

            if (domainPasswordAliasStore() != null) {
                if (getAlias(stringValue) != null) {
                    try {
                        return getRealPasswordFromAlias(stringValue);
                    } catch (Exception e) {
                        Logger.getAnonymousLogger().severe(Strings.get("TranslatedConfigView.aliaserror", stringValue, e.getLocalizedMessage()));
                        return stringValue;
                    }
                }
            }
            stringValue = performSubstitution(stringValue, SubstitutionType.MPCONFIG);
            stringValue = performSubstitution(stringValue, SubstitutionType.ENVIRONMENT);
            stringValue = performSubstitution(stringValue, SubstitutionType.SYSTEM);
            return stringValue;

        }
        return value;
    }

    private static String performSubstitution(String stringValue, SubstitutionType type) {
        String origValue = stringValue;
        int i = 0;
        Matcher matcher;
        switch (type) {
            case MPCONFIG:
                matcher = mpConfigP.matcher(stringValue);
                break;
            case ENVIRONMENT:
                matcher = envP.matcher(stringValue);
                break;
            case SYSTEM:
                matcher = p.matcher(stringValue);
                break;
            default:
                return stringValue;
        }
        StringBuffer stringBuffer = new StringBuffer();
        while (matcher.find() && i < MAX_SUBSTITUTION_DEPTH) {
            String[] envValue = splitForTranslatedDefaultValue(matcher.group(2).trim());
            String matchValue = envValue[0];
            String defaultValue = envValue.length > 1 ? envValue[1] : null;
            Optional<String> newValue = getReplacedValue(type, matchValue, defaultValue);
            if (newValue.isPresent()) {
                matcher.appendReplacement(stringBuffer, Matcher.quoteReplacement(matcher.group(1) + newValue.get() + matcher.group(3)));
            }
            i++;
        }
        matcher.appendTail(stringBuffer);
        if (i >= MAX_SUBSTITUTION_DEPTH) {
            Logger.getAnonymousLogger().severe(Strings.get("TranslatedConfigView.badprop", i, origValue));
        }
        return stringBuffer.toString();
    }

    private static Optional<String> getReplacedValue(SubstitutionType type, String matchValue, String defaultValue) {
        switch (type) {
            case MPCONFIG: {
                Config config = configResolver().getConfig();
                ConfigValue configValue = config.getConfigValue(matchValue);
                String newValue = configValue.getValue();
                if (newValue != null && !newValue.isEmpty()) {
                    Logger.getAnonymousLogger().fine("Found property '" + matchValue + "' in source '" + configValue.getSourceName() + "' with ordinal '" + configValue.getSourceOrdinal() + "'");
                    return Optional.of(newValue);
                } else if (defaultValue != null) {
                    return Optional.of(defaultValue);
                }
                Logger.getAnonymousLogger().warning("MicroProfile Config: property '" + matchValue + "': no value found, no default given.");
                break;
            }
            case SYSTEM: {
                String newValue = System.getProperty(matchValue);
                if (newValue != null) {
                    return Optional.of(newValue);
                }
                break;
            }
            case ENVIRONMENT: {
                String newValue = System.getenv(matchValue);
                if (newValue != null) {
                    return Optional.of(newValue);
                } else if (defaultValue != null) {
                    return Optional.of(defaultValue);
                }
                break;
            }
        }
        return Optional.empty();
    }

    private static String[] splitForTranslatedDefaultValue(String matchValue) {
        return (matchValue == null) ? null : matchValue.split(DEFAULT_SEPARATOR, 2);
    }

    public static void setHabitat(ServiceLocator h) {
        habitat = h;
    }

    private static DomainScopedPasswordAliasStore domainPasswordAliasStore() {
        if (habitat != null) {
            domainPasswordAliasStore = AccessController.doPrivileged(new PrivilegedAction<DomainScopedPasswordAliasStore>() {
                @Override
                public DomainScopedPasswordAliasStore run() {
                    return habitat.getService(DomainScopedPasswordAliasStore.class);
                }
            });
        }
        return domainPasswordAliasStore;
    }

    private static ConfigProviderResolver configResolver() {
        if (habitat != null) {
            return AccessController.doPrivileged((PrivilegedAction<ConfigProviderResolver>) () -> habitat.getService(ConfigProviderResolver.class));
        } else {
            throw new IllegalStateException("Trying to access MP Config before Service Locator started");
        }
    }

    /**
     * check if a given property name matches AS alias pattern ${ALIAS=aliasname}.
     * if so, return the aliasname, otherwise return null.
     *
     * @param propName The property name to resolve. ex. ${ALIAS=aliasname}.
     * @return The aliasname or null.
     */
    public static String getAlias(String propName) {
        String aliasName = null;
        String starter = "${" + ALIAS_TOKEN + "="; //no space is allowed in starter
        String ender = "}";

        propName = propName.trim();
        if (propName.startsWith(starter) && propName.endsWith(ender)) {
            propName = propName.substring(starter.length());
            int lastIdx = propName.length() - 1;
            if (lastIdx > 1) {
                propName = propName.substring(0, lastIdx);
                if (propName != null) {
                    aliasName = propName.trim();
                }
            }
        }
        return aliasName;
    }

    public static String getRealPasswordFromAlias(final String at) throws KeyStoreException, CertificateException, IOException, NoSuchAlgorithmException, UnrecoverableKeyException {

        final String an = getAlias(at);
        final boolean exists = domainPasswordAliasStore.containsKey(an);
        if (!exists) {

            final String msg = String.format("Alias  %s does not exist", an);
            throw new IllegalArgumentException(msg);
        }
        return new String(domainPasswordAliasStore.get(an));
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        return getTranslatedValue(masterView.invoke(proxy, method, args));
    }

    @Override
    public ConfigView getMasterView() {
        return masterView;
    }

    @Override
    public void setMasterView(ConfigView view) {
        // immutable implementation
    }

    @Override
    public <T extends ConfigBeanProxy> Class<T> getProxyType() {
        return masterView.getProxyType();
    }

    @Override
    public <T extends ConfigBeanProxy> T getProxy(Class<T> proxyType) {
        return proxyType.cast(Proxy.newProxyInstance(proxyType.getClassLoader(), new Class[]{proxyType}, this));
    }

    public enum SubstitutionType {
        MPCONFIG, ENVIRONMENT, SYSTEM
    }
}
