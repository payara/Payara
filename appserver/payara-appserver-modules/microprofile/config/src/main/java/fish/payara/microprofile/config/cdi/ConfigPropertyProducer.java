/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2017 Payara Foundation and/or its affiliates. All rights reserved.
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

import fish.payara.microprofile.config.spi.PayaraConfig;
import java.lang.reflect.Member;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.DeploymentException;
import javax.enterprise.inject.spi.InjectionPoint;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * This class is used as the template for synthetic beans when creating a bean
 * for each converter type registered in the Config
 * @author Steve Millidge (Payara Foundation)
 */
public class ConfigPropertyProducer {
    
    /**
     * General producer method for injecting a property into a field annotated 
     * with the @ConfigProperty annotation.
     * Note this does not have @Produces annotation as a synthetic bean using this method
     * is created in teh CDI Extension.
     * @param ip
     * @return 
     */
    @ConfigProperty
    @Dependent
    public static final Object getGenericProperty(InjectionPoint ip) {
        Object result = null;
        ConfigProperty property = ip.getAnnotated().getAnnotation(ConfigProperty.class);
        PayaraConfig config = (PayaraConfig) ConfigProvider.getConfig();
        Class<?> type = (Class<?>) ip.getType();
        String name = property.name();
        if (name.isEmpty()) {
            // derive the property name from the injection point
            Class beanClass = null;
            Bean bean = ip.getBean();
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
        result = config.getValue(name, property.defaultValue(),type);
        if (result == null) {
            throw new DeploymentException("Microprofile Config Property " + property.name() + " can not be found");
        }
        return result;
    }

}
