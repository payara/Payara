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
import fish.payara.microprofile.openapi.impl.model.headers.HeaderImpl;

import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.createMap;
import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.mergeProperty;
import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.readOnlyView;

import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.microprofile.openapi.models.headers.Header;
import org.eclipse.microprofile.openapi.models.media.Encoding;
import org.glassfish.hk2.classmodel.reflect.AnnotationModel;

public class EncodingImpl extends ExtensibleImpl<Encoding> implements Encoding {

    private String contentType;
    private Map<String, Header> headers = createMap();
    private Style style;
    private Boolean explode;
    private Boolean allowReserved;

    public static Encoding createInstance(AnnotationModel annotation, ApiContext context) {
        Encoding from = new EncodingImpl();
        from.setContentType(annotation.getValue("contentType", String.class));
        HeaderImpl.createInstances(annotation, context).forEach(from::addHeader);
        String styleEnum = annotation.getValue("style", String.class);
        if (styleEnum != null) {
            from.setStyle(Style.valueOf(styleEnum.toUpperCase()));
        }
        from.setExplode(annotation.getValue("explode", Boolean.class));
        from.setAllowReserved(annotation.getValue("allowReserved", Boolean.class));
        from.setExtensions(parseExtensions(annotation));

        return from;
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    @Override
    public void setContentType(String contentType) {
        this.contentType = contentType;
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
    public Encoding addHeader(String key, Header header) {
        if (header != null) {
            if (headers == null) {
                headers = createMap();
            }
            headers.put(key, header);
        }
        return this;
    }

    @Override
    public void removeHeader(String key) {
        if (headers != null) {
            headers.remove(key);
        }
    }

    @Override
    public Style getStyle() {
        return style;
    }

    @Override
    public void setStyle(Style style) {
        this.style = style;
    }

    @Override
    public Boolean getExplode() {
        return explode;
    }

    @Override
    public void setExplode(Boolean explode) {
        this.explode = explode;
    }

    @Override
    public Boolean getAllowReserved() {
        return allowReserved;
    }

    @Override
    public void setAllowReserved(Boolean allowReserved) {
        this.allowReserved = allowReserved;
    }

    public static void merge(Encoding from, Encoding to,
            boolean override, ApiContext context) {
        if (from == null) {
            return;
        }
        to.setContentType(mergeProperty(to.getContentType(), from.getContentType(), override));
        to.setStyle(mergeProperty(to.getStyle(), from.getStyle(), override));
        to.setExplode(mergeProperty(to.getExplode(), from.getExplode(), override));
        to.setAllowReserved(mergeProperty(to.getAllowReserved(), from.getAllowReserved(), override));
        to.setExtensions(mergeProperty(to.getExtensions(), from.getExtensions(), override));
        if (from.getHeaders() != null) {
            for (Entry<String, Header> header : from.getHeaders().entrySet()) {
                final String headerName = header.getKey();
                if (headerName != null) {
                    HeaderImpl.merge(
                        headerName,
                        header.getValue(),
                        ((EncodingImpl) to).headers,
                        override,
                        context
                    );
                }
            }
        }
    }

    public static void merge(String encodingName, Encoding encoding,
            Map<String, Encoding> encodings, boolean override, ApiContext context) {
        if (encoding == null) {
            return;
        }

        if (encodingName != null && !encodingName.isEmpty()) {
            // Get or create the encoding
            Encoding model = encodings.getOrDefault(encodingName, new EncodingImpl());
            encodings.put(encodingName, model);

            // Merge the annotation
            merge(encoding, model, override, context);
        }
    }

}
