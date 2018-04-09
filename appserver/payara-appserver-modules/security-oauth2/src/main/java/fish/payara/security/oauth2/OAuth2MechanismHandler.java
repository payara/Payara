/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *  Copyright (c) [2018] Payara Foundation and/or its affiliates. All rights reserved.
 * 
 *  The contents of this file are subject to the terms of either the GNU
 *  General Public License Version 2 only ("GPL") or the Common Development
 *  and Distribution License("CDDL") (collectively, the "License").  You
 *  may not use this file except in compliance with the License.  You can
 *  obtain a copy of the License at
 *  https://github.com/payara/Payara/blob/master/LICENSE.txt
 *  See the License for the specific
 *  language governing permissions and limitations under the License.
 * 
 *  When distributing the software, include this License Header Notice in each
 *  file and include the License file at glassfish/legal/LICENSE.txt.
 * 
 *  GPL Classpath Exception:
 *  The Payara Foundation designates this particular file as subject to the "Classpath"
 *  exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *  file that accompanied this code.
 * 
 *  Modifications:
 *  If applicable, add the following below the License Header, with the fields
 *  enclosed by brackets [] replaced by your own identifying information:
 *  "Portions Copyright [year] [name of copyright owner]"
 * 
 *  Contributor(s):
 *  If you wish your version of this file to be governed by only the CDDL or
 *  only the GPL Version 2, indicate your decision by adding "[Contributor]
 *  elects to include this software in this distribution under the [CDDL or GPL
 *  Version 2] license."  If you don't indicate a single choice of license, a
 *  recipient has the option to distribute your version of this file under
 *  either the CDDL, the GPL Version 2 or to extend the choice of license to
 *  its licensees as provided above.  However, if you add GPL Version 2 code
 *  and therefore, elected the GPL Version 2 license, then the option applies
 *  only if the new code is made subject to such option by the copyright
 *  holder.
 */
package fish.payara.security.oauth2;

import com.sun.enterprise.deployment.annotation.handlers.AbstractHandler;
import java.util.function.Function;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.CDI;
import javax.enterprise.inject.spi.Extension;
import javax.security.enterprise.authentication.mechanism.http.HttpAuthenticationMechanism;
import org.glassfish.apf.AnnotationHandlerFor;
import org.glassfish.apf.AnnotationInfo;
import org.glassfish.apf.AnnotationProcessorException;
import org.glassfish.apf.HandlerProcessingResult;
import org.jvnet.hk2.annotations.Service;

/**
 *
 * @author jonathan
 * @since 4.1.2.172
 */
@Service
@AnnotationHandlerFor(OAuth2AuthenticationDefinition.class)
public class OAuth2MechanismHandler extends AbstractHandler implements Extension {

    private OAuth2AuthenticationDefinition annotation;

    @Override
    public HandlerProcessingResult processAnnotation(AnnotationInfo element) throws AnnotationProcessorException {

        if (annotation != null) {
            throw new AnnotationProcessorException("An OAuth 2 Authentication Mechanism is already defined", element);
        }

        annotation = (OAuth2AuthenticationDefinition) element.getAnnotation();
    }

    public void afterBeanDiscovery(@Observes AfterBeanDiscovery afterBean, BeanManager beanManager) {
        /*afterBean.addBean()
                .beanClass(OAuth2AuthenticationMechanism.class)
                .scope(ApplicationScoped.class)
                .produceWith(new OAuth2Producer());*/
        afterBean.addBean(new OAuth2Producer<>(annotation));

    }

    private OAuth2AuthenticationMechanism build(Instance<Object> definition) {
        OAuth2AuthenticationDefinition truedefintion = (OAuth2AuthenticationDefinition) definition.get();
        OAuth2AuthenticationMechanism mechanism = new OAuth2AuthenticationMechanism();
        mechanism.setAuthEndpoint(truedefintion.authEndpoint());
        return mechanism;
    }

    public class FunctionApply implements Function<Instance<Object>, OAuth2AuthenticationMechanism> {

        @Override
        public OAuth2AuthenticationMechanism apply(Instance<Object> definition) {
            OAuth2AuthenticationDefinition truedefintion = (OAuth2AuthenticationDefinition) definition.get();
            OAuth2AuthenticationMechanism mechanism = new OAuth2AuthenticationMechanism();
            mechanism.setAuthEndpoint(truedefintion.authEndpoint());
            return mechanism;
        }

    }

}
