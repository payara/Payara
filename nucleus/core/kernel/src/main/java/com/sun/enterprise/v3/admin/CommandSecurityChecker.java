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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.security.auth.Subject;
import org.glassfish.api.admin.AccessRequired;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.SelfAuthorizer;
import org.jvnet.hk2.annotations.Service;

/**
 * Utility class which checks if the Subject is allowed to execute the specified command.
 * <p>
 * The command should use one of two ways of doing authorization.  It can be
 * annotated with @AccessRequired or @AccessRequired.List, or it can implement
 * SelfAuthorizer.  This class performs the logic if one of the two annos is used;
 * it delegates to the command object itself if the command is a SelfAuthorizer.
 * 
 * @author tjquinn
 */
@Service
public class CommandSecurityChecker {
    
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
        
        if (command instanceof SelfAuthorizer) {
            ((SelfAuthorizer) command).authorize(subject, env);
        } else {
            isAuthorizedUsingAnnos(subject, env, command);
        }
    }
    
    private static void isAuthorizedUsingAnnos(final Subject subject,
            final Map<String,Object> env,
            final AdminCommand command) throws SecurityException {
        final List<AccessRequired> accessesRequired = getAccessesRequired(command);
        for (AccessRequired a : accessesRequired) {
            final String fullResourceName = replaceTokens(a.resourceName(), env);
            if ( ! authService.isAuthorized(subject, fullResourceName, a.action())) {
                throw new SecurityException();
            }
        }
    }
    
    private static String replaceTokens(final String original, final Map<String,Object> env) {
        final Matcher m = TOKEN_PATTERN.matcher(original);
        final StringBuffer sb = new StringBuffer();
        while (m.find()) {
            final String matchedTokenValue = env.get(m.group()).toString();
            if (matchedTokenValue != null) {
                m.appendReplacement(sb, matchedTokenValue);
            }
        }
        m.appendTail(sb);
        return sb.toString();
    }
    
    private static List<AccessRequired> getAccessesRequired(final AdminCommand command) {
        final List<AccessRequired> accesses = new ArrayList<AccessRequired>();
        /*
         * Optimize to check the single anno first.  If it's there, then the
         * List one will not be so we do not have to look for it.
         */
        if ( ! addFromAnno(accesses, command)) {
            addFromListAnno(accesses, command);
        }
        return accesses;
    }
    
    private static boolean addFromListAnno(final List<AccessRequired> accesses, final AdminCommand command) {
        final AccessRequired.List list;
        if ((list = command.getClass().getAnnotation(AccessRequired.List.class)) != null) {
            accesses.addAll(Arrays.asList(list.value()));
        }
        return list != null;
    }
    
    private static boolean addFromAnno(final List<AccessRequired> accesses, final AdminCommand command) {
        final AccessRequired a;
        if ((a = command.getClass().getAnnotation(AccessRequired.class)) != null) {
            accesses.add(a);
        }
        return a != null;
    }
}
