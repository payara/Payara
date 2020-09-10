/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2018-2020] Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
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
package fish.payara.microprofile.openapi.impl.model.responses;

import fish.payara.microprofile.openapi.api.visitor.ApiContext;
import fish.payara.microprofile.openapi.impl.model.ExtensibleImpl;
import fish.payara.microprofile.openapi.impl.model.headers.HeaderImpl;
import fish.payara.microprofile.openapi.impl.model.links.LinkImpl;
import fish.payara.microprofile.openapi.impl.model.media.ContentImpl;
import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.applyReference;
import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.extractAnnotations;
import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.mergeProperty;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.eclipse.microprofile.openapi.models.headers.Header;
import org.eclipse.microprofile.openapi.models.links.Link;
import org.eclipse.microprofile.openapi.models.media.Content;
import org.eclipse.microprofile.openapi.models.responses.APIResponse;
import org.glassfish.hk2.classmodel.reflect.AnnotationModel;

public class APIResponseImpl extends ExtensibleImpl<APIResponse> implements APIResponse {

    private String description;
    private Map<String, Header> headers = new HashMap<>();
    private Content content = new ContentImpl();
    private List<Content> contents = new ArrayList<>();
    private Map<String, Link> links = new HashMap<>();
    private String ref;
    private String responseCode;

    public static APIResponseImpl createInstance(AnnotationModel annotation, ApiContext context) {
        APIResponseImpl from = new APIResponseImpl();
        from.setDescription(annotation.getValue("description", String.class));
        from.getHeaders().putAll(HeaderImpl.createInstances(annotation, context));
        extractAnnotations(annotation, context, "content", ContentImpl::createInstance, from.getContents());
        extractAnnotations(annotation, context, "links", "name", LinkImpl::createInstance, from.getLinks());
        String ref = annotation.getValue("ref", String.class);
        if (ref != null && !ref.isEmpty()) {
            from.setRef(ref);
        }
        from.setResponseCode(annotation.getValue("responseCode", String.class));
        return from;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public Map<String, Header> getHeaders() {
        return headers;
    }

    @Override
    public void setHeaders(Map<String, Header> headers) {
        this.headers = headers;
    }

    @Override
    public APIResponse addHeader(String name, Header header) {
        if (header != null) {
            headers.put(name, header);
        }
        return this;
    }

    @Override
    public void removeHeader(String name) {
        headers.remove(name);
    }

    @Override
    public Content getContent() {
        return content;
    }

    @Override
    public void setContent(Content content) {
        this.content = content;
    }

    @Override
    public Map<String, Link> getLinks() {
        return links;
    }

    @Override
    public void setLinks(Map<String, Link> links) {
        this.links = links;
    }

    @Override
    public APIResponse addLink(String name, Link link) {
        if (link != null) {
            links.put(name, link);
        }
        return this;
    }

    @Override
    public void removeLink(String name) {
        links.remove(name);
    }

    @Override
    public String getRef() {
        return ref;
    }

    @Override
    public void setRef(String ref) {
        if (ref != null && !ref.contains(".") && !ref.contains("/")) {
            ref = "#/components/responses/" + ref;
        }
        this.ref = ref;
    }

    public String getResponseCode() {
        return responseCode;
    }

    public void setResponseCode(String responseCode) {
        this.responseCode = responseCode;
    }

    public List<Content> getContents() {
        return contents;
    }

    public void setContents(List<Content> contents) {
        this.contents = contents;
    }

    public static void merge(APIResponse from, APIResponse to,
            boolean override, ApiContext context) {
        if (from == null) {
            return;
        }
        if (from.getRef() != null && !from.getRef().isEmpty()) {
            applyReference(to, from.getRef());
            return;
        }
        to.setDescription(mergeProperty(to.getDescription(), from.getDescription(), override));
        if (from.getContent() != null) {
            if (to.getContent() == null) {
                to.setContent(new ContentImpl());
            }
            ContentImpl.merge((ContentImpl)from.getContent(), to.getContent(), override, context);
        }
        if (from instanceof APIResponseImpl) {
            APIResponseImpl fromImpl = (APIResponseImpl) from;
            if (fromImpl.getContents() != null) {
                if (to.getContent() == null) {
                    to.setContent(new ContentImpl());
                }
                for (Content content : fromImpl.getContents()) {
                    ContentImpl.merge((ContentImpl)content, to.getContent(), override, context);
                }
            }
        }
        if (from.getContent() != null) {
            if (to.getContent() == null) {
                to.setContent(new ContentImpl());
            }
            ContentImpl.merge((ContentImpl)from.getContent(), to.getContent(), override, context);
        }
        if (from.getHeaders()!= null) {
            for (String headerName : from.getHeaders().keySet()) {
                HeaderImpl.merge(
                        headerName,
                        from.getHeaders().get(headerName),
                        to.getHeaders(),
                        override,
                        context
                );
            }
        }
        if (from.getLinks() != null) {
            for (String linkName : from.getLinks().keySet()) {
                LinkImpl.merge(linkName, from.getLinks().get(linkName), to.getLinks(), override);
            }
        }
    }

}
