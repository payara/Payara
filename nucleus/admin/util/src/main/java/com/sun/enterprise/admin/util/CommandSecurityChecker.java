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
package com.sun.enterprise.admin.util;

import java.lang.reflect.Field;
import java.net.URI;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.security.auth.Subject;
import org.glassfish.security.services.api.authorization.AuthorizationService;
import org.glassfish.api.admin.*;
import org.glassfish.api.admin.AccessRequired.AccessCheck;
import org.glassfish.logging.annotation.LogMessagesResourceBundle;
import org.glassfish.logging.annotation.LoggerInfo;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.BaseServiceLocator;
import org.jvnet.hk2.config.ConfigBean;
import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.config.ConfigModel;
import org.jvnet.hk2.config.Dom;

/**
 * Utility class which checks if the Subject is allowed to execute the specified command.
 * <p>
 * The processing includes {@link AccessRequired}} annotations, CRUD commands, 
 * {@code RestEndpoint} annotations, and if the command
 * class implements {@link AccessRequired.Authorizer} it also invokes the
 * corresponding {@code isAuthorized} method.  To succeed the overall authorization
 * all annotations must pass and the {@code isAuthorized} method must return true.
 * 
 * @author tjquinn
 */
@Service
@Singleton
public class CommandSecurityChecker {
    
    @LoggerInfo(subsystem="ADMSEC", description="Admin security ")
    private static final String ADMSEC_LOGGER_NAME = "javax.enterprise.system.tools.admin.security";

    @LogMessagesResourceBundle
    private static final String LOG_MESSAGES_RB = "com.sun.enterprise.admin.util.LogMessages";

    static final Logger ADMSEC_LOGGER = Logger.getLogger(ADMSEC_LOGGER_NAME, LOG_MESSAGES_RB);
    
    private static final Level PROGRESS_LEVEL = Level.FINE;
    private static final String LINE_SEP = System.getProperty("line.separator");
    
    @Inject
    private BaseServiceLocator locator;

// TODO waiting for the correct config and implementation
//    @Inject
//    private AuthorizationService authService;
    
    
    private static final Map<RestEndpoint.OpType,String> optypeToAction = initOptypeMap();
    
    /**
     * Maps RestEndpoint HTTP methods to the corresponding security action.
     * @return 
     */
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
                final URI resourceURI,
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
        final List<AccessCheck> accessChecks = new ArrayList<AccessCheck>();
        
        try {
            if (command instanceof AccessRequired.CommandContextDependent) {
                ((AccessRequired.CommandContextDependent) command).setCommandContext(adminCommandContext);
            }
            if ( ! checkAccessRequired(subject, env, command, accessChecks)) {
                final List<AccessCheck> failedAccessChecks = new ArrayList<AccessCheck>();
                for (AccessCheck a : accessChecks) {
                    if ( ! a.isSuccessful()) {
                        failedAccessChecks.add(a);
                    }
                }
                throw new SecurityException(failedAccessChecks.toString());
            }
        } catch (Exception ex) {
            throw new SecurityException(ex);
        }
    }
    
    private boolean addChecksFromAuthorizer(final Subject subject, 
            final AdminCommand command, final List<AccessCheck> accessChecks,
            final StringBuilder sb) {
        if (command instanceof AccessRequired.Authorizer) {
            final Collection<? extends AccessCheck> checks = ((AccessRequired.Authorizer) command).getAccessChecks();
            accessChecks.addAll(checks);
            if (sb != null && ! checks.isEmpty()) {
                sb.append("  Class's getAccessChecks()").append(LINE_SEP);
                for (AccessCheck a : checks) {
                    sb.append("    ").append(a).append(LINE_SEP);
                }
            }
            return ! checks.isEmpty();
        }
        return false;
    }
    
    private boolean checkAccessRequired(final Subject subject,
            final Map<String,Object> env,
            final AdminCommand command,
            final List<AccessCheck> accessChecks) 
                throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
        
        final StringBuilder sb = (ADMSEC_LOGGER.isLoggable(PROGRESS_LEVEL) ? (new StringBuilder(LINE_SEP)).append("AccessCheck processing...").append(LINE_SEP) : null);
        
        addChecksFromAuthorizer(subject, command, accessChecks, sb);
        addChecksFromExplicitAccessRequiredAnnos(command, accessChecks, sb);
        addChecksFromReSTEndpoints(command, accessChecks, sb);

        boolean result = true;
        for (final AccessCheck a : accessChecks) {
            a.setSuccessful(authService.isAuthorized(subject, resourceURIFromAccessCheck(a), a.action()));
            if (sb != null) {
                sb.append("      isSuccessful -> ").append(a.isSuccessful()).append(LINE_SEP);
            }
            result &= ( (! a.isFailureFinal()) || a.isSuccessful());
        }
        if (sb != null) {
            sb.append(LINE_SEP).append("...final result: ").append(result).append(LINE_SEP);
            ADMSEC_LOGGER.log(PROGRESS_LEVEL, sb.toString());
        }
        return result;
    }
    
    private URI resourceURIFromAccessCheck(final AccessCheck c) {
        return URI.create(resourceNameFromAccessCheck(c));
    }
    
    private String resourceNameFromAccessCheck(final AccessCheck c) {
        String resourceName = c.resourceName();
        if (resourceName == null) {
            resourceName = AccessRequired.Util.resourceNameFromConfigBeanType(c.parent(), c.childType());
        }
        return resourceName;
    }
    
    private boolean addChecksFromExplicitAccessRequiredAnnos(final AdminCommand command,
            final List<AccessCheck> accessChecks,
            final StringBuilder sb) 
            throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
        boolean isAnnotated = false;
                
        for (ClassLineageIterator cIt = new ClassLineageIterator(command.getClass()); cIt.hasNext();) {
            final Class<?> c = cIt.next();
            final AccessRequired ar = c.getAnnotation(AccessRequired.class);
            if (ar != null) {
                isAnnotated = true;
                if (sb != null) {
                    sb.append("  @AccessRequired on").append(c.getName()).append(LINE_SEP);
                }
                addAccessChecksFromAnno(ar, command, accessChecks, sb);
            }
            final AccessRequired.List arList = c.getAnnotation(AccessRequired.List.class);
            if (arList != null) {
                isAnnotated = true;
                if (sb != null) {
                    sb.append("  @AccessRequired on").append(c.getName()).append(LINE_SEP);
                }
                for (final AccessRequired repeatedAR : arList.value()) {
                    addAccessChecksFromAnno(repeatedAR, command, accessChecks, sb);
                }
            }
            isAnnotated |= addAccessChecksFromFields(c, command, accessChecks, sb);
            
        }
        return isAnnotated;
    }
    
    private boolean addAccessChecksFromFields(
            final Class<?> c,
            final AdminCommand command,
            final List<AccessCheck> accessChecks,
            final StringBuilder sb) throws IllegalArgumentException, IllegalAccessException {
        boolean isAnnotatedOnFields = false;
        for (Field f : c.getDeclaredFields()) {
            isAnnotatedOnFields |= addAccessChecksFromAnno(f, command, accessChecks, sb);
        }
        return isAnnotatedOnFields;
    }
    
    private void addAccessChecksFromAnno(final AccessRequired ar, final AdminCommand command,
            final List<AccessCheck> accessChecks, final StringBuilder sb) 
            throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
        for (final String resource : ar.resource()) {
            final String translatedResource = processTokens(resource, command);
            for (final String action : ar.action()) {
                final AccessCheck a = new AccessCheck(translatedResource, action);
                accessChecks.add(a);
                if (sb != null) {
                    sb.append("  @AccessRequired on class")
                            .append(command.getClass().getName())
                            .append(a)
                            .append(LINE_SEP);
                }
            }
        }
    }
    
    private boolean addAccessChecksFromAnno(final Field f, final AdminCommand command,
            final List<AccessCheck> accessChecks, final StringBuilder sb) throws IllegalArgumentException, IllegalAccessException {
        boolean isAnnotated = false;
        final AccessRequired.To arTo = f.getAnnotation(AccessRequired.To.class);
        if (arTo != null) {
            isAnnotated = true;
            final String resourceNameForField = resourceNameFromField(f, command);
            for (final String access : arTo.value()) {
                final AccessCheck a = new AccessCheck(resourceNameForField, access);
                accessChecks.add(a);
                if (sb != null) {
                    sb.append("  @AccessRequired.To on field ")
                            .append(f.getDeclaringClass().getName())
                            .append("#")
                            .append(f.getName())
                            .append(LINE_SEP)
                            .append("    ")
                            .append(a)
                            .append(LINE_SEP);
                }
            }
        }
        final AccessRequired.NewChild arNC = f.getAnnotation(AccessRequired.NewChild.class);
        if (arNC != null) {
            isAnnotated = true;
            String resourceNameForField = null;
            if (ConfigBeanProxy.class.isAssignableFrom(arNC.type())) {
                isAnnotated = true;
                resourceNameForField = resourceNameFromConfigBeanType(arNC.type(), locator);
            }
            if (resourceNameForField != null) {
                for (final String action : arNC.action()) {
                    final AccessCheck a = new AccessCheck(resourceNameForField, action);
                    accessChecks.add(a);
                    if (sb != null) {
                        sb.append("  @AccessRequired.NewChild on field ")
                                .append(f.getDeclaringClass().getName())
                                .append("#")
                                .append(f.getName())
                                .append(LINE_SEP)
                                .append("    ")
                                .append(a)
                                .append(LINE_SEP);
                    }
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
            return AccessRequired.Util.resourceNameFromConfigBeanProxy((ConfigBeanProxy) f.get(command));
        } else if (ConfigBean.class.isAssignableFrom(f.getType())) {
            return AccessRequired.Util.resourceNameFromDom((ConfigBean) f.get(command));
        }
        return f.get(command).toString();
    }
    
    private void addChecksFromReSTEndpoints(final AdminCommand command, 
            final List<AccessCheck> accessChecks,
            final StringBuilder sb) {
        for (ClassLineageIterator cIt = new ClassLineageIterator(command.getClass()); cIt.hasNext();) {
            final Class<?> c = cIt.next();
            final RestEndpoint restEndpoint;
            if ((restEndpoint = c.getAnnotation(RestEndpoint.class)) != null) {
                addAccessChecksFromReSTEndpoint(restEndpoint, accessChecks, sb);
            }
            final RestEndpoints restEndpoints = c.getAnnotation(RestEndpoints.class);
            if (restEndpoints != null) {
                for (RestEndpoint re : restEndpoints.value()) {
                    addAccessChecksFromReSTEndpoint(re, accessChecks, sb);
                }
            }
        }
    }
    
    private void addAccessChecksFromReSTEndpoint(
            final RestEndpoint restEndpoint, final List<AccessCheck> accessChecks,
            final StringBuilder sb) {
        final String action = optypeToAction.get(restEndpoint.opType());
        /*
         * For the moment, if there is no RestParam then the config bean class given
         * in the anno is an unnamed singleton target of the action.  If there is a RestParam
         * then the target is (probably) the unnamed singleton parent of the config bean class given
         * and the new child's type is the config bean class.
         */
        String resource;
        if (restEndpoint.params().length == 0) {
            resource = resourceNameFromConfigBeanType(restEndpoint.configBean(), locator);
        } else {
            // TODO need to do something with the endpoint params
            resource = resourceNameFromConfigBeanType(restEndpoint.configBean(), locator);
        }
        final AccessCheck a = new AccessCheck(resource, action);
        accessChecks.add(a);
        if (sb != null) {
            sb.append("  From @RestEndpoint ")
                    .append(restEndpoint.configBean().getName())
                    .append(", op=")
                    .append(restEndpoint.opType())
                    .append(LINE_SEP)
                    .append("    ")
                    .append(a)
                    .append(LINE_SEP);
        }
    }
    
    private static String resourceNameFromConfigBeanType(Class<? extends ConfigBeanProxy> c, final BaseServiceLocator locator) {
        ConfigBeanProxy b = locator.getComponent(c);
        return (b != null ? AccessRequired.Util.resourceNameFromConfigBeanProxy(b) : "?");
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
