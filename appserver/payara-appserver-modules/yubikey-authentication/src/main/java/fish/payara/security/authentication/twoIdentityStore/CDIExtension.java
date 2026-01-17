/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *    Copyright (c) [2018-2021] Payara Foundation and/or its affiliates. All rights reserved.
 * 
 *     The contents of this file are subject to the terms of either the GNU
 *     General Public License Version 2 only ("GPL") or the Common Development
 *     and Distribution License("CDDL") (collectively, the "License").  You
 *     may not use this file except in compliance with the License.  You can
 *     obtain a copy of the License at
 *     https://github.com/payara/Payara/blob/main/LICENSE.txt
 *     See the License for the specific
 *     language governing permissions and limitations under the License.
 * 
 *     When distributing the software, include this License Header Notice in each
 *     file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
 * 
 *     GPL Classpath Exception:
 *     The Payara Foundation designates this particular file as subject to the "Classpath"
 *     exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *     file that accompanied this code.
 * 
 *     Modifications:
 *     If applicable, add the following below the License Header, with the fields
 *     enclosed by brackets [] replaced by your own identifying information:
 *     "Portions Copyright [year] [name of copyright owner]"
 * 
 *     Contributor(s):
 *     If you wish your version of this file to be governed by only the CDDL or
 *     only the GPL Version 2, indicate your decision by adding "[Contributor]
 *     elects to include this software in this distribution under the [CDDL or GPL
 *     Version 2] license."  If you don't indicate a single choice of license, a
 *     recipient has the option to distribute your version of this file under
 *     either the CDDL, the GPL Version 2 or to extend the choice of license to
 *     its licensees as provided above.  However, if you add GPL Version 2 code
 *     and therefore, elected the GPL Version 2 license, then the option applies
 *     only if the new code is made subject to such option by the copyright
 *     holder.
 */
package fish.payara.security.authentication.twoIdentityStore;

import static org.glassfish.soteria.cdi.CdiUtils.getAnnotation;

import fish.payara.security.identitystores.YubikeyIdentityStore;
import fish.payara.security.annotations.YubikeyIdentityStoreDefinition;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.AfterBeanDiscovery;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.BeforeBeanDiscovery;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.spi.ProcessBean;
import jakarta.security.enterprise.authentication.mechanism.http.HttpAuthenticationMechanism;
import jakarta.security.enterprise.identitystore.IdentityStore;
import org.glassfish.soteria.cdi.LoginToContinueAnnotationLiteral;
import fish.payara.security.annotations.TwoIdentityStoreAuthenticationMechanismDefinition;
import fish.payara.security.identitystores.YubikeyIdentityStoreDefinitionAnnotationLiteral;
import org.glassfish.common.util.PayaraCdiProducer;

/**
 * CDI Extension class. Uses Dynamic producers to add {@link TwoIdentityStoreAuthenticationMechanism},
 * {@link YubikeyIdentityStore} if annotations containing their corresponding *Definition are found.
 *
 * @author Mark Wareham
 */
public class CDIExtension implements Extension {

    private static final Logger LOG = Logger.getLogger(CDIExtension.class.getName());
    private List<Bean<IdentityStore>> identityStoreBeans = new ArrayList<>();
    private Bean<HttpAuthenticationMechanism> authenticationMechanismBean;
    private static final String MODULE_PREFIX = "2FA Module Extension ";

    public void register(@Observes BeforeBeanDiscovery beforeBean, BeanManager beanManager) {
        beforeBean.addAnnotatedType(beanManager.createAnnotatedType(TwoIdentityStoreAuthenticationMechanism.class),
                MODULE_PREFIX + TwoIdentityStoreAuthenticationMechanism.class.getName());
        beforeBean.addAnnotatedType(beanManager.createAnnotatedType(TwoIdentityStoreAuthenticationMechanismState.class),
                MODULE_PREFIX + TwoIdentityStoreAuthenticationMechanismState.class.getName());
        beforeBean.addAnnotatedType(beanManager.createAnnotatedType(YubikeyIdentityStore.class), 
                MODULE_PREFIX + YubikeyIdentityStore.class.getName());
        
    }

    public <T> void processBean(@Observes ProcessBean<T> eventIn, BeanManager beanManager) {

        ProcessBean<T> event = eventIn; // JDK8 u60 workaround

        //create the bean being proccessed.
        Class<?> beanClass = event.getBean().getBeanClass();

        //get the identity store from the annotation (if it exists)
        Optional<YubikeyIdentityStoreDefinition> optionalYubikeyIdentityStore = getAnnotation(beanManager,
                event.getAnnotated(), YubikeyIdentityStoreDefinition.class);

        optionalYubikeyIdentityStore.ifPresent(yubikeyIdentityStoreDefinition -> {
            logActivatedIdentityStore(YubikeyIdentityStoreDefinition.class, beanClass);
            identityStoreBeans.add(new PayaraCdiProducer<IdentityStore>()
                    .scope(ApplicationScoped.class)
                    .beanClass(IdentityStore.class)
                    .types(Object.class, IdentityStore.class)
                    .addToId(YubikeyIdentityStoreDefinition.class)
                    .create(e -> CDI.current().select(YubikeyIdentityStore.class)
                            .get()
                            .init(YubikeyIdentityStoreDefinitionAnnotationLiteral.eval(yubikeyIdentityStoreDefinition))
                    )
            );
        });

        Optional<TwoIdentityStoreAuthenticationMechanismDefinition> optionalOneTimePasswordMechanism
                = getAnnotation(beanManager, event.getAnnotated(), TwoIdentityStoreAuthenticationMechanismDefinition.class);
        optionalOneTimePasswordMechanism.ifPresent(oneTimePasswordAuthenticationMechanismDefinition -> {

            logActivatedAuthenticationMechanism(TwoIdentityStoreAuthenticationMechanismDefinition.class, beanClass);

            authenticationMechanismBean = new PayaraCdiProducer<HttpAuthenticationMechanism>()
                    .scope(ApplicationScoped.class)
                    .beanClass(HttpAuthenticationMechanism.class)
                    .types(Object.class, HttpAuthenticationMechanism.class)
                    .addToId(TwoIdentityStoreAuthenticationMechanismDefinition.class)
                    .create(e -> {
                        return CDI.current()
                                .select(TwoIdentityStoreAuthenticationMechanism.class)
                                .get()
                                .loginToContinue(LoginToContinueAnnotationLiteral.eval(
                                        oneTimePasswordAuthenticationMechanismDefinition.loginToContinue()));
                    });
        });
    }

    public void afterBean(final @Observes AfterBeanDiscovery afterBeanDiscovery, BeanManager beanManager) {

        if (!identityStoreBeans.isEmpty()) {
            for (Bean<IdentityStore> identityStoreBean : identityStoreBeans) {
                afterBeanDiscovery.addBean(identityStoreBean);
            }
        }

        if (authenticationMechanismBean != null) {
            afterBeanDiscovery.addBean(authenticationMechanismBean);
        }

    }

    private void logActivatedIdentityStore(Class<?> identityStoreClass, Class<?> beanClass) {
        LOG.log(Level.INFO, "Activating {0} identity store from {1} class",
                new String[]{identityStoreClass.getName(), beanClass.getName()});
    }

    private void logActivatedAuthenticationMechanism(Class<?> authenticationMechanismClass, Class<?> beanClass) {
        LOG.log(Level.INFO, "Activating {0} authentication mechanism from {1} class",
                new String[]{authenticationMechanismClass.getName(), beanClass.getName()});
    }

}
