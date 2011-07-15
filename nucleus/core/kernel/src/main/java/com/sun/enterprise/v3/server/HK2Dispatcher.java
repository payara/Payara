/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006-2011 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.v3.server;


import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;


public class HK2Dispatcher {

/**    Field threadLocalsField = null;
    Field tableField = null;
    Field hashCode = null;
    Field value;

    private void init() {
        try {
            threadLocalsField = Thread.class.getDeclaredField("threadLocals");
            threadLocalsField.setAccessible(true);
            Class c = Class.forName("java.lang.ThreadLocal$ThreadLocalMap");
            tableField = c.getDeclaredField("table");
            tableField.setAccessible(true);
            hashCode = ThreadLocal.class.getDeclaredField("threadLocalHashCode");
            hashCode.setAccessible(true);
            c = Class.forName("java.lang.ThreadLocal$ThreadLocalMap$Entry");
            value = c.getDeclaredField("value");
            value.setAccessible(true);

        } catch(NoSuchFieldException e) {
            e.printStackTrace();
            
        } catch(ClassNotFoundException e) {
            e.printStackTrace();
            
        }
    }

*/
    public void dispatch(HttpHandler adapter, ClassLoader cl, Request req, Response res) {

        // save the thread local entries.
        Thread thread = Thread.currentThread();
/*        if (threadLocalsField==null) {
            init();
        }
        Set<Integer> entries = new HashSet();
        if (threadLocalsField!=null) {

            try {

                Object threadLocals = threadLocalsField.get(thread);
                WeakReference<ThreadLocal>[] table = (WeakReference<ThreadLocal>[]) tableField.get(threadLocals);
                int len = table.length;
                for (int j = 0; j < len; j++) {
                    WeakReference<ThreadLocal> e = table[j];
                    if (e != null) {
                        entries.add(hashCode.getInt(e.get()));
                        //System.out.println("Hashcode = " + hashCode.get(e.get()));
                        //System.out.println("Value = " + value.get(e));
                    }
                }
            } catch(IllegalAccessException e) {

            }
*/
            ClassLoader currentCL = thread.getContextClassLoader();
            try {
                if (cl==null) {
                    cl = adapter.getClass().getClassLoader();
                }
                Thread.currentThread().setContextClassLoader(cl);
                // wrap Request to intercept set/getNote
                adapter.service(req, res);
            } catch(Exception e) {
                // log.
                // swallows...

            } finally {
                thread.setContextClassLoader(currentCL);
            }
/*
            // same thing again...
            try {

                Object threadLocals = threadLocalsField.get(thread);
                WeakReference<ThreadLocal>[] table = (WeakReference<ThreadLocal>[]) tableField.get(threadLocals);
                int len = table.length;
                for (int j = 0; j < len; j++) {
                    WeakReference<ThreadLocal> e = table[j];
                    if (e != null) {
                        if (!entries.contains(hashCode.getInt(e.get()))) {
                            //System.out.println("Added Thread local Hashcode = " + hashCode.get(e.get()));
                            //System.out.println("Value = " + value.get(e));
                        }
                    }
                }
            } catch(IllegalAccessException e) {

            }            


        } else {
            // no thread local protection available

        }
*/
    }
}
