/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *  Copyright (c) [2019-2021] Payara Foundation and/or its affiliates. All rights reserved.
 * 
 *  The contents of this file are subject to the terms of either the GNU
 *  General Public License Version 2 only ("GPL") or the Common Development
 *  and Distribution License("CDDL") (collectively, the "License").  You
 *  may not use this file except in compliance with the License.  You can
 *  obtain a copy of the License at
 *  https://github.com/payara/Payara/blob/main/LICENSE.txt
 *  See the License for the specific
 *  language governing permissions and limitations under the License.
 * 
 *  When distributing the software, include this License Header Notice in each
 *  file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
package fish.payara.security.realm.cdi;

import com.sun.enterprise.config.serverbeans.AuthRealm;
import com.sun.enterprise.config.serverbeans.SecurityService;
import com.sun.enterprise.security.auth.realm.NoSuchRealmException;
import com.sun.enterprise.security.auth.realm.Realm;
import com.sun.enterprise.util.StringUtils;
import fish.payara.security.annotations.CertificateAuthenticationMechanismDefinition;
import fish.payara.security.annotations.CertificateIdentityStoreDefinition;
import fish.payara.security.annotations.FileIdentityStoreDefinition;
import fish.payara.security.annotations.PamIdentityStoreDefinition;
import fish.payara.security.annotations.RealmIdentityStoreDefinition;
import fish.payara.security.annotations.RealmIdentityStoreDefinitions;
import fish.payara.security.annotations.SolarisIdentityStoreDefinition;
import fish.payara.security.realm.config.FileRealmIdentityStoreConfiguration;
import fish.payara.security.realm.config.PamRealmIdentityStoreConfiguration;
import fish.payara.security.realm.RealmUtil;
import static fish.payara.security.realm.RealmUtil.ASSIGN_GROUPS;
import static fish.payara.security.realm.RealmUtil.JAAS_CONTEXT;
import fish.payara.security.realm.config.CertificateRealmIdentityStoreConfiguration;
import fish.payara.security.realm.config.RealmConfiguration;
import fish.payara.security.realm.config.SolarisRealmIdentityStoreConfiguration;
import fish.payara.security.realm.identitystores.CertificateRealmIdentityStore;
import fish.payara.security.realm.identitystores.FileRealmIdentityStore;
import fish.payara.security.realm.identitystores.PamRealmIdentityStore;
import fish.payara.security.realm.identitystores.RealmIdentityStore;
import fish.payara.security.realm.identitystores.SolarisRealmIdentityStore;
import fish.payara.security.realm.mechanisms.CertificateAuthenticationMechanism;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import static java.util.logging.Level.INFO;
import java.util.logging.Logger;
import java.util.regex.Pattern;
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
import org.glassfish.common.util.PayaraCdiProducer;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.internal.api.Globals;
import static org.glassfish.soteria.cdi.CdiUtils.getAnnotation;

/**
 * Activate Realm identity stores and authentication mechanism.
 *
 * @author Gaurav Gupta
 */
public class RealmExtension implements Extension {

    private Bean<HttpAuthenticationMechanism> authenticationMechanismBean;

    private final Set<String> realms = new HashSet<>();

    private final List<Bean<IdentityStore>> identityStoreBeans = new ArrayList<>();

    private SecurityService securityService;

    private static final Logger LOGGER = Logger.getLogger(RealmExtension.class.getName());

    protected void beforeBeanDiscovery(@Observes BeforeBeanDiscovery beforeBeanDiscovery, BeanManager manager) {
        addAnnotatedType(RealmIdentityStore.class, manager, beforeBeanDiscovery);
        addAnnotatedType(FileRealmIdentityStore.class, manager, beforeBeanDiscovery);
        addAnnotatedType(CertificateRealmIdentityStore.class, manager, beforeBeanDiscovery);
        addAnnotatedType(CertificateAuthenticationMechanism.class, manager, beforeBeanDiscovery);
        addAnnotatedType(PamRealmIdentityStore.class, manager, beforeBeanDiscovery);
        addAnnotatedType(SolarisRealmIdentityStore.class, manager, beforeBeanDiscovery);
    }

    protected <T extends Object> void addAnnotatedType(Class<T> type, BeanManager manager, BeforeBeanDiscovery beforeBeanDiscovery) {
        beforeBeanDiscovery.addAnnotatedType(manager.createAnnotatedType(type), type.getName());
    }

    /**
     * Find the Realm annotations.
     *
     * @param <T>
     * @param eventIn
     * @param beanManager
     */
    protected <T> void findRealmDefinitionAnnotation(@Observes ProcessBean<T> eventIn, BeanManager beanManager) {

        ProcessBean<T> event = eventIn;

        //create the bean being proccessed.
        Class<?> beanClass = event.getBean().getBeanClass();
        findRealmIdentityStoreDefinitions(beanManager, event, beanClass);
        findFileIdentityStoreDefinitions(beanManager, event, beanClass);
        findCertificateIdentityStoreDefinitions(beanManager, event, beanClass);
        findCertificateAuthenticationMechanismDefinition(beanManager, event, beanClass);
        findPamIdentityStoreDefinitions(beanManager, event, beanClass);
        findSolarisIdentityStoreDefinitions(beanManager, event, beanClass);
    }

    /**
     * Find the
     * {@link RealmIdentityStoreDefinition} & {@link RealmIdentityStoreDefinitions}
     * annotation.
     *
     * @param <T>
     * @param eventIn
     * @param beanManager
     */
    private <T> void findRealmIdentityStoreDefinitions(BeanManager beanManager, ProcessBean<T> event, Class<?> beanClass) {

        //get the identity store from the annotation (if it exists)
        Optional<RealmIdentityStoreDefinition> optionalStore
                = getAnnotation(beanManager, event.getAnnotated(), RealmIdentityStoreDefinition.class);

        optionalStore.ifPresent(definition -> {
            validateDefinition(definition);
            logActivatedIdentityStore(RealmIdentityStore.class, beanClass);

            identityStoreBeans.add(new PayaraCdiProducer<IdentityStore>()
                    .scope(ApplicationScoped.class)
                    .beanClass(IdentityStore.class)
                    .types(Object.class, IdentityStore.class)
                    .addToId(RealmIdentityStore.class + "-" + definition.value())
                    .create(e -> {
                        RealmIdentityStore mechanism = CDI.current().select(RealmIdentityStore.class).get();
                        mechanism.setConfiguration(definition);
                        return mechanism;
                    })
            );
        });

        //get the identity store from the annotation (if it exists)
        Optional<RealmIdentityStoreDefinitions> optionalStores
                = getAnnotation(beanManager, event.getAnnotated(), RealmIdentityStoreDefinitions.class);

        optionalStores.ifPresent(definitions -> {
            for (RealmIdentityStoreDefinition definition : definitions.value()) {
                validateDefinition(definition);
                logActivatedIdentityStore(RealmIdentityStore.class, beanClass);

                identityStoreBeans.add(new PayaraCdiProducer<IdentityStore>()
                        .scope(ApplicationScoped.class)
                        .beanClass(IdentityStore.class)
                        .types(Object.class, IdentityStore.class)
                        .addToId(RealmIdentityStore.class + "-" + definition.value())
                        .create(e -> {
                            RealmIdentityStore mechanism = CDI.current().select(RealmIdentityStore.class).get();
                            mechanism.setConfiguration(definition);
                            return mechanism;
                        })
                );
            }

        });
    }

    /**
     * Find the
     * {@link FileIdentityStoreDefinition} & {@link FileIdentityStoreDefinitions}
     * annotation.
     *
     * @param <T>
     * @param eventIn
     * @param beanManager
     */
    private <T> void findFileIdentityStoreDefinitions(BeanManager beanManager, ProcessBean<T> event, Class<?> beanClass) {

        //get the identity store from the annotation (if it exists)
        Optional<FileIdentityStoreDefinition> optionalStore
                = getAnnotation(beanManager, event.getAnnotated(), FileIdentityStoreDefinition.class);

        optionalStore.ifPresent(definition -> {
            validateDefinition(
                    definition.value(),
                    FileRealmIdentityStore.REALM_CLASS,
                    definition.jaasContext()
            );
            logActivatedIdentityStore(FileRealmIdentityStore.class, beanClass);

            FileRealmIdentityStoreConfiguration configuration = FileRealmIdentityStoreConfiguration.from(definition);
            Properties props = new Properties();
            props.put("file", configuration.getFile());
            props.put(JAAS_CONTEXT, configuration.getJaasContext());
            createRealm(
                    configuration,
                    FileRealmIdentityStore.REALM_CLASS,
                    FileRealmIdentityStore.REALM_LOGIN_MODULE_CLASS,
                    props
            );

            identityStoreBeans.add(new PayaraCdiProducer<IdentityStore>()
                    .scope(ApplicationScoped.class)
                    .beanClass(IdentityStore.class)
                    .types(Object.class, IdentityStore.class)
                    .addToId(FileRealmIdentityStore.class)
                    .create(e -> {
                        FileRealmIdentityStore mechanism = CDI.current().select(FileRealmIdentityStore.class).get();
                        mechanism.init(configuration);
                        return mechanism;
                    })
            );
        });

    }

    /**
     * Find the
     * {@link CertificateIdentityStoreDefinition} & {@link CertificateIdentityStoreDefinitions}
     * annotation.
     *
     * @param <T>
     * @param eventIn
     * @param beanManager
     */
    private <T> void findCertificateIdentityStoreDefinitions(BeanManager beanManager, ProcessBean<T> event, Class<?> beanClass) {

        //get the identity store from the annotation (if it exists)
        Optional<CertificateIdentityStoreDefinition> optionalStore
                = getAnnotation(beanManager, event.getAnnotated(), CertificateIdentityStoreDefinition.class);

        optionalStore.ifPresent(definition -> {
            validateDefinition(
                    definition.value(),
                    CertificateRealmIdentityStore.REALM_CLASS,
                    null
            );
            logActivatedIdentityStore(CertificateRealmIdentityStore.class, beanClass);

            CertificateRealmIdentityStoreConfiguration configuration = CertificateRealmIdentityStoreConfiguration.from(definition);
            createRealm(
                    configuration,
                    CertificateRealmIdentityStore.REALM_CLASS,
                    CertificateRealmIdentityStore.REALM_LOGIN_MODULE_CLASS,
                    new Properties()
            );

            identityStoreBeans.add(new PayaraCdiProducer<IdentityStore>()
                    .scope(ApplicationScoped.class)
                    .beanClass(IdentityStore.class)
                    .types(Object.class, IdentityStore.class)
                    .addToId(CertificateRealmIdentityStore.class)
                    .create(e -> {
                        CertificateRealmIdentityStore mechanism = CDI.current().select(CertificateRealmIdentityStore.class).get();
                        mechanism.init(configuration);
                        return mechanism;
                    })
            );
        });

    }

    /**
     * Find the {@link CertificateAuthenticationMechanismDefinition} annotation.
     *
     * @param <T>
     * @param eventIn
     * @param beanManager
     */
    private <T> void findCertificateAuthenticationMechanismDefinition(BeanManager beanManager, ProcessBean<T> event, Class<?> beanClass) {

        //get the authentication mechanism from the annotation (if it exists)
        Optional<CertificateAuthenticationMechanismDefinition> optionalStore
                = getAnnotation(beanManager, event.getAnnotated(), CertificateAuthenticationMechanismDefinition.class);

        optionalStore.ifPresent(definition -> {
            logActivatedAuthenticationMechanism(CertificateAuthenticationMechanism.class, beanClass);
            authenticationMechanismBean = new PayaraCdiProducer<HttpAuthenticationMechanism>()
                    .scope(ApplicationScoped.class)
                    .beanClass(HttpAuthenticationMechanism.class)
                    .types(Object.class, HttpAuthenticationMechanism.class)
                    .addToId(CertificateAuthenticationMechanism.class)
                    .create(e -> CDI.current().select(CertificateAuthenticationMechanism.class).get());
        });
    }

    /**
     * Find the
     * {@link PamIdentityStoreDefinition} & {@link PamIdentityStoreDefinitions}
     * annotation.
     *
     * @param <T>
     * @param eventIn
     * @param beanManager
     */
    private <T> void findPamIdentityStoreDefinitions(BeanManager beanManager, ProcessBean<T> event, Class<?> beanClass) {

        //get the identity store from the annotation (if it exists)
        Optional<PamIdentityStoreDefinition> optionalStore
                = getAnnotation(beanManager, event.getAnnotated(), PamIdentityStoreDefinition.class);

        optionalStore.ifPresent(definition -> {
            validateDefinition(
                    definition.value(),
                    PamRealmIdentityStore.REALM_CLASS,
                    definition.jaasContext()
            );
            logActivatedIdentityStore(PamRealmIdentityStore.class, beanClass);

            PamRealmIdentityStoreConfiguration configuration = PamRealmIdentityStoreConfiguration.from(definition);
            Properties props = new Properties();
            props.put(JAAS_CONTEXT, configuration.getJaasContext());
            createRealm(
                    configuration,
                    PamRealmIdentityStore.REALM_CLASS,
                    PamRealmIdentityStore.REALM_LOGIN_MODULE_CLASS,
                    props
            );

            identityStoreBeans.add(new PayaraCdiProducer<IdentityStore>()
                    .scope(ApplicationScoped.class)
                    .beanClass(IdentityStore.class)
                    .types(Object.class, IdentityStore.class)
                    .addToId(PamRealmIdentityStore.class)
                    .create(e -> {
                        PamRealmIdentityStore mechanism = CDI.current().select(PamRealmIdentityStore.class).get();
                        mechanism.init(configuration);
                        return mechanism;
                    })
            );
        });

    }

    /**
     * Find the
     * {@link SolarisIdentityStoreDefinition} & {@link SolarisIdentityStoreDefinitions}
     * annotation.
     *
     * @param <T>
     * @param eventIn
     * @param beanManager
     */
    private <T> void findSolarisIdentityStoreDefinitions(BeanManager beanManager, ProcessBean<T> event, Class<?> beanClass) {

        //get the identity store from the annotation (if it exists)
        Optional<SolarisIdentityStoreDefinition> optionalStore
                = getAnnotation(beanManager, event.getAnnotated(), SolarisIdentityStoreDefinition.class);

        optionalStore.ifPresent(definition -> {
            validateDefinition(
                    definition.value(),
                    SolarisRealmIdentityStore.REALM_CLASS,
                    definition.jaasContext()
            );
            logActivatedIdentityStore(SolarisRealmIdentityStore.class, beanClass);

            SolarisRealmIdentityStoreConfiguration configuration = SolarisRealmIdentityStoreConfiguration.from(definition);
            Properties props = new Properties();
            props.put(JAAS_CONTEXT, configuration.getJaasContext());
            createRealm(
                    configuration,
                    SolarisRealmIdentityStore.REALM_CLASS,
                    SolarisRealmIdentityStore.REALM_LOGIN_MODULE_CLASS,
                    props
            );

            identityStoreBeans.add(new PayaraCdiProducer<IdentityStore>()
                    .scope(ApplicationScoped.class)
                    .beanClass(IdentityStore.class)
                    .types(Object.class, IdentityStore.class)
                    .addToId(SolarisRealmIdentityStore.class)
                    .create(e -> {
                        SolarisRealmIdentityStore mechanism = CDI.current().select(SolarisRealmIdentityStore.class).get();
                        mechanism.init(configuration);
                        return mechanism;
                    })
            );
        });

    }

    private <T> T createRealm(
            RealmConfiguration configuration,
            Class<T> realmClass,
            Class loginModuleClass,
            Properties props) {

        try {
            if (!Realm.isValidRealm(configuration.getName())) {
                if (!configuration.getAssignGroups().isEmpty()) {
                    props.put(ASSIGN_GROUPS, String.join(",", configuration.getAssignGroups()));
                }
                RealmUtil.createAuthRealm(
                        configuration.getName(),
                        realmClass.getName(),
                        loginModuleClass.getName(),
                        props
                );
            }

            return realmClass.cast(Realm.getInstance(configuration.getName()));
        } catch (NoSuchRealmException ex) {
            throw new IllegalStateException(configuration.getName(), ex);
        }
    }

    private void validateDefinition(RealmIdentityStoreDefinition definition) {
        String realmName = definition.value();
        if(realmName.isEmpty()) {
            realmName = getSecurityService().getDefaultRealm();
        }
        boolean authRealmFound = getSecurityService().getAuthRealm().stream()
                .map(authRealm -> authRealm.getName())
                .anyMatch(realmName::equals);
        if (!authRealmFound) {
            throw new IllegalStateException(String.format(
                    "[%s] No such realm found.",
                    realmName
            ));
        }
        if (!realms.add(realmName)) {
            throw new IllegalStateException(String.format(
                    "Duplicate realm name [%s] defined in RealmIdentityStoreDefinition.",
                    definition.value()
            ));
        }
    }

    private static final Pattern SIMPLE_TEXT_PATTERN = Pattern.compile("[^a-z0-9 ]", Pattern.CASE_INSENSITIVE);

    private void validateDefinition(String realmName, Class realmClass, String jaasContext) {
        for (AuthRealm authRealm : getSecurityService().getAuthRealm()) {
            if (authRealm.getName().equals(realmName)
                    && !authRealm.getClassname().equals(realmClass.getName())) {
                throw new IllegalStateException(String.format(
                        "%s realm can't be created for realm class %s, as already registed with realm class %s.",
                        realmName,
                        realmClass.getName(),
                        authRealm.getClassname()
                ));
            }
        }
        if (jaasContext != null && SIMPLE_TEXT_PATTERN.matcher(jaasContext).find()) {
            throw new IllegalStateException(String.format(
                    "Special character not allowed in jaasContext %s.",
                    jaasContext
            ));
        }
    }

    private SecurityService getSecurityService() {
        if (securityService == null) {
            ServiceLocator serviceLocator = Globals.getDefaultHabitat();
            this.securityService = serviceLocator.getService(SecurityService.class);
        }
        return securityService;
    }

    protected void afterBeanDiscovery(@Observes AfterBeanDiscovery afterBeanDiscovery, BeanManager beanManager) {
        if (!identityStoreBeans.isEmpty()) {
            identityStoreBeans.forEach(afterBeanDiscovery::addBean);
        }
        if (authenticationMechanismBean != null) {
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
