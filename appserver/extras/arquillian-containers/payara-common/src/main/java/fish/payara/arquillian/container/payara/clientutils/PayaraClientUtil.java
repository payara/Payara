/*
 * Copyright (c) 2017-2018 Payara Foundation and/or its affiliates. All rights reserved.
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
 * file and include the License file at glassfish/legal/LICENSE.txt.
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
 *
 *
 * This file incorporates work covered by the following copyright and
 * permission notice:
 * 
 * JBoss, Home of Professional Open Source
 * Copyright 2011, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package fish.payara.arquillian.container.payara.clientutils;

import static java.lang.Double.parseDouble;
import static java.lang.Long.parseLong;
import static java.util.logging.Level.SEVERE;
import static javax.ws.rs.client.Entity.entity;
import static javax.ws.rs.core.MediaType.APPLICATION_XML_TYPE;
import static javax.ws.rs.core.Response.Status.Family.SUCCESSFUL;
import static javax.xml.stream.XMLInputFactory.IS_VALIDATING;
import static javax.xml.stream.XMLStreamConstants.END_ELEMENT;
import static javax.xml.stream.XMLStreamConstants.START_ELEMENT;
import static org.glassfish.jersey.client.authentication.HttpAuthenticationFeature.basic;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.StatusType;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.glassfish.jersey.client.filter.CsrfProtectionFilter;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ContainerException;

import fish.payara.arquillian.container.payara.CommonPayaraConfiguration;

/**
 * @author Z.Paulovics
 */
public class PayaraClientUtil {
    
    private static final Logger log = Logger.getLogger(PayaraClientUtil.class.getName());

    /**
     * Status for a successful Payara exit code deployment.
     */
    public static final String SUCCESS = "SUCCESS";

    /**
     * Status for a Payara exit code deployment which ended in warning.
     */
    public static final String WARNING = "WARNING";

    private final CommonPayaraConfiguration configuration;
    private final String adminBaseUrl;

    public PayaraClientUtil(CommonPayaraConfiguration configuration, String adminBaseUrl) {
        this.configuration = configuration;
        this.adminBaseUrl = adminBaseUrl;
    }

    public CommonPayaraConfiguration getConfiguration() {
        return configuration;
    }

    @SuppressWarnings("unchecked")
    public Map<String, String> getAttributes(String additionalResourceUrl) {

        Map<String, Object> extraProperties = getExtraProperties(GETRequest(additionalResourceUrl));
        if (extraProperties != null) {
            return (Map<String, String>) extraProperties.get("entity");
        }

        return new HashMap<>();
    }

    @SuppressWarnings("unchecked")
    public Map<String, String> getChildResources(String additionalResourceUrl) {

        Map<String, Object> extraProperties = getExtraProperties(GETRequest(additionalResourceUrl));
        if (extraProperties != null) {
            return (Map<String, String>) extraProperties.get("childResources");
        }

        return new HashMap<>();
    }

    public Map<String, Object> GETRequest(String additionalResourceUrl) {
        return getResponseMap(
                prepareClient(additionalResourceUrl).get());
    }

    public List<Map<String, Object>> getInstancesList(String additionalResourceUrl) throws ContainerException {

        Map<String, Object> extraProperties = getExtraProperties(GETRequest(additionalResourceUrl));
        
        if (extraProperties != null) {
            return getInstanceList(extraProperties);
        }

        return new ArrayList<>();
    }
    
    public Map<String, String> getServerSystemProperties(String additionalResourceUrl) {
        
        Map<String, String> systemProperties = new HashMap<>();
        
        String message = getMessage(GETRequest(additionalResourceUrl));
        int systemPropertiesHeader = message.indexOf("List of System Properties for the Java Virtual Machine:");
        if (systemPropertiesHeader != -1) {
            
            String systemMessage = message.substring(systemPropertiesHeader + "List of System Properties for the Java Virtual Machine:".length());
            
            for (String line : systemMessage.split("(\\r\\n|\\r|\\n)")) {
                if (line.contains("=")) {
                    String[] keyValue = line.split("=");
                    systemProperties.put(keyValue[0].trim(), keyValue[1].trim());
                    
                }
                
            }
            
        }
        
        return systemProperties;
        
    }
    
    @SuppressWarnings("unchecked")
    public Map<String, Object> getExtraProperties(Map<String, Object> responseMap) {
        return (Map<String, Object>) responseMap.get("extraProperties");
    }
    
    @SuppressWarnings("unchecked")
    public String getMessage(Map<String, Object> responseMap) {
        return (String) responseMap.get("message");
    }
    
    
    
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getInstanceList(Map<String, Object> resultExtraProperties) {
        return (List<Map<String, Object>>) resultExtraProperties.get("instanceList");
    }

    public Map<String, Object> POSTMultiPartRequest(String additionalResourceUrl, FormDataMultiPart form) {
        return getResponseMap(
                prepareClient(additionalResourceUrl, MultiPartFeature.class)
                    .post(entity(form, form.getMediaType())));
    }

    /**
     * Basic REST call preparation, with the additional resource url appended
     *
     * @param additionalResourceUrl
     *     url portion past the base to use
     *
     * @return the resource builder to execute
     */
    @SafeVarargs
    private final Builder prepareClient(final String additionalResourceUrl, final Class<? extends Feature>... features) {
        
        Client client = ClientBuilder.newClient();
        
        if (configuration.isAuthorisation()) {
            client.register(basic(configuration.getAdminUser(), configuration.getAdminPassword())) ;
        }
        
        client.register(new CsrfProtectionFilter());
        
        for (Class<? extends Feature> feature : features) {
            client.register(feature);
        }
        
        return client.target(adminBaseUrl + additionalResourceUrl)
                     .request(APPLICATION_XML_TYPE)
                     .header("X-GlassFish-3", "ignore");
    }

    private Map<String, Object> getResponseMap(Response response) {
        
        Map<String, Object> responseMap = new HashMap<>();
        String message = "";
        final String xmlDoc = response.readEntity(String.class);

        // Marshalling the XML format response to a java Map
        if (xmlDoc != null && !xmlDoc.isEmpty()) {
            responseMap = xmlToMap(xmlDoc);

            message = 
                "exit_code: " + responseMap.get("exit_code") + 
                ", message: " + responseMap.get("message");
        }

        StatusType status = response.getStatusInfo();
        if (status.getFamily() == SUCCESSFUL) {

            // O.K. the jersey call was successful, what about the Payara server response?
            if (responseMap.get("exit_code") == null) {
                throw new PayaraClientException(message);
            } else if (WARNING.equals(responseMap.get("exit_code"))) {
                // Warning is not a failure - some warnings in Payara are inevitable (i.e. persistence-related: ARQ-606)
                log.warning("Deployment resulted in a warning: " + message);
            } else if (!SUCCESS.equals(responseMap.get("exit_code"))) {
                // Response is not a warning nor success - it's surely a failure.
                throw new PayaraClientException(message);
            }
        } else if (status.getReasonPhrase() == "Not Found") {
            // the REST resource can not be found (for optional resources it can be O.K.)
            message += " [status: " + status.getFamily() + " reason: " + status.getReasonPhrase() + "]";
            log.warning(message);
        } else {
            message += " [status: " + status.getFamily() + " reason: " + status.getReasonPhrase() + "]";
            log.severe(message);
            throw new ContainerException(message);
        }

        return responseMap;
    }

    /**
     * Marshalling a Payara Mng API response XML document to a java Map object
     *
     * @param document the XMl document to be converted
     *     
     *
     * @return map containing the XML doc representation in java map format
     */
    public Map<String, Object> xmlToMap(String document) {

        if (document == null) {
            return new HashMap<>();
        }

        Map<String, Object> map = null;
        
        XMLInputFactory factory = XMLInputFactory.newInstance();
        factory.setProperty(IS_VALIDATING, false);
        
        try (InputStream input = new ByteArrayInputStream(document.trim().getBytes("UTF-8"))) {
            
            XMLStreamReader stream = factory.createXMLStreamReader(input);
            while (stream.hasNext()) {
                if (stream.next() == START_ELEMENT && "map".equals(stream.getLocalName())) {
                    map = resolveXmlMap(stream);
                }
            }
        } catch (Exception ex) {
            log.log(SEVERE, null, ex);
            throw new RuntimeException(ex);
        }

        return map;
    }

    private Map<String, Object> resolveXmlMap(XMLStreamReader stream) throws XMLStreamException {

        boolean endMapFlag = false;
        Map<String, Object> entry = new HashMap<String, Object>();
        String key = null;
        String elementName = null;

        while (!endMapFlag) {

            int currentEvent = stream.next();
            if (currentEvent == START_ELEMENT) {

                if ("entry".equals(stream.getLocalName())) {
                    key = stream.getAttributeValue(null, "key");
                    String value = stream.getAttributeValue(null, "value");
                    if (value != null) {
                        entry.put(key, value);
                        key = null;
                    }
                } else if ("map".equals(stream.getLocalName())) {
                    entry.put(key, resolveXmlMap(stream));
                } else if ("list".equals(stream.getLocalName())) {
                    entry.put(key, resolveXmlList(stream));
                } else {
                    elementName = stream.getLocalName();
                }
            } else if (currentEvent == END_ELEMENT) {

                if ("map".equals(stream.getLocalName())) {
                    endMapFlag = true;
                }
                elementName = null;
            } else {

                String document = stream.getText();
                if (elementName != null) {
                    if ("number".equals(elementName)) {
                        if (document.contains(".")) {
                            entry.put(key, parseDouble(document));
                        } else {
                            entry.put(key, parseLong(document));
                        }
                    } else if ("string".equals(elementName)) {
                        entry.put(key, document);
                    }
                    elementName = null;
                }
            } // end if
        } // end while
        
        return entry;
    }

    private List<Object> resolveXmlList(XMLStreamReader stream) throws XMLStreamException {

        boolean endListFlag = false;
        List<Object> list = new ArrayList<>();
        String elementName = null;

        while (!endListFlag) {

            int currentEvent = stream.next();
            if (currentEvent == START_ELEMENT) {
                if ("map".equals(stream.getLocalName())) {
                    list.add(resolveXmlMap(stream));
                } else {
                    elementName = stream.getLocalName();
                }
            } else if (currentEvent == END_ELEMENT) {

                if ("list".equals(stream.getLocalName())) {
                    endListFlag = true;
                }
                elementName = null;
            } else {

                String document = stream.getText();
                if (elementName != null) {
                    if ("number".equals(elementName)) {
                        if (document.contains(".")) {
                            list.add(parseDouble(document));
                        } else {
                            list.add(parseLong(document));
                        }
                    } else if ("string".equals(elementName)) {
                        list.add(document);
                    }
                    elementName = null;
                }
            } // end if
        } // end while
        
        return list;
    }
}
