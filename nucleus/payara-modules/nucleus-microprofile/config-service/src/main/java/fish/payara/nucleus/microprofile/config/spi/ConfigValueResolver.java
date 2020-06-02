/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2020 Payara Foundation and/or its affiliates. All rights reserved.
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

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

import org.eclipse.microprofile.config.spi.Converter;

/**
 * <p>
 * This is an abstraction to resolve {@link Config} values using a fluent API that is used
 * when the basic {@link Config#getValue(String, Class)} does not suffice
 * or the user wants to make sure a value is returned without an exception and excluding empty {@link String} values.
 * The method names are chosen for good readability when used as a fluent API.
 *
 * <p>
 * This API is designed so reliably result in a return value. This means by default it will not throw exceptions for
 * missing properties or failed conversion but instead return a default value that callers have to provide.
 * Alternatively to providing a default value a value can be resolved as {@link Optional}.
 *
 * <p>
 * Arrays of values can be resolved as {@link List} or {@link Set}.
 * By default these return empty lists or sets in case of missing property or failed conversion.
 *
 * <p>
 * Simple value properties defined as empty string by default are considered missing. This can be changed
 * using {@link #acceptEmpty()}.
 *
 * <p>
 * Should exceptions be thrown for either missing properties or failed conversion the default of not throwing exception
 * and using default values can be overridden using {@link #throwOnMissingProperty()} and
 * {@link #throwOnFailedConversion()}.
 *
 * <p>
 * Usually conversion relies on registered {@link org.eclipse.microprofile.config.spi.Converter}s.
 * Ad-hoc conversion can be done using {@link #asConvertedBy(Converter, Object)}.
 * Values resolved as {@link String} are not converted.
 * Values resolved as {@code String[]} are only split but elements are not converted.
 *
 * <p>
 * Typed defaults are provided with one of the {@code as}-methods.
 * Raw property string defaults can be provided using {@link #withDefault(String)}.
 * As the API can not capture if such a raw default has been provided it still requires to provide a typed default
 * or use {@link Optional} to ensure resolution will return a value.
 * If a raw {@link String} is provided it takes precedence over a typed default.
 * Should conversion of a raw default fail the typed default is used
 * or in case of {@link Optional} the property is considered not present.
 *
 * <p>
 * The only way {@code as}-methods might return a {@code null} reference is by using {@code null} as typed default.
 * In such case it is considered the intention of the caller and therefore not problematic.
 *
 * @author Jan Bernitt
 */
public interface ConfigValueResolver {

    /**
     * Provides a raw property value default value.
     * <p>
     * The raw default is considered to be on the source level and therefore takes precedence over typed defaults
     * provided to any of the {@code as}-methods. Should raw default be empty of fail to convert to the target type the
     * typed default is used.
     *
     * @param value raw default value, not {@code null}
     * @return This resolver for fluent API usage
     */
    ConfigValueResolver withDefault(String value);

    /**
     * Disables the default behaviour of considering properties defined as empty string as being not present (missing).
     * Effectively this means {@code as}-methods might then return empty things in case the property is defined as empty
     * in the source.
     *
     * @return This resolver for fluent API usage
     */
    ConfigValueResolver acceptEmpty();

    /**
     * Disables the default behaviour of not throwing exceptions and instead returning default values for case of
     * missing or empty raw value. If used in combination with {@link #acceptEmpty()} empty raw values will not throw an
     * exception, otherwise they do.
     *
     * @return This resolver for fluent API usage
     */
    ConfigValueResolver throwOnMissingProperty();

    /**
     * Disables the default behaviour of not throwing exceptions and instead returning default values for case of failed
     * conversion or missing converter for the target type.
     *
     * @return This resolver for fluent API usage
     */
    ConfigValueResolver throwOnFailedConversion();

    /**
     * Resolves the property as a simple or array type.
     * <p>
     * If the property is missing, defined empty, the required converter is missing or conversion fails the provided
     * default value is returned.
     *
     * @param type         target type of the conversion, not {@code null}
     * @param defaultValue any value including null. It is returned in case of missing property or failed conversion
     *                     unless throwing exceptions has been explicitly requested using
     *                     {@link #throwOnMissingProperty()} or {@link #throwOnFailedConversion()}.
     * @return the resolved and converted property value or the default value
     * @throws java.lang.IllegalArgumentException if the property cannot be converted to the specified type and throwing
     *         exceptions has been requested using {@link #throwOnFailedConversion()}
     * @throws java.util.NoSuchElementException if the property isn't present in the configuration and throwing
     *         exceptions has been requested using {@link #throwOnMissingProperty()}
     **/
    <T> T as(Class<T> type, T defaultValue);

    /**
     * Resolves the property as a simple or array type wrapped in an {@link Optional}.
     * <p>
     * If the property is missing, defined empty, the required converter is missing or conversion fails
     * {@link Optional#empty()} is returned.
     *
     * @param type         target type of the conversion, not {@code null}
     * @return the resolved and converted property value as present or empty {@link Optional}
     * @throws java.lang.IllegalArgumentException if the property cannot be converted to the specified type and throwing
     *         exceptions has been requested using {@link #throwOnFailedConversion()}
     * @throws java.util.NoSuchElementException if the property isn't present in the configuration and throwing
     *         exceptions has been requested using {@link #throwOnMissingProperty()}
     **/
    <T> Optional<T> as(Class<T> type);

    /**
     * Resolves the property as converted by the provided converter {@link Function}.
     * <p>
     * If the property is missing, defined empty or the conversion fails the provided default value is returned.
     * <p>
     * This is meant for ad-hoc conversions using lambda expressions as converter to circumvent the need to register a
     * special {@link org.eclipse.microprofile.config.spi.Converter} for single case use or to allow using different
     * converter functions for same target type at multiple usages.
     * <p>
     * If this method is used in connection with {@link #acceptEmpty()} the empty {@link String} might be passed to the
     * provided converter function, otherwise the empty string will be considered missing.
     *
     * @param type         target type of the conversion, not {@code null}
     * @param defaultValue any value including null. It is returned in case of missing property or failed conversion
     *                     unless throwing exceptions has been explicitly requested using
     *                     {@link #throwOnMissingProperty()} or {@link #throwOnFailedConversion()}.
     * @return the resolved and converted property value or the default value
     * @throws java.lang.IllegalArgumentException if the property cannot be converted to the specified type and throwing
     *         exceptions has been requested using {@link #throwOnFailedConversion()}
     * @throws java.util.NoSuchElementException if the property isn't present in the configuration and throwing
     *         exceptions has been requested using {@link #throwOnMissingProperty()}
     **/
    <T> T asConvertedBy(Function<String, T> converter, T defaultValue);

    /**
     * Resolves the property as {@link List}.
     * Raw value is split into elements as defined by {@link Config} for array types.
     * <p>
     * If the property is missing, defined empty, the required element type converter is missing or conversion fails an
     * empty list is returned. The returned list must not be considered modifiable.
     * <p>
     * This is equivalent to {@link #asList(Class, List)} with an empty list as default value.
     * <p>
     * This is equivalent to {@link #as(Class, Object)} with 1-dimensional array type of the provided element type as
     * type where the returned array is later wrapped in a {@link List} while also honouring defaults.
     *
     * @param elementType type of the list elements, not {@code null}
     * @return the resolved and converted property value or an empty list
     * @throws java.lang.IllegalArgumentException if the property cannot be converted to the specified type and throwing
     *         exceptions has been requested using {@link #throwOnFailedConversion()}
     * @throws java.util.NoSuchElementException if the property isn't present in the configuration and throwing
     *         exceptions has been requested using {@link #throwOnMissingProperty()}
     **/
    <E> List<E> asList(Class<E> elementType);

    /**
     * Resolves the property as {@link List}.
     * Raw value is split into elements as defined by {@link Config} for array types.
     * <p>
     * If the property is missing, defined empty, the required element type converter is missing or conversion fails the
     * provided default value is returned. The returned list must not be considered modifiable.
     * <p>
     * This is equivalent to {@link #as(Class, Object)} with 1-dimensional array type of the provided element type as
     * type where the returned array is later wrapped in a {@link List} while also honouring defaults.
     *
     * @param elementType  type of the list elements, not {@code null}
     * @param defaultValue any value including null. It is returned in case of missing property or failed conversion
     *                     unless throwing exceptions has been explicitly requested using
     *                     {@link #throwOnMissingProperty()} or {@link #throwOnFailedConversion()}.
     * @return the resolved and converted property value or an empty list
     * @throws java.lang.IllegalArgumentException if the property cannot be converted to the specified type and throwing
     *         exceptions has been requested using {@link #throwOnFailedConversion()}
     * @throws java.util.NoSuchElementException if the property isn't present in the configuration and throwing
     *         exceptions has been requested using {@link #throwOnMissingProperty()}
     **/
    <E> List<E> asList(Class<E> elementType, List<E> defaultValue);

    /**
     * Resolves the property as {@link Set}.
     * Raw value is split into elements as defined by {@link Config} for array types.
     * <p>
     * If the property is missing, defined empty, the required element type converter is missing or conversion fails an
     * empty set is returned. The returned set must not be considered modifiable.
     * <p>
     * This is equivalent to {@link #asSet(Class, Set)} with an empty set as default value.
     * <p>
     * This is equivalent to {@link #as(Class, Object)} with 1-dimensional array type of the provided element type as
     * type where the returned array is later wrapped in a {@link Set} while also honouring defaults.
     *
     * @param elementType type of the set elements, not {@code null}
     * @return the resolved and converted property value or an empty set
     * @throws java.lang.IllegalArgumentException if the property cannot be converted to the specified type and throwing
     *         exceptions has been requested using {@link #throwOnFailedConversion()}
     * @throws java.util.NoSuchElementException if the property isn't present in the configuration and throwing
     *         exceptions has been requested using {@link #throwOnMissingProperty()}
     **/
    <E> Set<E> asSet(Class<E> elementType);

    /**
     * Resolves the property as {@link Set}.
     * Raw value is split into elements as defined by {@link Config} for array types.
     * <p>
     * If the property is missing, defined empty, the required element type converter is missing or conversion fails an
     * empty set is returned. The returned set must not be considered modifiable.
     * <p>
     * This is equivalent to {@link #asSet(Class, Set)} with an empty set as default value.
     * <p>
     * This is equivalent to {@link #as(Class, Object)} with 1-dimensional array type of the provided element type as
     * type where the returned array is later wrapped in a {@link Set} while also honouring defaults.
     *
     * @param elementType  type of the set elements, not {@code null}
     * @param defaultValue any value including null. It is returned in case of missing property or failed conversion
     *                     unless throwing exceptions has been explicitly requested using
     *                     {@link #throwOnMissingProperty()} or {@link #throwOnFailedConversion()}.
     * @return the resolved and converted property value or an empty set
     * @throws java.lang.IllegalArgumentException if the property cannot be converted to the specified type and throwing
     *         exceptions has been requested using {@link #throwOnFailedConversion()}
     * @throws java.util.NoSuchElementException if the property isn't present in the configuration and throwing
     *         exceptions has been requested using {@link #throwOnMissingProperty()}
     **/
    <E> Set<E> asSet(Class<E> elementType, Set<E> defaultValue);

    /**
     * Consume successfully converted element values for multi-value properties.
     *
     * Resolves the property as if resolving an array of the given element type and passing each of them to the provided
     * {@link Consumer} action. Raw value is split into elements as defined by {@link Config} for array types.
     * <p>
     * If the property is missing, defined empty, the required element type converter is missing the action is not called at all.
     * If conversion fails for an element that element is skipped and not passed to the action.
     * Other elements are still converted.
     * <p>
     * This method might also help avoid creation of intermediate collections to some degree.
     * Implementations should attempt to avoid such intermediate collections where possible within reason.
     *
     * @param elementType type to be consumed by the provided action, not {@code null}
     * @param action      The action to be performed for each element, not {@code null}
     * @throws java.lang.IllegalArgumentException if the property cannot be converted to the specified type and throwing
     *         exceptions has been requested using {@link #throwOnFailedConversion()}
     * @throws java.util.NoSuchElementException if the property isn't present in the configuration and throwing
     *         exceptions has been requested using {@link #throwOnMissingProperty()}
     **/
    <E> void forEach(Class<E> elementType, Consumer<? super E> action);
}
