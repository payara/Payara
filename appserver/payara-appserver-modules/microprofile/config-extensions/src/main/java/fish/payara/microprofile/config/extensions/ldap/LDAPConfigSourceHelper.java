/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2020] Payara Foundation and/or its affiliates. All rights reserved.
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
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
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
package fish.payara.microprofile.config.extensions.ldap;

import com.sun.enterprise.util.StringUtils;
import static fish.payara.microprofile.config.extensions.ldap.LDAPConfigSourceConfiguration.AUTH_TYPE_NONE;
import static fish.payara.microprofile.config.extensions.ldap.LDAPConfigSourceConfiguration.AUTH_TYPE_SIMPLE;
import static fish.payara.microprofile.config.extensions.ldap.LDAPConfigSourceConfiguration.LDAP_CONNECT_TIMEOUT;
import static fish.payara.microprofile.config.extensions.ldap.LDAPConfigSourceConfiguration.LDAP_CONTEXT_FACTORY;
import static fish.payara.microprofile.config.extensions.ldap.LDAPConfigSourceConfiguration.LDAP_READ_TIMEOUT;
import static fish.payara.microprofile.config.extensions.ldap.LDAPConfigSourceConfiguration.SEARCH_SCOPE_OBJECT;
import static fish.payara.microprofile.config.extensions.ldap.LDAPConfigSourceConfiguration.SEARCH_SCOPE_ONELEVEL;
import static fish.payara.microprofile.config.extensions.ldap.LDAPConfigSourceConfiguration.SEARCH_SCOPE_SUBTREE;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toMap;
import javax.naming.AuthenticationException;
import static javax.naming.Context.INITIAL_CONTEXT_FACTORY;
import static javax.naming.Context.PROVIDER_URL;
import static javax.naming.Context.SECURITY_AUTHENTICATION;
import static javax.naming.Context.SECURITY_CREDENTIALS;
import static javax.naming.Context.SECURITY_PRINCIPAL;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchControls;
import static javax.naming.directory.SearchControls.OBJECT_SCOPE;
import static javax.naming.directory.SearchControls.ONELEVEL_SCOPE;
import static javax.naming.directory.SearchControls.SUBTREE_SCOPE;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;
import javax.naming.ldap.StartTlsRequest;
import javax.naming.ldap.StartTlsResponse;
import org.glassfish.config.support.TranslatedConfigView;
import static org.glassfish.config.support.TranslatedConfigView.getAlias;

/**
 *
 * @author Gaurav Gupta
 */
public class LDAPConfigSourceHelper {

    private static final Logger logger = Logger.getLogger(LDAPConfigSourceHelper.class.getName());

    private final LDAPConfigSourceConfiguration configuration;

    public LDAPConfigSourceHelper(LDAPConfigSourceConfiguration configuration) {
        this.configuration = configuration;
    }

    public synchronized String getConfigValue(String propertyName) {
        String propertyValue = null;
        StartTlsResponse tlsResponse = null;
        LdapContext context = getContext();
        if (Boolean.valueOf(configuration.getStartTLSEnabled())) {
            tlsResponse = startTLSConnection(context);
        }
        try {
            if (context != null) {
                if (StringUtils.ok(configuration.getSearchBase()) && StringUtils.ok(configuration.getSearchFilter())) {
                    SearchControls controls = new SearchControls();
                    controls.setReturningAttributes(new String[]{propertyName});
                    controls.setSearchScope(convertScopeValue(configuration.getSearchBase()));
                    NamingEnumeration<SearchResult> searchResults = context.search(configuration.getSearchBase(), configuration.getSearchFilter(), controls);
                    List<Object> results = new ArrayList<>();
                    while (searchResults.hasMoreElements()) {
                        SearchResult searchResult = (SearchResult) searchResults.next();
                        Attributes attributes = searchResult.getAttributes();
                        Attribute attribute = attributes.get(propertyName);
                        if (attribute != null) {
                            if (attribute.size() > 1) {
                                results.addAll(Collections.list(attribute.getAll()));
                            } else {
                                results.add(attribute.get());
                            }
                        }
                    }
                    if (!results.isEmpty()) {
                        propertyValue = results
                                .stream()
                                .map(e -> String.valueOf(e))
                                .collect(joining(","));
                    }
                } else {
                    Attributes attributes = context.getAttributes(configuration.getBindDN());
                    Attribute attribute = attributes.get(propertyName);
                    if (attribute != null) {
                        if (attribute.size() > 1) {
                            propertyValue = Collections.list(attribute.getAll())
                                    .stream()
                                    .map(e -> String.valueOf(e))
                                    .collect(joining(","));
                        } else {
                            propertyValue = attribute.get().toString();
                        }
                    }
                }
            }
        } catch (NamingException ex) {
            logger.log(Level.WARNING, "Could not find the LDAP attibute named {0}:{1}", new Object[]{propertyName, ex.getMessage()});
        } finally {
            closeConnection(context, tlsResponse);
        }
        return propertyValue;
    }

    public synchronized Map<String, String> getAllConfigValues() {
        Map<String, String> configValues = new HashMap<>();
        StartTlsResponse tlsResponse = null;
        LdapContext context = getContext();
        if (Boolean.valueOf(configuration.getStartTLSEnabled())) {
            tlsResponse = startTLSConnection(context);
        }
        try {
            if (context != null) {
                if (StringUtils.ok(configuration.getSearchBase()) && StringUtils.ok(configuration.getSearchFilter())) {
                    SearchControls controls = new SearchControls();
                    controls.setReturningAttributes(null);
                    controls.setSearchScope(convertScopeValue(configuration.getSearchBase()));
                    NamingEnumeration<SearchResult> searchResults = context.search(configuration.getSearchBase(), configuration.getSearchFilter(), controls);
                    Map<String, List<Object>> results = new HashMap<>();
                    while (searchResults.hasMoreElements()) {
                        SearchResult searchResult = (SearchResult) searchResults.next();
                        Attributes attributes = searchResult.getAttributes();
                        NamingEnumeration<? extends Attribute> attributeEnumeration = attributes.getAll();
                        while (attributeEnumeration.hasMoreElements()) {
                            Attribute attribute = attributeEnumeration.nextElement();
                            List<Object> values = results.get(attribute.getID());
                            if (values == null) {
                                values = new ArrayList<>();
                                results.put(attribute.getID(), values);
                            }
                            values.add(attribute.get());
                        }
                    }
                    configValues = results
                            .entrySet()
                            .stream()
                            .collect(toMap(
                                    Entry::getKey,
                                    e -> e.getValue().stream().map(String::valueOf).collect(joining(","))
                            ));
                } else {
                    Attributes attributes = context.getAttributes(configuration.getBindDN());
                    NamingEnumeration<? extends Attribute> attributeEnumeration = attributes.getAll();
                    while (attributeEnumeration.hasMoreElements()) {
                        Attribute attribute = attributeEnumeration.nextElement();
                        configValues.put(attribute.getID(), attribute.get().toString());
                    }
                }
            }
        } catch (NamingException ex) {
            logger.log(Level.WARNING, "Could not fetch the LDAP attibutes:{0}", ex.getMessage());
        } finally {
            closeConnection(context, tlsResponse);
        }
        return configValues;
    }

    private LdapContext getContext() {
        LdapContext context = null;
        if (StringUtils.ok(configuration.getUrl())) {
            try {
                context = getContext(
                        configuration.getUrl(),
                        configuration.getBindDN(),
                        configuration.getBindDNPassword(),
                        Boolean.valueOf(configuration.getStartTLSEnabled()),
                        configuration.getConnectionTimeout(),
                        configuration.getReadTimeout()
                );
            } catch (NamingException ex) {
                logger.log(Level.SEVERE, "Could not create the LDAP context for {0}:{1}", new Object[]{configuration.getUrl(), ex.getMessage()});
            }
        }
        return context;
    }

    private StartTlsResponse startTLSConnection(LdapContext context) {
        StartTlsResponse tlsResponse = null;
        if (Boolean.valueOf(configuration.getStartTLSEnabled())) {
            try {
                tlsResponse = (StartTlsResponse) context.extendedOperation(new StartTlsRequest());
                if (tlsResponse == null) {
                    throw new NamingException("Could not establish the LDAP connection through StartTLS");
                }
                try {
                    tlsResponse.negotiate();
                } catch (IOException ex) {
                    throw new AuthenticationException("Could not negotiate TLS");
                }
                context.addToEnvironment(SECURITY_AUTHENTICATION, configuration.getAuthType());
                if (!AUTH_TYPE_NONE.equals(configuration.getAuthType())) {
                    context.addToEnvironment(SECURITY_PRINCIPAL, configuration.getBindDN());
                    context.addToEnvironment(SECURITY_CREDENTIALS, translatePassword(configuration.getBindDNPassword()));
                }
                context.lookup("");
            } catch (NamingException ex) {
                logger.log(Level.SEVERE, "Could not create the LDAP context for '{0}':{1}", new Object[]{configuration.getUrl(), ex.getMessage()});
            }
        }
        return tlsResponse;
    }

    private LdapContext getContext(String url,
            String bindDN, String bindDNPassword,
            boolean startTLS, String connectionTimeout, String readTimeout) throws NamingException {
        Hashtable<String, Object> environment = new Hashtable<>();
        environment.put(INITIAL_CONTEXT_FACTORY, LDAP_CONTEXT_FACTORY);
        environment.put(PROVIDER_URL, url);
        if (!startTLS) {
            environment.put(SECURITY_AUTHENTICATION, configuration.getAuthType());
            if (!AUTH_TYPE_NONE.equals(configuration.getAuthType())) {
                environment.put(SECURITY_PRINCIPAL, bindDN);
                environment.put(SECURITY_CREDENTIALS, translatePassword(bindDNPassword));
            }
        }
        if (StringUtils.ok(connectionTimeout)) {
            environment.put(LDAP_CONNECT_TIMEOUT, connectionTimeout);
        }
        if (StringUtils.ok(readTimeout)) {
            environment.put(LDAP_READ_TIMEOUT, readTimeout);
        }
        return new InitialLdapContext(environment, null);
    }

    private char[] translatePassword(String bindDNPassword) {
        if (bindDNPassword != null
                && TranslatedConfigView.getAlias(bindDNPassword) != null) {
            try {
                bindDNPassword = TranslatedConfigView.getRealPasswordFromAlias(bindDNPassword);
            } catch (Exception iae) {
                logger.log(Level.WARNING, iae.getMessage(), iae);
            }
        }
        return bindDNPassword != null ? bindDNPassword.toCharArray() : null;
    }

    private static void closeConnection(LdapContext context, StartTlsResponse response) {
        try {
            if (response != null) {
                response.close();
            }
        } catch (IOException ex) {
        }
        try {
            if (context != null) {
                context.close();
            }
        } catch (NamingException ex) {
        }
    }

    private static int convertScopeValue(String searchScope) {
        if (SEARCH_SCOPE_ONELEVEL.equals(searchScope)) {
            return ONELEVEL_SCOPE;
        } else if (SEARCH_SCOPE_SUBTREE.equals(searchScope)) {
            return SUBTREE_SCOPE;
        } else if (SEARCH_SCOPE_OBJECT.equals(searchScope)) {
            return OBJECT_SCOPE;
        } else {
            return ONELEVEL_SCOPE;
        }
    }

}
