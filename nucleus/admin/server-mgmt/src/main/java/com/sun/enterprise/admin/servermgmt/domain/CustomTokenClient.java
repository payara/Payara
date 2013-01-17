/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.admin.servermgmt.domain;

import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sun.enterprise.admin.servermgmt.DomainConfig;
import com.sun.enterprise.admin.servermgmt.DomainException;
import com.sun.enterprise.config.modularity.CustomizationTokensProviderFactory;
import com.sun.enterprise.config.modularity.customization.ConfigCustomizationToken;
import com.sun.enterprise.config.modularity.customization.CustomizationTokensProvider;
import com.sun.enterprise.config.modularity.customization.FileTypeDetails;
import com.sun.enterprise.config.modularity.customization.PortTypeDetails;
import com.sun.enterprise.universal.i18n.LocalStringsImpl;
import com.sun.enterprise.util.SystemPropertyConstants;
import com.sun.enterprise.util.net.NetUtils;

/**
 * Client class to retrieve customize tokens.
 */
public class CustomTokenClient {

    private static final Logger _logger = Logger.getLogger(CustomTokenClient.class.getPackage().getName());
    private static final LocalStringsImpl _strings = new LocalStringsImpl(DomainBuilder.class);
    /** Place holder for the PORT BASE value in domain.xml */
    public static final String PORTBASE_PLACE_HOLDER = "%%%PORT_BASE%%%";
    /** Place holder for the custom tokens in domain.xml */
    public static final String CUSTOM_TOKEN_PLACE_HOLDER = "%%%TOKENS_HERE%%%";

    private DomainConfig _domainConfig;
    private File _configDirLocation;

    public CustomTokenClient(DomainConfig domainConfig, File configDirLocation) {
        _domainConfig = domainConfig;
        _configDirLocation = configDirLocation;
    }

    /**
     * Get's the substitutable custom tokens.
     *
     * @return {@link Map} of substitutable tokens, or empty Map
     *   if no custom token found.
     * @throws DomainException If error occurred in retrieving the
     *   custom tokens.
     */
    public Map<String, String> getSubstitutableTokens() throws DomainException {
        CustomizationTokensProvider provider = CustomizationTokensProviderFactory.createCustomizationTokensProvider();
        Map<String, String> generatedTokens = new HashMap<String, String>();
        try {
            List<ConfigCustomizationToken> customTokens = provider.getPresentConfigCustomizationTokens();
            if (!customTokens.isEmpty()) {
                StringBuffer newlyCreatedSysProps = new StringBuffer();
                File domainXMLFile = new File(_configDirLocation, DomainConstants.DOMAIN_XML_FILE);
                FileReader reader = null;
                String loadedDomainXML = null;
                int count = 0;
                try {
                    reader = new FileReader(domainXMLFile);
                    char[] buf = new char[(int)domainXMLFile.length()];
                    count = reader.read(buf);
                    if (count < buf.length) {
                        throw new DomainException(_strings.get("loadingFailure", domainXMLFile.getAbsolutePath()));
                    }
                    loadedDomainXML = new String(buf);
                } finally {
                    if (reader != null) {
                        reader.close();
                    }
                }

                // Check presence of token place-holder
                if (loadedDomainXML.indexOf(CUSTOM_TOKEN_PLACE_HOLDER) != -1) {
                    Set<String> existingSysProps = new HashSet<String>();
                    Pattern sysPropTagPattern = Pattern.compile("<system-property.*?>");
                    Pattern nameAttributePattern = Pattern.compile("name=[\\\"](.*?)[\\\"]");
                    Matcher sysPropTagMatcher = sysPropTagPattern.matcher(loadedDomainXML);
                    while (sysPropTagMatcher.find()) {
                        String systemTag = sysPropTagMatcher.group();
                        Matcher nameTagMatcher = nameAttributePattern.matcher(systemTag);
                        while (nameTagMatcher.find()) {
                            String strs[] = nameTagMatcher.group().split("\"");
                            if (strs != null && strs.length == 2) {
                                existingSysProps.add(strs[1]);
                            }
                        }
                    }
                    Set<Integer> usedPorts = new HashSet<Integer>();
                    Properties domainProps = _domainConfig.getDomainProperties();
                    String portBase = (String)_domainConfig.get(DomainConfig.K_PORTBASE);

                    Map<String, String> filePaths = new HashMap<String, String>(3, 1);
                    filePaths.put(SystemPropertyConstants.INSTALL_ROOT_PROPERTY, System.getProperty(SystemPropertyConstants.INSTALL_ROOT_PROPERTY));
                    filePaths.put(SystemPropertyConstants.INSTANCE_ROOT_PROPERTY, System.getProperty(SystemPropertyConstants.INSTANCE_ROOT_PROPERTY));
                    filePaths.put(SystemPropertyConstants.JAVA_ROOT_PROPERTY, System.getProperty(SystemPropertyConstants.JAVA_ROOT_PROPERTY));
                    int noOfTokens = customTokens.size();
                    String lineSeparator = System.getProperty("line.separator");
                    for (ConfigCustomizationToken token : customTokens) {
                        String key = token.getName();
                        // Check for valid custom token parameters.
                        if (isNullOrEmpty(key) || isNullOrEmpty(token.getValue()) || isNullOrEmpty(token.getDescription())) {
                            throw new IllegalArgumentException(_strings.get("invalidTokenParameters", key, token.getValue(), token.getDescription()));
                        }
                        // Skipping the token processing if token already exist in domain.xml
                        if (existingSysProps.contains(key)) {
                            _logger.log(Level.FINER, _strings.get("existingCustomToken", key));
                            continue;
                        }
                        switch (token.getCustomizationType()) {
                            case PORT :
                                Integer port = null;
                                if (domainProps.containsKey(token.getName())) {
                                    port = Integer.valueOf(domainProps.getProperty(token.getName()));
                                    if (!NetUtils.isPortFree(port)) {
                                        throw new DomainException(_strings.get("unavailablePort", port));
                                    }
                                } else {
                                    if (portBase != null && token.getTokenTypeDetails() instanceof PortTypeDetails) {
                                        PortTypeDetails portTypeDetails = (PortTypeDetails)token.getTokenTypeDetails();
                                        port = Integer.valueOf(domainProps.getProperty(token.getName())) + Integer.valueOf(portTypeDetails.getBaseOffset());
                                        generatedTokens.put(PORTBASE_PLACE_HOLDER, SystemPropertyTagBuilder.buildSystemTag(
                                                PORTBASE_PLACE_HOLDER, portBase));
                                    } else {
                                        port = Integer.valueOf(token.getValue());
                                    }
                                    while (!NetUtils.isPortFree(port) && !usedPorts.contains(port++));
                                }
                                usedPorts.add(port);
                                newlyCreatedSysProps.append(SystemPropertyTagBuilder.buildSystemTag(token, port.toString()));
                                break;
                            case FILE:
                                String path = token.getValue();
                                for (Map.Entry<String, String> entry : filePaths.entrySet()) {
                                    if (path.contains(entry.getKey())) {
                                        path = path.replace(entry.getKey(), entry.getValue());
                                        break;
                                    }
                                }
                                if (token.getTokenTypeDetails() instanceof FileTypeDetails) {
                                    FileTypeDetails details = (FileTypeDetails) token.getTokenTypeDetails();
                                    File file = new File(path);
                                    switch (details.getExistCondition()) {
                                        case MUST_EXIST:
                                            if (!file.exists()) {
                                                throw new DomainException(_strings.get("missingFile", file.getAbsolutePath()));
                                            }
                                            break;
                                        case MUST_NOT_EXIST:
                                            if (file.exists()) {
                                                throw new DomainException(_strings.get("filePresenceNotDesired", file.getAbsolutePath()));
                                            }
                                            break;
                                        case NO_OP:
                                            break;
                                    }
                                }
                                newlyCreatedSysProps.append(SystemPropertyTagBuilder.buildSystemTag(token, path));
                                break;
                            case STRING:
                                break;
                        }
                        if (--noOfTokens > 1) {
                            newlyCreatedSysProps.append(lineSeparator);
                        }
                    }
                    generatedTokens.put(CUSTOM_TOKEN_PLACE_HOLDER, newlyCreatedSysProps.toString());
                }
            }
        } catch (DomainException de) {
            throw de;
        } catch (Exception ex) {
            throw new DomainException(ex);
        }
        return generatedTokens;
    }

    /**
     * Check for empty or null input string.
     * @return true Only if given string string is null or empty.
     */
    private boolean isNullOrEmpty(String input) {
        return input == null || input.isEmpty();
    }

    /**
     * A builder class to build the custom tag.
     */
    private static class SystemPropertyTagBuilder {

        private static final String placeHolderTagWithDesc = "<system-property name=\"%%%NAME%%%\" value=\"%%%VALUE%%%\" description=\"%%%DESCRIPTION%%%\" />";
        private static final String placeHolderTagWithoutDesc = "<system-property name=\"%%%NAME%%%\" value=\"%%%VALUE%%%\" />";
        private static final String namePlaceHolder = "%%%NAME%%%";
        private static final String valuePlaceHolder = "%%%VALUE%%%";
        private static final String descriptionPlaceHolder = "%%%DESCRIPTION%%%";

        /**
         * Build the System tag for the given token & value.
         */
        private static String buildSystemTag(ConfigCustomizationToken token, String value) {
            String builtTag = placeHolderTagWithDesc.replace(valuePlaceHolder, value);
            builtTag = builtTag.replace(descriptionPlaceHolder, token.getDescription());
            builtTag = builtTag.replace(namePlaceHolder, token.getName());
            return builtTag;
        }

        /**
         * Build the System tag for the given name & value.
         */
        private static String buildSystemTag(String name, String value) {
            String builtTag = placeHolderTagWithoutDesc.replace(valuePlaceHolder, value);
            builtTag = builtTag.replace(namePlaceHolder, name);
            return builtTag;
        }
    } 
}
