/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2017-2020 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.microprofile.config.cdi;

import fish.payara.nucleus.microprofile.config.spi.ConfigValueResolver;

import static fish.payara.nucleus.microprofile.config.spi.ConfigValueResolver.ElementPolicy.FAIL;

import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.Set;
import java.util.function.Supplier;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.DeploymentException;
import javax.enterprise.inject.spi.InjectionPoint;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.inject.ConfigProperties;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * This class is used as the template for synthetic beans when creating a bean
 * for each converter type registered in the Config
 * @author Steve Millidge (Payara Foundation)
 */
public class ConfigPropertyProducer {

    // FIXME: refactor
    // This currently just takes a generic @ConfigProperties bean and attempts
    // to initialise it. Field injection may be performed here as opposed
    // to in the extension. Dealer's choice
    @ConfigProperties
    @Dependent
    public static final Object getGenericObject(InjectionPoint ip)
            throws InstantiationException, IllegalAccessException {
        final Field field = (Field) ip.getMember();
        final Class<?> objectClass = field.getType();
        return objectClass.newInstance();
    }

    /**
     * General producer method for injecting a property into a field annotated
     * with the @ConfigProperty annotation.
     * Note this does not have @Produces annotation as a synthetic bean using this method
     * is created in the CDI Extension.
     * @param ip
     * @return
     */
    @ConfigProperty
    @Dependent
    public static final Object getGenericProperty(InjectionPoint ip) {
        Object result = null;
        ConfigProperty property = ip.getAnnotated().getAnnotation(ConfigProperty.class);
        Config config = ConfigProvider.getConfig();

        String name = property.name();
        if (name.isEmpty()) {
            // derive the property name from the injection point
            Class<?> beanClass = null;
            Bean<?> bean = ip.getBean();
            if (bean == null) {
                Member member = ip.getMember();
                beanClass = member.getDeclaringClass();
            } else {
                beanClass = bean.getBeanClass();
            }
            StringBuilder sb = new StringBuilder(beanClass.getCanonicalName());
            sb.append('.');
            sb.append(ip.getMember().getName());
            name =  sb.toString();
        }

        Type type = ip.getType();
        String defaultValue = property.defaultValue();
        if (type instanceof Class) {
            if (type == OptionalDouble.class) {
                result = config.getValue(property.name(), ConfigValueResolver.class)
                    .throwOnFailedConversion()
                    .withDefault(property.defaultValue())
                    .as(OptionalDouble.class)
                    .orElse(OptionalDouble.empty());
            } else if (type == OptionalInt.class) {
                result = config.getValue(property.name(), ConfigValueResolver.class)
                    .throwOnFailedConversion()
                    .withDefault(property.defaultValue())
                    .as(OptionalInt.class)
                    .orElse(OptionalInt.empty());
            } else if (type == OptionalLong.class) {
                result = config.getValue(property.name(), ConfigValueResolver.class)
                    .throwOnFailedConversion()
                    .withDefault(property.defaultValue())
                    .as(OptionalLong.class)
                    .orElse(OptionalLong.empty());
            } else {
                result = config.getValue(name, ConfigValueResolver.class)
                    .throwOnMissingProperty(defaultValue == null)
                    .throwOnFailedConversion()
                    .withDefault(defaultValue)
                    .withPolicy(FAIL)
                    .as((Class<?>)type)
                    .get();
            }
        } else if (type instanceof ParameterizedType) {
            ParameterizedType ptype = (ParameterizedType)type;
            Type rawType = ptype.getRawType();
            if (List.class.equals(rawType)) {
                result = config.getValue(name, ConfigValueResolver.class)
                    .throwOnMissingProperty(defaultValue == null)
                    .throwOnFailedConversion()
                    .withDefault(defaultValue)
                    .withPolicy(FAIL)
                    .asList(getElementTypeFrom(ptype));
            } else if (Set.class.equals(rawType)) {
                result = config.getValue(name, ConfigValueResolver.class)
                    .throwOnMissingProperty(defaultValue == null)
                    .throwOnFailedConversion()
                    .withDefault(defaultValue)
                    .withPolicy(FAIL)
                    .asSet(getElementTypeFrom(ptype));
            } else if (Supplier.class.equals(rawType)) {
                result = config.getValue(name, ConfigValueResolver.class)
                    .throwOnMissingProperty(defaultValue == null)
                    .throwOnFailedConversion()
                    .withDefault(defaultValue)
                    .withPolicy(FAIL)
                    .asSupplier(getElementTypeFrom(ptype));
            } else {
                result = config.getValue(name, (Class<?>) rawType);
            }
        }

        if (result == null) {
            throw new DeploymentException("Microprofile Config Property " + property.name() + " can not be found");
        }
        return result;
    }

    private static Class<?> getElementTypeFrom(ParameterizedType collectionType) {
        Type elementType = collectionType.getActualTypeArguments()[0];
        if (!(elementType instanceof Class)) {
            throw new DeploymentException(
                    "Only config values of lists and sets of non generic types (Class types) are supported but found: "
                            + collectionType);
        }
        return (Class<?>) elementType;
    }

}
