/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2021 Oracle and/or its affiliates. All rights reserved.
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
// Portions Copyright 2019-2022 Payara Foundation and/or its affiliates
// Payara Foundation and/or its affiliates elects to include this software in this distribution under the GPL Version 2 license
package com.sun.enterprise.security.jaspic.config;

import com.sun.enterprise.config.serverbeans.MessageSecurityConfig;
import com.sun.enterprise.config.serverbeans.ProviderConfig;
import com.sun.enterprise.config.serverbeans.RequestPolicy;
import com.sun.enterprise.config.serverbeans.ResponsePolicy;
import com.sun.enterprise.config.serverbeans.SecurityService;
import com.sun.enterprise.security.jaspic.AuthMessagePolicy;
import com.sun.logging.LogDomains;
import jakarta.security.auth.message.MessagePolicy;
import org.glassfish.internal.api.Globals;
import org.jvnet.hk2.config.types.Property;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.sun.logging.LogDomains.SECURITY_LOGGER;
import static java.util.logging.Level.FINE;
import static org.glassfish.api.admin.ServerEnvironment.DEFAULT_INSTANCE_NAME;

/**
 * Parser for message-security-config in domain.xml
 */
public class ConfigDomainParser implements ConfigParser {

    private static final Logger _logger = LogDomains.getLogger(ConfigDomainParser.class, SECURITY_LOGGER);

    private static final Pattern PROPERTY_PATTERN = Pattern.compile("\\$\\{\\{(.*?)}}|\\$\\{(.*?)}");

    // configuration info
    private Map<String, GFServerConfigProvider.InterceptEntry> configMap = new HashMap<>();
    private Set<String> layersWithDefault = new HashSet<String>();

    public ConfigDomainParser() throws IOException {
    }

    public void initialize(Object service) throws IOException {
        if (service == null && Globals.getDefaultHabitat() != null) {
            service = Globals.getDefaultHabitat().getService(SecurityService.class, DEFAULT_INSTANCE_NAME);
        }

        if (service instanceof SecurityService) {
            processServerConfig((SecurityService) service, configMap);
        }
    }

    private void processServerConfig(SecurityService service, Map<String, GFServerConfigProvider.InterceptEntry> newConfig) throws IOException {
        List<MessageSecurityConfig> configList = service.getMessageSecurityConfig();

        if (configList != null) {
            Iterator<MessageSecurityConfig> cit = configList.iterator();

            while (cit.hasNext()) {
                MessageSecurityConfig next = cit.next();

                // single message-security-config for each auth-layer
                // auth-layer is synonymous with intercept
                String intercept = parseInterceptEntry(next, newConfig);

                List<ProviderConfig> provList = next.getProviderConfig();
                if (provList != null) {
                    Iterator<ProviderConfig> pit = provList.iterator();
                    while (pit.hasNext()) {
                        ProviderConfig provider = pit.next();
                        parseIDEntry(provider, newConfig, intercept);
                    }
                }
            }
        }
    }

    public Map<String, GFServerConfigProvider.InterceptEntry> getConfigMap() {
        return configMap;
    }

    public Set<String> getLayersWithDefault() {
        return layersWithDefault;
    }

    private String parseInterceptEntry(MessageSecurityConfig msgConfig, Map<String, GFServerConfigProvider.InterceptEntry> newConfig) throws IOException {
        String intercept = null;
        String defaultServerID = null;
        String defaultClientID = null;

        intercept = msgConfig.getAuthLayer();
        defaultServerID = msgConfig.getDefaultProvider();
        defaultClientID = msgConfig.getDefaultClientProvider();

        if (_logger.isLoggable(FINE)) {
            _logger.fine("Intercept Entry: " + "\n    intercept: " + intercept + "\n    defaultServerID: "
                    + defaultServerID + "\n    defaultClientID:  " + defaultClientID);
        }

        if (defaultServerID != null || defaultClientID != null) {
            layersWithDefault.add(intercept);
        }

        GFServerConfigProvider.InterceptEntry intEntry = newConfig.get(intercept);

        if (intEntry != null) {
            throw new IOException("found multiple MessageSecurityConfig " + "entries with the same auth-layer");
        }

        // create new intercept entry
        intEntry = new GFServerConfigProvider.InterceptEntry(defaultClientID, defaultServerID, null);
        newConfig.put(intercept, intEntry);
        return intercept;
    }

    private void parseIDEntry(ProviderConfig pConfig, Map<String, GFServerConfigProvider.InterceptEntry> newConfig, String intercept) throws IOException {

        String id = pConfig.getProviderId();
        String type = pConfig.getProviderType();
        String moduleClass = pConfig.getClassName();
        MessagePolicy requestPolicy = parsePolicy(pConfig.getRequestPolicy());
        MessagePolicy responsePolicy = parsePolicy(pConfig.getResponsePolicy());

        // get the module options

        Map<String, Object> options = new HashMap<>();

        List<Property> pList = pConfig.getProperty();

        if (pList != null) {
            Iterator<Property> pit = pList.iterator();
            while (pit.hasNext()) {
                Property property = pit.next();

                try {
                    options.put(property.getName(), expand(property.getValue()));
                } catch (IllegalStateException ise) {
                    // log warning and give the provider a chance to interpret value itself.
                    if (_logger.isLoggable(Level.FINE)) {
                        _logger.log(Level.FINE, "jaspic.unexpandedproperty");
                    }
                    options.put(property.getName(), property.getValue());
                }
            }
        }

        if (_logger.isLoggable(FINE)) {
            _logger.fine("ID Entry: " + "\n    module class: " + moduleClass + "\n    id: " + id + "\n    type: " + type
                    + "\n    request policy: " + requestPolicy + "\n    response policy: " + responsePolicy + "\n    options: " + options);
        }

        // create ID entry
        GFServerConfigProvider.IDEntry idEntry = new GFServerConfigProvider.IDEntry(type, moduleClass, requestPolicy, responsePolicy,
                options);

        GFServerConfigProvider.InterceptEntry intEntry = newConfig.get(intercept);
        if (intEntry == null) {
            throw new IOException("intercept entry for " + intercept + " must be specified before ID entries");
        }

        if (intEntry.idMap == null) {
            intEntry.idMap = new HashMap<>();
        }

        // map id to Intercept
        intEntry.idMap.put(id, idEntry);
    }

    private String expand(String rawProperty) {
        Matcher propertyMatcher = PROPERTY_PATTERN.matcher(rawProperty);
        StringBuilder propertyBuilder = new StringBuilder();
        while (propertyMatcher.find()) {
            // Check if the ignore pattern matched
            if (propertyMatcher.group(1) != null) {
                // Ignore ${{...}} matched, so just append everything
                propertyMatcher.appendReplacement(propertyBuilder, Matcher.quoteReplacement(propertyMatcher.group()));
            } else {

                String replacement = System.getProperty(propertyMatcher.group(2));
                if (replacement == null) {
                    throw new IllegalStateException("No system property for " + propertyMatcher.group(2));
                }

                // The replacement pattern matched
                propertyMatcher.appendReplacement(propertyBuilder, Matcher.quoteReplacement(replacement));
            }
        }
        propertyMatcher.appendTail(propertyBuilder);

        return propertyBuilder.toString();
    }

    private MessagePolicy parsePolicy(RequestPolicy policy) {

        if (policy == null) {
            return null;
        }

        String authSource = policy.getAuthSource();
        String authRecipient = policy.getAuthRecipient();
        return AuthMessagePolicy.getMessagePolicy(authSource, authRecipient);
    }

    private MessagePolicy parsePolicy(ResponsePolicy policy) {

        if (policy == null) {
            return null;
        }

        String authSource = policy.getAuthSource();
        String authRecipient = policy.getAuthRecipient();
        return AuthMessagePolicy.getMessagePolicy(authSource, authRecipient);
    }
    
}