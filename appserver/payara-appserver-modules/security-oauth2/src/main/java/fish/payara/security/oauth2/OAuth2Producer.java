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

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionPoint;

import fish.payara.security.oauth2.annotation.OAuth2AuthenticationDefinition;
import java.util.Arrays;
import java.util.HashSet;
import java.util.UUID;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.spi.PassivationCapable;
import javax.enterprise.util.AnnotationLiteral;
import javax.security.enterprise.authentication.mechanism.http.HttpAuthenticationMechanism;

/**
 * Class to produce a bean of {@link OAuth2AuthenticationMechanism}
 * @author jonathan coustick
 * @since 4.1.2.172
 */
public class OAuth2Producer implements Bean<OAuth2AuthenticationMechanism>, PassivationCapable {

    private OAuth2AuthenticationDefinition definition;
    
    public OAuth2Producer(OAuth2AuthenticationDefinition definition){
        this.definition = definition;
    }
    
    @Override
    public Class<?> getBeanClass() {
        return OAuth2AuthenticationMechanism.class;
    }

    @Override
    public Set<InjectionPoint> getInjectionPoints() {
        return Collections.EMPTY_SET;
    }

    @Override
    public boolean isNullable() {
        return false;
    }

    @Override
    public void destroy(OAuth2AuthenticationMechanism instance, CreationalContext<OAuth2AuthenticationMechanism> creationalContext) {
        //no-op
    }

    @Override
    public OAuth2AuthenticationMechanism create(CreationalContext<OAuth2AuthenticationMechanism> creationalContext) {
//        CDI.current().select(OAuth2AuthenticationMechanism.class).get();
        OAuth2AuthenticationMechanism mechanism = new OAuth2AuthenticationMechanism(definition);
        creationalContext.push(mechanism);
        return mechanism;
    }

    @Override
    public Set<Type> getTypes() {
        return new HashSet<>(Arrays.asList(OAuth2AuthenticationMechanism.class, HttpAuthenticationMechanism.class, Object.class));
    }

    @Override
    public Set<Annotation> getQualifiers() {
        return Collections.singleton((Annotation) new DefaultAnnotationLiteral());
    }

    @Override
    public Class<? extends Annotation> getScope() {
        return ApplicationScoped.class;
    }

    @Override
    public String getName() {
        return "OAuth2AuthenticationMechanism";
    }

    @Override
    public Set<Class<? extends Annotation>> getStereotypes() {
        return Collections.EMPTY_SET;
    }

    @Override
    public boolean isAlternative() {
        return false;
    }

    @Override
    public String getId() {
        return UUID.randomUUID().toString();
    }
    
    public class DefaultAnnotationLiteral extends AnnotationLiteral {
        
        
        @Override
         public Class<? extends Annotation> annotationType() {
            return Default.class;
        }
    }

}
