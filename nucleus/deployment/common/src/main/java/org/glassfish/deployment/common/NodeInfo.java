/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2012 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.deployment.common;

import org.objectweb.asm.*;

import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.io.InputStream;


/**
 * A class that maintains information about a class
 *
 * @author Mahesh Kannan
 * 
 */
class NodeInfo
        implements ClassVisitor {

    private int nodeId;

    private int access;

    private String className;

    private String superClassName;

    private List<String> classLevelAnnotations;

    private String[] interfaces;

    private boolean parsed;

    private Set<String> directSubClasses;

    private Set<String> directImplementors;

    private Set<NodeInfo> directSubClassesNodeInfos;

    private Set<NodeInfo> directImplementorsNodeInfos;

    private int classType;

    private String classTypeAsString;

    private static final List<String> EMPTY_STRING_LIST = new ArrayList<String>(0);

    private static final Set<NodeInfo> EMPTY_NODEINFO_SET = new HashSet<NodeInfo>();

    NodeInfo(byte[] classData) {
        ClassReader cr = new ClassReader(classData);

        cr.accept(this, ClassReader.SKIP_CODE);
    }

    void load(byte[] classData) {
        ClassReader cr = new ClassReader(classData);
        cr.accept(this, ClassReader.SKIP_CODE);
    }

    void markAsAnnotaionType() {
        if (! parsed) {
            classType = Opcodes.ACC_ANNOTATION;
        } else {
            throw new IllegalStateException("Cannot mark as AnnotationType. "
                    + "Already marked as " + classTypeAsString);
        }
    }

    void markAsClassType() {
        if (! parsed) {
            classType = 0;
        } else {
            throw new IllegalStateException("Cannot mark as Class Type. "
                    + "Already marked as " + classTypeAsString);
        }
    }

    void markAsInterfaceType() {
        if (! parsed) {
            classType = Opcodes.ACC_INTERFACE;
        } else {
            throw new IllegalStateException("Cannot mark as Interface Type. "
                    + "Already marked as " + classTypeAsString);
        }
    }

    void markAsEnumType() {
        if (! parsed) {
            classType = Opcodes.ACC_ENUM;
        } else {
            throw new IllegalStateException("Cannot mark as Enum Type. "
                    + "Already marked as " + classTypeAsString);
        }
    }

    NodeInfo(String className) {
        this.className = className;
    }

    int getNodeId() {
        return nodeId;
    }

    void setNodeId(int nodeId) {
        this.nodeId = nodeId;
    }

    String getClassName() {
        return className;
    }

    String getSuperClassName() {
        return superClassName;
    }

    List<String> getClassLevelAnnotations() {
        return classLevelAnnotations == null ? EMPTY_STRING_LIST : classLevelAnnotations;
    }

    String[] getInterfaces() {
        return interfaces == null ? new String[0] : interfaces;
    }

    void addDirectSubClass(NodeInfo ni) {
        String subClass = ni.getClassName();
        if (directSubClasses == null) {
            directSubClasses = new HashSet<String>();
            directSubClassesNodeInfos = new HashSet<NodeInfo>();
        }

        directSubClasses.add(subClass);
        directSubClassesNodeInfos.add(ni);
    }

    void addDirectImplementor( NodeInfo ni) {
        String implementor = ni.getClassName();
        if (directImplementors == null) {
            directImplementors = new HashSet<String>();
            directImplementorsNodeInfos = new HashSet<NodeInfo>();
        }

        directImplementors.add(implementor);
        directImplementorsNodeInfos.add(ni);
    }

    Set<NodeInfo> getDirectImplementors() {
        return directImplementorsNodeInfos == null ? EMPTY_NODEINFO_SET : directImplementorsNodeInfos;
    }

    Set<NodeInfo> getDirectSubClass() {
        return directSubClassesNodeInfos == null ?  EMPTY_NODEINFO_SET : directSubClassesNodeInfos;
    }

    boolean isInterface() {
        return classType == Opcodes.ACC_INTERFACE;
    }

    boolean isAnnotation() {
        return classType == Opcodes.ACC_ANNOTATION;
    }

    boolean isClass() {
        return classType == 0;
    }

    @Override
    public void visit(int version, int access, String className, String signature,
                      String superName, String[] interfaces) {
        if ((this.className != null) && (! this.className.equals(className))) {
            throw new IllegalStateException("Internal error: " + className + " != " + this.className);
        }

        this.access = access;
        this.className = className;
        this.superClassName = superName;
        this.interfaces = interfaces == null ? new String[0] : interfaces;

        determineClassType();
    }

    public void determineClassType() {

        if ((access & Opcodes.ACC_ANNOTATION) != 0) {
            classType = Opcodes.ACC_ANNOTATION;
            classTypeAsString = "ANNOTATION";
        } else if ((access & Opcodes.ACC_INTERFACE) != 0) {
            classType = Opcodes.ACC_INTERFACE;
            classTypeAsString = "INTERFACE";
        } else if ((access & Opcodes.ACC_ENUM) != 0) {
            classType = Opcodes.ACC_ENUM;
            classTypeAsString = "ENUM";
        } else {
            classType = 0;
            classTypeAsString = "CLASS";
        }
    }

    public String getClassTypeAsString() {
        return classTypeAsString;
    }

    @Override
    public void visitSource(String s, String s1) {
    }

    @Override
    public void visitOuterClass(String s, String s1, String s2) {
    }

    @Override
    public AnnotationVisitor visitAnnotation(String s, boolean b) {
        if (classLevelAnnotations == null) {
            classLevelAnnotations = new ArrayList<String>(2);
        }
        int len = s.length();
        s = s.substring(1, len-1);
        classLevelAnnotations.add(s);
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
    public MethodVisitor visitMethod(int i, String s, String s1, String s2, String[] strings) {
        return null;
    }

    public boolean isParsed() {
        return parsed;
    }

    @Override
    public void visitEnd() {
        parsed = true;
    }

    public String toString() {

        StringBuilder sb = new StringBuilder(className).append(": ");
        if (classLevelAnnotations != null) {
            for (String ann : classLevelAnnotations) {
                sb.append("\n  @").append(ann).append(" ");
            }
        }
        sb.append(className).append(":");
        if (superClassName != null) {
            sb.append(" -> ").append(superClassName);
        }
        if (interfaces != null) {
            String delim = " & ";
            for (String inter : interfaces) {
                sb.append(delim).append(inter);
                delim = ", ";
            }
        }

        if (directSubClasses != null) {
            for (String sub : directSubClasses) {
                sb.append("\n\tdirect subclass => ").append(sub);
            }
        }
        if (directImplementors != null) {
            for (String impl : directImplementors) {
                sb.append("\n\tdirect implementor => ").append(impl);
            }
        }
        sb.append("\n**********************");
        return sb.toString();
    }
}
