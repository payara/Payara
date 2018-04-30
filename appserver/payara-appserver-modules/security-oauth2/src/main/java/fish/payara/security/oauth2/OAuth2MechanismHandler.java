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

import java.util.logging.Level;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Extension;
import fish.payara.security.oauth2.annotation.OAuth2AuthenticationDefinition;
import fish.payara.security.oauth2.api.OAuth2State;
import fish.payara.security.oauth2.api.OAuthIdentityStore;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.CDI;
import javax.enterprise.inject.spi.DefinitionException;
import javax.enterprise.inject.spi.ProcessBean;
import javax.security.enterprise.authentication.mechanism.http.HttpAuthenticationMechanism;
import javax.security.enterprise.identitystore.IdentityStore;
import org.glassfish.soteria.cdi.CdiProducer;

/**
 * Handles the {@link OAuth2AuthenticationDefinition} annotation
 *
 * @author jonathan
 * @since 4.1.2.172
 */
public class OAuth2MechanismHandler implements Extension {

    private List<OAuth2AuthenticationDefinition> annotations;
    private Logger logger = Logger.getLogger("OAuth2Mechanism");

    public OAuth2MechanismHandler() {
        annotations = new ArrayList<>();
    }

    /**
     * This method tries to find the {@link OAuth2AuthenticationDefinition} annotation and if does flags that fact.
     *
     * @param <T>
     * @param eventIn
     * @param beanManager
     */
    public <T> void findOAuth2DefinitionAnnotation(@Observes ProcessBean<T> eventIn, BeanManager beanManager) {

        ProcessBean<T> event = eventIn;

        OAuth2AuthenticationDefinition annotation = event.getAnnotated().getAnnotation(OAuth2AuthenticationDefinition.class);
        if (annotation != null && !annotations.contains(annotation)) {
            logger.log(Level.FINE, "Processing annotation {0}", annotation);
            annotations.add(annotation);
            for (String param : annotation.extraParameters()){
                if (param.split("=").length != 2){
                    throw new DefinitionException("Exception processing OAuth2AuthenticationDefinition: "
                            + "extraParameter on annotation " + annotation.toString() + " is not of the format key=value");
                }
            }
        }
    }

    void beforeBeanDiscovery(@Observes BeforeBeanDiscovery event, BeanManager manager) {
        logger.log(Level.FINER, "OAuth2Handler - BeforeBeanDiscovery" + event.toString());
        event.addAnnotatedType(manager.createAnnotatedType(OAuth2AuthenticationMechanism.class), "OAuth2 Mechanism");
        event.addAnnotatedType(manager.createAnnotatedType(OAuth2StateHolder.class), "OAuth2Token");
        event.addAnnotatedType(manager.createAnnotatedType(OAuthIdentityStore.class), "OAuth2IdentityStore");
        event.addAnnotatedType(manager.createAnnotatedType(OAuth2State.class), "OAuth2State");

    }

    void afterBeanDiscovery(@Observes AfterBeanDiscovery afterBean, BeanManager beanManager) {
        logger.log(Level.FINE, "Creating OAuth2 Mechanism");
        
        if (!annotations.isEmpty() && beanManager.getBeans(IdentityStore.class).isEmpty()) {
            afterBean.addBean(new CdiProducer<IdentityStore>().
                    scope(ApplicationScoped.class)
                    .beanClass(IdentityStore.class)
                    .types(IdentityStore.class, Object.class)
                    .create(obj -> {return CDI.current()
                               .select(OAuthIdentityStore.class).get();}
                    ));
        }
        for (OAuth2AuthenticationDefinition annotation : annotations) {

            afterBean.addBean(new CdiProducer<HttpAuthenticationMechanism>().
                    scope(ApplicationScoped.class)
                    .beanClass(HttpAuthenticationMechanism.class)
                    .types(HttpAuthenticationMechanism.class, Object.class)
                    .create(obj -> {return CDI.current()
                               .select(OAuth2AuthenticationMechanism.class).get().setDefinition(annotation);}
                    ));
            logger.log(Level.FINE, "OAuth2 Mechanism created successfully");

        }
        annotations.clear();
    }

}
