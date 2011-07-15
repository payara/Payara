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
import org.glassfish.ha.store.apt.processor.ClassVisitor;

/**
 * @author Mahesh Kannan
 */
public class StoreEntryMetadataGenerator
        extends AbstractGenerator
        implements ClassVisitor {

    private String className;

    private int index = 0;

    public void visit(String packageName, String javaDoc, String className) {
        this.className = className;
        println("package " + packageName + ";");
        println();
        println("import java.util.ArrayList;");
        println("import java.util.Collection;");
        println("import java.util.HashMap;");
        println("import java.util.Map;");
        println();
        println("import org.glassfish.ha.store.spi.AttributeMetadata;");
        println();
        println("/**");
        println(" * Metadata for " + className);
        println(" *");
        println(" */");
        println();
        println("public class MetadataFor" + className + " {");
        increaseIndent();
        println();
    }

    public void visitSetter(String methodName, String attrName, String javaDoc, TypeMirror paramType) {
        printInfo(methodName, attrName, javaDoc, paramType, "Attribute");
    }

    public void visitVersionMethod(String methodName, String attrName, String javaDoc, TypeMirror paramType) {
        printInfo(methodName, attrName, javaDoc, paramType, "Version");
    }

    public void visitHashKeyMethod(String methodName, String attrName, String javaDoc, TypeMirror paramType) {
        printInfo(methodName, attrName, javaDoc, paramType, "HashKey");
    }

    private void printInfo(String methodName, String attrName, String javaDoc, TypeMirror paramType, String token) {
        attrNames.add(attrName);
        println("//@" + token + "(name=\"" + attrName + "\")");
        println("public static AttributeMetadata<" + className + ", " + getWrapperType(paramType) + "> "
                + attrName + " = ");
        println("\tnew AttributeMetadataImpl<" + className + ", " + getWrapperType(paramType) + ">("
                + index++ + ", \"" + attrName + "\", " + className + ".class" + ", " + getWrapperType(paramType) + ".class"
                + ", \"" + token + "\");");
        println();
    }

    public void visitEnd() {
        //generateStoreEntryMetadataMethods();
        println();
        println("public Collection<AttributeMetadata<" + className + ", ?>> getAllAttributeMetadata() {");
        println("\t return attributes__;");
        println("}");
        println();
        println("private static Collection<AttributeMetadata<" + className + ", ?>> attributes__");
        println("\t= new ArrayList<AttributeMetadata<" + className + ", ?>>();");
        println();
        print("static {");
        increaseIndent();

        println();
        for (String attr : attrNames) {
            println("attributes__.add(" + attr + ");");
        }
        decreaseIndent();
        println("}");

        printAttibuteMetaDataImplClass();
        decreaseIndent();
        println("}");
        println();
    }
    
    private void generateStoreEntryMetadataMethods() {
        /*
        println("public AttributeMetadata<" + className + ", ?> getAttributeMetadata("
            + "String attrName) {");
        println("\treturn attrMap__.get(attrName);");
        println("}");

        println();
        println("public Collection<AttributeMetadata<" + className + ", ?>> getAllAttributeMetadata() {");
        println("\t return attrMap__.values();");
        println("}");

        println();
        println("public Collection<String> getAllAttributeNames() {");
        println("\t return attributes__;");
        println("}");
        */
        println();
    }

    private void printAttibuteMetaDataImplClass() {
        println("private static class AttributeMetaDataImpl<V, T>");
        increaseIndent();
            println("implements AttributeMetaData<V, T> {");
            println();
            println("int index;");
            println("String attrName;");
            println("Class<V> vClazz;");
            println("T type;");
            println("String token;");
            println();
            println("public AttributeMetaDataImpl(int index, String attrName, Class<V> vClazz, Class<T> type, ");
            increaseIndent();
                println("String token) {");
                println("this.index = index;");
                println("this.attrName = attrName;");
                println("this.vClazz = vClazz;");
                println("this.type = type;");
                println("this.token = token;");
            decreaseIndent();
            println("}");
            println();
            println("public Class<T> getAttributeType() {");
            increaseIndent();
                println("return type;");
            decreaseIndent();
            println("}");
            println();
            println("public boolean isVersionAttribute() {");
            increaseIndent();
                println("return \"Version\".equals(token);");
            decreaseIndent();
            println("}");
            println();
            println("public Method isHashKeyAttribute() {");
            increaseIndent();
                println("return \"HashKey\".equals(token);");
            decreaseIndent();
            println("}");
            println();

        decreaseIndent();
        println("}");
    }
}
