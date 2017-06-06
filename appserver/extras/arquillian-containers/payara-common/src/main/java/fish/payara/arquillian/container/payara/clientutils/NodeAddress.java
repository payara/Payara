/*
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
// Portions Copyright [2016-2017] [Payara Foundation]
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
     * @return none
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
     * @return none
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
     * @return none
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
     * @return none
     */
    public void setHttpsPort(int secure_port) {
        this.httpsPort = secure_port;
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
