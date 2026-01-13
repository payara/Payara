/*
 *
 * Copyright (c) 2026 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.tools.dev.resources;

import fish.payara.tools.dev.model.DecoratedClassInfo;
import fish.payara.tools.dev.model.InterceptedClassInfo;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author Gaurav Gupta
 */
public class AbstractSummarySupport extends AbstractConsoleServlet {

    protected List<InterceptedClassInfo> interceptorSummary(Map<String, List<Class<?>>> chains) {

        Map<String, Map<String, List<Class<?>>>> grouped = groupByClass(chains);

        List<InterceptedClassInfo> result = new ArrayList<>();

        grouped.forEach((className, methodMap) -> {

            // remove empty chains BEFORE processing
            Map<String, List<Class<?>>> nonEmpty
                    = methodMap.entrySet().stream()
                            .filter(e -> e.getValue() != null && !e.getValue().isEmpty())
                            .collect(LinkedHashMap::new,
                                    (m, e) -> m.put(e.getKey(), e.getValue()),
                                    LinkedHashMap::putAll);

            // If nothing left after removing empties → skip entire class
            if (nonEmpty.isEmpty()) {
                return;
            }

            // Only method-level entries (methodName != null)
            Map<String, List<Class<?>>> methodOnly
                    = nonEmpty.entrySet().stream()
                            .filter(e -> e.getKey() != null)
                            .collect(LinkedHashMap::new,
                                    (m, e) -> m.put(e.getKey(), e.getValue()),
                                    LinkedHashMap::putAll);

            // CASE A: no method-level entries → class-only chain
            if (methodOnly.isEmpty()) {
                List<Class<?>> classChain = nonEmpty.values().iterator().next();
                result.add(new InterceptedClassInfo(className, toNames(classChain)));
                return;
            }

            // Distinct interceptor sets across methods
            Set<List<Class<?>>> distinct = new HashSet<>(methodOnly.values());

            // CASE B: all methods share same chain → class-only
            if (distinct.size() == 1) {
                List<Class<?>> chain = distinct.iterator().next();
                result.add(new InterceptedClassInfo(className, toNames(chain)));
                return;
            }

            // CASE C: methods differ → per-method output
            methodOnly.forEach((methodName, chain) -> {
                result.add(new InterceptedClassInfo(
                        className + "#" + methodName,
                        toNames(chain)
                ));
            });
        });

        return result;
    }

    protected List<DecoratedClassInfo> decoratorSummary(Map<String, List<Class<?>>> chains) {

        List<DecoratedClassInfo> result = new ArrayList<>();

        chains.forEach((className, chain) -> {

            // skip empty
            if (chain == null || chain.isEmpty()) {
                return;
            }

            // decorators apply to whole class only
            result.add(new DecoratedClassInfo(
                    className,
                    toNames(chain)
            ));
        });

        return result;
    }

    protected Map<String, Map<String, List<Class<?>>>> groupByClass(Map<String, List<Class<?>>> chains) {
        Map<String, Map<String, List<Class<?>>>> grouped = new HashMap<>();

        chains.forEach((key, chain) -> {
            String className;
            String methodName = null;

            int idx = key.indexOf('#');
            if (idx > 0) {
                className = key.substring(0, idx);
                methodName = key.substring(idx + 1);
            } else {
                className = key;
            }

            grouped
                    .computeIfAbsent(className, k -> new LinkedHashMap<>())
                    .put(methodName, chain); // methodName = null means class-level
        });

        return grouped;
    }

    protected List<String> toNames(List<Class<?>> chain) {
        return chain.stream().map(Class::getName).toList();
    }

}
