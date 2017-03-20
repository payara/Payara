/*

 DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

 Copyright (c) 2014-2017 Payara Foundation. All rights reserved.

 The contents of this file are subject to the terms of the Common Development
 and Distribution License("CDDL") (collectively, the "License").  You
 may not use this file except in compliance with the License.  You can
 obtain a copy of the License at
 https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 or packager/legal/LICENSE.txt.  See the License for the specific
 language governing permissions and limitations under the License.

 When distributing the software, include this License Header Notice in each
 file and include the License file at packager/legal/LICENSE.txt.
 */
package fish.payara.cdi.jsr107.implementation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.cache.annotation.CacheDefaults;
import javax.cache.annotation.CacheInvocationParameter;
import javax.cache.annotation.CacheKey;
import javax.cache.annotation.CacheKeyGenerator;
import javax.cache.annotation.CacheKeyInvocationContext;
import javax.cache.annotation.CachePut;
import javax.cache.annotation.CacheRemove;
import javax.cache.annotation.CacheRemoveAll;
import javax.cache.annotation.CacheResolverFactory;
import javax.cache.annotation.CacheResult;
import javax.cache.annotation.CacheValue;
import javax.interceptor.InvocationContext;

/**
 *
 * @author steve
 */
public class PayaraCacheKeyInvocationContext<A extends Annotation> implements CacheKeyInvocationContext<A> {

    private final InvocationContext ctx;
    private final A annotation;
    private CacheDefaults defaults;
    private CacheResolverFactory factory;
    private CacheKeyGenerator generator;

    public PayaraCacheKeyInvocationContext(InvocationContext ctx, A annotation) {
        this.ctx = ctx;
        this.annotation = annotation;
        Class<?> clazz = ctx.getTarget().getClass();
        //hunt for cache defaults annotation
        while (defaults == null && clazz != null) {
            defaults = clazz.getAnnotation(CacheDefaults.class);
            if (defaults == null) {
                clazz = clazz.getSuperclass();
            }
        }
        
        if (!(annotation instanceof CacheRemoveAll)) {
            generator = getGenerator();            
        }
        factory = getFactory();
    }

    @Override
    public Object getTarget() {
        return ctx.getTarget();
    }

    public final CacheResolverFactory getFactory() {
        if (factory != null) {
            return factory;
        }
        factory = new PayaraCacheResolverFactory();
        Class defaultClazz = javax.cache.annotation.CacheResolverFactory.class;
        Class suggestedClazz = null;
        if (annotation instanceof CachePut) {
            CachePut put = CachePut.class.cast(annotation);
            suggestedClazz = put.cacheResolverFactory();
        } else if (annotation instanceof CacheRemove) {
            CacheRemove put = CacheRemove.class.cast(annotation);
            suggestedClazz = put.cacheResolverFactory();
        } else if (annotation instanceof CacheResult) {
            CacheResult put = CacheResult.class.cast(annotation);
            suggestedClazz = put.cacheResolverFactory();
        } else if (annotation instanceof CacheRemoveAll) {
            CacheRemoveAll put = CacheRemoveAll.class.cast(annotation);
            suggestedClazz = put.cacheResolverFactory();
        }

        if (suggestedClazz.equals(defaultClazz) && defaults != null) {
            suggestedClazz = defaults.cacheResolverFactory();
        }

        if (!defaultClazz.equals(suggestedClazz)) {
            try {
                factory = (CacheResolverFactory) suggestedClazz.newInstance();
            } catch (InstantiationException | IllegalAccessException ex) {
                Logger.getLogger(PayaraCacheKeyInvocationContext.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return factory;
    }

    public final CacheKeyGenerator getGenerator() {
        if (generator != null) {
            return generator;
        }
        generator = new PayaraCacheKeyGenerator();
        Class defaultClazz = javax.cache.annotation.CacheKeyGenerator.class;
        Class suggestedClazz = null;
        if (annotation instanceof CachePut) {
            CachePut put = CachePut.class.cast(annotation);
            suggestedClazz = put.cacheKeyGenerator();
        } else if (annotation instanceof CacheRemove) {
            CacheRemove put = CacheRemove.class.cast(annotation);
            suggestedClazz = put.cacheKeyGenerator();
        } else if (annotation instanceof CacheResult) {
            CacheResult put = CacheResult.class.cast(annotation);
            suggestedClazz = put.cacheKeyGenerator();
        }

        if (suggestedClazz.equals(defaultClazz) && defaults != null) {
            suggestedClazz = defaults.cacheKeyGenerator();
        }

        if (!defaultClazz.equals(suggestedClazz)) {
            try {
                generator = (CacheKeyGenerator) suggestedClazz.newInstance();
            } catch (InstantiationException | IllegalAccessException ex) {
                Logger.getLogger(PayaraCacheKeyInvocationContext.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return generator;
    }

    @Override
    public CacheInvocationParameter[] getAllParameters() {
        Class parameters[] = getMethod().getParameterTypes();
        Annotation annotations[][] = getMethod().getParameterAnnotations();
        Object values[] = ctx.getParameters();
        CacheInvocationParameter result[] = new CacheInvocationParameter[parameters.length];
        if (values != null) {
            for (int i = 0; i < values.length; i++) {
                Object value = values[i];
                result[i] = new PayaraCacheInvocationParameter(parameters[i], annotations[i], value, i);
            }
        }
        return result;
    }

    @Override
    public Object unwrap(Class type) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Method getMethod() {
        return ctx.getMethod();
    }

    @Override
    public Set getAnnotations() {
        HashSet<Annotation> result = new HashSet<>();
        Annotation annotations[] = getMethod().getAnnotations();
        for (Annotation annotation1 : annotations) {
            result.add(annotation1);
        }
        return result;
    }

    @Override
    public String getCacheName() {
        String result = null;

        if (annotation instanceof CachePut) {
            CachePut put = CachePut.class.cast(annotation);
            result = put.cacheName();
        } else if (annotation instanceof CacheRemove) {
            CacheRemove put = CacheRemove.class.cast(annotation);
            result = put.cacheName();
        } else if (annotation instanceof CacheRemoveAll) {
            CacheRemoveAll put = CacheRemoveAll.class.cast(annotation);
            result = put.cacheName();
        } else if (annotation instanceof CacheResult) {
            CacheResult put = CacheResult.class.cast(annotation);
            result = put.cacheName();
        }

        if ((result == null || result.isEmpty()) && (defaults != null)) {
            result = defaults.cacheName();
        }

        if ((result == null) || (result.isEmpty())) {
            String targetClassName = ctx.getTarget().getClass().getName();
            String methodName = ctx.getMethod().getName();
            Object params[] = ctx.getParameters();
            StringBuilder cacheName = new StringBuilder(targetClassName);
            cacheName.append('.').append(methodName).append('(');
            if (params != null) {
                for (int i = 0; i < params.length; i++) {
                    cacheName.append(params[i].getClass().getName());
                    if (i != params.length - 1) {
                        cacheName.append(',');
                    }
                }
            }
            cacheName.append(')');
            result = cacheName.toString();
        }
        return result;
    }

    @Override
    public A getCacheAnnotation() {
        return annotation;
    }

    @Override
    public CacheInvocationParameter[] getKeyParameters() {
        CacheInvocationParameter allParams[] = getAllParameters();
        LinkedList<CacheInvocationParameter> result = new LinkedList<>();

        // add only CacheKey elements
        for (CacheInvocationParameter result1 : allParams) {
            for (Annotation annotation : result1.getAnnotations()) {
                if (annotation.annotationType().equals(CacheKey.class)) {
                    result.add(result1);
                }
            }
        }

        if (result.isEmpty()) {
            // we need to rescan and add all parameters except any CacheValue annotated parameters
            for (CacheInvocationParameter result1 : allParams) {
                boolean add = true;
                for (Annotation annotation : result1.getAnnotations()) {
                    if (annotation.annotationType().equals(CacheValue.class)) {
                        add = false;
                        break;
                    }
                }
                if (add) {
                    result.add(result1);
                }
            }
        }
        CacheInvocationParameter resultArray[] = new CacheInvocationParameter[result.size()];
        int i = 0;
        for (CacheInvocationParameter resultArray1 : result) {
            resultArray[i++] = resultArray1;
        }
        return resultArray;
    }

    @Override
    public CacheInvocationParameter getValueParameter() {
        CacheInvocationParameter result = null;
        CacheInvocationParameter allParams[] = getAllParameters();
        for (CacheInvocationParameter result1 : allParams) {
            for (Annotation annotation : result1.getAnnotations()) {
                if (annotation.annotationType().equals(CacheValue.class)) {
                    result = result1;
                    break;
                }
            }
        }
        return result;
    }

}
