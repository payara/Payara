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
package fish.payara.microprofile.openapi.impl.model.examples;

import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.UNKNOWN_ELEMENT_NAME;
import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.isAnnotationNull;
import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.mergeProperty;

import java.util.Map;

import org.eclipse.microprofile.openapi.annotations.media.ExampleObject;
import org.eclipse.microprofile.openapi.models.examples.Example;

import fish.payara.microprofile.openapi.impl.model.ExtensibleImpl;

public class ExampleImpl extends ExtensibleImpl<Example> implements Example {

    private String summary;
    private String description;
    private Object value;
    private String externalValue;
    private String ref;

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

    public static void merge(ExampleObject from, Example to, boolean override) {
        if (isAnnotationNull(from)) {
            return;
        }
        to.setSummary(mergeProperty(to.getSummary(), from.summary(), override));
        to.setDescription(mergeProperty(to.getDescription(), from.description(), override));
        to.setValue(mergeProperty(to.getValue(), from.value(), override));
        to.setExternalValue(mergeProperty(to.getExternalValue(), from.externalValue(), override));
    }

    public static void merge(ExampleObject example, Map<String, Example> examples, boolean override) {
        if (isAnnotationNull(example)) {
            return;
        }

        // Get the example name
        String exampleName = example.name();
        if (example.name() == null || example.name().isEmpty()) {
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