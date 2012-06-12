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
package org.glassfish.security.services.spi;

import org.glassfish.security.services.api.authorization.AuthorizationService;
import org.glassfish.security.services.api.authorization.AzAction;
import org.glassfish.security.services.api.authorization.AzEnvironment;
import org.glassfish.security.services.api.authorization.AzResource;
import org.glassfish.security.services.api.authorization.AzResult;
import org.glassfish.security.services.api.authorization.AzSubject;

import org.jvnet.hk2.annotations.Contract;


@Contract
public interface AuthorizationProvider extends SecurityProvider {

    
    
    /**
     * Evaluate the specified subject, resource, action, and environment against the body of 
     * policy managed by this provider and return an access control result.
     * @param subject The attributes collection representing the Subject for which an authorization decision 
     *                is requested.
     * @param resource The attributes collection representing the resource for which access is being requested.
     * @param action The attributes collection representing the action, with respect to the resource, 
     *               for which access is being requested.
     * @param environment The attributes collection representing the environment, or context, 
     *                    in which the access decision is being requested.
     * @return The AzResult indicating the result of the access decision.
     */
    AzResult getAuthorizationDecision(AzSubject subject, 
                                      AzResource resource, 
                                      AzAction action, 
                                      AzEnvironment environment);
    
    
    /**
     * Find an existing PolicyDeploymentContext, or create a new one if one does not already exist 
     * for the specified appContext. The context will be returned in an "open" state, and will stay 
     * that way until commit() or delete() is called.
     * @param appContext The application context for which the PolicyDeploymentContext is desired.
     * @return The resulting PolicyDeployment Context.
     */
    AuthorizationService.PolicyDeploymentContext findOrCreateDeployContext(String appContext);

    
    
}
