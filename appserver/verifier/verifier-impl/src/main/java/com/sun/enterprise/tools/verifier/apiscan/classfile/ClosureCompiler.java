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

/*
 * ClosureCompiler.java
 *
 * Created on September 7, 2004, 5:02 PM
 */

package com.sun.enterprise.tools.verifier.apiscan.classfile;

import java.util.Collection;
import java.util.Map;

/**
 * This is single most important interface of the apiscan package. This class is
 * used to compute the complete closure of a set of classes.
 *
 * @author Sanjeeb.Sahoo@Sun.COM
 * @see ClosureCompilerImpl
 */
public interface ClosureCompiler {
    /**
     * @param externalClsName class name (in external format) whose closure will
     *                        be computed.
     * @return true if it can compute the closure for all the classes that are
     *         new to it, else false. In other words, this operation is not
     *         idempotent. e.g. ClosureCompiler cc; cc.reset(); boolean
     *         first=cc.buildClosure("a.B"); boolean second=cc.buildClosure("a.B");
     *         second will always be true irrespective of value of first. This
     *         is because it uses list of classes that it has already visited.
     *         That list gets cleared when {@link #reset()} is called).
     */
    boolean buildClosure(String externalClsName);

    /**
     * @return unmodifiable collection of class names which it visited during
     *         closure computation. e.g. Let's say a.class references b1.class
     *         and b2.class. b1.class references c1.class, c2.class and c3.class
     *         b2.class references d1.class, d2.class and d3.class.
     *         c1/c2/d1/d2.class are all not loadable where as c3 and d3.class
     *         are loadable. When we build the closure of a.class, closure will
     *         contain the following... {"a", "b1", "b2", "c3", "d3"}
     */
    Collection getClosure();

    /**
     * @return unmodifiable collection of class names whose closure could not be
     *         computed. The typical reason for not able to build closure for a
     *         class is that class not being found in loader's search path. See
     *         it returns a map which is keyed by the access path and the value
     *         is a list of class names which could not be loaded. e.g. Let's
     *         say a.class references b1.class and b2.class. b1.class references
     *         c1.class, c2.class and c3.class b2.class references d1.class,
     *         d2.class and d3.class. c1/c2/d1/d2.class are all not loadable
     *         where as c3 and d3.class are loadable. When we build the closure
     *         of a.class, failed map will contain the following... {("a:b1",
     *         {"c1","c2"}), ("a.b2", {"d1","d2"})}
     */
    Map getFailed();

    /**
     * Clear the internal cache. It includes the result it has collected since
     * last reset(). But it does not clear the excludedd list. If you want to
     * reset the excluded list, create a new ClosureCompiler.
     */
    void reset();

}
