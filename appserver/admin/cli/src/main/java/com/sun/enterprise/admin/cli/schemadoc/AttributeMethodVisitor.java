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
import org.glassfish.api.admin.config.PropertiesDesc;
import org.glassfish.api.admin.config.PropertyDesc;
import org.jvnet.hk2.config.Attribute;

public class AttributeMethodVisitor extends EmptyVisitor {
    private ClassDef def;
    private String name;
    private String type;
    private boolean duckTyped;

    public AttributeMethodVisitor(ClassDef classDef, String method, String aggType) {
        def = classDef;
        name = method;
        type = aggType;
        def.addAttribute(name, null);
    }

    @Override
    public String toString() {
        return "AttributeMethodVisitor{" +
            "def=" + def +
            ", name='" + name + '\'' +
            ", type='" + type + '\'' +
            ", duckTyped=" + duckTyped +
            '}';
    }

    /**
     * Visits an annotation of this method.
     *
     * @param desc the class descriptor of the annotation class.
     * @param visible <tt>true</tt> if the annotation is visible at runtime.
     *
     * @return a visitor to visit the annotation values, or <tt>null</tt> if this visitor is not interested in visiting
     *         this annotation.
     */
    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        duckTyped |= "Lorg/jvnet/hk2/config/DuckTyped;".equals(desc);
        AnnotationVisitor visitor = null;
        if ("Lorg/jvnet/hk2/config/Attribute;".equals(desc) || "Lorg/jvnet/hk2/config/Element;".equals(desc)) {
            try {
                final Class<?> configurable = Thread.currentThread().getContextClassLoader().loadClass(def.getDef());
                final Attribute annotation = configurable.getMethod(name).getAnnotation(Attribute.class);
                def.addAttribute(name, annotation);
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage(), e);
            }

        } else if ("Lorg/glassfish/api/admin/config/PropertiesDesc;".equals(desc)) {
            try {
                final Class<?> configurable = Thread.currentThread().getContextClassLoader().loadClass(def.getDef());
                final PropertiesDesc annotation = configurable.getMethod(name).getAnnotation(PropertiesDesc.class);
                final PropertyDesc[] propertyDescs = annotation.props();
                for (PropertyDesc prop : propertyDescs) {
                    def.addProperty(prop);
                }
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        }
        return visitor;
    }

    @Override
    public void visitEnd() {
        if (!duckTyped) {
            if (!isSimpleType(type)) {
                def.addAggregatedType(name, type);
                def.removeAttribute(name);
            }
        } else {
            def.removeAttribute(name);
        }
    }

    private boolean isSimpleType(String type) {
        return type.startsWith("java");
    }
}
