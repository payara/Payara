/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2017-2021 Payara Foundation and/or its affiliates. All rights reserved.
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

import fish.payara.nucleus.microprofile.config.spi.ConfigValueResolver;
import fish.payara.nucleus.microprofile.config.spi.InjectedPayaraConfig;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.spi.InjectionPoint;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.glassfish.api.invocation.InvocationManager;
import org.glassfish.internal.api.Globals;

/**
 * Class that provides a couple of basic producer methods used for injection of Config
 * and the injection of Optional properties.
 * @author Steve Millidge <Payara Services Limited>
 */
@Dependent
public class ConfigProducer {

    private InvocationManager im;

    @PostConstruct
    public void postConstruct() {
        im = Globals.getDefaultHabitat().getService(InvocationManager.class);
    }

    /**
     * Producer method for the application config
     * @return The Config object registered for this application
     */
    @Produces
    public Config getConfig() {
        return new InjectedPayaraConfig(ConfigProvider.getConfig(), im.getCurrentInvocation().getAppName());
    }

    /**
     * Producer method for Sets
     * @param <T> Type
     * @param ip Injection Point
     * @return
     */
    @Produces
    @ConfigProperty
    public <T> Set<T> getSetProperty(InjectionPoint ip) {
        ConfigProperty property = ip.getAnnotated().getAnnotation(ConfigProperty.class);
        Config config = ConfigProvider.getConfig();
        Type type = ip.getType();
        if (type instanceof ParameterizedType) {
         // it is an List, get the element type of the List
            @SuppressWarnings("unchecked")
            Class<T> elementType = (Class<T>) ((ParameterizedType) type).getActualTypeArguments()[0];
            String defaultValue = property.defaultValue();
            return config.getValue(property.name(), ConfigValueResolver.class)
                    .throwOnMissingProperty(defaultValue == null)
                    .throwOnFailedConversion()
                    .withDefault(defaultValue)
                    .asSet(elementType);
        }
        return new HashSet<>();
    }

    /**
     * Producer method for Lists
     * @param <T> Type
     * @param ip Injection Point
     * @return
     */
    @Produces
    @ConfigProperty
    public <T> List<T> getListProperty(InjectionPoint ip) {
        ConfigProperty property = ip.getAnnotated().getAnnotation(ConfigProperty.class);
        Config config = ConfigProvider.getConfig();
        Type type = ip.getType();
        if (type instanceof ParameterizedType) {
            // it is an List, get the element type of the List
            @SuppressWarnings("unchecked")
            Class<T> elementType = (Class<T>) ((ParameterizedType) type).getActualTypeArguments()[0];
            String defaultValue = property.defaultValue();
            return config.getValue(property.name(), ConfigValueResolver.class)
                    .throwOnMissingProperty(defaultValue == null)
                    .throwOnFailedConversion()
                    .withDefault(defaultValue)
                    .asList(elementType);
        }
        return new ArrayList<>();
    }

    /**
     * Produces an Optional for the property specified by the ConfigProperty
     * and of the type specified
     * @param <T>
     * @param ip
     * @return
     */
    @Produces
    @ConfigProperty
    public <T> Optional<T> getOptionalProperty(InjectionPoint ip) {
        ConfigProperty property = ip.getAnnotated().getAnnotation(ConfigProperty.class);
        Config config = ConfigProvider.getConfig();

        Type type = ip.getType();
        if (type instanceof ParameterizedType) {
            // it is an Optional
            // get the class of the generic parameterized Optional
            @SuppressWarnings("unchecked")
            Class<T> valueType = (Class<T>) ((ParameterizedType) type).getActualTypeArguments()[0];

            String defaultValue = property.defaultValue();
            return config.getValue(property.name(), ConfigValueResolver.class)
                    .throwOnFailedConversion()
                    .withDefault(defaultValue)
                    .as(valueType);
        }
        return Optional.empty();
    }

    /**
     * Produces an Supplier for the property specified by the ConfigProperty
     * and of the type specified
     * @param <T>
     * @param ip
     * @return
     */
    @Produces
    @ConfigProperty
    public <T> Supplier<T> getPropertySupplier(InjectionPoint ip) {
        ConfigProperty property = ip.getAnnotated().getAnnotation(ConfigProperty.class);
        Config config = ConfigProvider.getConfig();

        Type type = ip.getType();
        if (type instanceof ParameterizedType) {
            // it is an Optional
            // get the class of the generic parameterized Optional
            @SuppressWarnings("unchecked")
            Class<T> valueType = (Class<T>) ((ParameterizedType) type).getActualTypeArguments()[0];

            String defaultValue = property.defaultValue();
            return config.getValue(property.name(), ConfigValueResolver.class)
                    .throwOnFailedConversion()
                    .withDefault(defaultValue)
                    .asSupplier(valueType);
        }

        // Should never be called
        return () -> null;
    }
}
