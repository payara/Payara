/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2018] Payara Foundation and/or its affiliates. All rights reserved.
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

import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.isAnnotationNull;

import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.microprofile.openapi.annotations.media.Encoding;
import org.eclipse.microprofile.openapi.annotations.media.ExampleObject;
import org.eclipse.microprofile.openapi.models.media.Content;
import org.eclipse.microprofile.openapi.models.media.MediaType;
import org.eclipse.microprofile.openapi.models.media.Schema;

import fish.payara.microprofile.openapi.impl.model.examples.ExampleImpl;

public class ContentImpl extends LinkedHashMap<String, MediaType> implements Content {

    private static final long serialVersionUID = 1575356277308242221L;

    public ContentImpl() {
        super();
    }

    public ContentImpl(Map<? extends String, ? extends MediaType> items) {
        super(items);
    }

    @Override
    public Content addMediaType(String name, MediaType item) {
        this.put(name, item);
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

    public static void merge(org.eclipse.microprofile.openapi.annotations.media.Content from, Content to,
            boolean override, Map<String, Schema> currentSchemas) {
        if (from == null) {
            return;
        }

        // Get the name of the media type
        String typeName = from.mediaType();
        if (typeName == null || typeName.isEmpty()) {
            typeName = javax.ws.rs.core.MediaType.WILDCARD;
        }

        // Get or create the corresponding media type
        MediaType mediaType = to.getOrDefault(typeName, new MediaTypeImpl());
        to.addMediaType(typeName, mediaType);

        // Merge encoding
        for (Encoding encoding : from.encoding()) {
            EncodingImpl.merge(encoding, to.getMediaType(typeName).getEncoding(), override, currentSchemas);
        }

        // Merge examples
        for (ExampleObject example : from.examples()) {
            ExampleImpl.merge(example, to.getMediaType(typeName).getExamples(), override);
        }
        if (!from.example().isEmpty()) {
            to.getMediaType(typeName).setExample(from.example());
        }

        // Merge schema
        if (!isAnnotationNull(from.schema())) {
            if (mediaType.getSchema() == null) {
                mediaType.setSchema(new SchemaImpl());
            }
            Schema schema = mediaType.getSchema();
            SchemaImpl.merge(from.schema(), schema, true, currentSchemas);
        }
    }

}
