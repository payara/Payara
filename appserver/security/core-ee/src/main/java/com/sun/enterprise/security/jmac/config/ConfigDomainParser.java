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

package com.sun.enterprise.security.jmac.config;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.IOException;
import javax.security.auth.message.MessagePolicy;

import com.sun.enterprise.config.serverbeans.MessageSecurityConfig;
import org.jvnet.hk2.config.types.Property;
import com.sun.enterprise.config.serverbeans.ProviderConfig;
import com.sun.enterprise.config.serverbeans.RequestPolicy;
import com.sun.enterprise.config.serverbeans.ResponsePolicy;

import com.sun.enterprise.config.serverbeans.SecurityService;
import com.sun.enterprise.security.common.Util;
import com.sun.enterprise.security.jmac.AuthMessagePolicy;

import com.sun.logging.LogDomains;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.internal.api.Globals;

import sun.security.util.PropertyExpander;
import sun.security.util.PropertyExpander.ExpandException;


/**
 * Parser for message-security-config in domain.xml
 */
public class ConfigDomainParser implements ConfigParser {

    private static Logger _logger=null;
    static {
        _logger = LogDomains.getLogger(ConfigDomainParser.class, LogDomains.SECURITY_LOGGER);
    }

    // configuration info
    private Map configMap = new HashMap();
    private Set<String> layersWithDefault = new HashSet<String>();

    public ConfigDomainParser() throws IOException {
    }

    public void initialize(Object service) throws IOException {
	if (service == null && Globals.getDefaultHabitat() != null) {
	    service = Globals.getDefaultHabitat().getService(SecurityService.class,
                    ServerEnvironment.DEFAULT_INSTANCE_NAME);
	}

	if (service instanceof SecurityService) {
	    processServerConfig((SecurityService) service,configMap);
	} /*else {
            throw new IOException("invalid configBean type passed to parser");
	}*/
    }

    private void processServerConfig(SecurityService service, Map newConfig) 
	throws IOException {
  
	List<MessageSecurityConfig> configList = 
	    service.getMessageSecurityConfig();

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

    public Map getConfigMap() {
        return configMap;
    }

    public Set<String> getLayersWithDefault() {
        return layersWithDefault;
    }
    
    private String parseInterceptEntry(MessageSecurityConfig msgConfig, 
				       Map newConfig) throws IOException {

        String intercept = null;
        String defaultServerID = null;
        String defaultClientID = null;

	intercept = msgConfig.getAuthLayer();
        defaultServerID = msgConfig.getDefaultProvider();
        defaultClientID = msgConfig.getDefaultClientProvider();

        if (_logger.isLoggable(Level.FINE)) {
            _logger.fine("Intercept Entry: " +
                        "\n    intercept: " + intercept +
                        "\n    defaultServerID: " + defaultServerID +
                        "\n    defaultClientID:  " + defaultClientID);
        }

        if (defaultServerID != null || defaultClientID != null) {
            layersWithDefault.add(intercept);
        }

        GFServerConfigProvider.InterceptEntry intEntry =
            (GFServerConfigProvider.InterceptEntry)newConfig.get(intercept);

        if (intEntry != null) {
            throw new IOException("found multiple MessageSecurityConfig " +
                                "entries with the same auth-layer");
        }

        // create new intercept entry
        intEntry = new GFServerConfigProvider.InterceptEntry(defaultClientID,
                defaultServerID, null);
        newConfig.put(intercept, intEntry);
        return intercept;
    }

    private void parseIDEntry(ProviderConfig pConfig,
			      Map newConfig, String intercept)
	throws IOException {

        String id = pConfig.getProviderId();
        String type = pConfig.getProviderType();
        String moduleClass = pConfig.getClassName();
        MessagePolicy requestPolicy = 
	    parsePolicy((RequestPolicy) pConfig.getRequestPolicy());
        MessagePolicy responsePolicy = 
	    parsePolicy((ResponsePolicy) pConfig.getResponsePolicy());

        // get the module options

        Map options = new HashMap();
        String key;
        String value;

	List<Property> pList = pConfig.getProperty();

	if (pList != null) {

	    Iterator<Property> pit = pList.iterator();

	    while (pit.hasNext()) {

		Property property = pit.next();

		try {
		    options.put
			(property.getName(),
			 PropertyExpander.expand(property.getValue(),false));
		} catch (ExpandException ee) {
		    // log warning and give the provider a chance to 
		    // interpret value itself.
		    if (_logger.isLoggable(Level.FINE)) {
			_logger.log(Level.FINE, "jmac.unexpandedproperty");
		    }
		    options.put(property.getName(),property.getValue());
		}
            }
        }

        if (_logger.isLoggable(Level.FINE)) {
            _logger.fine("ID Entry: " +
                        "\n    module class: " + moduleClass +
                        "\n    id: " + id +
                        "\n    type: " + type +
                        "\n    request policy: " + requestPolicy +
                        "\n    response policy: " + responsePolicy +
                        "\n    options: " + options);
        }

        // create ID entry
        GFServerConfigProvider.IDEntry idEntry =
                new GFServerConfigProvider.IDEntry(type, moduleClass,
                requestPolicy, responsePolicy, options);

        GFServerConfigProvider.InterceptEntry intEntry =
                (GFServerConfigProvider.InterceptEntry)newConfig.get(intercept);
        if (intEntry == null) {
            throw new IOException
                ("intercept entry for " + intercept +
                " must be specified before ID entries");
        }

        if (intEntry.idMap == null) {
            intEntry.idMap = new HashMap();
        }

        // map id to Intercept
        intEntry.idMap.put(id, idEntry);
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
