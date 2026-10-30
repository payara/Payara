/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2020-2021 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.nucleus.microprofile.config.converters;

import java.lang.reflect.Array;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.microprofile.config.spi.Converter;

import fish.payara.nucleus.microprofile.config.spi.ConfigValueResolver;

/**
 * Converts reference and primitive arrays.
 *
 * Argument for {@link Converter} can only be {@link Object} as both reference and primitive arrays are created.
 *
 * @param <T> element type of the array, can be both a primitive or a reference type
 */
public final class ArrayConverter<T> implements Converter<Object> {

    private static final Logger LOGGER = Logger.getLogger(ArrayConverter.class.getName());

    private final Class<T> elementType;
    private final Converter<T> elementConverter;
    private final ConfigValueResolver.ElementPolicy elementPolicy;

    public ArrayConverter(Class<T> elementType, Converter<T> elementConverter) {
        this(elementType, elementConverter, ConfigValueResolver.ElementPolicy.FAIL);
    }

    public ArrayConverter(Class<T> elementType, Converter<T> elementConverter, ConfigValueResolver.ElementPolicy elementPolicy) {
        this.elementType = elementType;
        this.elementConverter = elementConverter;
        this.elementPolicy = elementPolicy;
    }

    @Override
    public Object convert(String value) {
        String[] sourceValues = splitValue(value);
        Object array = Array.newInstance(elementType, sourceValues.length);
        int j = 0;
        IllegalArgumentException lastConversionException = null;
        for (String sourceValue : sourceValues) {
            if (sourceValue != null && !sourceValue.isEmpty()) {
                try {
                    T elementValue = elementConverter.convert(sourceValue);
                    Array.set(array, j++, elementValue);
                } catch (IllegalArgumentException ex) {
                    if (elementPolicy == ConfigValueResolver.ElementPolicy.FAIL)
                        throw ex;
                    if (elementPolicy == ConfigValueResolver.ElementPolicy.NULL) {
                        if (elementType.isPrimitive()) {
                            throw ex; // null is not allowed => fail
                        }
                        Array.set(array, j++, null);
                    } else {
                        // ignore that value but remember exception
                        lastConversionException = ex;
                    }
                }
            }
        }
        if (j == sourceValues.length) {
            return array;
        }
        if (lastConversionException != null) {
            if (j == 0) { // all failed, also fail
                throw lastConversionException;
            }
            LOGGER.log(Level.WARNING,
                    "Souce value defined a list with illegal elements which are ignored: " + value,
                    lastConversionException);
        } else {
            LOGGER.warning("Souce value defined a list with empty elements which are ignored: " + value);
        }
        Object copy = Array.newInstance(elementType, j);
        System.arraycopy(array, 0, copy, 0, j);
        return copy;
    }

    private static String[] splitValue(String value) {
        String keys[] = value.split("(?<!\\\\),");
        for (int i=0; i < keys.length; i++) {
            keys[i] = keys[i].replaceAll("\\\\,", ",");
        }
        return keys;
    }
}