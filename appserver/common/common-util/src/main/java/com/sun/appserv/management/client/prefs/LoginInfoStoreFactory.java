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

package com.sun.appserv.management.client.prefs;

import java.util.Arrays;
import com.sun.appserv.management.client.prefs.LoginInfo;
import com.sun.appserv.management.client.prefs.LoginInfoStoreFactory;

/** A factory class to create instances of LoginInfoStore.
 * @since Appserver 9.0
 */
public class LoginInfoStoreFactory {
    
    /** Private constructor.
     */
    private LoginInfoStoreFactory() {
    }
    
    /** Returns the store that is represented by given class name. The parameter must
     * implement the {@link LoginInfoStore} interface. If a null is passed, an instance of the default
     * store {@link MemoryHashLoginInfoStore} is returned.
     * @param storeImplClassName fully qualified name of the class implementing LoginInfoStore. May be null.
     * @return the instance of LoginInfoStore of your choice
     * @throws IllegalArgumentException if the parameter does not implement LoginInfoStore
     * @throws StoreException if the construction of default store results in problems
     * @throws ClassNotFoundException if the given class could not be loaded
     */
    public static LoginInfoStore getStore(final String storeImplClassName) 
        throws StoreException, ClassNotFoundException, IllegalAccessException, InstantiationException {
        LoginInfoStore store = null;
        if (storeImplClassName == null)
            store = getDefaultStore();
        else 
            store = getCustomStore(storeImplClassName);
        return ( store );
    }
    
    public static LoginInfoStore getDefaultStore() throws StoreException {
        return ( new MemoryHashLoginInfoStore() );
    }
    
    private static LoginInfoStore getCustomStore(final String icn) 
        throws ClassNotFoundException, IllegalAccessException, InstantiationException{
        final Class ic  = Class.forName(icn);
        final String in = LoginInfoStore.class.getName();
        if (ic == null || !isStore(ic))
            throw new IllegalArgumentException("Class: " + ic.getName() + " does not implement: " + in);
        final LoginInfoStore store = (LoginInfoStore) ic.newInstance();
        return ( store );
    }
    
    private static boolean isStore(final Class c) {
        final Class[] ifs = c.getInterfaces();
        final Class sc    = LoginInfoStore.class;
        return ( Arrays.asList(ifs).contains(sc) );
    }
}
