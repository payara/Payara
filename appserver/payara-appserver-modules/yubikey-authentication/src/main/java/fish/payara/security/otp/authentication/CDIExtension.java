/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fish.payara.security.otp.authentication;

import fish.payara.security.otp.identitystores.YubikeyIdentityStore;
import static org.glassfish.soteria.cdi.CdiUtils.getAnnotation;

import fish.payara.security.otp.identitystores.YubikeyIdentityStoreDefinitionAnnotationLiteral;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.CDI;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessBean;
import javax.security.enterprise.authentication.mechanism.http.AutoApplySession;
import javax.security.enterprise.authentication.mechanism.http.HttpAuthenticationMechanism;
import javax.security.enterprise.authentication.mechanism.http.LoginToContinue;
import javax.security.enterprise.authentication.mechanism.http.RememberMe;
import javax.security.enterprise.identitystore.IdentityStore;
import javax.security.enterprise.identitystore.IdentityStoreHandler;
import org.glassfish.soteria.cdi.CdiProducer;
import org.glassfish.soteria.cdi.DefaultIdentityStoreHandler;
import org.glassfish.soteria.cdi.LoginToContinueAnnotationLiteral;
import fish.payara.security.otp.identitystores.YubikeyIdentityStoreDefinition;


/**
 *
 * @author Mark Wareham
 */
public class CDIExtension implements Extension{
    
    private static final Logger LOGGER = Logger.getLogger(CDIExtension.class.getName());
    private List<Bean<IdentityStore>> identityStoreBeans = new ArrayList<>();
    private Bean<HttpAuthenticationMechanism> authenticationMechanismBean;
    private boolean httpAuthenticationMechanismFound;
    
    public void register(@Observes BeforeBeanDiscovery beforeBean, BeanManager beanManager) {
       beforeBean.addAnnotatedType(beanManager.createAnnotatedType(OneTimePasswordAuthenticationMechanism.class),
                OneTimePasswordAuthenticationMechanism.class.getName());
    }
    
    
    public <T> void processBean(@Observes ProcessBean<T> eventIn, BeanManager beanManager) {
        LOGGER.info("CDIExtension.processBean()");
        ProcessBean<T> event = eventIn; // JDK8 u60 workaround

        //create the bean being proccessed.
        Class<?> beanClass = event.getBean().getBeanClass();
        
        //get the identity store from the annotation (if it exists)
        Optional<YubikeyIdentityStoreDefinition> optionalYubikeyIdentityStore = getAnnotation(beanManager, 
                event.getAnnotated(), YubikeyIdentityStoreDefinition.class);
        
        
        optionalYubikeyIdentityStore
            //if it exists
            .ifPresent(yubikeyIdentityStoreDefinition -> {
                    //log about it
                    logActivatedIdentityStore(YubikeyIdentityStoreDefinition.class, beanClass);

                //also 
                identityStoreBeans.add(new CdiProducer<IdentityStore>()
                    .scope(ApplicationScoped.class)
                    .beanClass(IdentityStore.class)
                    .types(Object.class, IdentityStore.class, YubikeyIdentityStore.class)
                    .addToId(YubikeyIdentityStoreDefinition.class)
                     //using the cdi producer to
                     // create a YubikeyIdentityStore from the definition.
                    .create(e -> new YubikeyIdentityStore(
                        YubikeyIdentityStoreDefinitionAnnotationLiteral.eval(
                            yubikeyIdentityStoreDefinition)))
            );
        });


        Optional<OneTimePasswordAuthenticationMechanismDefinition> optionalOneTimePasswordMechanism = 
                getAnnotation(beanManager, event.getAnnotated(), OneTimePasswordAuthenticationMechanismDefinition.class);
        optionalOneTimePasswordMechanism.ifPresent(oneTimePasswordAuthenticationMechanismDefinition -> {
            logActivatedAuthenticationMechanism(OneTimePasswordAuthenticationMechanismDefinition.class, beanClass);

            authenticationMechanismBean = new CdiProducer<HttpAuthenticationMechanism>()
                    .scope(ApplicationScoped.class)
                    .beanClass(HttpAuthenticationMechanism.class)
                    .types(Object.class, HttpAuthenticationMechanism.class)
                    .addToId(OneTimePasswordAuthenticationMechanismDefinition.class)
                    .create(e -> {
                        return CDI.current()
                                .select(OneTimePasswordAuthenticationMechanism.class)
                                .get()
                                .loginToContinue(
                                    LoginToContinueAnnotationLiteral.eval(
                                        oneTimePasswordAuthenticationMechanismDefinition.loginToContinue()));
                    });
        });
        
        
        if (event.getBean().getTypes().contains(HttpAuthenticationMechanism.class)) {
            // enabled bean implementing the HttpAuthenticationMechanism found
            httpAuthenticationMechanismFound = true;
        }

        checkForWrongUseOfInterceptors(event.getAnnotated(), beanClass);
    }
    public boolean isHttpAuthenticationMechanismFound() {
        return httpAuthenticationMechanismFound;
    }
    
    private void logActivatedIdentityStore(Class<?> identityStoreClass, Class<?> beanClass) {
        LOGGER.log(Level.INFO, "Activating {0} identity store from {1} class", 
                new Object[]{identityStoreClass.getName(), beanClass.getName()});
    }
    
    private void logActivatedAuthenticationMechanism(Class<?> authenticationMechanismClass, Class<?> beanClass) {
        LOGGER.log(Level.INFO, "Activating {0} authentication mechanism from {1} class", 
                new Object[]{authenticationMechanismClass.getName(), beanClass.getName()});
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
    
    private void checkForWrongUseOfInterceptors(Annotated annotated, Class<?> beanClass) {
        List<Class<? extends Annotation>> annotations = Arrays.asList(AutoApplySession.class, LoginToContinue.class, 
                RememberMe.class);

        for (Class<? extends Annotation> annotation : annotations) {
            // Check if the class is not an interceptor, and is not a valid class to be intercepted.
            if (annotated.isAnnotationPresent(annotation)
                    && !annotated.isAnnotationPresent(javax.interceptor.Interceptor.class)
                    && !HttpAuthenticationMechanism.class.isAssignableFrom(beanClass)) {
                LOGGER.log(Level.WARNING, "Only classes implementing {0} may be annotated with {1}. {2} is annotated, but the interceptor won't take effect on it.",
                        new Object[]{
                    HttpAuthenticationMechanism.class.getName(),
                    annotation.getName(),
                    beanClass.getName()});
            }
        }
    }
}
