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
package fish.payara.microprofile.openapi.impl.model.parameters;

import fish.payara.microprofile.openapi.api.visitor.ApiContext;
import fish.payara.microprofile.openapi.impl.model.ExtensibleImpl;
import fish.payara.microprofile.openapi.impl.model.media.ContentImpl;

import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.applyReference;
import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.createList;
import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.extractAnnotations;
import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.mergeProperty;

import java.util.List;
import org.eclipse.microprofile.openapi.models.media.Content;
import org.eclipse.microprofile.openapi.models.parameters.RequestBody;
import org.glassfish.hk2.classmodel.reflect.AnnotationModel;

public class RequestBodyImpl extends ExtensibleImpl<RequestBody> implements RequestBody {

    private String description;
    private Content content = new ContentImpl();
    private Boolean required;
    private String ref;

    public static RequestBodyImpl createInstance(AnnotationModel annotation, ApiContext context) {
        RequestBodyImpl from = new RequestBodyImpl();
        from.setDescription(annotation.getValue("description", String.class));
        from.setExtensions(parseExtensions(annotation));
        from.setRequired(annotation.getValue("required", Boolean.class));
        String ref = annotation.getValue("ref", String.class);
        if (ref != null && !ref.isEmpty()) {
            from.setRef(ref);
        }

        final List<ContentImpl> contents = createList();
        extractAnnotations(annotation, context, "content", ContentImpl::createInstance, contents::add);
        for (ContentImpl content : contents) {
            content.getMediaTypes().forEach(from.content::addMediaType);
        }

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
    public Content getContent() {
        return content;
    }

    @Override
    public void setContent(Content content) {
        this.content = content;
    }

    @Override
    public Boolean getRequired() {
        return required;
    }

    @Override
    public void setRequired(Boolean required) {
        this.required = required;
    }

    @Override
    public String getRef() {
        return ref;
    }

    @Override
    public void setRef(String ref) {
        if (ref != null && !ref.contains(".") && !ref.contains("/")) {
            ref = "#/components/requestBodies/" + ref;
        }
        this.ref = ref;
    }

    public static void merge(RequestBody from, RequestBody to,
            boolean override, ApiContext context) {
        if (from == null) {
            return;
        }
        if (from.getRef() != null && !from.getRef().isEmpty()) {
            applyReference(to, from.getRef());
            return;
        }
        to.setDescription(mergeProperty(to.getDescription(), from.getDescription(), override));
        to.setExtensions(mergeProperty(to.getExtensions(), from.getExtensions(), override));
        to.setRequired(mergeProperty(to.getRequired(), from.getRequired(), override));
        if (from.getContent() != null) {
            if (to.getContent() == null) {
                to.setContent(new ContentImpl());
            }
            ContentImpl.merge((ContentImpl)from.getContent(), to.getContent(), override, context);
        }

    }

}
