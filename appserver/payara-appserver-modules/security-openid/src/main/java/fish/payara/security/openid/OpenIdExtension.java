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
package fish.payara.security.openid;

import fish.payara.security.annotations.OpenIdAuthenticationDefinition;
import fish.payara.security.openid.controller.AuthenticationController;
import fish.payara.security.openid.controller.ConfigurationController;
import fish.payara.security.openid.controller.NonceController;
import fish.payara.security.openid.controller.ProviderMetadataContoller;
import fish.payara.security.openid.controller.StateController;
import fish.payara.security.openid.controller.TokenController;
import fish.payara.security.openid.controller.UserInfoController;
import fish.payara.security.openid.domain.OpenIdContextImpl;
import java.util.ArrayList;
import java.util.List;
import static java.util.Objects.nonNull;
import java.util.Optional;
import java.util.logging.Level;
import static java.util.logging.Level.FINE;
import static java.util.logging.Level.INFO;
import java.util.logging.Logger;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.AfterTypeDiscovery;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.CDI;
import javax.enterprise.inject.spi.DefinitionException;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.inject.spi.ProcessBean;
import javax.security.enterprise.authentication.mechanism.http.HttpAuthenticationMechanism;
import javax.security.enterprise.identitystore.IdentityStore;
import org.glassfish.soteria.cdi.CdiExtension;
import org.glassfish.soteria.cdi.CdiProducer;
import static org.glassfish.soteria.cdi.CdiUtils.getAnnotation;

/**
 * Activates {@link OpenIdAuthenticationMechanism} with the
 * {@link OpenIdAuthenticationDefinition} annotation configuration.
 *
 * @author Gaurav Gupta
 */
public class OpenIdExtension implements Extension {

    private final List<Bean<IdentityStore>> identityStoreBeans = new ArrayList<>();
    private Bean<HttpAuthenticationMechanism> authenticationMechanismBean;

    private static final Logger LOGGER = Logger.getLogger(OpenIdExtension.class.getName());

    protected void beforeBeanDiscovery(@Observes BeforeBeanDiscovery beforeBeanDiscovery, BeanManager manager) {
        addAnnotatedType(OpenIdAuthenticationMechanism.class, manager, beforeBeanDiscovery);
        addAnnotatedType(OpenIdIdentityStore.class, manager, beforeBeanDiscovery);

        addAnnotatedType(OpenIdContextImpl.class, manager, beforeBeanDiscovery);

        addAnnotatedType(NonceController.class, manager, beforeBeanDiscovery);
        addAnnotatedType(StateController.class, manager, beforeBeanDiscovery);
        addAnnotatedType(ConfigurationController.class, manager, beforeBeanDiscovery);
        addAnnotatedType(ProviderMetadataContoller.class, manager, beforeBeanDiscovery);
        addAnnotatedType(AuthenticationController.class, manager, beforeBeanDiscovery);
        addAnnotatedType(TokenController.class, manager, beforeBeanDiscovery);
        addAnnotatedType(UserInfoController.class, manager, beforeBeanDiscovery);
    }

    protected <T extends Object> void addAnnotatedType(Class<T> type, BeanManager manager, BeforeBeanDiscovery beforeBeanDiscovery) {
        beforeBeanDiscovery.addAnnotatedType(manager.createAnnotatedType(type), type.getName());
    }

    /**
     * Find the {@link OpenIdAuthenticationDefinition} annotation and validate.
     *
     * @param <T>
     * @param eventIn
     * @param beanManager
     */
    protected <T> void findOpenIdDefinitionAnnotation(@Observes ProcessBean<T> eventIn, BeanManager beanManager) {

        ProcessBean<T> event = eventIn;

        //create the bean being proccessed.
        Class<?> beanClass = event.getBean().getBeanClass();

        //get the identity store from the annotation (if it exists)
        Optional<OpenIdAuthenticationDefinition> optionalOpenIdStore
                = getAnnotation(beanManager, event.getAnnotated(), OpenIdAuthenticationDefinition.class);

        optionalOpenIdStore.ifPresent(definition -> {
            validateExtraParametersFormat(definition);
            logActivatedIdentityStore(OpenIdIdentityStore.class, beanClass);

            identityStoreBeans.add(new CdiProducer<IdentityStore>()
                    .scope(ApplicationScoped.class)
                    .beanClass(IdentityStore.class)
                    .types(Object.class, IdentityStore.class)
                    .addToId(OpenIdIdentityStore.class)
                    .create(e -> CDI.current().select(OpenIdIdentityStore.class).get())
            );

            logActivatedAuthenticationMechanism(OpenIdAuthenticationMechanism.class, beanClass);
            authenticationMechanismBean = new CdiProducer<HttpAuthenticationMechanism>()
                    .scope(ApplicationScoped.class)
                    .beanClass(HttpAuthenticationMechanism.class)
                    .types(Object.class, HttpAuthenticationMechanism.class)
                    .addToId(OpenIdAuthenticationMechanism.class)
                    .create(e -> {
                        OpenIdAuthenticationMechanism mechanism = CDI.current().select(OpenIdAuthenticationMechanism.class).get();
                        mechanism.setConfiguration(definition);
                        return mechanism;
                    });
        });
    }

    protected void validateExtraParametersFormat(OpenIdAuthenticationDefinition definition) {
        for (String extraParameter : definition.extraParameters()) {
            String[] parts = extraParameter.split("=");
            if (parts.length != 2) {
                throw new DefinitionException(
                        OpenIdAuthenticationDefinition.class.getSimpleName()
                        + ".extraParameters() value '" + extraParameter
                        + "' is not of the format key=value"
                );
            }
        }
    }

    protected void afterBeanDiscovery(@Observes AfterBeanDiscovery afterBeanDiscovery, BeanManager beanManager) {

        if (!identityStoreBeans.isEmpty()) {
            identityStoreBeans.forEach(afterBeanDiscovery::addBean);
        }

        if (authenticationMechanismBean != null) {
            LOGGER.log(FINE, "Creating OpenId Mechanism");
            afterBeanDiscovery.addBean(authenticationMechanismBean);
        }
    }

    private void logActivatedIdentityStore(Class<?> identityStoreClass, Class<?> beanClass) {
        LOGGER.log(INFO, "Activating {0} identity store from {1} class", new Object[]{identityStoreClass.getName(), beanClass.getName()});
    }

    private void logActivatedAuthenticationMechanism(Class<?> authenticationMechanismClass, Class<?> beanClass) {
        LOGGER.log(INFO, "Activating {0} authentication mechanism from {1} class", new Object[]{authenticationMechanismClass.getName(), beanClass.getName()});
    }

}
