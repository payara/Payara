/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2016 Oracle and/or its affiliates. All rights reserved.
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

package org.apache.catalina.core;


import org.apache.catalina.ContainerEvent;
import org.apache.catalina.LogFacade;
import org.apache.catalina.deploy.FilterDef;
import org.apache.catalina.security.SecurityUtil;

import javax.servlet.Filter;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import java.io.Serializable;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.ResourceBundle;



/**
 * Implementation of a <code>javax.servlet.FilterConfig</code> useful in
 * managing the filter instances instantiated when a web application
 * is first started.
 *
 * @author Craig R. McClanahan
 * @version $Revision: 1.6 $ $Date: 2007/03/22 18:04:04 $
 */

final class ApplicationFilterConfig implements FilterConfig, Serializable {

    private static final Logger log = LogFacade.getLogger();
    private static final ResourceBundle rb = log.getResourceBundle();

    // ----------------------------------------------------------- Constructors


    /**
     * Construct a new ApplicationFilterConfig for the specified filter
     * definition.
     *
     * @param context The context with which we are associated
     * @param filterDef Filter definition for which a FilterConfig is to be
     *  constructed
     *
     * @exception ClassCastException if the specified class does not implement
     *  the <code>javax.servlet.Filter</code> interface
     * @exception ClassNotFoundException if the filter class cannot be found
     * @exception IllegalAccessException if the filter class cannot be
     *  publicly instantiated
     * @exception InstantiationException if an exception occurs while
     *  instantiating the filter object
     * @exception ServletException if thrown by the filter's init() method
     */
    public ApplicationFilterConfig(StandardContext context,
                                   FilterDef filterDef)
        throws ClassCastException, ClassNotFoundException,
               IllegalAccessException, InstantiationException,
               ServletException {
        super();
        this.context = context;
        setFilterDef(filterDef);
        // init the filter
        try {
            getFilter();
        } catch(InstantiationException iex) {
            throw iex;
        } catch(Exception ex) {
            InstantiationException iex = new InstantiationException();
            iex.initCause(ex);
            throw iex;
        }
    }


    // ----------------------------------------------------- Instance Variables


    /**
     * The Context with which we are associated.
     */
    private transient StandardContext context = null;


    /**
     * The application Filter we are configured for.
     */
    private transient Filter filter = null;


    /**
     * The <code>FilterDef</code> that defines our associated Filter.
     */
    private FilterDef filterDef = null;


    /**
     * Does the filter instance need to be initialized?
     */
    private boolean needInitialize = true;


    // --------------------------------------------------- FilterConfig Methods


    /**
     * Return the name of the filter we are configuring.
     */
    public String getFilterName() {
        return (filterDef.getFilterName());
    }


    /**
     * Checks if this filter has been annotated or flagged in the deployment
     * descriptor as being able to support asynchronous operations.
     *
     * @return true if this filter supports async operations, and false
     * otherwise
     */
    public boolean isAsyncSupported() {
        return filterDef.isAsyncSupported();
    }


    /**
     * Return a <code>String</code> containing the value of the named
     * initialization parameter, or <code>null</code> if the parameter
     * does not exist.
     *
     * @param name Name of the requested initialization parameter
     */
    public String getInitParameter(String name) {
        return filterDef.getInitParameter(name);
    }


    /**
     * Return an <code>Enumeration</code> of the names of the initialization
     * parameters for this Filter.
     */
    public Enumeration<String> getInitParameterNames() {
        return filterDef.getInitParameterNames();
    }


    /**
     * Return the ServletContext of our associated web application.
     */
    public ServletContext getServletContext() {
        return (this.context.getServletContext());
    }


    /**
     * Return a String representation of this object.
     */
    public String toString() {
        StringBuilder sb = new StringBuilder("ApplicationFilterConfig[");
        sb.append("name=");
        sb.append(filterDef.getFilterName());
        sb.append(", filterClass=");
        sb.append(filterDef.getFilterClassName());
        sb.append("]");
        return (sb.toString());
    }


    // -------------------------------------------------------- Package Methods


    /**
     * Return the application Filter we are configured for.
     */
    synchronized Filter getFilter() throws Exception {

        // Return the existing filter instance, if any
        if (filter != null && !needInitialize) {
            return filter;
        }

        if (filter == null) {
            Class<? extends Filter> clazz = filterDef.getFilterClass();
            if (clazz == null) {
                // Identify the class loader we will be using
                ClassLoader classLoader = null;
                String filterClassName = filterDef.getFilterClassName();
                if (filterClassName.startsWith("org.apache.catalina.")) {
                    classLoader = this.getClass().getClassLoader();
                } else {
                    classLoader = context.getLoader().getClassLoader();
                }

                // Instantiate a new instance of this filter and return it
                clazz = loadFilterClass(classLoader, filterClassName);
            }

            this.filter = context.createFilterInstance(clazz);
        }

        // START PWC 1.2
        if (context != null) {
            context.fireContainerEvent(
                ContainerEvent.BEFORE_FILTER_INITIALIZED,
                filter);
        }
        // END PWC 1.2

        filter.init(this);
        needInitialize = false;

        // START PWC 1.2
        if (context != null) {
            context.fireContainerEvent(ContainerEvent.AFTER_FILTER_INITIALIZED,
                                       filter);
        }
        // END PWC 1.2

        return (this.filter);
    }

    @SuppressWarnings("unchecked")
    private Class<? extends Filter> loadFilterClass(ClassLoader classLoader,
            String filterClassName) throws ClassNotFoundException {
        return (Class<? extends Filter>)classLoader.loadClass(filterClassName);
    }


    /**
     * Return the filter definition we are configured for.
     */
    FilterDef getFilterDef() {
        return (this.filterDef);
    }


    /**
     * Release the Filter instance associated with this FilterConfig,
     * if there is one.
     */
    void release() {

        if (this.filter != null){

            if (context != null) {
                context.fireContainerEvent(
                    ContainerEvent.BEFORE_FILTER_DESTROYED,
                    filter);
            }

            // START SJS WS 7.0 6236329
            //if( System.getSecurityManager() != null) {
            if ( SecurityUtil.executeUnderSubjectDoAs() ){
            // END OF SJS WS 7.0 6236329
                try{
                    SecurityUtil.doAsPrivilege("destroy",
                                               filter); 
                    SecurityUtil.remove(filter);
                } catch(java.lang.Exception ex){
                    String msg = rb.getString(LogFacade.DO_AS_PRIVILEGE);
                    log.log(Level.SEVERE, msg, ex);
                }
            } else { 
                filter.destroy();
            }

            if (context != null) {
                context.fireContainerEvent(
                    ContainerEvent.AFTER_FILTER_DESTROYED,
                    filter);
                // See GlassFish IT 7071
                context = null;
            }

        }

        this.filter = null;
        needInitialize = true;
     }


    /**
     * Set the filter definition we are configured for.  This has the side
     * effect of instantiating an instance of the corresponding filter class.
     *
     * @param filterDef The new filter definition
     *
     * @exception ClassCastException if the specified class does not implement
     *  the <code>javax.servlet.Filter</code> interface
     * @exception ClassNotFoundException if the filter class cannot be found
     * @exception IllegalAccessException if the filter class cannot be
     *  publicly instantiated
     * @exception InstantiationException if an exception occurs while
     *  instantiating the filter object
     * @exception ServletException if thrown by the filter's init() method
     */
    void setFilterDef(FilterDef filterDef)
        throws ClassCastException, ClassNotFoundException,
               IllegalAccessException, InstantiationException,
               ServletException {

        this.filterDef = filterDef;
        if (filterDef == null) {

            // Release any previously allocated filter instance
            if (this.filter != null){
                // START SJS WS 7.0 6236329
                //if( System.getSecurityManager() != null) {
                if ( SecurityUtil.executeUnderSubjectDoAs() ){
                // END OF SJS WS 7.0 6236329
                    try{
                        SecurityUtil.doAsPrivilege("destroy",
                                                   filter);  
                        SecurityUtil.remove(filter);
                    } catch(java.lang.Exception ex){
                        String msg = rb.getString(LogFacade.DO_AS_PRIVILEGE);
                        log.log(Level.SEVERE, msg, ex);
                    }
                } else { 
                    filter.destroy();
                }
            }
            this.filter = null;

        } else {
            filter = filterDef.getFilter();
        }
    }

}
