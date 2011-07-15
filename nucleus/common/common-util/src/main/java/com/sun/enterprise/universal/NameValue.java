/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008-2010 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.universal;

import java.util.*;

/**
 * This class is needed so often in so many places.  It is centralized once and
 * for all here.
 * @param K key type
 * @param V value type
 * @author bnevins
 */
public class NameValue<K, V> {
    /**
     * 
     * @param k key
     * @param v value
     */
    public NameValue(K k, V v)
    {
        if(k == null)
            throw new NullPointerException();
        
        value = v;
        key = k;
    }

    public K getName    () {
        return key;
    }

    public V getValue() {
        return value;
    }

    public V setValue(V newValue) {
        V oldValue = value;
        value = newValue;
        return oldValue;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof NameValue))
            return false;
        
        NameValue nv = (NameValue) o;
        Object k1 = getName();
        Object k2 = nv.getName();
        if (k1 == k2 || k1.equals(k2)) {
            Object v1 = getValue();
            Object v2 = nv.getValue();
            
            if (v1 == v2 || (v1 != null && v1.equals(v2)))
                return true;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return key.hashCode() ^ (value == null ? 0 : value.hashCode());
    }

    @Override
    public String toString() {
        return getName() + "=" + getValue();
    }

    private final K key;
    private V value;
}

