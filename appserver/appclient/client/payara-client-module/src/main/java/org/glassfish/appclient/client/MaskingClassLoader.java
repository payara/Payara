/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008-2011 Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.net.URL;
import java.util.*;

/**
 * {@link ClassLoader} that masks a specified set of classes
 * from its parent class loader.
 *
 * <p>
 * This code is used to create an isolated environment.
 *
 * @author Jerome Dochez
 */
public class MaskingClassLoader extends ClassLoader {

    private final Set<String> punchins = new HashSet<String>();
    private final String[] multiples;

    private final boolean useExplicitCallsToFindSystemClass;

    public MaskingClassLoader(ClassLoader parent, Collection<String> punchins, Collection<String> multiples) {
        this(parent, punchins, multiples, true /* use explicit calls to findSystemClass*/);
    }
    /**
     * Creates a new masking class loader letting a set of defined packages be loaded by the parent
     * classloader. Multiples packages can be specified so that only the parent package needs to
     * be provided.
     *
     * @param parent the parent classloader to delegate actual loading from when punchin is allowed
     * @param punchins list of packages allowed to be visible from the parent
     * @param multiples list of parent packages allowed to be visible from the parent class loader
     */
    public MaskingClassLoader(ClassLoader parent, Collection<String> punchins, Collection<String> multiples,
            boolean useExplicitCallsToFindSystemClass) {
        super(parent);
        this.punchins.addAll(punchins);
        this.multiples = multiples.toArray(new String[multiples.size()]);
        this.useExplicitCallsToFindSystemClass = useExplicitCallsToFindSystemClass;
    }

    @Override
    protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {

        // I do not mask java packages, and I only mask javax. stuff for now.
        try {
            if (useExplicitCallsToFindSystemClass) {
                return findSystemClass(name);
            }
        } catch(ClassNotFoundException e) {

        }
        if (isDottedNameLoadableByParent(name)) {
            return super.loadClass(name, resolve);
        }
        throw new ClassNotFoundException(name);
     }

    @Override
    public URL getResource(String name) {
        if (isDottedNameLoadableByParent(resourceToDotted(name))) {
            return super.getResource(name);
        } else {
            return null;
        }
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        if (isDottedNameLoadableByParent(resourceToDotted(name))) {
            return super.getResources(name);
        } else {
            return new Enumeration<URL>() {

                @Override
                public boolean hasMoreElements() {
                    return false;
                }

                @Override
                public URL nextElement() {
                    throw new NoSuchElementException();
                }
            };
        }
    }
    
    private String resourceToDotted(String name) {
        if (name.startsWith("/")) {
            name = name.substring(1);
        }
        return name.replace("/", ".");
    }
    
    protected boolean isDottedNameLoadableByParent(final String name) {
        if (!(name.startsWith("javax.") || name.startsWith("org."))) {
            return true;
        }
        final String packageName = name.substring(0, name.lastIndexOf("."));
        if (punchins.contains(packageName)) {
            return true;
        }
        for (String multiple : multiples) {
            if (name.startsWith(multiple)) {
                return true;
            }
        }
        return false;
    }
    
    
}
