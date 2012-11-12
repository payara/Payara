/*
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
 */
package org.glassfish.admin.rest.adapter;

import java.io.IOException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;
import org.glassfish.admin.rest.Constants;
import org.glassfish.admin.rest.utils.Util;

/**
 * This filter reformats string entities from non-success responses 
 * into arrays of message entities (when not using the REST legacy mode).
 *
 * @author tmoreau
 */
@Provider
public class ExceptionFilter implements ContainerResponseFilter {

    public ExceptionFilter() {
    }

    public void filter(ContainerRequestContext reqCtx, ContainerResponseContext resCtx) throws IOException {
        if (reqCtx.getHeaderString(Constants.HEADER_LEGACY_FORMAT) != null) {
            // TBD - don't wrap if the legacy switch is set
            return;
        }

        int status = resCtx.getStatus();
        if (200 <= status && status <= 299) {
            // don't wrap success messages
            return;
        }

        Object entity = resCtx.getEntity();
        if (!(entity instanceof String)) {
            // don't wrap null and non-String entities
            return;
        }

        String errorMsg = (String)entity;
        Object wrappedEntity = Util.responseBody().addFailure(errorMsg);
        resCtx.setEntity(wrappedEntity, resCtx.getEntityAnnotations(), resCtx.getMediaType());
    }
}
