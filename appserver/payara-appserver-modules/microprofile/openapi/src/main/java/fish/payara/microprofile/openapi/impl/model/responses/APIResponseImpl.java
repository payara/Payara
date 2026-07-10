/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2018-2023] Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
import fish.payara.microprofile.openapi.impl.model.media.MediaTypeImpl;
import fish.payara.microprofile.openapi.impl.model.media.SchemaImpl;

import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.applyReference;
import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.createList;
import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.createMap;
import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.extractAnnotations;
import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.mergeProperty;
import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.readOnlyView;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import jakarta.ws.rs.core.MediaType;
import java.util.Objects;

import org.eclipse.microprofile.openapi.models.headers.Header;
import org.eclipse.microprofile.openapi.models.links.Link;
import org.eclipse.microprofile.openapi.models.media.Content;
import org.eclipse.microprofile.openapi.models.responses.APIResponse;
import org.glassfish.hk2.classmodel.reflect.AnnotationModel;

public class APIResponseImpl extends ExtensibleImpl<APIResponse> implements APIResponse {

    private String description;
    private Map<String, Header> headers = createMap();
    private Content content = new ContentImpl();
    private Map<String, Link> links = createMap();
    private String ref;
    private String responseCode;

    public static APIResponseImpl createInstance(AnnotationModel annotation, ApiContext context) {
        APIResponseImpl from = new APIResponseImpl();
        from.setDescription(annotation.getValue("description", String.class));
        from.setExtensions(parseExtensions(annotation));
        HeaderImpl.createInstances(annotation, context).forEach(from::addHeader);

        final List<ContentImpl> contents = createList();

        // If the annotation is @APIResponseSchema, parse the schema and description
        final String implementationClass = annotation.getValue("value", String.class);
        if (implementationClass != null) {
            ContentImpl content = new ContentImpl()
                    .addMediaType(MediaType.WILDCARD, new MediaTypeImpl()
                            .schema(SchemaImpl.fromImplementation(implementationClass, context)));
            contents.add(content);
            
            from.setDescription(annotation.getValue("responseDescription", String.class));
        }

        extractAnnotations(annotation, context, "content", ContentImpl::createInstance, contents::add);
        for (ContentImpl content : contents) {
            content.getMediaTypes().forEach(from.content::addMediaType);
            // copy extensions down to media types
            if (content.getExtensions() != null) {
                content.getExtensions().forEach((extKey, extValue) -> content.getMediaTypes().forEach((mtKey, mtValue) -> from.content.getMediaType(mtKey).addExtension(extKey, extValue)));
            }
        }

        extractAnnotations(annotation, context, "links", "name", LinkImpl::createInstance, from::addLink);
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
        return readOnlyView(headers);
    }

    @Override
    public void setHeaders(Map<String, Header> headers) {
        this.headers = createMap(headers);
    }

    @Override
    public APIResponse addHeader(String name, Header header) {
        if (header != null) {
            if (headers == null) {
                headers = createMap();
            }
            headers.put(name, header);
        }
        return this;
    }

    @Override
    public void removeHeader(String name) {
        if (headers != null) {
            headers.remove(name);
        }
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
        return readOnlyView(links);
    }

    @Override
    public void setLinks(Map<String, Link> links) {
        this.links = createMap(links);
    }

    @Override
    public APIResponse addLink(String name, Link link) {
        if (link != null) {
            if (links == null) {
                links = createMap();
            }
            links.put(name, link);
        }
        return this;
    }

    @Override
    public void removeLink(String name) {
        if (links != null) {
            links.remove(name);
        }
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
        ExtensibleImpl.merge(from, to, override);
        if (from.getContent() != null) {
            if (to.getContent() == null) {
                to.setContent(new ContentImpl());
            }
            ContentImpl.merge((ContentImpl) from.getContent(), to.getContent(), override, context);
        }
        if (from.getHeaders()!= null) {
            for (Entry<String, Header> header : from.getHeaders().entrySet()) {
                HeaderImpl.merge(
                    header.getKey(),
                    header.getValue(),
                    ((APIResponseImpl) to).headers,
                    override,
                    context
                );
            }
        }
        if (from.getLinks() != null) {
            for (Entry<String, Link> link : from.getLinks().entrySet()) {
                LinkImpl.merge(
                    link.getKey(),
                    link.getValue(),
                    ((APIResponseImpl) to).links,
                    override
                );
            }
        }
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 37 * hash + Objects.hashCode(this.responseCode);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final APIResponseImpl other = (APIResponseImpl) obj;
        return Objects.equals(this.responseCode, other.responseCode);
    }

}
