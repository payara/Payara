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

package com.sun.enterprise.admin.cli.schemadoc;

import java.util.ArrayList;
import java.util.List;
import org.objectweb.asm.*;

public class DocClassVisitor implements ClassVisitor {
    private boolean hasConfiguredAnnotation = false;
    private String className;
    private List<String> interfaces;
    private ClassDef classDef;
    private boolean showDeprecated;

    public DocClassVisitor(final boolean showDep) {
        showDeprecated = showDep;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] intfs) {
        className = GenerateDomainSchema.toClassName(name);
        interfaces = new ArrayList<String>();
        for (String intf : intfs) {
            interfaces.add(GenerateDomainSchema.toClassName(intf));
        }
        classDef = new ClassDef(className, interfaces);
    }

    public List<String> getInterfaces() {
        return interfaces;
    }

    @Override
    public void visitSource(String source, String debug) {
    }

    @Override
    public void visitOuterClass(String owner, String name, String desc) {
    }

    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        hasConfiguredAnnotation |= "Lorg/jvnet/hk2/config/Configured;".equals(desc);
        if ("Ljava/lang/Deprecated;".equals(desc) && classDef != null) {
            classDef.setDeprecated(true);
        }
        return null;
    }

    @Override
    public void visitAttribute(Attribute attr) {
    }

    @Override
    public void visitInnerClass(String name, String outerName, String innerName, int access) {
    }

    @Override
    public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
        return null;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature,
        String[] exceptions) {
        String type = null;
        try {
            if (showDeprecated || ((access & Opcodes.ACC_DEPRECATED) != Opcodes.ACC_DEPRECATED)) {
                if (hasConfiguredAnnotation) {
                    if (signature != null) {
                        type = GenerateDomainSchema.toClassName(
                            signature.substring(signature.indexOf("<") + 1, signature.lastIndexOf(">") - 1));
                    } else {
                        type = GenerateDomainSchema.toClassName(desc);
                    }
                }
            }
        } catch (StringIndexOutOfBoundsException e) {
            throw new RuntimeException(e.getMessage());
        }
        return name.startsWith("get") && type != null ? new AttributeMethodVisitor(classDef, name, type)
            : null;
    }

    /**
     * Visits the end of the class. This method, which is the last one to be called, is used to inform the visitor that
     * all the fields and methods of the class have been visited.
     */
    @Override
    public void visitEnd() {
    }

    public boolean isConfigured() {
        return hasConfiguredAnnotation;
    }

    public ClassDef getClassDef() {
        return hasConfiguredAnnotation ? classDef : null;
    }

    @Override
    public String toString() {
        return "DocClassVisitor{" +
            "className='" + className + '\'' +
            ", hasConfiguredAnnotation=" + hasConfiguredAnnotation +
            '}';
    }
}
