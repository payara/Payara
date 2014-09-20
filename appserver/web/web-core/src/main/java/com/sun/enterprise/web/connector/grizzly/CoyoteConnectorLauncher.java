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
 *
 *
 * This file incorporates work covered by the following copyright and
 * permission notice:
 *
 * Copyright 2004 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sun.enterprise.web.connector.grizzly;

import java.util.HashMap;
import java.util.Map;
import javax.management.ObjectName;

import org.apache.catalina.connector.ProtocolHandler;
import org.glassfish.grizzly.config.ssl.ServerSocketFactory;
import org.glassfish.grizzly.http.server.HttpHandler;

/**
 * Abstract the protocol implementation, including threading, etc.
 * Processor is single threaded and specific to stream-based protocols,
 * will not fit Jk protocols like JNI.
 *
 * @author Remy Maucherat
 * @author Costin Manolache
 */
public class CoyoteConnectorLauncher implements ProtocolHandler
{
    // START SJSAS 6439313     
    protected boolean blocking = false;
    // END SJSAS 6439313     

    /**
     * The <code>SelectorThread</code> implementation class. Not used when 
     * Coyote is used.
     */
    protected String selectorThreadImpl = null; 
    
    
    public CoyoteConnectorLauncher() {
        // START SJSAS 6439313 
        this(false,false,null);
    }

    
    public CoyoteConnectorLauncher(boolean secure, boolean blocking, 
                          String selectorThreadImpl) {
        this.secure = secure;
        this.blocking = blocking; 
        this.selectorThreadImpl = selectorThreadImpl;
    }


    public int getMaxHttpHeaderSize() {
        return maxHttpHeaderSize;
    }
    

    public void setMaxHttpHeaderSize(int valueI) {
        maxHttpHeaderSize = valueI;
        setAttribute("maxHttpHeaderSize", "" + valueI);
    }
    

    /** Pass config info
     */
    @Override
    public void setAttribute( String name, Object value ) {

        attributes.put(name, value);
/*
        if ("maxKeepAliveRequests".equals(name)) {
            maxKeepAliveRequests = Integer.parseInt((String) value.toString());
        } else if ("port".equals(name)) {
            setPort(Integer.parseInt((String) value.toString()));
        }
*/
    }

    @Override
    public Object getAttribute( String key ) {
        return attributes.get(key);
    }

    /**
     * Set a property.
     */
    public void setProperty(String name, String value) {
        setAttribute(name, value);
    }

    /**
     * Get a property
     */
    public String getProperty(String name) {
        return (String)getAttribute(name);
    }

    /** The adapter, used to call the connector 
     */
    @Override
    public void setHandler(HttpHandler adapter) {
        this.adapter=adapter;
    }

    @Override
    public HttpHandler getHandler() {
        return adapter;
    }

    
    /** Start the protocol
     */
    @Override
    public void init() throws Exception {
    }
    
    @Override
    public void start() throws Exception {

    }

    @Override
    public void destroy() throws Exception {

    }
    
    // -------------------- Properties--------------------
    protected boolean secure;

    // socket factory attributes ( XXX replace with normal setters )
    protected Map<String, Object> attributes = new HashMap<String, Object>();
    protected String socketFactoryName=null;
    protected String sslImplementationName=null;

    private int maxKeepAliveRequests=100; // as in Apache HTTPD server
    protected int timeout = 300000;	// 5 minutes as in Apache HTTPD server
    protected int maxPostSize = 2 * 1024 * 1024;
    protected int maxHttpHeaderSize = 4 * 1024;
    private String reportedname;
    protected int socketCloseDelay=-1;
    protected boolean disableUploadTimeout = true;
    protected HttpHandler adapter;
    
    // START OF SJSAS PE 8.1 6172948
    /**
     * The input request buffer size.
     */
    protected int requestBufferSize = 4096;
    // END OF SJSAS PE 8.1 6172948
    
    /**
     * Compression value.
     */
    protected String compression = "off";

    // -------------------- Pool setup --------------------


    public String getSocketFactory() {
        return socketFactoryName;
    }

    public void setSocketFactory( String valueS ) {
        socketFactoryName = valueS;
        setAttribute("socketFactory", valueS);
    }

    public String getSSLImplementation() {
        return sslImplementationName;
    }

    public void setSSLImplementation( String valueS) {
        sslImplementationName=valueS;
        setAttribute("sslImplementation", valueS);
    }

    public boolean getDisableUploadTimeout() {
        return disableUploadTimeout;
    }

    public void setDisableUploadTimeout(boolean isDisabled) {
        disableUploadTimeout = isDisabled;
    }

    public String getCompression() {
        return compression;
    }

    public void setCompression(String valueS) {
        compression = valueS;
        setAttribute("compression", valueS);
    }

    public int getMaxPostSize() {
        return maxPostSize;
    }

    public void setMaxPostSize(int valueI) {
        maxPostSize = valueI;
        setAttribute("maxPostSize", "" + valueI);
    }
    
    public String getKeystore() {
        return getProperty("keystore");
    }

    public void setKeystore( String k ) {
        setAttribute("keystore", k);
    }

    public String getKeypass() {
        return getProperty("keypass");
    }

    public void setKeypass( String k ) {
        attributes.put("keypass", k);
        //setAttribute("keypass", k);
    }

    public String getKeytype() {
        return getProperty("keystoreType");
    } 

    public void setKeytype( String k ) {
        setAttribute("keystoreType", k);
    }

    // START GlassFish Issue 657
    public void setTruststore(String truststore) {
        setAttribute("truststore", truststore);
    }

    public void setTruststoreType(String truststoreType) {
        setAttribute("truststoreType", truststoreType);
    }    
    // END GlassFish Issue 657

    public String getClientauth() {
        return getProperty("clientauth");
    }

    public void setClientauth( String k ) {
        setAttribute("clientauth", k);
    }
    
    public String getProtocol() {
        return getProperty("protocol");
    }

    public void setProtocol( String k ) {
        setAttribute("protocol", k);
    }

    public String getProtocols() {
        return getProperty("protocols");
    }

    public void setProtocols(String k) {
        setAttribute("protocols", k);
    }

    public String getAlgorithm() {
        return getProperty("algorithm");
    }

    public void setAlgorithm( String k ) {
        setAttribute("algorithm", k);
    }

    public boolean getSecure() {
        return secure;
    }

    public void setSecure( boolean b ) {
    	secure=b;
        setAttribute("secure", "" + b);
    }
    
    // START SJSAS 6439313     
    public boolean getBlocking() {
        return blocking;
    }

    public void setBlocking( boolean b ) {
    	blocking=b;
        setAttribute("blocking", "" + b);
    }
    // END SJSAS 6439313     
    
    public String getCiphers() {
        return getProperty("ciphers");
    }

    public void setCiphers(String ciphers) {
        setAttribute("ciphers", ciphers);
    }

    public String getKeyAlias() {
        return getProperty("keyAlias");
    }

    public void setKeyAlias(String keyAlias) {
        setAttribute("keyAlias", keyAlias);
    }

    public int getMaxKeepAliveRequests() {
        return maxKeepAliveRequests;
    }

    /** Set the maximum number of Keep-Alive requests that we will honor.
     */
    public void setMaxKeepAliveRequests(int mkar) {
	maxKeepAliveRequests = mkar;
        setAttribute("maxKeepAliveRequests", "" + mkar);
    }

    public int getSocketCloseDelay() {
        return socketCloseDelay;
    }

    public void setSocketCloseDelay( int d ) {
        socketCloseDelay=d;
        setAttribute("socketCloseDelay", "" + d);
    }

    protected static ServerSocketFactory string2SocketFactory( String val)
	throws ClassNotFoundException, IllegalAccessException,
	InstantiationException
    {
	Class chC=Class.forName( val );
	return (ServerSocketFactory)chC.newInstance();
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout( int timeouts ) {
	timeout = timeouts * 1000;
        setAttribute("timeout", "" + timeouts);
    }
 
    public String getReportedname() {
        return reportedname;
    }

    public void setReportedname( String reportedName) {
	reportedname = reportedName;
    }
    
    // START OF SJSAS PE 8.1 6172948
    /**
     * Set the request input buffer size
     */
    public void setBufferSize(int requestBufferSize){
        this.requestBufferSize = requestBufferSize;
    }
    

    /**
     * Return the request input buffer size
     */
    public int getBufferSize(){
        return requestBufferSize;
    }    
    // END OF SJSAS PE 8.1 6172948 
}
