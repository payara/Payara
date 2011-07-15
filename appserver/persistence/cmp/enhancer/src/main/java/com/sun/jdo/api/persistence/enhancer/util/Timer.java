/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.jdo.api.persistence.enhancer.util;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

import java.text.DecimalFormat;

import java.io.PrintWriter;


/**
 * Utility class for simple performance analysis.
 */
//@olsen: added class
public final class Timer {
    // a method's timing descriptor
    static private class MethodDescriptor {
        final String name;
        int instantiations;
        int calls;
        long self;
        long total;

        MethodDescriptor(String name) {
            this.name = name;
        }
    }

    // a method call's timing descriptor
    static private class MethodCall {
        final MethodDescriptor method;
        final String message;
        long self;
        long total;

        MethodCall(MethodDescriptor method,
                   String message,
                   long self,
                   long total) {
            this.method = method;
            this.message = message;
            this.self = self;
            this.total = total;
        }
    }

    // output device
    PrintWriter out = new PrintWriter(System.out, true);

    // methods
    HashMap methods = new HashMap();

    // method call stack
    private final ArrayList calls = new ArrayList(16);
    
    public Timer()
    {
        
    }

    public Timer(PrintWriter out)
    {
        this.out = out;
    }

    public final synchronized void push(String name) {
        push(name, name);
    }
    
    public final synchronized void push(String name, String message) {
        // get time
        final long now = System.currentTimeMillis();

        // get a method descriptor
        MethodDescriptor current = (MethodDescriptor)methods.get(name);
        if (current == null) {
            current = new MethodDescriptor(name);
            methods.put(name, current);
        }

        // update method descriptor
        current.calls++;
        current.instantiations++;

        // update method call stack
        calls.add(new MethodCall(current, message, now, now));
    }

    public final synchronized void pop()
    {
        // get time
        final long now = System.currentTimeMillis();

        // update method call stack
        final MethodCall call = (MethodCall)calls.remove(calls.size()-1);

        // get current call's time
        final long currentSelf = now - call.self;
        final long currentTotal = now - call.total;

        // update previous call's self time
        if (calls.size() > 0) {
            final MethodCall previous = (MethodCall)calls.get(calls.size()-1);
            previous.self += currentTotal;
        }

        // update method descriptor
        final MethodDescriptor current = call.method;
        current.self += currentSelf;
        if (--current.instantiations == 0) {
            current.total += currentTotal;
        }

        if (false) {
            out.println("Timer (n,g): " + call.message + " : ("
                        + currentSelf + ", " + currentTotal + ")");
        }
    }

    static private final String pad(String s, int i)
    {
        StringBuffer b = new StringBuffer();
        for (i -= s.length(); i > 0; i--)
            b.append((char)' ');
        b.append(s);
        return b.toString();
    }
    
    public final synchronized void print()
    {
        out.println("Timer : printing accumulated times ...");
        final Object[] calls = methods.values().toArray();

        Arrays.sort(calls,
                    new Comparator() {
                            public int compare(Object o1,
                                               Object o2) {
                                return (int)(((MethodDescriptor)o2).total
                                             - ((MethodDescriptor)o1).total);
                            }
                            public boolean equals(Object obj) {
                                return (obj != null && compare(this, obj) == 0);
                            }
                        });
        
        out.println("Timer :  total s    self s  #calls  name");
        DecimalFormat nf = new DecimalFormat();
        nf.setMaximumFractionDigits(2);
        nf.setMinimumFractionDigits(2);
        //nf.applyPattern("#,##0.00");
        //out.println("Timer : pattern = " + nf.toPattern());
        for (int i = 0; i < calls.length; i++) {
            final MethodDescriptor current = (MethodDescriptor)calls[i];

            out.println("Timer : "
                        + pad(nf.format(current.total / 1000.0), 8) + "  "
                        + pad(nf.format(current.self / 1000.0), 8) + "  "
                        + pad(String.valueOf(current.calls), 6) + "  "
                        + current.name);
        }
    }
}
