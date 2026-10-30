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
package fish.payara.microprofile.config.cdi;

import static org.eclipse.microprofile.config.inject.ConfigProperties.UNCONFIGURED_PREFIX;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.enterprise.inject.spi.Annotated;
import jakarta.enterprise.inject.spi.AnnotatedField;
import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.InjectionPoint;

import org.eclipse.microprofile.config.inject.ConfigProperties;

import fish.payara.microprofile.config.cdi.model.ConfigPropertyModel;

public class ConfigPropertiesProducer {

    private static final Logger LOGGER = Logger.getLogger(ConfigPropertiesProducer.class.getName());

    @ConfigProperties
    public static final Object getGenericObject(InjectionPoint injectionPoint, BeanManager bm)
            throws InstantiationException, IllegalAccessException {
        Type type = injectionPoint.getType();
        if (!(type instanceof Class)) {
            throw new IllegalArgumentException("Unable to process injection point with @ConfigProperties of type " + type);
        }

        // Initialise the object. This may throw exceptions
        final Object object = ((Class) type).newInstance();

        // Model the class
        final AnnotatedType<?> annotatedType = bm.createAnnotatedType((Class) type);

        // Find the @ConfigProperties annotations, and calculate the property prefix
        final ConfigProperties injectionAnnotation = getQualifier(injectionPoint);
        final ConfigProperties classAnnotation = annotatedType.getAnnotation(ConfigProperties.class);
        final String prefix = parsePrefixes(injectionAnnotation, classAnnotation);

        for (AnnotatedField<?> field : annotatedType.getFields()) {

            // Find the java field and field name
            final Field javaField = field.getJavaMember();

            // Make sure the field is accessible
            javaField.setAccessible(true);

            // Model the field
            final InjectionPoint fieldInjectionPoint = bm.createInjectionPoint(field);
            final ConfigPropertyModel model = new ConfigPropertyModel(fieldInjectionPoint, prefix);

            try {
                final Object value = ConfigPropertyProducer.getGenericPropertyFromModel(model);

                if (value != null) {
                    javaField.set(object, value);
                }
            } catch (Exception ex) {
                if (javaField.get(object) == null) {
                    LOGGER.log(Level.WARNING, String.format("Unable to inject property with name %s into type %s.",
                            model.getName(), type.getTypeName()), ex);
                    throw ex;
                }
            }
        }

        return object;
    }

    private static ConfigProperties getQualifier(InjectionPoint injectionPoint) {

        // If it's an @Inject point
        final Annotated annotated = injectionPoint.getAnnotated();
        if (annotated != null) {
            return annotated.getAnnotation(ConfigProperties.class);
        }

        // If it's a programmatic lookup
        final Set<Annotation> qualifiers = injectionPoint.getQualifiers();
        for (Annotation qualifier : qualifiers) {
            if (qualifier instanceof ConfigProperties) {
                return (ConfigProperties) qualifier;
            }
        }

        return null;
    }

    private static String parsePrefixes(ConfigProperties injectionAnnotation, ConfigProperties classAnnotation) {
        final String injectionPrefix = parsePrefix(injectionAnnotation);
        if (injectionPrefix != null) {
            return injectionPrefix;
        }
        return parsePrefix(classAnnotation);
    }

    private static String parsePrefix(ConfigProperties annotation) {
        if (annotation == null) {
            return null;
        }
        final String value = annotation.prefix();
        if (value == null || value.equals(UNCONFIGURED_PREFIX)) {
            return null;
        }
        if (value.isEmpty()) {
            return "";
        }
        return value + ".";
    }

}
