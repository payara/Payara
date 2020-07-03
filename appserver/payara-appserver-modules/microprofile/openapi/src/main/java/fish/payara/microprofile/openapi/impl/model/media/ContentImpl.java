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
package fish.payara.microprofile.openapi.impl.model.media;

import fish.payara.microprofile.openapi.api.visitor.ApiContext;
import fish.payara.microprofile.openapi.impl.model.examples.ExampleImpl;
import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.extractAnnotations;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.eclipse.microprofile.openapi.models.media.Content;
import org.eclipse.microprofile.openapi.models.media.MediaType;
import org.eclipse.microprofile.openapi.models.media.Schema;
import org.glassfish.hk2.classmodel.reflect.AnnotationModel;

public class ContentImpl extends LinkedHashMap<String, MediaType> implements Content {

    private static final long serialVersionUID = 1575356277308242221L;

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
            typeName = javax.ws.rs.core.MediaType.WILDCARD;
        }
        MediaType mediaType = new MediaTypeImpl();
        from.addMediaType(typeName, mediaType);
        extractAnnotations(annotation, context, "examples", "name", ExampleImpl::createInstance, mediaType.getExamples());
        mediaType.setExample(annotation.getValue("example", String.class));
        AnnotationModel schemaAnnotation = annotation.getValue("schema", AnnotationModel.class);
        if (schemaAnnotation != null) {
            mediaType.setSchema(SchemaImpl.createInstance(schemaAnnotation, context));
        }
        extractAnnotations(annotation, context, "encoding", "name", EncodingImpl::createInstance, mediaType.getEncoding());
        return from;
    }

    @Override
    public Content addMediaType(String name, MediaType item) {
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
        return new ContentImpl(this);
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

        for (String typeName : from.getMediaTypes().keySet()) {

            MediaType fromMediaType = from.getMediaType(typeName);

            // Get or create the corresponding media type
            MediaType toMediaType = to.getOrDefault(typeName, new MediaTypeImpl());
            to.addMediaType(typeName, toMediaType);

            // Merge encoding
            for (String encodingName : fromMediaType.getEncoding().keySet()) {
                EncodingImpl.merge(encodingName,
                        fromMediaType.getEncoding().get(encodingName),
                        to.getMediaType(typeName).getEncoding(), override, context);
            }

            // Merge examples
            for (String exampleName : fromMediaType.getExamples().keySet()) {
                ExampleImpl.merge(exampleName, fromMediaType.getExamples().get(exampleName), to.getMediaType(typeName).getExamples(), override);
            }
            if (fromMediaType.getExample() != null) {
                to.getMediaType(typeName).setExample(fromMediaType.getExample());
            }

            // Merge schema
            if (fromMediaType.getSchema() != null) {
                if (toMediaType.getSchema() == null) {
                    toMediaType.setSchema(new SchemaImpl());
                }
                Schema schema = toMediaType.getSchema();
                SchemaImpl.merge(fromMediaType.getSchema(), schema, true, context);
            }
        }
    }

}
