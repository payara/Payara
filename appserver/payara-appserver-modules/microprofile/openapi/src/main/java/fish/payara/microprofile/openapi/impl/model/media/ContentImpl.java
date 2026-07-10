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
package fish.payara.microprofile.openapi.impl.model.media;

import fish.payara.microprofile.openapi.api.visitor.ApiContext;
import fish.payara.microprofile.openapi.impl.model.ExtensibleImpl;
import fish.payara.microprofile.openapi.impl.model.examples.ExampleImpl;

import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.extractAnnotations;
import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.mergeProperty;
import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.readOnlyView;

import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.microprofile.openapi.models.examples.Example;
import org.eclipse.microprofile.openapi.models.media.Content;
import org.eclipse.microprofile.openapi.models.media.Encoding;
import org.eclipse.microprofile.openapi.models.media.MediaType;
import org.eclipse.microprofile.openapi.models.media.Schema;
import org.glassfish.hk2.classmodel.reflect.AnnotationModel;

public class ContentImpl extends LinkedHashMap<String, MediaType> implements Content {

    private static final long serialVersionUID = 1575356277308242221L;

    private Map<String, Object> extensions = null; // workaround as Content doesn't extend Extendable!

    public ContentImpl() {
        super();
    }

    public ContentImpl(Map<? extends String, ? extends MediaType> items) {
        super(items);
    }

    public static ContentImpl createInstance(AnnotationModel annotation, ApiContext context) {
        ContentImpl from = new ContentImpl();
        String typeName = annotation.getValue("mediaType", String.class);
        if (typeName == null || typeName.isEmpty()) {
            typeName = jakarta.ws.rs.core.MediaType.WILDCARD;
        }
        from.setExtensions(ExtensibleImpl.parseExtensions(annotation));
        MediaType mediaType = new MediaTypeImpl();
        from.addMediaType(typeName, mediaType);
        extractAnnotations(annotation, context, "examples", "name", ExampleImpl::createInstance, mediaType::addExample);
        mediaType.setExample(annotation.getValue("example", String.class));
        AnnotationModel schemaAnnotation = annotation.getValue("schema", AnnotationModel.class);
        if (schemaAnnotation != null) {
            Boolean hidden = schemaAnnotation.getValue("hidden", Boolean.class);
            if (hidden == null || !hidden) {
                mediaType.setSchema(SchemaImpl.createInstance(schemaAnnotation, context));
            }
        }
        extractAnnotations(annotation, context, "encoding", "name", EncodingImpl::createInstance, mediaType::addEncoding);
        return from;
    }

    @Override
    public ContentImpl addMediaType(String name, MediaType item) {
        if (item != null) {
            this.put(name, item);
        }
        return this;
    }

    @Override
    public void removeMediaType(String name) {
        remove(name);
    }

    @Override
    public Map<String, MediaType> getMediaTypes() {
        return readOnlyView(this);
    }

    @Override
    public void setMediaTypes(Map<String, MediaType> mediaTypes) {
        clear();
        putAll(mediaTypes);
    }

    public static void merge(ContentImpl from, Content to, boolean override, ApiContext context) {

        if (from == null) {
            return;
        }
        for (Map.Entry<String, MediaType> fromEntry : from.getMediaTypes().entrySet()) {

            final String typeName = fromEntry.getKey();
            final MediaType fromMediaType = fromEntry.getValue();

            // Get or create the corresponding media type

            MediaTypeImpl toMediaType = (MediaTypeImpl) to.getMediaTypes().getOrDefault(typeName, new MediaTypeImpl());
            to.addMediaType(typeName, toMediaType);
            // Merge encoding
            for (Map.Entry<String, Encoding> encoding : fromMediaType.getEncoding().entrySet()) {
                EncodingImpl.merge(
                    encoding.getKey(),
                    encoding.getValue(),
                    toMediaType.encoding,
                    override,
                    context
                );
            }

            // Merge examples
            for (Map.Entry<String, Example> example : fromMediaType.getExamples().entrySet()) {
                ExampleImpl.merge(
                    example.getKey(),
                    example.getValue(),
                    toMediaType.examples,
                    override
                );
            }

            toMediaType.setExample(mergeProperty(toMediaType.getExample(), fromMediaType.getExample(), override));

            // Merge schema
            if (fromMediaType.getSchema() != null) {
                if (toMediaType.getSchema() == null) {
                    toMediaType.setSchema(new SchemaImpl());
                }
                Schema schema = toMediaType.getSchema();
                SchemaImpl.merge(fromMediaType.getSchema(), schema, true, context);
            }

            // extensions
            ExtensibleImpl.merge(fromMediaType, toMediaType, override);
        }
    }

    public Map<String, Object> getExtensions() {
        return extensions;
    }

    public void setExtensions(Map<String, Object> extensions) {
        this.extensions = extensions;
    }

}
