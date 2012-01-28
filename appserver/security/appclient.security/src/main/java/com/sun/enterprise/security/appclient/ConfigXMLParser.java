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

package com.sun.enterprise.security.appclient;

import com.sun.enterprise.security.common.Util;
import com.sun.enterprise.security.jmac.config.*;
import com.sun.enterprise.security.jmac.config.GFServerConfigProvider;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.IOException;

import com.sun.logging.LogDomains;

import java.util.List;
import javax.xml.bind.JAXBException;
import org.glassfish.appclient.client.acc.config.*;
import sun.security.util.PropertyExpander;
import com.sun.enterprise.security.jmac.AuthMessagePolicy;
import java.io.FileInputStream;
import java.io.InputStream;
import javax.security.auth.message.MessagePolicy;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import org.glassfish.internal.api.Globals;


/**
 * Parser for message-security-config in  glassfish-acc.xml
 */
public class ConfigXMLParser implements ConfigParser { 
    private static Logger _logger=null;
    static {
        _logger = LogDomains.getLogger(ConfigXMLParser.class, LogDomains.SECURITY_LOGGER);
    }

    // configuration info
    private Map configMap = new HashMap();
    private Set<String> layersWithDefault = new HashSet<String>();
    private List<MessageSecurityConfig> msgSecConfigs = null;
    private static final String ACC_XML = "glassfish-acc.xml.url";

    public ConfigXMLParser() throws IOException {
    }

    public void initialize(List<MessageSecurityConfig> msgConfigs) throws IOException {
        this.msgSecConfigs = msgConfigs;
        if (this.msgSecConfigs != null) {
            processClientConfigContext(configMap);
        }
    }


    private void processClientConfigContext(Map newConfig) throws IOException {
        // auth-layer
        String intercept = null;

        List<MessageSecurityConfig> msgConfigs = this.msgSecConfigs;
        assert(msgConfigs != null);
        for (MessageSecurityConfig config : msgConfigs) {
            intercept = parseInterceptEntry(config, newConfig);
            List<ProviderConfig> pConfigs = config.getProviderConfig();
            for (ProviderConfig pConfig : pConfigs) {
                parseIDEntry(pConfig, newConfig, intercept);
            }
        }

    }

    public Map getConfigMap() {
        return configMap;
    }

    public Set<String> getLayersWithDefault() {
        return layersWithDefault;
    }
    
    private String parseInterceptEntry(
            MessageSecurityConfig msgConfig, Map newConfig) throws IOException {

        String intercept = null;
        String defaultServerID = null;
        String defaultClientID = null;
        MessageSecurityConfig clientMsgSecConfig = msgConfig;
        intercept = clientMsgSecConfig.getAuthLayer();
        defaultServerID = clientMsgSecConfig.getDefaultProvider();
        defaultClientID = clientMsgSecConfig.getDefaultClientProvider();
        
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

    // duplicate implementation for clientbeans config
    private void parseIDEntry(
            ProviderConfig pConfig,
            Map newConfig, String intercept)
            throws IOException {

        String id = pConfig.getProviderId();
        String type = pConfig.getProviderType();
        String moduleClass = pConfig.getClassName();
        MessagePolicy requestPolicy = parsePolicy(pConfig.getRequestPolicy());
        MessagePolicy responsePolicy = parsePolicy(pConfig.getResponsePolicy());

        // get the module options

        Map options = new HashMap();
        List<Property> props = pConfig.getProperty();
        for (Property prop : props) {
            try {
                options.put(prop.getName(),
                            PropertyExpander.expand
                            (prop.getValue(),
                             false));
            } catch (sun.security.util.PropertyExpander.ExpandException ee) {
                // log warning and give the provider a chance to 
                // interpret value itself.
                if (_logger.isLoggable(Level.WARNING)) {
                    _logger.warning("jmac.unexpandedproperty");
                }
                options.put(prop.getName(),
                            prop.getValue());
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

        if (intEntry.getIdMap() == null) {
            intEntry.setIdMap(new HashMap());
        }

        // map id to Intercept
        intEntry.getIdMap().put(id, idEntry);
    }

    
    private MessagePolicy parsePolicy(Object policy) {

        if (policy == null) {
            return null;
        }
        String authSource = null;
        String authRecipient = null;

       if (policy instanceof RequestPolicy) {
            RequestPolicy clientRequestPolicy = (RequestPolicy)policy;
            authSource = clientRequestPolicy.getAuthSource();
            authRecipient = clientRequestPolicy.getAuthRecipient();
        } else if (policy instanceof ResponsePolicy) {
            ResponsePolicy clientResponsePolicy = (ResponsePolicy)policy;
            authSource = clientResponsePolicy.getAuthSource();
            authRecipient = clientResponsePolicy.getAuthRecipient();
        }
        return AuthMessagePolicy.getMessagePolicy(authSource, authRecipient);
    }

    public void initialize(Object config) throws IOException {
        String sun_acc = System.getProperty(ACC_XML, "glassfish-acc.xml");
        List<MessageSecurityConfig> msgconfigs = null;
        if (Globals.getDefaultHabitat() == null && sun_acc != null) {
            InputStream is = null;
            try {
                is = new FileInputStream(sun_acc);
                JAXBContext jc = JAXBContext.newInstance(ClientContainer.class);
                Unmarshaller u = jc.createUnmarshaller();
                ClientContainer cc = (ClientContainer) u.unmarshal(is);
                msgconfigs = cc.getMessageSecurityConfig();
            } catch (JAXBException ex) {
                _logger.log(Level.SEVERE, null, ex);
            } finally {
                if (is != null) {
                    is.close();
                }
            }
        } else {
            Util util = Util.getInstance();
            assert (util != null);
            msgconfigs = (List<MessageSecurityConfig>) util.getAppClientMsgSecConfigs();
        }
        this.initialize(msgconfigs);
        //this.initialize(config);
    }
}
