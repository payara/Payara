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

package org.glassfish.ha.store.apt.processor;

import com.sun.mirror.apt.AnnotationProcessor;
import com.sun.mirror.apt.AnnotationProcessorEnvironment;
import com.sun.mirror.declaration.*;
import com.sun.mirror.util.DeclarationFilter;
import com.sun.mirror.type.TypeMirror;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

import org.glassfish.ha.store.annotations.Attribute;
import org.glassfish.ha.store.annotations.Version;
import org.glassfish.ha.store.apt.generators.StorableGenerator;
import org.glassfish.ha.store.apt.generators.StoreEntryMetadataGenerator;

/**
 * @author Mahesh Kannan
 */
public class StoreEntryAnnotationProcessor
        implements AnnotationProcessor {

    private Set<AnnotationTypeDeclaration> decls;

    private AnnotationProcessorEnvironment env;

    private String qName;

    private Collection<ClassInfo> classInfos
            = new ArrayList<ClassInfo>();

    StoreEntryAnnotationProcessor(Set<AnnotationTypeDeclaration> decls,
                                  AnnotationProcessorEnvironment env) {
        this.decls = decls;
        this.env = env;
    }

    public void process() {
        int counter = 0;
        AnnotationTypeDeclaration storeEntryAnnDecls = decls.iterator().next();
        DeclarationFilter classFilter = DeclarationFilter.getFilter(ClassDeclaration.class);
        for (Declaration decl : classFilter.filter(env.getDeclarationsAnnotatedWith(storeEntryAnnDecls))) {
            if (decl != null) {
                ClassDeclaration classDecl = (ClassDeclaration) decl;
                ClassInfo classInfo = new ClassInfo(classDecl);
                classInfo.setJavaDoc(classDecl.getDocComment());
                classInfos.add(classInfo);
                qName = classDecl.getQualifiedName();


                DeclarationFilter getterFilter = new DeclarationFilter() {
                    public boolean matches(Declaration d) {
                        return d.getSimpleName().startsWith("set");
                    }
                };

                Collection<? extends MethodDeclaration> methods = classDecl.getMethods();
                TypeMirror paramType = null;
                for (MethodDeclaration m : getterFilter.filter(methods)) {
                    MethodInfo methodInfo = new MethodInfo();
                    String attributeName = null;
                    Attribute attrAnn = m.getAnnotation(Attribute.class);
                    if (attrAnn != null) {
                        attributeName = attrAnn.value();
                        methodInfo.type = MethodInfo.MethodType.SETTER;
                    } else {
                        Version versionAnn = m.getAnnotation(Version.class);
                        if (versionAnn != null) {
                            attributeName = versionAnn.name();
                            methodInfo.type = MethodInfo.MethodType.VERSION;
                        } else {
                            //Some getter method
                            continue;
                        }
                    }

                    String simpleName = m.getSimpleName();
                    if (! simpleName.startsWith("set")) {
                        //TODO Warning??
                        continue;
                    }
                    
                    if (attributeName == null || attributeName.length() == 0) {
                        attributeName = simpleName;
                        attributeName = Character.toLowerCase(attributeName.charAt(3)) + attributeName.substring(4);
                    }
                    methodInfo.attrName = attributeName;

                    System.out.println("Found attribute: " + attributeName);
                    Collection<ParameterDeclaration> paramDecls = m.getParameters();
                    if ((paramDecls != null) && (paramDecls.size() == 1)) {
                        ParameterDeclaration paramDecl =  paramDecls.iterator().next();
                        paramType = paramDecl.getType();
                    }

                    methodInfo.getter = m;
                    methodInfo.paramType = paramType;
                    classInfo.addMethodInfo(methodInfo);
                }
            }
        }

        this.accept(new StorableGenerator());
        this.accept(new StoreEntryMetadataGenerator());
    }

    public void accept(ClassVisitor cv) {
        for (ClassInfo classInfo : classInfos) {
            ClassDeclaration classDecl = classInfo.getClassDeclaration();
            cv.visit(classDecl.getPackage().toString(), classInfo.getJavaDoc(), classDecl.getSimpleName());
            for (MethodInfo methodInfo : classInfo.getMethodInfos()) {
                switch (methodInfo.type) {
                    case SETTER:
                        cv.visitSetter(methodInfo.getter.getSimpleName(), methodInfo.attrName, null, methodInfo.paramType);
                        break;
                    case VERSION:
                        cv.visitVersionMethod(methodInfo.getter.getSimpleName(), methodInfo.attrName, null, methodInfo.paramType);
                        break;
                    /*
                    case HASHKEY:
                        cv.visitHashKeyMethod(methodInfo.getter.getSimpleName(), methodInfo.attrName, null, methodInfo.paramType);
                        break;
                    */
                }
            }
            cv.visitEnd();
        }
    }
}
