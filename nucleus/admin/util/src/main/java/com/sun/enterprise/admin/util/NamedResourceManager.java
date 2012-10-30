/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
 * 
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 * 
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 * 
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
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
package com.sun.enterprise.admin.util;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import javax.inject.Singleton;
import org.glassfish.api.admin.NamedResource;
import org.glassfish.logging.annotation.LogMessagesResourceBundle;
import org.glassfish.logging.annotation.LoggerInfo;
import org.jvnet.hk2.annotations.Service;

/**
 * Records classes that are annotated as NamedResources and specific instances
 * of such classes.
 * <p>
 * The NamedResource support mimics some of what's provided by the ConfigBean
 * implementation in hk2, primarily for security authorization support.  Developers
 * annotate a POJO class with @NamedResource.  If the class is a singleton (as
 * the top-level Domain interface is in the config structure) no further annos
 * are needed on the class.  If the class can have multiple occurrences then
 * each instance will need a unique identifier and the developer annotates that
 * field on the class with {@code @NamedResource.ID}.  
 * <p>
 * When some code has fully populated an instance of such a {@code @NamedResource} 
 * class it invokes the {@link #prepare(java.lang.Object) } method, passing
 * the just-created object.  The manager records the new instance and its
 * named resource children, primarily so
 * that authorization-related annotations such as {@code @AccessRequired.To} 
 * can refer to such an object.  The secure admin infrastructure can then 
 * automatically determine the resource name for that instance for use in 
 * authorization checks.
 * <p>
 * The named resource POJO class can have fields that point to other named 
 * resource classes or fields that are collections or arrays of named resource
 * classes.  
 * 
 * @author tjquinn
 */
@Service
@Singleton
public class NamedResourceManager {

    private final WeakHashMap<Class<?>, Model> models = new WeakHashMap<Class<?>, Model>();
    private final WeakHashMap<Object, String> instanceNames = new WeakHashMap<Object, String>();
    
    private final String LINE_SEP = System.getProperty("line.separator");
    
    private static final Logger  ADMSEC_LOGGER = GenericAdminAuthenticator.ADMSEC_LOGGER;

    public <T> T register(T instance) throws IllegalArgumentException, IllegalAccessException {
        final Model model = findOrCreateModel(instance.getClass());
        if ( ! model.isPrimary) {
            throw new IllegalArgumentException(instance.getClass().getName() + " ! isPrimary");
        }
        final StringBuilder addedNames = new StringBuilder();
        final T result = register("", model.owningCollectionName, instance, model, addedNames);
        if (ADMSEC_LOGGER.isLoggable(Level.FINER)) {
            ADMSEC_LOGGER.log(Level.FINER, "Added named resources:\n{0}", addedNames.toString());
        }
        return result;
        
    }
    
    public String find(final Object resource) {
        return instanceNames.get(resource);
    }
    
    private <T> T register(String prefix, 
            final String containingCollectionName,
            final T instance,
            final StringBuilder addedNames) throws IllegalArgumentException, IllegalAccessException {
        final Model model = findOrCreateModel(instance.getClass());
        //return prepare(prefix, (model.isSingleton ? null : model.owningCollectionName + '/' + model.name + '/'), instance, model, addedNames);
        return register(prefix, containingCollectionName, instance, model, addedNames);
    }
    
    private <T> T register(String prefix, 
            String containingCollectionName, 
            final T instance, 
            final Model model, 
            final StringBuilder addedNames) throws IllegalArgumentException, IllegalAccessException {
        final String instanceName = prefix + 
                instanceName(instance, containingCollectionName, model);
        
        instanceNames.put(instance, instanceName);
        addedNames.append(instanceName).append(LINE_SEP);
        registerChildren(instanceName + "/", instance, model, addedNames);
        return instance;
    }
    
    private <T> void registerChildren(final String prefix, 
            final T instance, 
            final Model model, 
            final StringBuilder addedNames) throws IllegalArgumentException, IllegalAccessException {
        for (Field f : model.resourceNamedFields) {
            Object child = f.get(instance);
            if (f.getClass().isArray()) {
                for (int i = 0; i < Array.getLength(child); i++) {
                    register(prefix, collectionName(f), Array.get(child, i), addedNames);
                }
            } else if (Collection.class.isAssignableFrom(f.getType())) {
                final Collection<?> coll = (Collection<?>) child;
                for (Object o : coll) {
                    register(prefix, collectionName(f), o, addedNames);
                }
            } else {
                register(prefix, null, child, addedNames);
            }
        }
    }
    
    private String collectionName(final Field f) {
//        final Class<?> c = f.getClass();
//        final String shortClassName = c.getName().substring(c.getName().lastIndexOf('.'));
        return f.getName();
    }
    
    private Model findOrCreateModel(Class<?> c) {
        Model result = models.get(c);
        if (result == null) {
            result = buildModel(c);
            models.put(c, result);
        }
        return result;
    }
    
    private String instanceName(final Object instance, final String containingCollectionName, final Model model) throws IllegalArgumentException, IllegalAccessException {
        final StringBuilder sb = new StringBuilder();
        if (containingCollectionName != null && ! containingCollectionName.isEmpty()) {
            sb.append(containingCollectionName).append('/').append(model.subpath).append('/');
        }
        return sb.append(model.keyField.get(instance).toString()).toString();
    }
    
    private Model buildModel(final Class<?> c) {
        final Model model = new Model(c);
        return model;
    }
    
    /**
     * 
     */
    private static class Model {
        private boolean isPrimary;
        private boolean isSingleton;
        private String owningCollectionName;
        private String subpath;
        private final Collection<Field> resourceNamedFields = new ArrayList<Field>();
        private Field keyField;
        
        /*
         * Next block stolen shamelessly from the hk2 config Dom class, pending
         * a slight refactoring of that code there to expose the part we need.
         */
        static final Pattern TOKENIZER;
        private static String split(String lookback,String lookahead) {
            return "((?<="+lookback+")(?="+lookahead+"))";
        }
        private static String or(String... tokens) {
            StringBuilder buf = new StringBuilder();
            for (String t : tokens) {
                if(buf.length()>0)  buf.append('|');
                buf.append(t);
            }
            return buf.toString();
        }
        static {
            String pattern = or(
                    split("x","X"),     // AbcDef -> Abc|Def
                    split("X","Xx"),    // USArmy -> US|Army
                    //split("\\D","\\d"), // SSL2 -> SSL|2
                    split("\\d","\\D")  // SSL2Connector -> SSL|2|Connector
            );
            pattern = pattern.replace("x","\\p{Lower}").replace("X","\\p{Upper}");
            TOKENIZER = Pattern.compile(pattern);
        }
        
        private static String convertName(final String name) {
            // tokenize by finding 'x|X' and 'X|Xx' then insert '-'.
            StringBuilder buf = new StringBuilder(name.length()+5);
            for(String t : TOKENIZER.split(name)) {
                if(buf.length()>0)  buf.append('-');
                buf.append(t.toLowerCase(Locale.ENGLISH));
            }
            return buf.toString();  
        }
        
        /* end of shameless copy */
        
        private static String defaultSubpath(final Class<?> c) {
            final String className = convertName(c.getName());
            final String shortClassName = className.substring(className.lastIndexOf('.') + 1);
            return shortClassName;
        }
        
        private static String defaultCollectionName(final Class<?> c) {
            return pluralize(defaultSubpath(c));
        }
        
        private static String pluralize(final String s) {
            final char lastChar = s.charAt(s.length() - 1);
            if (lastChar == 's' || lastChar == 'S' || lastChar == 'x' || lastChar == 'X') {
                return s + "es";
            } else {
                return s + "s";
            }
        }
        
        private Model(final Class<?> c) {
                
            /*
             * Any field that is also annotated with @NamedResource is a child
             * that itself is treated as named within the containing class.
             * Record such fields so when the developer prepares an instance
             * of the containing object we will also prepare the children's names
             * within their container.
             */
            boolean isNamedResource = false;
            isSingleton = true;
            for (ClassLineageIterator cIT = new ClassLineageIterator(c); cIT.hasNext();) {
                final Class<?> currentClass = cIT.next();
                final NamedResource r = currentClass.getAnnotation(NamedResource.class);
                if (r != null) {
                    isNamedResource = true;
                    isPrimary = r.isPrimary();
                    subpath = (r.subpath().isEmpty() ? defaultSubpath(c) : r.subpath());
                    for (Field f : currentClass.getDeclaredFields()) {
                        f.setAccessible(true);
                        if (f.getAnnotation(NamedResource.class) != null) {
                            resourceNamedFields.add(f);
                        }
                        if (f.getAnnotation(NamedResource.Key.class) != null) {
                            if (f.getType().isArray()) {
                                throw new IllegalArgumentException(c.getName() + "@NamedResource.ID");
                            }
                            keyField = f;
                        }
                    }
                    if (isSingleton) {
                        if ( ! r.collectionName().isEmpty()) {
                            throw new IllegalArgumentException(c.getName() + " @NamedResource isSingleton & collectionName");
                        }
                        owningCollectionName = null;
                    } else {
                        owningCollectionName = (r.collectionName().isEmpty() ? defaultCollectionName(c) : r.collectionName());
                    }
                
                    
                }
            }
            if ( ! isNamedResource) {
                throw new IllegalArgumentException(c.getName() + " ! @NamedResource");
            }
            if (keyField == null) {
                throw new IllegalArgumentException(c.getName() + " ! @NamedResource.ID");
            }
        }
    }
    
}
