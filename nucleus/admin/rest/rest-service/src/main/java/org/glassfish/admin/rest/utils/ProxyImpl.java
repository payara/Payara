/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2013 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.admin.rest.utils;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Map;
import java.util.Properties;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;

import org.glassfish.hk2.api.ServiceLocator;

import org.glassfish.admin.rest.client.utils.MarshallingUtils;

import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.SecureAdmin;
import com.sun.enterprise.config.serverbeans.Server;
import com.sun.enterprise.security.ssl.SSLUtils;

/**
 * @author Mitesh Meswani
 */
public abstract class ProxyImpl implements Proxy {

    @Override
    public Properties proxyRequest(UriInfo sourceUriInfo, Client client, ServiceLocator habitat) {
        Properties proxiedResponse = new Properties();
        try {
            Domain domain = habitat.getService(Domain.class);
            String forwardInstanceName = extractTargetInstanceName(sourceUriInfo);
            Server forwardInstance = domain.getServerNamed(forwardInstanceName);
            if (forwardInstance != null) {
                UriBuilder forwardUriBuilder = constructForwardURLPath(sourceUriInfo);
                URI forwardURI = forwardUriBuilder.scheme("https").host(forwardInstance.getAdminHost()).port(forwardInstance.getAdminPort()).build(); //Host and Port are replaced to that of forwardInstanceName
                client = addAuthenticationInfo(client, forwardInstance, habitat);
                WebTarget resourceBuilder = client.target(forwardURI);
                Response response = resourceBuilder.request(MediaType.APPLICATION_JSON).get(Response.class); //TODO if the target server is down, we get ClientResponseException. Need to handle it
                Response.Status status = Response.Status.fromStatusCode(response.getStatus());
                if (status.getFamily() == javax.ws.rs.core.Response.Status.Family.SUCCESSFUL) {
                    String jsonDoc = response.readEntity(String.class);
                    Map responseMap = MarshallingUtils.buildMapFromDocument(jsonDoc);
                    Map resultExtraProperties = (Map) responseMap.get("extraProperties");
                    if (resultExtraProperties != null) {
                        Object entity = resultExtraProperties.get("entity");
                        if(entity != null) {
                            proxiedResponse.put("entity", entity);
                        }

                        @SuppressWarnings({"unchecked"}) Map<String, String> childResources = (Map<String, String>) resultExtraProperties.get("childResources");
                        for (Map.Entry<String, String> entry : childResources.entrySet()) {
                            String targetURL = null;
                            try {
                                URL originalURL = new URL(entry.getValue());
                                //Construct targetURL which has host+port of DAS and path from originalURL
                                targetURL = constructTargetURLPath(sourceUriInfo, originalURL).build().toASCIIString();
                            } catch (MalformedURLException e) {
                                //TODO There was an exception while parsing URL. Need to decide what to do. For now ignore the child entry
                            }
                            entry.setValue(targetURL);
                        }
                        proxiedResponse.put("childResources", childResources);
                    }
                    Object message = responseMap.get("message");
                    if(message != null) {
                        proxiedResponse.put("message", message);
                    }
                    Object properties = responseMap.get("properties");
                    if(properties != null) {
                        proxiedResponse.put("properties", properties);
                    }
                } else {
                    throw new WebApplicationException(response.readEntity(String.class), status);
                }
            } else { // server == null
                // TODO error to user. Can not locate server for whom data is being looked for

            }
        } catch (Exception ex) {
            throw new WebApplicationException(ex, Response.Status.INTERNAL_SERVER_ERROR);
        }
        return proxiedResponse;

    }

    /**
     * Use SSL to authenticate
     */
    private Client addAuthenticationInfo(Client client, Server server, ServiceLocator habitat) {
        SecureAdmin secureAdmin = habitat.getService(SecureAdmin.class);

        // TODO need to get hardcoded "TLS" from corresponding ServerRemoteAdminCommand constant);
        final SSLContext sslContext = habitat
                .<SSLUtils>getService(SSLUtils.class)
                .getAdminSSLContext(SecureAdmin.Util.DASAlias(secureAdmin), "TLS");

        // Instruct Jersey to use HostNameVerifier and SSLContext provided by us.
        final ClientBuilder clientBuilder = ClientBuilder.newBuilder()
                .withConfig(client.getConfiguration())
                .hostnameVerifier(new BasicHostnameVerifier(server.getAdminHost()))
                .sslContext(sslContext);

        return clientBuilder.build();
    }

    /**
     * TODO copied from HttpConnectorAddress. Need to refactor code there to reuse
     */
    private static class BasicHostnameVerifier implements HostnameVerifier {
        private final String host;
        public BasicHostnameVerifier(String host) {
            if (host == null)
                throw new IllegalArgumentException("null host");
            this.host = host;
        }

        public boolean verify(String s, SSLSession sslSession) {
            return host.equals(s);
        }
    }

}
