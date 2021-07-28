/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *  Copyright (c) [2018-2020] Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.security.openid.google;

import fish.payara.security.annotations.ClaimsDefinition;
import fish.payara.security.annotations.GoogleAuthenticationDefinition;
import fish.payara.security.annotations.LogoutDefinition;
import fish.payara.security.annotations.OpenIdAuthenticationDefinition;
import fish.payara.security.annotations.OpenIdProviderMetadata;
import fish.payara.security.openid.OpenIdExtension;
import fish.payara.security.openid.OpenIdIdentityStore;
import fish.payara.security.openid.api.DisplayType;
import fish.payara.security.openid.api.PromptType;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import static java.util.Objects.nonNull;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.CDI;
import javax.enterprise.inject.spi.ProcessBean;
import javax.security.enterprise.authentication.mechanism.http.HttpAuthenticationMechanism;
import javax.security.enterprise.identitystore.IdentityStore;

/**
 * Activates {@link GoogleOpenIdAuthenticationMechanism} with the
 * {@link GoogleAuthenticationDefinition} annotation configuration.
 *
 * @author Gaurav Gupta
 */
public class GoogleOpenIdExtension extends OpenIdExtension {

    private final List<GoogleAuthenticationDefinition> definitions = new ArrayList<>();

    @Override
    protected void beforeBeanDiscovery(@Observes BeforeBeanDiscovery beforeBeanDiscovery, BeanManager manager) {
        addAnnotatedType(GoogleOpenIdAuthenticationMechanism.class, manager, beforeBeanDiscovery);
        super.beforeBeanDiscovery(beforeBeanDiscovery, manager);
    }

    /**
     * Find the {@link GoogleAuthenticationDefinition} annotation and validate.
     *
     * @param <T>
     * @param bean
     * @param beanManager
     */
    @Override
    protected <T> void findOpenIdDefinitionAnnotation(@Observes ProcessBean<T> bean, BeanManager beanManager) {
        GoogleAuthenticationDefinition definition = bean.getAnnotated().getAnnotation(GoogleAuthenticationDefinition.class);
        if (nonNull(definition) && !definitions.contains(definition)) {
            definitions.add(definition);
            validateExtraParametersFormat(toOpenIdAuthDefinition(definition));
        }
    }

    @Override
    protected void afterBeanDiscovery(@Observes AfterBeanDiscovery afterBean, BeanManager beanManager) {
        if (!definitions.isEmpty() && beanManager.getBeans(IdentityStore.class).isEmpty()) {
            afterBean.addBean()
                    .scope(ApplicationScoped.class)
                    .beanClass(IdentityStore.class)
                    .types(IdentityStore.class, Object.class)
                    .createWith(obj -> CDI.current().select(OpenIdIdentityStore.class).get());
        }

        for (GoogleAuthenticationDefinition definition : definitions) {
            afterBean.addBean()
                    .scope(ApplicationScoped.class)
                    .beanClass(HttpAuthenticationMechanism.class)
                    .types(HttpAuthenticationMechanism.class, Object.class)
                    .createWith(obj -> CDI.current().select(GoogleOpenIdAuthenticationMechanism.class).get().setConfiguration(definition));
        }

        definitions.clear();
    }

    static OpenIdAuthenticationDefinition toOpenIdAuthDefinition(GoogleAuthenticationDefinition googleDefinition) {

        return new OpenIdAuthenticationDefinition() {

            @Override
            public String providerURI() {
                return googleDefinition.providerURI();
            }

            @Override
            public OpenIdProviderMetadata providerMetadata() {
                return googleDefinition.providerMetadata();
            }

            @Override
            public ClaimsDefinition claimsDefinition() {
                return googleDefinition.claimsDefinition();
            }

            @Override
            public LogoutDefinition logout() {
                return googleDefinition.logout();
            }

            @Override
            public String clientId() {
                return googleDefinition.clientId();
            }

            @Override
            public String clientSecret() {
                return googleDefinition.clientSecret();
            }

            @Override
            public String redirectURI() {
                return googleDefinition.redirectURI();
            }

            @Override
            public String[] scope() {
                return googleDefinition.scope();
            }

            @Override
            public String responseType() {
                return googleDefinition.responseType();
            }

            @Override
            public String responseMode() {
                return googleDefinition.responseMode();
            }

            @Override
            public PromptType[] prompt() {
                return googleDefinition.prompt();
            }

            @Override
            public DisplayType display() {
                return googleDefinition.display();
            }

            @Override
            public boolean useNonce() {
                return googleDefinition.useNonce();
            }

            @Override
            public boolean useSession() {
                return googleDefinition.useSession();
            }

            @Override
            public String[] extraParameters() {
                return googleDefinition.extraParameters();
            }

            @Override
            public Class<? extends Annotation> annotationType() {
                return googleDefinition.annotationType();
            }

            @Override
            public int jwksConnectTimeout() {
                return googleDefinition.jwksConnectTimeout();
            }

            @Override
            public int jwksReadTimeout() {
                return googleDefinition.jwksReadTimeout();
            }

            @Override
            public boolean tokenAutoRefresh() {
                return googleDefinition.tokenAutoRefresh();
            }

            @Override
            public int tokenMinValidity() {
                return googleDefinition.tokenMinValidity();
            }
        };
    }

}
