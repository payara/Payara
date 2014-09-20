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
package org.glassfish.admin.rest.resources;

import java.util.List;
import java.util.logging.Logger;
import javax.inject.Inject;
import javax.security.auth.Subject;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import org.glassfish.admin.rest.RestLogging;
import org.glassfish.admin.rest.adapter.LocatorBridge;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.jersey.internal.util.collection.Ref;
import org.glassfish.security.services.common.SubjectUtil;

/**
 *
 * @author jdlee
 */
public abstract class AbstractResource {
    @Context
    protected HttpHeaders requestHeaders;
    @Context
    protected UriInfo uriInfo;
    @Inject
    protected Ref<Subject> subjectRef;
    @Inject
    protected LocatorBridge locatorBridge;
    @Context
    protected SecurityContext securityContext;
    @Context
    protected ServiceLocator serviceLocator;

    private String authenticatedUser;
    protected static final Logger logger = RestLogging.restLogger;

    /**
     * This method will return the Subject associated with the current request.
     *
     * @return
     */
    protected Subject getSubject() {
        return subjectRef.get();
    }

    /**
     * This method will return the authenticated user associated with the current request.
     * @return
     */
    protected String getAuthenticatedUser() {
        if (authenticatedUser == null) {
            Subject s = getSubject();
            if (s != null) {
                List<String> list = SubjectUtil.getUsernamesFromSubject(s);
                if (list != null) {
                    authenticatedUser = list.get(0);
                }
            }
        }

        return authenticatedUser;
    }
}
