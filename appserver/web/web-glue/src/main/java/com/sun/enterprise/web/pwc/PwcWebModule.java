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

package com.sun.enterprise.web.pwc;

import com.sun.enterprise.web.session.SessionCookieConfig;
import com.sun.enterprise.web.session.WebSessionCookieConfig;
import org.apache.catalina.Globals;
import org.apache.catalina.Wrapper;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.core.StandardWrapper;

import java.util.Enumeration;

/**
 * Class representing a web module (servlet context).
 */
public abstract class PwcWebModule extends StandardContext {

    // ----------------------------------------------------- Class Variables


    // ----------------------------------------------------- Instance Variables

    // The id of this web module as specified in the configuration.
    protected String _id = null;

    // The session cookie configuration for this web module.
    private SessionCookieConfig _cookieConfig = null;

    private boolean _useResponseCTForHeaders = false;

   /**
    * Determines whether or not we should encode the cookies.
    * By default cookies are not  URL encoded.
    */
    private boolean _encodeCookies = false;

    // START OF IASRI 4731830
    /**
     * Maximum number of SingleThreadModel instances for each wrapper
     * in this context. 
     */
    private int stmPoolSize = 5;
    // END OF IASRI 4731830

    // START S1AS8PE 4920021
    /**
     * Rave required attribute
     * True if the web-module implements web services endpoints.
     */
    private boolean hasWebServices = false;

    /**
     * Rave required attribute
     * An array of URL addresses defined in this web-module to invoke 
     * web services endpoints implementations.
     */
    private String[] endpointAddresses = null;
    // END S1AS8PE 4920021

    // The web module context root
    private String contextRoot;

    // Indicates whether this WebModule has a web.xml deployment descriptor
    private boolean hasWebXml;

    private String moduleName;

    private String[] cacheControls;

    protected String formHintField = null;

    protected String defaultCharset;

    protected WebSessionCookieConfig webSessionCookieConfig = null;


    /**
     * Gets this web module's identifier.
     *
     * @return Web module identifier
     */
    public String getID() {
        return _id;
    }


    /**
     * Sets this web module's identifier.
     *
     * @param id Web module identifier
     */
    public void setID(String id) {
        _id = id;
    }


    /**
     * Gets the session tracking cookie configuration of this
     * <tt>ServletContext</tt>.
     */
    @Override
    public synchronized javax.servlet.SessionCookieConfig getSessionCookieConfig() {
        if (webSessionCookieConfig == null) {
            webSessionCookieConfig = new WebSessionCookieConfig(this);
        }
        return webSessionCookieConfig;
    }


    /**
     * Return the session cookie configuration for this web module.
     */
    public SessionCookieConfig getSessionCookieConfigFromSunWebXml() {
        return _cookieConfig;
    }


    /**
     * Set the session cookie configuration for this web module.
     *
     * @param cookieConfig The new session cookie configuration
     */
    public void setSessionCookieConfigFromSunWebXml(SessionCookieConfig cookieConfig) {
        _cookieConfig = cookieConfig;
    }


    /**
     * return parameter-encoding form-hint-field attribute value
     */
    public String getFormHintField() {
        return formHintField;
    }


    /**
     * Gets the value of the default-charset attribute of the
     * parameter-encoding element
     *
     * @return Value of the default-charset attribute of the
     * parameter-encoding element, or null if not present
     */
    public String getDefaultCharset() {
        return defaultCharset;
    }


    /**
     * sets _useResponseCTForHeaders property value. When
     * _useResponseCTForHeaders is set to true, it means that 
     * we send the response header in the same encoding of the 
     * response charset instead of UTF-8, (see the method sendHeaders
     * in com.sun.enterprise.web.connector.nsapi.nsapiNSAPIResponse) 
     */
    public void setResponseCTForHeaders() {
        _useResponseCTForHeaders = true;
    }


    /**
     * Determines whether cookies should be encoded or not.
     * If the property encodeCookies is set to false in sun-web.xml,
     * cookies will not be URL encoded. The default behaviuor is that
     * we always encode the cookies unless the property encodeCookies
     * is set to false in sun-web.xml.
     */
    public void setEncodeCookies(boolean flag) {
        _encodeCookies = flag;
    }


    /**
     * return _useResponseCTForHeaders property value
     */
    public boolean getResponseCTForHeaders() {
        return _useResponseCTForHeaders;
    }


    /**
     * return _encodeCookies property value
     */
    public boolean getEncodeCookies() {
        return _encodeCookies;
    }


    /**
     * Return maximum number of instances that will be allocated when a single
     * thread model servlet is used in this web module.
     */
    public int getSTMPoolSize() {
        return (this.stmPoolSize);
    }

     
    /**
     * Set the maximum number of instances that will be allocated when a single
     * thread model servlet is used in this web module.
     *
     * @param newPoolSize New value of SingleThreadModel servlet pool size
     */
    public void setSTMPoolSize(int newPoolSize) {
     
        int oldPoolSize = this.stmPoolSize;
        this.stmPoolSize = newPoolSize;
        support.firePropertyChange("stmPoolSize", Integer.valueOf(oldPoolSize),
                                   Integer.valueOf(this.stmPoolSize));
    }

         
    /**
     * Factory method to create and return a new Wrapper instance, of
     * the Java implementation class appropriate for this Context
     * implementation.  The constructor of the instantiated Wrapper
     * will have been called, but no properties will have been set.
     */
    @Override
    public Wrapper createWrapper() {
        Wrapper wrapper = super.createWrapper();
        ((StandardWrapper) wrapper).setMaxInstances(stmPoolSize);
        return wrapper;
    }


    // START S1AS8PE 4920021
    /**
     * Return the hasWebServices flag for this web module.
     */
    public boolean getHasWebServices() {
        return hasWebServices;
    }


    /**
     * Set the hasWebServices boolean flag for this web module.
     *
     * @param hasWebServices boolean flag hasWebServices for this web module
     */
    public void setHasWebServices(boolean hasWebServices) {
        this.hasWebServices = hasWebServices;
    }


   /**
    * Gets the URL addresses corresponding to the web services endpoints of
    * this web module.
    *
    * @return Array of URL addresses corresponding to the web services
    * endpoints of this web module
    */
    public String[] getEndpointAddresses() {
        return endpointAddresses;
    }


    /**
     * Sets the URL addresses corresponding to the web services endpoints of
     * this web module.
     *
     * @param endpointAddresses Array of URL addresses corresponding to the
     * web services endpoints of this web module
     */
    public void setEndpointAddresses(String[] endpointAddresses) {
        this.endpointAddresses = (String[])endpointAddresses.clone();
    }
    // END S1AS8PE 4920021
    

    /**
     * Gets this web module's context root.
     *
     * @return Web module context root
     */
    public String getContextRoot(){
        return contextRoot;
    }


    /**
     * Sets this web module's context root.
     *
     * @param contextRoot Web module context root
     */
    public void setContextRoot(String contextRoot){
        this.contextRoot = contextRoot;
    }  

    
    /*
     * @param hasWebXml true if this WebModule has a web.xml deployment
     * descriptor, false otherwise
     */
    public void setHasWebXml(boolean hasWebXml) {
        this.hasWebXml = hasWebXml;
    }


    /*
     * @return true if this WebModule has a web.xml deployment descriptor,
     * false otherwise
     */
    public boolean hasWebXml() {
        return hasWebXml;
    }


    /**
     * Sets this web module's name.
     *
     * @param moduleName Web module name
     */ 
    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
    }


    /**
     * Gets this web module's name.
     *
     * @return Web module name
     */ 
    public String getModuleName() {
        return this.moduleName;
    }


    /**
     * Sets the Cache-Control configuration for this web module.
     *
     * @param cacheControls Cache-Control configuration settings for this
     * web module
     */
    public void setCacheControls(String[] cacheControls) {
        this.cacheControls = cacheControls;
    }


    /**
     * Gets the Cache-Control settings of this web module.
     *
     * @return Cache-Control settings of this web module, or null if
     * no such settings exist for this web module.
     */
    public String[] getCacheControls() {
        return cacheControls;
    }


    /**
     * Returns true if this web module specifies a locale-charset-map in its
     * sun-web.xml, false otherwise.
     *
     * @return true if this web module specifies a locale-charset-map in its
     * sun-web.xml, false otherwise
     */
    public abstract boolean hasLocaleToCharsetMapping();


    /**
     * Matches the given request locales against the charsets specified in
     * the locale-charset-map of this web module's sun-web.xml, and returns
     * the first matching charset.
     *
     * @param locales Request locales
     *
     * @return First matching charset, or null if this web module does not
     * specify any locale-charset-map in its sun-web.xml, or no match was
     * found
     */
    public abstract String mapLocalesToCharset(Enumeration locales);

}
