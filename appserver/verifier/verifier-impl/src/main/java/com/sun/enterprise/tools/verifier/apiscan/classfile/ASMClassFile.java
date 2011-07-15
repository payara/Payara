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

package com.sun.enterprise.tools.verifier.apiscan.classfile;

import static org.objectweb.asm.Opcodes.*;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.commons.EmptyVisitor;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

/**
 * @author Sanjeeb.Sahoo@Sun.COM
 */
class ASMClassFile implements ClassFile {

    private static Logger logger = Logger.getLogger("apiscan.classfile"); // NOI18N

    private String externalName;

    private String internalName;

    private String packageName;

    private ClassReader cr;

    private Set<Method> methods = new HashSet<Method>();

    private String internalNameOfSuperClass;

    private String[] internalNamesOfInterfaces;

    private int access;

    public boolean isInterface() {
        return (access & ACC_INTERFACE) == ACC_INTERFACE;
    }

    public Method getMethod(MethodRef methodRef) {
        for (Method m : methods) {
            if (m.getName().equals(methodRef.getName()) &&
                    m.getDescriptor().equals(methodRef.getDescriptor())) {
                return m;
            }
        }
        return null;
    }

    public Collection<Method> getMethods() {
        return methods;
    }

    public ASMClassFile(InputStream is)
            throws IOException {
        cr = new ClassReader(is);
        cr.accept(new MyVisitor(this), ClassReader.SKIP_CODE);
        is.close();
    }

    public Collection<String> getAllReferencedClassNames() {
        throw new UnsupportedOperationException();
    }

    public Collection getAllReferencedClassNamesInInternalForm() {
        throw new UnsupportedOperationException();
    }

   public String getName() {
        return externalName;
    }

    public String getInternalName() {
        return internalName;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getNameOfSuperClass() {
        return Util.convertToExternalClassName(internalNameOfSuperClass);
    }

    public String getInternalNameOfSuperClass() {
        return internalNameOfSuperClass;
    }

    public String[] getNamesOfInterfaces() {
        String[] result = new String[internalNamesOfInterfaces.length];
        for(int i = 0; i< internalNamesOfInterfaces.length; ++i){
            result[i] = Util.convertToExternalClassName(internalNamesOfInterfaces[i]);
        }
        return result;
    }

    public String[] getInternalNamesOfInterfaces() {
        return internalNamesOfInterfaces;
    }

    @Override public String toString() {
        StringBuilder sb = new StringBuilder(decodeAccessFlag(access)+ externalName);
        if(internalNameOfSuperClass!=null) sb.append(" extends "+getNameOfSuperClass()); // NOI18N
        if(internalNamesOfInterfaces.length>0){
            sb.append(" implements "); // NOI18N
            for(String s : getNamesOfInterfaces()) sb.append(s);
        }
        sb.append("{\n"); // NOI18N
        for(Method m : methods){
            sb.append(m).append("\n"); // NOI18N
        }
        sb.append("}"); // NOI18N
        return sb.toString();
    }

    private static class MyVisitor extends EmptyVisitor {
        ASMClassFile cf;

        public MyVisitor(ASMClassFile cf) {
            this.cf = cf;
        }

        @Override public void visit(
                int version, int access, String name, String signature,
                String superName, String[] interfaces) {
            logger.entering(
                    "com.sun.enterprise.tools.verifier.apiscan.classfile.ASMClassFile$MyVisitor", "visit", // NOI18N
                    new Object[]{version, access, name, signature});
            cf.internalName = name;
            cf.externalName = Util.convertToExternalClassName(name);
            cf.internalNameOfSuperClass = superName;
            cf.internalNamesOfInterfaces = interfaces;
            int index = name.lastIndexOf('/');
            if (index < 0)
                cf.packageName = "";
            else
                cf.packageName = name.substring(0, index);
            cf.access = access;
        }

        @Override public MethodVisitor visitMethod(
                int access, String name, String desc, String signature,
                String[] exceptions) {
            logger.entering(
                    "com.sun.enterprise.tools.verifier.apiscan.classfile.ASMClassFile$MyVisitor", "visitMethod", // NOI18N
                    new Object[]{access, name, signature, desc});
            ASMMethod method = new ASMMethod(cf, name, desc, access,
                    signature, exceptions);
            cf.methods.add(method);
            return method;
        }

        @Override public void visitEnd() {
            logger.entering("ASMClassFile$MyVisitor", "visitEnd", // NOI18N
                    new Object[]{cf.getName()});
        }
    }

    public static String decodeAccessFlag(int access) {
        StringBuilder result = new StringBuilder("");
        if ((access & ACC_PRIVATE) == ACC_PRIVATE) {
            result.append("private "); // NOI18N
        } else if ((access & ACC_PROTECTED) == ACC_PROTECTED) {
            result.append("protected "); // NOI18N
        } else if ((access & ACC_PUBLIC) == ACC_PUBLIC) {
            result.append("public "); // NOI18N
        }
        if ((access & ACC_ABSTRACT) == ACC_ABSTRACT) {
            result.append("abstract "); // NOI18N
        } else if ((access & ACC_FINAL) == ACC_FINAL) {
            result.append("final "); // NOI18N
        }
        if ((access & ACC_INTERFACE) == ACC_INTERFACE) {
            result.append("interface "); // NOI18N
        } else {
            result.append("class "); // NOI18N
        }
        return result.toString();
    }

}
