/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2017-2022 Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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

import fish.payara.microprofile.jwtauth.eesecurity.JWTAuthenticationMechanism;
import fish.payara.microprofile.jwtauth.eesecurity.SignedJWTIdentityStore;
import fish.payara.microprofile.jwtauth.jwt.ClaimAnnotationLiteral;
import fish.payara.microprofile.jwtauth.jwt.ClaimValueImpl;
import fish.payara.microprofile.jwtauth.jwt.JWTInjectableType;
import fish.payara.microprofile.jwtauth.jwt.JsonWebTokenImpl;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.AfterBeanDiscovery;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.json.JsonArray;
import jakarta.json.JsonNumber;
import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import jakarta.json.JsonStructure;
import jakarta.json.JsonValue;
import jakarta.security.enterprise.SecurityContext;
import jakarta.security.enterprise.authentication.mechanism.http.HttpAuthenticationMechanism;
import jakarta.security.enterprise.identitystore.IdentityStore;
import java.lang.annotation.Annotation;
import java.security.Principal;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import static java.util.stream.Collectors.toSet;
import org.eclipse.microprofile.auth.LoginConfig;
import org.eclipse.microprofile.jwt.Claim;
import org.eclipse.microprofile.jwt.ClaimValue;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.glassfish.common.util.PayaraCdiProducer;
import org.glassfish.soteria.cdi.CdiUtils;

/**
 * This class contains most of the actual logic from CdiExtension. Places in a
 * separate class since otherwise the <code>@Observes</code> effectively
 * disappears.
 *
 * @author Arjan Tijms
 */
public class CdiInitEventHandler {

    private final static JsonWebTokenImpl emptyJsonWebToken = new JsonWebTokenImpl(null, Collections.emptyMap());

    public static void installAuthenticationMechanism(AfterBeanDiscovery afterBeanDiscovery) {

        afterBeanDiscovery.addBean(new PayaraCdiProducer<IdentityStore>()
                .scope(ApplicationScoped.class)
                .beanClass(IdentityStore.class)
                .types(Object.class, IdentityStore.class, SignedJWTIdentityStore.class)
                .addToId("store " + LoginConfig.class)
                .create(e -> new SignedJWTIdentityStore()));

        afterBeanDiscovery.addBean(new PayaraCdiProducer<HttpAuthenticationMechanism>()
                .scope(ApplicationScoped.class)
                .beanClass(HttpAuthenticationMechanism.class)
                .types(Object.class, HttpAuthenticationMechanism.class, JWTAuthenticationMechanism.class)
                .addToId("mechanism " + LoginConfig.class)
                .create(e -> new JWTAuthenticationMechanism()));

        // MP-JWT 1.0 7.1.1. Injection of JsonWebToken
        afterBeanDiscovery.addBean(new PayaraCdiProducer<JsonWebToken>()
                .scope(RequestScoped.class)
                .beanClass(JsonWebToken.class)
                .types(Object.class, JsonWebToken.class)
                .addToId("token " + LoginConfig.class)
                .create(e -> getJsonWebToken()));

        // MP-JWT 1.0 7.1.2
        for (JWTInjectableType injectableType : computeTypes()) {

            // Add a new Bean<T>/Dynamic producer for each type that 7.1.2 asks us to support.

            afterBeanDiscovery.addBean(new PayaraCdiProducer<>()
                    .scope(Dependent.class)
                    .beanClass(CdiInitEventHandler.class)
                    .types(injectableType.getFullType())
                    .qualifiers(new ClaimAnnotationLiteral())
                    .addToId("claim for " + injectableType.getFullType())
                    .create(creationalContext -> {

                        // Get the qualifier from the injection point
                        Claim claim = getQualifier(
                                getCurrentInjectionPoint(
                                        CdiUtils.getBeanManager(),
                                        (CreationalContext) creationalContext), Claim.class);

                        String claimName = getClaimName(claim);

                        Function<String, Object> claimValueSupplier = (String claimNameParam) -> {
                            return loadClaimObject(injectableType, claimNameParam);
                        };

                        Object claimObj;
                        if (injectableType.isClaimValue()) {
                            // If the target type has a ClaimValue in it, wrap the converted value
                            // into a ClaimValue, e.g. ClaimValue<Long> or ClaimValue<Optional<Long>>
                            claimObj = new ClaimValueImpl<>(claimName, claimValueSupplier);
                        } else {
                            // otherwise simply return the value
                            claimObj = claimValueSupplier.apply(claimName);
                        }

                        return claimObj;
                    }));
        }
    }

    private static Object loadClaimObject(JWTInjectableType injectableType, String claimNameParam) {
        // Obtain the raw named value from the request scoped JsonWebToken's embedded claims and
        // convert it according to the target type for which this Bean<T> was created.
        Object claimObj = injectableType.convert(
                getJsonWebToken().getClaims()
                        .get(claimNameParam));

        // If the target type has an Optional in it, wrap the converted value
        // into an Optional. I.e. Optional<Long> or ClaimValue<Optional<Long>>
        if (injectableType.isOptional()) {
            claimObj = Optional.ofNullable(claimObj);
        }
        return claimObj;
    }

    private static Set<JWTInjectableType> computeTypes() {
        Set<JWTInjectableType> baseTypes = new HashSet<>(Arrays.asList(
                new JWTInjectableType(String.class),
                new JWTInjectableType(new ParameterizedTypeImpl(Set.class, String.class), Set.class),
                new JWTInjectableType(Long.class),
                new JWTInjectableType(Boolean.class),
                new JWTInjectableType(JsonString.class),
                new JWTInjectableType(JsonNumber.class),
                new JWTInjectableType(JsonStructure.class),
                new JWTInjectableType(JsonArray.class),
                new JWTInjectableType(JsonObject.class),
                new JWTInjectableType(JsonValue.class)));

        Set<JWTInjectableType> optionalTypes = new HashSet<>(baseTypes);
        optionalTypes.addAll(
                baseTypes.stream()
                        .map(t -> new JWTInjectableType(new ParameterizedTypeImpl(Optional.class, t.getFullType()), t))
                        .collect(toSet()));

        Set<JWTInjectableType> claimValueTypes = new HashSet<>(optionalTypes);
        claimValueTypes.addAll(
                optionalTypes.stream()
                        .map(t -> new JWTInjectableType(new ParameterizedTypeImpl(ClaimValue.class, t.getFullType()), t))
                        .collect(toSet()));

        return claimValueTypes;
    }

    public static InjectionPoint getCurrentInjectionPoint(BeanManager beanManager, CreationalContext<?> creationalContext) {
        Bean<InjectionPointGenerator> bean = resolve(beanManager, InjectionPointGenerator.class);

        return bean != null
                ? (InjectionPoint) beanManager.getInjectableReference(bean.getInjectionPoints().iterator().next(), creationalContext)
                : null;
    }

    @SuppressWarnings("unchecked")
    public static <T> Bean<T> resolve(BeanManager beanManager, Class<T> beanClass, Annotation... qualifiers) {
        Set<Bean<?>> beans = beanManager.getBeans(beanClass, qualifiers);

        for (Bean<?> bean : beans) {
            if (bean.getBeanClass() == beanClass) {
                return (Bean<T>) beanManager.resolve(Collections.<Bean<?>>singleton(bean));
            }
        }

        Bean<T> bean = (Bean<T>) beanManager.resolve(beans);

        if (bean == null && beanClass.getSuperclass() != Object.class) {
            return (Bean<T>) resolve(beanManager, beanClass.getSuperclass(), qualifiers);
        } else {
            return bean;
        }
    }

    public static <A extends Annotation> A getQualifier(InjectionPoint injectionPoint, Class<A> qualifierClass) {
        for (Annotation annotation : injectionPoint.getQualifiers()) {
            if (qualifierClass.isAssignableFrom(annotation.getClass())) {
                return qualifierClass.cast(annotation);
            }
        }

        return null;
    }

    public static JsonWebTokenImpl getJsonWebToken() {
        SecurityContext context = CdiUtils.getBeanReference(SecurityContext.class);
        Principal principal = context.getCallerPrincipal();
        if (principal instanceof JsonWebTokenImpl) {
            return (JsonWebTokenImpl) principal;
        } else {
            Set<JsonWebTokenImpl> principals = context.getPrincipalsByType(JsonWebTokenImpl.class);
            if (!principals.isEmpty()) {
                return principals.iterator().next();
            }
        }
        return emptyJsonWebToken;
    }

    public static String getClaimName(Claim claim) {
        if (claim.value().equals("")) {
            return claim.standard().name();
        }

        return claim.value();
    }

}
