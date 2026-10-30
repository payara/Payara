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

import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.createMap;
import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.readOnlyView;

import java.util.Map;

import org.eclipse.microprofile.openapi.models.examples.Example;
import org.eclipse.microprofile.openapi.models.media.Encoding;
import org.eclipse.microprofile.openapi.models.media.MediaType;
import org.eclipse.microprofile.openapi.models.media.Schema;

import fish.payara.microprofile.openapi.impl.model.ExtensibleImpl;

public class MediaTypeImpl extends ExtensibleImpl<MediaType> implements MediaType {

    private Schema schema;
    private Object example;
    protected Map<String, Example> examples = createMap();
    protected Map<String, Encoding> encoding = createMap();

    @Override
    public Schema getSchema() {
        return schema;
    }

    @Override
    public void setSchema(Schema schema) {
        this.schema = schema;
    }

    @Override
    public Map<String, Example> getExamples() {
        return readOnlyView(examples);
    }

    @Override
    public void setExamples(Map<String, Example> examples) {
        this.examples = createMap(examples);
    }

    @Override
    public MediaType addExample(String key, Example example) {
        if (example != null) {
            if (this.examples == null) {
                this.examples = createMap();
            }
            this.examples.put(key, example);
        }
        return this;
    }

    @Override
    public void removeExample(String key) {
        if (this.examples != null) {
            this.examples.remove(key);
        }
    }

    @Override
    public Object getExample() {
        return example;
    }

    @Override
    public void setExample(Object example) {
        this.example = example;
    }

    @Override
    public Map<String, Encoding> getEncoding() {
        return readOnlyView(encoding);
    }

    @Override
    public void setEncoding(Map<String, Encoding> encoding) {
        this.encoding = createMap(encoding);
    }

    @Override
    public MediaType addEncoding(String key, Encoding encodingItem) {
        if (encodingItem != null) {
            if (this.encoding == null) {
                this.encoding = createMap();
            }
            this.encoding.put(key, encodingItem);
        }
        return this;
    }

    @Override
    public void removeEncoding(String key) {
        if (this.encoding != null) {
            this.encoding.remove(key);
        }
    }

}
