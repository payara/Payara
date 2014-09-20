/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010 Oracle and/or its affiliates. All rights reserved.
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

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

public class EmptyVisitor implements ClassVisitor, FieldVisitor, MethodVisitor, AnnotationVisitor {
    @Override
    public void visit(int i, int i1, String s, String s1, String s2,
        String[] strings) {
    }

    @Override
    public void visitSource(String s, String s1) {
    }

    @Override
    public void visitOuterClass(String s, String s1, String s2) {
    }

    @Override
    public AnnotationVisitor visitAnnotation(String s, boolean b) {
        return null;
    }

    @Override
    public void visitAttribute(Attribute attribute) {
    }

    @Override
    public void visitInnerClass(String s, String s1, String s2, int i) {
    }

    @Override
    public FieldVisitor visitField(int i, String s, String s1, String s2, Object o) {
        return null;
    }

    @Override
    public MethodVisitor visitMethod(int i, String s, String s1, String s2,
        String[] strings) {
        return null;
    }

    @Override
    public void visitEnd() {
    }

    @Override
    public void visit(String s, Object o) {
    }

    @Override
    public void visitEnum(String s, String s1, String s2) {
    }

    @Override
    public AnnotationVisitor visitAnnotation(String s, String s1) {
        return null;
    }

    @Override
    public AnnotationVisitor visitArray(String s) {
        return null;
    }

    @Override
    public AnnotationVisitor visitAnnotationDefault() {
        return null;
    }

    @Override
    public AnnotationVisitor visitParameterAnnotation(int i, String s, boolean b) {
        return null;
    }

    @Override
    public void visitCode() {
    }

    @Override
    public void visitFrame(int i, int i1, Object[] objects, int i2, Object[] objects1) {
    }

    @Override
    public void visitInsn(int i) {
    }

    @Override
    public void visitIntInsn(int i, int i1) {
    }

    @Override
    public void visitVarInsn(int i, int i1) {
    }

    @Override
    public void visitTypeInsn(int i, String s) {
    }

    @Override
    public void visitFieldInsn(int i, String s, String s1, String s2) {
    }

    @Override
    public void visitMethodInsn(int i, String s, String s1, String s2) {
    }

    @Override
    public void visitJumpInsn(int i, Label label) {
    }

    @Override
    public void visitLabel(Label label) {
    }

    @Override
    public void visitLdcInsn(Object o) {
    }

    @Override
    public void visitIincInsn(int i, int i1) {
    }

    @Override
    public void visitTableSwitchInsn(int i, int i1, Label label, Label[] labels) {
    }

    @Override
    public void visitLookupSwitchInsn(Label label, int[] ints, Label[] labels) {
    }

    @Override
    public void visitMultiANewArrayInsn(String s, int i) {
    }

    @Override
    public void visitTryCatchBlock(Label label, Label label1, Label label2, String s) {
    }

    @Override
    public void visitLocalVariable(String s, String s1, String s2, Label label,
        Label label1, int i) {
    }

    @Override
    public void visitLineNumber(int i, Label label) {
    }

    @Override
    public void visitMaxs(int i, int i1) {
    }
}
