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
package com.sun.enterprise.admin.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.PrivilegedActionException;
import java.util.LinkedList;
import javax.annotation.PostConstruct;

/**
 *
 * @author tjquinn
 */
public class PostConstructRunner {
    
    public static void runPostConstructs(final Object obj) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, PrivilegedActionException {
        /*
         * As we ascend the hierarchy, record the @PostConstruct methods we find
         * at each level at the beginning of the list.  After we have processed
         * the whole hierarchy, the highest-level @PostConstruct methods will be
         * first in the list. Processing them from first to last will execute them
         * from the top of the hierarchy down.
         */
        final LinkedList<Method> postConstructMethods = new LinkedList<Method>();
        for (ClassLineageIterator cIT = new ClassLineageIterator(obj.getClass()); cIT.hasNext(); ) {
            final Class<?> c = cIT.next();
            for (Method m : c.getDeclaredMethods()) {
                /*
                 * The injection manager will already have run a postConstruct
                 * method if the class implements the hk2 PostConstruct interface,
                 * so don't invoke it again if the developer also annotated it
                 * with @PostConstruct.  Ideally this will eventually migrate into
                 * the injection manager implementation.
                 */
                if (m.getAnnotation(PostConstruct.class) != null) {
                    if ( ( ! PostConstruct.class.isAssignableFrom(c)) || ! m.getName().equals("postConstruct")) {
                        postConstructMethods.addFirst(m);
                    }
                }
            }
        }
        for (final Method m : postConstructMethods) {
            java.security.AccessController.doPrivileged(
                new java.security.PrivilegedExceptionAction() {
                    public java.lang.Object run() throws Exception {
                        if( !m.isAccessible() ) {
                            m.setAccessible(true);
                        }
                        m.invoke(obj);
                        return null;
                    }
                });
        }
    }
    
}
