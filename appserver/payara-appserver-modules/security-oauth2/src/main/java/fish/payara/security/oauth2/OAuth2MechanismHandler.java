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

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Extension;
import fish.payara.security.annotations.OAuth2AuthenticationDefinition;
import fish.payara.security.oauth2.api.OAuth2State;
import fish.payara.security.oauth2.api.OAuthIdentityStore;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import static java.util.logging.Level.FINE;
import static java.util.logging.Level.FINER;
import static java.util.logging.Level.INFO;
import java.util.logging.Logger;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.CDI;
import javax.enterprise.inject.spi.DefinitionException;
import javax.enterprise.inject.spi.ProcessBean;
import javax.security.enterprise.authentication.mechanism.http.HttpAuthenticationMechanism;
import javax.security.enterprise.identitystore.IdentityStore;
import org.glassfish.soteria.cdi.CdiProducer;
import static org.glassfish.soteria.cdi.CdiUtils.getAnnotation;

/**
 * Handles the {@link OAuth2AuthenticationDefinition} annotation
 *
 * @author jonathan
 * @since 4.1.2.172
 */
public class OAuth2MechanismHandler implements Extension {

    private static final Logger LOGGER = Logger.getLogger(OAuth2MechanismHandler.class.getName());

    private final List<Bean<IdentityStore>> identityStoreBeans = new ArrayList<>();
    private Bean<HttpAuthenticationMechanism> authenticationMechanismBean;

    /**
     * This method tries to find the {@link OAuth2AuthenticationDefinition}
     * annotation and if does flags that fact.
     *
     * @param <T>
     * @param eventIn
     * @param beanManager
     */
    public <T> void findOAuth2DefinitionAnnotation(@Observes ProcessBean<T> eventIn, BeanManager beanManager) {

        ProcessBean<T> event = eventIn;

        //create the bean being proccessed.
        Class<?> beanClass = event.getBean().getBeanClass();

        //get the identity store from the annotation (if it exists)
        Optional<OAuth2AuthenticationDefinition> optionalOAuthIdentityStore
                = getAnnotation(beanManager, event.getAnnotated(), OAuth2AuthenticationDefinition.class);

        optionalOAuthIdentityStore.ifPresent(definition -> {
            validateDefinition(definition);
            LOGGER.log(FINE, "Processing definition {0}", definition);

            logActivatedIdentityStore(OAuth2AuthenticationDefinition.class, beanClass);
            identityStoreBeans.add(new CdiProducer<IdentityStore>()
                    .scope(ApplicationScoped.class)
                    .beanClass(IdentityStore.class)
                    .types(Object.class, IdentityStore.class)
                    .addToId(OAuthIdentityStore.class)
                    .create(e -> new OAuthIdentityStore())
            );

            logActivatedAuthenticationMechanism(OAuth2AuthenticationMechanism.class, beanClass);
            authenticationMechanismBean = new CdiProducer<HttpAuthenticationMechanism>()
                    .scope(ApplicationScoped.class)
                    .beanClass(HttpAuthenticationMechanism.class)
                    .types(Object.class, HttpAuthenticationMechanism.class)
                    .addToId(OAuth2AuthenticationMechanism.class)
                    .create(e -> {
                        OAuth2AuthenticationMechanism mechanism = CDI.current().select(OAuth2AuthenticationMechanism.class).get();
                        mechanism.setDefinition(definition);
                        return mechanism;
                    });
        });
    }

    private void validateDefinition(OAuth2AuthenticationDefinition definition) {
        for (String param : definition.extraParameters()) {
            if (param.split("=").length != 2) {
                throw new DefinitionException("Exception processing OAuth2AuthenticationDefinition: "
                        + "extraParameter on annotation " + definition.toString() + " is not of the format key=value");
            }
        }
    }
    void beforeBeanDiscovery(@Observes BeforeBeanDiscovery event, BeanManager manager) {
        LOGGER.log(FINER, "OAuth2Handler - BeforeBeanDiscovery {0}", event.toString());
        event.addAnnotatedType(manager.createAnnotatedType(OAuth2AuthenticationMechanism.class), "OAuth2 Mechanism");
        event.addAnnotatedType(manager.createAnnotatedType(OAuth2StateHolder.class), "OAuth2Token");
        event.addAnnotatedType(manager.createAnnotatedType(OAuth2State.class), "OAuth2State");

    }

    void afterBeanDiscovery(@Observes AfterBeanDiscovery afterBeanDiscovery, BeanManager beanManager) {
        if (!identityStoreBeans.isEmpty()) {
            identityStoreBeans.forEach(afterBeanDiscovery::addBean);
        }

        if (authenticationMechanismBean != null) {
            LOGGER.log(FINE, "Creating OAuth2 Mechanism");
            afterBeanDiscovery.addBean(authenticationMechanismBean);
        }
    }

    private void logActivatedIdentityStore(Class<?> identityStoreClass, Class<?> beanClass) {
        LOGGER.log(INFO, "Activating {0} identity store from {1} class",
                new String[]{identityStoreClass.getName(), beanClass.getName()});
    }

    private void logActivatedAuthenticationMechanism(Class<?> authenticationMechanismClass, Class<?> beanClass) {
        LOGGER.log(INFO, "Activating {0} authentication mechanism from {1} class",
                new String[]{authenticationMechanismClass.getName(), beanClass.getName()});
    }
}
