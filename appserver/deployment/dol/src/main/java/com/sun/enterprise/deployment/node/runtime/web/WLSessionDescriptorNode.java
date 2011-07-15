/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.deployment.node.runtime.web;

import com.sun.enterprise.deployment.CookieConfigDescriptor;
import com.sun.enterprise.deployment.SessionConfigDescriptor;
import com.sun.enterprise.deployment.WebBundleDescriptor;
import com.sun.enterprise.deployment.runtime.web.ManagerProperties;
import com.sun.enterprise.deployment.runtime.web.SunWebApp;
import com.sun.enterprise.deployment.runtime.web.SessionManager;
import com.sun.enterprise.deployment.runtime.web.SessionProperties;
import com.sun.enterprise.deployment.runtime.web.StoreProperties;
import com.sun.enterprise.deployment.runtime.web.WebProperty;
import com.sun.enterprise.deployment.runtime.web.WebPropertyContainer;
import com.sun.enterprise.deployment.web.CookieConfig;
import com.sun.enterprise.deployment.web.SessionConfig;
import com.sun.enterprise.deployment.node.XMLElement;
import com.sun.enterprise.deployment.node.XMLNode;
import com.sun.enterprise.deployment.node.runtime.RuntimeDescriptorNode;
import com.sun.enterprise.deployment.xml.RuntimeTagNames;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.util.Set;
import javax.servlet.SessionTrackingMode;

/**
 * This node is responsible for handling weblogic.xml session-descriptor.
 *
 * @author  Shing Wai Chan
 */
public class WLSessionDescriptorNode extends RuntimeDescriptorNode {
    private static final String COOKIE = "COOKIE";
    private static final String URL = "URL";
    private static final String SSL = "SSL";

    // name from sun-web.xml
    private static final String TIMEOUT_SECONDS = "timeoutSeconds";
    private static final String REAP_INTERVAL_SECONDS = "reapIntervalSeconds";
    private static final String MAX_SESSIONS = "maxSessions";
    private static final String DIRECTORY = "directory";

    // default value from weblogic
    private boolean cookieTrackingMode = true;
    private boolean urlTrackingMode = true;
    private int timeoutSecs = 3600;

    /**
     * receives notification of the value for a particular tag
     * 
     * @param element the xml element
     * @param value it's associated value
     */
    public void setElementValue(XMLElement element, String value) {
        String name = element.getQName();
        if (name.equals(RuntimeTagNames.TIMEOUT_SECS)) {
            timeoutSecs = Integer.parseInt(value);
        } else if (name.equals(RuntimeTagNames.INVALIDATION_INTERVAL_SECS)) {
            // make sure that it is an integer
            int reapIntervalSeconds = Integer.parseInt(value);
            addManagerProperty(REAP_INTERVAL_SECONDS, value);
        } else if (name.equals(RuntimeTagNames.MAX_IN_MEMORY_SESSIONS)) {
            // make sure that it is an integer
            int maxSessions = Integer.parseInt(value);
            addManagerProperty(MAX_SESSIONS, value);
        } else if (name.equals(RuntimeTagNames.COOKIE_NAME)) {
            CookieConfig cookieConfig = getCookieConfig();
            cookieConfig.setName(value);
        } else if (name.equals(RuntimeTagNames.COOKIE_PATH)) {
            CookieConfig cookieConfig = getCookieConfig();
            cookieConfig.setPath(value);
        } else if (name.equals(RuntimeTagNames.COOKIE_DOMAIN)) {
            CookieConfig cookieConfig = getCookieConfig();
            cookieConfig.setDomain(value);
        } else if (name.equals(RuntimeTagNames.COOKIE_COMMENT)) {
            CookieConfig cookieConfig = getCookieConfig();
            cookieConfig.setComment(value);
        } else if (name.equals(RuntimeTagNames.COOKIE_SECURE)) {
            CookieConfig cookieConfig = getCookieConfig();
            cookieConfig.setSecure(Boolean.parseBoolean(value));
        } else if (name.equals(RuntimeTagNames.COOKIE_MAX_AGE_SECS)) {
            CookieConfig cookieConfig = getCookieConfig();
            cookieConfig.setMaxAge(Integer.parseInt(value));
        } else if (name.equals(RuntimeTagNames.COOKIE_HTTP_ONLY)) {
            CookieConfig cookieConfig = getCookieConfig();
            cookieConfig.setHttpOnly(Boolean.parseBoolean(value));
        } else if (name.equals(RuntimeTagNames.COOKIES_ENABLED)) {
            cookieTrackingMode = Boolean.parseBoolean(value);
        } else if (name.equals(RuntimeTagNames.URL_REWRITING_ENABLED)) {
            urlTrackingMode = Boolean.parseBoolean(value);
        } else if (name.equals(RuntimeTagNames.PERSISTENT_STORE_DIR)) {
            addStoreProperty(DIRECTORY, value);
        } else {
            super.setElementValue(element, value);
        }
    }

    /** 
     * notification of the end of an XML element in the source XML 
     * file. 
     * 
     * @param element the XML element type name
     * @return true if this node is done with the processing of elements 
     * in the processing
     */
    public boolean endElement(XMLElement element) {
        if (RuntimeTagNames.SESSION_DESCRIPTOR.equals(element.getQName())) {
            com.sun.enterprise.deployment.runtime.web.SessionConfig runtimeSessionConfig =
                    getRuntimeSessionConfig();
            SessionProperties sessionProperties = runtimeSessionConfig.getSessionProperties();
            if (sessionProperties == null) {
                sessionProperties = new SessionProperties();
                runtimeSessionConfig.setSessionProperties(sessionProperties);
            }
            addWebProperty(sessionProperties, TIMEOUT_SECONDS, Integer.toString(timeoutSecs));
 
            if (cookieTrackingMode && urlTrackingMode) {
                SessionConfig sessionConfig = getSessionConfig();
                sessionConfig.addTrackingMode(COOKIE);
                sessionConfig.addTrackingMode(URL);
            } else if (!cookieTrackingMode && urlTrackingMode) {
                SessionConfig sessionConfig = getSessionConfig();
                sessionConfig.removeTrackingMode(COOKIE);
                sessionConfig.addTrackingMode(URL);
            } else if (cookieTrackingMode && !urlTrackingMode) {
                SessionConfig sessionConfig = getSessionConfig();
                sessionConfig.addTrackingMode(COOKIE);
                sessionConfig.removeTrackingMode(URL);
            } else {
                SessionConfig sessionConfig = getSessionConfig();
                sessionConfig.removeTrackingMode(COOKIE);
                sessionConfig.removeTrackingMode(URL);
                // turn off cookie and url, only ssl is left
                sessionConfig.addTrackingMode(SSL);
            }
        }
        return super.endElement(element);
    }

    /**
     * @return the descriptor instance to associate with this XMLNode
     */    
    public Object getDescriptor() {
        return null;
    }

    public Node writeDescriptor(Element root, WebBundleDescriptor webBundleDescriptor) {
        SessionConfig sessionConfig = webBundleDescriptor.getSessionConfig();
        com.sun.enterprise.deployment.runtime.web.SessionConfig runtimeSessionConfig =
                webBundleDescriptor.getSunDescriptor().getSessionConfig();
        Node scNode = null;
        if (sessionConfig != null || runtimeSessionConfig != null) {
            scNode = appendChild(root, RuntimeTagNames.SESSION_DESCRIPTOR);
        }

        if (runtimeSessionConfig != null) {
            // timeout-secs
            SessionProperties sessionProperties = runtimeSessionConfig.getSessionProperties();
            if (sessionProperties != null && sessionProperties.sizeWebProperty() > 0) {
                for (WebProperty prop : sessionProperties.getWebProperty()) {
                    String name = prop.getAttributeValue(WebProperty.NAME);
                    String value = prop.getAttributeValue(WebProperty.VALUE);
                    if (TIMEOUT_SECONDS.equals(name)) {
                        appendTextChild(scNode, RuntimeTagNames.TIMEOUT_SECS, value);
                        break;
                    }
                }
            }

            // invalidation-interval-secs, max-in-memory-sessions
            SessionManager sessionManager = runtimeSessionConfig.getSessionManager();
            if (sessionManager != null) {
                ManagerProperties managerProperties = sessionManager.getManagerProperties();
                if (managerProperties != null && managerProperties.sizeWebProperty() > 0) {
                    for (WebProperty prop : managerProperties.getWebProperty()) {
                        String name = prop.getAttributeValue(WebProperty.NAME);
                        String value = prop.getAttributeValue(WebProperty.VALUE);
                        if (name.equals(REAP_INTERVAL_SECONDS)) {
                            appendTextChild(scNode,
                                    RuntimeTagNames.INVALIDATION_INTERVAL_SECS, value);
                        } else if (name.equals(MAX_SESSIONS)) {
                            appendTextChild(scNode,
                                    RuntimeTagNames.MAX_IN_MEMORY_SESSIONS, value);
                        }
                    }
                }
            }
        }

        if (sessionConfig != null) {
            Set<SessionTrackingMode> trackingModes = sessionConfig.getTrackingModes();
            if (trackingModes.contains(SessionTrackingMode.COOKIE)) {
                appendTextChild(scNode, RuntimeTagNames.COOKIES_ENABLED, "true");
            }

            CookieConfig cookieConfig = sessionConfig.getCookieConfig();
            if (cookieConfig != null) {
                if (cookieConfig.getName() != null && cookieConfig.getName().length() > 0) {
                    appendTextChild(scNode, RuntimeTagNames.COOKIE_NAME, cookieConfig.getName());
                }
                if (cookieConfig.getPath() != null) {
                    appendTextChild(scNode, RuntimeTagNames.COOKIE_PATH, cookieConfig.getPath());
                }
                if (cookieConfig.getDomain() != null) {
                    appendTextChild(scNode, RuntimeTagNames.COOKIE_DOMAIN, cookieConfig.getDomain());
                }
                if (cookieConfig.getComment() != null) {
                    appendTextChild(scNode, RuntimeTagNames.COOKIE_COMMENT, cookieConfig.getComment());
                }
                appendTextChild(scNode, RuntimeTagNames.COOKIE_SECURE,
                        Boolean.toString(cookieConfig.isSecure()));
                appendTextChild(scNode, RuntimeTagNames.COOKIE_MAX_AGE_SECS,
                        Integer.toString(cookieConfig.getMaxAge()));
                appendTextChild(scNode, RuntimeTagNames.COOKIE_HTTP_ONLY,
                        Boolean.toString(cookieConfig.isHttpOnly()));
            }

            if (trackingModes.contains(SessionTrackingMode.URL)) {
                appendTextChild(scNode, RuntimeTagNames.URL_REWRITING_ENABLED, "true");
            }
        }

        if (runtimeSessionConfig != null) {
            // persistent-store-dir
            SessionManager sessionManager = runtimeSessionConfig.getSessionManager();
            if (sessionManager != null) {
                StoreProperties storeProperties = sessionManager.getStoreProperties();
                if (storeProperties != null && storeProperties.sizeWebProperty() > 0) {
                    for (WebProperty prop : storeProperties.getWebProperty()) {
                        String name = prop.getAttributeValue(WebProperty.NAME);
                        String value = prop.getAttributeValue(WebProperty.VALUE);
                        if (name.equals(DIRECTORY)) {
                            appendTextChild(scNode,
                                    RuntimeTagNames.PERSISTENT_STORE_DIR, value);
                            break;
                        }
                    }
                }
            }
        }

        return scNode;
    }

    /**
     * Get and create a runtime SessionConfig if necessary.
     */
    private com.sun.enterprise.deployment.runtime.web.SessionConfig getRuntimeSessionConfig() {
        WebBundleDescriptor webBundleDescriptor = (WebBundleDescriptor)getParentNode().getDescriptor();
        SunWebApp sunDescriptor = webBundleDescriptor.getSunDescriptor();
        com.sun.enterprise.deployment.runtime.web.SessionConfig runtimeSessionConfig =
                sunDescriptor.getSessionConfig();
        if (runtimeSessionConfig == null) {
            runtimeSessionConfig = new com.sun.enterprise.deployment.runtime.web.SessionConfig();
            sunDescriptor.setSessionConfig(runtimeSessionConfig);
        }

        return runtimeSessionConfig;
    }


    /**
     * Add Manager property.
     */
    private void addManagerProperty(String name, String value) {
        SessionManager sessionManager = getSessionManager();
        ManagerProperties managerProperties = sessionManager.getManagerProperties();
        if (managerProperties == null) {
            managerProperties = new ManagerProperties();
            sessionManager.setManagerProperties(managerProperties);
        }

        addWebProperty(managerProperties, name, value);
    }

    /**
     * Add Store Property.
     */
    private void addStoreProperty(String name, String value) {
        SessionManager sessionManager = getSessionManager();
        StoreProperties storeProperties = sessionManager.getStoreProperties();
        if (storeProperties == null) {
            storeProperties = new StoreProperties();
            sessionManager.setStoreProperties(storeProperties);
        }

        addWebProperty(storeProperties, name, value);
    }

    private void addWebProperty(WebPropertyContainer webPropertyContainer,
            String name, String value) {
        WebProperty webProperty = new WebProperty();
        webProperty.setAttributeValue(WebProperty.NAME, name);
        webProperty.setAttributeValue(WebProperty.VALUE, value);
        webPropertyContainer.addWebProperty(webProperty);
    }

    /**
     * Get and create a SessionManager if necessary.
     */
    private SessionManager getSessionManager() {
        com.sun.enterprise.deployment.runtime.web.SessionConfig runtimeSessionConfig = 
                getRuntimeSessionConfig();
        SessionManager sessionManager = runtimeSessionConfig.getSessionManager();
        if (sessionManager == null) {
            sessionManager = new SessionManager();
            runtimeSessionConfig.setSessionManager(sessionManager);
        }

        return sessionManager;
    }

    /**
     * Get and create a SessionConfigDescriptor if necessary.
     */
    private SessionConfig getSessionConfig() {
        WebBundleDescriptor webBundleDescriptor = (WebBundleDescriptor)getParentNode().getDescriptor();
        SessionConfig sessionConfig = webBundleDescriptor.getSessionConfig();
        if (sessionConfig == null) {
            sessionConfig = new SessionConfigDescriptor();
            webBundleDescriptor.setSessionConfig(sessionConfig);
        }

        return sessionConfig;
    }

    /**
     * Get and create a CookieConfigDescriptor if necessary.
     */
    private CookieConfig getCookieConfig() {
        SessionConfig sessionConfig = getSessionConfig();
        CookieConfig cookieConfig = sessionConfig.getCookieConfig();
        if (cookieConfig == null) {
            cookieConfig = new CookieConfigDescriptor();
            sessionConfig.setCookieConfig(cookieConfig);
        }
        return cookieConfig;
    }
}
