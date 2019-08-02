/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2019 Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
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
package fish.payara.monitoring.collect;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BiConsumer;

/**
 * API for collecting monitoring data points from a {@link MonitoringDataSource}.
 * 
 * @author Jan Bernitt
 */
public interface MonitoringDataCollector {

    /**
     * Collect a single metric data point (within the current context of tags of this collector).
     * 
     * @param key the plain (context free) name of the metric (e.g. "size")
     * @param value the current value of the metric
     * @return this collector for chaining (with unchanged tags)
     */
    MonitoringDataCollector collect(CharSequence key, long value);

    /**
     * Creates a collector with an extended context.
     * 
     * For example if this collector has the context "foo=bar" and this method is called with name "x" and value "y" the
     * resulting context is "foo=bar x=y".
     * 
     * When the context of this collector already contains the tag name the context restarts from that tag regardless if
     * the value is different or identical to the previous one. For example if this collector has the context "foo=bar
     * x=y" and tag is called with name "foo" and value "baz" the new context is "foo=baz" (not "foo=bar x=y foo=baz").
     * 
     * @param name  name of the type of context
     * @param value identifier within the context type, if <code>null</code> or empty the tag is ignored
     * @return A collector that collects data within the context of this collector extended by the given name-value
     *         pair.
     */
    MonitoringDataCollector tag(CharSequence name, CharSequence value);


    /*
     * Helper methods for convenience and consistent tagging.
     */


    /**
     * Same as {@link #collect(CharSequence, long)} except that zero value are ignored and not collected.
     */
    default MonitoringDataCollector collectNonZero(CharSequence key, long value) {
        return value != 0L ? collect(key, value) : this;
    }

    /**
     * Similar to {@link #collect(CharSequence, long)}. Double values are converted to long by multiplying with 10K
     * effectively offering a precision of 4 fraction digits when converting back to FP number later on.
     */
    default MonitoringDataCollector collect(CharSequence key, double value) {
        return collect(key, Math.round(value * 10000L));
    }

    /**
     * Similar to {@link #collect(CharSequence, long)}, true becomes 1L, false zero. 
     */
    default MonitoringDataCollector collect(CharSequence key, boolean value) {
        return collect(key, value ? 1L : 0L);
    }

    /**
     * Similar to {@link #collect(CharSequence, long)}, the char simply becomes a number
     */
    default MonitoringDataCollector collect(CharSequence key, char value) {
        return collect(key, (long) value);
    }

    /**
     * Ignores <code>null</code>, collects {@link Double} and {@link Float} using
     * {@link #collect(CharSequence, double)}, others using {@link #collect(CharSequence, long)}.
     */
    default MonitoringDataCollector collect(CharSequence key, Number value) {
        if (value != null) {
            if (value instanceof Double || value instanceof Float) {
                return collect(key, value.doubleValue());
            }
            return collect(key, value.longValue());
        }
        return this;
    }

    /**
     * Ignores <code>null</code>, otherwise collects using {@link #collect(CharSequence, boolean)}.
     */
    default MonitoringDataCollector collect(CharSequence key, Boolean value) {
        return value != null ? collect(key, value.booleanValue()) : this;
    }

    /**
     * Same as calling {@link #collect(CharSequence, long)} with a non null value and {@link Instant#toEpochMilli()}.
     */
    default MonitoringDataCollector collect(CharSequence key, Instant value) {
        return value != null ?  collect(key, value.toEpochMilli()) : this;
    }

    /**
     * Same as calling {@link BiConsumer#accept(Object, Object)} with this collector and the passed object.
     * @return this for chaining, added purely to allow fluent API use
     */
    default <V> MonitoringDataCollector collectObject(V obj, BiConsumer<MonitoringDataCollector, V> collect) {
        if (obj != null) {
            collect.accept(this, obj);
        }
        return this;
    }

    /**
     * Collects data points using the passed {@link BiConsumer} for every entry if the passed {@link Collection}.
     * The context is not changed
     * @param entries <code>null</code> or empty {@link Collection}s are ignored
     * @param collect the function collecting the individual data points
     * @return this for chaining (unchanged context)
     */
    default <V> MonitoringDataCollector collectObjects(Collection<V> entries, BiConsumer<MonitoringDataCollector, V> collect) {
        if (entries != null) {
            for (V entry : entries) {
                collect.accept(this, entry);
            }
        }
        return this;
    }

    default <K, V> MonitoringDataCollector collectAll(Map<K, V> entries,
            BiConsumer<MonitoringDataCollector, V> collect) {
        if (entries != null) {
            collectNonZero("size", entries.size());
            for (Entry<K,V> entry : entries.entrySet()) {
                K key = entry.getKey();
                CharSequence tag = key instanceof CharSequence ? (CharSequence) key : key.toString();
                collect.accept(entity(tag), entry.getValue());
            }
        }
        return this;
    }

    default <V> MonitoringDataCollector collectObject(V obj, Class<V> as) {
        for (Method getter : as.getDeclaredMethods()) {
            String name = getter.getName();
            int offset = name.startsWith("get") ? 3 : name.startsWith("is") ? 2 : 0;
            if (offset > 0) {
                String property = Character.toLowerCase(name.charAt(offset)) + name.substring(offset+1);
                Class<?> returnType = getter.getReturnType();
                try {
                    if (Boolean.class.isAssignableFrom(returnType) || returnType == boolean.class) {
                        collect(property, (Boolean) getter.invoke(obj));
                    } else if (Number.class.isAssignableFrom(returnType) || returnType.isPrimitive()) {
                        collect(property, (Number) getter.invoke(obj));
                    }
                } catch (Exception e) {
                    // try next
                }
            }
        }
        return this;
    }

    default MonitoringDataCollector app(CharSequence appName) {
        return tag("app", appName);
    }

    /**
     * Namespaces are used to distinguish data points of different origin.
     * Each origin uses its own namespace. The namespace should be the first (top/right most) tag added.
     *
     * @param namespace the namespace to use, e.g. "health-monitor"
     * @return A collector with the namespace context added/set
     */
    default MonitoringDataCollector in(CharSequence namespace) {
        return tag("ns", namespace);
    }

    /**
     * The type tag states the type of a collected entry. Most often used for collections of entries of the same kind.
     * The type is usually the second tag added after the namespace.
     *
     * @param type type of entity or collection of entities that are about to be collected
     * @return A collector with the type context added/set
     */
    default MonitoringDataCollector type(CharSequence type) {
        return tag("type", type);
    }

    /**
     * The entity tag states the identity that distinguishes values of one entry from another within the same collection
     * of entries. The entity is usually the third tag added after the namespace and type (if needed).
     *
     * @param entity the entity to use, e.g. "log-notifier" (as opposed to "teams-notifier")
     * @return A collector with the entity context added/set
     */
    default MonitoringDataCollector entity(CharSequence entity) {
        return tag("entity", entity);
    }

}
