/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2020] Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.microprofile.config.cdi.model;

import java.lang.reflect.Member;

import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionPoint;

import org.eclipse.microprofile.config.inject.ConfigProperties;
import org.eclipse.microprofile.config.inject.ConfigProperty;

// FIXME: this is currently unutilised, but may be needed to aid in fixing TCK failures.
// It currently essentially abstracts the direct annotation reading performed in each producer
// method.
public final class ConfigPropertyModel {

    private final InjectionPoint injectionPoint;

    private final String name;
    private final String defaultValue;

    public ConfigPropertyModel(InjectionPoint injectionPoint) {
        this.injectionPoint = injectionPoint;

        final ConfigProperty propAnnotation = injectionPoint.getAnnotated().getAnnotation(ConfigProperty.class);

        assert propAnnotation != null;
        
        this.name = parseName(injectionPoint, propAnnotation.name());
        this.defaultValue = propAnnotation.defaultValue();
    }

    public InjectionPoint getInjectionPoint() {
        return injectionPoint;
    }

    public String getName() {
        return name;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    private static final String parseName(InjectionPoint injectionPoint, String propName) {

        String name = "";

        // FIXME: not sure how correct this is. This is the only step in logic
        // that is new in nature and not currently present in each producer
        // Add the prefix from the element
        final Annotated element = injectionPoint.getAnnotated();
        if (element.isAnnotationPresent(ConfigProperties.class)) {
            name += getPrefix(element.getAnnotation(ConfigProperties.class));
        } else {
            // Search for a prefix on the class
            final Class<?> declaringClass = injectionPoint.getMember().getDeclaringClass();
            if (declaringClass.isAnnotationPresent(ConfigProperties.class)) {
                name += getPrefix(declaringClass.getAnnotation(ConfigProperties.class));
            }
        }

        // If no resolved name has been found
        if (propName == null || propName.isEmpty()) {
            // derive the property name from the injection point
            Class<?> beanClass = null;
            Bean<?> bean = injectionPoint.getBean();
            if (bean == null) {
                Member member = injectionPoint.getMember();
                beanClass = member.getDeclaringClass();
            } else {
                beanClass = bean.getBeanClass();
            }
            StringBuilder sb = new StringBuilder(beanClass.getCanonicalName());
            sb.append('.');
            sb.append(injectionPoint.getMember().getName());
            name += sb.toString();
        } else {
            name += propName;
        }

        return name;
    }

    private static String getPrefix(ConfigProperties annotation) {
        final String annotationPrefix = annotation.prefix();
        if (ConfigProperties.UNCONFIGURED_PREFIX.equals(annotationPrefix)) {
            return "";
        }
        return annotationPrefix;
    }
    
}
