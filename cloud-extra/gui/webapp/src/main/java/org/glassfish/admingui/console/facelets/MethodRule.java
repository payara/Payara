/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.admingui.console.facelets;

import java.io.Serializable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.el.ELException;
import javax.el.MethodExpression;

import javax.faces.context.FacesContext;
import javax.faces.el.EvaluationException;
import javax.faces.el.MethodBinding;

import javax.faces.el.MethodNotFoundException;
import javax.faces.view.facelets.FaceletContext;
import javax.faces.view.facelets.MetaRule;
import javax.faces.view.facelets.Metadata;
import javax.faces.view.facelets.MetadataTarget;
import javax.faces.view.facelets.TagAttribute;
import javax.faces.view.facelets.TagAttributeException;

/**
 * Optional Rule for binding Method[Binding|Expression] properties
 *
 * @author Mike Kienenberger
 * @author Jacob Hookom
 * 
 * Implementation copied from Facelets 1.1.14, as it got hidden by JSF 2.0
 */
public class MethodRule extends MetaRule {

    private final String methodName;

    private final Class returnTypeClass;

    private final Class[] params;

    public MethodRule(String methodName, Class returnTypeClass,
                      Class[] params) {
        this.methodName = methodName;
        this.returnTypeClass = returnTypeClass;
        this.params = params;
    }

    public Metadata applyRule(String name, TagAttribute attribute,
                              MetadataTarget meta) {
        if (false == name.equals(this.methodName))
            return null;

        if (MethodBinding.class.equals(meta.getPropertyType(name))) {
            Method method = meta.getWriteMethod(name);
            if (method != null) {
                return new MethodBindingMetadata(method, attribute,
                                                 this.returnTypeClass,
                                                 this.params);
            }
        } else if (MethodExpression.class.equals(meta.getPropertyType(name))) {
            Method method = meta.getWriteMethod(name);
            if (method != null) {
                return new MethodExpressionMetadata(method, attribute,
                                                    this.returnTypeClass,
                                                    this.params);
            }
        }

        return null;
    }

    private static class MethodBindingMetadata extends Metadata {
        private final Method _method;

        private final TagAttribute _attribute;

        private Class[] _paramList;

        private Class _returnType;

        public MethodBindingMetadata(Method method, TagAttribute attribute,
                                     Class returnType, Class[] paramList) {
            _method = method;
            _attribute = attribute;
            _paramList = paramList;
            _returnType = returnType;
        }

        public void applyMetadata(FaceletContext ctx, Object instance) {
            MethodExpression expr =
                _attribute.getMethodExpression(ctx, _returnType, _paramList);

            try {
                _method.invoke(instance,
                               new Object[] { new LegacyMethodBinding(expr) });
            } catch (InvocationTargetException e) {
                throw new TagAttributeException(_attribute, e.getCause());
            } catch (Exception e) {
                throw new TagAttributeException(_attribute, e);
            }
        }
    }

    private static class MethodExpressionMetadata extends Metadata {
        private final Method _method;

        private final TagAttribute _attribute;

        private Class[] _paramList;

        private Class _returnType;

        public MethodExpressionMetadata(Method method, TagAttribute attribute,
                                        Class returnType, Class[] paramList) {
            _method = method;
            _attribute = attribute;
            _paramList = paramList;
            _returnType = returnType;
        }

        public void applyMetadata(FaceletContext ctx, Object instance) {
            MethodExpression expr =
                _attribute.getMethodExpression(ctx, _returnType, _paramList);

            try {
                _method.invoke(instance, new Object[] { expr });
            } catch (InvocationTargetException e) {
                throw new TagAttributeException(_attribute, e.getCause());
            } catch (Exception e) {
                throw new TagAttributeException(_attribute, e);
            }
        }
    }

    private static class LegacyMethodBinding extends MethodBinding implements Serializable {

        private static final long serialVersionUID = 1L;

        private final MethodExpression m;

        public LegacyMethodBinding(MethodExpression m) {
            this.m = m;
        }

        /*
       * (non-Javadoc)
       *
       * @see javax.faces.el.MethodBinding#getType(javax.faces.context.FacesContext)
       */

        public Class getType(FacesContext context) throws MethodNotFoundException {
            try {
                return m.getMethodInfo(context.getELContext()).getReturnType();
            } catch (javax.el.MethodNotFoundException e) {
                throw new MethodNotFoundException(e.getMessage(),
                                                  e.getCause());
            } catch (ELException e) {
                throw new EvaluationException(e.getMessage(), e.getCause());
            }
        }

        /*
       * (non-Javadoc)
       *
       * @see javax.faces.el.MethodBinding#invoke(javax.faces.context.FacesContext,
       *      java.lang.Object[])
       */

        public Object invoke(FacesContext context,
                             Object[] params) throws EvaluationException,
                                                     MethodNotFoundException {
            try {
                return m.invoke(context.getELContext(), params);
            } catch (javax.el.MethodNotFoundException e) {
                throw new MethodNotFoundException(e.getMessage(),
                                                  e.getCause());
            } catch (ELException e) {
                throw new EvaluationException(e.getMessage(), e.getCause());
            }
        }

        public String getExpressionString() {
            return m.getExpressionString();
        }
    }
}
