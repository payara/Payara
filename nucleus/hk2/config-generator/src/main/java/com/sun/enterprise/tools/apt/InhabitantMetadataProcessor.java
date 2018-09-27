/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007-2017 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://oss.oracle.com/licenses/CDDL+GPL-1.1
 * or LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at LICENSE.txt.
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

package com.sun.enterprise.tools.apt;

import org.glassfish.hk2.api.Metadata;
import org.jvnet.hk2.component.MultiMap;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Discoveres all {@link InhabitantMetadata} and puts them into the bag.
 *
 * @author Kohsuke Kawaguchi
 */
public class InhabitantMetadataProcessor extends TypeHierarchyVisitor<MultiMap<String,String>> {

    private final Map<DeclaredType, Model> models = new HashMap<DeclaredType, Model>();

    /**
     * For a particular {@link DeclaredType}, remember what properties are to be added as metadata.
     */
    private static final class Model {
        private final DeclaredType type;
        private final Map<ExecutableElement, String> metadataProperties = new HashMap<ExecutableElement, String>();

        public Model(DeclaredType type) {
            this.type = type;
            for (ExecutableElement e : ElementFilter.methodsIn(type.asElement().getEnclosedElements())) {
                Metadata im = e.getAnnotation(Metadata.class);
                if(im==null)    continue;

                String name = im.value();
                if (name.length() == 0) name = ((TypeElement) type.asElement()).getQualifiedName().toString() + '.' + e.getSimpleName();

                metadataProperties.put(e,name);
            }
        }

        /**
         * Based on the model, parse the annotation mirror and updates the metadata bag by adding
         * discovered values.
         */
        public void parse(AnnotationMirror a, MultiMap<String,String> metadataBag) {
            assert a.getAnnotationType().equals(type);

            for (Map.Entry<ExecutableElement, String> e : metadataProperties.entrySet()) {
                Map<? extends ExecutableElement, ? extends AnnotationValue> vals = a.getElementValues();
                AnnotationValue value = vals.get(e.getKey());
                if (value!=null) {
                    metadataBag.add(e.getValue(), toString(value));
                } else {
                    Collection<ExecutableElement> methods = ElementFilter.methodsIn(a.getAnnotationType().asElement().getEnclosedElements());
                    for (ExecutableElement decl : methods) {
                        if (e.getKey().equals(decl)) {
                            value = decl.getDefaultValue();
                            metadataBag.add(e.getValue(), toString(value));
                            break;
                        }
                    }
                }
            }
        }

        private String toString(AnnotationValue value) {
            if (value.getValue() instanceof TypeMirror) {
                TypeMirror tm = (TypeMirror) value.getValue();
                if (tm.getKind().equals(TypeKind.DECLARED)) {
                    DeclaredType dt = (DeclaredType) tm;
                    return getClassName((TypeElement) dt.asElement());
                }
            }
            return value.toString();
        }

        /**
         * Returns the fully qualified class name.
         * The difference between this and {@link TypeElement#getQualifiedName()}
         * is that this method returns the same format as {@link Class#getName()}.
         *
         * Notably, separator for nested classes is '$', not '.'
         */
        private String getClassName(TypeElement d) {
            if (d.getEnclosingElement() != null)
                return getClassName((TypeElement) d.getEnclosingElement()) + '$' + d.getSimpleName();
            else
                return d.getQualifiedName().toString();
        }
    }

    public MultiMap<String, String> process(TypeElement d) {
        visited.clear();
        MultiMap<String,String> r = new MultiMap<String, String>();
        check(d,r);
        return r;
    }

    protected void check(TypeElement d, MultiMap<String, String> result) {
        checkAnnotations(d, result);
        super.check(d,result);
    }

    private void checkAnnotations(TypeElement d, MultiMap<String, String> result) {
        for (AnnotationMirror a : d.getAnnotationMirrors()) {
            getModel(a.getAnnotationType()).parse(a,result);
            // check meta-annotations
            for (AnnotationMirror b : a.getAnnotationType().asElement().getAnnotationMirrors()) {
                getModel(b.getAnnotationType()).parse(b,result);
            }
        }
    }

    /**
     * Checks if the given annotation mirror has the given meta-annotation on it.
     */
    /*private boolean hasMetaAnnotation(AnnotationMirror a, Class<? extends Annotation> type) {
        return a.getAnnotationType().asElement().getAnnotation(type) != null;
    }*/

    private Model getModel(DeclaredType type) {
        Model model = models.get(type);
        if(model==null)
            models.put(type,model=new Model(type));
        return model;
    }
}
