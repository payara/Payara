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

package org.glassfish.ha.store.apt.generators;

import com.sun.mirror.type.TypeMirror;

import java.util.StringTokenizer;

import org.glassfish.ha.store.apt.processor.ClassVisitor;

/**
 * @author Mahesh Kannan
 */
public class StorableGenerator
        extends AbstractGenerator
        implements ClassVisitor {

    private static final String ATTR_NAMES = "_dirtyAttributeNames";

    private String versionGetterMethodName;

    public void visit(String packageName, String javaDoc, String className) {
        println("package " + packageName + ";");
        println();
        println("import java.util.Set;");
        println("import java.util.HashSet;");
        println();
        println("import org.glassfish.ha.store.spi.Storable;");
        println("import org.glassfish.ha.store.MutableStoreEntry;");
        println();
        println("/**");
        StringTokenizer st = new StringTokenizer(javaDoc, "\n");
        while (st.hasMoreTokens()) {
            println(" * " + st.nextToken() + "\n * ");
        }
        println(" */");
        println();
        println("public class Storable" + className + "__");
        increaseIndent();
        println("extends " + className);
        println("implements Storable, MutableStoreEntry {");
        println();
        println("private String _storeName;");
        println();
        println("private String _hashKey;");
        println();
        println("private Set<String> " + ATTR_NAMES + " = new HashSet<String>();");
        println();
    }

    private void handleDirtyAttribute(String setterMethodName, String attrName, TypeMirror paramType) {
        super.addAttribute(attrName, paramType);
        println("public void " + setterMethodName + "("
                + getWrapperType(paramType) + " value) { ");
        increaseIndent();        
        println("_markAsDirty(\"" + attrName + "\");");
        println("super." + setterMethodName + "(value);");
        decreaseIndent();
    }

    public void visitSetter(String setterMethodName, String attrName, String javaDoc, TypeMirror paramType) {
        println("//@Attribute(name=\"" + attrName + "\")");
        handleDirtyAttribute(setterMethodName, attrName, paramType);
        println("}");
        println();
    }

    public void visitVersionMethod(String setterMethodName, String attrName, String javaDoc, TypeMirror paramType) {
        versionGetterMethodName = setterMethodName;
        println("//@Version(name=\"" + attrName + "\")");
        handleDirtyAttribute(setterMethodName, attrName, paramType);
        println("}");
        println();
    }

    public void visitEnd() {
        println("//Storable method");
        println("public String _getStoreName() {");
        increaseIndent();
        println("return _storeName;");
        decreaseIndent();
        println("}");
        println();

        String getVersionName = (versionGetterMethodName == null)
                ? null : versionGetterMethodName;
        if (getVersionName != null) {
            getVersionName = "g" + getVersionName.substring(1);
        }
        
        println("public String _getVersion() {");
        increaseIndent();
        println("return " + getVersionName + "();");
        decreaseIndent();
        println("}");
        println();

        println("public Set<String> _getDirtyAttributeNames() {");
        increaseIndent();
        println("return " + ATTR_NAMES + ";");
        decreaseIndent();
        println("}");
        println();

        println("//MutableStoreEntry methods");
        println("public void _markAsDirty(String attrName) {");
        increaseIndent();
        println(ATTR_NAMES + ".add(attrName);");
        decreaseIndent();
        println("}");
        println();

        println("public void _markAsClean(String attrName) {");
        increaseIndent();
        println(ATTR_NAMES + ".remove(attrName);");
        decreaseIndent();
        println("}");
        println();

        println("public void _markAsClean() {");
        increaseIndent();
        println(ATTR_NAMES + " = new HashSet<String>();");
        decreaseIndent();
        println("}");
        println();

        decreaseIndent();
        println("}");
    }
}
