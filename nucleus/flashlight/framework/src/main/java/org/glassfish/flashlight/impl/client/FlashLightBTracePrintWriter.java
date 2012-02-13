/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2012 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.flashlight.impl.client;

/**
 * @author Sreenivas Munnangi
 *         Date: 04aug2009
 */

import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FlashLightBTracePrintWriter extends PrintWriter {

    private final Logger l;
    private final Level logLevel = Level.WARNING ;
    private final static String BTRACE_PREFIX = "btrace:";

    FlashLightBTracePrintWriter(OutputStream out, Logger l) {
        super(out);
        this.l = l;
    }

    public void print(boolean b) {
        this.println(b);
    }

    public void print(char c) {
        this.println(c);
    }

    public void print(char[] cArr) {
        this.println(cArr);
    }

    public void print(double d) {
        this.println(d);
    }

    public void print(float f) {
        this.println(f);
    }

    public void print(int i) {
        this.println(i);
    }

    public void print(long lng) {
        this.println(lng);
    }

    public void print(Object o) {
        this.println(o);
    }

    public void print(String s) {
        this.println(s);
    }


    public void println() {
    }

    public void println(boolean b) {
        l.log(logLevel, BTRACE_PREFIX + String.valueOf(b));
    }

    public void println(char c) {
        l.log(logLevel, BTRACE_PREFIX + String.valueOf(c));
    }

    public void println(char[] cArr) {
        l.log(logLevel, BTRACE_PREFIX + String.valueOf(cArr));
    }

    public void println(double d) {
        l.log(logLevel, BTRACE_PREFIX + String.valueOf(d));
    }

    public void println(float f) {
        l.log(logLevel, BTRACE_PREFIX + String.valueOf(f));
    }

    public void println(int i) {
        l.log(logLevel, BTRACE_PREFIX + String.valueOf(i));
    }

    public void println(long lng) {
        l.log(logLevel, BTRACE_PREFIX + String.valueOf(lng));
    }

    public void println(Object o) {
        l.log(logLevel, BTRACE_PREFIX + o.toString());
    }

    public void println(String s) {
        l.log(logLevel, BTRACE_PREFIX + s);
    }


}
