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

package com.sun.enterprise.universal.glassfish;

import com.sun.enterprise.universal.collections.CollectionUtils;
import java.util.*;

/**
 * A replacement for System Properties
 * An InheritableThreadLocal is used to store the "impl".  This means that the
 * initial thread that uses this class -- and all its sub-threads will get the
 * same System Properties.
 * Make sure that you don't create it from the main Thread -- otherwise all instances
 * will get the same props.
 * E.g.
 * main thread creates instance1-thread and instance2-thread
 * The 2 created threads should each call init() -- but the main thread should not.
 * In the usual case where there is just one instance in the JVM -- this class is also
 * perfectly usable.  Just call any method when you need something.
 * 
 * @author bnevins
 */
public final class GFSystem {
    public final static void init() {
        // forces creation
        getProperty("java.lang.separator");
    }
    
    /**
     * Get the GFSystem Properties
     * @return a snapshot copy of the dcurrent Properties
     */
    public final static Map<String,String> getProperties()
    {
        return gfsi.get().getProperties();
    }
    
    /**
     * Get a GF System Property
     * @param key the name of the property
     * @return the value of the property
     */
    public final static String getProperty(String key)
    {
        return gfsi.get().getProperty(key);
    }

    /**
     * Set a GF System Property, null is acceptable for the name and/or value.
     * @param key the name of the property
     * @param value the value of the property
     */
    public final static void setProperty(String key, String value)
    {
        gfsi.get().setProperty(key, value);
    }
    
    private static final InheritableThreadLocal<GFSystemImpl> gfsi = 
         new InheritableThreadLocal<GFSystemImpl>() {
             @Override 
             protected GFSystemImpl initialValue() {
                 return new GFSystemImpl();
         }
     };
}
