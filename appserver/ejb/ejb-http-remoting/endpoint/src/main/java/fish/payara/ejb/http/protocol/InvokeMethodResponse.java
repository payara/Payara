/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2019-2021 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.ejb.http.protocol;

import jakarta.json.bind.annotation.JsonbPropertyOrder;
import jakarta.json.bind.annotation.JsonbTypeDeserializer;
import jakarta.json.bind.serializer.DeserializationContext;
import jakarta.json.bind.serializer.JsonbDeserializer;
import jakarta.json.stream.JsonParser;
import java.io.Serializable;
import java.lang.reflect.Type;

/**
 * Result of invoking an EJB method.
 * 
 * @author Jan Bernitt
 */
@JsonbPropertyOrder({"type", "result"})
@JsonbTypeDeserializer(InvokeMethodResponse.Deserializer.class)
public class InvokeMethodResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Actual type of the result
     */
    public final String type;
    public final Object result;

    public InvokeMethodResponse(Object result) {
        this(result == null ? "" : result.getClass().getName(), result);
    }

    public InvokeMethodResponse(String type, Object result) {
        this.type = type;
        this.result = result;
    }

    // This deserializer is only fit for limited use cases. Client should employ different deserialization means,
    // that make use of the expected return type as well (especially to support Optional returns)
    public static class Deserializer implements JsonbDeserializer<InvokeMethodResponse> {

        @Override
        public InvokeMethodResponse deserialize(JsonParser parser, DeserializationContext ctx, Type rtType) {
            String type = null;
            Object result = null;
            while (parser.hasNext()) {
                JsonParser.Event event = parser.next();
                if (event == JsonParser.Event.KEY_NAME && parser.getString().equals("type")) {
                    parser.next();
                    type = parser.getString();
                } else if (event == JsonParser.Event.KEY_NAME && parser.getString().equals("result")) {
                    Class<?> resultClass = determineClass(type);
                    result = ctx.deserialize(resultClass, parser);
                }
            }
            return new InvokeMethodResponse(result);
        }

        private static Class<?> determineClass(String type) {
            if (type == null) {
                throw new IllegalArgumentException("Type was not specified");
            }
            try {
                return Class.forName(type);
            } catch (ClassNotFoundException e) {
                throw new IllegalArgumentException("Could not find class for type "+type, e);
            }
        }
    }
}
