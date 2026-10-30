/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2019-2022] Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.microprofile.openapi.test.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fish.payara.microprofile.openapi.impl.model.OpenApiBuilderTest;
import java.util.Arrays;
import org.eclipse.microprofile.openapi.models.Constructible;
import static org.junit.Assert.fail;

public class JsonUtils {

    public static JsonNode path(JsonNode root, String path) {
        return path(root, path.split("\\."));
    }

    public static JsonNode path(JsonNode root, String... pathElements) {
        JsonNode current = root;
        if (pathElements == null || pathElements.length == 0) {
            return root;
        }
        for (int i = 0; i < pathElements.length; i++) {
            String nameOrIndex = pathElements[i];
            if (current != null) {
                if (nameOrIndex.startsWith("[") && nameOrIndex.endsWith("]")) {
                    current = current.get(Integer.parseInt(nameOrIndex.substring(1, nameOrIndex.length() - 1)));
                } else {
                    current = current.get(nameOrIndex);
                }
            }
            if (current == null) {
                String subPath = String.join(".", Arrays.copyOf(pathElements, i)) + ".??? " + pathElements[i] + " ???";
                fail("Missing path `" + String.join(".", pathElements) + "` at `" + subPath + "` in :\n"
                        + prettyPrint(root));
            }
        }
        return current;
    }

    public static String prettyPrint(JsonNode node) {
        try {
            return new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(node);
        } catch (JsonProcessingException e) {
            return String.valueOf(node); // best we can do
        }
    }

    public static ObjectNode toJson(Constructible node) {
        return OpenApiBuilderTest.JSON_MAPPER.valueToTree(node);
    }

    public static boolean hasPath(JsonNode root, String... pathElements) {
        JsonNode current = root;
        if (pathElements == null || pathElements.length == 0) {
            return false;
        }
        for (int i = 0; i < pathElements.length; i++) {
            String nameOrIndex = pathElements[i];
            if (current != null) {
                if (nameOrIndex.startsWith("[") && nameOrIndex.endsWith("]")) {
                    current = current.get(Integer.parseInt(nameOrIndex.substring(1, nameOrIndex.length() - 1)));
                } else {
                    current = current.get(nameOrIndex);
                }
            }
            if (current == null) {
                return false;
            }
        }
        return true;
    }
}
