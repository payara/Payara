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

import org.jvnet.hk2.annotations.Contract;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Given a {@link TypeElement},
 * find all super-types that have {@link Contract},
 * including ones pointed by {@link ContractProvided}.
 *
 * @author Kohsuke Kawaguchi
 */
public class ContractFinder {
    /**
     * The entry point.
     */
    public static Set<TypeElement> find(TypeElement d) {
        LinkedHashSet<TypeElement> retVal = new LinkedHashSet<TypeElement>();
        
        for (Map.Entry<String, TypeElement> entry : new ContractFinder().check(d).result.entrySet()) {
            retVal.add(entry.getValue());
        }
        
        return retVal;
    }
    
    /**
     * Converts the Name from the Element to a String
     * @param name
     * @return
     */
    private static String convertNameToString(Name name) {
        if (name == null) return null;
        return name.toString();
    }

    /**
     * {@link TypeElement}s whose contracts are already checked.
     */
    private final Set<TypeElement> checkedInterfaces = new HashSet<TypeElement>();

    private final TreeMap<String, TypeElement> result = new TreeMap<String, TypeElement>();

    private ContractFinder() {
    }

    private ContractFinder check(TypeElement d) {

        // traverse up the inheritance tree and find all supertypes that have @Contract
        while(true) {
            checkSuperInterfaces(d);
            if (ElementKind.CLASS.equals(d.getKind())) {
                checkContract(d);
                TypeMirror sc = d.getSuperclass();
                if (sc.getKind().equals(TypeKind.NONE))
                    break;
                d = (TypeElement) ((DeclaredType) sc).asElement();
            } else
                break;
        }

        return this;
    }

    private void checkContract(TypeElement type) {
        if (type.getAnnotation(Contract.class) != null)
            result.put(convertNameToString(type.getQualifiedName()), type);
    }

    private void checkSuperInterfaces(TypeElement d) {
        for (TypeMirror intf : d.getInterfaces()) {
            TypeElement i = (TypeElement) ((DeclaredType) intf).asElement();
            if(checkedInterfaces.add(i)) {
                checkContract(i);
                checkSuperInterfaces(i);
            }
        }
    }
}
