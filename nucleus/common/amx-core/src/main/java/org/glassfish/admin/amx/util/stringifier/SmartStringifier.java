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
package org.glassfish.admin.amx.util.stringifier;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import org.glassfish.admin.amx.util.ArrayConversion;
import org.glassfish.admin.amx.util.ClassUtil;

/**
 * Stringifies an Object in the "best" possible way, using the
 * StringifierRegistry.DEFAULT registry and/or internal logic.
 */
public final class SmartStringifier implements Stringifier {

    public static final SmartStringifier DEFAULT = new SmartStringifier(",");
    private final String mMultiDelim;
    private final boolean mEncloseArrays;
    private StringifierRegistry mRegistry;

    public SmartStringifier() {
        this(",");
    }

    public SmartStringifier(String multiDelim) {
        this(multiDelim, true);
    }

    public SmartStringifier(String multiDelim, boolean encloseArrays) {
        this(StringifierRegistryImpl.DEFAULT, multiDelim, encloseArrays);
    }

    public SmartStringifier(StringifierRegistry registry, String multiDelim, boolean encloseArrays) {
        mMultiDelim = multiDelim;
        mEncloseArrays = encloseArrays;
        mRegistry = registry;
    }

    public void setRegistry(StringifierRegistry registry) {
        mRegistry = registry;
    }
    private final static Class[] STRINGIFIER_REGISTRY_LOOKUPS = {
        Iterator.class,
        Collection.class,
        HashMap.class
    };

    private Stringifier getStringifier(final Object target) {
        if (target == null) {
            return (null);
        }

        final Class<?> targetClass = target.getClass();

        Stringifier stringifier = mRegistry.lookup(targetClass);

        if (target instanceof javax.management.ObjectName) {
            assert (stringifier != null);
        }

        if (stringifier == null) {
            // exact match failed...look for match in defined order
            final int numLookups = STRINGIFIER_REGISTRY_LOOKUPS.length;
            for (int i = 0; i < numLookups; ++i) {
                final Class<?> theClass = STRINGIFIER_REGISTRY_LOOKUPS[ i];

                stringifier = mRegistry.lookup(theClass);
                if (stringifier != null && theClass.isAssignableFrom(target.getClass())) {
                    break;
                }
            }
        }

        if (stringifier == null) {
            // see if there is a Stringifier for any superclass;
            Class tempClass = targetClass;
            while (tempClass != Object.class) {
                stringifier = mRegistry.lookup(tempClass);
                if (stringifier != null) {
                    break;
                }

                tempClass = tempClass.getSuperclass();
            }

        }

        if (stringifier == null) {
            final Class[] interfaces = targetClass.getInterfaces();
            if (interfaces.length != 0) {
                stringifier = new InterfacesStringifier(interfaces);
            }
        }

        return (stringifier);
    }

    private String smartStringify(Object target) {
        String result = null;

        if (ClassUtil.objectIsArray(target)) {
            

            final Class elementClass =
                    ClassUtil.getArrayElementClass(target.getClass());

            Object[] theArray = ClassUtil.isPrimitiveClass(elementClass) ?
                ArrayConversion.toAppropriateType(target) :
                (Object[]) target;

            result = ArrayStringifier.stringify(theArray, mMultiDelim, this);
            if (mEncloseArrays) {
                result = "{" + result + "}";
            }
        } else {
            Stringifier stringifier = getStringifier(target);

            if (stringifier != null && stringifier.getClass() == this.getClass()) {
                // avoid recursive call to self
                stringifier = null;
            }

            if (stringifier != null) {
                result = stringifier.stringify(target);
            }
        }

        if (result == null) {
            result = target.toString();
        }

        return (result);
    }

    public static String toString(Object target) {
        return (DEFAULT.stringify(target));
    }

    @Override
    public String stringify(Object target) {
        if (target == null) {
            return ("<null>");
        }

        return (smartStringify(target));
    }
}
