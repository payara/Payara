/*
 * Copyright (c) 2017 Payara Foundation and/or its affiliates. All rights reserved.
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

import java.net.URI;

/**
 * @author Z.Paulovics
 */
public class NodeAddress {
    
    /**
     * HTTP protocol URI prefix
     */
    public static final String HTTP_PROTOCOL_PREFIX = "http://";

    /**
     * HTTPS protocol URI prefix
     */
    public static final String HTTPS_PROTOCOL_PREFIX = "https://";

    /**
     * name of the server instance
     */
    private String serverName = PayaraClient.ADMINSERVER;

    /**
     * IP or HOST name of the node
     */
    private String host = "localhost";

    /**
     * Port number for http:// calls
     */
    private int httpPort;

    /**
     * Port number for https:// calls
     */
    private int httpsPort;

    public NodeAddress() {
    }

    public NodeAddress(String host) {
        this.host = host;
    }

    public NodeAddress(String serverName, String host, int port, int secure_port) {
        this.serverName = serverName;
        this.host = host;
        this.httpPort = port;
        this.httpsPort = secure_port;
    }

    /**
     * @return the serverName
     */
    public String getServerName() {
        return serverName;
    }

    /**
     * @param serverName name of the server
     */
    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    /**
     * @return the ip
     */
    public String getHost() {
        return host;
    }

    /**
     * @param host the host
     */
    public void setHost(String host) {
        this.host = host;
    }

    /**
     * @return the port
     */
    public int getHttpPort() {
        return httpPort;
    }

    /**
     * @param httpPort the http port
     */
    public void setHttpPort(int httpPort) {
        this.httpPort = httpPort;
    }

    /**
     * @return the secure port
     */
    public int getHttpsPort() {
        return httpsPort;
    }

    /**
     * @param httpsPort the port used for https
     */
    public void setHttpsPort(int httpsPort) {
        this.httpsPort = httpsPort;
    }

    public URI getURI() {
        return getURI(false);
    }

    public URI getURI(boolean secure) {
        return URI.create(getHttpProtocolPrefix(secure) + host + ":" + ((!secure) ? this.httpPort : this.httpsPort));
    }

    public static String getHttpProtocolPrefix(boolean secure) {
        return secure ? HTTPS_PROTOCOL_PREFIX : HTTP_PROTOCOL_PREFIX;
    }
}
