/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2021-2023] Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.nucleus.microprofile.config.spi;

import fish.payara.nucleus.microprofile.config.util.ConfigValueType;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.glassfish.config.support.TranslatedConfigView;

import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Set;
import org.jboss.logging.Logger;

final class ConfigExpressionResolver {
    
    private static final Logger log = Logger.getLogger(ConfigExpressionResolver.class);

    private final Iterable<ConfigSource> sources;

    private final boolean expansionEnabled;

    private final Set<String> resolvingExpressions;

    private final String profile;

    protected ConfigExpressionResolver(Iterable<ConfigSource> sources) {
        this(sources, true, null);
    }

    protected ConfigExpressionResolver(Iterable<ConfigSource> sources, String profile) {
        this(sources, true, profile);
    }

    protected ConfigExpressionResolver(Iterable<ConfigSource> sources, boolean expansionEnabled) {
        this(sources, expansionEnabled, null);
    }

    protected ConfigExpressionResolver(Iterable<ConfigSource> sources, boolean expansionEnabled, String profile) {
        this.sources = sources;
        this.expansionEnabled = expansionEnabled;
        this.resolvingExpressions = new HashSet<>();
        this.profile = profile;
    }

    protected ConfigValueImpl resolve(String propertyName) {
        return resolve(propertyName, null, ConfigValueType.NORMAL);
    }

    protected ConfigValueImpl resolve(String propertyName, String propertyDefault) {
        return resolve(propertyName, propertyDefault, ConfigValueType.NORMAL);
    }

    protected ConfigValueImpl resolve(String propertyName, String propertyDefault, ConfigValueType type) {
        return resolve(propertyName, propertyDefault, false, type);
    }

    private ConfigValueImpl resolve(String propertyName, String propertyDefault, boolean resolveDefault, ConfigValueType type) {

        String translated = TranslatedConfigView.expandConfigValue(propertyName);
        if (!translated.equals(propertyName)) {
            return new ConfigValueImpl(
                    propertyName,
                    translated,
                    resolveExpression(translated),
                    "TranslatedConfigView",
                    0
            );
        }
        
        String profiledPropertyName = resolveExpression((profile == null ? "" : "%" + profile + ".") + propertyName);
        ConfigValueImpl result = getValue(profiledPropertyName);
        
        if(profile != null && result != null) {
            ConfigValueImpl resultWithoutProfile = getValue(resolveExpression(propertyName));
            // Note: In case there is a non-profiled value from a source with the same ordinal value, it will be ignored.
            //       All spec versions including v3.1 do not include a definition for this edge case -
            //       all sources are supposed to have a unique ordinal value.
            if (resultWithoutProfile != null && resultWithoutProfile.getSourceOrdinal() > result.getSourceOrdinal()) {
                result = resultWithoutProfile;
            }
        } 
        

        if (result == null) {
            String resolvedPropertyName = resolveExpression(propertyName);
            result = getValue(resolvedPropertyName);
        }

        if (result == null) {
            result = new ConfigValueImpl(profiledPropertyName, propertyDefault,
                    resolveDefault ? resolveExpression(propertyDefault, type) : propertyDefault, null, 0);
        }
        return result;
    }

    private ConfigValueImpl getValue(String propertyName) {
        for (ConfigSource source : sources) {
            final String result = source.getValue(propertyName);
            if (result != null && !result.isEmpty()) {
                String resolvedExpression = null;
                try {
                    resolvedExpression = resolveExpression(result);
                } catch(NoSuchElementException noSuchElementException) {
                    log.warn(String.format("Using null value in configuration, expression %s", result));
                }
                return new ConfigValueImpl(
                    propertyName,
                        result,
                        resolvedExpression,
                        source.getName(),
                        source.getOrdinal()
                );
            }
        }
        return null;
    }


    private synchronized String resolveExpression(String expression) {
        return resolveExpression(expression, ConfigValueType.NORMAL);
    }

    private synchronized String resolveExpression(String expression, ConfigValueType type) {
        if (expression == null) {
            return null;
        }

        if (!expansionEnabled) {
            return expression;
        }

        if (resolvingExpressions.contains(expression)) {
            throw new IllegalArgumentException("Infinitely recursive expression found within expression: " + expression);
        }

        String result = "";

        try {
            resolvingExpressions.add(expression);

            final char[] characters = expression.toCharArray();

            // These two variables store if the current character is part of
            // an expression that will need resolving, and the contents of that expression.
            // This essentially acts as a buffer that can be cleared when the expression is complete,
            // then added to the result
            boolean isExpression = false;
            String expressionBuilder = "";

            // These two variables perform a similar buffer function, but for the fallback if
            // the above expression fails to be resolved
            boolean isDefaultValue = false;
            String expressionDefaultBuilder = "";
            boolean defaultValueFound = false;

            // Counts the depth of brackets, to help discern when an expression has actually ended
            // I.e. without this variable, nested closing braces will cause the expression to close early
            // this is the reason that RegEx can't be used
            int bracketDepth = 0;

            for (int i = 0; i < characters.length; i++) {
                final char c = characters[i];

                // Configure the context if expression markers are found
                if (c == ':' && bracketDepth == 1) {
                    // Start building the default (only accept colons outside of any nested expressions)
                    isDefaultValue = true;
                    defaultValueFound = true;
                    continue;
                } else if (bracketDepth == 0 && isExpressionStart(characters, i)) {
                    // Ignore starting $ symbols
                    continue;
                } else if (isExpressionStart(characters, i - 1) && bracketDepth++ == 0) {
                    // Start the expression (only if the expression isn't nested)
                    isExpression = true;
                    continue;
                } else if (isExpression && isExpressionEnd(characters, i) && bracketDepth-- == 1) {
                    // End the expression (only if the expression isn't nested)
                    isDefaultValue = false;
                    isExpression = false;
                } else if (isCharacterEscaped(characters, i + 1) && !isCharacterEscaped(characters, i)) {
                    continue;
                }

                // React to the given character (given the previously calculated context)
                if (isDefaultValue) {
                    expressionDefaultBuilder += c;
                } else if (isExpression) {
                    expressionBuilder += c;
                } else if (expressionBuilder.isEmpty()) {
                    result += c;
                } else {
                    // If the expression has ended, resolve the expression
                    final String resolvedExpression = resolve(expressionBuilder, expressionDefaultBuilder, true, type).getValue();

                    // Clear the buffers
                    expressionBuilder = "";
                    expressionDefaultBuilder = "";

                    // Append the expression to the result, to continue processing the rest
                    if (resolvedExpression != null) {
                        result += resolvedExpression;
                    }

                    if ((result.isEmpty() && !defaultValueFound) && type == ConfigValueType.NORMAL) {
                        throw new NoSuchElementException("Unable to resolve expression " + expression);
                    }
                }
            }
        } finally {
            resolvingExpressions.remove(expression);
        }

        return result;
    }

    /**
     * @param characters a array of characters
     * @param index the index of the character to test
     * @return if the character at the given index marks the '$' at the beginning of an expression
     */
    private static boolean isExpressionStart(final char[] characters, final int index) {
        return index >= 0
            && index + 1 < characters.length
            && characters[index] == '$'
            && characters[index + 1] == '{'
            && !isCharacterEscaped(characters, index);
    }

    /**
     * @param characters an array of characters
     * @param index the index of the character to test
     * @return if the character at the given index marks the '}' to close an expression
     */
    private static boolean isExpressionEnd(final char[] characters, final int index) {
        return index >= 0
            && index < characters.length
            && characters[index] == '}'
            && !isCharacterEscaped(characters, index);
    }

    /**
     * @param characters an array of characters
     * @param index the index of the character to test
     * @return if the character at the given index is escaped
     */
    private static boolean isCharacterEscaped(final char[] characters, final int index) {
        if (index == 0 || index >= characters.length) {
            return false;
        }

        final char c = characters[index];
        final boolean backslashFound = characters[index - 1] == '\\';

        if (!backslashFound) {
            return false;
        }

        // Only allow certain characters to be escaped. This is so that, for example, the array converter still receives
        // the expected escape characters
        switch (c) {
            case '$':
            case '{':
            case '}':
                return true;
            default:
                return false;
        }
    }

}
