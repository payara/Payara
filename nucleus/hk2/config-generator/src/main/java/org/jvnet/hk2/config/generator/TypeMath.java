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

package org.jvnet.hk2.config.generator;

import com.sun.codemodel.JExpr;
import com.sun.codemodel.JExpression;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.WildcardType;
import javax.lang.model.util.SimpleTypeVisitor6;
import java.util.Collection;
import java.util.Locale;

/**
 * Defines several type arithemetic operations.
 * @author Kohsuke Kawaguchi
 */
class TypeMath {

    protected final ProcessingEnvironment env;

    /**
     * Given a declaration X and mirror Y, finds the parametrization of Z=X&lt;...> such that
     * Y is assignable to Z.
     */
    final SimpleTypeVisitor6<TypeMirror, TypeElement> baseClassFinder = new SimpleTypeVisitor6<TypeMirror, TypeElement>() {

        @Override
        public TypeMirror visitDeclared(DeclaredType t, TypeElement sup) {

            TypeMirror r = onDeclaredType(t, sup);
            if (r != null) return r;

            Element e = t.asElement();
            switch (e.getKind()) {
                case CLASS: {
                    // otherwise recursively apply super class and base types
                    TypeMirror sc = ((TypeElement) e).getSuperclass();
                    if (!TypeKind.NONE.equals(sc.getKind()))
                        r = visitDeclared((DeclaredType) sc, sup);
                    if (r != null) return r;
                }
            }
            return null;
        }

        @Override
        protected TypeMirror defaultAction(TypeMirror e, TypeElement typeElement) {
            return null;
        }

        private TypeMirror onDeclaredType(DeclaredType t, TypeElement sup) {
            // t = sup<...>
            if (t.asElement().equals(sup))
                return t;

            for (TypeMirror i : env.getTypeUtils().directSupertypes(t)) {
                TypeMirror r = visitDeclared((DeclaredType) i, sup);
                if (r != null) return r;
            }

            return null;
        }

        @Override
        public TypeMirror visitTypeVariable(TypeVariable t, TypeElement sup) {
            // we are checking if T (declared as T extends A&B&C) is assignable to sup.
            // so apply bounds recursively.
            for (TypeMirror r : ((TypeParameterElement) t.asElement()).getBounds()) {
                TypeMirror m = visit(r, sup);
                if (m != null) return m;
            }
            return null;
        }

        @Override
        public TypeMirror visitWildcard(WildcardType type, TypeElement sup) {
            // we are checking if T (= ? extends A&B&C) is assignable to sup.
            // so apply bounds recursively.
            return visit(type.getExtendsBound(), sup);
        }
    };
    /**
     * Adapts the string expression into the expression of the given type.
     */
    final SimpleTypeVisitor6<JExpression, JExpression> simpleValueConverter = new SimpleTypeVisitor6<JExpression, JExpression>() {

        @Override
        public JExpression visitPrimitive(PrimitiveType type, JExpression param) {
            String kind = type.getKind().toString();
            return JExpr.invoke("as" + kind.charAt(0) + kind.substring(1).toLowerCase(Locale.ENGLISH)).arg(param);
        }

        @Override
        public JExpression visitDeclared(DeclaredType type, JExpression param) {
            String qn = ((TypeElement) type.asElement()).getQualifiedName().toString();
            if (qn.equals("java.lang.String"))
                return param;   // no conversion needed for string
            // return JExpr.invoke("as"+type.getDeclaration().getSimpleName()).arg(param);
            throw new UnsupportedOperationException();
        }

        @Override
        protected JExpression defaultAction(TypeMirror e, JExpression jExpression) {
            throw new UnsupportedOperationException();
        }
    };

    public TypeMath(ProcessingEnvironment env) {
        this.env = env;
    }

    TypeMirror isCollection(TypeMirror t) {
        TypeMirror collectionType = baseClassFinder.visit(t, env.getElementUtils().getTypeElement(Collection.class.getName()));
        if (collectionType != null) {
            DeclaredType d = (DeclaredType) collectionType;
            return d.getTypeArguments().iterator().next();
        } else
            return null;
    }
}
