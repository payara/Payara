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
package org.glassfish.admin.rest.composite;

import org.glassfish.admin.rest.composite.metadata.RestModelMetadata;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

/**
 *
 * @author jdlee
 */
public class RestCollection<T> implements Map<RestModelMetadata, T> {
    private List<T> models = new ArrayList();
    private List<RestModelMetadata> metadata = new ArrayList<RestModelMetadata>();

    public void put(String id, T model) {
        models.add(model);
        metadata.add(new RestModelMetadata(id));
    }

    public T get(String id) {
        return get (new RestModelMetadata(id));
    }

    public T remove(String id) {
        return remove (new RestModelMetadata(id));
    }

    public boolean containsKey(String id) {
        return containsKey (new RestModelMetadata(id));
    }

    @Override
    public int size() {
        return models.size();
    }

    @Override
    public boolean isEmpty() {
        return models.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        checkClass(RestModelMetadata.class, key.getClass());
        RestModelMetadata desired = (RestModelMetadata) key;
        boolean found = false;

        for (RestModelMetadata md : metadata) {
            if (md.equals(desired)) {
                found = true;
                break;
            }
        }

        return found;
    }

    @Override
    public boolean containsValue(Object value) {
        checkClass(RestModel.class, value.getClass());
        RestModel desired = (RestModel) value;
        boolean found = false;

        for (T rm : models) {
            if (rm.equals(desired)) {
                found = true;
                break;
            }
        }

        return found;
    }

    @Override
    public T get(Object key) {
        checkClass(RestModelMetadata.class, key.getClass());
        RestModelMetadata desired = (RestModelMetadata) key;
        T result = null;

        for (int index = 0, total = metadata.size(); index < total; index++) {
            if (metadata.get(index).equals(desired)) {
                result = models.get(index);
                break;
            }
        }

        return result;
    }

    @Override
    public T put(RestModelMetadata key, T value) {
        models.add(value);
        metadata.add(key);

        return value;
    }

    @Override
    public T remove(Object key) {
        checkClass(RestModelMetadata.class, key.getClass());
        RestModelMetadata desired = (RestModelMetadata) key;
        T result = null;

        for (int index = 0, total = metadata.size(); index < total; index++) {
            if (metadata.get(index).equals(desired)) {
                result = models.get(index);
                models.remove(index);
                metadata.remove(index);
                break;
            }
        }

        return result;
    }

    @Override
    public void putAll(Map<? extends RestModelMetadata, ? extends T> m) {
        for (Map.Entry<? extends RestModelMetadata, ? extends T> entry : m.entrySet()) {
            metadata.add(entry.getKey());
            models.add(entry.getValue());
        }
    }

    @Override
    public void clear() {
        models.clear();
        metadata.clear();
    }

    @Override
    public Set<RestModelMetadata> keySet() {
        return new TreeSet<RestModelMetadata>(metadata);
    }

    @Override
    public Collection<T> values() {
        return new RestModelSet(models);
    }

    @Override
    public Set<Entry<RestModelMetadata, T>> entrySet() {
        if (metadata.size() != models.size()) {
            throw new IllegalStateException("InternalError: keys and values out of sync");
        }
        ArrayList al = new ArrayList();
        for (int i = 0; i < metadata.size(); i++) {
            al.add(new RestCollectionEntry(metadata.get(i), models.get(i)));
        }
        return new TreeSet<Entry<RestModelMetadata, T>>(al);
    }

    protected void checkClass(Class<?> desired, Class<?> given) throws IllegalArgumentException {
        if (!desired.isAssignableFrom(given)) {
            throw new IllegalArgumentException("Expected " + desired.getName() + ". Found " + given.getName());
        }
    }

    private class RestCollectionEntry<T> implements Map.Entry<RestModelMetadata, T>, Comparable {
        private RestModelMetadata key;
        private T value;

        public RestCollectionEntry(RestModelMetadata key, T value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public RestModelMetadata getKey() {
            return key;
        }

        @Override
        public T getValue() {
            return value;
        }

        @Override
        public T setValue(T newValue) {
            value = newValue;
            return newValue;
        }

        @Override
        public int compareTo(Object o) {
            if (!(o instanceof RestCollectionEntry)) {
                throw new IllegalArgumentException(
                        "Huh? Not a MapEntry?");
            }
            Object otherKey = ((RestCollectionEntry) o).getKey();
            return ((Comparable) key).compareTo((Comparable) otherKey);
        }
    }

    private static class RestModelSet<T> extends AbstractSet<T> {
        private List<T> items;

        private RestModelSet(List<T> items) {
            this.items = items;
        }

        @Override
        public Iterator<T> iterator() {
            return items.iterator();
        }

        @Override
        public int size() {
            return items.size();
        }

        @Override
        public boolean containsAll(Collection<?> objects) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        /*
        @Override
        public boolean contains(Object o) {
            for (T item : items) {
                if (CompositeUtil.compare(item, o)) {
                    return true;
                }
            }
            return false;
        }
*/
    }
}
