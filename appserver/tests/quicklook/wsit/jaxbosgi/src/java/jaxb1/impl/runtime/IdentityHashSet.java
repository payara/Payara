/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved.
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

package jaxb1.impl.runtime;

/**
 * A set of {@link Object}s that uses the == (instead of equals)
 * for the comparison.
 * 
 * @author
 *     Kohsuke Kawaguchi (kohsuke.kawaguchi@sun.com)
 */
final class IdentityHashSet {
    /** The hash table data. */
    private Object table[];

    /** The total number of mappings in the hash table. */
    private int count;

    /**
     * The table is rehashed when its size exceeds this threshold.  (The
     * value of this field is (int)(capacity * loadFactor).)
     */
    private int threshold;

    /** The load factor for the hashtable. */
    private static final float loadFactor = 0.3f;
    private static final int initialCapacity = 191;


    public IdentityHashSet() {
        table = new Object[initialCapacity];
        threshold = (int) (initialCapacity * loadFactor);
    }

    public boolean contains(Object key) {
        Object tab[] = table;
        int index = (System.identityHashCode(key) & 0x7FFFFFFF) % tab.length;

        while (true) {
            final Object e = tab[index];
            if (e == null)
                return false;
            if (e==key)
                return true;
            index = (index + 1) % tab.length;
        }
    }

    /**
     * rehash.
     * 
     * It is possible for one thread to call get method
     * while another thread is performing rehash.
     * Keep this in mind.
     */
    private void rehash() {
        // create a new table first.
        // meanwhile, other threads can safely access get method.
        int oldCapacity = table.length;
        Object oldMap[] = table;

        int newCapacity = oldCapacity * 2 + 1;
        Object newMap[] = new Object[newCapacity];

        for (int i = oldCapacity; i-- > 0;)
            if (oldMap[i] != null) {
                int index = (System.identityHashCode(oldMap[i]) & 0x7FFFFFFF) % newMap.length;
                while (newMap[index] != null)
                    index = (index + 1) % newMap.length;
                newMap[index] = oldMap[i];
            }

        // threshold is not accessed by get method.
        threshold = (int) (newCapacity * loadFactor);
        // switch!
        table = newMap;
    }

    public boolean add(Object newObj) {
        if (count >= threshold)
            rehash();

        Object tab[] = table;
        int index = (System.identityHashCode(newObj) & 0x7FFFFFFF) % tab.length;

        Object existing;
        
        while ((existing=tab[index]) != null) {
            if(existing==newObj)    return false;
            index = (index + 1) % tab.length;
        }
        tab[index] = newObj;

        count++;
        
        return true;
    }
}
