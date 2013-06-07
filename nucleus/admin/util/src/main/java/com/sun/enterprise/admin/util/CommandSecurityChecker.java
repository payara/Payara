/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright (c) 2012-2013 Oracle and/or its affiliates. All rights reserved.
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

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.Principal;
import java.security.PrivilegedAction;
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
import org.glassfish.hk2.api.IterableProvider;
import org.glassfish.hk2.api.PostConstruct;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.internal.api.EmbeddedSystemAdministrator;
import org.glassfish.logging.annotation.LogMessagesResourceBundle;
import org.glassfish.logging.annotation.LoggerInfo;
import org.glassfish.security.services.api.authorization.AuthorizationAdminConstants;
import org.glassfish.security.services.api.authorization.AuthorizationService;
import org.glassfish.security.services.api.authorization.AzAction;
import org.glassfish.security.services.api.authorization.AzResource;
import org.glassfish.security.services.api.authorization.AzResult;
import org.glassfish.security.services.api.authorization.AzSubject;
import org.glassfish.security.services.api.common.Attributes;
import org.glassfish.security.services.api.context.SecurityContextService;
import org.jvnet.hk2.annotations.Service;
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
 * {@code getAccessChecks} - for which {@code isFailureFatal} is true must pass.
 * 
 * @author tjquinn
 */
@Service
@Singleton
public class CommandSecurityChecker implements PostConstruct {
    
    @LoggerInfo(subsystem="ADMSECAUTHZ", description="Admin security authorization")
    private static final String ADMSEC_AUTHZ_LOGGER_NAME = "javax.enterprise.system.tools.admin.security.authorization";

    @LogMessagesResourceBundle
    private static final String LOG_MESSAGES_RB = "com.sun.enterprise.admin.util.LogMessages";

    static final Logger ADMSEC_AUTHZ_LOGGER = Logger.getLogger(ADMSEC_AUTHZ_LOGGER_NAME, LOG_MESSAGES_RB);
    private static final String RESOURCE_NAME_URL_ENCODING = "UTF-8";
    
    private static final Level PROGRESS_LEVEL = Level.FINE;
    private static final String LINE_SEP = System.getProperty("line.separator");
    private static final String ADMIN_RESOURCE_SCHEME = "admin";
    
    @Inject
    private ServiceLocator locator;

    @Inject
    private AuthorizationService authService;
    
    @Inject
    private NamedResourceManager namedResourceMgr;
    
    @Inject
    private IterableProvider<AuthorizationPreprocessor> authPreprocessors;
    
    @Inject
    private ServerEnvironment serverEnv;
    
    @Inject
    private SecurityContextService securityContextService;
    
    @Inject
    private EmbeddedSystemAdministrator embeddedSystemAdministrator;
    
    private static final Map<RestEndpoint.OpType,String> optypeToAction = initOptypeMap();

    @Override
    public void postConstruct() {
        /*
         * Indicate whether this is the DAS or an instance in the security
         * environment so the provider implementation can see it and use it
         * in making authorization decisions.
         */
        securityContextService.getEnvironmentAttributes().addAttribute(
                AuthorizationAdminConstants.ISDAS_ATTRIBUTE, 
                Boolean.toString(serverEnv.isDas()), true);
    }
    
    
    /**
     * Maps RestEndpoint HTTP methods to the corresponding security action.
     * @return 
     */
    private static EnumMap<RestEndpoint.OpType,String> initOptypeMap() {
        final EnumMap<RestEndpoint.OpType,String> result = new EnumMap(RestEndpoint.OpType.class);
        result.put(RestEndpoint.OpType.DELETE, "delete");
        result.put(RestEndpoint.OpType.GET, "read");
        result.put(RestEndpoint.OpType.POST, "update"); // write
        result.put(RestEndpoint.OpType.PUT, "create"); // insert
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
    public boolean authorize(Subject subject,
            final Map<String,Object> env,
            final AdminCommand command,
            final AdminCommandContext adminCommandContext) throws SecurityException {
        if (subject == null) {
            ADMSEC_AUTHZ_LOGGER.log(Level.WARNING, command.getClass().getName(),
                    new IllegalArgumentException("subject"));
            subject = new Subject();
        }
        
        boolean result;
        try {
            if (command instanceof AdminCommandSecurity.Preauthorization) {
                /*
                 * Invoke preAuthorization in the context of the Subject.
                 */
                result = Subject.doAs(subject, new PrivilegedAction<Boolean> () {

                    @Override
                    public Boolean run() {
                        return ((AdminCommandSecurity.Preauthorization) command).preAuthorization(adminCommandContext);
                    }

                });
                if ( ! result) {
                    return false;
                }
            }
            final List<AccessCheckWork> accessChecks = assembleAccessCheckWork(command, subject);
            result = (embeddedSystemAdministrator.matches(subject)) ||
                checkAccessRequired(subject, env, command, accessChecks);

        } catch (Exception ex) {
            ADMSEC_AUTHZ_LOGGER.log(Level.SEVERE, AdminLoggerInfo.mUnexpectedException, ex);
            throw new RuntimeException(ex);
        }
        /*
         * Check the result and throw the SecurityException outside the previous
         * try block. Otherwise the earlier catch will dump the stack which we
         * do not need for simple authorization errors.
         */
        if ( ! result) {
//                final List<AccessCheck> failedAccessChecks = new ArrayList<AccessCheck>();
//                for (AccessCheckWork acWork : accessChecks) {
//                    if ( ! acWork.accessCheck.isSuccessful()) {
//                        failedAccessChecks.add(acWork.accessCheck);
//                    }
//                }
            throw new SecurityException();
        }
        return result;
    }
    
    private List<AccessCheckWork> assembleAccessCheckWork(
            final AdminCommand command,
            final Subject subject) throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
        final boolean isTaggable = ADMSEC_AUTHZ_LOGGER.isLoggable(PROGRESS_LEVEL);
        final List<AccessCheckWork> accessChecks = new ArrayList<AccessCheckWork>();
        /*
         * The CRUD classes such as GenericCreateCommand implement AccessRequired.AccessCheckProvider
         * and so provide their own AccessCheck objects.  So the addChecksFromAccessCheckProvider
         * method will cover the CRUD commands.
         */
        final boolean isCommandAccessCheckProvider = addChecksFromAccessCheckProvider(command, accessChecks, isTaggable, subject);
        addChecksFromExplicitAccessRequiredAnnos(command, accessChecks, isTaggable);
        addChecksFromReSTEndpoints(command, accessChecks, isTaggable);

        /*
         * If this command has no access requirements specified and does not
         * implement AccessCheckProvider, use
         * one from the "unguarded" part of the resource tree.
         */
        if (accessChecks.isEmpty() && ! isCommandAccessCheckProvider) {
            accessChecks.add(new UnguardedCommandAccessCheckWork(command));
        }
        
        return accessChecks;
    }
    
    private boolean checkAccessRequired(Subject subject,
            final Map<String,Object> env,
            final AdminCommand command,
            final List<AccessCheckWork> accessChecks) 
                throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException, URISyntaxException, UnsupportedEncodingException {
        
        final boolean isTaggable = ADMSEC_AUTHZ_LOGGER.isLoggable(PROGRESS_LEVEL);
        boolean result = true;
        final StringBuilder sb = (isTaggable ? (new StringBuilder(LINE_SEP)).append("AccessCheck processing on ").append(command.getClass().getName()).append(LINE_SEP) : null);
        for (final AccessCheckWork a : accessChecks) {
            final URI resourceURI = resourceURIFromAccessCheck(a.accessCheck);
            final AzSubject azSubject = authService.makeAzSubject(subject);
            final AzResource azResource = authService.makeAzResource(resourceURI);
            final AzAction azAction = authService.makeAzAction(a.accessCheck.action());
            final Map<String,String> subjectAttrs = new HashMap<String,String>();
            final Map<String,String> resourceAttrs = new HashMap<String,String>();
            final Map<String,String> actionAttrs = new HashMap<String,String>();
            for (AuthorizationPreprocessor ap : authPreprocessors) {
                ap.describeAuthorization(
                        subject, 
                        a.accessCheck.resourceName(), 
                        a.accessCheck.action(),
                        command,
                        env, 
                        subjectAttrs, 
                        resourceAttrs, 
                        actionAttrs);
            }
            mapToAzAttrs(subjectAttrs, azSubject);
            mapToAzAttrs(resourceAttrs, azResource);
            mapToAzAttrs(actionAttrs, azAction);
            
            final AzResult azResult = authService.getAuthorizationDecision(azSubject, azResource, azAction);
            a.accessCheck.setSuccessful(azResult.getDecision() == AzResult.Decision.PERMIT);
            if (isTaggable) {
                sb.append(a.tag).append(LINE_SEP).
                   append("    ").append(formattedAccessCheck(resourceURI, a.accessCheck)).append(LINE_SEP);
            }
            result &= ( (! a.accessCheck.isFailureFinal()) || a.accessCheck.isSuccessful());
        }
        if (isTaggable) {
            sb.append(LINE_SEP).append("...final result: ").append(result).append(LINE_SEP);
            ADMSEC_AUTHZ_LOGGER.log(PROGRESS_LEVEL, sb.toString());
        }
        return result;
    }
    
    private String formattedAccessCheck(final URI resourceURI, final AccessCheck a) {
        return (new StringBuilder("AccessCheck ")).
                    append(resourceURI.toASCIIString()).
                    append("=").
                    append(a.action()).
                    append(", isSuccessful=").
                    append(a.isSuccessful()).
                    append(", isFailureFatal=").
                    append(a.isFailureFinal()).
                    append("//").
                    append(a.note()).
                    toString();
    }
    private void mapToAzAttrs(final Map<String,String> info, final Attributes attrs) {
        for (Map.Entry<String,String> i : info.entrySet()) {
            attrs.addAttribute(i.getKey(), i.getValue(), false /* replace */);
        }
    }
    
    /**
     * Returns all AccessCheck objects which apply to the specified command.
     * @param command the AdminCommand for which the AccessChecks are needed
     * @param subject the Subject resulting from successful authentication
     * @return the AccessChecks resulting from analyzing the command
     * @throws NoSuchFieldException
     * @throws IllegalArgumentException
     * @throws IllegalAccessException 
     */
    public Collection<? extends AccessCheck> getAccessChecks(final AdminCommand command,
            final Subject subject) throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
        final List<AccessCheckWork> work = assembleAccessCheckWork(command, subject);
        final Collection<AccessCheck> accessChecks = new ArrayList<AccessCheck>();
        for (AccessCheckWork w : work) {
            accessChecks.add(w.accessCheck());
        }
        return accessChecks;
    }

    /**
     * Adds access checks from the command's explicit getAccessChecks method.
     * @param subject
     * @param command
     * @param accessChecks
     * @param isTaggable
     * @return 
     */
    private boolean addChecksFromAccessCheckProvider( 
            final AdminCommand command, final List<AccessCheckWork> accessChecks,
            final boolean isTaggable,
            final Subject subject) {
        if (command instanceof AdminCommandSecurity.AccessCheckProvider) {
            /*
             * Invoke getAccessChecks in the context of the Subject.
             */
            final Collection<? extends AccessCheck> checks = 
                    Subject.doAs(subject, new PrivilegedAction<Collection<? extends AccessCheck>>() {

                @Override
                public Collection<? extends AccessCheck> run() {
                    return ((AdminCommandSecurity.AccessCheckProvider) command).getAccessChecks();
                }
            });
                        
            for (AccessCheck ac : checks) {
                accessChecks.add(new AccessCheckWork(ac,
                        isTaggable ? "  Class's getAccessChecks()"  : null));
            }
            return true;
        }
        return false;
    }
    
    private URI resourceURIFromAccessCheck(final AccessCheck c) throws URISyntaxException, UnsupportedEncodingException {
        return new URI(ADMIN_RESOURCE_SCHEME,
                        resourceNameFromAccessCheck(c) /* ssp */,
                        null /* fragment */);
    }
    
    private String resourceNameFromAccessCheck(final AccessCheck c) throws UnsupportedEncodingException {
        String resourceName = c.resourceName();
        if (resourceName == null) {
            resourceName = AccessRequired.Util.resourceNameFromConfigBeanType(c.parent(), null, c.childType());
        }
        if ( ! resourceName.startsWith("/")) {
            resourceName = '/' + resourceName;
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
            
            isAnnotated |= addAccessChecksFromFields(c, command, accessChecks, isTaggable);
        }
        return isAnnotated;
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
        for (Class c = command.getClass(); c != null; c = c.getSuperclass()) {
            try {
                result = c.getDeclaredField(fieldName);
                return result;
            } catch (NoSuchFieldException ex) {
                continue; 
            }
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
        final Object fieldValue = f.get(command);
        if (fieldValue == null) {
            throw new IllegalArgumentException(command.getClass().getName() + "." + f.getName() + "== null");
        }
        return fieldValue.toString();
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
        if ( ! restEndpoint.useForAuthorization()) {
            return;
        }
        final String action = optypeToAction.get(restEndpoint.opType());
        /*
         * For the moment, if there is no RestParam then the config bean class given
         * in the anno is an unnamed singleton target of the action.  If there is a RestParam
         * then the target is (probably) the unnamed singleton parent of the config bean class given
         * and the new child's type is the config bean class.
         */
        String resource;
//        if (restEndpoint.params().length == 0) {
            resource = resourceNameFromRestEndpoint(restEndpoint.configBean(), 
                    restEndpoint.path(),
                    locator);
//        } else {
//            // TODO need to do something with the endpoint params
//            resource = resourceNameFromRestEndpoint(restEndpoint.configBean(), 
//                    restEndpoint.path(),
//                    locator);
//        }
        final AccessCheck a = new AccessCheck(resource, action);
        String tag = null;
        if (isTaggable) {
            tag = "  @RestEndpoint " + restEndpoint.configBean().getName() + ", op=" + restEndpoint.opType();
        }
        accessChecks.add(new AccessCheckWork(a, tag));
    }
    
    private static String resourceNameFromRestEndpoint(Class<? extends ConfigBeanProxy> c, 
            final String path,
            final ServiceLocator locator) {
        ConfigBeanProxy b = locator.getService(c);
        String name = (b != null ? AccessRequired.Util.resourceNameFromConfigBeanProxy(b) : "?");
        if (path != null) {
            name += '/' + path;
        }
        return name;
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
        
        private AccessCheck accessCheck() {
            return accessCheck;
        }
    }
    
    private static class UnguardedCommandAccessCheckWork extends AccessCheckWork {
        private UnguardedCommandAccessCheckWork(final AdminCommand c) {
            /*
             * Get the name of the command from the @Service annotation.
             */
            super(new AccessCheck("unguarded/" + getCommandName(c), "execute"),"  Unguarded access control on " + c.getClass().getName());
        }
    }
    
    private static String getCommandName(final AdminCommand c) {
        final Service serviceAnno = c.getClass().getAnnotation(Service.class);
        if (serviceAnno == null) {
            return "no-name";
        }
        return serviceAnno.name();
    }
    
}
