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

package org.apache.catalina.deploy;


import org.apache.catalina.util.RequestUtil;

import java.io.Serializable;


/**
 * Representation of a web resource collection for a web application's security
 * constraint, as represented in a <code>&lt;web-resource-collection&gt;</code>
 * element in the deployment descriptor.
 * <p>
 * <b>WARNING</b>:  It is assumed that instances of this class will be created
 * and modified only within the context of a single thread, before the instance
 * is made visible to the remainder of the application.  After that, only read
 * access is expected.  Therefore, none of the read and write access within
 * this class is synchronized.
 *
 * @author Craig R. McClanahan
 * @version $Revision: 1.2 $ $Date: 2005/12/08 01:27:42 $
 */

public class SecurityCollection implements Serializable {


    // ----------------------------------------------------------- Constructors


    /**
     * Construct a new security collection instance with default values.
     */
    public SecurityCollection() {

        this(null, null);

    }


    /**
     * Construct a new security collection instance with specified values.
     *
     * @param name Name of this security collection
     */
    public SecurityCollection(String name) {

        this(name, null);

    }


    /**
     * Construct a new security collection instance with specified values.
     *
     * @param name Name of this security collection
     * @param description Description of this security collection
     */
    public SecurityCollection(String name, String description) {

        super();
        setName(name);
        setDescription(description);

    }


    // ----------------------------------------------------- Instance Variables


    /**
     * Description of this web resource collection.
     */
    private String description = null;


    /**
     * The HTTP methods covered by this web resource collection.
     */
    private String methods[] = new String[0];


    /**
     * The HTTP method omissions covered by this web resource collection.
     */
    private String methodOmissions[] = new String[0];


    /**
     * The name of this web resource collection.
     */
    private String name = null;


    /**
     * The URL patterns protected by this security collection.
     */
    private String patterns[] = new String[0];


    // ------------------------------------------------------------- Properties


    /**
     * Return the description of this web resource collection.
     */
    public String getDescription() {

        return (this.description);

    }


    /**
     * Set the description of this web resource collection.
     *
     * @param description The new description
     */
    public void setDescription(String description) {

        this.description = description;

    }


    /**
     * Return the name of this web resource collection.
     */
    public String getName() {

        return (this.name);

    }


    /**
     * Set the name of this web resource collection
     *
     * @param name The new name
     */
    public void setName(String name) {

        this.name = name;

    }


    // --------------------------------------------------------- Public Methods


    /**
     * Add an HTTP request method to be part of this web resource collection.
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


    /**
     * Add an HTTP request method omission to be part of this web resource collection.
     */
    public void addMethodOmission(String methodOmission) {

        if (methodOmission == null)
            return;
        String results[] = new String[methodOmissions.length + 1];
        for (int i = 0; i < methodOmissions.length; i++)
            results[i] = methodOmissions[i];
        results[methodOmissions.length] = methodOmission;
        methodOmissions = results;

    }


    /**
     * Add a URL pattern to be part of this web resource collection.
     */
    public void addPattern(String pattern) {

        if (pattern == null)
            return;
        pattern = RequestUtil.urlDecode(pattern);
        String results[] = new String[patterns.length + 1];
        for (int i = 0; i < patterns.length; i++)
            results[i] = patterns[i];
        results[patterns.length] = pattern;
        patterns = results;

    }


    /**
     * Return <code>true</code> if the specified HTTP request method is
     * part of this web resource collection.
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
     * Return the set of HTTP request methods that are part of this web
     * resource collection, or a zero-length array if all request methods
     * are included.
     */
    public String[] findMethods() {

        return (methods);

    }


    /**
     * Return <code>true</code> if the specified HTTP request method omission is
     * part of this web resource collection.
     *
     * @param method Request method to check
     */
    public boolean findMethodOmission(String methodOmission) {

        if (methodOmissions.length == 0)
            return (false);
        for (int i = 0; i < methodOmissions.length; i++) {
            if (methodOmissions[i].equals(methodOmission))
                return (true);
        }
        return (false);

    }


    /**
     * Return the set of HTTP request method omissions that are part of this web
     * resource collection.
     */
    public String[] findMethodOmissions() {

        return (methodOmissions);

    }


    /**
     * Is the specified pattern part of this web resource collection?
     *
     * @param pattern Pattern to be compared
     */
    public boolean findPattern(String pattern) {

        for (int i = 0; i < patterns.length; i++) {
            if (patterns[i].equals(pattern))
                return (true);
        }
        return (false);

    }


    /**
     * Return the set of URL patterns that are part of this web resource
     * collection.  If none have been specified, a zero-length array is
     * returned.
     */
    public String[] findPatterns() {

        return (patterns);

    }


    /**
     * Remove the specified HTTP request method from those that are part
     * of this web resource collection.
     *
     * @param method Request method to be removed
     */
    public void removeMethod(String method) {

        if (method == null)
            return;
        int n = -1;
        for (int i = 0; i < methods.length; i++) {
            if (methods[i].equals(method)) {
                n = i;
                break;
            }
        }
        if (n >= 0) {
            int j = 0;
            String results[] = new String[methods.length - 1];
            for (int i = 0; i < methods.length; i++) {
                if (i != n)
                    results[j++] = methods[i];
            }
            methods = results;
        }

    }


    /**
     * Remove the specified HTTP request method omission from those that are part
     * of this web resource collection.
     *
     * @param method Request method to be removed
     */
    public void removeMethodOmission(String methodOmission) {

        if (methodOmission == null)
            return;
        int n = -1;
        for (int i = 0; i < methodOmissions.length; i++) {
            if (methodOmissions[i].equals(methodOmission)) {
                n = i;
                break;
            }
        }
        if (n >= 0) {
            int j = 0;
            String results[] = new String[methodOmissions.length - 1];
            for (int i = 0; i < methodOmissions.length; i++) {
                if (i != n)
                    results[j++] = methodOmissions[i];
            }
            methodOmissions = results;
        }

    }


    /**
     * Remove the specified URL pattern from those that are part of this
     * web resource collection.
     *
     * @param pattern Pattern to be removed
     */
    public void removePattern(String pattern) {

        if (pattern == null)
            return;
        int n = -1;
        for (int i = 0; i < patterns.length; i++) {
            if (patterns[i].equals(pattern)) {
                n = i;
                break;
            }
        }
        if (n >= 0) {
            int j = 0;
            String results[] = new String[patterns.length - 1];
            for (int i = 0; i < patterns.length; i++) {
                if (i != n)
                    results[j++] = patterns[i];
            }
            patterns = results;
        }

    }


    /**
     * Return a String representation of this security collection.
     */
    public String toString() {

        StringBuilder sb = new StringBuilder("SecurityCollection: ");
        sb.append(" name: ").append(name);
        sb.append(" description ").append(description);
        for (String method : methods) {
            sb.append(" method: ").append(method);
        }
        for (String methodOmission : methodOmissions) {
            sb.append(" methodOmission ").append(methodOmission);
        }
        for (String pattern : patterns) {
            sb.append(" pattern ").append(pattern);
        }
        return (sb.toString());

    }


}
