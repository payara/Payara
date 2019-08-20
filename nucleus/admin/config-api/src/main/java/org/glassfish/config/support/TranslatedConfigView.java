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
// Portions Copyright [2016-2019] [Payara Foundation and/or its affiliates]

package org.glassfish.config.support;

import com.sun.enterprise.security.store.DomainScopedPasswordAliasStore;
import org.jvnet.hk2.config.ConfigView;
import org.jvnet.hk2.config.ConfigBeanProxy;
import org.glassfish.hk2.api.ServiceLocator;

import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.logging.Logger;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.security.AccessController;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.io.IOException;
import java.security.PrivilegedAction;
import java.util.Optional;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

/**
 * View that translate configured attributes containing properties like ${foo.bar}
 * into system properties values.
 * 
 * Also support translation of Password Aliases
 * 
 * Also support translation of Environment Variables
 *
 * @author Jerome Dochez
 */
public class TranslatedConfigView implements ConfigView {

    static final Pattern p = Pattern.compile("([^\\$]*)\\$\\{([^\\}]*)\\}([^\\$]*)");
    static final Pattern envP = Pattern.compile("([^\\$]*)\\$\\{ENV=([^\\}]*)\\}([^\\$]*)");
    static final Pattern mpConfigP = Pattern.compile("([^\\$]*)\\$\\{MPCONFIG=([^\\}]*)\\}([^\\$]*)");

    private static final String ALIAS_TOKEN = "ALIAS";
    private static final int MAX_SUBSTITUTION_DEPTH = 100;

    private static final boolean SUBSTITUTION_DISABLED = Boolean.valueOf(System.getProperty("fish.payara.substitution.disable", "false"));

    public static final ThreadLocal<Boolean> doSubstitution = new ThreadLocal<Boolean>() {
        @Override
        protected Boolean initialValue() {
            return Boolean.TRUE;
        }
    };

    /**
     * Expand variables in string value (non-config usage).
     * This expansion can be disabled by system property {@code fish.payara.substitution.disable}, and should be used by any code that handles
     * deployment descriptors, where this constitutes a non-standard behavior.
     * @param value value to be expanded
     * @return expanded value or {@code null} when value is null. Original value when substitution is disabled.
     */
    public static String expandValue(String value) {
        if (value == null || SUBSTITUTION_DISABLED) {
            return value;
        }
        return (String) getTranslatedValue(value);
    }

    /**
     * Expand variables in string value (config usage).
     * This method should be called when expanding values from config, where substitution is necessary at all times.
     * @param value value to be expanded
     * @return expanded value or {@code null} when value is null.     
     */
    public static String expandConfigValue(String value) {
        return (String) getTranslatedValue(value);
    }

    private static Object getTranslatedValue(Object value) {
        if (value!=null && value instanceof String) {
            String stringValue = value.toString();
            if (stringValue.indexOf('$')==-1) {
                return value;
            }
            if(!doSubstitution.get()) {
                return value;
            }
            
           
            if (domainPasswordAliasStore() != null) {
                if (getAlias(stringValue, ALIAS_TOKEN) != null) {
                    try{
                        return getRealPasswordFromAlias(stringValue);
                    } catch (Exception e) {
                        Logger.getAnonymousLogger().severe(Strings.get("TranslatedConfigView.aliaserror", stringValue, e.getLocalizedMessage()));
                        return stringValue;
                    }
                }
            }
            
            String origValue = stringValue;
            
            int i = 0;            // Perform Environment variable substitution
            Matcher m3 = mpConfigP.matcher(stringValue);

            while (m3.find() && i < MAX_SUBSTITUTION_DEPTH) {
                String matchValue = m3.group(2).trim();
                Config config = ConfigProvider.getConfig();
                Optional<String> newValue = config.getOptionalValue(matchValue,String.class);
                if (newValue != null && newValue.isPresent()) {
                    stringValue = m3.replaceFirst(Matcher.quoteReplacement(m3.group(1) + newValue.get() + m3.group(3)));
                    m3.reset(stringValue);
                } 
                i++;     
            }
            if (i >= MAX_SUBSTITUTION_DEPTH) {
                Logger.getAnonymousLogger().severe(Strings.get("TranslatedConfigView.badprop", i, origValue));
            }  
            
            
            
            
            i = 0;            // Perform Environment variable substitution
            Matcher m2 = envP.matcher(stringValue);

            while (m2.find() && i < MAX_SUBSTITUTION_DEPTH) {
                String matchValue = m2.group(2).trim();
                String newValue = System.getenv(matchValue);
                if (newValue != null) {
                    stringValue = m2.replaceFirst(Matcher.quoteReplacement(m2.group(1) + newValue + m2.group(3)));
                    m2.reset(stringValue);
                } 
                i++;     
            }
            if (i >= MAX_SUBSTITUTION_DEPTH) {
                Logger.getAnonymousLogger().severe(Strings.get("TranslatedConfigView.badprop", i, origValue));
            }            

            // Perform system property substitution in the value
            // The loop limit is imposed to prevent infinite looping to values
            // such as a=${a} or a=foo ${b} and b=bar {$a}
            Matcher m = p.matcher(stringValue);
            i = 0;
            while (m.find() && i < MAX_SUBSTITUTION_DEPTH) {
                String matchValue = m.group(2).trim();
                String newValue = System.getProperty(matchValue);
                if (newValue != null) {
                    stringValue = m.replaceFirst(
                            Matcher.quoteReplacement(m.group(1) + newValue + m.group(3)));
                    m.reset(stringValue);
                } 
                i++;     
            }
            if (i >= MAX_SUBSTITUTION_DEPTH) {
                Logger.getAnonymousLogger().severe(Strings.get("TranslatedConfigView.badprop", i, origValue));
            }
            

            return stringValue;
            
        }
        return value;
    }

    final ConfigView masterView;

    
    TranslatedConfigView(ConfigView master ) {
        this.masterView = master;

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
    static ServiceLocator habitat;
    public static void setHabitat(ServiceLocator h) {
         habitat = h;
    }
    
    private static DomainScopedPasswordAliasStore domainPasswordAliasStore = null;
    private static DomainScopedPasswordAliasStore domainPasswordAliasStore() {
        if (habitat != null) {
            domainPasswordAliasStore = AccessController.doPrivileged(
                    new PrivilegedAction<DomainScopedPasswordAliasStore>() {
                        @Override
                        public DomainScopedPasswordAliasStore run() {
                            return habitat.getService(DomainScopedPasswordAliasStore.class);
                        }
                    }
            );
        }
        return domainPasswordAliasStore;
    }
    
   /**
     * check if a given property name matches AS alias pattern ${ALIAS=aliasname}.
     * if so, return the aliasname, otherwise return null.
     * @param propName The property name to resolve. ex. ${ALIAS=aliasname}.
     * @param token
     * @return The aliasname or null.
     */
    public static String getAlias(String propName, String token)
    {
       String aliasName=null;
       String starter = "${" + token + "="; //no space is allowed in starter
       String ender   = "}";

       propName = propName.trim();
       if (propName.startsWith(starter) && propName.endsWith(ender) ) {
           propName = propName.substring(starter.length() );
           int lastIdx = propName.length() - 1;
           if (lastIdx > 1) {
              propName = propName.substring(0,lastIdx);
              if (propName!=null) {
                 aliasName = propName.trim();
              }
           }
       }
       return aliasName;
    }

    public static String getRealPasswordFromAlias(final String at) throws
            KeyStoreException, CertificateException, IOException, NoSuchAlgorithmException, UnrecoverableKeyException {

        final String an = getAlias(at, ALIAS_TOKEN);
        final boolean exists = domainPasswordAliasStore.containsKey(an);
        if (!exists) {

            final String msg = String.format("Alias  %s does not exist", an);
            throw new IllegalArgumentException(msg);
        }
        return new String(domainPasswordAliasStore.get(an));
    }


}
