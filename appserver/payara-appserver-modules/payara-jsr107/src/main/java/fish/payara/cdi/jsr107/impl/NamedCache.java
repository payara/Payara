/*

 DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

 Copyright (c) 2016 Payara Foundation. All rights reserved.

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
package fish.payara.cdi.jsr107.impl;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to be applied to a Cache @Inject point to define the cache configuration
 * for the Producer to configure the cache
 * @author steve
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({METHOD, FIELD, PARAMETER})
public @interface NamedCache {
    /**
     * The name of the Cache in the Cache Manager
     * @return 
     */
    String cacheName() default "";
    
    /**
     * The class of the Cache Keys
     * @return 
     */
    Class keyClass() default Object.class;
    
    /**
     * The class of the cache values
     * @return 
     */
    Class valueClass() default Object.class;
    
    /**
     * Are statistics enabled for the cache
     * @return 
     */
    boolean statisticsEnabled() default false;
    
    /**
     * Is Managemenet Enabled for the Cache
     * @return 
     */
    boolean managementEnabled() default false;
    
    /**
     * Is the cache configured for read through. If this is set to true a CacheLoader factory
     * class must also be specified
     * @return 
     */
    boolean readThrough() default false;
    
    /**
     * Is the cache configured for write through. If this is set a CacheWriter factory
     * class must be specified
     * @return 
     */
    boolean writeThrough() default false;
    
    /**
     * The factory class of the CacheLoader to be attached to the cache
     * @return 
     */
    Class cacheLoaderFactoryClass() default Object.class;
    
    /**
     * The factory class of the CacheWriter to be attached to the cache
     * @return 
     */
    Class cacheWriterFactoryClass() default Object.class;
    
    /**
     * The class of the expiry policy factory used to create an expiry policy for the cache
     */
    Class expiryPolicyFactoryClass() default Object.class;
}
