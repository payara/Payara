/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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
    protected LocatorBridge habitat;
    @Context
    protected SecurityContext sc;
    @Context
    protected ServiceLocator injector;

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
