/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2012 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.admin.rest.generator;

import org.glassfish.hk2.api.ServiceLocator;

/**
 * @author Ludovic Champenois
 */
public class ASMResourcesGenerator extends ResourcesGeneratorBase {

    protected final static String GENERATED_PATH = "org/glassfish/admin/rest/resources/generatedASM/";
    protected final static String GENERATED_PACKAGE = GENERATED_PATH.replace("/", ".");

    public ASMResourcesGenerator(ServiceLocator habitat) {
        super(habitat);
    }

    @Override
    public ClassWriter getClassWriter(String className, String baseClassName, String resourcePath) {
        try {
            Class.forName(GENERATED_PACKAGE + "." + className);
            return null;
        } catch (ClassNotFoundException ex) {
            ClassWriter writer = new ASMClassWriter(habitat, GENERATED_PATH, className, baseClassName, resourcePath);
            return writer;
        }
    }

    @Override
    public String endGeneration() {
        return "Code Generation done at  ";
    }
}
