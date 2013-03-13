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
package org.glassfish.admin.rest.composite.resource;

import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriBuilderException;
import javax.ws.rs.core.UriInfo;

/**
 * This Dummy class is meant to do the bare minimum to make the tests pass. It is not meant as a full-featured implementation
 * of <code>UriInfo</code>.  Attempts to use it as such will likely fail.
 * @author jdlee
 */
public class DummyUriInfo implements UriInfo {

    @Override
    public String getPath() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getPath(boolean decode) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<PathSegment> getPathSegments() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<PathSegment> getPathSegments(boolean decode) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public URI getRequestUri() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public UriBuilder getRequestUriBuilder() {
        return new UriBuilderImpl();
    }

    @Override
    public URI getAbsolutePath() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public UriBuilder getAbsolutePathBuilder() {
        return new UriBuilderImpl();
    }

    @Override
    public URI getBaseUri() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public UriBuilder getBaseUriBuilder() {
        return new UriBuilderImpl();
    }

    @Override
    public MultivaluedMap<String, String> getPathParameters() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public MultivaluedMap<String, String> getPathParameters(boolean decode) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public MultivaluedMap<String, String> getQueryParameters() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public MultivaluedMap<String, String> getQueryParameters(boolean decode) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<String> getMatchedURIs() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<String> getMatchedURIs(boolean decode) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<Object> getMatchedResources() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public URI resolve(URI uri) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public URI relativize(URI uri) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private static class UriBuilderImpl extends UriBuilder {

        public UriBuilderImpl() {
        }

        @Override
        public UriBuilder clone() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public UriBuilder uri(URI uri) throws IllegalArgumentException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public UriBuilder uri(String uriTemplate) throws IllegalArgumentException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public UriBuilder scheme(String scheme) throws IllegalArgumentException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public UriBuilder schemeSpecificPart(String ssp) throws IllegalArgumentException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public UriBuilder userInfo(String ui) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public UriBuilder host(String host) throws IllegalArgumentException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public UriBuilder port(int port) throws IllegalArgumentException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public UriBuilder replacePath(String path) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public UriBuilder path(String path) throws IllegalArgumentException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public UriBuilder path(Class type) throws IllegalArgumentException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public UriBuilder path(Class type, String string) throws IllegalArgumentException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public UriBuilder path(Method method) throws IllegalArgumentException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public UriBuilder segment(String... segments) throws IllegalArgumentException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public UriBuilder replaceMatrix(String matrix) throws IllegalArgumentException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public UriBuilder matrixParam(String name, Object... values) throws IllegalArgumentException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public UriBuilder replaceMatrixParam(String name, Object... values) throws IllegalArgumentException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public UriBuilder replaceQuery(String query) throws IllegalArgumentException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public UriBuilder queryParam(String name, Object... values) throws IllegalArgumentException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public UriBuilder replaceQueryParam(String name, Object... values) throws IllegalArgumentException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public UriBuilder fragment(String fragment) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public URI buildFromMap(Map<String, ? extends Object> values) throws IllegalArgumentException, UriBuilderException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public URI buildFromEncodedMap(Map<String, ? extends Object> values) throws IllegalArgumentException, UriBuilderException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public URI build(Object... values) throws IllegalArgumentException, UriBuilderException {
            URI uri = null;
            try {
                uri = new URI("");
            } catch (URISyntaxException ex) {
                Logger.getLogger(DummiesResource.class.getName()).log(Level.SEVERE, null, ex);
            }
            return uri;
        }

        @Override
        public URI buildFromEncoded(Object... values) throws IllegalArgumentException, UriBuilderException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public URI buildFromMap(Map<String, ?> values, boolean encodeSlashInPath) throws IllegalArgumentException, UriBuilderException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public URI build(Object[] values, boolean encodeSlashInPath) throws IllegalArgumentException, UriBuilderException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public String toTemplate() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public UriBuilder resolveTemplate(String name, Object value) throws IllegalArgumentException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public UriBuilder resolveTemplate(String name, Object value, boolean encodeSlashInPath) throws IllegalArgumentException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public UriBuilder resolveTemplateFromEncoded(String name, Object value) throws IllegalArgumentException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public UriBuilder resolveTemplates(Map<String, Object> templateValues) throws IllegalArgumentException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public UriBuilder resolveTemplates(Map<String, Object> templateValues, boolean encodeSlashInPath) throws IllegalArgumentException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public UriBuilder resolveTemplatesFromEncoded(Map<String, Object> templateValues) throws IllegalArgumentException {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }
}
