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

import java.io.Serializable;
import java.util.Locale;


/**
 * Representation of a security constraint element for a web application,
 * as represented in a <code>&lt;security-constraint&gt;</code> element in the
 * deployment descriptor.
 * <p>
 * <b>WARNING</b>:  It is assumed that instances of this class will be created
 * and modified only within the context of a single thread, before the instance
 * is made visible to the remainder of the application.  After that, only read
 * access is expected.  Therefore, none of the read and write access within
 * this class is synchronized.
 *
 * @author Craig R. McClanahan
 * @version $Revision: 1.3 $ $Date: 2005/12/08 01:27:42 $
 */

public class SecurityConstraint implements Serializable {


    // ----------------------------------------------------------- Constructors


    /**
     * Construct a new security constraint instance with default values.
     */
    public SecurityConstraint() {

        super();

    }


    // ----------------------------------------------------- Instance Variables


    /**
     * Was the "all roles" wildcard included in the authorization constraints
     * for this security constraint?
     */
    private boolean allRoles = false;


    /**
     * Was an authorization constraint included in this security constraint?
     * This is necessary to distinguish the case where an auth-constraint with
     * no roles (signifying no direct access at all) was requested, versus
     * a lack of auth-constraint which implies no access control checking.
     */
    private boolean authConstraint = false;


    /**
     * The set of roles permitted to access resources protected by this
     * security constraint.
     */
    private String authRoles[] = new String[0];


    /**
     * The set of web resource collections protected by this security
     * constraint.
     */
    private SecurityCollection collections[] = new SecurityCollection[0];


    /**
     * The display name of this security constraint.
     */
    private String displayName = null;


    /**
     * The user data constraint for this security constraint.  Must be NONE,
     * INTEGRAL, or CONFIDENTIAL.
     */
    private String userConstraint = "NONE";


    // ------------------------------------------------------------- Properties


    /**
     * Was the "all roles" wildcard included in this authentication
     * constraint?
     */
    public boolean getAllRoles() {

        return (this.allRoles);

    }


    /**
     * Return the authorization constraint present flag for this security
     * constraint.
     */
    public boolean getAuthConstraint() {

        return (this.authConstraint);

    }


    /**
     * Set the authorization constraint present flag for this security
     * constraint.
     */
    public void setAuthConstraint(boolean authConstraint) {

        this.authConstraint = authConstraint;

    }


    /**
     * Return the display name of this security constraint.
     */
    public String getDisplayName() {

        return (this.displayName);

    }


    /**
     * Set the display name of this security constraint.
     */
    public void setDisplayName(String displayName) {

        this.displayName = displayName;

    }


    /**
     * Return the user data constraint for this security constraint.
     */
    public String getUserConstraint() {

        return (userConstraint);

    }


    /**
     * Set the user data constraint for this security constraint.
     *
     * @param userConstraint The new user data constraint
     */
    public void setUserConstraint(String userConstraint) {

        if (userConstraint != null)
            this.userConstraint = userConstraint;

    }


    // --------------------------------------------------------- Public Methods


    /**
     * Add an authorization role, which is a role name that will be
     * permitted access to the resources protected by this security constraint.
     *
     * @param authRole Role name to be added
     */
    public void addAuthRole(String authRole) {

        if (authRole == null)
            return;
        if ("*".equals(authRole)) {
            allRoles = true;
            return;
        }
        String results[] = new String[authRoles.length + 1];
        for (int i = 0; i < authRoles.length; i++)
            results[i] = authRoles[i];
        results[authRoles.length] = authRole;
        authRoles = results;
        authConstraint = true;

    }


    /**
     * Add a new web resource collection to those protected by this
     * security constraint.
     *
     * @param collection The new web resource collection
     */
    public void addCollection(SecurityCollection collection) {

        if (collection == null)
            return;
        SecurityCollection results[] =
            new SecurityCollection[collections.length + 1];
        for (int i = 0; i < collections.length; i++)
            results[i] = collections[i];
        results[collections.length] = collection;
        collections = results;

    }


    /**
     * Return <code>true</code> if the specified role is permitted access to
     * the resources protected by this security constraint.
     *
     * @param role Role name to be checked
     */
    public boolean findAuthRole(String role) {

        if (role == null)
            return (false);
        for (int i = 0; i < authRoles.length; i++) {
            if (role.equals(authRoles[i]))
                return (true);
        }
        return (false);

    }


    /**
     * Return the set of roles that are permitted access to the resources
     * protected by this security constraint.  If none have been defined,
     * a zero-length array is returned (which implies that all authenticated
     * users are permitted access).
     */
    public String[] findAuthRoles() {

        return (authRoles);

    }


    /**
     * Return the web resource collection for the specified name, if any;
     * otherwise, return <code>null</code>.
     *
     * @param name Web resource collection name to return
     */
    public SecurityCollection findCollection(String name) {

        if (name == null)
            return (null);
        for (int i = 0; i < collections.length; i++) {
            if (name.equals(collections[i].getName()))
                return (collections[i]);
        }
        return (null);

    }


    /**
     * Return all of the web resource collections protected by this
     * security constraint.  If there are none, a zero-length array is
     * returned.
     */
    public SecurityCollection[] findCollections() {

        return (collections);

    }


    /**
     * Return <code>true</code> if the specified context-relative URI (and
     * associated HTTP method) are protected by this security constraint.
     *
     * @param uri Context-relative URI to check
     * @param method Request method being used
     */
    /* SJSWS 6324431
    public boolean included(String uri, String method) {
    */
    // START SJSWS 6324431
    public boolean included(String uri, String method, 
                            boolean caseSensitiveMapping) {
    // END SJSWS 6324431

        // We cannot match without a valid request method
        if (method == null)
            return (false);

        // Check all of the collections included in this constraint
        for (int i = 0; i < collections.length; i++) {
            if (!collections[i].findMethod(method))
                continue;
            String patterns[] = collections[i].findPatterns();
            for (int j = 0; j < patterns.length; j++) {
                /* SJSWS 6324431
                if (matchPattern(uri, patterns[j]))
                */
                // START SJSWS 6324431
                if (matchPattern(uri, patterns[j], 
                                 caseSensitiveMapping))
                // END SJSWS 6324431
                    return (true);
            }
        }

        // No collection included in this constraint matches this request
        return (false);

    }


    /**
     * Remove the specified role from the set of roles permitted to access
     * the resources protected by this security constraint.
     *
     * @param authRole Role name to be removed
     */
    public void removeAuthRole(String authRole) {

        if (authRole == null)
            return;
        int n = -1;
        for (int i = 0; i < authRoles.length; i++) {
            if (authRoles[i].equals(authRole)) {
                n = i;
                break;
            }
        }
        if (n >= 0) {
            int j = 0;
            String results[] = new String[authRoles.length - 1];
            for (int i = 0; i < authRoles.length; i++) {
                if (i != n)
                    results[j++] = authRoles[i];
            }
            authRoles = results;
        }

    }


    /**
     * Remove the specified web resource collection from those protected by
     * this security constraint.
     *
     * @param collection Web resource collection to be removed
     */
    public void removeCollection(SecurityCollection collection) {

        if (collection == null)
            return;
        int n = -1;
        for (int i = 0; i < collections.length; i++) {
            if (collections[i].equals(collection)) {
                n = i;
                break;
            }
        }
        if (n >= 0) {
            int j = 0;
            SecurityCollection results[] =
                new SecurityCollection[collections.length - 1];
            for (int i = 0; i < collections.length; i++) {
                if (i != n)
                    results[j++] = collections[i];
            }
            collections = results;
        }

    }


    /**
     * Return a String representation of this security constraint.
     */
    public String toString() {

        StringBuilder sb = new StringBuilder("SecurityConstraint: ");
        for (SecurityCollection collection : collections) {
            sb.append(" collection: ").append(collection);
        }
        for (String authRole : authRoles) {
            sb.append(" authRole: "+authRole);
        }
        sb.append(" userConstraint: ").append(userConstraint);
        return (sb.toString());

    }


    // -------------------------------------------------------- Private Methods


    /**
     * Does the specified request path match the specified URL pattern?
     * This method follows the same rules (in the same order) as those used
     * for mapping requests to servlets.
     *
     * @param path Context-relative request path to be checked
     *  (must start with '/')
     * @param pattern URL pattern to be compared against
     */
    /* SJSWS 6324431
    private boolean matchPattern(String path, String pattern) {
    */
    // START SJSWS 6324431
    private boolean matchPattern(String path, String pattern,
                                 boolean caseSensitiveMapping) {
    // END SJSWS 6324431        

        // Normalize the argument strings
        if ((path == null) || (path.length() == 0))
            path = "/";
        if ((pattern == null) || (pattern.length() == 0))
            pattern = "/";

        // START SJSWS 6324431
        if (!caseSensitiveMapping) {
            path = path.toLowerCase(Locale.ENGLISH);
            pattern = pattern.toLowerCase(Locale.ENGLISH);
        }
        // END SJSWS 6324431

        // Check for exact match
        if (path.equals(pattern))
            return (true);

        // Check for path prefix matching
        if (pattern.startsWith("/") && pattern.endsWith("/*")) {
            pattern = pattern.substring(0, pattern.length() - 2);
            if (pattern.length() == 0)
                return (true);  // "/*" is the same as "/"
            if (path.endsWith("/"))
                path = path.substring(0, path.length() - 1);
            while (true) {
                if (pattern.equals(path))
                    return (true);
                int slash = path.lastIndexOf('/');
                if (slash <= 0)
                    break;
                path = path.substring(0, slash);
            }
            return (false);
        }

        // Check for suffix matching
        if (pattern.startsWith("*.")) {
            int slash = path.lastIndexOf('/');
            int period = path.lastIndexOf('.');
            if ((slash >= 0) && (period > slash) &&
                path.endsWith(pattern.substring(1))) {
                return (true);
            }
            return (false);
        }

        // Check for universal mapping
        if (pattern.equals("/"))
            return (true);

        return (false);

    }


}
