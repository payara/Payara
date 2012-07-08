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
import org.glassfish.api.admin.*;
import org.glassfish.api.admin.AccessRequired.AccessCheck;
import org.glassfish.logging.annotation.LogMessagesResourceBundle;
import org.glassfish.logging.annotation.LoggerInfo;
import org.glassfish.security.services.api.authorization.AuthorizationService;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.BaseServiceLocator;
import org.jvnet.hk2.config.ConfigBean;
import org.jvnet.hk2.config.ConfigBeanProxy;

/**
 * Utility class which checks if the Subject is allowed to execute the specified command.
 * <p>
 * The processing includes {@link AccessRequired}} annotations, CRUD commands, 
 * {@code RestEndpoint} annotations, and if the command
 * class implements {@link AdminCommandSecurity.AccessCheckProvider} it also invokes the
 * corresponding {@code getAccessChecks} method.  To succeed the overall authorization
 * all access checks - whether inferred from annotations or returned from 
 * {@code getAccessChecks} must pass.
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

    @Inject
    private AuthorizationService authService;
    
    @Inject
    private NamedResourceManager namedResourceMgr;
    
    
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
    
    /*
     * Group 1 contains the token from $token; group 2 contains the token from ${token}
     */
    private final static Pattern TOKEN_PATTERN = Pattern.compile("(?:\\$(\\w+))|(?:\\$\\{(\\w+)\\})");
    
    /**
     * Reports whether the Subject is allowed to perform the specified admin command.
     * @param subject Subject for the current user to authorize
     * @param env environmental settings that might be used in the resource name expression
     * @param command the admin command the Subject wants to execute
     * @return 
     */
    public boolean authorize(final Subject subject,
            final Map<String,Object> env,
            final AdminCommand command,
            final AdminCommandContext adminCommandContext) throws SecurityException {
        final List<AccessCheckWork> accessChecks = new ArrayList<AccessCheckWork>();
        
        boolean result;
        try {
            if (command instanceof AdminCommandSecurity.Preauthorization) {
                if ( ! ((AdminCommandSecurity.Preauthorization) command).preAuthorization(adminCommandContext)) {
                    return false;
                }
            }
            result = checkAccessRequired(subject, env, command, accessChecks);
        } catch (Exception ex) {
            ADMSEC_LOGGER.log(Level.SEVERE, command.getClass().getName(), ex);
            throw new SecurityException(ex);
        }
        if ( ! result) {
            final List<AccessCheck> failedAccessChecks = new ArrayList<AccessCheck>();
            for (AccessCheckWork acWork : accessChecks) {
                if ( ! acWork.accessCheck.isSuccessful()) {
                    failedAccessChecks.add(acWork.accessCheck);
                }
            }
            throw new SecurityException();
        }
        return result;
    }
    
    private boolean checkAccessRequired(final Subject subject,
            final Map<String,Object> env,
            final AdminCommand command,
            final List<AccessCheckWork> accessChecks) 
                throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
        
        final boolean isTaggable = ADMSEC_LOGGER.isLoggable(PROGRESS_LEVEL);
        /*
         * The CRUD classes such as GenericCreateCommand implement AccessRequired.Authorizer
         * and so provide their own AccessCheck objects.  So the addChecksFromAuthorizer
         * method will cover the CRUD commands.
         */
        addChecksFromAccessCheckProvider(subject, command, accessChecks, isTaggable);
        addChecksFromExplicitAccessRequiredAnnos(command, accessChecks, isTaggable);
        addChecksFromReSTEndpoints(command, accessChecks, isTaggable);

        /*
         * If this command has no access requirements specified, use a
         * default one requiring write access on the domain.
         */
        if (accessChecks.isEmpty()) {
            accessChecks.add(new BlanketAccessCheckWork(command));
        }
        boolean result = true;
        final StringBuilder sb = (isTaggable ? (new StringBuilder(LINE_SEP)).append("AccessCheck processing on ").append(command.getClass().getName()).append(LINE_SEP) : null);
        for (final AccessCheckWork a : accessChecks) {
            a.accessCheck.setSuccessful(authService.isAuthorized(subject, resourceURIFromAccessCheck(a.accessCheck), a.accessCheck.action()));
            if (isTaggable) {
                sb.append(a.tag).append(LINE_SEP).
                   append("    ").append(a.accessCheck).append(LINE_SEP);
            }
            result &= ( (! a.accessCheck.isFailureFinal()) || a.accessCheck.isSuccessful());
        }
        if (isTaggable) {
            sb.append(LINE_SEP).append("...final result: ").append(result).append(LINE_SEP);
            ADMSEC_LOGGER.log(PROGRESS_LEVEL, sb.toString());
        }
        return result;
    }

    /**
     * Adds access checks from the command's explicit getAccessChecks method.
     * @param subject
     * @param command
     * @param accessChecks
     * @param isTaggable
     * @return 
     */
    private boolean addChecksFromAccessCheckProvider(final Subject subject, 
            final AdminCommand command, final List<AccessCheckWork> accessChecks,
            final boolean isTaggable) {
        if (command instanceof AdminCommandSecurity.AccessCheckProvider) {
            final Collection<? extends AccessCheck> checks = ((AdminCommandSecurity.AccessCheckProvider) command).getAccessChecks();
            for (AccessCheck ac : checks) {
                
                accessChecks.add(new AccessCheckWork(ac,
                        isTaggable ? "  Class's getAccessChecks()"  : null));
            }
            return ! checks.isEmpty();
        }
        return false;
    }
    
    private URI resourceURIFromAccessCheck(final AccessCheck c) {
        return URI.create(resourceNameFromAccessCheck(c));
    }
    
    private String resourceNameFromAccessCheck(final AccessCheck c) {
        String resourceName = c.resourceName();
        if (resourceName == null) {
            resourceName = AccessRequired.Util.resourceNameFromConfigBeanType(c.parent(), null, c.childType());
        }
        return resourceName;
    }
    
    private boolean addChecksFromExplicitAccessRequiredAnnos(final AdminCommand command,
            final List<AccessCheckWork> accessChecks,
            final boolean isTaggable) 
            throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
        boolean isAnnotated = false;
                
        for (ClassLineageIterator cIt = new ClassLineageIterator(command.getClass()); cIt.hasNext();) {
            final Class<?> c = cIt.next();
            final AccessRequired ar = c.getAnnotation(AccessRequired.class);
            if (ar != null) {
                isAnnotated = true;
                addAccessChecksFromAnno(ar, command, accessChecks, c, isTaggable);
            }
            final AccessRequired.List arList = c.getAnnotation(AccessRequired.List.class);
            if (arList != null) {
                isAnnotated = true;
                
                for (final AccessRequired repeatedAR : arList.value()) {
                    addAccessChecksFromAnno(repeatedAR, command, accessChecks, c, isTaggable);
                }
            }
            
            /*
             * Process any AccessRequired.Typed annotations.
             */
            final AccessRequired.Typed arTyped = c.getAnnotation(AccessRequired.Typed.class);
            if (arTyped != null) {
                isAnnotated = true;
                addAccessChecksFromAnno(arTyped, command, accessChecks, c, isTaggable);
            }
            
            final AccessRequired.Typed.List arTypedList = c.getAnnotation(AccessRequired.Typed.List.class);
            if (arTypedList != null) {
                for (AccessRequired.Typed arT : arTypedList.value()) {
                    isAnnotated = true;
                    addAccessChecksFromAnno(arT, command, accessChecks, c, isTaggable);
                }
            }
            
            isAnnotated |= addAccessChecksFromFields(c, command, accessChecks, isTaggable);
            
        }
        return isAnnotated;
    }
    
    private void addAccessChecksFromAnno(final AccessRequired.Typed arTyped, 
            final AdminCommand command,
            final List<AccessCheckWork> accessChecks,
            final Class<?> c,
            final boolean isTaggable) {
//        final List<String> keyValues = new ArrayList<String>();
//        for (String key : arTyped.key()) {
//            
//        }
    }
    private boolean addAccessChecksFromFields(
            final Class<?> c,
            final AdminCommand command,
            final List<AccessCheckWork> accessChecks,
            final boolean isTaggable) throws IllegalArgumentException, IllegalAccessException {
        boolean isAnnotatedOnFields = false;
        for (Field f : c.getDeclaredFields()) {
            isAnnotatedOnFields |= addAccessChecksFromAnno(f, command, accessChecks, isTaggable);
        }
        return isAnnotatedOnFields;
    }
    
    private void addAccessChecksFromAnno(final AccessRequired ar, final AdminCommand command,
            final List<AccessCheckWork> accessChecks, final Class<?> currentClass, final boolean isTaggable) 
            throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
        for (final String resource : ar.resource()) {
            final String translatedResource = processTokens(resource, command);
            for (final String action : ar.action()) {
                final AccessCheck a = new AccessCheck(translatedResource, action);
                String tag = null;
                if (isTaggable) {
                    tag = "  @AccessRequired on " + currentClass.getName() + LINE_SEP;
                }
                accessChecks.add(new AccessCheckWork(a, tag));
            }
        }
    }
    
    private boolean addAccessChecksFromAnno(final Field f, final AdminCommand command,
            final List<AccessCheckWork> accessChecks, final boolean isTaggable) throws IllegalArgumentException, IllegalAccessException {
        boolean isAnnotated = false;
        f.setAccessible(true);
        final AccessRequired.To arTo = f.getAnnotation(AccessRequired.To.class);
        if (arTo != null) {
            isAnnotated = true;
            final String resourceNameForField = resourceNameFromField(f, command);
            for (final String access : arTo.value()) {
                final AccessCheck a = new AccessCheck(resourceNameForField, access);
                String tag = null;
                if (isTaggable) {
                    tag = "  @AccessRequired.To on field " + f.getDeclaringClass().getName() + "#" + f.getName();
                }
                accessChecks.add(new AccessCheckWork(a, tag));
            }
        }
        final AccessRequired.NewChild arNC = f.getAnnotation(AccessRequired.NewChild.class);
        if (arNC != null) {
            isAnnotated = true;
            String resourceNameForField = resourceNameFromNewChildAnno(arNC, f, command);
            /*
             * We have the resource name for the parent.  Compute the rest of
             * the resource name using the explicit collection name in the
             * anno or the inferred name from the child type.
             */
            
            for (final String action : arNC.action()) {
                final AccessCheck a = new AccessCheck(resourceNameForField, action);
                String tag = null;
                if (isTaggable) {
                    tag = "  @AccessRequired.NewChild on field " + f.getDeclaringClass().getName() + "#" + f.getName();
                }
                accessChecks.add(new AccessCheckWork(a, tag));
            }
        }
        return isAnnotated;
    }
    
    private String resourceNameFromNewChildAnno(final AccessRequired.NewChild arNC, final Field f, final AdminCommand command) throws IllegalArgumentException, IllegalAccessException {
        /*
         * The config beans convention - although not a requirement - is that
         * an owner bean which has a collection of sub-beans has a single
         * child named for the plural of the subtype, and then that single 
         * child actually holds the collection.  For example, the domain 
         * can contain multiple resources, each of which can be of a different
         * subtype of resource.  This is modeled with the Domain having
         * 
         *  Resources getResources(); 
         * 
         * (this returns a single Resources object) and 
         * the Resources object has 
         * 
         *  List<*> getResources();  // plural
         * 
         * Note that the name for the method on the single container object is not
         * consistent.  For example, Domain also has 
         * 
         *  Servers getServers();
         * 
         * and then the Servers bean has
         * 
         *  List<*> getServer();  // singular
         * 
         * But in either case, the config path to an actual child is
         * 
         *  parent/collectionName/childType/childID
         * 
         * or in these examples,
         * 
         *  domain/resources/javamail-resource/MyIMAPMail
         *  domain/servers/server/MyInstance
         * 
         * The AccessRequired.NewChild annotation requires the developer to provide
         * the type of the child to be created and the name of the collection
         * in which it is to be stored.  (Maybe in the future we can be
         * smarter about inferring one from the other.)
         */
        final StringBuilder sb = new StringBuilder();
        final Object parent = f.get(command);
        final Class<?> childType = arNC.type();
        if ( ! ConfigBeanProxy.class.isAssignableFrom(childType)) {
            throw new SecurityException(Strings.get("secure.admin.childNotConfigBeanProxy", childType.getName()));
        }
        if (ConfigBeanProxy.class.isAssignableFrom(parent.getClass())) {
            sb.append(AccessRequired.Util.resourceNameFromConfigBeanType(
                    (ConfigBeanProxy) parent, 
                    arNC.collection(),
                    (Class<? extends ConfigBeanProxy>) childType));
        } else if (ConfigBean.class.isAssignableFrom(parent.getClass())) {
            sb.append(AccessRequired.Util.resourceNameFromConfigBeanType(
                    (ConfigBean) parent, 
                    arNC.collection(),
                    (Class<? extends ConfigBeanProxy>) childType));
        }

        return sb.toString();
    }
    
    private boolean addAccessChecksFromAnno(final AccessRequired.Typed arTyped,
            final AdminCommand command,
            final List<AccessCheckWork> accessChecks,
            final boolean isTaggable) {
//        final Class<?> type = arTyped.type();
        return false; // not yet anyway
    }
    
    private String processTokens(final String expr, final AdminCommand command) throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
        
        final Matcher m = TOKEN_PATTERN.matcher(expr);
        final StringBuffer translated = new StringBuffer();
        while (m.find()) {
            final String token = (m.group(1) != null ? m.group(1) : m.group(2));
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
        } else {
            final String savedResourceName = namedResourceMgr.find(f.get(command));
            if (savedResourceName != null) {
                return savedResourceName;
            }
        }
        return f.get(command).toString();
    }
    
    private void addChecksFromReSTEndpoints(final AdminCommand command, 
            final List<AccessCheckWork> accessChecks,
            final boolean isTaggable) {
        for (ClassLineageIterator cIt = new ClassLineageIterator(command.getClass()); cIt.hasNext();) {
            final Class<?> c = cIt.next();
            final RestEndpoint restEndpoint;
            if ((restEndpoint = c.getAnnotation(RestEndpoint.class)) != null) {
                addAccessChecksFromReSTEndpoint(restEndpoint, accessChecks, isTaggable);
            }
            final RestEndpoints restEndpoints = c.getAnnotation(RestEndpoints.class);
            if (restEndpoints != null) {
                for (RestEndpoint re : restEndpoints.value()) {
                    addAccessChecksFromReSTEndpoint(re, accessChecks, isTaggable);
                }
            }
        }
    }
    
    private void addAccessChecksFromReSTEndpoint(
            final RestEndpoint restEndpoint, final List<AccessCheckWork> accessChecks,
            final boolean isTaggable) {
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
        String tag = null;
        if (isTaggable) {
            tag = "  @RestEndpoint " + restEndpoint.configBean().getName() + ", op=" + restEndpoint.opType();
        }
        accessChecks.add(new AccessCheckWork(a, tag));
    }
    
    private static String resourceNameFromConfigBeanType(Class<? extends ConfigBeanProxy> c, final BaseServiceLocator locator) {
        ConfigBeanProxy b = locator.getComponent(c);
        return (b != null ? AccessRequired.Util.resourceNameFromConfigBeanProxy(b) : "?");
    }
        
        
//    private String resourceKeyFromReSTEndpoint(final RestEndpoint restEndpoint) {
//        for (RestParam p : restEndpoint.params()) {
//
//        }
//        return "";
//    }
    
    private static class AccessCheckWork {
        private final AccessCheck accessCheck;
        private final String tag;
        
        private AccessCheckWork(final AccessCheck accessCheck, final String tag) {
            this.accessCheck = accessCheck;
            this.tag = tag;
        }
    }
    
    private static class BlanketAccessCheckWork extends AccessCheckWork {
        private BlanketAccessCheckWork(final AdminCommand c) {
            super(new AccessCheck("domain", "write"),"  Blanket access control on " + c.getClass().getName());
        }
    }
    
}
