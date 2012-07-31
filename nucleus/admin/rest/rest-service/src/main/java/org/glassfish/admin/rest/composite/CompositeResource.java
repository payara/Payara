/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.glassfish.admin.rest.composite;

import java.security.Principal;
import javax.inject.Inject;
import javax.security.auth.Subject;
import org.glassfish.admin.rest.Constants;
import org.glassfish.admin.rest.RestResource;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.jersey.internal.util.collection.Ref;
import org.glassfish.security.common.Group;

/**
 *
 * @author jdlee
 */
public abstract class CompositeResource implements RestResource {
    @Inject
    protected Ref<Request> requestRef;
    private String authenticatedUser;
    protected CompositeUtil compositeUtil = CompositeUtil.instance();

    protected String getAuthenticatedUser() {
        if (authenticatedUser == null) {
            Request req = requestRef.get();
            Subject subject = (Subject)req.getAttribute(Constants.REQ_ATTR_SUBJECT);
            if (subject != null) {
               for (Principal p : subject.getPrincipals()) {
                   // TODO: This will be replaced with a proper check once the security team delivers the API
                   if (!(p instanceof Group)) {
                       authenticatedUser = p.getName();
                   }
               }
            }
        }

        return authenticatedUser;
    }
}
