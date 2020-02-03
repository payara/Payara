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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

/**
 * Utility to collect beans using their getter {@link Method}s.
 * 
 * Only return types a known collection function has been {@link #register(Class, BiConsumer)}ed for are included.
 * 
 * The usual usage of this helper is to call {@link #collectObject(MonitoringDataCollector, Object)}.
 */
public final class MonitoringDataCollection {

    private MonitoringDataCollection() {
        throw new UnsupportedOperationException("util");
    }

    private static final Map<Class<?>, BiConsumer<MonitoringDataCollector, ?>>  MAPPED_TYPES = new ConcurrentHashMap<>();

    /**
     * Can be used to plug in collection functions for types that otherwise would not be collected even for objects that
     * are passed to collection elsewhere.
     * 
     * @param type the type as stated by the return type of the getter method returning the value to collect from an object
     * @param collectWith the function to use to collect values of the given type
     */
    public static <T> void register(Class<T> type, BiConsumer<MonitoringDataCollector, T> collectWith) {
        MAPPED_TYPES.putIfAbsent(type, collectWith);
    }

    public static void collectObject(MonitoringDataCollector collector, Object obj) {
        for (Method getter : obj.getClass().getMethods()) {
            if (isGetter(getter)) {
                try {
                    Object value = getter.invoke(obj);
                    Class<?> returnType = getter.getReturnType();
                    if (MAPPED_TYPES.containsKey(returnType)) {
                        collectMapped(collector, returnType, value);
                    } else if (value instanceof Number) {
                        collector.collect(name(getter), (Number) value);
                    }
                } catch (Exception ex) {
                    // ignore this getter
                }
            }
        }
    }

    private static boolean isGetter(Method method) {
        return method.getParameterCount() == 0 
                && (method.getName().startsWith("get") || method.getName().startsWith("is"));
    }

    private static String name(Method method) {
        String name = method.getName();
        return name.startsWith("get") ? name.substring(3) : name.substring(2);
    }

    @SuppressWarnings("unchecked")
    private static <T> void collectMapped(MonitoringDataCollector collector, Class<T> type, Object value) {
        collector.collectObject((T) value, (BiConsumer<MonitoringDataCollector, T>) MAPPED_TYPES.get(type));
    }
}
