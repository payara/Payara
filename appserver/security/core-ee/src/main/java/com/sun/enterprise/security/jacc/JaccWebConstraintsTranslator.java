/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2013 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
// Portions Copyright [2018-2021] [Payara Foundation and/or its affiliates]
package com.sun.enterprise.security.jacc;

import com.sun.enterprise.deployment.SecurityRoleDescriptor;
import com.sun.enterprise.deployment.WebBundleDescriptor;
import com.sun.enterprise.deployment.WebComponentDescriptor;
import com.sun.enterprise.deployment.web.*;
import org.glassfish.security.common.Role;

import jakarta.security.jacc.*;
import java.security.Permission;
import java.security.Permissions;
import java.util.*;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.sun.enterprise.security.jacc.MethodValue.methodArrayToSet;
import static com.sun.logging.LogDomains.SECURITY_LOGGER;
import static java.util.Collections.list;
import static java.util.logging.Level.*;

/**
 * This class is used for translating security constrains from <code>web.xml</code> and corresponding
 * annotations into JACC permissions, and writing this to the pluggable {@link PolicyConfiguration} (which is
 * EE standard permission repository).
 *
 * @author Harpreet Singh
 * @author Jean-Francois Arcand
 * @author Ron Monzillo
 * @author Arjan Tijms (refactoring)
 */
public class JaccWebConstraintsTranslator {

    static final Logger logger = Logger.getLogger(SECURITY_LOGGER);

    /* Changed to order default pattern / below extension */
    private static final int PT_DEFAULT = 0;
    private static final int PT_EXTENSION = 1;
    private static final int PT_PREFIX = 2;
    private static final int PT_EXACT = 3;

    private JaccWebConstraintsTranslator() {
    }

    /**
     * Translate the security constraints presents in the given <code>WebBundleDescriptor</code> to JACC permissions
     * and store those in the given <code>PolicyConfiguration</code>.
     *
     * @param webBundleDescriptor the source of the security constraints
     * @param policyConfiguration the target of the security permissions
     * @throws PolicyContextException
     */
    public static void translateConstraintsToPermissions(WebBundleDescriptor webBundleDescriptor, PolicyConfiguration policyConfiguration) throws PolicyContextException {
        createResourceAndDataPermissions(webBundleDescriptor, policyConfiguration);
        createWebRoleRefPermission(webBundleDescriptor, policyConfiguration);
    }

    private static void createResourceAndDataPermissions(WebBundleDescriptor webBundleDescriptor, PolicyConfiguration policyConfiguration) throws PolicyContextException {
        if (logger.isLoggable(FINE)) {
            logger.entering(JaccWebConstraintsTranslator.class.getSimpleName(), "processConstraints");
            logger.log(FINE, "JACC: constraint translation: CODEBASE = " + policyConfiguration.getContextID());
        }

        // ### 1 ###

        // Parse the constraints in the webBundleDescriptor (representing web.xml and annotations) into
        // a number of raw pattern builders. From these pattern builders we'll generate and write out
        // permissions below

        Map<String, PatternBuilder> patternBuilderMap = parseConstraints(webBundleDescriptor);

        // Permissions for resources that can't be accessed by anyone
        Permissions excluded = new Permissions();

        // Permissions for resources that are open to be accessed by everyone
        Permissions unchecked = new Permissions();

        // Permissions for resources that require a role
        Map<String, Permissions> perRole = new HashMap<String, Permissions>();

        boolean deny = webBundleDescriptor.isDenyUncoveredHttpMethods();

        if (logger.isLoggable(FINE)) {
            logger.fine(
                "JACC: constraint capture: begin processing qualified url patterns" +
                " - uncovered http methods will be " +
                (deny ? "denied" : "permitted"));
        }


        // ### 2 ###

        // For all patterns that were created by the "parseConstraints" methods above, we now
        // create permissions and add these to the various collections defined above.

        for (PatternBuilder patternBuilder : patternBuilderMap.values()) {
            if (!patternBuilder.irrelevantByQualifier) {

                String urlPatternSpec = patternBuilder.urlPatternSpec.toString();

                if (logger.isLoggable(FINE)) {
                    logger.fine("JACC: constraint capture: urlPattern: " + urlPatternSpec);
                }

                // Handle uncovered methods
                patternBuilder.handleUncovered(deny);

                // Handle excluded methods - adds resource permissions to the excluded collection
                handleExcluded(excluded, patternBuilder, urlPatternSpec);

                // Handle methods requiring a role - adds resource permissions to the per role collection
                handlePerRole(perRole, patternBuilder, urlPatternSpec);

                // Handle unchecked methods - adds resource permissions to the unchecked collection
                handleUnchecked(unchecked, patternBuilder, urlPatternSpec);

                // Handle transport constraints - adds data permissions to the unchecked collection
                handleConnections(unchecked, patternBuilder, urlPatternSpec);
            }
        }

        // ### 3 ###

        // Now that we have created and added permissions to the various collections, we'll write them
        // out to the policyConfiguration


        // Write out the translated/generated excluded permissions
        policyConfiguration.addToExcludedPolicy(excluded);

        // Write out the translated/generated unchecked permissions
        policyConfiguration.addToUncheckedPolicy(unchecked);

        logExcludedUncheckedPermissionsWritten(excluded, unchecked);

        // Write out the translated/generated per role permissions
        for (Entry<String, Permissions> roleEntry : perRole.entrySet()) {
            String role = roleEntry.getKey();
            Permissions permissions = roleEntry.getValue();

            policyConfiguration.addToRole(role, permissions);

            logPerRolePermissionsWritten(role, permissions);
        }

        if (logger.isLoggable(Level.FINE)) {
            logger.exiting(JaccWebConstraintsTranslator.class.getSimpleName(), "processConstraints");
        }
    }

    private static Map<String, PatternBuilder> parseConstraints(WebBundleDescriptor webBundleDescriptor) {
        if (logger.isLoggable(FINE)) {
            logger.entering(JaccWebConstraintsTranslator.class.getSimpleName(), "parseConstraints");
        }

        Set<Role> roleSet = webBundleDescriptor.getRoles();

        Map<String, PatternBuilder> patternBuilderMap = new HashMap<>();

        // Bootstrap the map with the default pattern; the default pattern will not be "committed", unless a constraint is
        // defined on "\". This will ensure that a more restrictive constraint can be assigned to it
        patternBuilderMap.put("/", new PatternBuilder("/"));

        // Iterate over security constraints
        for (SecurityConstraint securityConstraint : webBundleDescriptor.getSecurityConstraintsSet()) {

            logger.fine("JACC: constraint translation: begin parsing security constraint");

            AuthorizationConstraint authorizationConstraint = securityConstraint.getAuthorizationConstraint();
            UserDataConstraint dataConstraint = securityConstraint.getUserDataConstraint();

            // Iterate over collections of URLPatterns within constraint
            for (WebResourceCollection webResourceCollection : securityConstraint.getWebResourceCollections()) {

                logger.fine("JACC: constraint translation: begin parsing web resource collection");

                // Enumerate over URLPatterns within collection
                for (String urlPattern : webResourceCollection.getUrlPatterns()) {
                    if (urlPattern != null) {
                        // FIX TO BE CONFIRMED: encode all colons
                        urlPattern = urlPattern.replaceAll(":", "%3A");
                    }

                    if (logger.isLoggable(FINE)) {
                        logger.fine("JACC: constraint translation: process url pattern: " + urlPattern);
                    }

                    // Determine if pattern is already in map
                    PatternBuilder patternBuilder = patternBuilderMap.get(urlPattern);

                    // Apply new patterns to map
                    if (patternBuilder == null) {
                        patternBuilder = new PatternBuilder(urlPattern);

                        // Iterate over patterns in map
                        for (Entry<String, PatternBuilder> patternBuilderEntry : patternBuilderMap.entrySet()) {

                            String otherUrl = patternBuilderEntry.getKey();

                            int otherUrlType = patternType(otherUrl);
                            switch (patternType(urlPattern)) {

                            // If the new url/pattern is a path-prefix pattern, it must be qualified by every
                            // different (from it) path-prefix pattern (in the map) that is implied by the new
                            // pattern, and every exact pattern (in the map) that is implied by the new URL.
                            //
                            // Also, the new pattern must be added as a qualifier of the default pattern, and every
                            // extension pattern (existing in the map), and of every different path-prefix pattern that
                            // implies the new pattern.
                            //
                            // Note that we know that the new pattern does not exist in the map, thus we know that the
                            // new pattern is different from any existing path prefix pattern.

                            case PT_PREFIX:
                                if ((otherUrlType == PT_PREFIX || otherUrlType == PT_EXACT) && implies(urlPattern, otherUrl)) {
                                    patternBuilder.addQualifier(otherUrl);
                                } else if (otherUrlType == PT_PREFIX && implies(otherUrl, urlPattern)) {
                                    patternBuilderEntry.getValue().addQualifier(urlPattern);
                                } else if (otherUrlType == PT_EXTENSION || otherUrlType == PT_DEFAULT) {
                                    patternBuilderEntry.getValue().addQualifier(urlPattern);
                                }
                                break;

                            // If the new pattern is an extension pattern, it must be qualified by every path-prefix
                            // pattern (in the map), and every exact pattern (in the map) that is implied by
                            // the new pattern.
                            //
                            // Also, it must be added as a qualifier of the default pattern, if it exists in the
                            // map.
                            case PT_EXTENSION:
                                if (otherUrlType == PT_PREFIX || (otherUrlType == PT_EXACT && implies(urlPattern, otherUrl))) {
                                    patternBuilder.addQualifier(otherUrl);
                                } else if (otherUrlType == PT_DEFAULT) {
                                    patternBuilderEntry.getValue().addQualifier(urlPattern);
                                }
                                break;

                            // If the new pattern is the default pattern it must be qualified by every other pattern
                            // in the map.
                            case PT_DEFAULT:
                                if (otherUrlType != PT_DEFAULT) {
                                    patternBuilder.addQualifier(otherUrl);
                                }
                                break;

                            // If the new pattern is an exact pattern, it is not be qualified, but it must be added as
                            // as a qualifier to the default pattern, and to every path-prefix or extension pattern (in
                            // the map) that implies the new pattern.
                            case PT_EXACT:
                                if ((otherUrlType == PT_PREFIX || otherUrlType == PT_EXTENSION) && implies(otherUrl, urlPattern)) {
                                    patternBuilderEntry.getValue().addQualifier(urlPattern);
                                }
                                else if (otherUrlType == PT_DEFAULT) {
                                    patternBuilderEntry.getValue().addQualifier(urlPattern);
                                }
                                break;
                            default:
                                break;
                            }
                        }

                        // Add the new pattern and its pattern spec builder to the map
                        patternBuilderMap.put(urlPattern, patternBuilder);

                    }

                    BitSet methods = methodArrayToSet(webResourceCollection.getHttpMethodsAsArray());

                    BitSet omittedMethods = null;
                    if (methods.isEmpty()) {
                        omittedMethods = methodArrayToSet(webResourceCollection.getHttpMethodOmissionsAsArray());
                    }

                    // Set and commit the method outcomes on the pattern builder
                    //
                    // Note that an empty omitted method set is used to represent
                    // the set of all HTTP methods
                    patternBuilder.setMethodOutcomes(roleSet, authorizationConstraint, dataConstraint, methods, omittedMethods);

                    if (logger.isLoggable(FINE)) {
                        logger.fine("JACC: constraint translation: end processing url pattern: " + urlPattern);
                    }
                }

                logger.fine("JACC: constraint translation: end parsing web resource collection");
            }

            logger.fine("JACC: constraint translation: end parsing security constraint");
        }

        if (logger.isLoggable(FINE)) {
            logger.exiting(JaccWebConstraintsTranslator.class.getName(), "parseConstraints");
        }

        return patternBuilderMap;
    }

    private static void handleExcluded(Permissions collection, PatternBuilder patternBuilder, String name) {
        String actions = null;
        BitSet excludedMethods = patternBuilder.getExcludedMethods();

        if (patternBuilder.otherConstraint.isExcluded()) {
            BitSet methods = patternBuilder.getMethodSet();
            methods.andNot(excludedMethods);
            if (!methods.isEmpty()) {
                actions = "!" + MethodValue.getActions(methods);
            }
        } else if (!excludedMethods.isEmpty()) {
            actions = MethodValue.getActions(excludedMethods);
        } else {
            return;
        }

        collection.add(new WebResourcePermission(name, actions));
        collection.add(new WebUserDataPermission(name, actions));

        if (logger.isLoggable(FINE)) {
            logger.fine("JACC: constraint capture: adding excluded methods: " + actions);
        }
    }

    private static void handlePerRole(Map<String, Permissions> map, PatternBuilder patternBuilder, String urlPatternSpec) {
        Map<String, BitSet> roleMap = patternBuilder.getRoleMap();
        List<String> roleList = null;

        // Handle the roles for the omitted methods
        if (!patternBuilder.otherConstraint.isExcluded() && patternBuilder.otherConstraint.isAuthConstrained()) {
            roleList = patternBuilder.otherConstraint.roleList;

            for (String roleName : roleList) {
                BitSet methods = patternBuilder.getMethodSet();

                // Reduce omissions for explicit methods granted to role
                BitSet roleMethods = roleMap.get(roleName);
                if (roleMethods != null) {
                    methods.andNot(roleMethods);
                }

                String httpMethodSpec = null;
                if (!methods.isEmpty()) {
                    httpMethodSpec = "!" + MethodValue.getActions(methods);
                }

                addToRoleMap(map, roleName, new WebResourcePermission(urlPatternSpec, httpMethodSpec));
            }
        }

        // Handle explicit methods, skip roles that were handled above
        BitSet methods = patternBuilder.getMethodSet();

        if (!methods.isEmpty()) {
            for (Entry<String, BitSet> roleEntry : roleMap.entrySet()) {
                String roleName = roleEntry.getKey();
                if (roleList == null || !roleList.contains(roleName)) {
                    BitSet roleMethods = roleEntry.getValue();
                    if (!roleMethods.isEmpty()) {
                        addToRoleMap(map, roleName, new WebResourcePermission(urlPatternSpec, MethodValue.getActions(roleMethods)));
                    }
                }
            }
        }
    }

    private static void handleUnchecked(Permissions collection, PatternBuilder patternBuilder, String urlPatternSpec) {
        String httpMethodSpec = null;
        BitSet noAuthMethods = patternBuilder.getNoAuthMethods();

        if (!patternBuilder.otherConstraint.isAuthConstrained()) {
            BitSet methods = patternBuilder.getMethodSet();
            methods.andNot(noAuthMethods);
            if (!methods.isEmpty()) {
                httpMethodSpec = "!" + MethodValue.getActions(methods);
            }
        } else if (!noAuthMethods.isEmpty()) {
            httpMethodSpec = MethodValue.getActions(noAuthMethods);
        } else {
            return;
        }

        collection.add(new WebResourcePermission(urlPatternSpec, httpMethodSpec));

        if (logger.isLoggable(FINE)) {
            logger.fine("JACC: constraint capture: adding unchecked (for authorization) methods: " + httpMethodSpec);
        }
    }

    private static void handleConnections(Permissions permissions, PatternBuilder patternBuilder, String name) {
        BitSet allConnectMethods = null;
        boolean allConnectAtOther = patternBuilder.otherConstraint.isConnectAllowed(ConstraintValue.connectTypeNone);

        for (int i = 0; i < ConstraintValue.connectKeys.length; i++) {

            String actions = null;
            String transport = ConstraintValue.connectKeys[i];

            BitSet connectMethods = patternBuilder.getConnectMap(1 << i);
            if (i == 0) {
                allConnectMethods = connectMethods;
            } else {

                // If connect type protected, remove methods that accept any connect
                connectMethods.andNot(allConnectMethods);
            }

            if (patternBuilder.otherConstraint.isConnectAllowed(1 << i)) {
                if (i != 0 && allConnectAtOther) {

                    // If all connect allowed at other

                    if (connectMethods.isEmpty()) {

                        // Skip, if remainder is empty, because methods that accept any connect were handled at i==0.
                        continue;
                    }

                    // Construct actions using methods with specific connection requirements
                    actions = MethodValue.getActions(connectMethods);
                } else {
                    BitSet methods = patternBuilder.getMethodSet();
                    methods.andNot(connectMethods);
                    if (!methods.isEmpty()) {
                        actions = "!" + MethodValue.getActions(methods);
                    }
                }
            } else if (!connectMethods.isEmpty()) {
                actions = MethodValue.getActions(connectMethods);
            } else {
                continue;
            }

            actions = (actions == null) ? "" : actions;
            String combinedActions = actions + ":" + transport;

            permissions.add(new WebUserDataPermission(name, combinedActions));

            if (logger.isLoggable(FINE)) {
                logger.fine(
                    "JACC: constraint capture: adding methods that accept connections with protection: " +
                    transport +
                    " methods: " + actions);
            }
        }
    }

    static int patternType(Object urlPattern) {
        String pattern = urlPattern.toString();

        if (pattern.startsWith("*.")) {
            return PT_EXTENSION;
        }

        if (pattern.startsWith("/") && pattern.endsWith("/*")) {
            return PT_PREFIX;
        }

        if (pattern.equals("/")) {
            return PT_DEFAULT;
        }

        return PT_EXACT;
    }

    static boolean implies(String pattern, String path) {

        // Check for exact match
        if (pattern.equals(path)) {
            return true;
        }

        // Check for path prefix matching
        if (pattern.startsWith("/") && pattern.endsWith("/*")) {
            pattern = pattern.substring(0, pattern.length() - 2);

            int length = pattern.length();

            if (length == 0) {
                return true; // "/*" is the same as "/"
            }

            return path.startsWith(pattern) && (path.length() == length || path.substring(length).startsWith("/"));
        }

        // Check for suffix matching
        if (pattern.startsWith("*.")) {
            int slash = path.lastIndexOf('/');
            int period = path.lastIndexOf('.');
            if ((slash >= 0) && (period > slash) && path.endsWith(pattern.substring(1))) {
                return true;
            }

            return false;
        }

        // Check for universal mapping
        if (pattern.equals("/")) {
            return true;
        }

        return false;
    }

    private static void addToRoleMap(Map<String, Permissions> roleMap, String roleName, Permission permission) {
        roleMap.computeIfAbsent(roleName, e -> new Permissions())
               .add(permission);

        if (logger.isLoggable(FINE)) {
            logger.fine("JACC: constraint capture: adding methods to role: " + roleName + " methods: " + permission.getActions());
        }
    }

    private static void createWebRoleRefPermission(WebBundleDescriptor webBundleDescriptor, PolicyConfiguration policyConfiguration) throws PolicyContextException {
        if (logger.isLoggable(FINE)) {
            logger.entering(JaccWebConstraintsTranslator.class.getSimpleName(), "createWebRoleRefPermission");
            logger.log(FINE, "JACC: role-reference translation: Processing WebRoleRefPermission : CODEBASE = " + policyConfiguration.getContextID());
        }

        List<Role> servletScopedRoleNames = new ArrayList<>();
        Collection<Role> allRoles = webBundleDescriptor.getRoles();

        Role anyAuthUserRole = new Role("**");
        boolean rolesetContainsAnyAuthUserRole = allRoles.contains(anyAuthUserRole);

        // Iterate through all Servlets in the application and write out role ref permissions for each

        for (WebComponentDescriptor componentDescriptor : webBundleDescriptor.getWebComponentDescriptors()) {

            // Name of Servlet being processed in this iteration
            String servletName = componentDescriptor.getCanonicalName();

            writeOutPermissionsForRoleRefRoles(componentDescriptor.getSecurityRoleReferenceSet(), servletScopedRoleNames, servletName, policyConfiguration);

            if (logger.isLoggable(FINE)) {
                logger.fine("JACC: role-reference translation: Going through the list of roles not present in RoleRef elements and creating WebRoleRefPermissions ");
            }

            // For every role in the application for which there is no mapping (role reference) defined for the current servlet
            // we insert a 1:1 role mapping. E.g global role "foo" maps to an identical named role "foo" in the scope of Servlet
            // "MyServlet"
            //
            // Note this is the most common situation as mapping roles per Servlet is quite rare in practice
            writeOutPermissionsForNonRoleRefRoles(allRoles, servletScopedRoleNames, servletName, policyConfiguration);

            // JACC MR8 add WebRoleRefPermission for the any authenticated user role '**'
            if ((!servletScopedRoleNames.contains(anyAuthUserRole)) && !rolesetContainsAnyAuthUserRole) {
                addAnyAuthenticatedUserRoleRef(policyConfiguration, servletName);
            }
        }

        // After looking at roles per Servlet, look at global concerns

        // For every security role in the web application add a WebRoleRefPermission to the corresponding role. The name of all
        // such permissions shall be the empty string, and the actions of each permission shall be the corresponding role name.

        // When checking a WebRoleRefPermission from a JSP not mapped to a servlet, use a permission with the empty string as
        // its name and with the argument to isUserInRole as its actions
        //
        // Note, this has the effect of creating (web) application scoped roles (global roles), next to the Servlet scoped
        // roles.
        //
        // See also S1AS8PE 4966609
        writeOutGlobalPermissionsForAllRoles(allRoles, policyConfiguration);

        // JACC MR8 add WebRoleRefPermission for the any authenticated user role '**'
        if (!rolesetContainsAnyAuthUserRole) {
            addAnyAuthenticatedUserRoleRef(policyConfiguration, "");
        }

        if (logger.isLoggable(FINE)) {
            logger.exiting(JaccWebConstraintsTranslator.class.getName(), "createWebRoleRefPermission");
        }
    }

    /**
     * Writes out global <code>WebRoleRefPermission</code>s to the <code>PolicyConfiguration</code>, one for each role in
     * the given role collection.
     *
     * @param allRoles collection of all roles in the web application
     * @param policyConfiguration the target that receives the security permissions created by this method
     * @throws PolicyContextException If the policy configuration throws an exception
     */
    private static void writeOutGlobalPermissionsForAllRoles(Collection<Role> allRoles, PolicyConfiguration policyConfiguration) throws PolicyContextException {
        for (Role role : allRoles) {
            if (logger.isLoggable(FINE)) {
                logger.fine("JACC: role-reference translation: Looking at Role =  " + role.getName());
            }

            String roleName = role.getName();
            policyConfiguration.addToRole(roleName, new WebRoleRefPermission("", roleName));

            if (logger.isLoggable(FINE)) {
                logger.fine("JACC: role-reference translation: RoleRef  = " + roleName + " is added for jsp's that can't be mapped to servlets");
                logger.fine("JACC: role-reference translation: Permission added for above role-ref =" + roleName + " " + "");
            }
        }
    }

    private static void writeOutPermissionsForRoleRefRoles(Collection<SecurityRoleReference> securityRoleReferences, Collection<Role> servletScopedRoleNames, String servletName, PolicyConfiguration policyConfiguration) throws PolicyContextException {
        for (SecurityRoleReference roleReference : securityRoleReferences) {
            if (roleReference != null) {

                // The name of a role, local (scoped) to a single Servlet
                String servletScopedRoleName = roleReference.getRoleName();
                servletScopedRoleNames.add(new Role(servletScopedRoleName));

                // The name of the global role to which the local Servlet scoped role links (is mapped)
                String globalRoleName = roleReference.getSecurityRoleLink().getName();

                // Write the role reference to the target policy configuration
                policyConfiguration.addToRole(
                    globalRoleName,
                    new WebRoleRefPermission(servletName, servletScopedRoleName));

                if (logger.isLoggable(FINE)) {
                    logger.fine(
                        "JACC: role-reference translation: " +
                         "RoleRefPermission created with name (servlet-name) = " + servletName +
                         " and action (role-name tag) = " + servletScopedRoleName +
                         " added to role (role-link tag) = " + globalRoleName);
                }
            }
        }
    }

    /**
     *
     * @param allRoles collection of all roles in the web application
     * @param roleRefRoles collection of roles for which there were role references (mappings from global roles)
     * @param componentName name of the (servlet) component for which the permissions are created
     * @param policyConfiguration the target that receives the security permissions created by this method
     * @throws PolicyContextException If the policy configuration throws an exception
     */
    private static void writeOutPermissionsForNonRoleRefRoles(Collection<Role> allRoles, Collection<Role> roleRefRoles, String componentName, PolicyConfiguration policyConfiguration) throws PolicyContextException {
        for (Role role : allRoles) {
            if (logger.isLoggable(FINE)) {
                logger.fine("JACC: role-reference translation: Looking at Role =  " + role.getName());
            }

            // For every role for which we didn't already create a role reference role, create a 1:1 mapping
            // from the global roles.
            if (!roleRefRoles.contains(role)) {

                String roleName = role.getName();
                policyConfiguration.addToRole(roleName, new WebRoleRefPermission(componentName, roleName));

                if (logger.isLoggable(Level.FINE)) {
                    logger.fine("JACC: role-reference translation: RoleRef  = " + roleName + " is added for servlet-resource = " + componentName);
                    logger.fine("JACC: role-reference translation: Permission added for above role-ref =" + componentName + " " + roleName);
                }
            }
        }
    }

    /**
     * JACC MR8 add WebRoleRefPermission for the any authenticated user role '**'
     */
    private static void addAnyAuthenticatedUserRoleRef(PolicyConfiguration policyConfiguration, String name) throws PolicyContextException {
        String action = "**";
        policyConfiguration.addToRole(action, new WebRoleRefPermission(name, action));

        if (logger.isLoggable(FINE)) {
            logger.fine("JACC: any authenticated user role-reference translation: Permission added for role-ref =" + name + " " + action);
        }
    }

    private static void logExcludedUncheckedPermissionsWritten(Permissions excluded, Permissions unchecked) {
        if (logger.isLoggable(FINE)) {
            logger.fine("JACC: constraint capture: end processing qualified url patterns");

            for (Permission p :  list(excluded.elements())) {
                String ptype = (p instanceof WebResourcePermission) ? "WRP  " : "WUDP ";
                logger.fine("JACC: permission(excluded) type: " + ptype + " name: " + p.getName() + " actions: " + p.getActions());
            }

            for (Permission p :  list(unchecked.elements())) {
                String ptype = (p instanceof WebResourcePermission) ? "WRP  " : "WUDP ";
                logger.fine("JACC: permission(unchecked) type: " + ptype + " name: " + p.getName() + " actions: " + p.getActions());
            }
        }
    }

    private static void logPerRolePermissionsWritten(String role, Permissions permissions) {
        if (logger.isLoggable(FINE)) {
            for (Permission p :  list(permissions.elements())) {
                String ptype = (p instanceof WebResourcePermission) ? "WRP  " : "WUDP ";

                logger.fine("JACC: permission(" + role + ") type: " + ptype + " name: " + p.getName() + " actions: " + p.getActions());
            }

        }
    }
}

class ConstraintValue {

    static String connectKeys[] = { "NONE", "INTEGRAL", "CONFIDENTIAL" };

    static int connectTypeNone = 1;
    static HashMap<String, Integer> connectHash = new HashMap<String, Integer>();
    static {
        for (int i = 0; i < connectKeys.length; i++)
            connectHash.put(connectKeys[i], Integer.valueOf(1 << i));
    };

    boolean excluded;
    boolean ignoreRoleList;
    final List<String> roleList = new ArrayList<String>();
    int connectSet;

    ConstraintValue() {
        excluded = false;
        ignoreRoleList = false;
        connectSet = 0;
    }

    static boolean bitIsSet(int map, int bit) {
        return (map & bit) == bit ? true : false;
    }

    void setRole(String role) {
        synchronized (roleList) {
            if (!roleList.contains(role)) {
                roleList.add(role);
            }
        }
    }

    void removeRole(String role) {
        synchronized (roleList) {
            if (roleList.contains(role)) {
                roleList.remove(role);
            }
        }
    }

    void setPredefinedOutcome(boolean outcome) {
        if (!outcome) {
            excluded = true;
        } else {
            ignoreRoleList = true;
        }
    }

    void addConnectType(String guarantee) {
        int b = connectTypeNone;
        if (guarantee != null) {
            Integer bit = connectHash.get(guarantee);
            if (bit == null) {
                throw new IllegalArgumentException("constraint translation error-illegal trx guarantee");
            }

            b = bit.intValue();
        }

        connectSet |= b;
    }

    boolean isExcluded() {
        return excluded;
    }

    /*
     * ignoreRoleList is true if there was a security-constraint without an auth-constraint; such a constraint combines to
     * allow access without authentication.
     */
    boolean isAuthConstrained() {
        if (excluded) {
            return true;
        } else if (ignoreRoleList || roleList.isEmpty()) {
            return false;
        }
        return true;
    }

    boolean isTransportConstrained() {
        if (excluded || (connectSet != 0 && !bitIsSet(connectSet, connectTypeNone))) {
            return true;
        }

        return false;
    }

    boolean isConnectAllowed(int cType) {
        if (!excluded && (connectSet == 0 || bitIsSet(connectSet, connectTypeNone) || bitIsSet(connectSet, cType))) {
            return true;
        }
        return false;
    }

    void setOutcome(Set<Role> roleSet, AuthorizationConstraint ac, UserDataConstraint udc) {
        if (ac == null) {
            setPredefinedOutcome(true);
        } else {
            boolean containsAllRoles = false;
            Enumeration eroles = ac.getSecurityRoles();
            if (!eroles.hasMoreElements()) {
                setPredefinedOutcome(false);
            } else
                while (eroles.hasMoreElements()) {
                    SecurityRoleDescriptor srd = (SecurityRoleDescriptor) eroles.nextElement();
                    String roleName = srd.getName();
                    if ("*".equals(roleName)) {
                        containsAllRoles = true;
                    } else {
                        setRole(roleName);
                    }
                }
            /**
             * JACC MR8 When role '*' named, do not include any authenticated user role '**' unless an application defined a role
             * named '**'
             */
            if (containsAllRoles) {
                removeRole("**");
                Iterator it = roleSet.iterator();
                while (it.hasNext()) {
                    setRole(((Role) it.next()).getName());
                }
            }
        }
        addConnectType(udc == null ? null : udc.getTransportGuarantee());

        if (JaccWebConstraintsTranslator.logger.isLoggable(Level.FINE)) {
            JaccWebConstraintsTranslator.logger.log(Level.FINE, "JACC: setOutcome yields: " + toString());
        }

    }

    void setValue(ConstraintValue constraint) {
        excluded = constraint.excluded;
        ignoreRoleList = constraint.ignoreRoleList;
        roleList.clear();
        Iterator rit = constraint.roleList.iterator();
        while (rit.hasNext()) {
            String role = (String) rit.next();
            roleList.add(role);
        }
        connectSet = constraint.connectSet;
    }

    @Override
    public String toString() {
        StringBuilder roles = new StringBuilder(" roles: ");
        Iterator rit = roleList.iterator();
        while (rit.hasNext()) {
            roles.append(" ").append((String) rit.next());
        }
        StringBuilder transports = new StringBuilder("transports: ");
        for (int i = 0; i < connectKeys.length; i++) {
            if (isConnectAllowed(1 << i)) {
                transports.append(" ").append(connectKeys[i]);
            }
        }
        return " ConstraintValue ( " + " excluded: " + excluded + " ignoreRoleList: " + ignoreRoleList + roles + transports + " ) ";
    }

    /*
     * ignoreRoleList is true if there was a security-constraint without an auth-constraint; such a constraint combines to
     * allow access without authentication.
     */
    boolean isUncovered() {
        return (!excluded && !ignoreRoleList && roleList.isEmpty() && connectSet == 0);
    }
}

class MethodValue extends ConstraintValue {

    private static final List<String> methodNames = new ArrayList<>();

    int index;

    MethodValue(String methodName) {
        index = getMethodIndex(methodName);
    }

    MethodValue(String methodName, ConstraintValue constraint) {
        index = getMethodIndex(methodName);
        setValue(constraint);
    }

    static String getMethodName(int index) {
        synchronized (methodNames) {
            return methodNames.get(index);
        }
    }

    static int getMethodIndex(String name) {
        synchronized (methodNames) {
            int index = methodNames.indexOf(name);
            if (index < 0) {
                index = methodNames.size();
                methodNames.add(index, name);
            }
            return index;
        }
    }

    static String getActions(BitSet methodSet) {
        if (methodSet == null || methodSet.isEmpty()) {
            return null;
        }

        StringBuilder actions = null;

        for (int i = methodSet.nextSetBit(0); i >= 0; i = methodSet.nextSetBit(i + 1)) {
            if (actions == null) {
                actions = new StringBuilder();
            } else {
                actions.append(",");
            }
            actions.append(getMethodName(i));
        }

        return (actions == null ? null : actions.toString());
    }

    static String[] getMethodArray(BitSet methodSet) {
        if (methodSet == null || methodSet.isEmpty()) {
            return null;
        }

        int size = 0;

        List<String> methods = new ArrayList<>();

        for (int i = methodSet.nextSetBit(0); i >= 0; i = methodSet.nextSetBit(i + 1)) {
            methods.add(getMethodName(i));
            size += 1;
        }

        return methods.toArray(new String[size]);
    }

    static BitSet methodArrayToSet(String[] methods) {
        BitSet methodSet = new BitSet();

        for (int i = 0; methods != null && i < methods.length; i++) {
            if (methods[i] == null) {
                throw new IllegalArgumentException("constraint translation error - null method name");
            }
            int bit = getMethodIndex(methods[i]);
            methodSet.set(bit);
        }

        return methodSet;
    }

    @Override
    public String toString() {
        return "MethodValue( " + getMethodName(index) + super.toString() + " )";
    }
}

class PatternBuilder {

    final int patternType;
    final int patternLength;
    final StringBuilder urlPatternSpec;
    final ConstraintValue otherConstraint;
    final Map<String, MethodValue> methodValues = new HashMap<>();

    boolean committed;
    boolean irrelevantByQualifier;

    PatternBuilder(String urlPattern) {
        patternType = JaccWebConstraintsTranslator.patternType(urlPattern);
        patternLength = urlPattern.length();
        urlPatternSpec = new StringBuilder(urlPattern);
        otherConstraint = new ConstraintValue();
    }

    void addQualifier(String urlPattern) {
        if (JaccWebConstraintsTranslator.implies(urlPattern, urlPatternSpec.substring(0, patternLength))) {
            irrelevantByQualifier = true;
        }

        urlPatternSpec.append(":" + urlPattern);
    }

    MethodValue getMethodValue(int methodIndex) {
        String methodName = MethodValue.getMethodName(methodIndex);

        synchronized (methodValues) {
            MethodValue methodValue = methodValues.get(methodName);
            if (methodValue == null) {
                methodValue = new MethodValue(methodName, otherConstraint);
                methodValues.put(methodName, methodValue);

                if (JaccWebConstraintsTranslator.logger.isLoggable(FINE)) {
                    JaccWebConstraintsTranslator.logger.log(FINE, "JACC: created MethodValue: " + methodValue);
                }
            }

            return methodValue;
        }
    }

    BitSet getExcludedMethods() {
        BitSet methodSet = new BitSet();

        synchronized (methodValues) {
            for (MethodValue methodValue : methodValues.values()) {
                if (methodValue.isExcluded()) {
                    methodSet.set(methodValue.index);
                }
            }
        }

        return methodSet;
    }

    BitSet getNoAuthMethods() {
        BitSet methodSet = new BitSet();

        synchronized (methodValues) {
            for (MethodValue methodValue : methodValues.values()) {
                if (!methodValue.isAuthConstrained()) {
                    methodSet.set(methodValue.index);
                }
            }
        }

        return methodSet;
    }

    BitSet getAuthConstrainedMethods() {
        BitSet methodSet = new BitSet();

        synchronized (methodValues) {
            for (MethodValue methodValue : methodValues.values()) {
                if (methodValue.isAuthConstrained()) {
                    methodSet.set(methodValue.index);
                }
            }
        }

        return methodSet;
    }

    BitSet getTransportConstrainedMethods() {
        BitSet methodSet = new BitSet();

        synchronized (methodValues) {
            for (MethodValue methodValue : methodValues.values()) {
                if (methodValue.isTransportConstrained()) {
                    methodSet.set(methodValue.index);
                }
            }
        }

        return methodSet;
    }

    /**
     * Map of methods allowed per role
     */
    HashMap<String, BitSet> getRoleMap() {
        HashMap<String, BitSet> roleMap = new HashMap<>();

        synchronized (methodValues) {
            for (MethodValue methodValue : methodValues.values()) {
                if (!methodValue.isExcluded() && methodValue.isAuthConstrained()) {
                    for (String role : methodValue.roleList) {
                        roleMap.computeIfAbsent(role, e -> new BitSet())
                               .set(methodValue.index);
                    }
                }
            }
        }

        return roleMap;
    }

    BitSet getConnectMap(int cType) {
        BitSet methodSet = new BitSet();

        synchronized (methodValues) {
            for (MethodValue methodValue : methodValues.values()) {
                /*
                 * NOTE WELL: prior version of this method could not be called during constraint parsing because it finalized the
                 * connectSet when its value was 0 (indicating any connection, until some specific bit is set) if (v.connectSet == 0) {
                 * v.connectSet = MethodValue.connectTypeNone; }
                 */

                if (methodValue.isConnectAllowed(cType)) {
                    methodSet.set(methodValue.index);
                }
            }
        }

        return methodSet;
    }

    BitSet getMethodSet() {
        BitSet methodSet = new BitSet();

        synchronized (methodValues) {
            for (MethodValue methodValue : methodValues.values()) {
                methodSet.set(methodValue.index);
            }
        }

        return methodSet;
    }

    void setMethodOutcomes(Set<Role> roleSet, AuthorizationConstraint ac, UserDataConstraint udc, BitSet methods, BitSet omittedMethods) {

        committed = true;

        if (omittedMethods != null) {

            // Get the omitted methodSet
            BitSet methodsInMap = getMethodSet();

            BitSet saved = (BitSet) omittedMethods.clone();

            // Determine methods being newly omitted
            omittedMethods.andNot(methodsInMap);

            // Create values for newly omitted, init from otherConstraint
            for (int i = omittedMethods.nextSetBit(0); i >= 0; i = omittedMethods.nextSetBit(i + 1)) {
                getMethodValue(i);
            }

            // Combine this constraint into constraint on all other methods
            otherConstraint.setOutcome(roleSet, ac, udc);

            methodsInMap.andNot(saved);

            // Recursive call to combine constraint into prior omitted methods
            setMethodOutcomes(roleSet, ac, udc, methodsInMap, null);

        } else {
            for (int i = methods.nextSetBit(0); i >= 0; i = methods.nextSetBit(i + 1)) {
                // Create values (and init from otherConstraint) if not in map
                // then combine with this constraint.
                getMethodValue(i).setOutcome(roleSet, ac, udc);
            }
        }
    }

    void handleUncovered(boolean deny) {

        // Bypass any uncommitted patterns (e.g. the default pattern) which were entered in the map, but that were not named in
        // a security constraint

        if (!committed) {
            return;
        }

        boolean otherIsUncovered = false;
        synchronized (methodValues) {
            BitSet uncoveredMethodSet = new BitSet();

            // For all the methods in the mapValue
            for (MethodValue methodValue : methodValues.values()) {
                // If the method is uncovered add its id to the uncovered set
                if (methodValue.isUncovered()) {
                    if (deny) {
                        methodValue.setPredefinedOutcome(false);
                    }
                    uncoveredMethodSet.set(methodValue.index);
                }
            }

            // If the constraint on all other methods is uncovered
            if (otherConstraint.isUncovered()) {

                // This is the case where the problem is most severe, since a non-enumerable set of HTTP methods has
                // been left uncovered.
                // The set of method will be logged and denied.

                otherIsUncovered = true;
                if (deny) {
                    otherConstraint.setPredefinedOutcome(false);
                }

                // Ensure that the methods that are reported as uncovered includes any enumerated methods that were found to be
                // uncovered.
                BitSet otherMethodSet = getMethodSet();
                if (!uncoveredMethodSet.isEmpty()) {

                    // UncoveredMethodSet contains methods that otherConstraint pertains to, so remove them from otherMethodSet
                    // which is the set to which the otherConstraint does not apply
                    otherMethodSet.andNot(uncoveredMethodSet);
                }


                // When otherIsUncovered, uncoveredMethodSet contains methods to which otherConstraint does NOT apply
                uncoveredMethodSet = otherMethodSet;
            }

            if (otherIsUncovered || !uncoveredMethodSet.isEmpty()) {
                String uncoveredMethods = MethodValue.getActions(uncoveredMethodSet);
                Object[] args = new Object[] { urlPatternSpec, uncoveredMethods };

                if (deny) {
                    if (otherIsUncovered) {
                        JaccWebConstraintsTranslator.logger.log(INFO, "JACC: For the URL pattern {0}, all but the following methods have been excluded: {1}", args);
                    } else {
                        JaccWebConstraintsTranslator.logger.log(INFO, "JACC: For the URL pattern {0}, the following methods have been excluded: {1}", args);
                    }
                } else {
                    if (otherIsUncovered) {
                        JaccWebConstraintsTranslator.logger.log(WARNING, "JACC: For the URL pattern {0}, all but the following methods were uncovered: {1}", args);
                    } else {
                        JaccWebConstraintsTranslator.logger.log(WARNING, "JACC: For the URL pattern {0}, the following methods were uncovered: {1}", args);
                    }
                }
            }
        }
    }
}
