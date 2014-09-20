/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright (c) 2011 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.appclient.client;

import java.util.Collection;
import java.util.Collections;

/**
 * Masks classes or resources that are listed in the "endorsed" packages that
 * GlassFish provides.
 * <p>
 * During Java Web Start launches we cannot set java.endorsed.dirs to have
 * the bootstrap class loader look in the GlassFish-provided JARs for classes
 * and resources that would otherwise be found in the system JARs (rt.jar for 
 * example). We need some other way of making sure the GlassFish-provided
 * JARs are used. Those JARs are listed in the JNLP documents for the app,
 * so the JNLPClassLoader which Java Web Start provides will find them if we
 * give it a chance.
 * <p>
 * This loader knows what packages are in the GlassFish-provided endorsed JARs
 * and will report any matching class or resource as not found. When an 
 * instance of this loader is inserted as the parent of the JNLPClassLoader then
 * the JNLPClassLoader will delegate to it first.  The masking loader will
 * report GlassFish-provided content as not found, so the JNLPClassLoader will
 * try to resolve them itself.  That resolution will use the downloaded GlassFish
 * JARs, thereby making sure the Java Web Start launch uses the GlassFish-provided
 * endorsed JARs instead of whatever happens to be in the Java system JARs.
 * 
 * @author Tim Quinn
 */
class JWSACCMaskingClassLoader extends MaskingClassLoader {

    private final Collection<String> endorsedPackagesToMask;

    JWSACCMaskingClassLoader(ClassLoader parent, Collection<String> endorsedPackagesToMask) {
        super(
            parent, 
            Collections.EMPTY_SET /* punchins */, 
            Collections.EMPTY_SET /* multiples */, 
            false /* useExplicitCallsToFindSystemClass */);
        this.endorsedPackagesToMask = endorsedPackagesToMask;
    }

    @Override
    protected boolean isDottedNameLoadableByParent(final String name) {
        /*
         * Currently the only masked packages start with javax. or org.
         * Check the prefix as an optimization to avoid searching the collection.
         */
        if (!(name.startsWith("javax.") || name.startsWith("org."))) {
            return true;
        }
        
        final String packageName = name.substring(0, name.lastIndexOf("."));
        if (endorsedPackagesToMask.contains(packageName)) {
            /*
             * The requested name is one to be masked, so do not let the caller
             * delegate to its parent.
             */
            return false;
        }
        /*
         * The requested name should not be masked.  Allow the caller to delegate
         * to its parent first.
         */
        return true;
    }
}
