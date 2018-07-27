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
package fish.payara.security.oidc;

import fish.payara.security.oidc.domain.OidcContextImpl;
import fish.payara.security.oidc.controller.ProviderMetadataContoller;
import fish.payara.security.oidc.controller.ConfigurationController;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Extension;
import fish.payara.security.oidc.api.OidcState;
import java.util.ArrayList;
import java.util.List;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.CDI;
import javax.enterprise.inject.spi.DefinitionException;
import javax.enterprise.inject.spi.ProcessBean;
import javax.security.enterprise.authentication.mechanism.http.HttpAuthenticationMechanism;
import javax.security.enterprise.identitystore.IdentityStore;
import fish.payara.security.annotations.OidcAuthenticationDefinition;
import fish.payara.security.oidc.controller.AuthenticationController;
import fish.payara.security.oidc.controller.TokenController;
import fish.payara.security.oidc.controller.UserInfoController;
import fish.payara.security.oidc.domain.OidcNonce;
import static java.util.Objects.nonNull;

/**
 * Activates {@link OidcAuthenticationMechanism} with the
 * {@link OidcAuthenticationDefinition} annotation configuration.
 *
 * @author Gaurav Gupta
 */
public class OidcExtension implements Extension {

    private final List<OidcAuthenticationDefinition> definitions = new ArrayList<>();

    void beforeBeanDiscovery(@Observes BeforeBeanDiscovery beforeBeanDiscovery, BeanManager manager) {
        addAnnotatedType(OidcAuthenticationMechanism.class, manager, beforeBeanDiscovery);
        addAnnotatedType(OidcIdentityStore.class, manager, beforeBeanDiscovery);

        addAnnotatedType(OidcContextImpl.class, manager, beforeBeanDiscovery);
        addAnnotatedType(OidcState.class, manager, beforeBeanDiscovery);
        addAnnotatedType(OidcNonce.class, manager, beforeBeanDiscovery);

        addAnnotatedType(ConfigurationController.class, manager, beforeBeanDiscovery);
        addAnnotatedType(ProviderMetadataContoller.class, manager, beforeBeanDiscovery);
        addAnnotatedType(AuthenticationController.class, manager, beforeBeanDiscovery);
        addAnnotatedType(TokenController.class, manager, beforeBeanDiscovery);
        addAnnotatedType(UserInfoController.class, manager, beforeBeanDiscovery);
    }

    private <T extends Object> void addAnnotatedType(Class<T> type, BeanManager manager, BeforeBeanDiscovery beforeBeanDiscovery) {
        beforeBeanDiscovery.addAnnotatedType(manager.createAnnotatedType(type), type.getName());
    }

    /**
     * Find the {@link OidcAuthenticationDefinition} annotation and validate.
     *
     * @param <T>
     * @param bean
     * @param beanManager
     */
    public <T> void findOIDCDefinitionAnnotation(@Observes ProcessBean<T> bean, BeanManager beanManager) {
        OidcAuthenticationDefinition definition = bean.getAnnotated().getAnnotation(OidcAuthenticationDefinition.class);
        if (nonNull(definition) && !definitions.contains(definition)) {
            definitions.add(definition);
            validateExtraParametersFormat(definition);
        }
    }

    private void validateExtraParametersFormat(OidcAuthenticationDefinition definition) {
        for (String extraParameter : definition.extraParameters()) {
            String[] parts = extraParameter.split("=");
            if (parts.length != 2) {
                throw new DefinitionException(
                        "OIDCAuthenticationDefinition.extraParameters() value '"
                        + extraParameter
                        + "' is not of the format key=value"
                );
            }
        }
    }

    void afterBeanDiscovery(@Observes AfterBeanDiscovery afterBean, BeanManager beanManager) {
        if (!definitions.isEmpty() && beanManager.getBeans(IdentityStore.class).isEmpty()) {
            afterBean.addBean()
                    .scope(ApplicationScoped.class)
                    .beanClass(IdentityStore.class)
                    .types(IdentityStore.class, Object.class)
                    .createWith(obj -> CDI.current().select(OidcIdentityStore.class).get());
        }
        
        for (OidcAuthenticationDefinition definition : definitions) {
            afterBean.addBean()
                    .scope(ApplicationScoped.class)
                    .beanClass(HttpAuthenticationMechanism.class)
                    .types(HttpAuthenticationMechanism.class, Object.class)
                    .createWith(obj -> CDI.current().select(OidcAuthenticationMechanism.class).get().setConfiguration(definition));
        }
        
        definitions.clear();
    }

}
