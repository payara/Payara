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

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;
import javax.security.auth.Subject;
import org.glassfish.api.admin.AccessRequired;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.RestEndpoint;
import org.glassfish.api.admin.RestEndpoints;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.BaseServiceLocator;
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
            final AdminCommand command) throws SecurityException {
        
        final List<AccessCheck> failedAccessChecks = new ArrayList<AccessCheck>();
        
        try {
            if ( ! (invokeIsAuthorizedOnCommand(subject, env, command, failedAccessChecks)
                    && checkAccessRequired(subject, env, command, failedAccessChecks))) {
                throw new SecurityException(failedAccessChecks.toString());
            }
        } catch (Exception ex) {
            throw new SecurityException(ex);
        }
    }
    
    private boolean invokeIsAuthorizedOnCommand(final Subject subject, final Map<String,Object> env, 
            final AdminCommand command, final List<AccessCheck> failedAccessChecks) {
        if (command instanceof AccessRequired.Authorizer) {
            final boolean result = ((AccessRequired.Authorizer) command).isAuthorized(subject, env);
            if (! result) {
                failedAccessChecks.add(new AccessCheck("Authorizer.isAuthorized","invocation"));
                return false;
            }
        }
        return true;
    }
    
    private boolean checkAccessRequired(final Subject subject,
            final Map<String,Object> env,
            final AdminCommand command,
            final List<AccessCheck> failedAccessChecks) 
                throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
        final List<AccessCheck> accessChecks = new ArrayList<AccessCheck>();
        
        addChecksFromExplicitAccessRequiredAnnos(command, accessChecks);
        /*
         * If the developer explicitly specified @AccessRequired annos, don't
         * try to process implied ReST ones.
         */
        if (accessChecks.isEmpty()) {
            addAccessChecksFromReSTEndpoints(command, accessChecks);
        }
//        addCRUDAccesses(command, accessChecks);
        
        boolean result = true;
        for (AccessCheck a : accessChecks) {
            final boolean thisResult = authService.isAuthorized(subject, a.resourceName, a.action);
            if ( ! thisResult) {
                failedAccessChecks.add(a);
            }
            result &= thisResult;
        }
        return result;
    }
    
    private void addChecksFromExplicitAccessRequiredAnnos(final AdminCommand command,
            final List<AccessCheck> accessChecks) 
            throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
        for (ClassLineageIterator cIt = new ClassLineageIterator(command.getClass()); cIt.hasNext();) {
            final Class<?> c = cIt.next();
            final AccessRequired ar = c.getAnnotation(AccessRequired.class);
            if (ar != null) {
                addAccessChecksFromAnno(ar, command, accessChecks);
            }
            final AccessRequired.List arList = c.getAnnotation(AccessRequired.List.class);
            if (arList != null) {
                for (final AccessRequired repeatedAR : arList.value()) {
                    addAccessChecksFromAnno(repeatedAR, command, accessChecks);
                }
            }
            addAccessChecksFromFields(c, command, accessChecks);            
        }
    }
    
    private void addAccessChecksFromFields(
            final Class<?> c,
            final AdminCommand command,
            final List<AccessCheck> accessChecks) throws IllegalArgumentException, IllegalAccessException {
        for (Field f : c.getDeclaredFields()) {
            addAccessChecksFromAnno(f, command, accessChecks);
        }
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
    
    private void addAccessChecksFromAnno(final Field f, final AdminCommand command,
            final List<AccessCheck> accessChecks) throws IllegalArgumentException, IllegalAccessException {
        final AccessRequired.To arTo = f.getAnnotation(AccessRequired.To.class);
        if (arTo != null) {
            final String resourceNameForField = resourceNameFromField(f, command);
            for (final String access : arTo.value()) {
                accessChecks.add(new AccessCheck(resourceNameForField, access));
            }
        }
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
        if (ConfigBeanProxy.class.isAssignableFrom(f.getDeclaringClass())) {
            return resourceNameFromConfigBean((ConfigBeanProxy) f.get(command));
        }
        return f.get(command).toString();
    }
    
    private String resourceNameFromConfigBean(ConfigBeanProxy b) {
        return resourceNameFromConfigBean(Dom.unwrap(b));
    }
        
    private String resourceNameFromConfigBean(Dom d) {
        
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
    
    private String resourceNameFromConfigBean(Class<? extends ConfigBeanProxy> c) {
        ConfigBeanProxy b = locator.getComponent(c);
        return resourceNameFromConfigBean(b);
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
        final String resource = resourceNameFromConfigBean(restEndpoint.configBean());
        accessChecks.add(new AccessCheck(resource, action));
    }
    
    private static class AccessCheck {
        private final String resourceName;
        private final String action;
        private String note = "";
        
        private AccessCheck(final String resourceName, final String action, final String note) {
            this.resourceName = resourceName;
            this.action = action;
            this.note = note;
        }
        
        private AccessCheck(final String resourceName, final String action) {
            this(resourceName, action, "");
        }

        @Override
        public String toString() {
            return (new StringBuilder("AccessCheck ")).
                    append(resourceName).
                    append("=").
                    append(action).
                    append("//").
                    append(note).
                    toString();
        }
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
