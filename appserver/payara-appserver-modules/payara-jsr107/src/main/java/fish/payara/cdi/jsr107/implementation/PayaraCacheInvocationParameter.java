/*

 DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

 Copyright (c) 2016-2017 Payara Foundation. All rights reserved.

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
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import javax.cache.annotation.CacheInvocationParameter;

/**
 *
 * @author steve
 */
public class PayaraCacheInvocationParameter implements CacheInvocationParameter {
    
    private final Class clazz;
    private final Annotation annotations[];
    private final Object value;
    private final int position;

    public PayaraCacheInvocationParameter(Class clazz, Annotation annotations[], Object value, int position) {
        this.clazz = clazz;
        this.annotations = annotations;
        this.value = value;
        this.position = position;
    }

    @Override
    public Class<?> getRawType() {
        return clazz;
    }

    @Override
    public Object getValue() {
        return value;
    }

    @Override
    public Set<Annotation> getAnnotations() {
        HashSet<Annotation> result = new HashSet<>(Arrays.asList(annotations));
        return result;
    }

    @Override
    public int getParameterPosition() {
        return position;
    }
    
}
