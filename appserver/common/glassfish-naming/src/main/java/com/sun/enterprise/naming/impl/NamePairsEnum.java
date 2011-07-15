/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008-2010 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.naming.impl;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.NameClassPair;
import javax.naming.OperationNotSupportedException;
import java.util.Iterator;

/**
 * Created by IntelliJ IDEA.
 * User: maheshk
 * Date: Dec 7, 2007
 * Time: 1:15:30 AM
 * To change this template use File | Settings | File Templates.
 */ // Class for enumerating name/class pairs
class NamePairsEnum implements NamingEnumeration {
    GlassfishNamingManagerImpl nm;

    Iterator names;

    NamePairsEnum(GlassfishNamingManagerImpl nm, Iterator names) {
        this.nm = nm;
        this.names = names;
    }

    public boolean hasMoreElements() {
        return names.hasNext();
    }

    public boolean hasMore() throws NamingException {
        return hasMoreElements();
    }

    public Object nextElement() {
        if (names.hasNext()) {
            try {
                String name = (String) names.next();
                String className = nm.lookup(name).getClass().getName();
                return new NameClassPair(name, className);
            } catch (Exception ex) {
                throw new RuntimeException("Exception during lookup: " + ex);
            }
        } else
            return null;
    }

    public Object next() throws NamingException {
        return nextElement();
    }

    // New API for JNDI 1.2
    public void close() throws NamingException {
        throw new OperationNotSupportedException("close() not implemented");
    }
}
