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

package com.sun.enterprise.admin.cli.remote;

import java.util.ArrayList;
import java.util.List;

/** An immutable class to analyze the exception stack trace of a given
 * instance of {@link java.lang.Exception}. Can be extended to handle
 * throwables, but it is not done in this version on purpose. Takes the
 * snapshot of given exception at the time of instantiation.
 * @author &#2325;&#2375;&#2342;&#2366;&#2352 (km@dev.java.net)
 * @since GlassFish v3 Prelude
 */

final class ExceptionAnalyzer {
    
    private final Exception exc;
    private final List<Throwable> chain;
    ExceptionAnalyzer(Exception e) {
        if (e == null)
            throw new IllegalArgumentException("null arg");
        this.exc   = e;
        this.chain = new ArrayList<Throwable>();
        chain.add(exc);
        build();
    }
    
    private void build() {
        Throwable t = exc.getCause();
        while (t != null) {
            chain.add(t);
            t = t.getCause();
        }
    }
    
    /** Returns the first instance of the given Exception class in the chain
     *  of causes. The counting starts from the instance of the Exception that
     *  created the ExceptionAnalyzer class itself.
     * @param ac the unknown subclass of Exception that needs the chain to be examined for
     * @return first instance of given Throwable (returned object will be an
     * instance of the given class) or null if there is no such instance
     */ 
    Throwable getFirstInstanceOf(Class<? extends Exception> ac) {
        for (Throwable t : chain) {
            try {
                ac.cast(t);
                return t;
            } catch(ClassCastException cce) {
                //ignore and continue
            }
        }
        return null;
    }
}

