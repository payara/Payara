/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2013 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
// Portions Copyright [2026] Payara Foundation and/or its affiliates

package org.glassfish.apf;

import com.sun.enterprise.util.LocalStringManagerImpl;
import org.glassfish.internal.deployment.ClassHierarchy;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.IndexView;

import java.lang.annotation.ElementType;

/**
 * An annotation found on an element of the deployment, as read from the Jandex index, together
 * with the context it is processed in. Attribute values are read with the annotation type's
 * defaults applied, so handlers see what {@code java.lang.annotation.Annotation} getters would
 * return without the class ever being loaded.
 *
 * @author Jerome Dochez
 */
public class AnnotationInfo {

    private static final LocalStringManagerImpl localStrings = new LocalStringManagerImpl(AnnotationInfo.class);

    private final AnnotationTarget annotatedElement;
    private final AnnotationInstance annotation;
    private final ProcessingContext context;
    private final ElementType type;

    /**
     * @param context the processing context
     * @param element the annotated element: a class, field, method or method parameter
     * @param annotation the annotation instance on that element
     * @param type the type of the element being processed
     */
    public AnnotationInfo(ProcessingContext context, AnnotationTarget element, AnnotationInstance annotation,
                          ElementType type) {
        this.context = context;
        this.annotatedElement = element;
        this.annotation = annotation;
        this.type = type;
    }

    /**
     * @return the annotated element: a {@code ClassInfo}, {@code FieldInfo}, {@code MethodInfo}
     * or {@code MethodParameterInfo}
     */
    public AnnotationTarget getAnnotatedElement() {
        return annotatedElement;
    }

    public AnnotationInstance getAnnotation() {
        return annotation;
    }

    /**
     * @return the fully qualified name of the annotation type
     */
    public String getAnnotationTypeName() {
        return annotation.name().toString();
    }

    public ProcessingContext getProcessingContext() {
        return context;
    }

    public ElementType getElementType() {
        return type;
    }

    /**
     * @return the index the deployment's classes, their super types and the annotation types resolve in
     */
    public IndexView getIndex() {
        return context.getIndex();
    }

    /**
     * @return the class declaring the annotated element, the class itself for a class element
     */
    public ClassInfo getDeclaringClass() {
        return ClassHierarchy.declaringClass(annotatedElement);
    }

    /**
     * @return the value of the attribute, explicit or the annotation type's default
     */
    public AnnotationValue value(String name) {
        return AnnotationValues.value(getIndex(), annotation, name);
    }

    public String stringValue(String name) {
        return AnnotationValues.string(getIndex(), annotation, name);
    }

    public String[] stringArrayValue(String name) {
        return AnnotationValues.strings(getIndex(), annotation, name);
    }

    public boolean booleanValue(String name) {
        return AnnotationValues.bool(getIndex(), annotation, name);
    }

    public int intValue(String name) {
        return AnnotationValues.integer(getIndex(), annotation, name);
    }

    public long longValue(String name) {
        return AnnotationValues.longValue(getIndex(), annotation, name);
    }

    /**
     * @return the name of the enum constant
     */
    public String enumValue(String name) {
        return AnnotationValues.enumName(getIndex(), annotation, name);
    }

    public <E extends Enum<E>> E enumValue(String name, Class<E> enumType) {
        return AnnotationValues.enumValue(getIndex(), annotation, name, enumType);
    }

    /**
     * @return the name of the class-valued attribute, as {@code Class.getName()} returns it
     */
    public String classValue(String name) {
        return AnnotationValues.className(getIndex(), annotation, name);
    }

    public String[] classArrayValue(String name) {
        return AnnotationValues.classNames(getIndex(), annotation, name);
    }

    public AnnotationInstance nestedValue(String name) {
        return AnnotationValues.nested(getIndex(), annotation, name);
    }

    public AnnotationInstance[] nestedArrayValue(String name) {
        return AnnotationValues.nestedArray(getIndex(), annotation, name);
    }

    @Override
    public String toString() {
        return localStrings.getLocalString("annotatedinfo.string",
                "annotation [{0}] on annotated element [{1}] of type [{2}]", annotation, annotatedElement, type);
    }
}
