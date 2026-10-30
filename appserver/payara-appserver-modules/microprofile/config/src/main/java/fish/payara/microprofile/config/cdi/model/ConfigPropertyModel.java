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
package fish.payara.microprofile.config.cdi.model;

import java.lang.reflect.Member;

import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.InjectionPoint;

import org.eclipse.microprofile.config.inject.ConfigProperty;

public final class ConfigPropertyModel {

    private final InjectionPoint injectionPoint;

    private final String name;
    private final String defaultValue;

    public ConfigPropertyModel(InjectionPoint injectionPoint) {
        this(injectionPoint, null);
    }

    public ConfigPropertyModel(InjectionPoint injectionPoint, String prefix) {
        this.injectionPoint = injectionPoint;

        final ConfigProperty propAnnotation = injectionPoint.getAnnotated().getAnnotation(ConfigProperty.class);

        if (prefix == null) {
            prefix = "";
        }
        if (propAnnotation == null) {
            this.name = prefix + injectionPoint.getMember().getName();
            this.defaultValue = null;
        } else {
            this.name = prefix + parseName(injectionPoint, propAnnotation.name());
            this.defaultValue = propAnnotation.defaultValue();
        }
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
    
}
