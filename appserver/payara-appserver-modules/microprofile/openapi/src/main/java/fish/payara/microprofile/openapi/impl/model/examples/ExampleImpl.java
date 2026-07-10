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
package fish.payara.microprofile.openapi.impl.model.examples;

import fish.payara.microprofile.openapi.api.visitor.ApiContext;
import fish.payara.microprofile.openapi.impl.model.ExtensibleImpl;
import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.UNKNOWN_ELEMENT_NAME;
import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.mergeProperty;
import java.util.Map;
import org.eclipse.microprofile.openapi.models.examples.Example;
import org.glassfish.hk2.classmodel.reflect.AnnotationModel;

public class ExampleImpl extends ExtensibleImpl<Example> implements Example {

    private String summary;
    private String description;
    private Object value;
    private String externalValue;
    private String ref;

    public static Example createInstance(AnnotationModel annotation, ApiContext context) {
        Example from = new ExampleImpl();
        from.setSummary(annotation.getValue("summary", String.class));
        from.setDescription(annotation.getValue("description", String.class));
        from.setValue(annotation.getValue("value", Object.class));
        from.setExternalValue(annotation.getValue("externalValue", String.class));
        String ref = annotation.getValue("ref", String.class);
        from.setExtensions(parseExtensions(annotation));
        if (ref != null && !ref.isEmpty()) {
            from.setRef(ref);
        }
        return from;
    }

    @Override
    public String getSummary() {
        return summary;
    }

    @Override
    public void setSummary(String summary) {
        this.summary = summary;
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
    public Object getValue() {
        return value;
    }

    @Override
    public void setValue(Object value) {
        this.value = value;
    }

    @Override
    public String getExternalValue() {
        return externalValue;
    }

    @Override
    public void setExternalValue(String externalValue) {
        this.externalValue = externalValue;
    }

    @Override
    public String getRef() {
        return ref;
    }

    @Override
    public void setRef(String ref) {
        if (ref != null && !ref.contains(".") && !ref.contains("/")) {
            ref = "#/components/examples/" + ref;
        }
        this.ref = ref;
    }

    public static void merge(Example from, Example to, boolean override) {
        if (from == null) {
            return;
        }
        to.setSummary(mergeProperty(to.getSummary(), from.getSummary(), override));
        to.setDescription(mergeProperty(to.getDescription(), from.getDescription(), override));
        to.setValue(mergeProperty(to.getValue(), from.getValue(), override));
        to.setExternalValue(mergeProperty(to.getExternalValue(), from.getExternalValue(), override));
        to.setExtensions(mergeProperty(to.getExtensions(), from.getExtensions(), override));
    }

    public static void merge(String exampleName, Example example, Map<String, Example> examples, boolean override) {
        if (example == null) {
            return;
        }

        if (exampleName == null || exampleName.isEmpty()) {
            exampleName = UNKNOWN_ELEMENT_NAME;
        }

        // Get or create the example
        Example model = examples.getOrDefault(exampleName, new ExampleImpl());
        examples.put(exampleName, model);

        // Merge the annotation
        merge(example, model, override);

        // If the merged annotation has a reference, set the name to the reference
        if (model.getRef() != null) {
            examples.remove(exampleName);
            examples.put(model.getRef().split("/")[3], model);
        }
    }

}