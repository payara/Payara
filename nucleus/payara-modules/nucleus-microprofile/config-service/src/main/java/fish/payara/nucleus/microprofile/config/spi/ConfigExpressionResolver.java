/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2021] Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.nucleus.microprofile.config.spi;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.microprofile.config.spi.ConfigSource;

public class ConfigExpressionResolver {

    private final Iterable<ConfigSource> sources;

    private final Set<String> resolvingExpressions;

    public ConfigExpressionResolver(Iterable<ConfigSource> sources) {
        this.sources = sources;
        this.resolvingExpressions = new HashSet<>();
    }

    public ConfigValueImpl resolve(String propertyName) {
        return resolve(propertyName, null);
    }

    public ConfigValueImpl resolve(String propertyName, String propertyDefault) {

        String resolvedPropertyName = resolveExpression(propertyName);

        for (ConfigSource source : sources) {
            final String result = source.getValue(resolvedPropertyName);
            if (result != null && !result.isEmpty()) {
                return new ConfigValueImpl(
                    resolvedPropertyName,
                    result,
                    resolveExpression(result),
                    source.getName(),
                    source.getOrdinal()
                );
            }
        }

        return new ConfigValueImpl(resolvedPropertyName, propertyDefault, resolveExpression(propertyDefault), null, 0);
    }

    private synchronized String resolveExpression(String expression) {
        if (expression == null) {
            return null;
        }

        if (resolvingExpressions.contains(expression)) {
            throw new IllegalArgumentException("Infinitely recursive expression found within expression: " + expression);
        }
        
        String result = "";

        try {
            resolvingExpressions.add(expression);

            final char[] characters = expression.toCharArray();

            String expressionBuilder = "";
            String expressionDefaultBuilder = "";

            boolean isExpression = false;
            boolean isDefaultValue = false;
            int bracketDepth = 0;
            for (int i = 0; i < characters.length; i++) {
                final char c = characters[i];

                // Configure the context if expression markers are found
                if (c == ':' && bracketDepth == 1) {
                    isDefaultValue = true;
                    continue;
                } else if (isExpression) {
                    if (c == '{' && bracketDepth++ == 0) {
                        continue;
                    } else if (c == '}' && bracketDepth-- == 1) {
                        isDefaultValue = false;
                        isExpression = false;
                    }
                } else if (c == '$' && characters[i + 1] == '{' && (i == 0 || characters[i - 1] != '\\')) {
                    isExpression = true;
                    continue;
                }

                // React to the given character (given the context)
                if (isDefaultValue) {
                    expressionDefaultBuilder += c;
                } else if (isExpression) {
                    expressionBuilder += c;
                } else if (expressionBuilder.isEmpty()) {
                    result += c;
                } else {
                    // If the expression has ended, resolve the expression and clear the context
                    final String resolvedExpression = resolve(expressionBuilder, expressionDefaultBuilder).getValue();
                    expressionBuilder = "";
                    expressionDefaultBuilder = "";
                    if (resolvedExpression != null) {
                        result += resolvedExpression;
                    }
                }
            }
        } finally {
            resolvingExpressions.remove(expression);
        }

        return result;
    }

}
