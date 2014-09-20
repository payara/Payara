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

package org.glassfish.embeddable.web.config;

/**
 * Class used for configuring VirtualServer instances.
 *
 * @see org.glassfish.embeddable.web.VirtualServer
 */
public class VirtualServerConfig {

    private boolean ssoEnabled;
    private boolean accessLoggingEnabled;
    private String defaultWebXml;
    private String contextXmlDefault;
    private boolean allowLinking;
    private String allowRemoteAddress;
    private String denyRemoteAddress;
    private String allowRemoteHost;
    private String denyRemoteHost;
    private String hostNames = "${com.sun.aas.hostName}";    

    /**
     * Enables or disables Single-Sign-On.
     *
     * @param ssoEnabled true if Single-Sign-On is to be enabled, false
     * otherwise
     */
    public void setSsoEnabled(boolean ssoEnabled) {
        this.ssoEnabled = ssoEnabled;
    }

    /**
     * Checks if Single-Sign-On is enabled or disabled.
     *
     * @return true if Single-Sign-On is enabled, false otherwise
     */
    public boolean isSsoEnabled() {
        return ssoEnabled;
    }

    /**
     * Enables or disables access logging.
     *
     * @param accessLoggingEnabled true if access logging is to be enabled,
     * false otherwise
     */
    public void setAccessLoggingEnabled(boolean accessLoggingEnabled) {
        this.accessLoggingEnabled = accessLoggingEnabled;
    }

    /**
     * Checks if access logging is enabled or disabled.
     *
     * @return true if access logging is enabled, false otherwise
     */
    public boolean isAccessLoggingEnabled() {
        return accessLoggingEnabled;
    }

    /**
     * Sets the location of the default web.xml configuration file.
     *
     * @param defaultWebXml the location of the default web.xml configuration
     * file
     */
    public void setDefaultWebXml(String defaultWebXml) {
        this.defaultWebXml = defaultWebXml;
    }

    /**
     * Gets the location of the default web.xml configuration file.
     *
     * @return the location of the default web.xml configuration file, or
     * <tt>null</tt> if <tt>setDefaultWebXml</tt> was never called on this 
     * <tt>VirtualServerConfig</tt>
     */
    public String getDefaultWebXml() {
        return defaultWebXml;
    }

    /**
     * Sets the location of the default context.xml configuration file.
     *
     * @param contextXmlDefault the location of the default context.xml
     * configuration file.
     */
    public void setContextXmlDefault(String contextXmlDefault) {
        this.contextXmlDefault = contextXmlDefault;
    }

    /**
     * Gets the location of the default context.xml configuration file.
     *
     * @return the location of the default context.xml configuration file,
     * or <tt>null</tt> if <tt>setContextXmlDefault</tt> was never called
     * on this <tt>VirtualServerConfig</tt>
     */
    public String getContextXmlDefault() {
        return contextXmlDefault;
    }

    /**
     * Enables or disables the serving of resources that are symbolic links.
     *
     * @param allowLinking true if resources that are symbolic links are
     * to be served, false otherwise
     */
    public void setAllowLinking(boolean allowLinking) {
        this.allowLinking = allowLinking;
    }

    /**
     * Checks if resources that are symbolic links will be served.
     *
     * @return true if resources that are symbolic links will be served,
     * false otherwise
     */
    public boolean isAllowLinking() {
        return allowLinking;
    }

    /**
     * Sets the comma-separated list of regular expression patterns that
     * the remote client's IP address is compared to.
     *
     * <p>If this property is specified, the remote address must match for
     * this request to be accepted. If this property is not specified,
     * all requests are accepted unless the remote address matches a
     * <tt>denyRemoteAddress</tt> pattern.
     *
     * @param allowRemoteAddress the comma-separated list of regular
     * expression patterns that the remote client's IP address is compared
     * to
     */
    public void setAllowRemoteAddress(String allowRemoteAddress) {
        this.allowRemoteAddress = allowRemoteAddress;
    }

    /**
     * Gets the comma-separated list of regular expression patterns that
     * the remote client's IP address is compared to.
     *
     * @return the comma-separated list of regular expression patterns that
     * the remote client's IP address is compared to, or <tt>null</tt>
     * if <tt>setAllowRemoteAddress</tt> was never called on this 
     * <tt>VirtualServerConfig</tt>
     */
    public String getAllowRemoteAddress() {
        return allowRemoteAddress;
    }

    /**
     * Sets the comma-separated list of regular expression patterns that
     * the remote client's IP address is compared to.
     *
     * <p>If this property is specified, the remote address must not match
     * for this request to be accepted. If this property is not specified,
     * request acceptance is governed solely by the allowRemoteAddress
     * property.
     *
     * @param denyRemoteAddress the comma-separated list of regular
     * expression patterns that the remote client's IP address is
     * compared to
     */
    public void setDenyRemoteAddress(String denyRemoteAddress) {
        this.denyRemoteAddress = denyRemoteAddress;
    }

    /**
     * Gets the comma-separated list of regular expression patterns that
     * the remote client's IP address is compared to.
     *
     * @return the comma-separated list of regular expression patterns that
     * the remote client's IP address is compared to, or <tt>null</tt>
     * if <tt>setDenyRemoteAddress</tt> was never called on this 
     * <tt>VirtualServerConfig</tt>
     */
    public String getDenyRemoteAddress() {
        return denyRemoteAddress;
    }

    /**
     * Sets the comma-separated list of regular expression patterns
     * that the remote client's hostname (as returned by
     * java.net.Socket.getInetAddress().getHostName()) is compared to.
     *
     * <p>If this property is specified, the remote hostname must match
     * for this request to be accepted. If this property is not specified,
     * all requests are accepted unless the remote hostname matches a
     * <tt>denyRemoteHost</tt> pattern.
     *
     * @param allowRemoteHost the comma-separated list of regular
     * expression patterns that the remote client's hostname (as returned
     * by java.net.Socket.getInetAddress().getHostName()) is compared to
     */
    public void setAllowRemoteHost(String allowRemoteHost) {
        this.allowRemoteHost = allowRemoteHost;
    }

    /**
     * Gets the comma-separated list of regular expression patterns
     * that the remote client's hostname (as returned by
     * java.net.Socket.getInetAddress().getHostName()) is compared to.
     *
     * @return the comma-separated list of regular expression patterns
     * that the remote client's hostname (as returned by
     * java.net.Socket.getInetAddress().getHostName()) is compared to,
     * or <tt>null</tt> if <tt>setAllowRemoteHost</tt> was never called
     * on this <tt>VirtualServerConfig</tt>
     */
    public String getAllowRemoteHost() {
        return allowRemoteHost;
    }

    /**
     * Sets the comma-separated list of regular expression patterns that
     * the remote client's hostname (as returned by
     * java.net.Socket.getInetAddress().getHostName()) is compared to.
     *
     * <p>If this property is specified, the remote hostname must not
     * match for this request to be accepted. If this property is not
     * specified, request acceptance is governed solely by the
     * <tt>allowRemoteHost</tt> property.
     *
     * @param denyRemoteHost the comma-separated list of regular
     * expression patterns that the remote client's hostname
     * (as returned by java.net.Socket.getInetAddress().getHostName())
     * is compared to
     */
    public void setDenyRemoteHost(String denyRemoteHost) {
        this.denyRemoteHost = denyRemoteHost;
    }

    /**
     * Gets the comma-separated list of regular expression patterns that
     * the remote client's hostname (as returned by
     * java.net.Socket.getInetAddress().getHostName()) is compared to.
     *
     * @return the comma-separated list of regular expression patterns that
     * the remote client's hostname (as returned by
     * java.net.Socket.getInetAddress().getHostName()) is compared to,
     * or <tt>null</tt> if <tt>setDenyRemoteHost</tt> was never called
     * on this <tt>VirtualServerConfig</tt>
     */
    public String getDenyRemoteHost() {
        return denyRemoteHost;
    }

    /**
     * Sets the host names that will be assigned to any
     * <tt>VirtualServer</tt> configured via this
     * <tt>VirtualServerConfig</tt> separated by commas.
     *
     * @param hostNames the host names
     */ 
    public void setHostNames(String hostNames) {
        this.hostNames = hostNames;
    }

    /**
     * Gets the host names assigned to any <tt>VirtualServer</tt> configured
     * via this <tt>VirtualServerConfig</tt>.
     *
     * @return the host names
     */
    public String getHostNames() {
        return hostNames;
    }
}
