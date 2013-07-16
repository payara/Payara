/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006-2013 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.common.util;

import org.jvnet.hk2.annotations.Service;

import java.io.*;
import java.lang.reflect.Array;

/**
 * This subclass of ObjectInputStream delegates loading of classes to
 * an existing ClassLoader.
 */

@Service
public class ObjectInputStreamWithLoader extends ObjectInputStream {
    protected ClassLoader loader;

    /**
     * Loader must be non-null;
     *
     * @throws IOException              on io error
     * @throws StreamCorruptedException on a corrupted stream
     */

    public ObjectInputStreamWithLoader(InputStream in, ClassLoader loader)
            throws IOException, StreamCorruptedException {

        super(in);
        if (loader == null) {
            throw new IllegalArgumentException("Illegal null argument to ObjectInputStreamWithLoader");
        }
        this.loader = loader;
    }

    /**
     * Make a primitive array class
     */

    private Class primitiveType(char type) {
        switch (type) {
            case 'B':
                return byte.class;
            case 'C':
                return char.class;
            case 'D':
                return double.class;
            case 'F':
                return float.class;
            case 'I':
                return int.class;
            case 'J':
                return long.class;
            case 'S':
                return short.class;
            case 'Z':
                return boolean.class;
            default:
                return null;
        }
    }

    /**
     * Use the given ClassLoader rather than using the system class
     *
     * @throws ClassNotFoundException if class can not be loaded
     */
    protected Class resolveClass(ObjectStreamClass classDesc)
            throws IOException, ClassNotFoundException {

        try {
            String cname = classDesc.getName();
            if (cname.startsWith("[")) {
                // An array
                Class component;        // component class
                int dcount;            // dimension
                for (dcount = 1; cname.charAt(dcount) == '['; dcount++) ;
                if (cname.charAt(dcount) == 'L') {
                    component = loader.loadClass(cname.substring(dcount + 1,
                            cname.length() - 1));
                } else {
                    if (cname.length() != dcount + 1) {
                        throw new ClassNotFoundException(cname);// malformed
                    }
                    component = primitiveType(cname.charAt(dcount));
                }
                int dim[] = new int[dcount];
                for (int i = 0; i < dcount; i++) {
                    dim[i] = 0;
                }
                return Array.newInstance(component, dim).getClass();
            } else {
                return loader.loadClass(cname);
            }
        } catch (ClassNotFoundException e) {
            // Try also the superclass because of primitive types
            return super.resolveClass(classDesc);
        }
    }
}
