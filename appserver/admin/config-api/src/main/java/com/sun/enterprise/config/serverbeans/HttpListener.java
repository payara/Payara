/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2011 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.config.serverbeans;

import java.beans.PropertyVetoException;
import java.util.List;

import org.jvnet.hk2.config.types.Property;
import org.jvnet.hk2.config.types.PropertyBag;
import org.glassfish.grizzly.config.dom.Ssl;
import org.glassfish.api.admin.config.PropertiesDesc;
import org.glassfish.api.admin.config.PropertyDesc;
import org.glassfish.api.admin.RestRedirects;
import org.glassfish.api.admin.RestRedirect;
import org.glassfish.config.support.datatypes.NonNegativeInteger;
import static org.glassfish.config.support.Constants.NAME_REGEX;
import org.jvnet.hk2.component.Injectable;
import org.jvnet.hk2.config.Attribute;
import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.config.Configured;
import org.jvnet.hk2.config.Element;

import javax.validation.constraints.Pattern;
import javax.validation.constraints.Min;
import javax.validation.constraints.Max;
import javax.validation.constraints.NotNull;



@Configured
@Deprecated
@RestRedirects({
 @RestRedirect(opType = RestRedirect.OpType.POST, commandName = "create-http-listener"),
 @RestRedirect(opType = RestRedirect.OpType.DELETE, commandName = "delete-http-listener")
})
public interface HttpListener extends ConfigBeanProxy, Injectable, PropertyBag {

    /**
     * Gets the value of the id property.
     *
     * Unique identifier for http listener.
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute(key=true)
    @NotNull
    @Pattern(regexp=NAME_REGEX)
    String getId();

    /**
     * Sets the value of the id property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setId(String value) throws PropertyVetoException;

    /**
     * Gets the value of the address property.
     *
     * IP address of the listen socket. Can be in dotted-pair or IPv6 notation.
     * Can also be any for INADDR-ANY. Configuring a  listen socket to listen on
     * any is required if more than one http-listener is configured to it.
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute
    @NotNull
    String getAddress();

    /**
     * Sets the value of the address property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setAddress(String value) throws PropertyVetoException;

    /**
     * Gets the value of the port property.
     *
     * Port number to create the listen socket on. Legal values are 1 - 65535.
     * On Unix, creating sockets that listen on ports 1 - 1024 requires
     * superuser privileges. Configuring an SSL listen socket to listen on port
     * 443 is recommended.
     * 
     * @return possible object is
     *         {@link String }
     */
    @Attribute
    @NotNull
    @Max(value=65535)
    @Min(value=1)    
    String getPort();

    /**
     * Sets the value of the port property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setPort(String value) throws PropertyVetoException;

    /**
     * Gets the value of the externalPort property.
     *
     * The port at which the user makes a request, typically a proxy server port
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute
    @Max(value=65535)
    @Min(value=1)    
    String getExternalPort();

    /**
     * Sets the value of the externalPort property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setExternalPort(String value) throws PropertyVetoException;

    /**
     * Gets the value of the family property.
     *
     * Specified the family of addresses either inet or ncsa
     * 
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="inet")
    String getFamily();

    /**
     * Sets the value of the family property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setFamily(String value) throws PropertyVetoException;

    /**
     * Gets the value of the blockingEnabled property.
     *
     * Enables blocking for the listen and external ports.
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="false", dataType=Boolean.class)
    String getBlockingEnabled();

    /**
     * Sets the value of the blockingEnabled property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setBlockingEnabled(String value) throws PropertyVetoException;

    /**
     * Gets the value of the acceptorThreads property.
     *
     * Number of acceptor threads for the listen socket. The recommended value
     * is the number of processors in the machine.
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="1")
    @Min(value=1)
    @Max(value=Integer.MAX_VALUE)
    String getAcceptorThreads();

    /**
     * Sets the value of the acceptorThreads property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setAcceptorThreads(String value) throws PropertyVetoException;

    /**
     * Gets the value of the securityEnabled property.
     *
     * Determines whether the http listener runs SSL. You can turn SSL2 or SSL3
     * on or off and set ciphers using an ssl element. The enable-ssl in the
     * protocol element should be set to true for this setting to work.
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="false", dataType=Boolean.class)
    String getSecurityEnabled();

    /**
     * Sets the value of the securityEnabled property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setSecurityEnabled(String value) throws PropertyVetoException;

    /**
     * Gets the value of the defaultVirtualServer property.
     *
     * The id attribute of the default virtual server for this particular
     * connection group.
     * 
     * @return possible object is
     *         {@link String }
     */
    @Attribute
    @NotNull
    String getDefaultVirtualServer();

    /**
     * Sets the value of the defaultVirtualServer property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setDefaultVirtualServer(String value) throws PropertyVetoException;

    /**
     * Gets the value of the serverName property.
     * 
     * Tells the server what to put in the host name section of any URLs it
     * sends to client. This affects URLs the server automatically generates;
     * it doesnt affect the URLs for directories and files stored in the server.
     * This name should be the alias name if your server uses an alias.
     * If you append a colon and port number, that port will be used in URLs the
     * server sends to the client.
     * 
     * @return possible object is
     *         {@link String }
     */
    @Attribute
    @NotNull
    String getServerName();

    /**
     * Sets the value of the serverName property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setServerName(String value) throws PropertyVetoException;

    /**
     * Gets the value of the redirectPort property.
     *
     * If the connector is supporting non-SSL requests and a request is received
     * for which a matching security-constraint requires SSL transport catalina
     * will automatically redirect the request to the port number specified here
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute
    @Max(value=65535)
    @Min(value=1)    
    String getRedirectPort();

    /**
     * Sets the value of the redirectPort property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setRedirectPort(String value) throws PropertyVetoException;

    /**
     * Gets the value of the xpoweredBy property.
     *
     * The Servlet 2.4 spec defines a special X-Powered-By:
     *  Servlet/2.4 header, which containers may add to
     *  servlet-generated responses. This is complemented by the JSP 2.0 spec,
     *  which defines a X-Powered-By: JSP/2.0 header to be added
     *  (on an optional basis) to responses utilizing JSP technology. The goal
     *  of these headers is to aid in gathering statistical data about the use
     *  of Servlet and JSP technology. If true, these headers will be added.
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="true", dataType=Boolean.class)
    String getXpoweredBy();

    /**
     * Sets the value of the xpoweredBy property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setXpoweredBy(String value) throws PropertyVetoException;

    /**
     * Gets the value of the enabled property.
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="true", dataType=Boolean.class)
    String getEnabled();

    /**
     * Sets the value of the enabled property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setEnabled(String value) throws PropertyVetoException;

    /**
     * Gets the value of the ssl property.
     *
     * @return possible object is
     *         {@link Ssl }
     */
    @Element
    Ssl getSsl();

    /**
     * Sets the value of the ssl property.
     *
     * @param value allowed object is
     *              {@link Ssl }
     */
    void setSsl(Ssl value) throws PropertyVetoException;
    
    
@PropertiesDesc(systemProperties=false,
    props={
    @PropertyDesc(name="recycle-objects", defaultValue="true", dataType=Boolean.class,
        description="Recycles internal objects instead of using the VM garbage collector"),
        
    @PropertyDesc(name="reader-threads", defaultValue="0", dataType=NonNegativeInteger.class,
        description="Number of reader threads, which read bytes from the non-blocking socket"),
        
    @PropertyDesc(name="acceptor-queue-length", defaultValue="4096", dataType=NonNegativeInteger.class,
        description="Length of the acceptor thread queue. Once full, connections are rejected"),
        
    @PropertyDesc(name="reader-queue-length", defaultValue="4096", dataType=NonNegativeInteger.class,
        description="Length of the reader thread queue. Once full, connections are rejected"),
        
    @PropertyDesc(name="use-nio-direct-bytebuffer", defaultValue="true", dataType=Boolean.class,
        description="Specifies that the NIO direct is used. In a limited resource environment, " +
                    "it might be faster to use non-direct Java's ByteBuffer by setting a value of false"),
        
    @PropertyDesc(name="authPassthroughEnabled", defaultValue="false", dataType=Boolean.class,
        description="Indicates that this http-listener element receives traffic from an  SSL-terminating proxy server. " +
                    "Overrides the authPassthroughEnabled  property of the parent http-service"),
        
    @PropertyDesc(name="proxyHandler", defaultValue="com.sun.enterprise.web.ProxyHandlerImpl",
        description="Specifies the fully qualified class name of a custom implementation of com.sun.appserv.ProxyHandler." +
                    "Used if the authPassthroughEnabled property of this http-listener and the parent http-service are both true. " +
                    "Overrides any value in the parent http-service element"),
        
    @PropertyDesc(name="proxiedProtocol", values={"ws/tcp", "http", "https", "tls"},
        description="Comma-separated list of protocols that can use the same port. " + 
        "For example, if you set this property to http,https and set the port to 4567, " +
        "you can access the port with either http://host:4567/ or https://host:4567/. " +
        " Specifying this property at the “http-service�? on page 42 level overrides settings at the http-listener level. " +
        "If this property is not set at either level, this feature is disabled"),
        
    @PropertyDesc(name="bufferSize", defaultValue="4096", dataType=NonNegativeInteger.class,
        description="Size in bytes of the buffer to be provided for input streams created by HTTP listeners"),
        
    @PropertyDesc(name="connectionTimeout", defaultValue="30", dataType=NonNegativeInteger.class,
        description="Number of seconds HTTP listeners wait after accepting a connection for the request URI line to be presented"),
        
    @PropertyDesc(name="maxKeepAliveRequests", defaultValue="250", dataType=NonNegativeInteger.class,
        description="Maximum number of HTTP requests that can be pipelined until the connection is closed by the server. " +
            "Set this property to 1 to disable HTTP/1.0  keep-alive, as well as HTTP/1.1 keep-alive and pipelining"),
        
    @PropertyDesc(name="traceEnabled", defaultValue="true", dataType=Boolean.class,
        description="Enables the TRACE operation. Set this property to false to make the server less susceptible to cross-site scripting attacks"),
        
    @PropertyDesc(name="cometSupport", defaultValue="false", dataType=Boolean.class,
        description="Enables Comet support for this listener.  If your servlet/JSP page uses Comet technology, " +
            "make sure it is initialized by adding the load-on-startup element to web.xml"),
        
    @PropertyDesc(name="jkEnabled", defaultValue="false", dataType=Boolean.class,
        description="Enables/disables mod_jk support."),
        
    @PropertyDesc(name="compression", defaultValue="off", values={"off","on","force"},
        description="Specifies use of HTTP/1.1 GZIP compression to save server bandwidth. " +
            "A positive integer specifies the minimum amount of data required before the output is compressed. " +
            "If the content-length is not known, the output is compressed only if compression is set to 'on' or 'force'" ),
        
    @PropertyDesc(name="compressableMimeType", defaultValue="text/html, text/xml, text/plain",
        description="Comma-separated list of MIME types for which HTTP compression is used"),
        
    @PropertyDesc(name="noCompressionUserAgents", defaultValue="",
        description="Comma-separated list of regular expressions matching user-agents of HTTP clients for which compression should not be used"),
        
    @PropertyDesc(name="compressionMinSize", dataType=NonNegativeInteger.class,
        description="Minimum size of a file when compression is applied"),
        
    @PropertyDesc(name="minCompressionSize", dataType=NonNegativeInteger.class,
        description="Minimum size of a file when compression is applied"),
        
    @PropertyDesc(name="crlFile",
        description="Location of the Certificate Revocation List (CRL) file to consult during SSL client authentication. " +
            "Can be an absolute or relative file path. If relative, it is resolved against domain-dir. If unspecified, CRL checking is disabled"),
        
    @PropertyDesc(name="trustAlgorithm", values="PKIX",
        description="Name of the trust management algorithm (for example, PKIX) to use for certification path validation"),
        
    @PropertyDesc(name="trustMaxCertLength", defaultValue="5", dataType=Integer.class,
        description="Maximum number of non-self-issued intermediate certificates that can exist in a certification path. " +
            "Considered only if trustAlgorithm is set to PKIX. A value of zero implies that the path can only contain a single certificate. " +
            "A value of -1 implies that the path length is unconstrained (no maximum)"),
        
    @PropertyDesc(name="disableUploadTimeout", defaultValue="true", dataType=Boolean.class,
        description="When false, the connection for a servlet that reads bytes slowly is closed after the 'connectionUploadTimeout' is reached"),
        
    @PropertyDesc(name="connectionUploadTimeout", defaultValue="5", dataType=NonNegativeInteger.class,
        description="Specifies the timeout for uploads. Applicable only if 'disableUploadTimeout' is false"),

    /** uriEncoding UTF-8 Specifies the character set used to decode the request URIs received on this 
    HTTP listener. Must be a valid IANA character set naname. */
    @PropertyDesc(name="uriEncoding", defaultValue="UTF-8", values={"UTF-8"},
        description="Character set used to decode the request URIs received on this HTTP listener. " +
            "Must be a valid IANA character set name. Overrides the property of the parent http-service")
})
	@Element("property")
    List<Property> getProperty();
}
