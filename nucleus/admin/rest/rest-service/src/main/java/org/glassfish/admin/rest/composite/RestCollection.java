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

import java.util.*;

/**
 *
 * @author jdlee
 */
public class RestCollection implements Map<RestModelMetadata, RestModel> {
    private List<RestModel> models = new ArrayList();
    private List<RestModelMetadata> metadata = new ArrayList<RestModelMetadata>();

    public void put(String id, RestModel model) {
        models.add(model);
        metadata.add(new RestModelMetadata(id));
    }

    public RestModel get(String id) {
        return get (new RestModelMetadata(id));
    }

    public RestModel remove(String id) {
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

        for (RestModel rm : models) {
            if (rm.equals(desired)) {
                found = true;
                break;
            }
        }

        return found;
    }

    @Override
    public RestModel get(Object key) {
        checkClass(RestModelMetadata.class, key.getClass());
        RestModelMetadata desired = (RestModelMetadata) key;
        RestModel result = null;

        for (int index = 0, total = metadata.size(); index < total; index++) {
            if (metadata.get(index).equals(desired)) {
                result = models.get(index);
                break;
            }
        }

        return result;
    }

    @Override
    public RestModel put(RestModelMetadata key, RestModel value) {
        models.add(value);
        metadata.add(key);

        return value;
    }

    @Override
    public RestModel remove(Object key) {
        checkClass(RestModelMetadata.class, key.getClass());
        RestModelMetadata desired = (RestModelMetadata) key;
        RestModel result = null;

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
    public void putAll(Map<? extends RestModelMetadata, ? extends RestModel> m) {
        for (Map.Entry<? extends RestModelMetadata, ? extends RestModel> entry : m.entrySet()) {
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
    public Collection<RestModel> values() {
        return new RestModelSet(models);
    }

    @Override
    public Set<Entry<RestModelMetadata, RestModel>> entrySet() {
        if (metadata.size() != models.size()) {
            throw new IllegalStateException("InternalError: keys and values out of sync");
        }
        ArrayList al = new ArrayList();
        for (int i = 0; i < metadata.size(); i++) {
            al.add(new RestCollectionEntry(metadata.get(i), models.get(i)));
        }
        return new TreeSet<Entry<RestModelMetadata, RestModel>>(al);
    }

    protected void checkClass(Class<?> desired, Class<?> given) throws IllegalArgumentException {
        if (!desired.isAssignableFrom(given)) {
            throw new IllegalArgumentException("Expected " + desired.getName() + ". Found " + given.getName());
        }
    }

    private class RestCollectionEntry implements Map.Entry<RestModelMetadata, RestModel>, Comparable {
        private RestModelMetadata key;
        private RestModel value;

        public RestCollectionEntry(RestModelMetadata key, RestModel value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public RestModelMetadata getKey() {
            return key;
        }

        @Override
        public RestModel getValue() {
            return value;
        }

        @Override
        public RestModel setValue(RestModel newValue) {
            put(key, newValue);

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

    private static class RestModelSet extends AbstractSet<RestModel> {
        private List<RestModel> items;

        private RestModelSet(List<RestModel> items) {
            this.items = items;
        }

        @Override
        public Iterator<RestModel> iterator() {
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
            for (RestModel item : items) {
                if (CompositeUtil.compare(item, o)) {
                    return true;
                }
            }
            return false;
        }
*/
    }
}
