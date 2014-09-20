/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.jdo.spi.persistence.utility;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;

import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * A WeakValueHashMap is implemented as a HashMap that maps keys to
 * WeakValues.  Because we don't have access to the innards of the
 * HashMap, we have to wrap/unwrap value objects with WeakValues on
 * every operation.  Fortunately WeakValues are small, short-lived
 * objects, so the added allocation overhead is tolerable. This
 * implementaton directly extends java.util.HashMap.
 *
 * @author	Markus Fuchs
 * @see		java.util.HashMap
 * @see     java.lang.ref.WeakReference
 */

public class WeakValueHashMap extends HashMap {

    /**
     *  Reference queue for cleared WeakValues.
     *  We do not expect instance of this class to be serialized. Marking a non serializable member as transient to make findbugs happy.
     */
    private transient ReferenceQueue queue = new ReferenceQueue();

    /**
     * Returns the number of key-value mappings in this map.<p>
     * @return the number of key-value mappings in this map.
     */
    public int size() {
        // delegate to entrySet, as super.size() also counts WeakValues
        return entrySet().size();
    }

    /**
     * Returns <tt>true</tt> if this map contains no key-value mappings.<p>
     * @return <tt>true</tt> if this map contains no key-value mappings.
     */
    public boolean isEmpty() {
        return size() == 0;
    }

    /**
     * Returns <tt>true</tt> if this map contains a mapping for the specified
     * key.<p>
     * @param key key whose presence in this map is to be tested
     * @return <tt>true</tt> if this map contains a mapping for the specified
     * key.
     */
    public boolean containsKey(Object key) {
        // need to clean up gc'ed values before invoking super method
        processQueue();
        return super.containsKey(key);
    }

   /**
     * Returns <tt>true</tt> if this map maps one or more keys to the
     * specified value.<p>
     * @param value value whose presence in this map is to be tested
     * @return <tt>true</tt> if this map maps one or more keys to this value.
     */
    public boolean containsValue(Object value) {
        return super.containsValue(WeakValue.create(value));
    }

    /**
     * Gets the value for the given key.<p>
     * @param key key whose associated value, if any, is to be returned
     * @return the value to which this map maps the specified key.
     */
    public Object get(Object key) {
        // We don't need to remove garbage collected values here;
        // if they are garbage collected, the get() method returns null;
        // the next put() call with the same key removes the old value
        // automatically so that it can be completely garbage collected
        return getReferenceObject((WeakReference) super.get(key));
    }

    /**
     * Puts a new (key,value) into the map.<p>
     * @param key key with which the specified value is to be associated.
     * @param value value to be associated with the specified key.
     * @return previous value associated with specified key, or null
     * if there was no mapping for key or the value has been garbage
     * collected by the garbage collector.
     */
    public Object put(Object key, Object value) {
        // If the map already contains an equivalent key, the new key
        // of a (key, value) pair is NOT stored in the map but the new
        // value only. But as the key is strongly referenced by the
        // map, it can not be removed from the garbage collector, even
        // if the key becomes weakly reachable due to the old
        // value. So, it isn't necessary to remove all garbage
        // collected values with their keys from the map before the
        // new entry is made. We only clean up here to distribute
        // clean up calls on different operations.
        processQueue();

        WeakValue oldValue = 
            (WeakValue)super.put(key, WeakValue.create(key, value, queue));
        return getReferenceObject(oldValue);
    }

    /**
     * Removes key and value for the given key.<p>
     * @param key key whose mapping is to be removed from the map.
     * @return previous value associated with specified key, or null
     * if there was no mapping for key or the value has been garbage
     * collected by the garbage collector.
     */
    public Object remove(Object key) {
        return getReferenceObject((WeakReference) super.remove(key));
    }

    /**
     * A convenience method to return the object held by the
     * weak reference or <code>null</code> if it does not exist.
     */
    private final Object getReferenceObject(WeakReference ref) {
        return (ref == null) ? null : ref.get();
    }

    /**
     * Removes all garbage collected values with their keys from the map.
     * Since we don't know how much the ReferenceQueue.poll() operation
     * costs, we should not call it every map operation.
     */
    private void processQueue() {
        WeakValue wv = null;

        while ((wv = (WeakValue) this.queue.poll()) != null) {
            // "super" is not really necessary but use it
            // to be on the safe side
            super.remove(wv.key);
        }
    }

    /* -- Helper classes -- */

    /**
     * We need this special class to keep the backward reference from
     * the value to the key, so that we are able to remove the key if
     * the value is garbage collected.
     */
    private static class WeakValue extends WeakReference {
        /**
         * It's the same as the key in the map. We need the key to remove
         * the value if it is garbage collected.
         */
        private Object key;

        private WeakValue(Object value) {
            super(value);
        }

        /**
         * Creates a new weak reference without adding it to a
         * ReferenceQueue.
         */
	private static WeakValue create(Object value) {
	    if (value == null) return null;
	    else return new WeakValue(value);
        }

        private WeakValue(Object key, Object value, ReferenceQueue queue) {
            super(value, queue);
            this.key = key;
        }

        /**
         * Creates a new weak reference and adds it to the given queue.
         */
        private static WeakValue create(Object key, Object value, 
                                        ReferenceQueue queue) {
	    if (value == null) return null;
	    else return new WeakValue(key, value, queue);
        }

        /**
         * A WeakValue is equal to another WeakValue iff they both refer
         * to objects that are, in turn, equal according to their own
         * equals methods.
         */
        public boolean equals(Object obj) {
            if (this == obj)
                return true;

            if (!(obj instanceof WeakValue))
                return false;

            Object ref1 = this.get();
            Object ref2 = ((WeakValue) obj).get();

            if (ref1 == ref2)
                return true;

            if ((ref1 == null) || (ref2 == null))
                return false;

            return ref1.equals(ref2);
        }

        /**
         *
         */
        public int hashCode() {
            Object ref = this.get();

            return (ref == null) ? 0 : ref.hashCode();
        }
    }

    /** 
     * Internal class for entries. This class wraps/unwraps the
     * values of the Entry objects returned from the underlying map.
     */
    private class Entry implements Map.Entry {
        private Map.Entry ent;
        private Object value;	/* Strong reference to value, so that the
				   GC will leave it alone as long as this
				   Entry exists */

        Entry(Map.Entry ent, Object value) {
            this.ent = ent;
            this.value = value;
        }

        public Object getKey() {
            return ent.getKey();
        }

        public Object getValue() {
            return value;
        }

        public Object setValue(Object value) {
            // This call changes the map. Please see the comment on 
            // the put method for the correctness remark.
            Object oldValue = this.value;
            this.value = value;
            ent.setValue(WeakValue.create(getKey(), value, queue));
            return oldValue;
        }

        private boolean valEquals(Object o1, Object o2) {
            return (o1 == null) ? (o2 == null) : o1.equals(o2);
        }

        public boolean equals(Object o) {
            if (!(o instanceof Map.Entry)) return false;
            Map.Entry e = (Map.Entry) o;
            return (valEquals(ent.getKey(), e.getKey())
                    && valEquals(value, e.getValue()));
        }

        public int hashCode() {
            Object k;
            return ((((k = ent.getKey()) == null) ? 0 : k.hashCode())
                    ^ ((value == null) ? 0 : value.hashCode()));
        }

    }

    /**
     * Internal class for entry sets to unwrap/wrap WeakValues stored
     * in the map.
     */
    private class EntrySet extends AbstractSet {

        public Iterator iterator() {
            // remove garbage collected elements
            processQueue();

            return new Iterator() {
                Iterator hashIterator = hashEntrySet.iterator();
                Entry next = null;

                public boolean hasNext() {
                    if (hashIterator.hasNext()) {
                        // since we removed garbage collected elements,
                        // we can simply return the next entry.
                        Map.Entry ent = (Map.Entry) hashIterator.next();
                        WeakValue wv = (WeakValue) ent.getValue();
                        Object v = (wv == null) ? null : wv.get();
                        next = new Entry(ent, v);
                        return true;
                    }
                    return false;
                }

                public Object next() {
                    if ((next == null) && !hasNext())
                        throw new NoSuchElementException();
                    Entry e = next;
                    next = null;
                    return e;
                }

                public void remove() {
                    hashIterator.remove();
                }

            };
        }

        public boolean isEmpty() {
            return !(iterator().hasNext());
        }

        public int size() {
            int j = 0;
            for (Iterator i = iterator(); i.hasNext(); i.next()) j++;
            return j;
        }

        public boolean remove(Object o) {
            if (!(o instanceof Map.Entry)) return false;
            Map.Entry e = (Map.Entry) o;
            Object ek = e.getKey();
            Object ev = e.getValue();
            Object hv = WeakValueHashMap.this.get(ek);
            if (hv == null) {
                // if the map's value is null, we have to check, if the
                // entry's value is null and the map contains the key
                if ((ev == null) && WeakValueHashMap.this.containsKey(ek)) {
                    WeakValueHashMap.this.remove(ek);
                    return true;
                } else {
                    return false;
                }
                // otherwise, simply compare the values
            } else if (hv.equals(ev)) {
                WeakValueHashMap.this.remove(ek);
                return true;
            }                
                
            return false;
        }

        public int hashCode() {
            int h = 0;
            for (Iterator i = hashEntrySet.iterator(); i.hasNext(); ) {
                Map.Entry ent = (Map.Entry) i.next();
                Object k;
                WeakValue wv = (WeakValue) ent.getValue();
                if (wv == null) continue;
                h += ((((k = ent.getKey()) == null) ? 0 : k.hashCode())
                        ^ wv.hashCode());
            }
            return h;
        }

    }

    // internal helper variable, because we can't access
    // entrySet from the superclass inside the EntrySet class
    private Set hashEntrySet = null;
    // stores the EntrySet instance
    private Set entrySet = null;

    /**
     * Returns a <code>Set</code> view of the mappings in this map.<p>
     * @return a <code>Set</code> view of the mappings in this map.
     */
    public Set entrySet() {
        if (entrySet == null) {
            hashEntrySet = super.entrySet();
            entrySet = new EntrySet();
        }
        return entrySet;
    }

    // stores the value collection
    private transient Collection values = null;

    /**
     * Returns a <code>Collection</code> view of the values contained
     * in this map.<p>
     * @return a <code>Collection</code> view of the values contained
     * in this map.
     */
    public Collection values() {
        // delegates to entrySet, because super method returns
        // WeakValues instead of value objects
	if (values == null) {
	    values = new AbstractCollection() {
		public Iterator iterator() {
		    return new Iterator() {
			private Iterator i = entrySet().iterator();

			public boolean hasNext() {
			    return i.hasNext();
			}

			public Object next() {
			    return ((Entry)i.next()).getValue();
			}

			public void remove() {
			    i.remove();
			}
                    };
                }

		public int size() {
		    return WeakValueHashMap.this.size();
		}

		public boolean contains(Object v) {
		    return WeakValueHashMap.this.containsValue(v);
		}
	    };
	}
	return values;
    }

}
