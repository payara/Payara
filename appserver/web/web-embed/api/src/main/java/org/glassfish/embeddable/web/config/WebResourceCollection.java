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

import java.util.Set;
import org.glassfish.embeddable.GlassFishException;

/**
 * This class represents a list of URL patterns and HTTP
 * methods that describe a set of Web resources to be protected.
 *
 * <p/> Usage example:
 *
 * <pre>
 *      WebResourceCollection webResource = new WebResourceCollection();
 *      webResource.setName("protected");
 *      webResource.setUrlPatterns("/*");
 *      Set<String> httpMethods = new HashSet<String>();
 *      httpMethods.add("GET");
 *      httpMethods.add("POST");
 *      webResource.setHttpMethods(httpMethods);
 * </pre>
 *
 * @see SecurityConfig
 *
 * @author Rajiv Mordani
 * @author Amy Roh
 */
public class WebResourceCollection {

    private String name;
    private Set<String> urlPatterns;
    private Set<String> httpMethods;
    private Set<String> httpMethodOmissions;

    /**
     * Sets the name of this collection
     *
     * @param name the name of this collection
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the name of this collection
     *
     * @return the name of this collection
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the url patterns that correspond to this
     * web resource
     * 
     * @param urlPatterns the url patterns 
     */
    public void setUrlPatterns(Set<String> urlPatterns) {
        this.urlPatterns = urlPatterns;          
    }

    /**
     * Gets the url patterns that correspond to this
     * web resource
     *
     * @return the url patterns
     */
    public Set<String> getUrlPatterns() {
        return urlPatterns;
    }

    /**
     * Sets the HTTP methods that should be protected
     *
     * @param httpMethods the HTTP methods
     *
     * @throws GlassFishException if HttpMethodOmissions is already defined
     */
    public void setHttpMethods(Set<String> httpMethods)
            throws GlassFishException {
        if (httpMethodOmissions != null) {
            throw new GlassFishException(
                    "Invalid content was found starting with element 'http-method'. " +
                            "One of 'http-method' or 'http-method-omission' is expected.");
        }
        this.httpMethods = httpMethods;
    }

    /**
     * Gets the HTTP methods that should be protected
     *
     * @return the HTTP methods
     */
    public Set<String> getHttpMethods() {
        return httpMethods;
    }

    /**
     * Sets the HTTP methods to be omitted from protection
     *
     * @param httpMethodOmissions the HTTP methods to be 
     * omitted from protection
     *
     * @throws GlassFishException if HttpMethods is already defined
     */
    public void setHttpMethodOmissions(Set<String> httpMethodOmissions)
            throws GlassFishException {
        if (httpMethods != null) {
            throw new GlassFishException(
                    "Invalid content was found starting with element 'http-method-omission'. " +
                            "One of 'http-method' or 'http-method-omission' is expected.");
        }
        this.httpMethodOmissions = httpMethodOmissions;
    }

    /**
     * Gets the HTTP methods to be omitted from protection
     *
     * @return the HTTP methods to be omitted from protection
     */
    public Set<String> getHttpMethodOmissions() {
        return httpMethodOmissions;
    }

    /**
     * Returns a formatted string of the state.
     */
    public String toString() {
        StringBuffer toStringBuffer = new StringBuffer();
        toStringBuffer.append("WebResourceCollection: ");
        toStringBuffer.append(" name: ").append(name);
        toStringBuffer.append(" urlPatterns: ").append(urlPatterns);
        toStringBuffer.append(" httpMethods ").append(httpMethods);
        toStringBuffer.append(" httpMethodOmissions ").append(httpMethodOmissions);
        return toStringBuffer.toString();
    }
}
