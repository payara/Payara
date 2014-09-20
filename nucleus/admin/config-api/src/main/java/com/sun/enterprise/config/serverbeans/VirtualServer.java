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

package com.sun.enterprise.config.serverbeans;

import java.beans.PropertyVetoException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import org.glassfish.grizzly.config.dom.NetworkConfig;
import org.glassfish.grizzly.config.dom.NetworkListener;
import org.glassfish.api.admin.RestRedirect;
import org.glassfish.api.admin.RestRedirects;
import org.glassfish.api.admin.config.PropertiesDesc;
import org.glassfish.api.admin.config.PropertyDesc;
import org.glassfish.config.support.datatypes.PositiveInteger;
import org.jvnet.hk2.config.Attribute;
import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.config.Configured;
import org.jvnet.hk2.config.DuckTyped;
import org.jvnet.hk2.config.Element;
import org.jvnet.hk2.config.types.Property;
import org.jvnet.hk2.config.types.PropertyBag;

/**
 * Configuration of Virtual Server
 *
 * Virtualization in Application Server allows multiple URL domains to be served by the same HTTP server process, which
 * is listening on multiple host addresses If an application is available at two virtual servers, they still share same
 * physical resource pools, such as JDBC connection pools. Sun ONE Application Server allows a list of virtual servers,
 * to be specified along with web-module and j2ee-application elements. This establishes an association between URL
 * domains, represented by the virtual server and the web modules (standalone web modules or web modules inside the ear
 * file)
 */
@Configured
@RestRedirects({
    @RestRedirect(opType = RestRedirect.OpType.POST, commandName = "create-virtual-server"),
    @RestRedirect(opType = RestRedirect.OpType.DELETE, commandName = "delete-virtual-server")
})
public interface VirtualServer extends ConfigBeanProxy, PropertyBag {
    /**
     * Gets the value of the id property.
     *
     * Virtual server ID. This is a unique ID that allows lookup of a specific virtual server. A virtual server ID
     * cannot begin with a number.
     *
     * @return possible object is {@link String }
     */
    @Attribute(key = true)
    @NotNull
    String getId();

    /**
     * Sets the value of the id property.
     *
     * @param value allowed object is {@link String }
     */
    void setId(String value) throws PropertyVetoException;

    /**
     * Gets the value of the httpListeners property.
     *
     * Comma-separated list of http-listener id(s), Required only for a Virtual Server that is not the default virtual
     * server.
     *
     * @return possible object is {@link String }
     */
    @Attribute
    @Deprecated
    String getHttpListeners();

    /**
     * Sets the value of the httpListeners property.
     *
     * @param value allowed object is {@link String }
     */
    @Deprecated
    void setHttpListeners(String value) throws PropertyVetoException;

    /**
     * Gets the value of the httpListeners property.
     *
     * @return possible object is {@link String }
     */
    @Attribute
    String getNetworkListeners();

    /**
     * Sets the value of the httpListeners property.
     *
     * @param value allowed object is {@link String }
     */
    void setNetworkListeners(String value) throws PropertyVetoException;

    /**
     * Gets the value of the defaultWebModule property.
     *
     * Stand alone web module associated with this virtual server by default
     *
     * @return possible object is {@link String }
     */
    @Attribute
    String getDefaultWebModule();

    /**
     * Sets the value of the defaultWebModule property.
     *
     * @param value allowed object is {@link String }
     */
    void setDefaultWebModule(String value) throws PropertyVetoException;

    /**
     * Gets the value of the hosts property.
     *
     * A comma-separated list of values allowed in the Host request header to select current virtual server. Each
     * Virtual Server that is configured to the same Connection Group must have a unique hosts value for that group.
     *
     * @return possible object is {@link String }
     */
    @Attribute(defaultValue = "${com.sun.aas.hostName}")
    @NotNull
    String getHosts();

    /**
     * Sets the value of the hosts property.
     *
     * @param value allowed object is {@link String }
     */
    void setHosts(String value) throws PropertyVetoException;

    /**
     * Gets the value of the state property.
     *
     * Determines whether Virtual Server is active(on) or inactive(off, disable) The default is on (active). When
     * inactive, a Virtual Server does not service requests. off returns a 404: Status code (404) indicating that the
     * requested resource is not available disabled returns a 403: Status code (403) indicating the server understood
     * the request but refused to fulfill it.
     *
     * @return possible object is {@link String }
     */
    @Attribute(defaultValue = "on")
    @Pattern(regexp = "(on|off|disabled)")
    String getState();

    /**
     * Sets the value of the state property.
     *
     * @param value allowed object is {@link String }
     */
    void setState(String value) throws PropertyVetoException;

    /**
     * Gets the value of the docroot property.
     *
     * The location on the filesystem where the files related to the content to be served by this virtual server is
     * stored.
     *
     * @return possible object is {@link String }
     */
    @Attribute(defaultValue = "${com.sun.aas.instanceRoot}/docroot")
    String getDocroot();

    /**
     * Sets the value of the docroot property.
     *
     * @param value allowed object is {@link String }
     */
    void setDocroot(String value) throws PropertyVetoException;

    /**
     * Gets the value of the accesslog property.
     *
     * @return possible object is {@link String }
     */
    @Attribute(defaultValue = "${com.sun.aas.instanceRoot}/logs/access")
    String getAccessLog();

    /**
     * Sets the value of the accesslog property.
     *
     * @param value allowed object is {@link String }
     */
    void setAccessLog(String value) throws PropertyVetoException;

    /**
     * Gets the value of the sso-enabled property.  Possible values:  true/false/inherit
     *
     * @return possible object is {@link String }
     */
    @Attribute(defaultValue = "inherit")
    @Pattern(regexp = "(true|on|false|off|inherit)")
    String getSsoEnabled();

    /**
     * Sets the value of the sso-enabled property.
     *
     * @param value allowed object is {@link String }
     */
    void setSsoEnabled(String value) throws PropertyVetoException;

    /**
     * Gets the value of the enabled property.  Possible values:  true/false/inherit
     *
     * @return possible object is {@link String }
     */
    @Attribute(defaultValue = "inherit")
    @Pattern(regexp = "(true|on|false|off|inherit)")
    String getAccessLoggingEnabled();

    /**
     * Sets the value of the access logging enabled property.
     *
     * @param value allowed object is {@link String }
     */
    void setAccessLoggingEnabled(String value) throws PropertyVetoException;

    /**
     * Gets the value of the logFile property.
     *
     * Specifies a log file for virtual-server-specific log messages. Default value is
     * ${com.sun.aas.instanceRoot}/logs/server.log
     *
     * @return possible object is {@link String }
     */
    @Attribute(defaultValue = "${com.sun.aas.instanceRoot}/logs/server.log")
    String getLogFile();

    /**
     * Sets the value of the logFile property.
     *
     * @param value allowed object is {@link String }
     */
    void setLogFile(String value) throws PropertyVetoException;

    /**
     * Gets the value of the httpAccessLog property.
     *
     * @return possible object is {@link HttpAccessLog }
     */
    @Element
    HttpAccessLog getHttpAccessLog();

    /**
     * Sets the value of the httpAccessLog property.
     *
     * @param value allowed object is {@link HttpAccessLog }
     */
    void setHttpAccessLog(HttpAccessLog value) throws PropertyVetoException;

    /**
     * Gets the Secure attribute of any JSESSIONIDSSO cookies associated with the web applications deployed to this
     * virtual server. Applicable only if the sso-enabled property is set to true. To set the Secure attribute of a
     * JSESSIONID cookie, use the cookieSecure cookie-properties property in the sun-web.xml file. Valid values: "true",
     * "false", "dynamic"
     */
    @Attribute(defaultValue = "dynamic")
    @Pattern(regexp = "(true|false|dynamic)")
    String getSsoCookieSecure();

    void setSsoCookieSecure(String value);

    @Attribute(defaultValue="true", dataType=Boolean.class)
    String getSsoCookieHttpOnly();

    void setSsoCookieHttpOnly(String value);

    @DuckTyped
    void addNetworkListener(String name) throws PropertyVetoException;

    @DuckTyped
    void removeNetworkListener(String name) throws PropertyVetoException;

    @DuckTyped
    NetworkListener findNetworkListener(String name);

    @DuckTyped
    List<NetworkListener> findNetworkListeners();

    class Duck {
        public static void addNetworkListener(VirtualServer server, String name) throws PropertyVetoException {
            final String listeners = server.getNetworkListeners();
            final String[] strings = listeners == null ? new String[0] : listeners.split(",");
            final Set<String> set = new TreeSet<String>();
            for (String string : strings) {
                set.add(string.trim());
            }
            set.add(name);
            server.setNetworkListeners(ConfigBeansUtilities.join(set, ","));
        }

        public static void removeNetworkListener(VirtualServer server, String name) throws PropertyVetoException {
            final String listeners = server.getNetworkListeners();
            final String[] strings = listeners == null ? new String[0] : listeners.split(",");
            final Set<String> set = new TreeSet<String>();
            for (String string : strings) {
                set.add(string.trim());
            }
            set.remove(name);
            server.setNetworkListeners(ConfigBeansUtilities.join(set, ","));
        }

        public static NetworkListener findNetworkListener(VirtualServer server, String name) {
            final String listeners = server.getNetworkListeners();
            if (listeners != null && listeners.contains(name)) {
                final NetworkConfig config = server.getParent().getParent(Config.class).getNetworkConfig();
                return config.getNetworkListener(name);
            } else {
                return null;
            }
        }

        public static List<NetworkListener> findNetworkListeners(VirtualServer server) {
            final String listeners = server.getNetworkListeners();
            final String[] strings = listeners == null ? new String[0] : listeners.split(",");
            final NetworkConfig config = server.getParent().getParent(Config.class).getNetworkConfig();
            List<NetworkListener> list = new ArrayList<NetworkListener>();
            for (String s : strings) {
                final String name = s.trim();
                final NetworkListener networkListener = config.getNetworkListener(name);
                if (networkListener != null) {
                    list.add(networkListener);
                }
            }
            return list;
        }
    }

    /**
     * Properties.
     */
    @PropertiesDesc(
        props = {
            @PropertyDesc(name = "sso-max-inactive-seconds", defaultValue = "300", dataType = PositiveInteger.class,
                description = "The time after which a user's single sign-on record becomes eligible for purging if "
                    + "no client activity is received. Since single sign-on applies across several applications on the same virtual server, "
                    + "access to any of the applications keeps the single sign-on record active. Higher values provide longer "
                    + "single sign-on persistence for the users at the expense of more memory use on the server"),
            @PropertyDesc(name = "sso-reap-interval-seconds", defaultValue = "60", dataType = PositiveInteger.class,
                description = "Interval between purges of expired single sign-on records"),
            @PropertyDesc(name = "setCacheControl",
                description =
                    "Comma-separated list of Cache-Control response directives. For a list of valid directives, "
                        + "see section 14.9 of the document at http://www.ietf.org/rfc/rfc2616.txt"),
            @PropertyDesc(name = "accessLoggingEnabled", defaultValue = "false", dataType = Boolean.class,
                description = "Enables access logging for this virtual server only"),
            @PropertyDesc(name = "accessLogBufferSize", defaultValue = "32768", dataType = PositiveInteger.class,
                description = "Size in bytes of the buffer where access log calls are stored. If the value is "
                    + "less than 5120, a warning message is issued, and the value is set to 5120. To set this "
                    + "property for all virtual servers, set it as a property of the parent http-service"),
            @PropertyDesc(name = "accessLogWriteInterval", defaultValue = "300", dataType = PositiveInteger.class,
                description = "Number of seconds before the log is written to the disk. The access log is written when "
                    + "the buffer is full or when the interval expires. If the value is 0, the buffer is always written even if "
                    + "it is not full. This means that each time the server is accessed, the log message is stored directly to the file. "
                    + "To set this property for all virtual servers, set it as a property of the parent http-service"),
            @PropertyDesc(name = "allowRemoteAddress",
                description =
                    "Comma-separated list of regular expression patterns that the remote client's IP address is "
                        + "compared to. If this property is specified, the remote address must match for this request to be accepted. "
                        + "If this property is not specified, all requests are accepted unless the remote address matches a 'denyRemoteAddress' pattern"),
            @PropertyDesc(name = "denyRemoteAddress",
                description = "Comma-separated list of regular expression patterns that the remote client's "
                    + "IP address is compared to. If this property is specified, the remote address must not "
                    + "match for this request to be accepted. If this property is not specified, request "
                    + "acceptance is governed solely by the 'allowRemoteAddress' property"),
            @PropertyDesc(name = "allowRemoteHost",
                description = "Comma-separated list of regular expression patterns that the remote client's "
                    + "hostname (as returned by java.net.Socket.getInetAddress().getHostName()) is compared to. "
                    + "If this property is specified, the remote hostname must match for the request to be accepted. "
                    + "If this property is not specified, all requests are accepted unless the remote hostname matches a 'denyRemoteHost' pattern"),
            @PropertyDesc(name = "denyRemoteHost",
                description =
                    "Specifies a comma-separated list of regular expression patterns that the remote client's "
                        + "hostname (as returned by java.net.Socket.getInetAddress().getHostName()) is compared to. "
                        + "If this property is specified, the remote hostname must not match for this request to be accepted. "
                        + "If this property is not specified, request acceptance is governed solely by the 'allowRemoteHost' property"),
            @PropertyDesc(name = "authRealm",
                description = "Specifies the name attribute of an “auth-realm�? on page 23 element, which overrides "
                    + "the server instance's default realm for stand-alone web applications deployed to this virtual server. "
                    + "A realm defined in a stand-alone web application's web.xml file overrides the virtual server's realm"),
            @PropertyDesc(name = "securePagesWithPragma", defaultValue = "true", dataType = Boolean.class,
                description =
                    "Set this property to false to ensure that for all web applications on this virtual server "
                        + "file downloads using SSL work properly in Internet Explorer. You can set this property for a specific web application."),
            @PropertyDesc(name = "contextXmlDefault",
                description = "The location, relative to domain-dir, of the context.xml file for this virtual server, if one is used"),
            @PropertyDesc(name = "allowLinking", defaultValue = "false", dataType = Boolean.class,
                description =
                    "If true, resources that are symbolic links in web applications on this virtual server are served. "
                        + "The value of this property in the sun-web.xml file takes precedence if defined. "
                        + "Caution: setting this property to true on Windows systems exposes JSP source code."),
            /**
             * Specifies an alternate document root (docroot), where n is a positive integer that
             * allows specification of more than one. Alternate docroots allow web applications to
             * serve requests for certain resources from outside their own docroot, based on whether
             * those requests match one (or more) of the URI patterns of the web application's
             * alternate docroots.
             * <p>
             * If a request matches an alternate docroot's URI pattern, it is mapped to the alternate
             * docroot by appending the request URI (minus the web application's context root) to
             * the alternate docroot's physical location (directory). If a request matches multiple URI
             * patterns, the alternate docroot is determined according to the following precedence order:
             * <p>
             * <ul>
             * <li>Exact match</li>
             * <li>Longest path match</li>
             * <li>Extension match</li>
             * </ul>
             * For example, the following properties specify three alternate docroots. The URI
             * pattern of the first alternate docroot uses an exact match, whereas the URI patterns of
             * the second and third alternate docroots use extension and longest path prefix matches,
             * respectively.
             * <p>
             * &lt;property name="alternatedocroot_1" value="from=/my.jpg dir=/srv/images/jpg"/><br>
             * &lt;property name="alternatedocroot_2" value="from=*.jpg dir=/srv/images/jpg"/> <br>
             * &lt;property name="alternatedocroot_3" value="from=/jpg/* dir=/src/images"/> <br>
             * <p>
             * The value of each alternate docroot has two components: The first component, from,
             * specifies the alternate docroot's URI pattern, and the second component, dir, specifies
             * the alternate docroot's physical location (directory). Spaces are allowed in the dir
             * component.
             * <p>
             * You can set this property for a specific web application. For details, see “sun-web-app�?
             * in Sun GlassFish Enterprise Server v3 Prelude Application Deployment Guide.
             */
            @PropertyDesc(name = "alternatedocroot_*", description = "The '*' denotes a positive integer. Example: "
                + "<property property name=\"alternatedocroot_1\" value=\"from=/my.jpg dir=/srv/images/jpg\" />"),
            /**
             * Specifies custom error page mappings for the virtual server, which are inherited by all
             * web applications deployed on the virtual server. A web application can override these
             * custom error page mappings in its web.xml deployment descriptor. The value of each
             * send-error_n property has three components, which may be specified in any order:
             * The first component, code, specifies the three-digit HTTP response status code for
             * which the custom error page should be returned in the response.
             * <p>
             * The second component, path, specifies the absolute or relative file system path of the
             * custom error page. A relative file system path is interpreted as relative to the
             * domain-dir/config directory.
             * <p>
             * The third component, reason, is optional and specifies the text of the reason string
             * (such as Unauthorized or Forbidden) to be returned.
             * <p>
             * For example:<br>
             * &lt;property name="send-error_1" value="code=401 path=/myhost/401.html reason=MY-401-REASON" />
             * <p>
             * This example property definition causes the contents of /myhost/401.html to be
             * returned with 401 responses, along with this response line:
             * <br>HTTP/1.1 401 MY-401-REASON
             */
            @PropertyDesc(name = "send-error_*", description = "The '*' denotes a positive integer. Example: "
                + "<property name=\"send-error_1\" value=\"code=401 path=/myhost/401.html reason=MY-401-REASON\" />"),
            /**
             * Specifies that a request for an old URL is treated as a request for a new URL. These
             * properties are inherited by all web applications deployed on the virtual server. The
             * value of each redirect_n property has two components, which may be specified in any order--
             * <p>
             * The first component, from, specifies the prefix of the requested URI to match.
             * <p>
             * The second component, url-prefix, specifies the new URL prefix to return to the
             * client. The from prefix is simply replaced by this URL prefix.
             * For example:
             * <br> &lt;property name="redirect_1" value="from=/dummy url-prefix=http://etude"/>
             */
            @PropertyDesc(name = "redirect_*", description = "The '*' denotes a positive integer. Example: "
                + "<property name=\"redirect_1\" value=\"from=/dummy url-prefix=http://etude\" />")
        }
    )
    @Element
    List<Property> getProperty();

}
