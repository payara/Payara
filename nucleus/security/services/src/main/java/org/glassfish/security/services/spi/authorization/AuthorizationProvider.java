/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.security.services.spi.authorization;

import org.glassfish.security.services.api.authorization.*;
import org.glassfish.security.services.spi.SecurityProvider;

import org.jvnet.hk2.annotations.Contract;

import java.util.List;

/**
 * <code>AuthorizationProvider</code> instances are used by a
 * <code>{@link org.glassfish.security.services.api.authorization.AuthorizationService}</code>
 * to make access authorization decisions. This is part of a plug-in mechanism,
 * which allows access decisions to deferred to an configured implementation.
 */
@Contract
public interface AuthorizationProvider extends SecurityProvider {


    /**
     * Evaluates the specified subject, resource, action, and environment against the body of
     * policy managed by this provider and returns an access control result.
     *
     * @param subject The attributes collection representing the Subject for which an authorization
     * decision is requested.
     * @param resource The attributes collection representing the resource for which access is
     * being requested.
     * @param action  The attributes collection representing the action, with respect to the resource,
     * for which access is being requested.  A null action is interpreted as all
     * actions, however all actions may also be represented by the AzAction instance.
     * See <code>{@link org.glassfish.security.services.api.authorization.AzAction}</code>.
     * @param environment The attributes collection representing the environment, or context,
     *                    in which the access decision is being requested, null if none.
     * @param attributeResolvers The ordered list of attribute resolvers, for
     * run time determination of missing attributes, null if none.
     * @return The AzResult indicating the result of the access decision.
     * @throws IllegalArgumentException Given null or illegal subject or resource
     * @throws IllegalStateException Provider was not initialized.
     * @see AuthorizationService#getAuthorizationDecision
     */
    AzResult getAuthorizationDecision(
        AzSubject subject,
        AzResource resource,
        AzAction action,
        AzEnvironment environment,
        List<AzAttributeResolver> attributeResolvers );


    /**
     * Finds an existing PolicyDeploymentContext, or create a new one if one does not
     * already exist for the specified appContext.  The context will be returned in
     * an "open" state, and will stay that way until commit() or delete() is called.
     *
     * @param appContext The application context for which the PolicyDeploymentContext
     * is desired.
     * @return The resulting PolicyDeploymentContext,
     * null if this provider does not support this feature.
     * @throws IllegalStateException Provider was not initialized, if this method is supported.
     * @see AuthorizationService#findOrCreateDeploymentContext(String)
     */
    AuthorizationService.PolicyDeploymentContext findOrCreateDeploymentContext(
        String appContext);
}
