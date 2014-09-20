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

package com.sun.jdo.api.persistence.enhancer.impl;

import java.util.Map;
//@olsen: disabled feature
/*
import java.util.Set;
import java.util.HashSet;
import java.util.Enumeration;
*/

import com.sun.jdo.api.persistence.enhancer.classfile.*;

import com.sun.jdo.api.persistence.enhancer.util.Support;

//@olsen: cosmetics
//@olsen: moved: this class -> package impl
//@olsen: subst: /* ... */ -> // ...
//@olsen: subst: FilterEnv -> Environment
//@olsen: dropped parameter 'Environment env', use association instead
//@olsen: subst: Hashtable -> Map, Set, HashSet
//@olsen: subst: theClass, classAction -> ca
//@olsen: added: support for I18N
//@olsen: subst: FilterError -> UserException, assert()
//@olsen: removed: old, disabled ODI code


/**
 * MethodAction controls the annotation actions applied to a single
 * method of a class.
 */
class MethodAction
    //@olsen: not needed
    //implements AnnotationConstants
    extends Support {

    /* hash table for lookup of known ok Generic attributes */
//@olsen: disabled feature
/*
    private static Set safeGenericAttributes;

    private static void addSafeAttribute(String attrName) {
        safeGenericAttributes.add(attrName);
    }

    static {
        safeGenericAttributes = new HashSet();

        // Microsoft COM attributes
        addSafeAttribute("COM_Class_type");
        addSafeAttribute("COM_DispMethod");
        addSafeAttribute("COM_ExposedAs");
        addSafeAttribute("COM_ExposedAs_Group");
        addSafeAttribute("COM_FuncDesc");
        addSafeAttribute("COM_Guid");
        addSafeAttribute("COM_GuidPool");
        addSafeAttribute("COM_MapsTo");
        addSafeAttribute("COM_MethodPool");
        addSafeAttribute("COM_ProxiesTo");
        addSafeAttribute("COM_Safety");
        addSafeAttribute("COM_TypeDesc");
        addSafeAttribute("COM_VarTypeDesc");
        addSafeAttribute("COM_VtblMethod");
    }
*/

    /* The parent ClassAction of this MethodAction */
    //@olsen: made final
    private final ClassAction ca;

    /* The method to which the actions apply */
    //@olsen: made final
    private final ClassMethod theMethod;

    /* The code annotater for the method */
    //@olsen: made final
    private final MethodAnnotater annotater;

    /* Central repository for the options and classes */
    //@olsen: added association
    //@olsen: made final
    private final Environment env;

    /**
     * Returns true if any code annotations need to be performed on
     * this method.
     */
    boolean needsAnnotation() {
        return annotater.needsAnnotation();
    }

    /**
     * Returns the method for which this MethodAction applies
     */
    ClassMethod method() {
        return theMethod;
    }

    /**
     * Constructor
     */
    //@olsen: added parameter 'env' for association
    MethodAction(ClassAction ca,
                 ClassMethod method,
                 Environment env) {
        this.ca = ca;
        theMethod = method;
        this.env = env;
        annotater = new MethodAnnotater(ca, method, env);
    }

    /**
     * Examine the method to determine what actions are required
     */
    void check() {
        annotater.checkMethod();
//@olsen: disabled feature
/*
        if (env.verbose()) {
            CodeAttribute codeAttr = theMethod.codeAttribute();
            if (codeAttr != null) {
                Enumeration e = codeAttr.attributes().elements();
                while (e.hasMoreElements()) {
                    ClassAttribute attr = (ClassAttribute) e.nextElement();
                    if ((attr instanceof GenericAttribute) &&
                        safeGenericAttributes.contains(attr.attrName().asString())) {
                        String userClass = ca.classControl().userClassName();
                        String msg = "method "  + userClass +
                            "." + theMethod.name().asString() +
                            Descriptor.userMethodArgs(theMethod.signature().asString()) +
                            " contains an unrecognized attribute of type " +
                            attr.attrName().asString() + ".  " +
                            "Please check with Object Design support to see " +
                            "whether this is a problem.";
                        env.warning(msg, userClass);
                    }
                }
            }
        }
*/
    }

    /**
     * Retarget class references according to the class name mapping
     * table.
     */
//@olsen: disabled feature
/*
    void retarget(Map classTranslations) {
        // No action needed currently
    }
*/

    /**
     * Perform annotations
     */
    void annotate() {
        annotater.annotateMethod();
    }
}
