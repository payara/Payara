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
 *
 *
 * This file incorporates work covered by the following copyright and
 * permission notice:
 *
 * Copyright 2004 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.catalina.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;

/**
 * Custom subclass of <code>ObjectInputStream</code> that loads from the
 * class loader for this web application.  This allows classes defined only
 * with the web application to be found correctly.
 *
 * @author Craig R. McClanahan
 * @author Bip Thelin
 * @version $Revision: 1.2 $, $Date: 2005/12/08 01:28:15 $
 */

public final class CustomObjectInputStream
    extends ObjectInputStream {


    /**
     * The class loader we will use to resolve classes.
     */
    private ClassLoader classLoader = null;


    /**
     * Construct a new instance of CustomObjectInputStream
     *
     * @param stream The input stream we will read from
     * @param classLoader The class loader used to instantiate objects
     *
     * @exception IOException if an input/output error occurs
     */
    public CustomObjectInputStream(InputStream stream,
                                   ClassLoader classLoader)
        throws IOException {

        super(stream);
        this.classLoader = classLoader;
    }


    /**
     * Load the local class equivalent of the specified stream class
     * description, by using the class loader assigned to this Context.
     *
     * @param classDesc Class description from the input stream
     *
     * @exception ClassNotFoundException if this class cannot be found
     * @exception IOException if an input/output error occurs
     */
    public Class resolveClass(ObjectStreamClass classDesc)
             throws ClassNotFoundException, IOException {
        try {
            return Class.forName(classDesc.getName(), false, classLoader);
        } catch (ClassNotFoundException e) {
            try {
                // Try also the superclass because of primitive types
                return super.resolveClass(classDesc);
            }  catch (ClassNotFoundException e2) {
                // Rethrow original exception, as it can have more information
                // about why the class was not found. BZ 48007
                throw e;
            }
        }
    }


}
