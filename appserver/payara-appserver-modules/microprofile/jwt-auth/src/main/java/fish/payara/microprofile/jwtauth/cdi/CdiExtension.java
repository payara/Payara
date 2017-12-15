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
package fish.payara.microprofile.jwtauth.cdi;

import java.lang.annotation.Annotation;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.SessionScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.DeploymentException;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.ProcessBean;
import javax.enterprise.inject.spi.ProcessInjectionTarget;

import org.eclipse.microprofile.auth.LoginConfig;
import org.eclipse.microprofile.jwt.Claim;

import fish.payara.microprofile.jwtauth.eesecurity.JWTAuthenticationMechanism;
import fish.payara.microprofile.jwtauth.eesecurity.SignedJWTIdentityStore;

/**
 * This CDI extension installs the {@link JWTAuthenticationMechanism} and related {@link SignedJWTIdentityStore}
 * when the <code>LoginConfig</code> annotation is encountered (MP-JWT 1.0 5).
 * 
 * <p>
 * Additionally this extension checks that injection of claims are in the right scope (non-transitively, 7.1.3).
 * 
 * @author Arjan Tijms
 */
public class CdiExtension implements Extension {
    
    /**
     * Tracks whether a LoginConfig annotation has been encountered and thus
     * a mechanism needs to be installed.
     */
    private boolean addJWTAuthenticationMechanism;
    
    public void register(@Observes BeforeBeanDiscovery beforeBean, BeanManager beanManager) {
        beforeBean.addAnnotatedType(beanManager.createAnnotatedType(InjectionPointGenerator.class), "JWT InjectionPointGenerator ");
    }
    
    /**
     * This method tries to find the LoginConfig annotation and if does flags that fact.
     * 
     */
    public <T> void findLoginConfigAnnotation(@Observes ProcessBean<T> eventIn, BeanManager beanManager) {
        
        ProcessBean<T> event = eventIn; // JDK8 u60 workaround
        
        LoginConfig loginConfig = event.getAnnotated().getAnnotation(LoginConfig.class);
        if (loginConfig != null && loginConfig.authMethod().equals("MP-JWT")) {
            addJWTAuthenticationMechanism = true;
        }
        
    }
    
    public <T> void checkInjectIntoRightScope(@Observes ProcessInjectionTarget<T> eventIn, BeanManager beanManager) {
        
        ProcessInjectionTarget<T> event = eventIn; // JDK8 u60 workaround
        
        for (InjectionPoint injectionPoint : event.getInjectionTarget().getInjectionPoints()) {
            if (hasClaim(injectionPoint)) {
                
                // MP-JWT 1.0 7.1.3. 
                
                Bean<?> bean = injectionPoint.getBean();
                
                Class<?> scope = bean != null? injectionPoint.getBean().getScope() : null;
                
                if (scope != null && (scope.equals(ApplicationScoped.class) || scope.equals(SessionScoped.class))) {
                    throw new DeploymentException(
                        "Can't inject using qualifier " + Claim.class + " in a target with scope " + scope);
                }
            }
        }
    }
    
    public void installMechanismIfNeeded(@Observes AfterBeanDiscovery eventIn, BeanManager beanManager) {

        AfterBeanDiscovery afterBeanDiscovery = eventIn; // JDK8 u60 workaround

        if (addJWTAuthenticationMechanism) {
            CdiExtension2.installAuthenticationMechanism(afterBeanDiscovery);
        }
    }
    
    private static boolean hasClaim(InjectionPoint injectionPoint) {
        for (Annotation qualifier : injectionPoint.getQualifiers()) {
            if (qualifier.annotationType().equals(Claim.class)) {
                return true;
            }
        }
        
        return false;
    }

}
