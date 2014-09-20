/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008-2012 Oracle and/or its affiliates. All rights reserved.
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

import org.jvnet.hk2.annotations.Service;
import javax.inject.Inject;
import javax.inject.Singleton;

import javax.xml.stream.XMLInputFactory;

import org.glassfish.hk2.api.Factory;
import org.glassfish.hk2.api.PerLookup;

/**
 * Allow people to inject {@link XMLInputFactory} via {@link Inject}.
 *
 * <p>
 * Component instantiation happens only when someone requests {@link XMLInputFactory},
 * so this is as lazy as it gets.
 *
 * <p>
 * TODO: if we need to let people choose StAX implementation, this is the place to do it. 
 *
 * @author Kohsuke Kawaguchi
 */
@Service
@Singleton
public class StAXParserFactory implements Factory<XMLInputFactory> {

    /* (non-Javadoc)
     * @see org.glassfish.hk2.api.Factory#provide()
     */
    @Override @PerLookup
    public XMLInputFactory provide() {
        // In JDK 1.6, StAX is part of JRE, so we use no argument variant of
        // newInstance(), where as on JDK 1.5, we use two argument version of
        // newInstance() so that we can pass the classloader that loads
        // XMLInputFactory to load the factory, otherwise by default StAX uses
        // Thread's context class loader to locate the factory. See:
        // https://glassfish.dev.java.net/issues/show_bug.cgi?id=6428
        return XMLInputFactory.class.getClassLoader() == null ?
                        XMLInputFactory.newInstance() :
                        XMLInputFactory.newInstance(XMLInputFactory.class.getName(),
                                XMLInputFactory.class.getClassLoader());
    }

    /* (non-Javadoc)
     * @see org.glassfish.hk2.api.Factory#dispose(java.lang.Object)
     */
    @Override
    public void dispose(XMLInputFactory instance) {
        
    }
}
