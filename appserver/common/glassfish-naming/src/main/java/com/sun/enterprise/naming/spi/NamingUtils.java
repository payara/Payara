/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008-2013 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
 * Portions Copyright [2017] Payara Foundation and/or affiliates
 */

package com.sun.enterprise.naming.spi;

import org.jvnet.hk2.annotations.Contract;

/**
 * Interface for methods to create instances of {@link NamingObjectFactory}
 */
@Contract
public interface NamingUtils {

    /**
     * Creates an instance of {@link com.sun.enterprise.naming.util.SimpleNamingObjectFactory} 
     * @param name the name of the object. This will be ignored
     * @param value the object that the create method of the factory will return
     * @return 
     */
    public NamingObjectFactory createSimpleNamingObjectFactory(String name, Object value);

    /**
     * 
     * @param name
     * @param jndiName
     * @param cacheResult
     * @return 
     */
    public NamingObjectFactory createLazyInitializationNamingObjectFactory(String name, String jndiName,
            boolean cacheResult);

    /**
     * Creates an instance of {@link com.sun.enterprise.naming.util.JndiNamingObjectFactory}
     * @param name the name of the object
     * @param jndiName the jndi name of the object to create/lookup
     * @param cacheResult whether the result may have been cached
     * @return 
     */
    public NamingObjectFactory createLazyNamingObjectFactory(String name, String jndiName,
        boolean cacheResult);

    /**
     * Creates an instance of {@link com.sun.enterprise.naming.util.CloningNamingObjectFactory}
     * that will create copies of the given object.
     * @param name the name of the object
     * @param value the object that will be copied when create is called
     * @return 
     */
    public NamingObjectFactory createCloningNamingObjectFactory(String name, Object value);

    /**
     * Creates an instance of {@link com.sun.enterprise.naming.util.CloningNamingObjectFactory}
     * that will create copies of the object created by the delegate.
     * @param name the name of the object
     * @param delegate the {@link NamingObjectFactory} that creates the object that will then be copied
     * @return 
     */
    public NamingObjectFactory createCloningNamingObjectFactory(String name,
        NamingObjectFactory delegate);

    /**
     * Creates an instance of {@link com.sun.enterprise.naming.util.DelegatingNamingObjectFactory}
     * that will use another {@link com.sun.enterprise.naming.spi.NamingObjectFactory} to do the work
     * @param name name of object
     * @param delegate {@link com.sun.enterprise.naming.spi.NamingObjectFactory} to do the work
     * @param cacheResult whether the result may have been cached
     * @return 
     */
    public NamingObjectFactory createDelegatingNamingObjectFactory(String name,
        NamingObjectFactory delegate, boolean cacheResult);

    /**
     * Makes a new instance of the specified object.
     * Object cannot implement {@link java.io.Serializable}
     * @throws RuntimeException
     * @param obj a non-serializable object
     * @return 
     */
    public Object makeCopyOfObject(Object obj);

}
