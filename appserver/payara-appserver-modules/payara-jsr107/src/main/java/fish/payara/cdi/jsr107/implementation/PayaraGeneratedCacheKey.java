/*

 DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

 Copyright (c) 2016-2018 Payara Foundation. All rights reserved.

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

import java.io.Serializable;
import java.util.Arrays;
import javax.cache.annotation.CacheInvocationParameter;
import javax.cache.annotation.GeneratedCacheKey;

/**
 *
 * @author steve
 */
public class PayaraGeneratedCacheKey implements GeneratedCacheKey, Serializable {
    
    private static final long serialVersionUID = -3982698381435289430L;
    
    private final int hashCode;
    private final Object values[];
    
    public PayaraGeneratedCacheKey(Object params[]) {
        values = params;
        hashCode = Arrays.deepHashCode(values);
    }

    PayaraGeneratedCacheKey(CacheInvocationParameter[] parameters) {
        
        values = new Object[parameters.length];
        for (int i = 0; i < values.length; i++) {
            CacheInvocationParameter parameter = parameters[i];
            values[i] = parameter.getValue();
        }
        hashCode = Arrays.deepHashCode(values);
    }

    @Override
    public boolean equals(Object obj) {
        boolean result = false;
        
        if (obj == null) {
            return false;
        }
        
        if (this == obj) {
            return true;
        }
        
        if (!obj.getClass().equals(this.getClass())) {
            return false;
        }
        
        if (this.hashCode != obj.hashCode()) {
            return false;
        }
        
        PayaraGeneratedCacheKey other = (PayaraGeneratedCacheKey)obj;
        if (Arrays.deepEquals(this.values, other.values)) {
            return true;
        }
        return result;
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public String toString() {
        return "PayaraGeneratedCacheKey{" + "values=" + Arrays.deepToString(values) + '}';
    }
}
