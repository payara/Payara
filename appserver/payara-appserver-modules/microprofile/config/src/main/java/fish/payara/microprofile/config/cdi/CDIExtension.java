/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fish.payara.microprofile.config.cdi;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.InjectionTarget;
import javax.enterprise.inject.spi.ProcessInjectionPoint;
import javax.enterprise.inject.spi.ProcessInjectionTarget;
import javax.enterprise.inject.spi.WithAnnotations;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 *
 * @author Steve Millidge <Payara Services Limited>
 */
public class CDIExtension implements Extension {
    
    public void createProducer(@Observes BeforeBeanDiscovery event, BeanManager bm) {
        AnnotatedType<ConfigProducer> at = bm.createAnnotatedType(ConfigProducer.class);
        event.addAnnotatedType(at);
    }
    
    public <T> void registerIP(@Observes ProcessInjectionTarget<T> pit) {
        // check for any fields
        Set<AnnotatedField<? super T>> fields = pit.getAnnotatedType().getFields();
        boolean requiresWrapping = false;
        final Set<Field> annotatedFields = new HashSet<Field>();
        for (AnnotatedField<? super T> field : fields) {
            if (field.isAnnotationPresent(ConfigProperty.class)) {
                requiresWrapping = true;
                annotatedFields.add(field.getJavaMember());
            }
        }

        final InjectionTarget it = pit.getInjectionTarget();
        if (requiresWrapping) {
            pit.setInjectionTarget(new InjectionTarget<T>() {
                @Override
                public void inject(T instance, CreationalContext<T> ctx) {
                    it.inject(instance, ctx);
                    for (Field annotatedField : annotatedFields) {
                        if (annotatedField.getType().equals(String.class)) {
                            annotatedField.setAccessible(true);
                            try {
                                annotatedField.set(instance, "Fuck ME Injection Worked");
                            } catch (IllegalArgumentException ex) {
                                Logger.getLogger(CDIExtension.class.getName()).log(Level.SEVERE, null, ex);
                            } catch (IllegalAccessException ex) {
                                Logger.getLogger(CDIExtension.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }
                        
                    }
                }

                @Override
                public void postConstruct(T instance) {
                    it.postConstruct(instance);
                }

                @Override
                public void preDestroy(T instance) {
                    it.preDestroy(instance);
                }

                @Override
                public T produce(CreationalContext<T> ctx) {
                    return (T) it.produce(ctx);
                }

                @Override
                public void dispose(T instance) {
                    it.dispose(instance);
                }

                @Override
                public Set<InjectionPoint> getInjectionPoints() {
                    return it.getInjectionPoints();
                }
            });
        }
    }    
    
    
    public void registerPIP(@Observes ProcessInjectionPoint<?,?> pip) {
        System.out.println("Register PIP called for " + pip.getInjectionPoint().getAnnotated().getBaseType().getTypeName());
        ConfigProperty property = pip.getInjectionPoint().getAnnotated().getAnnotation(ConfigProperty.class);
        if (property != null) {
            String propertyName = property.name();
            String defaultValue = property.defaultValue();
        }
    }
}
