/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.appserv.web.cache.mapping;

import com.sun.appserv.web.cache.CacheHelper;

/** CacheMapping represents a cache target specified via servlet-name or a 
 *  url-pattern, a timeout, allowed methods, a set of key fields to be used to 
 *  construct the key into the cache, and a set of constraints on the field 
 *  values. 
 */
public class CacheMapping {
    private String helperNameRef;
    private String servletName; 
    private String urlPattern; 

    // timeout specified value or via Field 
    private int timeout = CacheHelper.TIMEOUT_VALUE_NOT_SET;
    private Field timeoutField = null; 

    // a field to force caching engine to refresh entry
    private Field refreshField = null; 

    // set of standard HTTP methods eligible for caching 
    private String methods[] = new String[0];

    // components of the key to be used to access the cache. 
    private Field keyFields[] = new Field[0]; 

    // additional cache constraints 
    private ConstraintField constraintFields[] = new ConstraintField[0]; 

    /** default cache mapping
     */
    public CacheMapping() {
    }

    // public config getters and setters 

    /**
     * set the helper name ref associated with this cache mapping
     * @param helperNameRef helper name ref for this cache mapping
     */
    public void setHelperNameRef(String helperNameRef) {
        this.helperNameRef = helperNameRef;
    }

    /** set the servlet-name this mapping applies
     * @param servletName name of the servlet
     */
    public void setServletName(String servletName) {
        this.servletName = servletName;
    }

    /** set the url-pattern this mapping applies
     * @param urlPattern url pattern this mapping applies
     */
    public void setURLPattern(String urlPattern) {
        this.urlPattern = urlPattern;
    }

    /** set the timeout
     * @param timeout specific timeout of the cacheable entries
     */
    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    /** set the timeout field
     * @param field default timeout of the cacheable entries
     */
    public void setTimeoutField(Field field) {
        this.timeoutField = field;
    }

    /** set the refresh field
     * @param field Boolean field for controlling when a refresh is needed
     */
    public void setRefreshField(Field field) {
        this.refreshField = field;
    }

    /** get the refresh field
     * @return Configured refresh field
     */
    public Field getRefreshField() {
        return refreshField;
    }

    /** set allowable HTTP methods
     * @param methods allowable methods
     */
    public void setMethods(String[] methods) {
        if (methods == null)
            return;

        this.methods = methods; 
    }

    /** add an allowable HTTP method
     * @param method allowable method
     */
    public void addMethod(String method) {
        if (method == null)
            return;

        String results[] = new String[methods.length + 1];
        for (int i = 0; i < methods.length; i++)
            results[i] = methods[i];
        results[methods.length] = method;

        methods = results;
    }
    
    /** add a key field
     * @param field key Field to add
     */
    public void addKeyField(Field field) {
        if (field == null)
            return;

        Field results[] = new Field[keyFields.length + 1];
        for (int i = 0; i < keyFields.length; i++)
            results[i] = keyFields[i];
        results[keyFields.length] = field;

        keyFields = results;
    }

    /** add a constraint key field
     * @param field ConstraintField to add
     */
    public void addConstraintField(ConstraintField field) {
        if (field == null)
            return;

        ConstraintField results[] = 
                new ConstraintField[constraintFields.length + 1];

        for (int i = 0; i < constraintFields.length; i++)
            results[i] = constraintFields[i];
        results[constraintFields.length] = field;

        constraintFields = results;
    }

    /*** cache-mapping getter methods ***/

    /**
     * get helper-name-ref associated with this mapping
     * @return helper name associated
     */
    public String getHelperNameRef() {
        return helperNameRef;
    }

    /**
     * get the underlying servlet-name
     * @return servlet name
     */
    public String getServletName() {
        return servletName;
    }

    /**
     * get the underlying url-pattern this mapping applicable
     * @return url-pattern string configured
     */
    public String getURLPattern() {
        return urlPattern;
    }

    /**
     * Return <code>true</code> if the specified HTTP request method is
     * allowed for caching
     *
     * @param method Request method to check
     */
    public boolean findMethod(String method) {
        if (methods.length == 0)
            return (true);
        for (int i = 0; i < methods.length; i++) {
             if (methods[i].equals(method))
             return (true);
        }
        return (false);
    }

    /**
     * get the timeout 
     * @return timeout value configured
     */
    public int getTimeout() {
        return timeout;
    }

    /**
     * get the timeout field 
     * @return timeout field configured
     */
    public Field getTimeoutField() {
        return timeoutField;
    }

    /**
     * get the key fields
     * @return key fields configured
     */
    public Field[] getKeyFields() {
        return keyFields;
    }

    /**
     * get the constraint fields
     * @return constraint fields configured
     */
    public ConstraintField[] getConstraintFields() {
        return constraintFields;
    }
}
