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

package org.glassfish.embeddable.web;

import java.util.*;
import javax.servlet.*;
import org.glassfish.embeddable.web.config.SecurityConfig;

/**
 * Representation of a web application.
 *
 * <p/>See {@link WebContainer} for usage example.
 *
 * @author Rajiv Mordani
 * @author Jan Luehe
 */
// TODO: Add support for configuring environment entries
public interface Context extends ServletContext {
    
    /**
     * Adds the given <tt>Valve</tt> to this <tt>Context</tt>.
     *
     * @param valve the <tt>Valve</tt> to be added
     */
    //public void addValve(Valve valve);

    /**
     * Registers the given listener with this <tt>Context</tt>.
     * 
     * <p>The given listener must be an instance of one or more of the
     * following interfaces:
     * <ul>
     * <li><tt>javax.servlet.ServletContextAttributeListener</tt>
     * <li><tt>javax.servlet.ServletRequestAttributeListener</tt>
     * <li><tt>javax.servlet.ServletRequestListener</tt>
     * <li><tt>javax.servlet.ServletContextListener</tt>
     * <li><tt>javax.servlet.http.HttpSessionAttributeListener</tt>
     * <li><tt>javax.servlet.http.HttpSessionIdListener</tt>
     * <li><tt>javax.servlet.http.HttpSessionListener</tt>
     * </ul>
     *
     * @param t the listener to be registered with this <tt>Context</tt>
     *
     * @throws IllegalArgumentException if the given listener is not
     * an instance of any of the above interfaces
     * @throws IllegalStateException if this context has already been
     * initialized and started
     */
    public <T extends EventListener> void addListener(T t);

    /**
     * Instantiates a listener from the given class and registers it with
     * this <tt>Context</tt>.
     * 
     * <p>The given listener must be an instance of one or more of the
     * following interfaces:
     * <ul>
     * <li><tt>javax.servlet.ServletContextAttributeListener</tt>
     * <li><tt>javax.servlet.ServletRequestAttributeListener</tt>
     * <li><tt>javax.servlet.ServletRequestListener</tt>
     * <li><tt>javax.servlet.ServletContextListener</tt>
     * <li><tt>javax.servlet.http.HttpSessionAttributeListener</tt>
     * <li><tt>javax.servlet.http.HttpSessionListener</tt>
     * </ul>
     *
     * @param c the class from which to instantiate of the listener
     *
     * @throws IllegalArgumentException if the given class does not
     * implement any of the above interfaces
     * @throws IllegalStateException if this context has already been
     * initialized and started
     */
    public void addListener(Class <? extends EventListener> c);

    /**
     * Enables or disables directory listings on this <tt>Context</tt>.
     *
     * @param directoryListing true if directory listings are to be
     * enabled on this <tt>Context</tt>, false otherwise
     */
    public void setDirectoryListing(boolean directoryListing);

    /**
     * Checks whether directory listings are enabled or disabled on this
     * <tt>Context</tt>.
     *
     * @return true if directory listings are enabled on this 
     * <tt>Context</tt>, false otherwise
     */
    public boolean isDirectoryListing();

    /**
     * Set the security related configuration for this context
     *
     * @see org.glassfish.embeddable.web.config.SecurityConfig
     *
     * @param config the security configuration for this context
     */
    public void setSecurityConfig(SecurityConfig config);

    /**
     * Gets the security related configuration for this context
     *
     * @see org.glassfish.embeddable.web.config.SecurityConfig
     *
     * @return the security configuration for this context
     */
    public SecurityConfig getSecurityConfig();
    
    /**
     * Set the location of the default web xml that will be used.
     *
     * @param defaultWebXml the defaultWebXml path to be used
     */
    public void setDefaultWebXml(String defaultWebXml);
    
    /**
     * Return the context path for this Context.
     */
    public String getPath();
        
    /**
     * Set the context path for this Context.
     */
    public void setPath(String path);

}
