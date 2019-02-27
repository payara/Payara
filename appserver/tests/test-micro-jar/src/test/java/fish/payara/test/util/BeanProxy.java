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
package fish.payara.test.util;

import static org.junit.Assert.fail;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Base class for test wrappers on beans of classes only available at runtime.
 */
public class BeanProxy {

    private final Object bean;

    public BeanProxy(Object bean) {
        this.bean = bean;
    }

    @Override
    public final boolean equals(Object obj) {
        return obj != null && obj.getClass() == getClass() && bean.equals(((BeanProxy)obj).bean);
    }

    @Override
    public final int hashCode() {
        return bean.hashCode();
    }

    @Override
    public String toString() {
        return bean.getClass().getSimpleName();
    }

    public final boolean booleanValue(String name) {
        Object res = propertyValue(name, Boolean.class);
        return res instanceof Boolean ? ((Boolean)res).booleanValue() : Boolean.parseBoolean((String)res);
    }

    public final int intValue(String name) {
        Integer res = integerValue(name);
        return res == null ? 0 : res.intValue();
    }

    public final Integer integerValue(String name) {
        Object res = propertyValue(name, Integer.class);
        return res instanceof Integer || res == null ? ((Integer)res) : Integer.valueOf((String)res);
    }

    public final Long longValue(String name) {
        Object res = propertyValue(name, Long.class);
        return res instanceof Long || res == null ? ((Long)res) : Long.valueOf((String)res);
    }

    public final String stringValue(String name) {
        Object res = propertyValue(name, String.class);
        return res instanceof String || res == null ? (String) res : String.valueOf(res);
    }

    public final Enum<?> enumValue(String name) {
        return (Enum<?>) propertyValue(name, Enum.class);
    }
    @SuppressWarnings("unchecked")
    public final <E extends Enum<E>> E enumValue(String name, Class<E> type) {
        Object val = propertyValue(name, type);
        if (val == null || type.isAssignableFrom(val.getClass())) {
            return (E) val;
        }
        if (val instanceof String) {
            return Enum.valueOf(type, (String)val);
        }
        fail("Unexpected value type for enum: " + name);
        return null;
    }

    public final Object propertyValue(String name) {
        return propertyValue(name, Object.class);
    }

    public final Object propertyValue(String name, Class<?> type) {
        String errorText = "Failed to get " + type.getSimpleName() + " value for property " + name;
        return name.startsWith("get") || name.startsWith("is") || bean instanceof Annotation
                ? callMethod(name, errorText)
                : accessField(name, errorText);
    }

    public final Object callMethod(String name, String errorText) {
        try {
            Method getter = bean.getClass().getMethod(name, new Class[0]);
            return getter.invoke(bean);
        } catch (Throwable e) {
            throw asAssertionError(errorText + " for method " + name, e);
        }
    }

    public final <P> Object callMethod(String name, String errorText, Class<P> parameter, P argument) {
        try {
            Method getter = bean.getClass().getMethod(name, new Class[] { parameter });
            return getter.invoke(bean, argument);
        } catch (Throwable e) {
            throw asAssertionError(errorText + " for method " + name, e);
        }
    }

    public final Object accessField(String name, String errorText) {
        try {
            Field field = bean.getClass().getDeclaredField(name);
            field.setAccessible(true);
            return field.get(bean);
        } catch (Throwable e) {
            throw asAssertionError(errorText + " for field " + name, e);
        }
    }

    public static AssertionError asAssertionError(String msg, Throwable e) {
        return e.getClass() == AssertionError.class ? (AssertionError)e : new AssertionError(msg, e);
    }
}
