package org.glassfish.jms.injection;

import javax.enterprise.context.*;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.spi.*;
import javax.enterprise.util.AnnotationLiteral;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.glassfish.weld.WeldGFExtension;

/*
 * This CDI portable extension can register JMSContext beans to be system-level
 * that can be injected into all applications.
 */
@WeldGFExtension(beans={JMSContextManager.class, InjectableJMSContext.class})
public class JMSCDIExtension implements Extension {
    public JMSCDIExtension() {
    }

    private Bean createLocalBean(BeanManager beanManager, Class beanClass) {
        AnnotatedType annotatedType = beanManager.createAnnotatedType(beanClass);
        final InjectionTarget injectionTarget = beanManager.createInjectionTarget(annotatedType);
        return new LocalBean(beanClass, injectionTarget);
    }

    public void afterBeanDiscovery(@Observes AfterBeanDiscovery afterBeanDiscoveryEvent, BeanManager beanManager) {
// Workaround for WELD-1182. uncomment the following lines once the defect is addressed.
//        Bean managerBean = createLocalBean(beanManager, InjectableJMSContext.class);
//        Bean contextBean = createLocalBean(beanManager, JMSContextManager.class);
//        afterBeanDiscoveryEvent.addBean(managerBean);
//        afterBeanDiscoveryEvent.addBean(contextBean);
    }

    void addScope(@Observes final BeforeBeanDiscovery event) {
    }

    void afterBeanDiscovery(@Observes final AfterBeanDiscovery event) {
    }

    public void beforeBeanDiscovery(@Observes BeforeBeanDiscovery event) {
    }

    public void beforeBeanDiscovery(@Observes BeforeBeanDiscovery event, BeanManager beanManager) {
    }

    public  void afterDeploymentValidation(@Observes AfterDeploymentValidation event, BeanManager beanManager) {
    }

    public void beforeShutdown(@Observes BeforeShutdown event, BeanManager beanManager) {
    }

    public <T> void processInjectionTarget(@Observes ProcessInjectionTarget<T> pit) {
    }

    public <T> void processAnnotatedType(@Observes ProcessAnnotatedType<T> pat) {
    }

    public <T, X> void processProducer(@Observes ProcessProducer<T, X> event) {
    }

    public class LocalBean implements Bean {
        private Class beanClass;
        private InjectionTarget injectionTarget;

        public LocalBean(Class beanClass, InjectionTarget injectionTarget) {
            this.beanClass = beanClass;
            this.injectionTarget = injectionTarget;
        }

        @Override
        public Class<?> getBeanClass() {
            return beanClass;
        }

        @Override
        public Set<InjectionPoint> getInjectionPoints() {
            return injectionTarget.getInjectionPoints();
        }

        @Override
        public String getName() {
            return beanClass.getName();
        }

        @Override
        public Set<Annotation> getQualifiers() {
            Set<Annotation> qualifiers = new HashSet<Annotation>();
            qualifiers.add( new AnnotationLiteral<Default>() {} );
            qualifiers.add( new AnnotationLiteral<Any>() {} );
            return qualifiers;
        }

        @Override
        public Class<? extends Annotation> getScope() {
            if (beanClass.isAnnotationPresent(RequestScoped.class))
                return RequestScoped.class;
            else
                return Dependent.class;
        }

        @Override
        public Set<Class<? extends Annotation>> getStereotypes() {
            return Collections.emptySet();
        }

        @Override
        public Set<Type> getTypes() {
            Set<Type> types = new HashSet<Type>();
            types.add(beanClass);
            boolean loop = true;
            Class clazz = beanClass;
            while (loop) {
                Class[] interfaces = clazz.getInterfaces();
                for (Class t : interfaces) {
                    types.add(t);
                }
                clazz = clazz.getSuperclass();
                if (clazz == null) {
                    loop = false;
                    break;
                } else {
                    types.add(clazz);
                }
            }
            return types;
        }

        @Override
        public boolean isAlternative() {
            return false;
        }

        @Override
        public boolean isNullable() {
            return false;
        }

        @Override
        public Object create(CreationalContext ctx) {
            Object instance = injectionTarget.produce(ctx);
            injectionTarget.inject(instance, ctx);
            injectionTarget.postConstruct(instance);
            return instance;
        }

        @Override
        public void destroy(Object instance, CreationalContext ctx) {
            injectionTarget.preDestroy( instance );
            injectionTarget.dispose( instance );
            ctx.release();
        }
    }
}
