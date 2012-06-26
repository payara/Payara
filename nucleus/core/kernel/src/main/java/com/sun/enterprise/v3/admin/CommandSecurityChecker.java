/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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
package com.sun.enterprise.v3.admin;

import java.lang.reflect.Field;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;
import javax.security.auth.Subject;
import org.glassfish.api.admin.*;
import org.glassfish.api.admin.AccessRequired.AccessCheck;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.BaseServiceLocator;
import org.jvnet.hk2.config.ConfigBean;
import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.config.ConfigModel;
import org.jvnet.hk2.config.Dom;

/**
 * Utility class which checks if the Subject is allowed to execute the specified command.
 * <p>
 * The processing includes {@link AccessRequired}} annotations, and if the command
 * class implements {@link AccessRequired.Authorizer} it also invokes the
 * corresponding {@code isAuthorized} method.  To succeed the overall authorization
 * all annotations must pass and the {@code isAuthorized} method must return true.
 * 
 * @author tjquinn
 */
@Service
public class CommandSecurityChecker {
    
    @Inject
    private BaseServiceLocator locator;
    
    
    private static final Map<RestEndpoint.OpType,String> optypeToAction = initOptypeMap();
    
    private static EnumMap<RestEndpoint.OpType,String> initOptypeMap() {
        final EnumMap<RestEndpoint.OpType,String> result = new EnumMap(RestEndpoint.OpType.class);
        result.put(RestEndpoint.OpType.DELETE, "delete");
        result.put(RestEndpoint.OpType.GET, "read");
        result.put(RestEndpoint.OpType.POST, "write");
        result.put(RestEndpoint.OpType.PUT, "insert");
        return result;
    }
    
    // pattern for recognizing token expressions: ${tokenName}
    private final static Pattern TOKEN_PATTERN = Pattern.compile("\\$\\{[^}]*\\}");
    
    /**
     * Temporary stub implementation of the authentication service.
     */
    private final static class AuthService {
        private boolean isAuthorized(final Subject subject, 
                final String resourceName,
                final String action) {
            return true;
        }
    }
    
    private final static AuthService authService = new AuthService();
    
    /**
     * Reports whether the Subject is allowed to perform the specified admin command.
     * @param subject Subject for the current user to authorize
     * @param env environmental settings that might be used in the resource name expression
     * @param command the admin command the Subject wants to execute
     * @return 
     */
    public void authorize(final Subject subject,
            final Map<String,Object> env,
            final AdminCommand command,
            final Object adminCommandContext) throws SecurityException {
        
        final List<AccessCheck> failedAccessChecks = new ArrayList<AccessCheck>();
        
        try {
            if (command instanceof AccessRequired.CommandContextDependent) {
                ((AccessRequired.CommandContextDependent) command).setCommandContext(adminCommandContext);
            }
            if ( ! checkAccessRequired(subject, env, command, failedAccessChecks)) {
                throw new SecurityException(failedAccessChecks.toString());
            }
        } catch (Exception ex) {
            throw new SecurityException(ex);
        }
    }
    
    private boolean addChecksFromAuthorizer(final Subject subject, 
            final AdminCommand command, final List<AccessCheck> accessChecks) {
        if (command instanceof AccessRequired.Authorizer) {
            accessChecks.addAll(((AccessRequired.Authorizer) command).getAccessChecks());
            return true;
        }
        return false;
    }
    
    private boolean checkAccessRequired(final Subject subject,
            final Map<String,Object> env,
            final AdminCommand command,
            final List<AccessCheck> failedAccessChecks) 
                throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
        final List<AccessCheck> accessChecks = new ArrayList<AccessCheck>();
        
        addChecksFromAuthorizer(subject, command, accessChecks);
        final boolean isAnnotated = addChecksFromExplicitAccessRequiredAnnos(command, accessChecks);
        /*
         * If the developer explicitly specified @AccessRequired annos, don't
         * try to process implied ReST ones.
         */
        if ( ! isAnnotated) {
            addAccessChecksFromReSTEndpoints(command, accessChecks);
        }
        boolean result = true;
        for (AccessCheck a : accessChecks) {
            final boolean thisResult = authService.isAuthorized(subject, resourceNameFromAccessCheck(a), a.action());
            if ( ! thisResult) {
                failedAccessChecks.add(a);
            }
            result &= thisResult;
        }
        return result;
    }
    
    private String resourceNameFromAccessCheck(final AccessCheck c) {
        String resourceName = c.resource();
        if (resourceName == null) {
            resourceName = resourceNameFromConfigBeanType(c.parent(), c.childType());
        }
        return resourceName;
    }
    
    private boolean addChecksFromExplicitAccessRequiredAnnos(final AdminCommand command,
            final List<AccessCheck> accessChecks) 
            throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
        boolean isAnnotated = false;
        for (ClassLineageIterator cIt = new ClassLineageIterator(command.getClass()); cIt.hasNext();) {
            final Class<?> c = cIt.next();
            final AccessRequired ar = c.getAnnotation(AccessRequired.class);
            if (ar != null) {
                isAnnotated = true;
                addAccessChecksFromAnno(ar, command, accessChecks);
            }
            final AccessRequired.List arList = c.getAnnotation(AccessRequired.List.class);
            if (arList != null) {
                isAnnotated = true;
                for (final AccessRequired repeatedAR : arList.value()) {
                    addAccessChecksFromAnno(repeatedAR, command, accessChecks);
                }
            }
            isAnnotated |= addAccessChecksFromFields(c, command, accessChecks);
            
        }
        return isAnnotated;
    }
    
    private boolean addAccessChecksFromFields(
            final Class<?> c,
            final AdminCommand command,
            final List<AccessCheck> accessChecks) throws IllegalArgumentException, IllegalAccessException {
        boolean isAnnotatedOnFields = false;
        for (Field f : c.getDeclaredFields()) {
            isAnnotatedOnFields |= addAccessChecksFromAnno(f, command, accessChecks);
        }
        return isAnnotatedOnFields;
    }
    
    private void addAccessChecksFromAnno(final AccessRequired ar, final AdminCommand command,
            final List<AccessCheck> accessChecks) 
            throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
        for (final String resource : ar.resource()) {
            final String translatedResource = processTokens(resource, command);
            for (final String action : ar.action()) {
                accessChecks.add(new AccessCheck(translatedResource, action));
            }
        }
    }
    
    private boolean addAccessChecksFromAnno(final Field f, final AdminCommand command,
            final List<AccessCheck> accessChecks) throws IllegalArgumentException, IllegalAccessException {
        boolean isAnnotated = false;
        final AccessRequired.To arTo = f.getAnnotation(AccessRequired.To.class);
        if (arTo != null) {
            isAnnotated = true;
            final String resourceNameForField = resourceNameFromField(f, command);
            for (final String access : arTo.value()) {
                accessChecks.add(new AccessCheck(resourceNameForField, access));
            }
        }
        final AccessRequired.NewChild arNC = f.getAnnotation(AccessRequired.NewChild.class);
        if (arNC != null) {
            isAnnotated = true;
            String resourceNameForField = null;
            if (ConfigBeanProxy.class.isAssignableFrom(arNC.type())) {
                isAnnotated = true;
                resourceNameForField = resourceNameFromConfigBeanType(arNC.type());
            }
            if (resourceNameForField != null) {
                for (final String action : arNC.action()) {
                    accessChecks.add(new AccessCheck(resourceNameForField, action));
                }
            }
        }
        return isAnnotated;
    }
    
    private String processTokens(final String expr, final AdminCommand command) throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
        
        final Matcher m = TOKEN_PATTERN.matcher(expr);
        if ( ! m.matches()) {
            return expr;
        }
        final StringBuffer translated = new StringBuffer();
        while (m.find()) {
            final String token = m.group(1);
            String replacementValue = token; // in case we can't find or process the field
            final Field f = findField(command, token);
            if (f != null) {
                f.setAccessible(true);
                replacementValue = resourceNameFromField(f, command);
            }
            m.appendReplacement(translated, replacementValue);
        }
        m.appendTail(translated);
        return translated.toString();
    }
    
    private Field findField(final AdminCommand command, final String fieldName) throws NoSuchFieldException {
        Field result = null;
        for (Class c = command.getClass(); c != null && result == null; c = c.getSuperclass()) {
            result = c.getDeclaredField(fieldName);
        }
        return result;
    }
    
    private String resourceNameFromField(final Field f, final AdminCommand command) 
            throws IllegalArgumentException, IllegalAccessException {
        f.setAccessible(true);
        if (ConfigBeanProxy.class.isAssignableFrom(f.getType())) {
            return resourceNameFromConfigBeanProxy((ConfigBeanProxy) f.get(command));
        } else if (ConfigBean.class.isAssignableFrom(f.getType())) {
            return resourceNameFromDom((ConfigBean) f.get(command));
        }
        return f.get(command).toString();
    }
    
    private String resourceNameFromConfigBeanProxy(ConfigBeanProxy b) {
        return resourceNameFromDom(Dom.unwrap(b));
    }
        
    private String resourceNameFromDom(Dom d) {
        
        final StringBuilder path = new StringBuilder();
        while (d != null) {
            if (path.length() > 0) {
                path.insert(0, '/');
            }
            final ConfigModel m = d.model;
            final String key = d.getKey();
            final String pathSegment = m.getTagName() + (key == null ? "" : "/" + key);
            path.insert(0, pathSegment);
            d = d.parent();
        }
        return path.toString();
    }
    
    private String resourceNameFromConfigBeanType(Class<? extends ConfigBeanProxy> c) {
        ConfigBeanProxy b = locator.getComponent(c);
        return (b != null ? resourceNameFromConfigBeanProxy(b) : "?");
    }
    
    private String resourceNameFromConfigBeanType(final ConfigBeanProxy parent, final Class<? extends ConfigBeanProxy> childType) {
        final Dom dom = Dom.unwrap(parent);
        return resourceNameFromDom(dom) + "/" + dom.document.buildModel(childType).getTagName();
    }
    
    private void addAccessChecksFromReSTEndpoints(final AdminCommand command, 
            final List<AccessCheck> accessChecks) {
        for (ClassLineageIterator cIt = new ClassLineageIterator(command.getClass()); cIt.hasNext();) {
            final Class<?> c = cIt.next();
            final RestEndpoint restEndpoint;
            if ((restEndpoint = c.getAnnotation(RestEndpoint.class)) != null) {
                addAccessChecksFromReSTEndpoint(restEndpoint, accessChecks);
            }
            final RestEndpoints restEndpoints = c.getAnnotation(RestEndpoints.class);
            if (restEndpoints != null) {
                for (RestEndpoint re : restEndpoints.value()) {
                    addAccessChecksFromReSTEndpoint(re, accessChecks);
                }
            }
        }
    }
    
    private void addAccessChecksFromReSTEndpoint(
            final RestEndpoint restEndpoint, final List<AccessCheck> accessChecks) {
        final String action = optypeToAction.get(restEndpoint.opType());
        /*
         * For the moment, if there is no RestParam then the config bean class given
         * in the anno is an unnamed singleton target of the action.  If there is a RestParam
         * then the target is (probably) the unnamed singleton parent of the config bean class given
         * and the new child's type is the config bean class.
         */
        String resource;
        if (restEndpoint.params().length == 0) {
            resource = resourceNameFromConfigBeanType(restEndpoint.configBean());
        } else {
            // TODO need to do something with the endpoint params
            resource = resourceNameFromConfigBeanType(restEndpoint.configBean());
        }
        accessChecks.add(new AccessCheck(resource, action));
    }
    
    private String resourceKeyFromReSTEndpoint(final RestEndpoint restEndpoint) {
        for (RestParam p : restEndpoint.params()) {

        }
        return "";
    }
    
    /**
     * Iterates through the initially specified class and its ancestors.
     */
    private static class ClassLineageIterator implements Iterator<Class> {

        private Class c;
        
        private ClassLineageIterator(final Class c) {
            this.c = c;
        }
        
        @Override
        public boolean hasNext() {
            return c != null;
        }

        @Override
        public Class<?> next() {
            final Class result = c;
            c = c.getSuperclass();
            return result;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
    
}
